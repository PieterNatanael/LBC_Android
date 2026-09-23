package com.example.bodycamoutdoorgear.privategallery

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Backs Private Gallery: a second folder under filesDir the normal Gallery
 * listing never scans, gated by a 4-digit PIN.
 *
 * A plain object rather than an AndroidViewModel/DI-injected instance —
 * there's no dependency-injection framework in this app, and every call
 * site (GalleryScreen, the photo/video viewers, the settings sheet) already
 * has a Context on hand via LocalContext.current, so each function just
 * takes one rather than this object holding a reference of its own (which
 * would risk leaking whichever Activity first constructed it).
 */
object PrivateGalleryStore {

    private const val CRYPTO_PREFS = "private_gallery_crypto"
    private const val PLAIN_PREFS = "private_gallery_state"
    private const val KEY_SALT_AND_HASH = "pin_salt_and_hash"
    private const val KEY_WRONG_ATTEMPTS = "wrong_attempts"
    private const val KEY_LOCKED_OUT_UNTIL = "locked_out_until"
    private const val KEY_WIPE_AFTER_3 = "wipe_after_3"

    // MARK: Folder

    /** Invisible to GalleryViewModel's own listing, which only reads
     *  filesDir directly — this is a subfolder, not a sibling. */
    fun folder(context: Context): File = File(context.filesDir, "Private")

    private fun ensureFolderExists(context: Context) {
        folder(context).mkdirs()
    }

    fun currentFiles(context: Context): List<File> {
        val exts = setOf("mp4", "mov", "jpg", "jpeg", "png")
        return folder(context).listFiles { f -> f.extension.lowercase() in exts }?.toList() ?: emptyList()
    }

    /** Picks a filename that doesn't already exist in `directory`, appending
     *  a numeric suffix if the original name is taken. Confirmed on the iOS
     *  side of this app: a file sent out of Private via HipHop, received
     *  back into the normal gallery under the same name, then Private
     *  turned off — moveOut's plain same-name destination already existed,
     *  the move failed, and that failure was going uncaught, either
     *  crashing deactivateKeepingFiles outright or (once caught) silently
     *  stranding the file inside Private forever, with no screen left that
     *  could ever reach it again once Private itself was deactivated. Used
     *  by both directions now, rather than assuming a same-named collision
     *  can't happen. */
    private fun availableDestination(file: File, directory: File): File {
        var candidate = File(directory, file.name)
        var suffix = 2
        while (candidate.exists()) {
            val ext = file.extension
            val base = file.nameWithoutExtension
            val name = if (ext.isEmpty()) "$base $suffix" else "$base $suffix.$ext"
            candidate = File(directory, name)
            suffix++
        }
        return candidate
    }

    /** Moves a file from the normal gallery into Private. Retries once
     *  after re-creating the folder if the move fails — confirmed on the
     *  iOS side of this app that the very FIRST move (the one call where
     *  the destination folder didn't already exist) could fail once and
     *  never again afterward; cheap insurance here even though Android's
     *  single-volume internal storage makes the same race less likely. */
    fun moveIn(context: Context, file: File): File {
        ensureFolderExists(context)
        val destination = availableDestination(file, folder(context))
        if (!file.renameTo(destination)) {
            ensureFolderExists(context)
            if (!file.renameTo(destination)) {
                throw java.io.IOException("Could not move ${file.name} into Private")
            }
        }
        return destination
    }

    /** One-directional counterpart, used by "Show in Gallery". */
    fun moveOut(context: Context, file: File): File {
        val destination = availableDestination(file, context.filesDir)
        if (!file.renameTo(destination)) {
            throw java.io.IOException("Could not move ${file.name} out of Private")
        }
        return destination
    }

    private fun deleteAllFiles(context: Context) {
        folder(context).listFiles()?.forEach { it.delete() }
    }

    // MARK: PIN storage
    //
    // The PIN itself is never stored, only a salted SHA-256 hash, kept in
    // EncryptedSharedPreferences (backed by the Android Keystore) rather
    // than plain SharedPreferences — the same reasoning as iOS keeping its
    // hash in the Keychain rather than UserDefaults.

