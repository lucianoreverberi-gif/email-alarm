package com.reverstabilizer.emailalarm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.reverstabilizer.emailalarm.ui.theme.EmailalarmTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permisos = mutableStateOf<List<EstadoDePermiso>>(emptyList())
    private val appsEscuchadas = mutableStateOf<Set<String>>(emptySet())
    private val oferta = mutableStateOf<Suscripcion.Oferta?>(null)

    private val pedirPermisoNotificaciones =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            permisos.value = Permisos.revisarTodos(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pedirNotificacionesSiHaceFalta()

        appsEscuchadas.value = Ajustes.appsEscuchadas(this)
        Suscripcion.inicializar(this)
        val instaladas = AppsDeCorreo.instaladas(this)

        setContent {
            EmailalarmTheme {
                val base = remember { BaseDeDatos.obtener(this) }
                val reglas by remember { base.reglaDao().observarTodas() }
                    .collectAsState(initial = emptyList())
                val detecciones by remember { base.deteccionDao().observarUltimas() }
                    .collectAsState(initial = emptyList())
                val suscripcion by Suscripcion.estadoFlujo.collectAsState()
                val scope = rememberCoroutineScope()

                var enAjustes by rememberSaveable { mutableStateOf(false) }
                // El boton "atras" del telefono vuelve de Ajustes a la principal.
                BackHandler(enabled = enAjustes) { enAjustes = false }

                // Si Play no pudo dar la oferta, se manda a Ajustes, que explica por que.
                val suscribirse: () -> Unit = {
                    val o = oferta.value
                    if (o != null) {
                        scope.launch { Suscripcion.comprar(this@MainActivity, o) }
                    } else {
                        enAjustes = true
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (enAjustes) {
                        PantallaAjustes(
                            suscripcion = suscripcion,
                            oferta = oferta.value,
                            onVolver = { enAjustes = false },
                            onSuscribirse = { suscribirse() },
                            onGestionarSuscripcion = { Suscripcion.abrirGestion(this@MainActivity) },
                            onDetenerAlarma = { Alarma.detener() },
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        PantallaPrincipal(
                            permisos = permisos.value,
                            appsInstaladas = instaladas,
                            appsEscuchadas = appsEscuchadas.value,
                            reglas = reglas,
                            detecciones = detecciones,
                            suscripcion = suscripcion,
                            onResolverPermiso = { permiso -> permiso.intent?.let { startActivity(it) } },
                            onCambiarApp = { paquete, activada ->
                                val nuevas = appsEscuchadas.value.toMutableSet().apply {
                                    if (activada) add(paquete) else remove(paquete)
                                }
                                appsEscuchadas.value = nuevas
                                Ajustes.guardarAppsEscuchadas(this@MainActivity, nuevas)
                            },
                            onGuardarRegla = { regla ->
                                scope.launch {
                                    val dao = base.reglaDao()
                                    if (regla.id == 0L) dao.guardar(regla) else dao.actualizar(regla)
                                }
                            },
                            onCambiarActiva = { regla, activa ->
                                scope.launch { base.reglaDao().actualizar(regla.copy(activa = activa)) }
                            },
                            onBorrarRegla = { regla -> scope.launch { base.reglaDao().borrar(regla) } },
                            onProbarAhora = { Alarma.probarAhora(this@MainActivity) },
                            onProbarDespues = {
                                Alarma.probarDespues(this@MainActivity)
                                Toast.makeText(
                                    this@MainActivity,
                                    R.string.test_alarm_scheduled,
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            onSuscribirse = { suscribirse() },
                            onBorrarHistorial = { scope.launch { base.deteccionDao().borrarTodo() } },
                            onAbrirAjustes = { enAjustes = true },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Se revisan al volver de Ajustes del sistema, asi la pantalla queda al dia sola.
        permisos.value = Permisos.revisarTodos(this)
        // Al volver de la hoja de pago o de la pantalla de suscripciones de
        // Play, el estado puede haber cambiado.
        lifecycleScope.launch {
            Suscripcion.verificar(this@MainActivity)
            oferta.value = Suscripcion.oferta(this@MainActivity)
        }
    }

    /** Desde Android 13 hace falta permiso para que la app muestre notificaciones propias. */
    private fun pedirNotificacionesSiHaceFalta() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val concedido = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!concedido) pedirPermisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
