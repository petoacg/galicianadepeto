package gal.galiciana.app

import android.graphics.Color
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import gal.galiciana.app.ui.GalicianaApp
import gal.galiciana.app.ui.theme.AppFrame
import gal.galiciana.app.ui.theme.GalicianaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        setTaskDescription(android.app.ActivityManager.TaskDescription(getString(R.string.app_name)))
        val recovered = gal.galiciana.app.data.ArchiveBackup.recoverPendingImport(this)
        if (!recovered) {
            setContent { GalicianaTheme { androidx.compose.material3.Surface {
                androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.fillMaxSize().padding(24.dp)) {
                    androidx.compose.material3.Text("No se pudo recuperar una importación anterior. Tus datos se conservan en una copia interna.")
                    androidx.compose.material3.Button(onClick = { recreate() }) { androidx.compose.material3.Text("Reintentar recuperación") }
                }
            } } }
            return
        }
        val bar = Color.TRANSPARENT
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(bar),
            navigationBarStyle = SystemBarStyle.dark(bar),
        )
        setContent {
            GalicianaTheme {
                AppFrame {
                    GalicianaApp()
                }
            }
        }
    }
}
