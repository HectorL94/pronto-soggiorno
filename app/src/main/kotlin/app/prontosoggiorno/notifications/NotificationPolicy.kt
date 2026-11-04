package app.prontosoggiorno.notifications

import app.prontosoggiorno.core.ChangeKind
import app.prontosoggiorno.core.classification.AdministrativeStatus
import app.prontosoggiorno.data.RecordedCheck
import app.prontosoggiorno.data.db.CheckTrigger

enum class NotificationKind {
    /** El permiso está listo: canal de prioridad alta. */
    READY,

    /** Cualquier otro cambio del texto publicado por el portal. */
    STATUS_CHANGED,

    /** 30 días sin aparecer en el portal: se pausó la verificación automática. */
    NOT_FOUND_PAUSED,
}

/** Decide si una verificación merece una notificación. Lógica pura, sin Android. */
object NotificationPolicy {

    fun decide(recorded: RecordedCheck, trigger: CheckTrigger, notifyAllChanges: Boolean): NotificationKind? {
        // En una verificación manual el usuario está mirando la pantalla: no se le notifica.
        if (trigger == CheckTrigger.MANUAL) return null
        // La app deja de verificar sola: hay que avisarlo siempre, o el usuario no lo sabría.
        if (recorded.pausedNotFound) return NotificationKind.NOT_FOUND_PAUSED

        val ready = recorded.status == AdministrativeStatus.READY_FOR_COLLECTION
        return when (recorded.changeKind) {
            // "Listo" se avisa siempre, aunque el usuario haya pedido solo avisos importantes.
            ChangeKind.CHANGED -> when {
                ready -> NotificationKind.READY
                notifyAllChanges -> NotificationKind.STATUS_CHANGED
                else -> null
            }
            // Primera verificación automática (p. ej. añadida sin conexión) que ya resulta lista.
            ChangeKind.FIRST_RESULT -> if (ready) NotificationKind.READY else null
            ChangeKind.UNCHANGED, ChangeKind.FAILED -> null
        }
    }
}
