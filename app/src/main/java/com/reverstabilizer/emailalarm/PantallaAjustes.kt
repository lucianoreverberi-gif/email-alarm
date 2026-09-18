package com.reverstabilizer.emailalarm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Lo que se configura una vez y no hace falta ver cada dia: la suscripcion
 * y el boton de emergencia para cortar una alarma.
 */
@Composable
fun PantallaAjustes(
    suscripcion: Suscripcion.Estado,
    oferta: Suscripcion.Oferta?,
    onVolver: () -> Unit,
    onSuscribirse: () -> Unit,
    onGestionarSuscripcion: () -> Unit,
    onDetenerAlarma: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onVolver) { Text("← " + stringResource(R.string.action_back)) }
        }
        Text(
            text = stringResource(R.string.settings_title),
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Titulo(stringResource(R.string.section_subscription), Modifier.padding(top = 12.dp))
        Tarjeta {
            when (suscripcion) {
                Suscripcion.Estado.ACTIVA -> {
                    Text(stringResource(R.string.sub_active), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Ayuda(stringResource(R.string.sub_active_body))
                }
                else -> {
                    Text(
                        stringResource(
                            if (suscripcion == Suscripcion.Estado.VENCIDA) R.string.aviso_vencida_titulo
                            else R.string.sub_none
                        ),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = if (suscripcion == Suscripcion.Estado.VENCIDA) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (oferta == null) {
                        Ayuda(stringResource(R.string.sub_unavailable))
                    } else {
                        // Google exige mostrar prueba, precio, renovacion y como cancelar.
                        val dias = oferta.diasGratis
                        Text(
                            if (dias != null) stringResource(R.string.sub_trial_terms, dias, oferta.precioAnual)
                            else stringResource(R.string.sub_terms, oferta.precioAnual)
                        )
                        Button(
                            onClick = onSuscribirse,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                when {
                                    suscripcion == Suscripcion.Estado.VENCIDA -> stringResource(R.string.sub_renew)
                                    dias != null -> stringResource(R.string.sub_start_trial, dias)
                                    else -> stringResource(R.string.sub_subscribe)
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Google Play exige acceso claro a la gestion de la suscripcion desde
            // la app. Va siempre visible, y abre directo la de esta app.
            Column(Modifier.padding(top = 4.dp)) {
                TextButton(onClick = onGestionarSuscripcion) {
                    Text(stringResource(R.string.sub_manage), fontWeight = FontWeight.SemiBold)
                }
                Ayuda(stringResource(R.string.sub_manage_hint), Modifier.padding(start = 12.dp))
            }
        }

        Titulo(stringResource(R.string.section_alarm), Modifier.padding(top = 12.dp))
        Tarjeta {
            OutlinedButton(
                onClick = onDetenerAlarma,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) { Text(stringResource(R.string.action_stop_alarm)) }
            Ayuda(stringResource(R.string.stop_alarm_hint))
        }
    }
}
