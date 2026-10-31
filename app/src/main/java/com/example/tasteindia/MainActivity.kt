package com.example.tasteindia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.tasteindia.ui.navigation.TasteIndiaNavHost
import com.example.tasteindia.ui.theme.TasteIndiaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as TasteIndiaApplication).container
        setContent {
            TasteIndiaTheme {
                TasteIndiaNavHost(container)
            }
        }
    }
}
