"""Capturas de marketing para Google Play: titular + la captura real en un telefono.

Uso (desde la raiz del proyecto), despues de `bash play/capturas.sh`:
    python play/marketing.py

Toma las capturas de web/img/ y deja las imagenes en play/capturas-play/,
1080x1920 (9:16: dentro del maximo 2:1 de Play). Renderiza con Edge headless,
igual que play/renderizar.sh.
"""
import pathlib
import subprocess
import tempfile
import time

RAIZ = pathlib.Path(__file__).resolve().parent.parent
IMG = RAIZ / "web" / "img"
SALIDA = RAIZ / "play" / "capturas-play"
EDGE = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
ANCHO, ALTO = 1080, 1920

# (captura, titular con <em> en lo que se destaca, bajada, telefono completo?)
# El telefono completo se usa cuando lo importante esta abajo (el boton DETENER);
# si no, el telefono sale mas grande y cortado por el borde inferior.
TEXTOS = {
    "es": [
        ("principal", "Deja de revisar el correo <em>cada 10 minutos</em>",
         "Crea una alarma y sigue con tu vida. Cuando llega ese email, suena.", False),
        ("alarma", "Suena como un <em>despertador</em>",
         "Fuerte, a pantalla completa, aunque el teléfono esté en silencio.", True),
        ("editar", "Suena únicamente con <em>las cosas importantes para ti</em>",
         "Pon “uscis” y también suena con “USCIS Online Account”.", False),
        ("actividad", "Pruébala antes de <em>necesitarla</em>",
         "Y mira cada email que detectó. Nada sale de tu teléfono.", False),
    ],
    "en": [
        ("principal", "Stop checking your inbox <em>every 10 minutes</em>",
         "Set an alarm and get on with your life. When that email lands, it rings.", False),
        ("alarma", "Rings like an <em>alarm clock</em>",
         "Loud, full screen, even with your phone on silent.", True),
        ("editar", "It only rings for <em>the things that matter to you</em>",
         "Type “uscis” and it also rings for “USCIS Online Account”.", False),
        ("actividad", "Test it before you <em>need it</em>",
         "See every email it caught. Nothing leaves your phone.", False),
    ],
}

PLANTILLA = """<!DOCTYPE html>
<html><head><meta charset="utf-8"><style>
  * {{ margin: 0; box-sizing: border-box; }}
  html, body {{ width: {ancho}px; height: {alto}px; overflow: hidden; }}
  body {{
    font-family: "Segoe UI", Roboto, Arial, sans-serif;
    background:
      radial-gradient(900px 700px at 85% 5%, rgba(255,255,255,.16), transparent 60%),
      linear-gradient(165deg, #1FB86C 0%, #17A05A 45%, #0B6E3C 100%);
    color: #fff; position: relative;
  }}
  .marca {{ position: absolute; top: 96px; left: 90px; display: flex; align-items: center; gap: 18px;
            font-size: 34px; font-weight: 600; letter-spacing: .3px; opacity: .95; }}
  .marca img {{ width: 60px; height: 60px; border-radius: 16px;
               box-shadow: 0 0 0 3px rgba(255,255,255,.85), 0 6px 16px rgba(0,0,0,.2); }}
  h1 {{ position: absolute; top: 210px; left: 90px; right: 90px;
        font-size: 92px; line-height: 1.04; font-weight: 800; letter-spacing: -2px; }}
  h1 em {{ font-style: normal; color: #C8F5DA; }}
  p {{ position: absolute; left: 90px; right: 110px; font-size: 40px; line-height: 1.32;
       font-weight: 400; color: rgba(255,255,255,.9); }}
  .telefono {{ position: absolute; left: 50%; transform: translateX(-50%);
               background: #0C0F0D; padding: 16px; border-radius: 74px;
               box-shadow: 0 50px 90px rgba(0,0,0,.35), 0 0 0 3px rgba(255,255,255,.12); }}
  .telefono img {{ display: block; width: 100%; border-radius: 58px; }}
</style></head>
<body>
  <div class="marca"><img src="{icono}">Email Alarm</div>
  <h1 id="titulo">{titulo}</h1>
  <p id="bajada">{bajada}</p>
  <div class="telefono" id="tel"><img src="{captura}"></div>
<script>
  // La bajada va debajo del titular, y el telefono debajo de la bajada,
  // midiendo lo que ocupa cada texto (una o dos lineas).
  const t = document.getElementById('titulo'), b = document.getElementById('bajada'),
        tel = document.getElementById('tel');
  b.style.top = (t.offsetTop + t.offsetHeight + 30) + 'px';
  const arriba = b.offsetTop + b.offsetHeight + 70;
  if ({completo}) {{
    const alto = {alto} - arriba - 70;          // entra entero, con margen abajo
    tel.style.top = arriba + 'px';
    tel.style.width = Math.round((alto - 32) * 1080 / 2400 + 32) + 'px';
  }} else {{
    tel.style.top = arriba + 'px';
    tel.style.width = '780px';                  // grande, cortado por el borde
  }}
</script>
</body></html>
"""


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
    perfil = str(pathlib.Path(tempfile.gettempdir()) / "edge-marketing-perfil")
    with tempfile.TemporaryDirectory() as tmp:
        for idioma, piezas in TEXTOS.items():
            for n, (captura, titulo, bajada, completo) in enumerate(piezas, 1):
                html = pathlib.Path(tmp) / f"{n}-{idioma}.html"
                html.write_text(PLANTILLA.format(
                    ancho=ANCHO, alto=ALTO, titulo=titulo, bajada=bajada,
                    completo="true" if completo else "false",
                    icono=(RAIZ / "web" / "icon.svg").as_uri(),
                    captura=(IMG / f"{captura}-{idioma}.png").as_uri(),
                ), encoding="utf-8")
                png = SALIDA / f"{n}-{captura}-{idioma}.png"
                renderizar(html, png, perfil)
                print(f"ok  play/capturas-play/{png.name}")


if __name__ == "__main__":
    main()
