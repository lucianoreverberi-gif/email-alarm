"""Capturas de marketing para la App Store: titular + la captura real del iPhone.

Uso (desde la raiz del proyecto):
    python apple/marketing_ios.py

Toma las capturas de apple/capturas-iphone/ y deja las imagenes en
apple/capturas-app-store/, 1290x2796 (el tamano de 6.9" que pide Apple y el
mismo que traen las capturas originales). Renderiza con Edge headless,
igual que play/marketing.py.

Dos retoques sobre la captura original, los dos en pixeles del JPG:

- "saltos": franjas de filas que no se muestran. Sirve para sacar un aviso
  pasajero ("Alarm saved on your iPhone") o un elemento cortado arriba de todo;
  el corte cae sobre el fondo liso, asi que no se nota.
- "parches": tapan un rectangulo con el color del fondo (se toma de un pixel de
  la misma captura, asi no hay que adivinarlo) y escriben otro texto encima.
  Solo para los datos de prueba: el remitente del email que uso Luciano para
  probar es su propia empresa, y en la ficha queda mejor un ejemplo neutro.
  La interfaz de la app no se toca.
"""
import json
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

REMITENTE = "Acme Restaurant"
CORREO = "shifts@acmerestaurant.com"
# La alarma se llama "Inmigracion" en el telefono de prueba; en la ficha en
# ingles conviene que se lea en ingles.
INMIGRACION = "Immigration"

# El telefono completo se usa cuando lo importante esta abajo (el boton Stop alarm);
# si no, el telefono sale mas grande y cortado por el borde inferior.
TEXTOS = {
    "en": [
        dict(captura="lista",
             titulo="Stop watching <em>your inbox</em>",
             bajada="Set an alarm and get on with your day. When that email lands, it rings.",
             parches=[
                 dict(rect=(286, 2285, 420, 90), muestra=(838, 2320),
                      texto=INMIGRACION, fuente=56, color="#14213d", alinear="left"),
             ]),
        dict(captura="alarma",
             titulo="Rings like a <em>real alarm</em>",
             bajada="Full screen, with the sender and the subject, until you stop it.",
             completo=True,
             parches=[
                 # Las cuatro lineas del remitente pasan a una sola, centrada.
                 dict(rect=(40, 880, 1210, 610), muestra=(30, 1200), subir=170,
                      texto=REMITENTE, fuente=88, color="#ffffff", alinear="center"),
                 # La direccion, debajo del asunto.
                 dict(rect=(40, 1620, 1210, 90), muestra=(30, 1660),
                      texto=CORREO, fuente=44, peso=400,
                      color="#dfe6f2", alinear="center"),
             ]),
        dict(captura="editar",
             titulo="You choose <em>what matters</em>",
             bajada="A sender, a keyword, or both. Type “uscis” and it also rings for “USCIS Online Account”.",
             parches=[
                 dict(rect=(109, 700, 560, 120), muestra=(978, 762),
                      texto=INMIGRACION, fuente=58, peso=400,
                      color="#101828", alinear="left"),
             ]),
        dict(captura="horario",
             titulo="Only when it <em>should</em> ring",
             bajada="Pick the days and the hours of each alarm. Outside them, it stays quiet.",
             saltos=[(157, 414)]),
        dict(captura="actividad",
             titulo="Know <em>why</em> it rang",
             bajada="Every alert, with its sender and subject, saved on your iPhone.",
             parches=[
                 dict(rect=(250, 1500, 700, 80), muestra=(838, 1747),
                      texto=REMITENTE, fuente=50, color="#14213d", alinear="left"),
             ]),
    ],
}

