# Ficha de Google Play — Email Alarm

Todo lo que hay que cargar en Play Console, listo para copiar.
Los límites de caracteres son los de Play; todos los textos están verificados dentro del límite.

---

## Datos generales

| Campo | Valor |
|---|---|
| Categoría | Productividad |
| Etiquetas sugeridas | Alarma, Correo electrónico, Productividad |
| Público objetivo | **18 años o más** — evita entrar en las políticas de Familias, que no aportan nada para esta app |
| Contiene anuncios | No |
| Compras dentro de la app | **Sí**: suscripción anual con 30 días gratis (ver abajo) |
| Política de privacidad | `https://email-alarm.vercel.app/privacy/` (fuente: `web/privacy/index.html`) |

---

## Ficha en inglés (idioma por defecto)

**Título** (máx. 30): 

```
Email Alarm
```

**Descripción corta** (máx. 80):

```
Rings like an alarm when the email you're waiting for arrives.
```

**Descripción completa** (máx. 4000):

```
Email Alarm rings like a real alarm when an important email arrives — even with your phone on silent, locked, or in Do Not Disturb.

Built for the emails you can't afford to see late:
• Shift sign-ups that go first come, first served
• A government or immigration email you're waiting for, without knowing when or from which address
• The customer reply that closes the deal

HOW IT WORKS
Create a rule with a sender, a keyword, or both. When a matching email arrives, Email Alarm takes over the screen and rings until you stop it.
• Sender: an email address or a name
• Keyword: searched in the subject and the message preview
• A rule rings if the sender OR the keyword matches. Better to ring once too often than to miss the one that matters.

Works with Gmail and Outlook, plus Samsung Email, Yahoo Mail, Proton Mail, BlueMail and K-9 Mail — across every account you have in those apps.

PRICING
Free for 30 days, then US$9.99 per year (or the equivalent in your currency). Cancel anytime in Google Play — if you cancel during the free month, you pay nothing.

PRIVATE BY DESIGN
Your emails never leave your phone. Email Alarm doesn't log into your email account: it only reads the notifications your email app already shows, and checks them right there on your device. Its only connection is to Google Play, to manage your subscription. No accounts, no ads, no tracking.

GOOD TO KNOW
• Your email app must be installed on this phone with notifications turned on. If an account stops syncing, there is no notification to hear.
• Emails that land in spam don't create a notification. For senders you don't know yet, create a filter on the Gmail website with a keyword and check "Never send it to Spam".
• Outlook and some other apps only show the sender's name, not the address. For those, use the name or a keyword in your rule — the app warns you when a rule can't match.
• The app walks you through each permission and shows at a glance whether the alarm is ready to ring.
```

---

## Ficha en español

**Título** (máx. 30):

```
Email Alarm
```

**Descripción corta** (máx. 80):

```
Suena como alarma cuando llega el correo que estás esperando.
```

**Descripción completa** (máx. 4000):

```
Email Alarm suena como una alarma de verdad cuando llega un correo importante, aunque el teléfono esté en silencio, bloqueado o en No Molestar.

Hecha para los correos que no podés ver tarde:
• Turnos de trabajo que se asignan por orden de llegada
• Un correo de migraciones o de un trámite que esperás sin saber cuándo ni desde qué dirección
• La respuesta del cliente que cierra la venta

CÓMO FUNCIONA
Creá una regla con un remitente, una palabra clave, o las dos. Cuando llega un correo que coincide, Email Alarm toma la pantalla y suena hasta que la detengas.
• Remitente: una dirección de correo o un nombre
• Palabra clave: se busca en el asunto y en el resumen del mensaje
• Una regla suena si coincide el remitente O la palabra clave. Mejor que suene de más a perderte el que importa.

Funciona con Gmail y Outlook, y también con Samsung Email, Yahoo Mail, Proton Mail, BlueMail y K-9 Mail, en todas las cuentas que tengas en esas apps.

PRECIO
30 días gratis, después US$9,99 por año (o el equivalente en tu moneda). Cancelás cuando quieras desde Google Play: si cancelás durante el mes gratis, no pagás nada.

PRIVADA POR DISEÑO
Tus correos nunca salen del teléfono. Email Alarm no entra a tu cuenta de correo: solo lee las notificaciones que tu app de correo ya muestra y las revisa ahí mismo, en el teléfono. Su única conexión es con Google Play, para administrar tu suscripción. Sin cuentas, sin publicidad, sin rastreo.

PARA TENER EN CUENTA
• Tu app de correo tiene que estar instalada en este teléfono y con las notificaciones activadas. Si una cuenta deja de sincronizar, no hay notificación que escuchar.
• Los correos que caen en spam no generan notificación. Para remitentes que todavía no conocés, creá un filtro en Gmail desde la web con una palabra clave y marcá "Nunca enviar a spam".
• Outlook y algunas otras apps solo muestran el nombre del remitente, no la dirección. Para esas, usá el nombre o una palabra clave: la app te avisa cuando una regla no puede coincidir.
• La app te guía por cada permiso y te muestra de un vistazo si la alarma está lista para sonar.
```

---

## Seguridad de los datos (Data Safety)

| Pregunta | Respuesta recomendada |
|---|---|
| ¿Tu app recopila o comparte alguno de los tipos de datos del usuario requeridos? | **No** |

