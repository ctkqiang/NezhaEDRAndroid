package xin.ctkqiang.nezhaedr

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import xin.ctkqiang.nezhaedr.ui.main.MainScreen
import xin.ctkqiang.nezhaedr.ui.theme.NezhaEDRTheme
import xin.ctkqiang.nezhaedr.util.EDRLogger
import xin.ctkqiang.nezhaedr.util.LocaleHelper

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            EDRLogger.i(TAG, "Notification permission POST_NOTIFICATIONS granted by user")
        } else {
            EDRLogger.w(TAG, "Notification permission POST_NOTIFICATIONS denied by user")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val savedLang = LocaleHelper.getSavedLanguage(this)
        LocaleHelper.applyLanguage(this, savedLang)

        super.onCreate(savedInstanceState)
        EDRLogger.i(TAG, "onCreate: Activity initializing with locale '$savedLang'")
        enableEdgeToEdge()
        requestNotificationPermission()

        setContent {
            NezhaEDRTheme {
                MainScreen()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            if (status != PackageManager.PERMISSION_GRANTED) {
                EDRLogger.d(TAG, "Requesting POST_NOTIFICATIONS permission from system")
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                EDRLogger.d(TAG, "POST_NOTIFICATIONS permission already granted")
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
