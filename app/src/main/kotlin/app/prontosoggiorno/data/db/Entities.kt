package app.prontosoggiorno.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import app.prontosoggiorno.core.ChangeKind
import app.prontosoggiorno.core.classification.AdministrativeStatus
import app.prontosoggiorno.core.model.IdentifierType

/**
 * Una pratica que el usuario monitorea.
 * El número y el alias se guardan SOLO cifrados (ver FieldCipher); nunca en claro.
 * Nota: Room guarda los enums por su nombre; no renombrar sus valores sin una migración.
 */
@Entity(tableName = "pratiche")
class PraticaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "number_encrypted", typeAffinity = ColumnInfo.BLOB) val numberEncrypted: ByteArray,
    @ColumnInfo(name = "alias_encrypted", typeAffinity = ColumnInfo.BLOB) val aliasEncrypted: ByteArray?,
    @ColumnInfo(name = "type") val type: IdentifierType,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    /** El usuario pausó las verificaciones automáticas. */
    @ColumnInfo(name = "paused") val paused: Boolean = false,
    /** El permiso está listo: se dejó de consultar automáticamente. */
    @ColumnInfo(name = "monitoring_completed") val monitoringCompleted: Boolean = false,
    /**
     * El portal no encontró el número durante más de 30 días seguidos: la app pausó sola las
     * verificaciones automáticas. Se reactiva sola si una verificación lo encuentra.
     */
    @ColumnInfo(name = "not_found_paused", defaultValue = "0") val notFoundPaused: Boolean = false,
    /** Cuándo el usuario reanudó a mano tras esa pausa: el plazo de 30 días cuenta desde aquí. */
    @ColumnInfo(name = "resumed_at") val resumedAt: Long? = null,
)

enum class CheckTrigger { SCHEDULED, MANUAL }

/** Resultado técnico de una verificación. */
enum class CheckResultKind { CHECKED, NETWORK_FAILURE, HTTP_FAILURE, RESPONSE_TOO_LARGE, UNEXPECTED_RESPONSE }

/**
 * Una verificación realizada. No contiene el número de pratica: solo lo que publicó el portal
 * (estado, texto, oficina) y datos técnicos del intento.
 */
@Entity(
    tableName = "checks",
    foreignKeys = [
        ForeignKey(
            entity = PraticaEntity::class,
            parentColumns = ["id"],
            childColumns = ["pratica_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("pratica_id")],
)
data class CheckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "pratica_id") val praticaId: Long,
    @ColumnInfo(name = "checked_at") val checkedAt: Long,
    @ColumnInfo(name = "check_trigger") val trigger: CheckTrigger,
    @ColumnInfo(name = "result") val result: CheckResultKind,
    @ColumnInfo(name = "change_kind") val changeKind: ChangeKind,
    @ColumnInfo(name = "status") val status: AdministrativeStatus?,
    @ColumnInfo(name = "status_text") val statusText: String?,
    @ColumnInfo(name = "office") val office: String?,
    @ColumnInfo(name = "link_label") val linkLabel: String?,
    @ColumnInfo(name = "link_url") val linkUrl: String?,
    @ColumnInfo(name = "fingerprint") val fingerprint: String?,
    /** Detalle técnico del fallo: tipo de error de red, código HTTP o motivo. */
    @ColumnInfo(name = "failure_detail") val failureDetail: String?,
)
