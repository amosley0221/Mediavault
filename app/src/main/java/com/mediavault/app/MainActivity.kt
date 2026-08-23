package com.mediavault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import com.mediavault.app.ui.AppState
import com.mediavault.app.ui.MediaVaultApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var state: AppState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val appState = remember { AppState(applicationContext).also { state = it } }
            LaunchedEffect(Unit) { appState.rescan() }
            MediaVaultApp(appState)
        }
    }

    /** Coming back from the system permission screens should refill the library. */
    override fun onResume() {
        super.onResume()
        val current = state ?: return
        val before = Triple(current.hasMediaAccess, current.hasAllFilesAccess, current.hasPlaytimeAccess)
        current.refreshAccess()
        current.refreshPlaytimeAccess()
        val after = Triple(current.hasMediaAccess, current.hasAllFilesAccess, current.hasPlaytimeAccess)
        if (before != after) {
            lifecycleScope.launch { current.rescan() }
        }
    }
}
