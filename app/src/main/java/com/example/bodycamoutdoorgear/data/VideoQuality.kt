package com.example.bodycamoutdoorgear.data

import androidx.camera.video.Quality

enum class VideoQuality(val label: String, val shortLabel: String, val cameraQuality: Quality) {
    LOW("LOW", "LOW", Quality.SD),
    MEDIUM("MEDIUM", "MED", Quality.HD),
    HIGH("HIGH", "HIGH", Quality.HIGHEST);
}
