package com.mediavault.app.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.StrictMode
import com.mediavault.app.data.Emulators
import com.mediavault.app.data.GameSystem
import com.mediavault.app.data.MediaItem

/**
 * Gets a ROM into an emulator.
 *
 * Emulators disagree about how they want to be handed a game: RetroArch takes an absolute
 * path plus a core through its own activity, most standalone emulators take an ordinary
 * ACTION_VIEW, and a few of the older ones only accept a `file://` uri. This tries the
 * routes in that order and reports which one worked.
 */
object RomLauncher {

    sealed interface Result {
        data class Launched(val packageName: String) : Result
        /** No emulator chosen and more than one could do the job. */
        data class NeedsChoice(val system: GameSystem) : Result
        data class Failed(val reason: String) : Result
    }

    fun launch(context: Context, rom: MediaItem, preferredPackage: String?): Result {
        val system = rom.systemId?.let { id -> Emulators.systems.firstOrNull { it.id == id } }
            ?: return Result.Failed("Unknown system for ${rom.title}")

        val candidates = Emulators.candidatesFor(context, system)
        // One emulator installed for this console: no point asking which.
        val target = preferredPackage ?: candidates.singleOrNull()?.packageName

        if (target == null) {
            return if (candidates.isEmpty()) {
                if (openWithChooser(context, rom, system)) Result.Launched("")
                else Result.Failed("No emulator on this phone can open a ${system.label} ROM")
            } else {
                Result.NeedsChoice(system)
            }
        }

        return if (launchWith(context, rom, system, target)) Result.Launched(target)
        else Result.Failed("Couldn't hand ${rom.title} to that emulator")
    }

    fun launchWith(context: Context, rom: MediaItem, system: GameSystem, packageName: String): Boolean {
        if (Emulators.isRetroArch(packageName) && rom.filePath != null) {
            if (launchRetroArch(context, rom.filePath, system, packageName)) return true
        }
        val contentUri = Launch.contentUriFor(context, rom.uri ?: return false) ?: return false
        if (viewWithPackage(context, contentUri, packageName)) return true
        // Older emulators refuse content uris and want the raw path.
        val path = rom.filePath
        if (path != null && viewFileUri(context, path, packageName)) return true
        return false
    }

    /** RetroArch's documented intent: absolute ROM path plus the core to run it with. */
    private fun launchRetroArch(
        context: Context,
        path: String,
        system: GameSystem,
        packageName: String,
    ): Boolean {
        val core = Emulators.retroCorePath(packageName, system) ?: return false
        val intent = Intent().apply {
            component = ComponentName(packageName, "com.retroarch.browser.retroactivity.RetroActivityFuture")
            putExtra("ROM", path)
            putExtra("LIBRETRO", core)
            putExtra("CONFIGFILE", "/data/data/$packageName/retroarch.cfg")
            putExtra("QUITFOCUS", "")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return start(context, intent)
    }

    private fun viewWithPackage(context: Context, uri: Uri, packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/octet-stream")
            setPackage(packageName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (start(context, intent)) return true
        val wildcard = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "*/*")
            setPackage(packageName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return start(context, wildcard)
    }

    /**
     * A `file://` uri normally trips StrictMode's exposure check. Several emulators accept
     * nothing else, so the check is relaxed for the duration of the hand-off — this app only
     * ever passes paths the user already granted it access to.
     */
    private fun viewFileUri(context: Context, path: String, packageName: String?): Boolean {
        val previous = StrictMode.getVmPolicy()
        return try {
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().build())
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(java.io.File(path)), "application/octet-stream")
                if (packageName != null) setPackage(packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            start(context, intent)
        } finally {
            StrictMode.setVmPolicy(previous)
        }
    }

    /** Nothing known is installed: let Android offer whatever can open the file. */
    private fun openWithChooser(context: Context, rom: MediaItem, system: GameSystem): Boolean {
        val uri = rom.uri?.let { Launch.contentUriFor(context, it) } ?: return false
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/octet-stream")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(view, "Open ${rom.title} (${system.label}) with")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return start(context, chooser)
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }
}
