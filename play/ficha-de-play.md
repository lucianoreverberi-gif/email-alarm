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
| Compras dentro de la app | No |
| Política de privacidad | `https://<dominio>/privacy/` (fuente: `web/privacy/index.html`) |

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

PRIVATE BY DESIGN
Email Alarm has no internet permission: it physically cannot send your data anywhere. It doesn't log into your email account either — it only reads the notifications your email app already shows on your phone. No accounts, no ads, no analytics, no tracking.

GOOD TO KNOW
• Your email app must be installed on this phone with notifications turned on. If an account stops syncing, there is no notification to hear.
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

PRIVADA POR DISEÑO
Email Alarm no tiene permiso de internet: físicamente no puede mandar tus datos a ningún lado. Tampoco entra a tu cuenta de correo: solo lee las notificaciones que tu app de correo ya muestra en el teléfono. Sin cuentas, sin publicidad, sin analíticas, sin rastreo.

PARA TENER EN CUENTA
• Tu app de correo tiene que estar instalada en este teléfono y con las notificaciones activadas. Si una cuenta deja de sincronizar, no hay notificación que escuchar.
• Outlook y algunas otras apps solo muestran el nombre del remitente, no la dirección. Para esas, usá el nombre o una palabra clave: la app te avisa cuando una regla no puede coincidir.
• La app te guía por cada permiso y te muestra de un vistazo si la alarma está lista para sonar.
```

---

## Seguridad de los datos (Data Safety)

| Pregunta | Respuesta |
|---|---|
| ¿Tu app recopila o comparte alguno de los tipos de datos del usuario requeridos? | **No** |

Con ese "No", el resto del formulario no se muestra.

**Por qué "No" es correcto y no una picardía:** para Google, "recopilar" significa *transmitir datos fuera del dispositivo*. Los datos que se procesan solo en el teléfono y no se envían a ningún lado no cuentan como recopilados. La app no tiene permiso de internet y no hace copia de seguridad en la nube, así que no hay ningún camino por el que un dato salga del teléfono. Si algún día agregás algo que use internet (analíticas, informes de errores, un backend), **este formulario hay que cambiarlo antes de publicar esa versión**.

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
No login is required. To test: open the app, tap "Enable" next to "Read notifications" and allow Email Alarm. Create a rule with a keyword (for example "test"), then send an email containing that word to any Gmail account on the device. The alarm will ring and show full screen.
```

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