PLANTILLA = """<!DOCTYPE html>
<html><head><meta charset="utf-8">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&display=swap">
<style>
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
  .pantalla {{ overflow: hidden; border-radius: 69px; font-size: 0; position: relative; }}
  .franja {{ background-image: url("{captura}"); background-repeat: no-repeat; }}
  /* Inter es lo mas parecido a la tipografia del sistema del iPhone que hay aca. */
  .parche {{ position: absolute; display: flex; align-items: center;
             font-family: Inter, "Segoe UI", Arial, sans-serif; font-weight: 700;
             letter-spacing: -.5px; line-height: 1.1; }}
</style></head>
<body>
  <div class="marca"><img src="{icono}">Email Alarm</div>
  <h1 id="titulo">{titulo}</h1>
  <p id="bajada">{bajada}</p>
  <div class="telefono" id="tel"><div class="pantalla" id="pan"></div></div>
<script>
  const t = document.getElementById('titulo'), b = document.getElementById('bajada'),
        tel = document.getElementById('tel'), pan = document.getElementById('pan');
  b.style.top = (t.offsetTop + t.offsetHeight + 34) + 'px';
  const arriba = 730;   // fijo: las cinco capturas alinean el telefono a la misma altura
  let anchoPantalla;
  if ({completo}) {{
    const alto = {alto} - arriba - 80;          // entra entero, con margen abajo
    anchoPantalla = Math.round((alto - 38) * {ancho_origen} / {alto_origen});
  }} else {{
    anchoPantalla = 950;                        // entra casi entera, hasta el borde de abajo
  }}
  tel.style.top = arriba + 'px';
  pan.style.width = anchoPantalla + 'px';
  const k = anchoPantalla / {ancho_origen};
  const visibles = {tramos};
  // De una fila de la captura a su posicion en pantalla, descontando lo salteado.
  function enPantalla(fila) {{
    let alto = 0;
    for (const [a, z] of visibles) {{
      if (fila < z) return (alto + Math.max(0, fila - a)) * k;
      alto += z - a;
    }}
    return alto * k;
  }}
  // Cada franja muestra un tramo de filas de la captura; lo salteado no se dibuja.
  for (const [a, z] of visibles) {{
    const d = document.createElement('div');
    d.className = 'franja';
    d.style.width = anchoPantalla + 'px';
    d.style.height = Math.round((z - a) * k) + 'px';
    d.style.backgroundSize = anchoPantalla + 'px auto';
    d.style.backgroundPosition = '0 ' + (-Math.round(a * k)) + 'px';
    pan.appendChild(d);
  }}

  // Los parches van al final: tapan con el color real de la captura y reescriben.
  const parches = {parches};
  if (parches.length) {{
    const img = new Image();
    img.onload = () => {{
      const c = document.createElement('canvas');
      c.width = img.naturalWidth; c.height = img.naturalHeight;
      c.getContext('2d').drawImage(img, 0, 0);
      const ctx = c.getContext('2d');
      for (const p of parches) {{
        const [mx, my] = p.muestra;
        const [r, g, bl] = ctx.getImageData(mx, my, 1, 1).data;
        const [x, y, w, h] = p.rect;
        const d = document.createElement('div');
        d.className = 'parche';
        d.style.background = `rgb(${{r}},${{g}},${{bl}})`;
        d.style.left = Math.round(x * k) + 'px';
        d.style.top = Math.round(enPantalla(y)) + 'px';
        d.style.width = Math.round(w * k) + 'px';
        d.style.height = Math.round(enPantalla(y + h) - enPantalla(y)) + 'px';
        d.style.fontSize = Math.round(p.fuente * k) + 'px';
        d.style.fontWeight = p.peso || 700;
        d.style.color = p.color;
        d.style.justifyContent = p.alinear === 'left' ? 'flex-start' : 'center';
        if (p.subir) d.style.paddingBottom = Math.round(p.subir * k) + 'px';
        d.textContent = p.texto;
        pan.appendChild(d);
      }}
      document.title = 'listo';
    }};
    img.src = "{captura}";
  }}
</script>
</body></html>
"""


def tramos(saltos, alto_origen):
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
         "--allow-file-access-from-files", "--virtual-time-budget=6000",
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
            for n, pieza in enumerate(piezas, 1):
                nombre = pieza["captura"]
                jpg = CAPTURAS / f"{nombre}-{idioma}.jpg"
                visibles = tramos(pieza.get("saltos", []), ALTO)
                html = pathlib.Path(tmp) / f"{n}-{idioma}.html"
                html.write_text(PLANTILLA.format(
                    ancho=ANCHO, alto=ALTO,
                    titulo=pieza["titulo"], bajada=pieza["bajada"],
                    completo="true" if pieza.get("completo") else "false",
                    ancho_origen=ANCHO_ORIGEN, alto_origen=ALTO,
                    tramos=json.dumps([list(t) for t in visibles]),
                    parches=json.dumps(pieza.get("parches", [])),
                    icono=(RAIZ / "web" / "icon.svg").as_uri(),
                    captura=jpg.as_uri(),
                ), encoding="utf-8")
                png = SALIDA / f"{n}-{nombre}-{idioma}.png"
                renderizar(html, png, perfil)
                print(f"ok  apple/capturas-app-store/{png.name}")


if __name__ == "__main__":
    main()
