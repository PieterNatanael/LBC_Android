package com.example.bodycamoutdoorgear.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bodycamoutdoorgear.data.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val THUMB_SIZE = 512
    }

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    private val _thumbnails = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val thumbnails: StateFlow<Map<String, Bitmap>> = _thumbnails.asStateFlow()

    init {
        loadVideos()
    }

    fun loadVideos() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                val dir = getApplication<Application>().filesDir
                dir.listFiles { f -> f.extension == "mp4" || f.extension == "jpg" }
                    ?.sortedByDescending { it.lastModified() }
                    ?.map { VideoItem(it) }
                    ?: emptyList()
            }
            _videos.value = list
        }
    }

    fun loadThumbnail(videoPath: String) {
        if (_thumbnails.value.containsKey(videoPath)) return
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                try {
                    if (File(videoPath).extension == "jpg") {
                        // First pass: read dimensions only (no pixel decode)
                        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(videoPath, opts)
                        opts.inSampleSize = thumbnailSampleSize(opts.outWidth, opts.outHeight)
                        opts.inJustDecodeBounds = false
                        val bmp = BitmapFactory.decodeFile(videoPath, opts) ?: return@withContext null
                        val exif = ExifInterface(videoPath)
                        val rotation = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                            ExifInterface.ORIENTATION_ROTATE_90  -> 90f
                            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                            else -> 0f
                        }
                        if (rotation != 0f) {
                            val matrix = Matrix().apply { postRotate(rotation) }
                            Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                                .also { bmp.recycle() }
                        } else bmp
                    } else {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(videoPath)
                        val frame = if (android.os.Build.VERSION.SDK_INT >= 27) {
                            retriever.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, THUMB_SIZE, THUMB_SIZE * 9 / 16)
                        } else {
                            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        }
                        retriever.release()
                        frame
                    }
                } catch (e: Exception) {
                    null
                }
            }
            if (bmp != null) {
                _thumbnails.value = _thumbnails.value + (videoPath to bmp)
            }
        }
    }

    private fun thumbnailSampleSize(width: Int, height: Int): Int {
        var size = 1
        while ((width / (size * 2)) >= THUMB_SIZE || (height / (size * 2)) >= THUMB_SIZE) size *= 2
        return size
    }

    fun deleteVideo(video: VideoItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { video.file.delete() }
            _thumbnails.value = _thumbnails.value - video.file.absolutePath
            loadVideos()
        }
    }
}
