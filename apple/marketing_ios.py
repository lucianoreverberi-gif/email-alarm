"""Capturas de marketing para la App Store: titular + la captura real del iPhone.

Uso (desde la raiz del proyecto):
    python apple/marketing_ios.py

Toma las capturas de apple/capturas-iphone/ y deja las imagenes en
apple/capturas-app-store/, 1290x2796 (el tamano de 6.9" que pide Apple y el
mismo que traen las capturas originales). Renderiza con Edge headless,
igual que play/marketing.py.

Cada pieza puede saltear franjas de la captura original ("saltos"): filas en
pixeles del JPG que no se muestran. Sirve para sacar los avisos pasajeros
("Alarm saved on your iPhone") o un elemento cortado arriba de todo; el corte
cae siempre sobre el fondo liso, asi que no se nota.
"""
import pathlib
import subprocess
import tempfile
import time

RAIZ = pathlib.Path(__file__).resolve().parent.parent
CAPTURAS = RAIZ / "apple" / "capturas-iphone"
SALIDA = RAIZ / "apple" / "capturas-app-store"
EDGE = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
ANCHO, ALTO = 1290, 2796
ANCHO_ORIGEN = 1290  # ancho de las capturas del iPhone

# (captura, titular con <em> en lo que se destaca, bajada, telefono completo?, saltos)
# El telefono completo se usa cuando lo importante esta abajo (el boton Stop alarm);
# si no, el telefono sale mas grande y cortado por el borde inferior.
TEXTOS = {
    "en": [
        ("lista", "Stop watching <em>your inbox</em>",
         "Set an alarm and get on with your day. When that email lands, it rings.",
         False, [(392, 548)]),
        ("alarma", "Rings like a <em>real alarm</em>",
         "Full screen, with the sender and the subject, until you stop it.",
         True, []),
        ("editar", "You choose <em>what matters</em>",
         "A sender, a keyword, or both. Type “uscis” and it also rings for “USCIS Online Account”.",
         False, []),
        ("horario", "Only when it <em>should</em> ring",
         "Pick the days and the hours of each alarm. Outside them, it stays quiet.",
         False, [(157, 414)]),
        ("actividad", "Know <em>why</em> it rang",
         "Every alert, with its sender and subject, saved on your iPhone.",
         False, []),
    ],
}

PLANTILLA = """<!DOCTYPE html>
<html><head><meta charset="utf-8"><style>
  * {{ margin: 0; box-sizing: border-box; }}
  html, body {{ width: {ancho}px; height: {alto}px; overflow: hidden; }}
  body {{
    font-family: "Segoe UI", Roboto, Arial, sans-serif;
    background:
      radial-gradient(1100px 850px at 85% 5%, rgba(255,255,255,.16), transparent 60%),
      linear-gradient(165deg, #1FB86C 0%, #17A05A 45%, #0B6E3C 100%);
    color: #fff; position: relative;
  }}
  .marca {{ position: absolute; top: 130px; left: 108px; display: flex; align-items: center; gap: 22px;
            font-size: 42px; font-weight: 600; letter-spacing: .3px; opacity: .95; }}
  .marca img {{ width: 74px; height: 74px; border-radius: 20px;
               box-shadow: 0 0 0 3px rgba(255,255,255,.85), 0 7px 19px rgba(0,0,0,.2); }}
  h1 {{ position: absolute; top: 270px; left: 108px; right: 108px;
        font-size: 108px; line-height: 1.04; font-weight: 800; letter-spacing: -2.4px; }}
  h1 em {{ font-style: normal; color: #C8F5DA; }}
  p {{ position: absolute; left: 108px; right: 130px; font-size: 47px; line-height: 1.32;
       font-weight: 400; color: rgba(255,255,255,.9); }}
  .telefono {{ position: absolute; left: 50%; transform: translateX(-50%);
               background: #0C0F0D; padding: 19px; border-radius: 88px;
               box-shadow: 0 60px 108px rgba(0,0,0,.35), 0 0 0 4px rgba(255,255,255,.12); }}
  .pantalla {{ overflow: hidden; border-radius: 69px; font-size: 0; }}
  .franja {{ background-image: url("{captura}"); background-repeat: no-repeat; }}
</style></head>
<body>
  <div class="marca"><img src="{icono}">Email Alarm</div>
  <h1 id="titulo">{titulo}</h1>
  <p id="bajada">{bajada}</p>
  <div class="telefono" id="tel"><div class="pantalla" id="pan"></div></div>
<script>
  // La bajada va debajo del titular, y el telefono debajo de la bajada,
  // midiendo lo que ocupa cada texto (una o dos lineas).
  const t = document.getElementById('titulo'), b = document.getElementById('bajada'),
        tel = document.getElementById('tel'), pan = document.getElementById('pan');
  b.style.top = (t.offsetTop + t.offsetHeight + 34) + 'px';
  const arriba = 730;   // fijo: las cinco capturas alinean el telefono a la misma altura
  let anchoPantalla;
  if ({completo}) {{
    const alto = {alto} - arriba - 80;          // entra entero, con margen abajo
    anchoPantalla = Math.round((alto - 38) * {ancho_origen} / {alto_origen});
  }} else {{
    anchoPantalla = 1010;                       // grande, cortado por el borde
  }}
  tel.style.top = arriba + 'px';
  pan.style.width = anchoPantalla + 'px';
  // Cada franja muestra un tramo de filas de la captura; lo salteado no se dibuja.
  const k = anchoPantalla / {ancho_origen};
  for (const [a, z] of {tramos}) {{
    const d = document.createElement('div');
    d.className = 'franja';
    d.style.width = anchoPantalla + 'px';
    d.style.height = Math.round((z - a) * k) + 'px';
    d.style.backgroundSize = anchoPantalla + 'px auto';
    d.style.backgroundPosition = '0 ' + (-Math.round(a * k)) + 'px';
    pan.appendChild(d);
  }}
</script>
</body></html>
"""


