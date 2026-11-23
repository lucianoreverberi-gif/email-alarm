# Email Alarm — traspaso completo

Estado al **23 de noviembre de 2026**. Escrito para que otro Claude pueda llevar el negocio sin volver a descubrir nada.
Dueño del proyecto: **Luciano Reverberi**. Trabaja en español, en Windows 11, con Android Studio y la app de escritorio de Claude.

---

## 1. Qué es el producto

Una app que **suena como un despertador cuando llega un correo que importa**. No es un cliente de correo: no reemplaza a Gmail ni a Outlook, se sienta al lado y grita. El usuario define reglas ("si escribe USCIS", "si el asunto dice *aprobado*") y cuando llega ese correo el teléfono suena a pantalla completa, aunque esté en silencio (Android).

**El público real, descubierto con usuarios, no inventado:** hispanohablantes en EE.UU. esperando algo que no puede esperar. Trámites de migración, registros de clientes nuevos, un cliente puntual que si no se le contesta en minutos se pierde. Casos documentados:

- **Sergio Ruiz** (5/11/2026, nota de voz no solicitada): la tiene puesta para un cliente puntual y le responde al instante.
- **Jorge Anis** (6/11/2026): la usa para avisos de registros de clientes nuevos; le sonó a las 4 de la mañana y de ahí salió el arreglo del horario.
- **Emilse** (oct 2026): "no me suena" — tenía apagadas las notificaciones de Gmail y la app no daba ninguna pista. De ahí salió la 1.7.
- **Luciano**: ventas, clientes y su abogada de migraciones.

Idioma del producto: **español neutro con "tú"**, no voseo, en app, sitio, política y fichas. Decidido porque el público hispano de EE.UU. es mayormente mexicano y centroamericano.

**Precio:** US$1.99 al mes o US$9.99 al año, **7 días gratis en los dos planes**. Está vendiendo plata real en las dos tiendas.

---

## 2. Arquitectura y stack

Son **dos aplicaciones distintas que comparten marca y nada de código**, y la diferencia es de fondo: en Android no hay servidor, en iPhone sí.

### Android — lee notificaciones, nunca toca la cuenta

| | |
| --- | --- |
| Repo | `github.com/lucianoreverberi-gif/email-alarm`, rama `main`, local en `C:\Users\lucia\AndroidStudioProjects\emailalarm` |
| Lenguaje | Kotlin + Jetpack Compose (Material 3) |
| Paquete | `com.reverstabilizer.emailalarm` |
| Versión | **1.12 (versionCode 13)** |
| SDK | min 29, target y compile 37 |
| Datos | Room (local, en el teléfono) |
| Cobro | Google Play Billing |
| Servidor | **ninguno** |

**Cómo funciona:** `MailListener` es un `NotificationListenerService`. Lee las notificaciones de las apps de correo ya instaladas, compara remitente y asunto contra las reglas (`MotorDeReglas.kt`) y si coincide dispara `AlarmaActivity` a pantalla completa. **El correo nunca sale del teléfono y la app nunca pide acceso a la cuenta de nadie.** Eso es también el argumento de privacidad más fuerte que tiene el producto.

Archivos que importan: `MailListener.kt` (la entrada), `MotorDeReglas.kt` (la decisión), `AlarmPlayer.kt` + `Tonos.kt` (el sonido), `Suscripcion.kt` (Play Billing y el código de acceso), `Pausa.kt` + `PausaTile.kt` (pausa global y el mosaico de la cortina), `Horario.kt` + `SeccionDeHorario.kt`, `Registro.kt` (historial), `Permisos.kt`.

### iPhone — servidor en Vercel que mira la bandeja

| | |
| --- | --- |
| Repo | `github.com/lucianoreverberi-gif/email-alarm-ios` (privado), rama **`feat/expo-ios-foundation`**, local en `C:\Users\lucia\OneDrive\Escritorio\email alarm` |
| App | Expo 54 / React Native 0.81.5, TypeScript, carpeta `mobile/` |
| Build | **Expo EAS**, entrega por TestFlight y App Store |
| Bundle | `com.emailalarm.app` · Apple Team `5M5BJ7XP4V` · EAS project `83bf433a-3f12-419f-84fd-241f535c5679` |
| Cobro | `expo-iap` 3.4.13 + `@apple/app-store-server-library` |
| Servidor | Vercel, funciones en `api/`, lógica en `server/background/` |
| Base | **Neon Postgres** con Drizzle ORM |

**Cómo funciona:** el usuario conecta su **Gmail** (OAuth, scope `gmail.metadata`) o su **Outlook** (Microsoft Graph). El servidor recibe un aviso push (Gmail Pub/Sub, Graph subscriptions), mira **solo la bandeja de entrada**, evalúa la regla y manda una notificación al teléfono; la alarma suena cuando el usuario la abre. Mantenimiento diario por cron de Vercel a las **10:00 UTC** (`/api/background/maintenance`).

