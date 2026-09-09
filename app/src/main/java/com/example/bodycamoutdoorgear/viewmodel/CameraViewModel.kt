package com.example.bodycamoutdoorgear.viewmodel

import android.app.Application
import android.content.Context
import android.os.StatFs
import androidx.camera.video.Recording
import androidx.lifecycle.AndroidViewModel
import com.example.bodycamoutdoorgear.data.VideoQuality
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("bodycam_prefs", Context.MODE_PRIVATE)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _selectedQuality = MutableStateFlow(
        VideoQuality.entries[prefs.getInt("selected_quality", VideoQuality.MEDIUM.ordinal)]
    )
    val selectedQuality: StateFlow<VideoQuality> = _selectedQuality.asStateFlow()

    private val _lowLightMode = MutableStateFlow(false)
    val lowLightMode: StateFlow<Boolean> = _lowLightMode.asStateFlow()

    private val _isScreenDimmed = MutableStateFlow(false)
    val isScreenDimmed: StateFlow<Boolean> = _isScreenDimmed.asStateFlow()

    private val _recordingError = MutableStateFlow<String?>(null)
    val recordingError: StateFlow<String?> = _recordingError.asStateFlow()

    // Preview toggle — hide live feed to save battery (camera still records)
    private val _isPreviewEnabled = MutableStateFlow(true)
    val isPreviewEnabled: StateFlow<Boolean> = _isPreviewEnabled.asStateFlow()

    // Front/back camera
    private val _isFrontCamera = MutableStateFlow(false)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    var activeRecording: Recording? = null

    // Hardware volume button → toggle recording
    private val _hardwareButtonEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val hardwareButtonEvent: SharedFlow<Unit> = _hardwareButtonEvent.asSharedFlow()

    fun onHardwareButtonPressed() {
        _hardwareButtonEvent.tryEmit(Unit)
    }

    fun setRecording(recording: Boolean) {
        _isRecording.value = recording
    }

    fun setQuality(quality: VideoQuality) {
        _selectedQuality.value = quality
        prefs.edit().putInt("selected_quality", quality.ordinal).apply()
    }

    fun toggleLowLightMode() {
        _lowLightMode.value = !_lowLightMode.value
    }

    fun setScreenDimmed(dimmed: Boolean) {
        _isScreenDimmed.value = dimmed
    }

    fun togglePreview() {
        _isPreviewEnabled.value = !_isPreviewEnabled.value
    }

    fun flipCamera() {
        if (_isRecording.value) return  // can't flip while recording
        _isFrontCamera.value = !_isFrontCamera.value
    }

    fun setError(message: String?) {
        _recordingError.value = message
    }

    fun clearError() {
        _recordingError.value = null
    }

    fun hasEnoughStorage(): Boolean {
        val stat = StatFs(getApplication<Application>().filesDir.path)
        return stat.availableBytes > 50 * 1024 * 1024
    }

    override fun onCleared() {
        super.onCleared()
        activeRecording?.stop()
    }
}
