package com.example.bodycamoutdoorgear.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.bodycamoutdoorgear.data.VideoQuality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


enum class QualityTier(val shortLabel: String) { LOW("LOW"), MED("MED"), HIGH("HIGH") }
enum class AppearanceMode(val label: String) { NORMAL("NORMAL"), SIMPLE("SIMPLE"), TACTICAL("TACTICAL"), MATCHA("MATCHA"), ICE_CREAM("ICE CR."), TROPICAL("TROPICAL"), SPIDER("SPIDER") }
enum class DisplayMode(val label: String) { SAVE_BATTERY("SAVE BAT."), NORMAL("NORMAL") }

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("bodycam_settings", Context.MODE_PRIVATE)

    private val _videoQualityTier = MutableStateFlow(
        QualityTier.entries[prefs.getInt("video_quality_tier", QualityTier.HIGH.ordinal)]
    )
    val videoQualityTier: StateFlow<QualityTier> = _videoQualityTier.asStateFlow()

    private val _photoQualityTier = MutableStateFlow(
        QualityTier.entries[prefs.getInt("photo_quality_tier", QualityTier.HIGH.ordinal)]
    )
    val photoQualityTier: StateFlow<QualityTier> = _photoQualityTier.asStateFlow()

    private val _gridEnabled = MutableStateFlow(
        prefs.getBoolean("grid_enabled", true)
    )
    val gridEnabled: StateFlow<Boolean> = _gridEnabled.asStateFlow()

    private val _appearanceMode = MutableStateFlow(
        AppearanceMode.entries[prefs.getInt("appearance_mode", AppearanceMode.NORMAL.ordinal)]
    )
    val appearanceMode: StateFlow<AppearanceMode> = _appearanceMode.asStateFlow()

    private val _displayMode = MutableStateFlow(
        DisplayMode.entries[prefs.getInt("display_mode", DisplayMode.SAVE_BATTERY.ordinal)]
    )
    val displayMode: StateFlow<DisplayMode> = _displayMode.asStateFlow()

    init {
        // Remove legacy low_light pref so any user who had it on is reset
        prefs.edit().remove("low_light").apply()
    }

    fun toggleGrid() {
        _gridEnabled.value = !_gridEnabled.value
        prefs.edit().putBoolean("grid_enabled", _gridEnabled.value).apply()
    }

    val videoQuality: VideoQuality
        get() = when (_videoQualityTier.value) {
            QualityTier.LOW  -> VideoQuality.LOW
            QualityTier.MED  -> VideoQuality.MEDIUM
            QualityTier.HIGH -> VideoQuality.HIGH
        }

    val photoJpegQuality: Int
        get() = when (_photoQualityTier.value) {
            QualityTier.LOW  -> 25
            QualityTier.MED  -> 65
            QualityTier.HIGH -> 92
        }

    fun setVideoQualityTier(tier: QualityTier) {
        _videoQualityTier.value = tier
        prefs.edit().putInt("video_quality_tier", tier.ordinal).apply()
    }

    fun setPhotoQualityTier(tier: QualityTier) {
        _photoQualityTier.value = tier
        prefs.edit().putInt("photo_quality_tier", tier.ordinal).apply()
    }

    fun setAppearanceMode(mode: AppearanceMode) {
        _appearanceMode.value = mode
        prefs.edit().putInt("appearance_mode", mode.ordinal).apply()
    }

    fun setDisplayMode(mode: DisplayMode) {
        _displayMode.value = mode
        prefs.edit().putInt("display_mode", mode.ordinal).apply()
    }

    fun resetIntro() {
        getApplication<Application>().getSharedPreferences("bodycam_prefs", Context.MODE_PRIVATE)
            .edit().remove("hasSeenDisclaimer").apply()
    }
}
