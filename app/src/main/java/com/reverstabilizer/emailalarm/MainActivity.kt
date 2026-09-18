package com.reverstabilizer.emailalarm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.reverstabilizer.emailalarm.ui.theme.EmailalarmTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permisos = mutableStateOf<List<EstadoDePermiso>>(emptyList())
    private val appsEscuchadas = mutableStateOf<Set<String>>(emptySet())

    private val pedirPermisoNotificaciones =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            permisos.value = Permisos.revisarTodos(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pedirNotificacionesSiHaceFalta()

        appsEscuchadas.value = Ajustes.appsEscuchadas(this)
        val instaladas = AppsDeCorreo.instaladas(this)

        setContent {
            EmailalarmTheme {
                val dao = remember { BaseDeDatos.obtener(this).reglaDao() }
                val flujoDeReglas = remember { dao.observarTodas() }
                val reglas by flujoDeReglas.collectAsState(initial = emptyList())
                val scope = rememberCoroutineScope()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PantallaPrincipal(
                        permisos = permisos.value,
                        appsInstaladas = instaladas,
                        appsEscuchadas = appsEscuchadas.value,
                        reglas = reglas,
                        onResolverPermiso = { permiso ->
                            permiso.intent?.let { startActivity(it) }
                        },
                        onCambiarApp = { paquete, activada ->
                            val nuevas = appsEscuchadas.value.toMutableSet().apply {
                                if (activada) add(paquete) else remove(paquete)
                            }
                            appsEscuchadas.value = nuevas
                            Ajustes.guardarAppsEscuchadas(this@MainActivity, nuevas)
                        },
                        onGuardarRegla = { regla ->
                            scope.launch {
                                if (regla.id == 0L) dao.guardar(regla) else dao.actualizar(regla)
                            }
                        },
                        onCambiarActiva = { regla, activa ->
                            scope.launch { dao.actualizar(regla.copy(activa = activa)) }
                        },
                        onBorrarRegla = { regla -> scope.launch { dao.borrar(regla) } },
                        onDetenerAlarma = { Alarma.detener() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Se revisan al volver de Ajustes, asi la pantalla queda al dia sola.
        permisos.value = Permisos.revisarTodos(this)
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