Piezas del servidor: `gmail-api.ts` y `gmail-worker.ts`, `graph.ts` (Outlook), `worker.ts`, `billing.ts` + `billing-policy.ts`, `settings.ts`, `webhook.ts`, `crypto.ts`, y los `.sql` de esquema. **Los `.sql` no se aplican solos: se corren a mano contra Neon, y el ALTER va siempre ANTES del deploy** (al revés, las rutas escriben una columna que no existe y todo da 503).

> **La rama `master` del repo de iOS está congelada en el 18/9 y 70 commits atrás. Nunca se mergea.**

### Las cuatro diferencias entre los dos motores, verificadas en el código

Esto no es cosmético: una página de ayuda unificada mandaba a usuarios de iPhone a perderse correos. Por eso la ayuda del sitio está **separada por plataforma**.

| | Android | iPhone |
| --- | --- | --- |
| **Tildes** | Las ignora (`Normalizer.NFD` + minúsculas): `migracion` encuentra "Migración" | **Cuentan**: solo hace `toLowerCase()` |
| **Remitente** | Solo ve lo que muestra la notificación, casi siempre el nombre | Compara contra nombre **y** dirección, sirve cualquiera |
| **Fuera del horario** | Manda un aviso **silencioso** (queda registro) | Devuelve `outside_schedule` y **no manda nada** |
| **Alcance** | Cualquier app de correo que notifique | **Solo la bandeja de entrada** de la cuenta conectada |

Además: remitente + palabra se combinan con **"O"** en Android (suena si se cumple cualquiera); iOS tiene `matchMode: "any" | "all"`.

Tonos: **6 en Android, 21 en iPhone** (todos sintetizados, generados por scripts en `mobile/scripts/`; iOS no permite usar los tonos del sistema y copiar los de Android sería distribuir material ajeno). 22,2 MB de audio en el paquete de iOS.

### Sitio web

Estático, en `web/` del repo de **Android**, desplegado por Vercel desde `main`. 19 páginas, bilingüe (inglés en la raíz, español en `/es/`). Único endpoint: `web/api/contacto.js`.

Estructura: home, `/android/`, `/ios/`, privacidad (una por plataforma), contacto, la guía de intención de búsqueda (`/ring-when-an-email-arrives/` y `/es/sonar-cuando-llega-un-email/`), y la ayuda: un selector corto en `/help/` y `/es/ayuda/` que lleva a **cuatro páginas por plataforma** (`/help/android/`, `/help/iphone/` y sus versiones en español). Estilos: `styles.css` + `studio.css` + `ayuda.css`. Verde de marca **#17A05A**, el del logo.

---

## 3. Estado de cada plataforma

### Google Play — **publicado y vendiendo**

- **1.11 en producción al 100% desde el 11/11/2026**, 178 países. **1.12 (versionCode 13) enviada el 12/11/2026**, rollout completo.
- Acceso a producción concedido el **9/11/2026**, después de un rechazo el **10/10/2026** ("testers were not engaged").
- Prueba cerrada **"Closed testing - Alpha"** sigue activa, y **no hay que tocarla**.
- **Trader status declarado en los dos niveles** (cuenta y app). El de la app es una pantalla aparte y era el que bloqueaba el envío.
- Nombre de desarrollador cambiado a **"Rever Labs"** el 11/11/2026, **quedó en revisión**: hasta que aprueben, el público ve "Luciano Reverberi".
- Suscripción `email_alarm_pro`, dos planes base (anual y mensual), los 4 productos activos en **175 países** — que son todos los que Google ofrece para cobrar. **Esto no es un pendiente, ya está revisado.**

Identificadores (cuenta **info@reverstabilizer.com**, índice `u/1` en el navegador):

| | |
| --- | --- |
| developer | `8353001221777264880` |
| app | `4973177533273596170` |
| track Alpha | `4698636776602936806` |
| track Internal testing | `4701653233530757388` |

La URL `.../tracks/closed-testing` **siempre da error**: el canal se llama Alpha y tiene id numérico. Se entra por `.../releases/overview`.

### App Store — **publicado y vendiendo**

- **Email Alarm salió en la App Store el 17/11/2026.** Build **1.0 (30)**, *Ready for Distribution*, **175 países**, con las dos suscripciones cobrando plata real.
- Ficha id **6814333983** → `https://apps.apple.com/app/id6814333983`.
- Apple aprobó el envío del 15/11 **sin un solo rechazo**.
- Suscripciones `com.emailalarm.app.monthly` y `com.emailalarm.app.yearly`, las dos *Approved*.
- **Borrador de la 1.1 abierto** el 19/11/2026 con dos cambios cargados y sin publicar: nombre en español `Email Alarm: Correo Urgente` (27 de 30 caracteres) y las **URL de soporte** apuntando a `/help/iphone/` y `/es/ayuda/iphone/`. **Nada de eso se ve hasta que se publique un build de la 1.1.**
- Paid Apps Agreement aceptado el 13/11, banco y W-9 activos.
- Período de gracia de facturación y Family Sharing **apagados** en App Store Connect; el servidor ya soporta el de gracia, se puede encender cuando se quiera.

