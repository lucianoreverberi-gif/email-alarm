# Ficha de las tiendas — propuesta de ASO

Qué dice hoy cada campo, qué propongo poner, y por qué. Nada de esto está
aplicado todavía: es para revisar y después cargar.

Las fuentes de verdad, cuando se apruebe:

- Play: [`play/ficha-de-play.md`](play/ficha-de-play.md), y de ahí a Play Console a mano.
- App Store: `release/app-store-copy.json` del repo del servidor, que se sube
  con `release/sync-app-store-copy.mjs --apply`.

---

## El problema de fondo

La búsqueda de las tiendas no entiende frases, entiende palabras. Hoy los
campos que más pesan están ocupados por eslóganes de marca en vez de por las
palabras que la gente escribe.

Dos cosas concretas:

1. **El nombre en la App Store usa 11 de 30 caracteres.** Dice solo
   "Email Alarm". El nombre es el campo que más pesa en el buscador de Apple,
   y hay 19 caracteres tirados. Play ya hace bien esto ("Email Alarm: Alerta
   Urgente"); el iPhone no.
2. **Faltan las palabras que se buscan de verdad.** En ningún campo aparecen
   *correo*, *mail*, *gmail*, *outlook*, *aviso* ni *sonido*. Sobran en cambio
   palabras que nadie escribe en un buscador, como *prioridad*.

Un detalle de cómo funciona Apple, porque decide todo lo de abajo: el nombre,
el subtítulo y las keywords son **una sola bolsa de palabras**. Repetir una
palabra en dos campos no suma nada, solo gasta lugar. Por eso abajo ninguna
palabra aparece dos veces.

---

## App Store

| Campo | Hoy | Propuesto | Largo |
|---|---|---|---|
| Nombre EN | `Email Alarm` | `Email Alarm: Urgent Inbox` | 25/30 |
| Subtítulo EN | `An alarm for important emails` | `Alerts from Gmail and Outlook` | 29/30 |
| Keywords EN | `inbox,urgent,alerts,reminder,notify,sender,subject,shifts,jobs,clients,schedule,priority` | `mail,notification,sound,sender,subject,shift,work,job,visa,immigration,client,reminder,wake,ring` | 96/100 |
| Nombre ES | `Email Alarm` | `Email Alarm: Correo Urgente` | 27/30 |
| Subtítulo ES | `Alarma para emails importantes` | `Avisos de Gmail y Outlook` | 25/30 |
| Keywords ES | `urgente,notificacion,recordatorio,despertador,bandeja,remitente,asunto,turnos,trabajo,prioridad` | `alerta,notificacion,sonido,despertador,remitente,asunto,turnos,inmigracion,trabajo,mail,bandeja` | 95/100 |

**Por qué el subtítulo nombra a Gmail y Outlook.** Son las dos palabras de más
volumen que alguien puede escribir buscando esta app, y en iPhone es cierto
literalmente: la app se conecta a esas dos cuentas. El riesgo es que son marcas
ajenas y Apple a veces las objeta. Es un riesgo chico y reversible —si lo
rechazan, se cambia el subtítulo y listo—, pero si preferís no correrlo:

- EN: `Alerts for the mail you expect` (30/30)
- ES: `Avisos del correo que esperas` (29/30)

Y en ese caso **agregar `gmail,outlook` a las keywords**, que no se ven.

**Lo que saqué y por qué.** `recordatorio` y `reminder` describen otra cosa (una
app de recordatorios), `prioridad` y `schedule` no los escribe nadie buscando
esto, y `urgent`, `alerts`, `inbox` y `urgente` pasaron al nombre, donde pesan
más. Entran en cambio `visa` e `inmigracion`, que son un caso de uso real y
tienen gente buscando con el problema puesto.

---

## Google Play

| Campo | Hoy | Propuesto | Largo |
|---|---|---|---|
| Título EN | `Email Alarm: Urgent Alert` | `Email Alarm: Loud Mail Alert` | 28/30 |
| Descripción corta EN | `An alarm for the emails that matter. Stop waiting - we'll tell you.` | `Rings like an alarm when that email arrives, even on silent. Gmail, Outlook.` | 76/80 |
| Título ES | `Email Alarm: Alerta Urgente` | `Email Alarm: Alarma de Correo` | 29/30 |
| Descripción corta ES | `Alarma para tus emails importantes. No los esperes más, nosotros te avisamos.` | `Suena como un despertador cuando llega ese correo, aunque esté en silencio.` | 75/80 |

La descripción corta es el único texto que el usuario lee antes de decidir, y
Play además la indexa. La de hoy es una frase de marca: linda, pero no dice
qué hace la app ni contiene lo que alguien busca. La propuesta dice las dos
cosas y mete *despertador*, *correo* y *silencio*.

**En Android "aunque esté en silencio" es verdad** —pantalla completa, sobre la
pantalla de bloqueo— y es lo único que ninguna otra app hace. Por eso va
adelante. **En iPhone no lo es**, y por eso no aparece en ningún campo de la
App Store: ahí la notificación respeta el silencio y el modo Concentración.

La descripción larga de Play no la toco. Está bien escrita y arranca fuerte.

---

## Cómo se aplica cada cosa

| | Cuándo se ve |
|---|---|
| Play: título y descripción corta | Se cargan en Play Console y salen solas, con una revisión de ficha. **Hoy mismo.** |
| App Store: nombre y subtítulo | Son parte de *App Information*. Se guardan ahora pero **salen con la próxima versión que subas.** |
| App Store: keywords | Son del número de versión. Mismo caso: **con la próxima versión.** |

O sea: lo de Android se puede hacer ya y lo del iPhone viaja con el próximo
build. No es motivo para esperar — se deja cargado y sale cuando salga.

---

## Lo que no es ASO pero mueve más

La ficha hace que te encuentren. Lo que decide si te instalan son **las
capturas**, porque es lo único que la mayoría mira. Las de Play de hoy están
bien; vale la pena revisar que la primera diga el problema y no el nombre del
producto. Eso lo vemos después de esto.
