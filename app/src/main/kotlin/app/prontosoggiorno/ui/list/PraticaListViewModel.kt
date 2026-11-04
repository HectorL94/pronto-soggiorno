package app.prontosoggiorno.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.prontosoggiorno.data.PraticaRepository
import app.prontosoggiorno.data.PraticaSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface ListUiState {
    data object Loading : ListUiState
    data class Loaded(val pratiche: List<PraticaSummary>) : ListUiState
}

class PraticaListViewModel(repository: PraticaRepository) : ViewModel() {
    val state: StateFlow<ListUiState> = repository.observeSummaries()
        .map { ListUiState.Loaded(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListUiState.Loading)
}