    private fun cryptoPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context, CRYPTO_PREFS, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun plainPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PLAIN_PREFS, Context.MODE_PRIVATE)

    private fun hash(pin: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return digest.digest(pin.toByteArray(Charsets.UTF_8))
    }

    /** True exactly when a PIN exists — no separate on/off flag to drift
     *  out of sync with it, same as the iOS store. */
    fun isActive(context: Context): Boolean =
        cryptoPrefs(context).contains(KEY_SALT_AND_HASH)

    fun setPin(context: Context, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val digest = hash(pin, salt)
        val blob = android.util.Base64.encodeToString(salt + digest, android.util.Base64.NO_WRAP)
        cryptoPrefs(context).edit().putString(KEY_SALT_AND_HASH, blob).apply()
        resetAttempts(context)
    }

    // MARK: Verification & wrong-attempt handling

    private fun wrongAttempts(context: Context): Int = plainPrefs(context).getInt(KEY_WRONG_ATTEMPTS, 0)
    private fun setWrongAttempts(context: Context, value: Int) {
        plainPrefs(context).edit().putInt(KEY_WRONG_ATTEMPTS, value).apply()
    }
    private fun lockedOutUntil(context: Context): Long = plainPrefs(context).getLong(KEY_LOCKED_OUT_UNTIL, 0L)
    private fun setLockedOutUntil(context: Context, value: Long) {
        plainPrefs(context).edit().putLong(KEY_LOCKED_OUT_UNTIL, value).apply()
    }

    /** Off by default: a 4-digit PIN only has 10,000 combinations, so
     *  wiping on 3 wrong guesses turns one fumbled entry into a full,
     *  unrecoverable loss. There is no UI to turn this on yet. */
    fun wipeAfterThreeWrongAttempts(context: Context): Boolean =
        plainPrefs(context).getBoolean(KEY_WIPE_AFTER_3, false)
    fun setWipeAfterThreeWrongAttempts(context: Context, value: Boolean) {
        plainPrefs(context).edit().putBoolean(KEY_WIPE_AFTER_3, value).apply()
    }

    private fun resetAttempts(context: Context) {
        setWrongAttempts(context, 0)
        setLockedOutUntil(context, 0L)
    }

    /** Growing delay after repeated wrong guesses, independent of whether
     *  wipeAfterThreeWrongAttempts is on — this paces guessing, that one
     *  decides whether to delete. */
    private fun backoffMillis(forWrongAttempt: Int): Long? = when {
        forWrongAttempt < 3 -> null
        forWrongAttempt == 3 -> 5_000L
        forWrongAttempt == 4 -> 15_000L
        else -> 30_000L
    }

    sealed class VerifyResult {
        object Success : VerifyResult()
        object WrongPin : VerifyResult()
        data class LockedOut(val untilMillis: Long) : VerifyResult()
        object WipedAfterTooManyAttempts : VerifyResult()
    }

    fun verify(context: Context, pin: String): VerifyResult {
        val until = lockedOutUntil(context)
        if (until > System.currentTimeMillis()) return VerifyResult.LockedOut(until)

        val stored = cryptoPrefs(context).getString(KEY_SALT_AND_HASH, null)
            ?: return VerifyResult.WrongPin
        val blob = android.util.Base64.decode(stored, android.util.Base64.NO_WRAP)
        val salt = blob.copyOfRange(0, 16)
        val storedDigest = blob.copyOfRange(16, blob.size)
        val candidateDigest = hash(pin, salt)

        if (!candidateDigest.contentEquals(storedDigest)) {
            val attempts = wrongAttempts(context) + 1
            setWrongAttempts(context, attempts)
            if (wipeAfterThreeWrongAttempts(context) && attempts >= 3) {
                wipe(context)
                return VerifyResult.WipedAfterTooManyAttempts
            }
            backoffMillis(attempts)?.let { delay ->
                val untilMillis = System.currentTimeMillis() + delay
                setLockedOutUntil(context, untilMillis)
                return VerifyResult.LockedOut(untilMillis)
            }
            return VerifyResult.WrongPin
        }

        resetAttempts(context)
        return VerifyResult.Success
    }

    // MARK: Reset

    /** The "forgot my PIN" path — deletes every private file AND the PIN
     *  itself, fully resetting the feature to "never set up". No developer
     *  override exists; this is deliberately the only way back in without
     *  the correct PIN. */
    fun wipe(context: Context) {
        deleteAllFiles(context)
        cryptoPrefs(context).edit().remove(KEY_SALT_AND_HASH).apply()
        resetAttempts(context)
    }

    /** Turning the feature off with the CORRECT pin — files move back to
     *  the normal gallery rather than being deleted. moveOut throws on
     *  failure; caught per file (rather than left to crash the whole
     *  deactivation, which is what an uncaught throw here used to do) so
     *  one bad file can't block every other file from moving back — the
     *  same forgiving behavior the iOS build already has here. */
    fun deactivateKeepingFiles(context: Context) {
        currentFiles(context).forEach { runCatching { moveOut(context, it) } }
        cryptoPrefs(context).edit().remove(KEY_SALT_AND_HASH).apply()
        resetAttempts(context)
    }

    /** Self-healing safety net, called once at every launch: if Private
     *  Gallery is NOT active but its folder still holds files, moves them
     *  back to the normal gallery. Confirmed on the iOS side of this app
     *  that a same-name collision in moveOut (see availableDestination, now
     *  fixed) could leave a file stranded here even after
     *  deactivateKeepingFiles "succeeded" — and once Private is off, there
     *  is no UI left that can ever reach that folder again to notice or
     *  recover it manually. Anyone already affected before this fix existed
     *  gets their file back automatically the next time they open the app,
     *  with nothing for them to do. */
    fun recoverOrphanedFilesIfNeeded(context: Context) {
        if (isActive(context)) return
        currentFiles(context).forEach { runCatching { moveOut(context, it) } }
    }
}
