package com.mediavault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.mediavault.app.ui.AppState
import com.mediavault.app.ui.MediaVaultApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val state = remember { AppState(applicationContext) }
            LaunchedEffect(Unit) { state.rescan() }
            MediaVaultApp(state)
        }
    }
}
