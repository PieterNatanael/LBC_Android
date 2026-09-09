package com.example.bodycamoutdoorgear.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PhotoQuality(val label: String, val jpegQuality: Int, val scale: Float) {
    LOW("LOW", 25, 0.5f),
    MED("MED", 65, 1.0f),
    HIGH("HIGH", 92, 1.0f);

    companion object {
        val entries get() = values().toList()
    }
}

enum class CaptureStatus { SAVING, SUCCESS, FAILURE }

class PhotoViewModel(application: Application) : AndroidViewModel(application) {

    private val _selectedQuality = MutableStateFlow(PhotoQuality.HIGH)
    val selectedQuality: StateFlow<PhotoQuality> = _selectedQuality.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _captureStatus = MutableStateFlow<CaptureStatus?>(null)
    val captureStatus: StateFlow<CaptureStatus?> = _captureStatus.asStateFlow()

    private val _isPreviewEnabled = MutableStateFlow(true)
    val isPreviewEnabled: StateFlow<Boolean> = _isPreviewEnabled.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(false)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    private val _isScreenDimmed = MutableStateFlow(false)
    val isScreenDimmed: StateFlow<Boolean> = _isScreenDimmed.asStateFlow()

    // Hardware volume button → trigger shutter
    private val _hardwareButtonEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val hardwareButtonEvent: SharedFlow<Unit> = _hardwareButtonEvent.asSharedFlow()

    fun onHardwareButtonPressed() {
        _hardwareButtonEvent.tryEmit(Unit)
    }

    fun setQuality(q: PhotoQuality) {
        _selectedQuality.value = q
    }

    fun togglePreview() {
        _isPreviewEnabled.value = !_isPreviewEnabled.value
    }

    fun flipCamera() {
        if (_isCapturing.value) return
        _isFrontCamera.value = !_isFrontCamera.value
    }

    fun setScreenDimmed(d: Boolean) {
        _isScreenDimmed.value = d
    }

    fun setCapturing(c: Boolean) {
        _isCapturing.value = c
    }

    fun setCaptureStatus(s: CaptureStatus?) {
        _captureStatus.value = s
        if (s == CaptureStatus.SUCCESS || s == CaptureStatus.FAILURE) {
            viewModelScope.launch {
                delay(2000)
                _captureStatus.value = null
            }
        }
    }
}
