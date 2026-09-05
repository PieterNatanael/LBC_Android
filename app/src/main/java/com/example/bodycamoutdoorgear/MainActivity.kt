package com.example.bodycamoutdoorgear

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.bodycamoutdoorgear.manager.SubscriptionManager
import com.example.bodycamoutdoorgear.ui.disclaimer.DisclaimerScreen
import com.example.bodycamoutdoorgear.ui.navigation.AppNavigation
import com.example.bodycamoutdoorgear.ui.theme.LBCTheme
import com.example.bodycamoutdoorgear.viewmodel.CameraViewModel
import com.example.bodycamoutdoorgear.viewmodel.PhotoViewModel

class MainActivity : ComponentActivity() {

    private lateinit var subscriptionManager: SubscriptionManager

    private val cameraViewModel: CameraViewModel by viewModels()
    private val photoViewModel: PhotoViewModel   by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        subscriptionManager = SubscriptionManager(applicationContext)
        subscriptionManager.start()

        val prefs = getSharedPreferences("bodycam_prefs", MODE_PRIVATE)

        setContent {
            LBCTheme {
                var hasSeenDisclaimer by remember {
                    mutableStateOf(prefs.getBoolean("hasSeenDisclaimer", false))
                }

                if (!hasSeenDisclaimer) {
                    DisclaimerScreen(onDismiss = {
                        prefs.edit().putBoolean("hasSeenDisclaimer", true).apply()
                        hasSeenDisclaimer = true
                    })
                } else {
                    AppNavigation(
                        subscriptionManager = subscriptionManager,
                        onShowIntro = { hasSeenDisclaimer = false }
                    )
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            cameraViewModel.onHardwareButtonPressed()
            photoViewModel.onHardwareButtonPressed()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
