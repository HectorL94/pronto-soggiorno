package app.prontosoggiorno.data

import app.prontosoggiorno.core.ChangeKind
import app.prontosoggiorno.core.classification.AdministrativeStatus
import app.prontosoggiorno.core.model.IdentifierType
import app.prontosoggiorno.core.model.PraticaNumber
import app.prontosoggiorno.data.db.CheckResultKind
import app.prontosoggiorno.data.db.CheckTrigger

/** Lo que la UI necesita de una pratica. Nunca contiene el número completo, solo enmascarado. */
data class PraticaSummary(
    val id: Long,
    val alias: String?,
    /** p. ej. `26RM••••56`; null si los datos cifrados ya no se pueden leer. */
    val maskedNumber: String?,
    val type: IdentifierType,
    val paused: Boolean,
    val monitoringCompleted: Boolean,
    /** Último intento, haya salido bien o mal. */
    val lastCheck: CheckRecord?,
    /** Último estado conocido del portal (última verificación exitosa). */
    val lastKnownState: CheckRecord?,
    /** Pausada automáticamente: el portal no la encuentra desde hace más de 30 días. */
    val notFoundPaused: Boolean = false,
) {
    /** Clave perdida (p. ej. tras restaurar el teléfono): solo se puede eliminar. */
    val unreadable: Boolean get() = maskedNumber == null
}

data class CheckRecord(
    val id: Long,
    val checkedAt: Long,
    val trigger: CheckTrigger,
    val result: CheckResultKind,
    val changeKind: ChangeKind,
    val status: AdministrativeStatus?,
    val statusText: String?,
    val office: String?,
    val linkLabel: String?,
    val linkUrl: String?,
    val failureDetail: String?,
)

/** Pratica lista para ser consultada por el trabajo en segundo plano. */
class MonitoredPratica(val id: Long, val alias: String?, val number: PraticaNumber) {
    override fun toString(): String = "MonitoredPratica(id=$id, number=$number)" // número enmascarado
}

sealed interface AddPraticaResult {
    data class Added(val id: Long) : AddPraticaResult
    data class Invalid(val reason: PraticaNumber.InvalidReason) : AddPraticaResult
    data object Duplicate : AddPraticaResult
}

/** Resultado de guardar una verificación. */
data class RecordedCheck(
    val praticaId: Long,
    val changeKind: ChangeKind,
    val status: AdministrativeStatus?,
    /** Se acaba de detectar que está listo y se detuvo el monitoreo. */
    val monitoringJustCompleted: Boolean,
    /** Esta verificación cumplió los 30 días de "no encontrado" y pausó el monitoreo. */
    val pausedNotFound: Boolean = false,
)