### Verificación OAuth de Google y CASA — **la auditoría terminó, falta que Google apriete el botón**

Esta es la cadena más larga del proyecto y la que más plata y tiempo costó.

- Proyecto **`email-alarm-509118`**, número **989835751780**, cliente OAuth de tipo **iOS**.
- Scope **`gmail.metadata`**, que Google clasifica como **restricted**: exige verificación de marca, video, política de privacidad y **auditoría de seguridad externa (ADA-CASA)**.
- Enviado a Google el **29/9/2026**. El 10/10 Google confirmó que la política cumple y escribió que **CASA era el último paso**. Fecha límite que pusieron: **7 de enero de 2027**.
- **El hilo con Google va al Gmail personal de Luciano, no a support@.** El *Verification Center* de la consola **quedó desactualizado** y muestra como abierto un problema del 9/10 que ya se resolvió: **no creerle a esa pantalla, creerle al hilo de correo**.
- Laboratorio: **ValueMentor** (portal **Secusy / ADA**, `ada.secusy.ai`). Contactos: Jo Thomas (Customer Success) y Vishnu Pavithran (Associate Consultant). Plan **Advanced, US$599**, factura `1039B0EE-0093`.
- Resultado: **PASS en los 6 grupos de requisitos**, 48 de 48 del cuestionario, 20 de 20 casos de prueba del escaneo, SSL Labs **A+**. Los 8 hallazgos medios no hacen fallar ningún requisito.
- **Letter of Validation emitida el 20/11/2026 y presentada a Google por el laboratorio el 21/11/2026.** Certification ID **`dbb4407e`**, nivel **AL1 (Lab Tested – Lab Verified)**, **vence el 21/11/2027**.
- Seguimiento enviado a Google el **21/11/2026** al hilo de `api-oauth-dev-verification-reply@google.com`, con número de proyecto, client ID, Certification ID y la cita de su propia frase del 10/10.
- **No queda nada pendiente del lado nuestro en toda la cadena.** Falta que Google apruebe.

**Mientras no aprueben:** aplica el **OAuth user cap de 100 usuarios para toda la vida del proyecto**, que no se puede resetear. Iban 7 al 13/11/2026. **Outlook no tiene ningún tope.** El usuario 101 ve un error crudo de Google; el mensaje propio que lo explica y le ofrece Outlook **está escrito pero no viajó en el build 30**.

**La carta está en `C:\Users\lucia\Documents\email-alarm\CASA-AL1-Letter-of-Validation-2026-11-20.pdf`, fuera de los repos, y trae cláusula de confidencialidad: no se publica en el sitio ni se cita en público sin permiso de ValueMentor.**

### Cobro

| | Android | iPhone |
| --- | --- | --- |
| Estado | Cobrando desde la prueba cerrada | Cobrando desde el 15/11/2026, 16:04 UTC |
| Prueba de punta a punta | Hecha | Compra y restauración en sandbox, 14/11/2026 |
| Pendiente | Inscribirse a la comisión del 15% | **Una suscripción real con plata de verdad, por Outlook** |

Tres decisiones del cobro que no se deducen del código:

1. **Si no se puede verificar el derecho, la alarma suena.** La compuerta distingue una respuesta de una falla: si la consulta contesta "no hay suscriptor", no suena; si falla Apple, la red o la base, **suena**. Motivo: el worker descarta correo de más de cinco minutos, así que un error no atrasaba la alarma, la borraba. Despertar a quien no paga es un error de redondeo; no despertar a quien pagó es el producto roto.
2. **No hay usuarios gratis a quienes romper**: la base tiene una cuenta de Gmail y una de Outlook, las dos de prueba.
3. En iPhone el plan era 1.0 gratis y cobro en la 1.1, con los de la 1.0 gratis para siempre. **Eso ya no corre**: la 1.0 salió cobrando.

---

## 4. Decisiones tomadas y por qué

Están cerradas. Reabrirlas es perder tiempo.

