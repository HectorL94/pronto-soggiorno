package app.prontosoggiorno

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import app.prontosoggiorno.notifications.AndroidStatusNotifier
import app.prontosoggiorno.ui.ProntoSoggiornoAppUi
import app.prontosoggiorno.ui.theme.ProntoSoggiornoTheme
import kotlinx.coroutines.flow.MutableStateFlow

// AppCompatActivity (y no ComponentActivity) para el selector de idioma por app en Android < 13.
class MainActivity : AppCompatActivity() {

    /** Pratica que pidió abrir una notificación. */
    private val openPratica = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Solo en el arranque real: al redibujar (p. ej. cambio de idioma) no se vuelve a abrir.
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            ProntoSoggiornoTheme {
                ProntoSoggiornoAppUi(openPratica = openPratica, onPraticaOpened = { openPratica.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val id = intent?.getLongExtra(AndroidStatusNotifier.EXTRA_PRATICA_ID, -1L) ?: -1L
        if (id > 0) openPratica.value = id
    }
}
