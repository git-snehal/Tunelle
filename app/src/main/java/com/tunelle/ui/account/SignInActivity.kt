package com.tunelle.ui.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.tunelle.data.settings.AppSettings
import com.tunelle.ui.theme.TunelleTheme
import com.tunelle.ui.theme.SystemBars

/** Keeps account pages outside the app's player, navigation, and recorded glass layers. */
class SignInActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val theme by AppSettings.theme.collectAsState()
            TunelleTheme(theme) {
                SystemBars(MaterialTheme.colorScheme.background)
                LoginScreen(onDone = { finish() })
            }
        }
    }

    companion object {
        fun intent(context: Context): Intent = Intent(context, SignInActivity::class.java)
    }
}
