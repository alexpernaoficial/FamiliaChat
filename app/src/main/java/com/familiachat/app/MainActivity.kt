package com.familiachat.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.familiachat.app.data.FamilyRepository
import com.familiachat.app.data.LocalPrefs
import com.familiachat.app.ui.ChatScreen
import com.familiachat.app.ui.PairingScreen
import com.familiachat.app.ui.PinLockScreen
import com.familiachat.app.ui.PinSetupScreen
import com.familiachat.app.ui.theme.FamiliaChatTheme
import com.google.firebase.messaging.FirebaseMessaging

private enum class Screen { PAIRING, PIN_SETUP, PIN_LOCK, CHAT }

class MainActivity : ComponentActivity() {

    private val repository = FamilyRepository()
    private lateinit var prefs: LocalPrefs

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* ignorado: opcional */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = LocalPrefs(applicationContext)
        requestNotificationPermissionIfNeeded()

        setContent {
            FamiliaChatTheme {
                var screen by remember {
                    mutableStateOf(
                        when {
                            prefs.familyId == null -> Screen.PAIRING
                            !prefs.hasPin -> Screen.PIN_SETUP
                            else -> Screen.PIN_LOCK
                        }
                    )
                }

                when (screen) {
                    Screen.PAIRING -> PairingScreen(
                        repository = repository,
                        prefs = prefs,
                        onPaired = { screen = Screen.PIN_SETUP }
                    )

                    Screen.PIN_SETUP -> PinSetupScreen(
                        prefs = prefs,
                        onPinSet = {
                            registerFcmToken()
                            screen = Screen.CHAT
                        }
                    )

                    Screen.PIN_LOCK -> PinLockScreen(
                        prefs = prefs,
                        onUnlocked = {
                            registerFcmToken()
                            screen = Screen.CHAT
                        }
                    )

                    Screen.CHAT -> ChatScreen(repository = repository, prefs = prefs)
                }
            }
        }
    }

    /** Garante que o token de notificação deste aparelho está salvo na família (necessário pro push chegar). */
    private fun registerFcmToken() {
        val familyId = prefs.familyId ?: return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            repository.saveFcmToken(familyId, token)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
