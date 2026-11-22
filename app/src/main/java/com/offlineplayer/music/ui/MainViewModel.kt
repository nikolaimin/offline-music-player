package com.offlineplayer.music.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.offlineplayer.music.data.MusicRepository
import com.offlineplayer.music.data.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val tracks: List<Track> = emptyList(),
    val totalCount: Int = 0,
    val hasQuery: Boolean = false,
    val isLoading: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application.applicationContext)

    private val allTracks = MutableStateFlow<List<Track>>(emptyList())
    private val query = MutableStateFlow("")
    private val isLoading = MutableStateFlow(false)
    private var loadJob: Job? = null

    val uiState: StateFlow<LibraryUiState> =
        combine(allTracks, query, isLoading) { all, q, loading ->
            val trimmed = q.trim()
            val shown = if (trimmed.isEmpty()) all else all.filter { it.matches(trimmed) }
            LibraryUiState(
                tracks = shown,
                totalCount = all.size,
                hasQuery = trimmed.isNotEmpty(),
                isLoading = loading
            )
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    /** Читает библиотеку заново. Повторный вызов во время загрузки игнорируется. */
    fun loadTracks() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            isLoading.value = true
            try {
                allTracks.value = repository.loadTracks()
            } finally {
                isLoading.value = false
            }
        }
    }
}
