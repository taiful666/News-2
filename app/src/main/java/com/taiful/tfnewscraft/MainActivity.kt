package com.taiful.tfnewscraft

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.taiful.tfnewscraft.firebase.configureAppCheck
import com.taiful.tfnewscraft.ui.screens.TFStudioRoot
import com.taiful.tfnewscraft.viewmodel.StudioViewModel

class MainActivity : ComponentActivity() {

    private val studioViewModel: StudioViewModel by viewModels()
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI, ViewModel, or Firebase operations.
        configureAppCheck(this, intent)

        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* Foreground export service continues gracefully even if notification is denied */ }

        handleIncomingShareIntent(intent)

        // STEP 2: Render pure Jetpack Compose Material 3 UI
        setContent {
            TFStudioRoot(
                viewModel = studioViewModel,
                onRequestNotificationPermission = { requestNotificationPermissionIfNeeded() }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingShareIntent(intent)
    }

    private fun handleIncomingShareIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") return
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty().trim()
        val urlMatch = Regex("https?://\\S+").find(sharedText)?.value
        if (!urlMatch.isNullOrBlank()) {
            studioViewModel.importFromUrl(urlMatch)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
