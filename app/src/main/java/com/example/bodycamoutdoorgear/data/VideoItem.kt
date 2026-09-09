package com.example.bodycamoutdoorgear.data

import java.io.File
import java.util.Date

data class VideoItem(val file: File) {
    val id: String get() = file.absolutePath
    val url: String get() = file.absolutePath
    val createdAt: Date get() = Date(file.lastModified())
    val name: String get() = file.name
    val isPhoto: Boolean get() = file.extension == "jpg"
}
