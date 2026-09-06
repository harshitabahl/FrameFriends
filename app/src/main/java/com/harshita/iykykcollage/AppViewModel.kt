package com.harshita.iykykcollage

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.harshita.iykykcollage.model.ProcessingProgress
import com.harshita.iykykcollage.model.ProcessingResult
import com.harshita.iykykcollage.processing.VideoProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AppState {
    data object Ready : AppState
    data class Processing(val uri: Uri, val progress: ProcessingProgress) : AppState
    data class Complete(val uri: Uri, val result: ProcessingResult) : AppState
    data class Failed(val uri: Uri?, val message: String) : AppState
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val processor = VideoProcessor(application)
    private val _state = MutableStateFlow<AppState>(AppState.Ready)
    val state: StateFlow<AppState> = _state.asStateFlow()

    fun process(uri: Uri) {
        if (_state.value is AppState.Processing) return
        viewModelScope.launch {
            _state.value = AppState.Processing(uri, ProcessingProgress(0f, "Preparing"))
            runCatching { processor.process(uri) { _state.value = AppState.Processing(uri, it) } }
                .onSuccess { _state.value = AppState.Complete(uri, it) }
                .onFailure { error -> _state.value = AppState.Failed(uri, friendlyMessage(error)) }
        }
    }

    fun reset() { _state.value = AppState.Ready }
    override fun onCleared() { processor.close(); super.onCleared() }

    private fun friendlyMessage(error: Throwable): String = when {
        error.message?.contains("mobile_face_net") == true -> "Embedding model missing. Add mobile_face_net.tflite to app/src/main/assets, then rebuild."
        else -> error.message ?: "Processing failed. Please try another video."
    }
}