| Decisión | Fecha | Por qué |
| --- | --- | --- |
| Remitente + palabra se combinan con **"O"**, no "Y" | 18/9/2026 | Luciano lo ve más seguro: prefiere que suene de más que de menos |
| **Español neutro con "tú"** | 18/9/2026 | El público hispano de EE.UU. es mayormente mexicano y centroamericano |
| Verde **#17A05A** (el del logo) | 18/9/2026 | Es la marca |
| **7 días gratis en los dos planes** | 18/9/2026 | Lo eligió él; la recomendación era prueba solo en el anual |
| **Cuenta personal declarada trader, vender en todo el mundo** | 9/11/2026 | Dijo que no le importa publicar su teléfono y dirección y que quiere lo más simple. **Se caen la sociedad, el D-U-N-S y la cuenta de organización: ninguna hace falta.** No volver a plantearlo |
| Lanzar iPhone **con los dos proveedores**, Gmail topeado y Outlook libre | 13/11/2026 | Revierte la idea de septiembre de lanzar solo con Outlook. No había razón para esperar a CASA |
| **No pedir el cambio de "email alarm" a mayúsculas** en la LoV | 20/11/2026 | Google empareja por número de proyecto, no por ese texto. Pedirlo solo agregaba una vuelta antes de emitir |
| Rojo **solo** si una alarma no va a sonar | 18/9/2026 | Rechazó una pantalla con "más letras rojas que un envase de veneno" |
| Tarjetas blancas **solo para alarmas** | 18/9/2026 | Las acciones (probar) van como sección con botones: cosas distintas no se ven iguales |
| **No abrir Instagram ni TikTok** por ahora | nov 2026 | Primero la ficha de la tienda, que es donde la gente decide. Una cuenta vacía resta |
| **La prueba de 7 días cobra sola el día 8** | nov 2026 | Es lo que hacen todas las apps; está manejado por las tiendas, no por la app |
| Ayuda **separada por plataforma**, sin comparar | nov 2026 | Los dos motores difieren en cosas verificables; mezclarlos hacía perder correos. Y "no explicar demasiado para evitar copias" |
| **Nada de material ajeno**: tonos sintetizados, remitentes de ejemplo inventados | nov 2026 | Los packs "royalty free" piden atribución o prohíben uso comercial; y las marcas reales (Chase, Amazon) salieron de las capturas |
| Las dos declaraciones legales de CASA las marca **Luciano** | nov 2026 | Dicen que las correcciones posteriores a la carta **pueden cobrarse**: tienen plata atada |

### Lo que se descartó y por qué, para no volver a proponerlo

- **Reenviar o conectar Gmail dentro de Outlook.** Microsoft cortó las cuentas conectadas de Outlook.com en julio de 2024. Agregar Gmail a la **app** de Outlook no sirve: el correo sigue viviendo en Google y Microsoft Graph no lo ve. **Probado en el teléfono el 13/11: no sonó nada.** El reenvío funciona técnicamente pero es mal negocio para una alarma: puede caer en Correo no deseado por SPF/DMARC y el servidor lee solo la bandeja de entrada — o sea, silencio sin error.
- **CASA AL2.** La página de Secusy sugiere que AL2 es obligatorio para scopes restringidos, pero su ejemplo es lectura y escritura completa de Gmail. El scope de Email Alarm es más angosto y **el nivel lo asigna Google**: la consola dice AL1.
- **Un botón de "soltar el código de acceso"** para que el revisor pudiera suscribirse. Entre soltarlo y pagar la persona queda sin app, y si el pago falla queda colgada. Se resolvió **ofreciendo los planes con el código puesto**.
- **Rollout gradual en la primera salida a producción.** Con Production inactiva, 10% significa que el 90% no puede instalar.

---

## 5. Problemas resueltos, y cómo

Las trampas valen más que los arreglos: cada una costó horas o plata.

### Android / Play

