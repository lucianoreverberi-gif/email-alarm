---
name: subir-build-android
description: Sacar una version nueva de Android, de un cambio de codigo a un .aab firmado y verificado y subido a Play. Usar cuando haya que publicar, subir una version, sacar un release o un .aab de Email Alarm Android.
---

# Subir una versión de Android

El orden importa y hay dos pasos que no se pueden saltar: **verificar el `.aab` por dentro** y **que el clic final lo apriete Luciano**.

## 1. Antes de compilar

- [ ] `gradlew test` pasa.
- [ ] **Subir `versionCode` y `versionName`** en `app/build.gradle.kts`. Play rechaza un `versionCode` repetido. (Un `versionCode` ya subido se reusa en la consola con **"Add from library"**, no recompilando.)
- [ ] Si se agregó un tono: el archivo empieza con `tono_` (si no, el reductor lo borra aunque esté en `Tonos.kt`).
- [ ] Si se cambió un texto en español, revisar que su clave siga existiendo donde corresponda.

## 2. Compilar y firmar

```
gradlew bundleRelease
powershell -ExecutionPolicy Bypass -File play\firmar.ps1
```

El script abre una ventana y **la contraseña la escribe Luciano**. No pedírsela, no pasarla por línea de comandos, no escribirla en ningún archivo ni repetirla en el chat. Si falla, el detalle está en `%TEMP%\firmar-email-alarm.log` (nunca contiene la contraseña).

Sale `app/build/outputs/bundle/release/email-alarm-firmado.aab`.

## 3. Verificar el artefacto — NO SALTEAR

Esto existe porque la **1.7 se publicó sin los tonos**: el reductor de recursos borra lo que no se referencia como `R.algo`, y los tonos y los recursos de Play Billing se piden por nombre. En debug no se reduce nada, así que la función se prueba bien y se publica rota.

```
unzip -l app/build/outputs/bundle/release/email-alarm-firmado.aab | grep -E "ogg|billingclient"
```

- [ ] Aparecen **los tonos** (`tono_*.ogg`).
- [ ] Aparecen los recursos de **Play Billing** (`com_android_billingclient_*`).
- [ ] Si falta algo, mirar `app/build/outputs/mapping/release/resources.txt`: **"is not reachable" = lo borró el reductor**. Se arregla agregándolo a `app/src/main/res/raw/keep.xml`, no desactivando el reductor.
- [ ] El certificado coincide con el de la 1.6, o Play lo rechaza:
      `keytool -printcert -jarfile ... -J-Duser.language=en` (en español tira `MissingFormatArgumentException`). SHA-256 esperado: empieza con `55:7E:29:75`.

Si el cambio es sensible, correr el agente **`revisor-de-release`** antes de seguir.

## 4. Subir a Play

Cuenta **`info@reverstabilizer.com`**, índice **`u/1`** — verificar en el selector de cuentas, porque Chrome cae seguido a la personal. La consola **no se maneja bien desde acá**: si las capturas dan timeout, no insistir; preparar todo y pasarle a Luciano la ruta de menús exacta.

- [ ] Elegir el canal: **Internal testing** (`4701653233530757388`) para que Luciano lo pruebe en su teléfono — llega en minutos, sin revisión, y se actualiza encima sin borrarle las alarmas. **Estar en Alpha no alcanza: la prueba interna tiene su propia inscripción**, y hay que abrir una vez en el teléfono `https://play.google.com/apps/internaltest/4701653233530757388`. Si no, Play Store nunca ofrece la actualización y **no da ningún error**.
- [ ] Notas de versión en los **4 idiomas** (en-US, es-419, es-ES, es-US). **No se editan en la página**: hay que abrir el diálogo "Edit release details" / "Add release notes"; el textarea que se ve en el DOM sin abrir el diálogo está oculto y escribirle no hace nada.
- [ ] Si es a producción: **los países del canal de producción son una lista aparte** de los de prueba. Si dice "No countries or regions have been selected for this track", hay que seleccionarlos ahí.
- [ ] **No tocar el canal Alpha**: tiene que seguir activo.

## 5. El envío

Dos pasos: el botón azul **"Submit N changes for review"** abre un diálogo y recién **"Send changes for review"** confirma. El canal usa *managed publishing off*, así que no hay "Start rollout" en la página de la release: el envío se hace desde `.../publishing`.

**Ese clic lo aprieta Luciano.** Decirle qué va a enviar, en una línea.

## 6. Después

- [ ] Anotar en la memoria del proyecto qué versión salió y qué trae.
- [ ] Si hay algo que solo él puede cargar en la ficha, anotarlo en `play/ficha-de-play.md`.
