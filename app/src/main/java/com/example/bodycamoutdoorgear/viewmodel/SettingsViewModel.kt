package com.example.bodycamoutdoorgear.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.bodycamoutdoorgear.data.VideoQuality
import com.example.bodycamoutdoorgear.sound.ShutterSound
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


enum class QualityTier(val shortLabel: String) { LOW("LOW"), MED("MED"), HIGH("HIGH") }
enum class AppearanceMode(val label: String) { NORMAL("NORMAL"), SIMPLE("SIMPLE"), TACTICAL("TACTICAL"), MATCHA("MATCHA"), ICE_CREAM("ICE CR."), TROPICAL("TROPICAL"), SPIDER("SPIDER"), MARS("MARS") }
// Stored by position (see SettingsViewModel), so new modes go at the END. GIVEAWAY
// is shown first in the pickers, but must not come first here: that would shift
// every existing user's saved mode onto a different one.
enum class DisplayMode(val label: String, val title: String, val subtitle: String) {
    SAVE_BATTERY("SAVE BAT.", "SAVE BATTERY", "Compact preview, lower power use"),
    NORMAL("NORMAL", "NORMAL", "Full screen preview, uses more battery"),
    CIRCLE("CIRCLE", "CIRCLE", "Compact round preview, lower power use"),
    YAPPING("YAP", "YAPPING", "Script on screen, preview shrinks to a movable corner"),
    GIVEAWAY("GIVEAWAY", "GIVEAWAY", "Photo of something you don't need, as a shareable card"),
    SALE("SALE", "FOR SALE", "Photo of something you want to sell, with its price");

    /** Giveaway and For Sale both turn what is shot into a card. */
    val makesCard: Boolean get() = this == GIVEAWAY || this == SALE

    /** Which card this mode makes; null for the modes that just save the photo or video. */
    val cardMode: com.example.bodycamoutdoorgear.giveaway.CardMode?
        get() = when (this) {
            GIVEAWAY -> com.example.bodycamoutdoorgear.giveaway.CardMode.GIVEAWAY
            SALE -> com.example.bodycamoutdoorgear.giveaway.CardMode.SALE
            else -> null
        }

    companion object {
        /** The order the settings list shows them in: the card modes first. */
        val pickerOrder: List<DisplayMode> = listOf(GIVEAWAY, SALE) + entries.filter { !it.makesCard }
    }
}

/**
 * How the gallery grid is ordered. Recently added is the default: it puts
 * whatever was just recorded, received or imported on top. Date taken is the
 * file's own date (what every thumbnail shows). The two only differ for
 * imports — see AddedDateStore.
 */
enum class GallerySort(val label: String) {
    RECENTLY_ADDED("Recently added"),
    DATE_TAKEN("Date taken")
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("bodycam_settings", Context.MODE_PRIVATE)

    private val _videoQualityTier = MutableStateFlow(
        QualityTier.entries[prefs.getInt("video_quality_tier", QualityTier.MED.ordinal)]
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
        AppearanceMode.entries[prefs.getInt("appearance_mode", AppearanceMode.MARS.ordinal)]
    )
    val appearanceMode: StateFlow<AppearanceMode> = _appearanceMode.asStateFlow()

    private val _displayMode = MutableStateFlow(
        DisplayMode.entries[prefs.getInt("display_mode", DisplayMode.NORMAL.ordinal)]
    )
    val displayMode: StateFlow<DisplayMode> = _displayMode.asStateFlow()

    private val _showGalleryTimestamp = MutableStateFlow(
        prefs.getBoolean("show_gallery_timestamp", false)
    )
    val showGalleryTimestamp: StateFlow<Boolean> = _showGalleryTimestamp.asStateFlow()

    private val _stampTimestamp = MutableStateFlow(
        prefs.getBoolean("stamp_timestamp", true)
    )
    val stampTimestamp: StateFlow<Boolean> = _stampTimestamp.asStateFlow()

    private val _galleryColumns = MutableStateFlow(
        prefs.getInt("gallery_columns", 3)
    )
    val galleryColumns: StateFlow<Int> = _galleryColumns.asStateFlow()

    // One setting shared by Gallery and Private, so the two can't disagree.
    // Stored by name, not ordinal, so reordering the enum can't change it.
    private val _gallerySort = MutableStateFlow(
        GallerySort.entries.firstOrNull { it.name == prefs.getString("gallery_sort", null) }
            ?: GallerySort.RECENTLY_ADDED
    )
    val gallerySort: StateFlow<GallerySort> = _gallerySort.asStateFlow()

    private val _shutterSoundId = MutableStateFlow(
        prefs.getString("shutter_sound_id", ShutterSound.CAT.id) ?: ShutterSound.CAT.id
    )
    val shutterSoundId: StateFlow<String> = _shutterSoundId.asStateFlow()

    // Script text shown in YAPPING display mode, so the user's typed/pasted script
    // survives across recordings and app restarts rather than resetting every take.
    private val _yappingScript = MutableStateFlow(
        prefs.getString("yapping_script", "") ?: ""
    )
    val yappingScript: StateFlow<String> = _yappingScript.asStateFlow()

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

    fun toggleGalleryTimestamp() {
        _showGalleryTimestamp.value = !_showGalleryTimestamp.value
        prefs.edit().putBoolean("show_gallery_timestamp", _showGalleryTimestamp.value).apply()
    }

    fun toggleStampTimestamp() {
        _stampTimestamp.value = !_stampTimestamp.value
        prefs.edit().putBoolean("stamp_timestamp", _stampTimestamp.value).apply()
    }

    fun setGalleryColumns(columns: Int) {
        _galleryColumns.value = columns
        prefs.edit().putInt("gallery_columns", columns).apply()
    }

    fun setGallerySort(sort: GallerySort) {
        _gallerySort.value = sort
        prefs.edit().putString("gallery_sort", sort.name).apply()
    }

    fun setShutterSound(id: String) {
        _shutterSoundId.value = id
        prefs.edit().putString("shutter_sound_id", id).apply()
    }

    fun setYappingScript(text: String) {
        _yappingScript.value = text
        prefs.edit().putString("yapping_script", text).apply()
    }

    fun resetIntro() {
        getApplication<Application>().getSharedPreferences("bodycam_prefs", Context.MODE_PRIVATE)
            .edit().remove("hasSeenDisclaimer").apply()
    }
}