- **El reductor de recursos borró los tonos del .aab.** `optimization { enable = true }` saca todo recurso que no se referencia como `R.algo`, y los tonos se piden por nombre en runtime. **La 1.7 se publicó muda, y en debug no se reduce nada, así que la función se probó bien y salió rota.** Se arregla con `app/src/main/res/raw/keep.xml` (`tools:keep="@raw/tono_*"`). El mismo reductor borraba tres recursos internos de Play Billing, arreglado en la 1.10. **Lección: antes de subir, verificar el .aab, no el código** (`unzip -l ... | grep ogg`). El informe está en `app/build/outputs/mapping/release/resources.txt`: "is not reachable" = se borró. **La 1.9 y anteriores se publicaron sin los recursos de Billing: si aparece un reporte de pago fallido de esas versiones, esa es la causa probable.**
- **El formulario de acceso a producción no guarda lo que se edita.** El botón "Save" está deshabilitado y las respuestas viejas sobreviven en el servidor. Se reescribió todo el 5/11 y el 7/11 el servidor seguía teniendo el texto de octubre. **Hay que editar y apretar "Apply" en la misma sesión, y leer el valor real de cada textarea (`el.value` recorriendo el shadow DOM), no el contador de caracteres.**
- **Al reintentar un formulario rechazado, la pregunta que corresponde al motivo textual del rechazo se reescribe siempre**, aunque parezca aceptable. En octubre quedó sin tocar justo esa y el rechazo fue por ahí.
- **Las notas de versión no se editan en la página**: hay que abrir el diálogo "Edit release details". El textarea que se ve en el DOM sin abrir el diálogo está oculto y escribirle no hace nada.
- **Los países del canal de producción son una lista aparte** de los del canal de pruebas. El envío se trabó con "No countries or regions have been selected for this track" aunque Alpha ya tenía los 178.
- **Chrome pierde la sesión de `info@reverstabilizer.com` muy seguido** y cae a la cuenta personal (`u/0`). La buena es `u/1`. Verificar en el selector antes de dar por hecho que está logueado.
- **La consola de Play no se puede manejar desde acá**: se queda en el selector de cuenta, las capturas dan timeout y el árbol de accesibilidad vuelve vacío. **Lo que falta cargar a mano se anota en `play/ficha-de-play.md`.**
- **El .aab se firma aparte**, no hay signingConfig en Gradle: `jarsigner` con el keystore de `Documents\claves\`, alias `upload`. El certificado tiene que coincidir con el de la 1.6 o Play lo rechaza. **`jarsigner` pide la contraseña sin mostrar ni asteriscos**, y eso ya causó dos errores. Nunca pasarla en la línea de comandos.
- **Para que Luciano pruebe una versión en su teléfono: canal Internal testing, no Alpha.** Llega en minutos sin revisión y se actualiza encima sin borrarle las alarmas. **Estar en Alpha no alcanza**: la prueba interna tiene su propia inscripción, y hay que abrir una vez `https://play.google.com/apps/internaltest/4701653233530757388` en el teléfono. Si no, Play Store nunca ofrece la actualización y **no da ningún error**.
- **Tres "verificaciones" de Google que se confunden entre sí**: (1) la de la app y la marca para `gmail.metadata`; (2) la de identidad del desarrollador en Play Console, **hecha**; (3) la de la cuenta bancaria del perfil de pagos, que es solo para que Google **pague** — estaba anotada mal como pendiente y **ya está**.
- **Tres correos distintos en Play que se confunden**: la cuenta de Google dueña (`info@reverstabilizer.com`, solo para entrar), el *Contact email* privado por donde Google escribe (el Gmail personal), y el *Developer email address*, que es **público** y es `support@email-alarm.com`. Para cambiar el último hay que **verificar primero y guardar después**, y **el formulario no sobrevive a navegar**: abrir el buzón en otra pestaña.
- **Los textos de tester no los ve el público.** El "(Internal Beta)" y el aviso rojo los agrega Play solo en la página de instalación del tester.

### iPhone / Apple / Vercel

- **En `email-alarm-demo`, pushear no despliega.** La API había quedado desplegada en un commit del 29/9 aunque el endurecimiento para CASA estaba pusheado desde el 11/10. **Hay que forzar el deploy** (`npx.cmd vercel --prod`). Y **pegarle a `/health` no prueba nada**: el deploy anterior sigue contestando hasta que el alias se mueve — lo único que prueba que la API en vivo cambió es que el alias apunte al deploy nuevo.
- **El `tsc` de la raíz no cubre `mobile/`**, que tiene su propio tsconfig. El build 27 murió en EAS por un choque de nombres que el typecheck de la raíz no ve. **Todo cambio en `mobile/` se verifica con el typecheck de `mobile` y con `npx expo export --platform ios`**, que es el comando exacto que corre EAS.
- **EAS sube el build a App Store Connect pero no lo asigna a ningún grupo**, así que no aparece en TestFlight y el teléfono no recibe nada. Pasa siempre. Se arregla a mano: TestFlight → grupo "Email Alarm - Pruebas internas" → Builds → **+** → elegir **por fecha de subida** (el diálogo no muestra el número de build).
- **Builds de iOS: usar expo.dev → Builds → "Build from GitHub"**, no el CLI. En Windows la política de ejecución bloquea los `.ps1`, pide Apple ID + 2FA, y en "Select a Provider" **el cursor arranca en una cuenta que no es la de Luciano**.
- **Dos detalles de entorno que cuestan tiempo cada vez:** en PowerShell hay que escribir **`npx.cmd`** (no `npx`), y el binario de EAS es **`npx eas-cli`** (no `npx eas`). PowerShell tampoco acepta `&&`.
- **El permiso de notificaciones de iOS engaña.** iOS informa `granted` con cualquiera de los interruptores apagado. Pasó en el iPhone de Luciano: permiso concedido, *Pantalla bloqueada* apagada, **la alarma no sonó** y sonó recién al abrir la app, mientras Ajustes decía "Notificaciones activadas". Se arregló leyendo `allowsAlert`, `allowsSound`, `allowsDisplayOnLockScreen` y la autorización provisional, y nombrando cada interruptor apagado con su consecuencia.
- **Cambiar un texto en español rompe el inglés.** `tr()` busca por el texto en español exacto, así que al unificar el voseo a "tú" ocho claves quedaron huérfanas y un usuario con el teléfono en inglés veía esos ocho textos en español. **Cada vez que se cambia un string en español hay que cambiar también su clave en `shared/translations.en.json`.**
- **Agregar un tono toca cuatro lugares** y hay pruebas que lo atajan: `shared/alarm-sounds.ts`, el mapa de `require()` de `mobile/src/AlarmTest.tsx` (Metro resuelve `require` en compilación, no se puede iterar), la lista `sounds` de `mobile/app.json` y los nombres en `shared/translations.en.json`.
- **Antes de cada envío hay que releer las notas al revisor enteras.** Seguían diciendo "This version contains no in-app purchases", cierto cuando se escribió y falso desde que existe el cobro. **Cada cambio de producto deja mentiras viejas en los textos de la tienda.**
- **Apple rechaza las capturas de revisión por dimensiones**: acepta 1242 × 2208 y rechaza 1125 × 2000, que es como llegan las imágenes reescaladas por un chat. **Renombrar un producto para arreglar una captura mal cargada no es salida**: el ID y la duración son permanentes.
- **El nombre y el subtítulo de la ficha están `disabled` mientras no exista una versión en *Prepare for Submission*.** Crear la versión no envía ni publica nada.
- **Nunca encender el registro antes que el cobro.** Estuvieron abiertas diez horas en esa combinación el 15/11. La exposición fue mínima, pero la lección queda: verificar las cuatro variables en una sola lectura antes de desplegar.
- **La alarma y el silencio del iPhone:** con la app abierta ignora el silencio (`playsInSilentMode: true`); con la pantalla bloqueada el sonido lo pone iOS y **respeta el silencio y los modos de Concentración**. Atravesarlos requiere **Critical Alerts** o **AlarmKit**, ninguno pedido todavía (anotado en `mobile/IOS-VALIDATION.md`).
- **404 el primer día en la App Store es propagación**, no un problema. Preocuparse recién pasadas 24 horas.

