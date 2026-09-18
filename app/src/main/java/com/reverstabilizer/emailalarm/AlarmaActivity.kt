package com.reverstabilizer.emailalarm

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reverstabilizer.emailalarm.ui.theme.BlancoAlarma
import com.reverstabilizer.emailalarm.ui.theme.GrisAlarma
import com.reverstabilizer.emailalarm.ui.theme.NegroAlarma
import com.reverstabilizer.emailalarm.ui.theme.VerdeMarca

/**
 * La pantalla de alarma. Se muestra por encima del bloqueo y prende la pantalla.
 *
 * Que el sonido salga desde aca y no desde el servicio es lo que lo saca de la
 * categoria "reproduccion en segundo plano", que es la que Android 16 silencia.
 */
class AlarmaActivity : ComponentActivity() {

    private val remitente = mutableStateOf("")
    private val asunto = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        leerDatos(intent)
        // Idempotente: si ya venia sonando desde el servicio, no arranca otra.
        AlarmPlayer.sonar(this)

        setContent {
            PantallaDeAlarma(
                remitente = remitente.value,
                asunto = asunto.value,
                onDetener = { detenerYCerrar() }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        leerDatos(intent)
    }

    private fun leerDatos(intent: Intent) {
        remitente.value = intent.getStringExtra(EXTRA_REMITENTE).orEmpty()
        asunto.value = intent.getStringExtra(EXTRA_ASUNTO).orEmpty()
    }

    private fun detenerYCerrar() {
        Alarma.detener()
        finish()
    }
}

@Composable
private fun PantallaDeAlarma(
    remitente: String,
    asunto: String,
    onDetener: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NegroAlarma)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.padding(top = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = stringResource(R.string.alarm_banner),
                color = VerdeMarca,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )
            Text(
                text = remitente,
                color = BlancoAlarma,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 46.sp
            )
            Text(
                text = asunto,
                color = GrisAlarma,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                lineHeight = 27.sp
            )
        }

        Button(
            onClick = onDetener,
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VerdeMarca,
                contentColor = NegroAlarma
            )
        ) {
            Text(stringResource(R.string.action_stop), fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
    }
}
