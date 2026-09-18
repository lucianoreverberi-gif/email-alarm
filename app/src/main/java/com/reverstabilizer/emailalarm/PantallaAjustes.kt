package com.reverstabilizer.emailalarm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.reverstabilizer.emailalarm.ui.theme.Aviso

/**
 * Lo que se configura una vez y no hace falta ver cada dia: la suscripcion
 * y el boton de emergencia para cortar una alarma.
 */
@Composable
fun PantallaAjustes(
    suscripcion: Suscripcion.Estado,
    planes: List<Suscripcion.Plan>,
    onVolver: () -> Unit,
    onComprar: (Suscripcion.Plan) -> Unit,
    onGestionarSuscripcion: () -> Unit,
    onDetenerAlarma: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            IconButton(onClick = onVolver) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Seccion(stringResource(R.string.section_subscription))
        Tarjeta {
            val activa = suscripcion == Suscripcion.Estado.ACTIVA
            val vencida = suscripcion == Suscripcion.Estado.VENCIDA
            Row(verticalAlignment = Alignment.CenterVertically) {
                Circulo(
                    Icons.Rounded.WorkspacePremium,
                    fondo = if (vencida) MaterialTheme.colorScheme.error.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primaryContainer,
                    tinte = if (vencida) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(
                        stringResource(
                            when {
                                activa -> R.string.sub_active
                                vencida -> R.string.aviso_vencida_titulo
                                else -> R.string.sub_none
                            }
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (vencida) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                    if (activa) Ayuda(stringResource(R.string.sub_active_body))
                }
            }

            if (!activa) {
                if (planes.isEmpty()) {
                    Text(
                        stringResource(R.string.sub_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = Aviso.texto
                    )
                } else {
                    SelectorDePlan(planes, vencida, onComprar)
                }
            }

            // Google Play exige acceso claro a la gestion de la suscripcion desde
            // la app. Va siempre visible, y abre directo la de esta app.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onGestionarSuscripcion)
                    .padding(vertical = 10.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.sub_manage),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Ayuda(stringResource(R.string.sub_manage_hint))
                }
                Icon(
                    Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Seccion(stringResource(R.string.section_alarm))
        Tarjeta {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Circulo(Icons.Rounded.NotificationsOff)
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(stringResource(R.string.action_stop_alarm), style = MaterialTheme.typography.titleMedium)
                    Ayuda(stringResource(R.string.stop_alarm_hint))
                }
            }
            OutlinedButton(
                onClick = onDetenerAlarma,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) { Text(stringResource(R.string.action_stop_alarm)) }
        }
    }
}

/**
 * Elegir plan: el anual viene marcado y dice cuanto se ahorra. Debajo, las
 * condiciones del plan elegido: Google exige mostrar la duracion de la
 * prueba, el precio, que se renueva sola y como cancelar.
 */
@Composable
private fun SelectorDePlan(
    planes: List<Suscripcion.Plan>,
    vencida: Boolean,
    onComprar: (Suscripcion.Plan) -> Unit
) {
    var elegido by remember(planes) {
        mutableStateOf(planes.firstOrNull { it.periodo == Suscripcion.Periodo.ANUAL } ?: planes.first())
    }
    val mensual = planes.firstOrNull { it.periodo == Suscripcion.Periodo.MENSUAL }
    val anual = planes.firstOrNull { it.periodo == Suscripcion.Periodo.ANUAL }
    val ahorro = if (mensual != null && anual != null) ahorroAnual(mensual.precioMicros, anual.precioMicros) else null

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
        planes.forEach { plan ->
            OpcionDePlan(
                plan = plan,
                seleccionado = plan == elegido,
                ahorro = if (plan.periodo == Suscripcion.Periodo.ANUAL) ahorro else null,
                onElegir = { elegido = plan }
            )
        }

        val precio = precioConPeriodo(elegido)
        val dias = elegido.diasGratis
        Ayuda(
            if (dias != null) stringResource(R.string.sub_trial_terms, dias, precio)
            else stringResource(R.string.sub_terms, precio)
        )
        Button(
            onClick = { onComprar(elegido) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Text(
                when {
                    vencida -> stringResource(R.string.sub_renew)
                    dias != null -> stringResource(R.string.sub_start_trial, dias)
                    else -> stringResource(R.string.sub_subscribe)
                },
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun OpcionDePlan(
    plan: Suscripcion.Plan,
    seleccionado: Boolean,
    ahorro: Int?,
    onElegir: () -> Unit
) {
    val borde = if (seleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (seleccionado) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surface
            )
            .border(if (seleccionado) 2.dp else 1.dp, borde, RoundedCornerShape(16.dp))
            .selectable(selected = seleccionado, onClick = onElegir, role = Role.RadioButton)
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        RadioButton(selected = seleccionado, onClick = null, modifier = Modifier.padding(horizontal = 6.dp))
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(
                        if (plan.periodo == Suscripcion.Periodo.ANUAL) R.string.plan_annual else R.string.plan_monthly
                    ),
                    style = MaterialTheme.typography.titleSmall
                )
                if (ahorro != null) {
                    Text(
                        stringResource(R.string.plan_save, ahorro),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                precioConPeriodo(plan),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        plan.diasGratis?.let { dias ->
            Text(
                stringResource(R.string.plan_trial, dias),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

/** "US$9.99 por año" / "US$1.99 por mes". */
@Composable
private fun precioConPeriodo(plan: Suscripcion.Plan): String = stringResource(
    if (plan.periodo == Suscripcion.Periodo.ANUAL) R.string.plan_price_year else R.string.plan_price_month,
    plan.precio
)