Por qué, con las palabras exactas de Google
([Play Console Help, sección Data safety](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en)):

- **El contenido de los correos no se declara.** Google define *"'Collect' means transmitting data from your app off a user's device"*, y aclara que *"User data accessed by your app that is only processed locally on the user's device and not sent off device does not need to be disclosed."* La app revisa las notificaciones solo en el teléfono.
- **Los datos de pago no se declaran.** Google exime lo que recolecta un servicio de pagos como *"Google Play's billing system"* si *"Your app never accesses this information; and The payment service collects this information directly from the user, and collection is governed by that service's terms."* La app nunca ve la tarjeta: solo recibe si la suscripción está activa.

**La zona gris, para que decidas informado.** Google también exige declarar lo que recolecten las librerías de terceros incluidas en la app. La librería de pagos manda a Google estadísticas técnicas propias (por eso agrega el permiso de internet), y **Google no publica qué datos son**. Mi lectura es que quedan cubiertas por la exención del servicio de pagos, porque son parte del sistema de pagos de Google y se rigen por sus términos, pero no es una certeza. Si preferís ir a lo seguro, la alternativa es declarar *"Información y rendimiento de la app → Diagnósticos"*, recolectado, no compartido, con fin *"Funcionalidad de la app"*. Declarar de más está permitido; declarar de menos no.

Si algún día agregás algo que use internet por cuenta de la app (analíticas, informes de errores, un backend), **este formulario hay que revisarlo antes de publicar esa versión**.

---

## Declaraciones de permisos

### Alarma a pantalla completa (`USE_FULL_SCREEN_INTENT`)

Play Console pide declarar para qué se usa este permiso en apps que apuntan a Android 14+. Opción: **Alarmas**.

Justificación sugerida:

```
Email Alarm is an alarm app. Users create rules, and when an email matching one of them arrives, the app shows a full-screen alarm over the lock screen and rings until the user stops it — the same behavior as an alarm clock, triggered by an email instead of a time.
```

Si Google no lo aprueba como alarma, **la app sigue funcionando**: el permiso deja de darse solo y la app se lo pide al usuario desde la lista de permisos, que ya maneja ese caso. Lo único que cambia es un paso más en el onboarding.

### Acceso a notificaciones

No tiene formulario de declaración propio, pero el revisor lo mira. La descripción ya explica para qué se usa. En **Acceso a la app** (para revisores) conviene poner:

```
No login is required. The alarm needs an active subscription; reviewers can start the 30-day free trial from the main screen. To test: open the app, tap "Enable" next to "Read notifications" and allow Email Alarm. Create a rule with a keyword (for example "test"), then send an email containing that word to any Gmail account on the device. The alarm will ring and show full screen.
```

---

## Suscripción: cómo crearla en Play Console

**Requisito previo:** Google no deja crear suscripciones hasta que subas una versión de la app que incluya la librería de pagos. Primero se sube una versión a **Prueba interna**, después se crea esto.

En **Monetizar → Productos → Suscripciones → Crear suscripción**:

| Campo | Valor | Por qué |
|---|---|---|
| ID del producto | **`email_alarm_pro`** | **Tiene que ser exactamente este**: es el que busca el código (`Suscripcion.PRODUCTO`). No se puede cambiar después. |
| Nombre | Email Alarm Pro | Lo ve el usuario en Google Play |

Dentro, **Agregar plan base**:

| Campo | Valor |
|---|---|
| ID del plan base | `anual` |
| Tipo | Renovación automática |
| Período de facturación | 1 año |
| Precio | US$9.99 (Play convierte al resto de las monedas; se puede ajustar por país) |

Dentro del plan base, **Agregar oferta**:

| Campo | Valor |
|---|---|
| ID de la oferta | `prueba-30-dias` |
| Elegibilidad | Adquisición de clientes nuevos: *nunca tuvo esta suscripción* |
| Fase | Prueba gratuita, 30 días |

Activá el plan base y la oferta. El código elige la oferta solo: si la cuenta todavía puede usar la prueba, muestra "30 días gratis"; si ya la usó, muestra el precio directo.

**Para probar sin que te cobren:** en **Configuración → Pruebas de licencias**, agregá tu Gmail. Las compras de esa cuenta son de prueba y los plazos se aceleran: la prueba gratis dura minutos y el año se renueva en unos 30 minutos, así se puede ver el ciclo completo (prueba → cobro → cancelación → vencida) en una tarde.

---

## Clasificación de contenido

Completar el cuestionario como **app de utilidad / productividad**, respondiendo "No" a violencia, contenido sexual, lenguaje, sustancias, apuestas e interacción entre usuarios. Resultado esperado: apta para todo público.

---

## Assets gráficos

| Asset | Archivo | Requisito de Play |
|---|---|---|
| Ícono | `icono-play-512.png` | 512×512 PNG |
| Gráfico destacado (en) | `grafico-destacado-en.png` | 1024×500 |
| Gráfico destacado (es) | `grafico-destacado-es.png` | 1024×500 |
| Capturas de teléfono | *pendiente* | mínimo 2, recomendado 4 a 8 |

Para regenerar el ícono y los gráficos: `python play/generar_iconos.py` y después `bash play/renderizar.sh`.
