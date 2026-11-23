# Email Alarm — Android

App que **suena como un despertador cuando llega un correo que importa**. Publicada y cobrando.
Dueño: **Luciano Reverberi**. Se le contesta **en español, corto y concreto**.

**El contexto completo está en [TRASPASO.md](TRASPASO.md): leerlo antes de opinar sobre estado, decisiones o pendientes.** Acá va solo lo que hace falta para no romper nada.

---

## Los dos repos

| | |
| --- | --- |
| **Este** (`emailalarm`) | La app de Android (Kotlin + Compose) **y el sitio web** (`web/`, estático, lo despliega Vercel desde `main`) |
| **iPhone** | `C:\Users\lucia\OneDrive\Escritorio\email alarm`, rama **`feat/expo-ios-foundation`**. App Expo + API en Vercel. Tiene su propio CLAUDE.md |

Son apps distintas que no comparten código. **La rama `master` del repo de iOS está congelada y nunca se mergea.**

## Esta app

Paquete `com.reverstabilizer.emailalarm` · **1.12 (versionCode 13)** · minSdk 29, target 37 · Room · Play Billing · **sin servidor**.

`MailListener.kt` es un `NotificationListenerService`: lee las notificaciones de las apps de correo, compara contra las reglas en `MotorDeReglas.kt` y dispara `AlarmaActivity` a pantalla completa. **El correo nunca sale del teléfono.** Ese es el argumento de privacidad del producto: no romperlo.

## Comandos

```
gradlew bundleRelease                              # el .aab sin firmar
powershell -ExecutionPolicy Bypass -File play\firmar.ps1   # firmarlo (pide la contrasena en una ventana)
gradlew test                                       # pruebas
play/capturas.sh && play/marketing.py              # capturas de la ficha (necesita el emulador)
```

El bundle sale sin firmar (no hay `signingConfig` en Gradle). **La contraseña del keystore la escribe Luciano en la ventana del script: nunca pedirla, nunca pasarla por línea de comandos, nunca escribirla en un archivo.** El keystore vive en `Documents\claves\`, fuera del repo.

---

## Reglas que evitan daño real

1. **Verificar el artefacto, no el código.** El reductor de recursos (`optimization { enable = true }`) borra del `.aab` todo lo que no se referencia como `R.algo`. Ya publicó la **1.7 sin los tonos**, y en debug no se reduce nada, así que se probó bien y salió rota. `app/src/main/res/raw/keep.xml` los conserva. **Antes de subir: `unzip -l app/build/outputs/bundle/release/*.aab | grep -E "ogg|billingclient"`.** Usar el skill `/subir-build-android`.
2. **Las pantallas mienten.** El formulario de acceso a producción de Play no guarda lo que se edita (su "Save" está deshabilitado); las notas de versión solo se editan dentro del diálogo; el portal de CASA acepta escritura en campos `disabled` y revierte al recargar. **Siempre recargar y leer el valor real (`el.value`), no el contador ni la pantalla.**
3. **Los botones con consecuencias los aprieta Luciano**: publicar, enviar a revisión, aceptar términos, marcar declaraciones legales, pagar. Claude prepara todo y le dice **dónde hacer clic, con la ruta de menús completa**.
4. **Claude no maneja claves ni plata**: no crea cuentas, no resuelve CAPTCHAs, no carga datos de tarjeta, no lee variables marcadas como sensibles en Vercel, no escribe contraseñas ni códigos de verificación.
5. **La consola de Play no se puede manejar desde acá** (se queda en el selector de cuenta). Lo que hay que cargar a mano se anota en `play/ficha-de-play.md`. La cuenta buena es `info@reverstabilizer.com`, índice **`u/1`**: Chrome cae seguido a la personal (`u/0`).
6. **No tocar la prueba cerrada "Alpha"**: tiene que seguir activa.
7. **Nada de material ajeno**: tonos sintetizados, remitentes de ejemplo inventados, sin marcas reales en capturas.

## Decisiones cerradas — no reabrirlas

Remitente + palabra usan **"O"** (no "Y"). Español neutro con **"tú"**, no voseo. Verde **#17A05A**. **7 días gratis en los dos planes.** Cuenta personal declarada **trader**, sin sociedad ni D-U-N-S. Rojo **solo** si una alarma no va a sonar; tarjetas blancas **solo** para alarmas. **Reenviar o conectar Gmail dentro de Outlook no funciona** (probado en el teléfono: no suena) — no volver a proponerlo. El motivo de cada una está en TRASPASO.md.

## Cuando se cambia un texto

El sitio y la app son bilingües. En iOS, `tr()` busca **por el texto en español exacto**: cambiar un string en español sin cambiar su clave en `shared/translations.en.json` deja la traducción huérfana y el usuario en inglés ve español. Ya pasó con ocho textos.

## Herramientas de esta máquina

- Shell: **PowerShell** (no acepta `&&`; `npx` falla por la política de scripts → usar **`npx.cmd`**).
- PDFs: **`pdftotext -layout`**, que viene con Git Bash en `/mingw64/bin`. El Read tool pide `pdftoppm`, que no está.
- El servidor de `python http.server` **cachea**: si algo "se ve sin estilos" en la vista previa, descartar la caché antes de buscar el bug.
- Para dar un cambio por desplegado, buscar una **cadena única de la versión nueva**. Buscar un texto que también existía antes da falsos positivos.

## Skills de este proyecto

`/subir-build-android` · `/publicar-version-ios` · `/verificar-deploy` · y el agente `revisor-de-release` para la revisión del artefacto antes de publicar.
