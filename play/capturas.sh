#!/usr/bin/env bash
# Saca las capturas de la app en ingles y espanol, desde el emulador.
#
# Uso (Git Bash, desde la raiz del proyecto, con el emulador abierto):
#   bash play/capturas.sh
#
# Requiere el build de DEBUG instalado: usa DemoReceiver (src/debug) para
# cargar alarmas e historial de ejemplo y abrir la pantalla de alarma sin tipear a mano.
# Deja las capturas en web/img/ (el sitio las usa directo).

set -euo pipefail
export MSYS_NO_PATHCONV=1   # si no, Git Bash reescribe /sdcard como ruta de Windows

ADB="/c/Users/lucia/AppData/Local/Android/Sdk/platform-tools/adb.exe"
DISPOSITIVO="${DISPOSITIVO:-emulator-5554}"
P=com.reverstabilizer.emailalarm
SALIDA="$(cd "$(dirname "$0")/.." && pwd)/web/img"
mkdir -p "$SALIDA"

adb_() { "$ADB" -s "$DISPOSITIVO" "$@"; }
captura() { adb_ exec-out screencap -p > "$SALIDA/$1.png"; echo "ok  web/img/$1.png"; }
demo() { adb_ shell am broadcast -a com.android.systemui.demo -e command "$@" >/dev/null; }
receptor() { adb_ shell am broadcast -n $P/.DemoReceiver "$@" >/dev/null; }

# Centro del primer (o N-esimo) elemento en pantalla cuyo XML contiene $1.
centro() {
    local patron="$1" n="${2:-1}"
    adb_ shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
    adb_ shell cat /sdcard/ui.xml | tr '>' '\n' | grep "$patron" | sed -n "${n}p" \
        | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' \
        | sed -E 's/bounds="\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]"/\1 \2 \3 \4/' \
        | awk '{print int(($1+$3)/2), int(($2+$4)/2)}'
}

abrir_app() {
    adb_ shell am force-stop $P
    adb_ shell am start -W -n $P/.MainActivity >/dev/null
    sleep 3
}

# --- Preparacion: todos los permisos, para que no aparezca ninguna alerta ---
adb_ shell pm grant $P android.permission.POST_NOTIFICATIONS
adb_ shell cmd notification allow_listener $P/$P.MailListener
adb_ shell cmd notification allow_dnd $P
adb_ shell dumpsys deviceidle whitelist +$P >/dev/null
adb_ shell appops set $P USE_FULL_SCREEN_INTENT allow
# Suscripcion simulada (solo existe en debug): las capturas muestran la app
# como la ve alguien suscripto.
receptor --es accion pro

# --- Barra de estado limpia: 9:41, bateria llena, wifi lleno, sin iconos ---
adb_ shell settings put global sysui_demo_allowed 1
demo enter
demo clock -e hhmm 0941
demo battery -e level 100 -e plugged false
demo network -e wifi show -e level 4 -e fully true
demo network -e mobile hide
demo notifications -e visible false

for idioma in en es; do
    adb_ shell cmd locale set-app-locales $P --locales $idioma
    receptor --es accion reglas --es idioma $idioma
    receptor --es accion historial --es idioma $idioma
    sleep 1

    # 1. Pantalla principal: las alarmas primero
    abrir_app
    captura "principal-$idioma"

    # 2. Editar la alarma de migraciones: palabra clave USCIS y consejo de spam
    adb_ shell input tap $(centro 'text="Cita de migraciones"\|text="Immigration appointment"')
    sleep 2
    captura "editar-$idioma"
    adb_ shell input keyevent KEYCODE_BACK
    sleep 1

    # 3. Dias y horario: la alarma de los turnos ya trae uno, asi que la seccion
    # abre desplegada. Un arrastre dentro del formulario para que entre entera.
    adb_ shell input tap $(centro 'text="Turnos del restaurante"\|text="Restaurant shifts"')
    sleep 2
    adb_ shell input swipe 540 1800 540 ${ARRASTRE_HORARIO:-1000} 1500
    sleep 1
    captura "horario-$idioma"
    adb_ shell input keyevent KEYCODE_BACK
    sleep 1

    # 4. Probar y actividad reciente. Arrastre lento para que no siga de largo.
    adb_ shell input swipe 540 1900 540 ${ARRASTRE_FIN:-1050} 1500
    sleep 1
    captura "actividad-$idioma"

    # 5. La pantalla de alarma
    receptor --es accion alarma --es idioma $idioma
    sleep 3
    captura "alarma-$idioma"
    receptor --es accion detener
    adb_ shell input keyevent KEYCODE_BACK
    sleep 1
done

# Dejar el emulador como estaba
adb_ shell cmd locale set-app-locales $P --locales ""
demo exit
echo "listo"
