package com.abhinavsirohi.kiwi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.abhinavsirohi.kiwi.core.navigation.KiwiApp
import com.abhinavsirohi.kiwi.ui.theme.KiwiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KiwiTheme {
                KiwiApp()
            }
        }
    }
}
