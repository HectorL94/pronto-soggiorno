package app.prontosoggiorno.data

import androidx.room.withTransaction
import app.prontosoggiorno.core.ChangeDetector
import app.prontosoggiorno.core.CheckOutcome
import app.prontosoggiorno.core.classification.AdministrativeStatus
import app.prontosoggiorno.core.model.PraticaNumber
import app.prontosoggiorno.data.crypto.FieldCipher
import app.prontosoggiorno.data.crypto.FieldCipher.Purpose
import app.prontosoggiorno.data.db.AppDatabase
import app.prontosoggiorno.data.db.CheckEntity
import app.prontosoggiorno.data.db.CheckResultKind
import app.prontosoggiorno.data.db.CheckTrigger
import app.prontosoggiorno.data.db.PraticaEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Único punto de acceso a las pratiche y su historial.
 * Se encarga de cifrar/descifrar: el resto de la app nunca ve los datos cifrados ni
 * maneja el número completo salvo [monitoredPratiche] / [numberFor].
 */
class PraticaRepository(
    private val db: AppDatabase,
    private val cipher: FieldCipher,
    private val clock: () -> Long = System::currentTimeMillis,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val praticaDao = db.praticaDao()
    private val checkDao = db.checkDao()

    fun observeSummaries(): Flow<List<PraticaSummary>> =
        combine(
            praticaDao.observeAll(),
            checkDao.observeLatest(),
            checkDao.observeLatestSuccessful(),
        ) { pratiche, latest, latestOk ->
            val latestById = latest.associateBy { it.praticaId }
            val latestOkById = latestOk.associateBy { it.praticaId }
            pratiche.map { p ->
                PraticaSummary(
                    id = p.id,
                    alias = p.aliasEncrypted?.let { cipher.decrypt(it, Purpose.ALIAS) },
                    maskedNumber = decryptNumber(p)?.masked(),
                    type = p.type,
                    paused = p.paused,
                    monitoringCompleted = p.monitoringCompleted,
                    lastCheck = latestById[p.id]?.toRecord(),
                    lastKnownState = latestOkById[p.id]?.toRecord(),
                    notFoundPaused = p.notFoundPaused,
                )
            }
        }.flowOn(dispatcher)

    fun observeHistory(praticaId: Long): Flow<List<CheckRecord>> =
        checkDao.observeForPratica(praticaId).map { list -> list.map { it.toRecord() } }.flowOn(dispatcher)

    suspend fun add(rawNumber: String, alias: String?): AddPraticaResult = withContext(dispatcher) {
        val number = when (val parsed = PraticaNumber.parse(rawNumber)) {
            is PraticaNumber.ParseResult.Invalid -> return@withContext AddPraticaResult.Invalid(parsed.reason)
            is PraticaNumber.ParseResult.Valid -> parsed.number
        }
        db.withTransaction {
            // Duplicados: hay que descifrar para comparar (son pocas pratiche). No se guarda un
            // hash del número: con tan pocas combinaciones posibles sería fácil de invertir.
            if (praticaDao.getAll().any { decryptNumber(it) == number }) {
                return@withTransaction AddPraticaResult.Duplicate
            }
            val id = praticaDao.insert(
                PraticaEntity(
                    numberEncrypted = cipher.encrypt(number.reveal(), Purpose.PRATICA_NUMBER),
                    aliasEncrypted = encryptAlias(alias),
                    type = number.type,
                    createdAt = clock(),
                ),
            )
            AddPraticaResult.Added(id)
        }
    }

    suspend fun rename(praticaId: Long, alias: String?) = withContext(dispatcher) {
        praticaDao.setAlias(praticaId, encryptAlias(alias))
    }

    suspend fun setPaused(praticaId: Long, paused: Boolean) = withContext(dispatcher) {
        praticaDao.setPaused(praticaId, paused)
    }

    /** Reanuda el monitoreo de una pratica que ya estaba lista (p. ej. a petición del usuario). */
    suspend fun resumeMonitoring(praticaId: Long) = withContext(dispatcher) {
        praticaDao.setMonitoringCompleted(praticaId, false)
    }

    /** Reanuda tras la pausa por "no encontrado": vuelve a dar 30 días desde ahora. */
    suspend fun resumeAfterNotFound(praticaId: Long) = withContext(dispatcher) {
        praticaDao.resumeAfterNotFound(praticaId, clock())
    }

    /** Borra la pratica y todo su historial (borrado en cascada). */
    suspend fun delete(praticaId: Long) = withContext(dispatcher) { praticaDao.delete(praticaId) }

    suspend fun deleteAll() = withContext(dispatcher) { praticaDao.deleteAll() }

    /** Número completo para una consulta manual o para abrir el portal. */
    suspend fun numberFor(praticaId: Long): PraticaNumber? = withContext(dispatcher) {
        praticaDao.get(praticaId)?.let(::decryptNumber)
    }

    /**
     * Pratiche que el trabajo en segundo plano debe consultar: ni pausadas (por el usuario o por
     * llevar 30 días sin aparecer) ni ya listas.
     */
    suspend fun monitoredPratiche(): List<MonitoredPratica> = withContext(dispatcher) {
        praticaDao.getAll()
            .filter { !it.paused && !it.monitoringCompleted && !it.notFoundPaused }
            .mapNotNull { p ->
                decryptNumber(p)?.let { number ->
                    MonitoredPratica(p.id, p.aliasEncrypted?.let { cipher.decrypt(it, Purpose.ALIAS) }, number)
                }
            }
    }

    /**
     * Guarda el resultado de una verificación y decide si hubo cambio.
     * Devuelve null si la pratica se eliminó mientras se estaba verificando.
     */
    suspend fun recordOutcome(praticaId: Long, outcome: CheckOutcome, trigger: CheckTrigger): RecordedCheck? =
        withContext(dispatcher) {
            db.withTransaction {
                val pratica = praticaDao.get(praticaId) ?: return@withTransaction null
                val now = clock()
                val changeKind = ChangeDetector.evaluate(checkDao.lastSuccessfulFingerprint(praticaId), outcome)
                checkDao.insert(outcome.toEntity(praticaId, now, trigger, changeKind))

                val stop = ChangeDetector.shouldStopMonitoring(outcome) && !pratica.monitoringCompleted
                if (stop) praticaDao.setMonitoringCompleted(praticaId, true)

                val status = (outcome as? CheckOutcome.Checked)?.classification?.status
                val pausedNotFound = updateNotFoundPause(pratica, status, now)

                checkDao.pruneRoutine(before = now - HISTORY_RETENTION_MS)

                RecordedCheck(
                    praticaId = praticaId,
                    changeKind = changeKind,
                    status = status,
                    monitoringJustCompleted = stop,
                    pausedNotFound = pausedNotFound,
                )
            }
        }

    /**
     * "No encontrado" durante [NOT_FOUND_GRACE_MS] seguidos → pausa automática (devuelve true).
     * Cualquier otro estado con respuesta del portal → se quita la pausa (reactivación sola).
     * Los fallos de red no cambian nada.
     */
    private suspend fun updateNotFoundPause(pratica: PraticaEntity, status: AdministrativeStatus?, now: Long): Boolean {
        when (status) {
            null -> return false
            AdministrativeStatus.NOT_FOUND -> {
                if (pratica.notFoundPaused) return false
                val streakStart = checkDao.notFoundStreakStart(pratica.id) ?: now
                val since = maxOf(streakStart, pratica.resumedAt ?: 0L)
                if (now - since < NOT_FOUND_GRACE_MS) return false
                praticaDao.setNotFoundPaused(pratica.id, true)
                return true
            }
            else -> {
                if (pratica.notFoundPaused) praticaDao.setNotFoundPaused(pratica.id, false)
                return false
            }
        }
    }

    private fun decryptNumber(entity: PraticaEntity): PraticaNumber? =
        cipher.decrypt(entity.numberEncrypted, Purpose.PRATICA_NUMBER)
            ?.let { PraticaNumber.parse(it) as? PraticaNumber.ParseResult.Valid }
            ?.number

    private fun encryptAlias(alias: String?): ByteArray? =
        normalizeAlias(alias)?.let { cipher.encrypt(it, Purpose.ALIAS) }

    companion object {
        const val MAX_ALIAS_LENGTH = 40

        /** Las verificaciones rutinarias se conservan 60 días; los cambios de estado, siempre. */
        val HISTORY_RETENTION_MS: Long = TimeUnit.DAYS.toMillis(60)

        /**
         * Días de "no encontrado" seguidos antes de pausar la verificación automática.
         * Las solicitudes nuevas pueden tardar días en aparecer: por eso no se pausa enseguida.
         */
        val NOT_FOUND_GRACE_MS: Long = TimeUnit.DAYS.toMillis(30)

        fun normalizeAlias(alias: String?): String? =
            alias?.trim()?.take(MAX_ALIAS_LENGTH)?.takeIf { it.isNotEmpty() }
    }
}

