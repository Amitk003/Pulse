package com.pulse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.pulse.app.ui.PulseApp
import com.pulse.app.ui.PulseViewModel
import com.pulse.app.ui.theme.PulseTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PulseViewModel by viewModels { PulseViewModel.factory(this) }

    /** Callback for the embedded arena result. Set by PulseApp before launching. */
    var arenaResultHandler: ((String?) -> Unit)? = null

    private val arenaLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val json = if (result.resultCode == RESULT_OK)
            result.data?.getStringExtra(UnityPlayerActivity.EXTRA_FIGHT_RESULT)
        else null
        arenaResultHandler?.invoke(json)
        arenaResultHandler = null
    }

    fun launchArena(intent: android.content.Intent, onResult: (String?) -> Unit) {
        arenaResultHandler = onResult
        arenaLauncher.launch(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw the warm background edge-to-edge so the notch area is not black.
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        // Dark status bar icons for the light warm background.
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true
        setContent {
            PulseTheme {
                PulseApp(viewModel = viewModel)
            }
        }
    }
}
