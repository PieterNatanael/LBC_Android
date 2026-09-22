package com.example.bodycamoutdoorgear.privategallery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Whether Private Gallery has been unlocked THIS APP SESSION — pure runtime
 * state, never persisted. PrivateGalleryStore.isActive answers "does a PIN
 * exist at all"; this answers "has the correct PIN already been entered
 * since the app was last backgrounded".
 *
 * Backed by Compose's own mutableStateOf rather than a StateFlow — read
 * directly from AppNavigation, which relocks it from a plain lifecycle
 * observer (not a ViewModel), so there's no natural owner for a StateFlow
 * here the way GalleryViewModel owns its own state.
 */
object PrivateGallerySession {
    var isUnlocked by mutableStateOf(false)
        private set

    fun unlock() {
        isUnlocked = true
    }

    /** Called unconditionally whenever the app leaves the foreground —
     *  see AppNavigation's ProcessLifecycleOwner observer. Cheap even when
     *  Private was never opened this session. */
    fun lock() {
        isUnlocked = false
    }
}