private fun CheckOutcome.toEntity(
    praticaId: Long,
    now: Long,
    trigger: CheckTrigger,
    changeKind: app.prontosoggiorno.core.ChangeKind,
): CheckEntity {
    val base = CheckEntity(
        praticaId = praticaId,
        checkedAt = now,
        trigger = trigger,
        result = CheckResultKind.CHECKED,
        changeKind = changeKind,
        status = null, statusText = null, office = null,
        linkLabel = null, linkUrl = null, fingerprint = null, failureDetail = null,
    )
    return when (this) {
        is CheckOutcome.Checked -> {
            val c = classification
            val link = c.links.firstOrNull()
            base.copy(
                status = c.status,
                statusText = c.statusText,
                office = c.office,
                linkLabel = link?.label,
                linkUrl = link?.url?.toString(),
                fingerprint = c.fingerprint,
            )
        }
        is CheckOutcome.NetworkFailure ->
            base.copy(result = CheckResultKind.NETWORK_FAILURE, failureDetail = kind.name)
        is CheckOutcome.HttpFailure ->
            base.copy(result = CheckResultKind.HTTP_FAILURE, failureDetail = code.toString())
        CheckOutcome.ResponseTooLarge ->
            base.copy(result = CheckResultKind.RESPONSE_TOO_LARGE)
        is CheckOutcome.UnexpectedResponse ->
            base.copy(result = CheckResultKind.UNEXPECTED_RESPONSE, failureDetail = reason.name)
    }
}

private fun CheckEntity.toRecord() = CheckRecord(
    id = id,
    checkedAt = checkedAt,
    trigger = trigger,
    result = result,
    changeKind = changeKind,
    status = status,
    statusText = statusText,
    office = office,
    linkLabel = linkLabel,
    linkUrl = linkUrl,
    failureDetail = failureDetail,
)