### Portal de CASA (Secusy) — sirven para la renovación de 2027

- **Escribir con el teclado en los campos largos borra espacios y saltos de línea** y guarda el texto pegoteado. Se carga bien con el setter nativo de React más un evento `input`. **Antes de guardar, leer el valor real del campo, no mirar la pantalla.**
- **Varias preguntas tienen DOS cajas de texto** (descripción y fragmento de código). Un script que toque `querySelector("textarea")` deja la segunda rota. **Recorrer siempre `querySelectorAll` y recargar antes de verificar.**
- **Type y Program no son campos de texto**: parecen una caja donde se escribe, pero escribir solo llena un buscador. **Hay que hacer clic en la opción** hasta que diga "1 selected". El formulario guarda igual sin ellos y el error aparece recién al enviar.
- **La sección *LoV details* solo existe en la vista "Guided"**, y el botón *Submit to Lab* aparece encendido pero está deshabilitado, con el motivo escondido en el tooltip.
- **Cuando la auditoría pasa a *Passed*, toda la sección de la LoV queda `disabled`, sin ningún cartel que lo diga.** Peor: se le puede escribir el valor desde el DOM, **la pantalla lo muestra cambiado y al recargar vuelve al anterior**, porque nunca se guardó. **Siempre recargar y releer antes de dar un cambio por hecho.** A esa altura cualquier corrección la hace el laboratorio.
- **El tablero muestra un porcentaje viejo y cacheado.** El número bueno está en la página de la aplicación.
- **La tarjeta "Automated Scan Summary" mostró "2 fail" en rojo hasta el final y la auditoría salió aprobada igual.** La que vale es la pestaña *Test cases*: Pass 20 / Fail 0.

### De esta máquina

- **Para leer un PDF: `pdftotext -layout`**, que viene con Git Bash en `/mingw64/bin`. El Read tool pide `pdftoppm`, que no está, y su error manda a instalar poppler: no hace falta.
- **El servidor de `python http.server` cachea de forma heurística** y sirvió `styles.css` viejo varias veces, haciendo parecer roto lo que estaba bien. Si algo "se ve sin estilos" en el panel de vista previa, descartar la caché primero.
- **Para verificar que un cambio está en vivo hay que buscar una cadena única de la versión nueva.** Una verificación con un texto que también existía en la página vieja dio un falso "ya está desplegado".

---

## 6. Pendientes, con el siguiente paso de cada uno

### De Luciano (nadie más puede hacerlos)

| Pendiente | Siguiente paso |
| --- | --- |
| **Suscribirse con plata real por Outlook** desde la app publicada | Es la única verificación de producción que nunca se hizo. Comprar, confirmar que la alarma suena, y después cancelar |
| **Sitio web de los datos de contacto de Play** → `https://www.email-alarm.com/help/android/` | Play Console → Crecimiento → Presencia en Play → Ficha principal → Datos de contacto. **Sale solo, sin esperar una versión nueva.** Play no tiene campo de soporte aparte como Apple |
| **Inscribirse a la comisión del 15%** en Play | Trámite de la consola |
| **Capturas nuevas del iPhone** | Las crudas muestran "REVER STABILIZER SYST…" en la barra de estado. Hay que recapturarlas en el teléfono |
| **Probar la otra mitad de la pausa en Android** | Que llegue un correo que coincida estando en pausa y confirmar que deja el aviso silencioso sin sonar |
| **Formulario de DSA / trader status de Apple** | Apple lo pide por separado del de Google. Verificar en App Store Connect si quedó hecho; el plazo es el 1 de febrero de 2027 |

