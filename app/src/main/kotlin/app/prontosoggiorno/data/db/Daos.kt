package app.prontosoggiorno.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PraticaDao {
    @Insert
    suspend fun insert(pratica: PraticaEntity): Long

    @Query("SELECT * FROM pratiche ORDER BY created_at, id")
    fun observeAll(): Flow<List<PraticaEntity>>

    @Query("SELECT * FROM pratiche ORDER BY created_at, id")
    suspend fun getAll(): List<PraticaEntity>

    @Query("SELECT * FROM pratiche WHERE id = :id")
    suspend fun get(id: Long): PraticaEntity?

    @Query("UPDATE pratiche SET alias_encrypted = :aliasEncrypted WHERE id = :id")
    suspend fun setAlias(id: Long, aliasEncrypted: ByteArray?)

    @Query("UPDATE pratiche SET paused = :paused WHERE id = :id")
    suspend fun setPaused(id: Long, paused: Boolean)

    @Query("UPDATE pratiche SET monitoring_completed = :completed WHERE id = :id")
    suspend fun setMonitoringCompleted(id: Long, completed: Boolean)

    @Query("UPDATE pratiche SET not_found_paused = :paused WHERE id = :id")
    suspend fun setNotFoundPaused(id: Long, paused: Boolean)

    @Query("UPDATE pratiche SET not_found_paused = 0, resumed_at = :now WHERE id = :id")
    suspend fun resumeAfterNotFound(id: Long, now: Long)

    @Query("DELETE FROM pratiche WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM pratiche")
    suspend fun deleteAll()
}

@Dao
interface CheckDao {
    @Insert
    suspend fun insert(check: CheckEntity): Long

    @Query("SELECT * FROM checks WHERE pratica_id = :praticaId ORDER BY id DESC")
    fun observeForPratica(praticaId: Long): Flow<List<CheckEntity>>

    /** Última verificación (exitosa o no) de cada pratica. */
    @Query("SELECT * FROM checks WHERE id IN (SELECT MAX(id) FROM checks GROUP BY pratica_id)")
    fun observeLatest(): Flow<List<CheckEntity>>

    /** Última verificación EXITOSA de cada pratica: es el último estado conocido. */
    @Query(
        "SELECT * FROM checks WHERE id IN " +
            "(SELECT MAX(id) FROM checks WHERE result = 'CHECKED' GROUP BY pratica_id)",
    )
    fun observeLatestSuccessful(): Flow<List<CheckEntity>>

    @Query(
        "SELECT fingerprint FROM checks WHERE pratica_id = :praticaId AND result = 'CHECKED' " +
            "ORDER BY id DESC LIMIT 1",
    )
    suspend fun lastSuccessfulFingerprint(praticaId: Long): String?

    /**
     * Desde cuándo el portal responde "no encontrado" sin interrupción: la primera verificación
     * NOT_FOUND posterior al último estado distinto. Null si el último estado no es NOT_FOUND.
     * (Los fallos de red no cuentan: solo verificaciones con respuesta.)
     */
    @Query(
        "SELECT MIN(checked_at) FROM checks WHERE pratica_id = :praticaId AND result = 'CHECKED' " +
            "AND status = 'NOT_FOUND' AND id > COALESCE((SELECT MAX(id) FROM checks " +
            "WHERE pratica_id = :praticaId AND result = 'CHECKED' AND status != 'NOT_FOUND'), 0)",
    )
    suspend fun notFoundStreakStart(praticaId: Long): Long?

    /**
     * Limpieza: borra verificaciones rutinarias (sin cambios o fallidas) anteriores a [before].
     * Se conservan siempre los cambios de estado y la última verificación de cada pratica.
     */
    @Query(
        "DELETE FROM checks WHERE checked_at < :before " +
            "AND change_kind IN ('UNCHANGED', 'FAILED') " +
            "AND id NOT IN (SELECT MAX(id) FROM checks GROUP BY pratica_id)",
    )
    suspend fun pruneRoutine(before: Long)
}
