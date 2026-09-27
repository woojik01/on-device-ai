package com.woojik.ondeviceai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.woojik.ondeviceai.ui.nav.OnDeviceAiApp
import com.woojik.ondeviceai.ui.theme.OnDeviceAiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val locator = (application as OnDeviceAiApplication).locator
        setContent {
            OnDeviceAiTheme {
                OnDeviceAiApp(locator)
            }
        }
    }
}