### Técnicos

| Pendiente | Siguiente paso |
| --- | --- |
| **13 commits sin pushear** en el repo de iOS, rama `feat/expo-ios-foundation` | Incluyen el cierre de CASA en `server/CASA-EVIDENCIA.md` (commit `4b18d2e`). **Pushear solo con el OK de Luciano**, porque son varios y conviene que los vea |
| **Desplegar la API de iPhone** | `npx.cmd vercel --prod` desde el repo de iOS. **Pushear no despliega.** Verificar que el alias se movió, no que `/health` conteste |
| **El mensaje del usuario 101** | Está escrito pero no viajó en el build 30. Va en el build siguiente, con el chequeo de permisos de notificaciones |
| **Capturas de Android con la UI nueva** | Emulador `Medium_Phone_API_36.0` → build de debug → `play/capturas.sh` → `play/marketing.py`. Luciano eligió "no ahora" |
| **Resto de la propuesta de ASO** | Está escrita y medida en `ficha-de-tiendas-propuesta.md`: título y descripción corta de Play, subtítulo y keywords de Apple, en los dos idiomas. **Luciano eligió aplicar solo el nombre en español de Apple**; el resto sigue disponible |
| **Publicar la 1.1 de iPhone** | Es lo que hace visibles el nombre en español y las URL de soporte ya cargadas |

### Esperando a terceros

- **Google**: que apruebe la verificación de OAuth. Con eso **cae el tope de 100 usuarios de Gmail**. El laboratorio dijo "un par de días" el 21/11. **Si no contestan en una semana, insistir en el mismo hilo.** Puede que pidan algo chico (una captura, el video, un detalle de la política): todo ese material ya está armado.
- **Google Play**: que apruebe el nombre de desarrollador "Rever Labs".

---

## 7. Cuentas y servicios

**Sin contraseñas, sin claves, sin tokens. Nunca los pidas ni los escribas: los maneja Luciano.**

| Servicio | Para qué | Cuenta / identificador |
| --- | --- | --- |
| **Google Play Console** | Android | `info@reverstabilizer.com`, índice `u/1`. La cuenta personal es otra y está cerrada desde marzo de 2024 |
| **App Store Connect** | iPhone | Apple Team `5M5BJ7XP4V`, app id `6814333983` |
| **Expo / EAS** | Compilar iOS | owner `lucianoreverberis-team`, project `83bf433a-3f12-419f-84fd-241f535c5679` |
| **Google Cloud** | OAuth de Gmail, Pub/Sub | proyecto `email-alarm-509118`, número `989835751780` |
| **Vercel** | Sitio y API | `email-alarm` (`prj_MiOi5EblKX5Dl7In4IoU4e9hZQJV`) sirve el sitio desde el repo de Android, `main`. `email-alarm-demo` (`prj_d4uGL3Fno8wbyLxxwbCATetAcfqz`) es la API del iPhone, en `api.email-alarm.com`, desde el repo de iOS |
| **Neon** | Postgres de la API | tablas `ea_cloud_accounts`, `ea_gmail_accounts`, `ea_billing_*` |
| **Zoho Mail** (plan gratuito) | Buzón `support@email-alarm.com` | Se lee en mail.zoho.com o la app. El plan gratis no tiene IMAP ni POP |
| **Resend** | **Solo enviar** desde `send.email-alarm.com` | Nunca fue un buzón: listaba correos sin poder responder |
| **ValueMentor / Secusy (ADA)** | Auditoría CASA | `ada.secusy.ai`. Certification ID `dbb4407e` |
| **GitHub** | Los dos repos | `lucianoreverberi-gif/email-alarm` y `.../email-alarm-ios` (privado) |
| **Microsoft Graph** | Outlook en iPhone | Suscripciones de notificación |
| **YouTube** | Video de verificación, *no listado* | `https://youtu.be/hZ0QJWW4ZWk` es el que Google aceptó |

**Dominio:** `email-alarm.com` comprado en Vercel, DNS en `ns1/ns2.vercel-dns.com`. MX en Zoho, SPF `v=spf1 include:zohomail.com ~all`, DKIM selector `zoho`. **Hay un TXT de verificación de Search Console que no se borra nunca: si se va, se pierde la verificación del dominio para la pantalla de consentimiento de Google.** Los cambios de DNS por API están bloqueados para Claude; se hacen en el panel de Vercel.

