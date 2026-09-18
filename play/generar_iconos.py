"""
Genera todos los iconos de Email Alarm desde una sola geometria.

Salidas:
  - app/src/main/res/drawable/ic_launcher_*.xml   (icono adaptativo de Android)
  - play/icono-play-512.svg                        (icono de la ficha de Play)
  - play/grafico-destacado-{en,es}.svg             (feature graphic 1024x500)
  - web/icon.svg                                   (favicon del sitio)

Los PNG para Play se renderizan despues con Edge headless (ver renderizar.sh).

Todo esta en el espacio de 108x108 del icono adaptativo. Android solo garantiza
que se vea el circulo central de radio 33 (la "zona segura"): todo lo que
importa tiene que caer adentro, o algun launcher lo va a recortar.

Uso:  python play/generar_iconos.py   (desde la raiz del proyecto)
"""

from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
RES = RAIZ / "app" / "src" / "main" / "res"
PLAY = RAIZ / "play"

VERDE = "#17A05A"
BLANCO = "#FFFFFF"
TRAZO = 5          # grosor de linea del sobre
HALO = 6           # margen que despega la campana del sobre

# --- Geometria (espacio 108x108) -------------------------------------------

# Sobre: rectangulo redondeado cerrado, con la solapa en V.
SOBRE = ("M36,37 L72,37 A5,5 0 0 1 77,42 L77,66 A5,5 0 0 1 72,71 "
         "L36,71 A5,5 0 0 1 31,66 L31,42 A5,5 0 0 1 36,37 Z")
SOLAPA = "M34,40 L54,55.5 L74,40"

def circulo(cx: float, cy: float, r: float) -> str:
    return (f"M{cx - r:.2f},{cy:.2f} a{r:.2f},{r:.2f} 0 1,0 {2 * r:.2f},0 "
            f"a{r:.2f},{r:.2f} 0 1,0 {-2 * r:.2f},0 Z")


def campana(escala: float, ax: float, ay: float) -> tuple[str, str]:
    """
    Cupula con el borde acampanado, mas la perilla de arriba.

    Esta dibujada relativa al centro del borde inferior (ax, ay), asi se puede
    agrandar o mover sin recalcular cada punto.
    """
    def p(x: float, y: float) -> str:
        return f"{ax + x * escala:.2f},{ay + y * escala:.2f}"

    cuerpo = (f"M{p(0, -25.5)} C{p(7.5, -25.5)} {p(12, -20)} {p(12, -12.5)} "
              f"L{p(12, -7)} C{p(12, -5.5)} {p(13, -4)} {p(14.5, -3)} "
              f"L{p(16, -2)} C{p(17, -1.3)} {p(16.5, 0)} {p(15.3, 0)} "
              f"L{p(-15.3, 0)} C{p(-16.5, 0)} {p(-17, -1.3)} {p(-16, -2)} "
              f"L{p(-14.5, -3)} C{p(-13, -4)} {p(-12, -5.5)} {p(-12, -7)} "
              f"L{p(-12, -12.5)} C{p(-12, -20)} {p(-7.5, -25.5)} {p(0, -25.5)} Z")
    perilla = circulo(ax, ay - 27.5 * escala, 3.2 * escala)
    return cuerpo, perilla


# El borde de la campana queda ADENTRO del sobre, con aire para el halo: si lo
# toca, el halo se come las esquinas del sobre y deja ganchos sueltos.
# Solo el badajo rompe la linea de abajo, al centro, como en el logo.
CAMPANA, PERILLA = campana(escala=0.73, ax=54, ay=65.2)
BADAJO = circulo(54, 71, 3)

# Ondas de sonido: arcos alrededor de la campana, por FUERA del sobre.
# Solo en el grafico destacado; en el icono no entran en la zona segura.
def onda(cx: float, cy: float, r: float, lado: int) -> str:
    """Arco de 50 grados a la izquierda (lado=-1) o a la derecha (lado=1)."""
    import math
    desde, hasta = (155, 205) if lado < 0 else (-25, 25)
    x1 = cx + r * math.cos(math.radians(desde))
    y1 = cy + r * math.sin(math.radians(desde))
    x2 = cx + r * math.cos(math.radians(hasta))
    y2 = cy + r * math.sin(math.radians(hasta))
    return f"M{x1:.2f},{y1:.2f} A{r},{r} 0 0 1 {x2:.2f},{y2:.2f}"


ONDAS = [onda(54, 57, r, lado) for r in (30, 38) for lado in (-1, 1)]

CAMPANA_COMPLETA = [CAMPANA, PERILLA, BADAJO]


# --- Android: vector drawables ----------------------------------------------

