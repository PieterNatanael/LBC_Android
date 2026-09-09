package com.example.bodycamoutdoorgear.viewmodel

import android.app.Application
import android.content.ContentValues
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    fun getDuration(videoPath: String): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            retriever.release()
            dur
        } catch (e: Exception) {
            0L
        }
    }

    fun saveToGallery(videoPath: String) {
        viewModelScope.launch {
            _isSaving.value = true
            _saveError.value = null
            val success = withContext(Dispatchers.IO) {
                try {
                    val file = File(videoPath)
                    val resolver = getApplication<Application>().contentResolver
                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/LBC")
                            put(MediaStore.Video.Media.IS_PENDING, 1)
                        }
                    }
                    val uri: Uri? = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { out ->
                            FileInputStream(file).use { it.copyTo(out) }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            values.clear()
                            values.put(MediaStore.Video.Media.IS_PENDING, 0)
                            resolver.update(uri, values, null, null)
                        }
                        true
                    } else false
                } catch (e: Exception) {
                    false
                }
            }
            _isSaving.value = false
            if (success) {
                _saveSuccess.value = true
                delay(2000)
                _saveSuccess.value = false
            } else {
                _saveError.value = "Failed to save video to gallery"
            }
        }
    }

    fun savePhotoToGallery(photoPath: String) {
        viewModelScope.launch {
            _isSaving.value = true
            _saveError.value = null
            val success = withContext(Dispatchers.IO) {
                try {
                    val file = File(photoPath)
                    val resolver = getApplication<Application>().contentResolver
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/LBC")
                            put(MediaStore.Images.Media.IS_PENDING, 1)
                        }
                    }
                    val uri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { out ->
                            FileInputStream(file).use { it.copyTo(out) }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            values.clear()
                            values.put(MediaStore.Images.Media.IS_PENDING, 0)
                            resolver.update(uri, values, null, null)
                        }
                        true
                    } else false
                } catch (e: Exception) {
                    false
                }
            }
            _isSaving.value = false
            if (success) {
                _saveSuccess.value = true
                delay(2000)
                _saveSuccess.value = false
            } else {
                _saveError.value = "Failed to save photo to gallery"
            }
        }
    }

    fun clearSaveError() {
        _saveError.value = null
    }
}
