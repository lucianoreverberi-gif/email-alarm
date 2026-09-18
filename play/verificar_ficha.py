"""Comprueba que los textos de ficha-de-play.md entran en los limites de Play.

Uso: python play/verificar_ficha.py
"""
import pathlib
import re
import sys

LIMITES = {"Título": 30, "Descripción corta": 80, "Descripción completa": 4000}
PROHIBIDAS = ["gratis!", "free!", "#1", "mejor app", "best app"]

texto = pathlib.Path(__file__).with_name("ficha-de-play.md").read_text(encoding="utf-8")
ok = True
for idioma in re.split(r"\n## Ficha en ", texto)[1:]:
    nombre = idioma.splitlines()[0]
    for campo, limite in LIMITES.items():
        m = re.search(r"\*\*%s\*\*[^\n]*\n\n```\n(.*?)\n```" % re.escape(campo), idioma, re.S)
        if not m:
            continue
        largo = len(m.group(1))
        estado = "ok" if largo <= limite else "SE PASA"
        ok &= largo <= limite
        print(f"{nombre:<30} {campo:<22} {largo:>5} / {limite}  {estado}")
        for palabra in PROHIBIDAS:
            if palabra in m.group(1).lower():
                print(f"   ojo: contiene '{palabra}'")
sys.exit(0 if ok else 1)
