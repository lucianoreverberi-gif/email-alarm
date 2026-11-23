---
name: verificar-deploy
description: Comprobar de verdad que un cambio quedo en vivo, en el sitio email-alarm.com o en la API api.email-alarm.com. Usar cuando haya que desplegar, verificar un deploy, o confirmar que algo ya esta publicado en el servidor.
---

# Verificar que algo está en vivo

Este skill existe porque en este proyecto **ya se dio por desplegado algo que no lo estaba**, dos veces y de dos formas distintas. La regla general: **un deploy no está verificado hasta que una cadena única de la versión nueva vuelve desde la dirección pública.**

## Los dos proyectos de Vercel, que se confunden

| Proyecto | Qué sirve | Se despliega |
| --- | --- | --- |
| **`email-alarm`** | El sitio: `email-alarm.com` y `www` | **Solo** desde el repo de Android, rama `main`. Auto-deploy al pushear |
| **`email-alarm-demo`** | La **API del iPhone**: `api.email-alarm.com` | Desde el repo de iOS, rama `feat/expo-ios-foundation`. **Pushear NO despliega: hay que forzarlo** |

## Si es el sitio (repo de Android)

1. Pushear a `main`.
2. Esperar el deploy y verificar con **una cadena que solo exista en la versión nueva**:

```
curl -s https://www.email-alarm.com/ruta/ | grep -q "<cadena nueva y unica>" && echo EN VIVO
```

**El error que ya se cometió:** verificar con un texto que también existía en la página vieja. Dio "EN VIVO" con la versión anterior todavía servida. **Elegir la cadena antes de pushear, y que no exista en el archivo viejo** (`git show HEAD~1:web/... | grep` para asegurarse).

3. Si una página se ve sin estilos en la vista previa local, **es la caché heurística de `python http.server`**, no el CSS. Descartarla antes de buscar el bug.
4. Si se agregaron páginas, actualizar `web/sitemap.xml` y los enlaces del pie de las demás.

## Si es la API (repo de iOS)

1. Desde `C:\Users\lucia\OneDrive\Escritorio\email alarm`:

```
npx.cmd vercel --prod
```

(`npx` sin `.cmd` falla por la política de scripts de PowerShell, y PowerShell no acepta `&&`.)

2. **Pegarle a `/health` no prueba nada.** El deploy anterior sigue contestando hasta que el alias se mueve. **Lo único que prueba que la API en vivo cambió es que el alias `api.email-alarm.com` apunte al deploy nuevo** — mirar el deployment id y confirmar que el alias es suyo, no solo que el deploy esté READY.

3. Comprobaciones mínimas después de mover el alias:
   - [ ] La raíz contesta **200** (está reescrita a `/api/background/health`).
   - [ ] Algo protegido contesta **401** sin sesión (prueba de que el código nuevo cargó).
   - [ ] Siguen los **9 headers de seguridad** del `vercel.json` (`curl -sI` y contarlos). Son requisito de CASA.

4. **Si el cambio toca la base:** el **ALTER en Neon va ANTES del deploy**. Al revés, las rutas escriben una columna que no existe y toda sincronización de ajustes devuelve 503. Los `.sql` de `server/background/` **no se aplican solos**.

5. **Si el cambio es de variables de entorno:** cada cambio necesita un deploy manual, no se toma solo. Y verificar **todas las variables relacionadas en una sola lectura** antes de desplegar: ya quedó el registro abierto diez horas con el cobro apagado, que era justo la combinación a evitar.

6. **No leer variables marcadas como sensibles.** Si hace falta una, pedírsela a Luciano o que la cargue él.

7. El alias viejo `email-alarm-demo.vercel.app` **no se da de baja** mientras pueda quedar algún teléfono con una build anterior a la 28: cada teléfono le habla a la dirección que estaba compilada cuando se instaló la app.

## Lo que hay que reportar al terminar

Una línea: qué se desplegó, a qué dirección, **y con qué comprobación concreta** (la cadena que volvió, el alias que se movió, el header que apareció). Si la verificación no se pudo hacer, decirlo así —no "debería estar andando".
