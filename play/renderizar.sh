#!/usr/bin/env bash
# Renderiza los SVG de la ficha de Play a PNG con Edge headless.
# Uso (desde la raiz del proyecto, en Git Bash):  bash play/renderizar.sh
#
# Edge escribe la captura en diferido, asi que se espera a que aparezca cada
# archivo antes de seguir. El perfil aislado evita chocar con un Edge abierto.

set -euo pipefail

EDGE="/c/Program Files (x86)/Microsoft/Edge/Application/msedge.exe"
# Ruta absoluta: Edge resuelve las relativas desde su propio directorio.
DIR="$(cd "$(dirname "$0")" && pwd)"
PLAY_WIN="$(cygpath -w "$DIR")"
PERFIL="$(cygpath -w "${TMPDIR:-/tmp}")\\edge-render-perfil"

renderizar() {
    local nombre="$1" tamano="$2"
    local png="$DIR/$nombre.png"
    rm -f "$png"
    "$EDGE" --headless=new --disable-gpu --hide-scrollbars \
        --user-data-dir="$PERFIL" --window-size="$tamano" \
        --screenshot="$PLAY_WIN\\$nombre.png" \
        "file:///$(cygpath -m "$DIR")/$nombre.svg" >/dev/null 2>&1 || true

    for _ in $(seq 1 30); do
        [ -s "$png" ] && { echo "ok  $nombre.png ($tamano)"; return; }
        sleep 1
    done
    echo "FALLO  $nombre.png" >&2
    return 1
}

renderizar icono-play-512 512,512
renderizar grafico-destacado-en 1024,500
renderizar grafico-destacado-es 1024,500

# Los graficos destacados son tambien la imagen al compartir el sitio en redes.
cp "$DIR/grafico-destacado-en.png" "$DIR/../web/og.png"
cp "$DIR/grafico-destacado-es.png" "$DIR/../web/og-es.png"
echo "ok  web/og.png, web/og-es.png"
