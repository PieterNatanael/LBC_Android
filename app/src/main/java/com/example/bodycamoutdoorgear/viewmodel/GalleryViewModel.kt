package com.example.bodycamoutdoorgear.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bodycamoutdoorgear.data.AddedDateStore
import com.example.bodycamoutdoorgear.data.MediaImporter
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

    // SnapshotStateMap: Compose tracks per-key reads, so only the card whose
    // thumbnail just loaded recomposes — not the entire screen.
    val thumbnails: SnapshotStateMap<String, Bitmap> = mutableStateMapOf()
    val durations: SnapshotStateMap<String, Long> = mutableStateMapOf()

    // Which folder this instance lists — filesDir itself normally, or
    // Private Gallery's own subfolder for the instance shown there. Kept as
    // state on the ViewModel (not passed to loadVideos each call) because
    // deleteVideo() also calls loadVideos() afterward with no folder of its
    // own to hand it.
    private var folder: File = application.filesDir

    // Declared before init on purpose: init's first loadVideos() reads it.
    // Set from the screen once it has read the shared setting.
    private var sort: GallerySort = GallerySort.RECENTLY_ADDED

    init {
        loadVideos()
    }

    fun setFolder(folder: File) {
        this.folder = folder
        loadVideos()
    }

    fun setSort(sort: GallerySort) {
        if (this.sort == sort) return
        this.sort = sort
        loadVideos()
    }

    // (current, total) while a photo import is copying files; null otherwise.
    private val _importProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val importProgress: StateFlow<Pair<Int, Int>?> = _importProgress.asStateFlow()

    // Set only when something didn't make it; a clean import needs no
    // confirmation beyond the new items appearing in the grid.
    private val _importProblems = MutableStateFlow<MediaImporter.Result?>(null)
    val importProblems: StateFlow<MediaImporter.Result?> = _importProblems.asStateFlow()

    /** Copies the picked items into THIS gallery's folder, so choosing Import
     *  while inside Private lands the files in Private. Runs on the
     *  ViewModel's scope so leaving the screen mid-copy doesn't cancel it. */
    fun importMedia(uris: List<Uri>) {
        if (uris.isEmpty() || _importProgress.value != null) return
        val target = folder
        viewModelScope.launch {
            _importProgress.value = 0 to uris.size
            val result = MediaImporter.import(getApplication(), uris, target) { current, total ->
                _importProgress.value = current to total
            }
            _importProgress.value = null
            loadVideos()
            if (result.failed > 0 || result.noSpace > 0) _importProblems.value = result
        }
    }

    fun dismissImportProblems() {
        _importProblems.value = null
    }

    fun loadVideos() {
        // Read now, on the calling thread, so the listing below sorts by the
        // choice that was current when it was asked for.
        val sort = sort
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                // Only imports have an entry here; everything else falls back
                // to its own date, which for them IS the added date.
                val added = if (sort == GallerySort.RECENTLY_ADDED)
                    AddedDateStore.all(getApplication()) else emptyMap()
                folder.listFiles { f -> f.extension in setOf("mp4", "mov", "jpg", "jpeg", "png") }
                    ?.sortedWith(
                        // The name breaks ties so equal timestamps can't
                        // shuffle the grid between reloads.
                        compareByDescending<File> { added[it.name] ?: it.lastModified() }
                            .thenByDescending { it.name }
                    )
                    ?.map { VideoItem(it) }
                    ?: emptyList()
            }
            _videos.value = list
        }
    }

    fun loadThumbnail(videoPath: String) {
        if (thumbnails.containsKey(videoPath)) return
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                try {
                    if (File(videoPath).extension in setOf("jpg", "jpeg", "png")) {
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
            if (bmp != null) thumbnails[videoPath] = bmp
        }
    }

    private fun thumbnailSampleSize(width: Int, height: Int): Int {
        var size = 1
        while ((width / (size * 2)) >= THUMB_SIZE || (height / (size * 2)) >= THUMB_SIZE) size *= 2
        return size
    }

    fun loadDuration(videoPath: String) {
        if (durations.containsKey(videoPath)) return
        viewModelScope.launch {
            val ms = withContext(Dispatchers.IO) {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(videoPath)
                    val d = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    retriever.release()
                    d
                } catch (e: Exception) { 0L }
            }
            if (ms > 0) durations[videoPath] = ms
        }
    }

    fun deleteVideo(video: VideoItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                video.file.delete()
                AddedDateStore.remove(getApplication(), video.file.name)
            }
            thumbnails.remove(video.file.absolutePath)
            loadVideos()
        }
    }
}
