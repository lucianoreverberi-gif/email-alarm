---
name: publicar-version-ios
description: Sacar una version nueva de iPhone, de un cambio de codigo a un build en TestFlight o enviado a la App Store. Usar cuando haya que compilar, subir, publicar o enviar a revision la app de iPhone de Email Alarm.
---

# Publicar una versión de iPhone

El repo es `C:\Users\lucia\OneDrive\Escritorio\email alarm`, rama **`feat/expo-ios-foundation`**. **La rama `master` está congelada y nunca se mergea.**

## 1. Verificar antes de compilar

El `tsc` de la raíz **no cubre `mobile/`**, que tiene su propio tsconfig. Un error de tipos ahí pasa el commit y aparece recién en EAS, veinte minutos después. Ya mató el build 27.

```
npm test                                          # 47 pruebas
npm run check                                     # raiz
cd mobile && npx.cmd tsc --noEmit                 # el que importa para la app
cd mobile && npx.cmd expo export --platform ios   # el comando exacto que corre EAS, tarda un minuto
```

- [ ] Si falla `inbox.test.ts` por `better-sqlite3` y `NODE_MODULE_VERSION`, es la versión de Node de la máquina: `npm rebuild better-sqlite3`.
- [ ] Si se cambió un string en español, cambiar también su clave en `shared/translations.en.json`. `tr()` busca por el texto en español exacto y deja la traducción huérfana si no.
- [ ] Si se agregó un tono, están los cuatro lugares: `shared/alarm-sounds.ts`, el mapa de `require()` de `mobile/src/AlarmTest.tsx`, la lista `sounds` de `mobile/app.json` y los nombres en inglés.
- [ ] Si el cambio toca la base, el **ALTER en Neon va ANTES del deploy** (al revés, todo da 503), y los `.sql` **no se aplican solos**.

## 2. Si el cambio toca el servidor

**Pushear no despliega.** Correr el skill `/verificar-deploy` antes de dar por hecho que la API en vivo cambió.

## 3. Compilar

**Usar expo.dev → Builds → "Build from GitHub"**, no el CLI. En Windows el CLI bloquea los `.ps1`, pide Apple ID + 2FA, y en "Select a Provider" el cursor arranca en una cuenta ajena.

- Platform **iOS**, Git ref **`feat/expo-ios-foundation`**, carpeta base `/mobile`.
- Perfil **`beta`** (deja el cobro apagado) o **`billing`** (lo enciende). Elegir a conciencia.
- Si se quiere envío automático, tildar "Automatically submit to stores" con perfil de submit `beta`.

Alternativa por consola, si hace falta:
`./scripts/eas.ps1 build --platform ios --profile beta --auto-submit-with-profile beta`
(el binario es **`npx eas-cli`**, no `npx eas`).

Si falla con *"App Store Connect has agreement updates that must be resolved"*: hay contratos pendientes y bloquean cualquier subida. **Los acepta Luciano.**

## 4. Que llegue a TestFlight — NO SALTEAR

**EAS sube el build pero NO lo asigna a ningún grupo.** Pasa siempre. Síntoma: el build figura "Ready to Submit" con la columna GROUPS vacía y al iPhone no le llega nada, sin ningún error.

App Store Connect → TestFlight → grupo **"Email Alarm - Pruebas internas"** → Builds → **+** → elegir **por fecha de subida** (el diálogo no muestra el número de build) → Add. Al quedar bien, el estado pasa a "Testing".

El grupo tiene un solo tester, con un **iPhone 8 Plus con iOS 16.7.16**.

## 5. Antes de enviar a revisión

- [ ] **Releer las notas al revisor enteras.** Decían "This version contains no in-app purchases" mucho después de que existiera el cobro. **Cada cambio de producto deja mentiras viejas en los textos de la tienda.**
- [ ] Capturas de revisión: Apple acepta **1242 × 2208** y rechaza 1125 × 2000, que es como llegan las imágenes reescaladas por un chat.
- [ ] **Renombrar un producto para arreglar una captura mal cargada no es una salida**: el ID y la duración son permanentes y la app elige por ID.
- [ ] Si hay que tocar nombre o subtítulo de la ficha, **están `disabled` mientras no exista una versión en *Prepare for Submission***. Crear la versión no envía ni publica nada.
- [ ] La versión en **Manually release**, para que aprobarla no publique sola.
- [ ] Verificar que el build que se envía trae lo que se cree que trae: ya se aprobó un build que no incluía un cambio escrito ese mismo día.

## 6. El envío y la publicación

Los dos clics —**enviar a revisión** y **Release This Version**— los aprieta **Luciano**. Decirle en una línea qué se envía y qué pasa después.

## 7. Después

- [ ] Anotar en la memoria del proyecto qué build salió y qué trae.
- [ ] **404 el primer día es propagación**, no un problema: preocuparse recién pasadas 24 horas.
- [ ] Lo que cambie del lanzamiento o del cobro va a `release/LANZAMIENTO-IOS.md`, que es el documento vivo.