**Qué NO está en este documento y nunca debe estar:** la contraseña del keystore de firma (vive solo en la cabeza de Luciano; el keystore está en `Documents\claves\`, fuera del repo), la clave `.p8` de Apple, el código de acceso de los revisores (en el repo va solo su hash), y el valor de cualquier variable marcada como Sensible en Vercel. **Claude no pide, no lee, no escribe y no repite ninguna de esas cosas.**

---

## 8. Fechas importantes

| Fecha | Qué |
| --- | --- |
| 17/9/2026 | Primer commit versionado del repo de Android |
| 18/9/2026 | Rediseño UX aprobado ("me encantó") y precios decididos |
| 29/9/2026 | Verificación de OAuth enviada a Google |
| 10/10/2026 | Play **rechaza** el acceso a producción. Google confirma que solo falta CASA |
| 31/10/2026 | 1.8 publicada en Alpha |
| 9/11/2026 | **Play concede el acceso a producción.** Trader status decidido |
| 11/11/2026 | **Android en producción al 100%**, 178 países. Nombre "Rever Labs" enviado a revisión |
| 12/11/2026 | 1.12 enviada a producción |
| 13/11/2026 | API mudada a `api.email-alarm.com`. Decisión de lanzar iPhone con los dos proveedores |
| 15/11/2026 | Cobro de iPhone encendido. App enviada a revisión de Apple. **CASA pagada** |
| 17/11/2026 | **Email Alarm se publica en la App Store.** Cuestionario de CASA enviado al laboratorio |
| 19/11/2026 | **CASA pasa a *Passed***. Borrador de la 1.1 de Apple |
| 20/11/2026 | **Letter of Validation emitida** |
| 21/11/2026 | **El laboratorio presenta la LoV a Google** y se le manda el seguimiento. Fin de la cadena CASA del lado nuestro |
| **1/2/2027** | **Plazo del trader status** (Google y Apple) |
| **7/1/2027** | Fecha límite que Google había puesto para CASA — **ya cumplida con 7 semanas de margen** |
| **21/11/2027** | **Vence la Letter of Validation.** Se renueva todos los años, con hasta 10% de descuento. **Arrancar en septiembre u octubre de 2027**: toda la evidencia y las respuestas del cuestionario ya están escritas |

---

## 9. Cómo trabajar con Luciano

Esto es tan importante como lo técnico.

- **Contestarle en español rioplatense, corto y concreto.** Se pierde en explicaciones largas y con jerga. Una respuesta de tres párrafos con lo que importa le sirve más que una de quince con todo.
- **Decirle exactamente dónde hacer clic** para lo que solo puede hacer él. No "configuralo en la consola": la ruta de menús completa.
- **Plantear los costos y las contras antes de implementar**, no después.
- **Él aprieta los botones que tienen consecuencias**: publicar, enviar a revisión, aceptar términos, marcar declaraciones legales, pagar, cargar datos de tarjeta, escribir contraseñas y códigos de verificación. **Claude nunca crea cuentas, nunca resuelve CAPTCHAs, nunca entra datos de pago y nunca maneja claves.**
- **Verificar antes de decir que algo está hecho.** Este proyecto tiene un historial de pantallas que mienten: formularios que no guardan, campos deshabilitados que aceptan escritura, deploys que no se desplegaron, cachés que sirven lo viejo. **Recargar y releer el valor real.** Y si una verificación salió mal, decirlo sin vueltas: ya pasó y lo que esperaba era eso, no una disculpa larga.
- **Le importa que se vea profesional.** Para él el diseño es parte del producto que vende. Antes de mostrarle una pantalla, mirarla con captura y preguntarse si tiene jerarquía, íconos, marca y color con significado.
- **No explicar demasiado en el material público**, para no facilitar copias.
- Las notas vivas están en la memoria de este proyecto (`.claude/projects/.../memory/`), y los documentos largos en `play/ficha-de-play.md`, `ficha-de-tiendas-propuesta.md` (repo de Android) y `release/LANZAMIENTO-IOS.md`, `server/CASA-EVIDENCIA.md` (repo de iOS).

---

## Resumen en cinco líneas

1. **Las dos apps están publicadas y cobrando plata real.** Android 1.12 en 178 países, iPhone 1.0 (30) en 175.
2. **CASA terminó**: PASS, carta emitida, presentada a Google. **Vence el 21/11/2027.**
3. **El único bloqueo que queda es que Google apruebe la verificación**, y con eso cae el tope de 100 usuarios de Gmail. Outlook nunca tuvo tope.
4. **Lo más valioso sin hacer no es técnico: es que nadie compró todavía con plata real por Outlook**, y que la ficha de las tiendas puede mejorar mucho (la propuesta está escrita y medida).
5. **Lo que puede doler:** un deploy que se da por hecho y no se desplegó, un formulario que se da por guardado y no guardó, o un recurso que el reductor borra del .aab. Verificar el artefacto, no el código.