def vector(capas: str) -> str:
    return f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Generado por play/generar_iconos.py. No editar a mano: editar el script. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
{capas}
</vector>
'''


def path_relleno(datos: str, color: str) -> str:
    return (f'    <path\n        android:fillColor="{color}"\n'
            f'        android:pathData="{datos}" />')


def path_trazo(datos: str, color: str, ancho: float, relleno: str = "#00000000") -> str:
    return (f'    <path\n        android:fillColor="{relleno}"\n'
            f'        android:strokeColor="{color}"\n'
            f'        android:strokeWidth="{ancho}"\n'
            f'        android:strokeLineCap="round"\n'
            f'        android:strokeLineJoin="round"\n'
            f'        android:pathData="{datos}" />')


def escribir_android() -> None:
    fondo = vector(path_relleno("M0,0h108v108h-108z", VERDE))

    capas = [
        "    <!-- Sobre -->",
        path_trazo(SOBRE, BLANCO, TRAZO),
        path_trazo(SOLAPA, BLANCO, TRAZO),
        "    <!-- Halo del color del fondo: despega la campana de las lineas del sobre -->",
        *[path_trazo(p, VERDE, HALO, relleno=VERDE) for p in CAMPANA_COMPLETA],
        "    <!-- Campana -->",
        *[path_relleno(p, BLANCO) for p in CAMPANA_COMPLETA],
    ]
    frente = vector("\n".join(capas))

    # El icono tematico (Android 13+) usa solo la silueta y la tine con el
    # color del sistema. El halo se tenia igual que todo y haria un manchon,
    # asi que en monocromo va solo la campana. Y mas grande: sin el sobre al
    # lado, la campana chica queda perdida en el medio.
    cuerpo, perilla = campana(escala=1.05, ax=54, ay=70)
    badajo = "M49.3,72.1 A4.7,4.7 0 0 0 58.7,72.1 Z"
    monocromo = vector("\n".join(path_relleno(p, BLANCO) for p in (cuerpo, perilla, badajo)))

    drawable = RES / "drawable"
    (drawable / "ic_launcher_background.xml").write_text(fondo, encoding="utf-8")
    (drawable / "ic_launcher_foreground.xml").write_text(frente, encoding="utf-8")
    (drawable / "ic_launcher_monochrome.xml").write_text(monocromo, encoding="utf-8")

    adaptativo = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
'''
    anydpi = RES / "mipmap-anydpi"
    (anydpi / "ic_launcher.xml").write_text(adaptativo, encoding="utf-8")
    (anydpi / "ic_launcher_round.xml").write_text(adaptativo, encoding="utf-8")


# --- SVG para la ficha de Play ----------------------------------------------

def svg_marca(con_ondas: bool) -> str:
    partes = []
    if con_ondas:
        partes += [f'<path d="{o}" fill="none" stroke="{BLANCO}" stroke-width="4.5" '
                   f'stroke-linecap="round"/>' for o in ONDAS]
    partes += [
        f'<path d="{SOBRE}" fill="none" stroke="{BLANCO}" stroke-width="{TRAZO}" '
        f'stroke-linejoin="round"/>',
        f'<path d="{SOLAPA}" fill="none" stroke="{BLANCO}" stroke-width="{TRAZO}" '
        f'stroke-linecap="round" stroke-linejoin="round"/>',
    ]
    partes += [f'<path d="{p}" fill="{VERDE}" stroke="{VERDE}" stroke-width="{HALO}" '
               f'stroke-linejoin="round"/>' for p in CAMPANA_COMPLETA]
    partes += [f'<path d="{p}" fill="{BLANCO}"/>' for p in CAMPANA_COMPLETA]
    return "\n    ".join(partes)


def escribir_play() -> None:
    # Play pide 512x512 cuadrado, sin esquinas redondeadas (las pone Google).
    # Se encuadra el area visible de 72x72 del icono adaptativo, asi el icono
    # de la tienda y el del telefono se ven identicos.
    icono = f'''<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="18 18 72 72">
    <rect x="18" y="18" width="72" height="72" fill="{VERDE}"/>
    {svg_marca(con_ondas=False)}
</svg>
'''
    (PLAY / "icono-play-512.svg").write_text(icono, encoding="utf-8")
    # El mismo icono es el favicon del sitio.
    (RAIZ / "web" / "icon.svg").write_text(icono, encoding="utf-8")

    textos = {
        "en": ("Email Alarm", "The email you can&#8217;t afford to miss"),
        "es": ("Email Alarm", "El correo que no te puedes perder"),
    }
    for idioma, (titulo, bajada) in textos.items():
        grafico = f'''<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="500" viewBox="0 0 1024 500">
    <rect width="1024" height="500" fill="{VERDE}"/>
    <!-- Fondo liso a proposito: el halo de la campana es verde puro, y
         cualquier tono distinto detras lo haria visible como un anillo. -->
    <g transform="translate(-38,-8) scale(4.6)">
    {svg_marca(con_ondas=True)}
    </g>
    <text x="430" y="238" font-family="Segoe UI, Arial, sans-serif" font-weight="700"
          font-size="80" fill="{BLANCO}">{titulo}</text>
    <text x="434" y="296" font-family="Segoe UI, Arial, sans-serif"
          font-size="29" fill="{BLANCO}" fill-opacity="0.88">{bajada}</text>
</svg>
'''
        (PLAY / f"grafico-destacado-{idioma}.svg").write_text(grafico, encoding="utf-8")


if __name__ == "__main__":
    escribir_android()
    escribir_play()
    print("iconos generados")
