package app.prontosoggiorno.core

import app.prontosoggiorno.core.classification.AdministrativeStatus

/** Qué supuso una verificación respecto a la anterior. */
enum class ChangeKind {
    /** Primera verificación exitosa de esta pratica. */
    FIRST_RESULT,

    /** El texto del portal cambió: hay que avisar al usuario. */
    CHANGED,

    /** Mismo texto que la última verificación exitosa. */
    UNCHANGED,

    /** No se pudo verificar. NUNCA cuenta como cambio ni como "sin cambios". */
    FAILED,
}

object ChangeDetector {

    /**
     * @param previousFingerprint huella de la última verificación EXITOSA, o null si nunca la hubo.
     * Los fallos intermedios no se tienen en cuenta: un error de red seguido del mismo texto
     * de siempre es [ChangeKind.UNCHANGED], no un cambio.
     */
    fun evaluate(previousFingerprint: String?, outcome: CheckOutcome): ChangeKind = when (outcome) {
        is CheckOutcome.Failed -> ChangeKind.FAILED
        is CheckOutcome.Checked -> when (previousFingerprint) {
            null -> ChangeKind.FIRST_RESULT
            outcome.classification.fingerprint -> ChangeKind.UNCHANGED
            else -> ChangeKind.CHANGED
        }
    }

    /** Cuando el permiso está listo ya no tiene sentido seguir consultando. */
    fun shouldStopMonitoring(outcome: CheckOutcome): Boolean =
        outcome is CheckOutcome.Checked &&
            outcome.classification.status == AdministrativeStatus.READY_FOR_COLLECTION
}
