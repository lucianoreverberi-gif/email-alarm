package com.reverstabilizer.emailalarm

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

/**
 * La suscripcion anual, con la prueba gratis que maneja Google Play.
 *
 * La prueba NO se cuenta en la app: es una oferta configurada en Play Console
 * sobre el plan anual. Google garantiza una sola prueba por cuenta, cosa que
 * sin servidor no podriamos garantizar nosotros.
 *
 * Principio de todo este archivo: si no se puede confirmar el estado (sin red,
 * Play Store no disponible), vale el ultimo estado conocido. Un error nunca
 * pasa la suscripcion a "vencida": no se puede silenciar la alarma de alguien
 * que pago por un problema de conexion.
 */
object Suscripcion {

    /** Id del producto de suscripcion. Tiene que coincidir con el de Play Console. */
    const val PRODUCTO = "email_alarm_pro"

    enum class Estado { NUNCA, ACTIVA, VENCIDA }

    /** Lo que se le muestra a la persona para suscribirse, con precios de Play. */
    data class Oferta(
        val detalles: ProductDetails,
        val token: String,
        /** Precio anual ya formateado en la moneda del usuario, p. ej. "US$9.99". */
        val precioAnual: String,
        /** Dias de prueba gratis, o null si esta cuenta ya uso su prueba. */
        val diasGratis: Int?
    )

    private const val ARCHIVO = "suscripcion"
    private const val CLAVE_ACTIVA = "activa"
    private const val CLAVE_ALGUNA_VEZ = "alguna_vez"
    private const val CLAVE_DEMO = "demo_activa"

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val candado = Mutex()
    private var cliente: BillingClient? = null

    private val _estado = MutableStateFlow(Estado.NUNCA)
    val estadoFlujo: StateFlow<Estado> = _estado

    // --- Estado guardado (lo usa el listener, que no puede esperar a la red) ---

    fun estado(context: Context): Estado {
        val prefs = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
        // Solo en debug: permite simular una suscripcion activa para capturas y
        // pruebas antes de que exista el producto en Play Console. En release
        // BuildConfig.DEBUG es false y R8 elimina esta rama.
        if (BuildConfig.DEBUG && prefs.getBoolean(CLAVE_DEMO, false)) return Estado.ACTIVA
        return when {
            prefs.getBoolean(CLAVE_ACTIVA, false) -> Estado.ACTIVA
            prefs.getBoolean(CLAVE_ALGUNA_VEZ, false) -> Estado.VENCIDA
            else -> Estado.NUNCA
        }
    }

    fun estaActiva(context: Context): Boolean = estado(context) == Estado.ACTIVA

    /** Arranca la UI con el ultimo estado conocido, sin esperar a Google Play. */
    fun inicializar(context: Context) {
        _estado.value = estado(context)
    }

    /** Solo debug: ver [estado]. */
    fun simularActiva(context: Context, activa: Boolean) {
        if (!BuildConfig.DEBUG) return
        context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
            .edit().putBoolean(CLAVE_DEMO, activa).apply()
        _estado.value = estado(context)
    }

    private fun guardar(context: Context, activa: Boolean) {
        val anterior = estado(context)
        val prefs = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(CLAVE_ACTIVA, activa)
            .putBoolean(CLAVE_ALGUNA_VEZ, activa || prefs.getBoolean(CLAVE_ALGUNA_VEZ, false))
            .apply()
        val nuevo = estado(context)
        _estado.value = nuevo

        // Una suscripcion que se vence no puede dejar la alarma muda sin avisar.
        if (anterior == Estado.ACTIVA && nuevo == Estado.VENCIDA) {
            AvisoSuscripcion.suscripcionVencida(context)
        }
    }

    // --- Consultas a Google Play ---

    /**
     * Pregunta a Play si hay una suscripcion activa y guarda la respuesta.
     * Si Play no responde, devuelve el ultimo estado conocido sin cambiarlo.
     */
    suspend fun verificar(context: Context): Boolean {
        val ctx = context.applicationContext
        val c = cliente(ctx)
        if (!conectar(c)) return estaActiva(ctx)

        val resultado = c.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(ProductType.SUBS).build()
        )
        if (resultado.billingResult.responseCode != BillingResponseCode.OK) return estaActiva(ctx)

