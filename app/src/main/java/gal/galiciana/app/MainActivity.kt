package gal.galiciana.app

import gal.galiciana.app.i18n.tr
import android.graphics.Color
import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
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
        gal.galiciana.app.i18n.Lang.load(this)
        @Suppress("DEPRECATION")
        setTaskDescription(android.app.ActivityManager.TaskDescription(getString(R.string.app_name)))
        // Android 15+ draws edge to edge regardless; set the bar style before any content.
        val bar = Color.TRANSPARENT
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(bar),
            navigationBarStyle = SystemBarStyle.dark(bar),
        )
        val recovered = gal.galiciana.app.data.ArchiveBackup.recoverPendingImport(this)
        if (!recovered) {
            setContent { GalicianaTheme { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(
                    Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                        .verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(tr("Non se puido recuperar unha importación anterior", "No se pudo recuperar una importación anterior"),
                        style = MaterialTheme.typography.titleLarge)
                    Text(tr("Os teus datos consérvanse nunha copia interna.", "Tus datos se conservan en una copia interna."),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { recreate() }, modifier = Modifier.padding(top = 8.dp)) {
                        Text(tr("Reintentar a recuperación", "Reintentar recuperación"))
                    }
                }
            } } }
            return
        }
        setContent {
            GalicianaTheme {
                AppFrame {
                    GalicianaApp()
                }
            }
        }
    }
}
