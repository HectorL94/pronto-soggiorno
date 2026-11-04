package app.prontosoggiorno.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.prontosoggiorno.core.model.IdentifierType
import app.prontosoggiorno.core.model.PraticaNumber
import app.prontosoggiorno.data.AddPraticaResult
import app.prontosoggiorno.data.PraticaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

enum class AddError { EMPTY, WRONG_LENGTH, WRONG_FORMAT, DUPLICATE }

data class AddUiState(
    val number: String = "",
    val alias: String = "",
    /** Tipo reconocido mientras se escribe (null si aún no es válido). */
    val detectedType: IdentifierType? = null,
    /** Error a mostrar; solo aparece después de pulsar "Añadir". */
    val error: AddError? = null,
    val saving: Boolean = false,
    /** Pratica creada: la pantalla navega a su detalle. */
    val addedId: Long? = null,
)

class AddPraticaViewModel(private val repository: PraticaRepository) : ViewModel() {
    private val _state = MutableStateFlow(AddUiState())
    val state: StateFlow<AddUiState> = _state.asStateFlow()

    fun onNumberChange(value: String) {
        // En mayúsculas, como en los documentos (algunos teclados escriben en minúsculas).
        val trimmed = value.take(MAX_INPUT).uppercase(Locale.ROOT)
        val parsed = PraticaNumber.parse(trimmed)
        _state.update {
            it.copy(
                number = trimmed,
                detectedType = (parsed as? PraticaNumber.ParseResult.Valid)?.number?.type,
                error = null,
            )
        }
    }

    fun onAliasChange(value: String) {
        _state.update { it.copy(alias = value.take(PraticaRepository.MAX_ALIAS_LENGTH)) }
    }

    fun save() {
        val current = _state.value
        if (current.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val result = repository.add(current.number, current.alias)
            _state.update {
                when (result) {
                    is AddPraticaResult.Added -> it.copy(saving = false, addedId = result.id)
                    AddPraticaResult.Duplicate -> it.copy(saving = false, error = AddError.DUPLICATE)
                    is AddPraticaResult.Invalid -> it.copy(
                        saving = false,
                        error = when (result.reason) {
                            PraticaNumber.InvalidReason.EMPTY -> AddError.EMPTY
                            PraticaNumber.InvalidReason.WRONG_LENGTH -> AddError.WRONG_LENGTH
                            PraticaNumber.InvalidReason.WRONG_FORMAT -> AddError.WRONG_FORMAT
                        },
                    )
                }
            }
        }
    }

    private companion object {
        /** Margen para espacios y guiones que el usuario pueda escribir. */
        const val MAX_INPUT = 24
    }
}