        return procesar(ctx, resultado.purchasesList)
    }

    /** La oferta a mostrar: la de prueba gratis si esta cuenta todavia puede usarla. */
    suspend fun oferta(context: Context): Oferta? {
        val c = cliente(context.applicationContext)
        if (!conectar(c)) return null

        val parametros = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCTO)
                        .setProductType(ProductType.SUBS)
                        .build()
                )
            )
            .build()

        val resultado = c.queryProductDetails(parametros)
        val detalles = resultado.productDetailsList?.firstOrNull() ?: return null
        val ofertas = detalles.subscriptionOfferDetails.orEmpty()
        if (ofertas.isEmpty()) return null

        // Google solo devuelve la oferta de prueba si esta cuenta es elegible.
        val conPrueba = ofertas.firstOrNull { o ->
            o.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
        }
        val elegida = conPrueba ?: ofertas.first()
        val fases = elegida.pricingPhases.pricingPhaseList
        val faseGratis = fases.firstOrNull { it.priceAmountMicros == 0L }

        return Oferta(
            detalles = detalles,
            token = elegida.offerToken,
            precioAnual = fases.last().formattedPrice,
            diasGratis = faseGratis?.let { diasDelPeriodo(it.billingPeriod) }
        )
    }

    /** Abre la hoja de pago de Google Play. El resultado llega al listener del cliente. */
    suspend fun comprar(activity: Activity, oferta: Oferta) {
        val c = cliente(activity.applicationContext)
        if (!conectar(c)) return
        val parametros = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(oferta.detalles)
                        .setOfferToken(oferta.token)
                        .build()
                )
            )
            .build()
        c.launchBillingFlow(activity, parametros)
    }

    /** Pantalla de Google Play para ver, cambiar o cancelar la suscripcion. */
    fun abrirGestion(context: Context) {
        val url = "https://play.google.com/store/account/subscriptions" +
            "?sku=$PRODUCTO&package=${context.packageName}"
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // --- Internos ---

    private fun cliente(context: Context): BillingClient =
        cliente ?: BillingClient.newBuilder(context)
            .setListener { resultado, compras ->
                if (resultado.responseCode == BillingResponseCode.OK && compras != null) {
                    alcance.launch { procesar(context, compras) }
                }
            }
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()
            .also { cliente = it }

    private suspend fun conectar(c: BillingClient): Boolean = candado.withLock {
        if (c.isReady) return@withLock true
        suspendCancellableCoroutine { continuacion ->
            c.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(resultado: BillingResult) {
                    if (continuacion.isActive) {
                        continuacion.resume(resultado.responseCode == BillingResponseCode.OK)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    if (continuacion.isActive) continuacion.resume(false)
                }
            })
        }
    }

    /**
     * Guarda si hay una compra activa del producto. Las compras nuevas hay que
     * "reconocerlas" antes de 3 dias, o Google las reembolsa automaticamente.
     */
    private suspend fun procesar(context: Context, compras: List<Purchase>): Boolean {
        val nuestras = compras.filter {
            PRODUCTO in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        val c = cliente(context)
        for (compra in nuestras.filter { !it.isAcknowledged }) {
            c.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(compra.purchaseToken)
                    .build()
            )
        }
        val activa = nuestras.isNotEmpty()
        guardar(context, activa)
        return activa
    }
}

/** "P30D" -> 30, "P4W" -> 28, "P1M" -> 30. Formato ISO 8601 que usa Play. */
internal fun diasDelPeriodo(periodo: String): Int? {
    val coincidencia = Regex("""P(\d+)([DWMY])""").matchEntire(periodo) ?: return null
    val n = coincidencia.groupValues[1].toInt()
    return when (coincidencia.groupValues[2]) {
        "D" -> n
        "W" -> n * 7
        "M" -> n * 30
        "Y" -> n * 365
        else -> null
    }
}