def tramos(saltos: list[tuple[int, int]], alto_origen: int) -> list[tuple[int, int]]:
    """Las filas que si se muestran, a partir de las franjas que se saltean."""
    visibles, desde = [], 0
    for a, z in sorted(saltos):
        if a > desde:
            visibles.append((desde, a))
        desde = max(desde, z)
    if desde < alto_origen:
        visibles.append((desde, alto_origen))
    return visibles


def renderizar(html: pathlib.Path, png: pathlib.Path, perfil: str) -> None:
    png.unlink(missing_ok=True)
    subprocess.run(
        [EDGE, "--headless=new", "--disable-gpu", "--hide-scrollbars",
         f"--user-data-dir={perfil}", f"--window-size={ANCHO},{ALTO}",
         "--allow-file-access-from-files", "--virtual-time-budget=3000",
         f"--screenshot={png}", html.as_uri()],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=False,
    )
    for _ in range(30):  # Edge escribe la captura en diferido
        if png.exists() and png.stat().st_size > 0:
            return
        time.sleep(1)
    raise SystemExit(f"FALLO {png.name}")


def main() -> None:
    SALIDA.mkdir(exist_ok=True)
    perfil = str(pathlib.Path(tempfile.gettempdir()) / "edge-marketing-ios-perfil")
    with tempfile.TemporaryDirectory() as tmp:
        for idioma, piezas in TEXTOS.items():
            for n, (captura, titulo, bajada, completo, saltos) in enumerate(piezas, 1):
                jpg = CAPTURAS / f"{captura}-{idioma}.jpg"
                alto_origen = ALTO  # las capturas vienen a 1290x2796
                html = pathlib.Path(tmp) / f"{n}-{idioma}.html"
                html.write_text(PLANTILLA.format(
                    ancho=ANCHO, alto=ALTO, titulo=titulo, bajada=bajada,
                    completo="true" if completo else "false",
                    ancho_origen=ANCHO_ORIGEN, alto_origen=alto_origen,
                    tramos=repr(tramos(saltos, alto_origen)).replace("(", "[").replace(")", "]"),
                    icono=(RAIZ / "web" / "icon.svg").as_uri(),
                    captura=jpg.as_uri(),
                ), encoding="utf-8")
                png = SALIDA / f"{n}-{captura}-{idioma}.png"
                renderizar(html, png, perfil)
                print(f"ok  apple/capturas-app-store/{png.name}")


if __name__ == "__main__":
    main()
