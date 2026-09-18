# Crea la llave de subida (solo la primera vez) y firma el bundle de release.
#
# Uso: doble clic no alcanza por la politica de PowerShell; desde la raiz:
#   powershell -ExecutionPolicy Bypass -File play\firmar.ps1
#
# La contrasena se pide en una ventana con opcion "Mostrar", se valida antes
# de usarla, y se le pasa a keytool/jarsigner por una variable de entorno que
# solo existe mientras corre este proceso. No se escribe en ningun archivo.
# Lo que responden las herramientas (nunca la contrasena) queda en
# %TEMP%\firmar-email-alarm.log para poder diagnosticar errores.

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

$jbr       = 'C:\Program Files\Android\Android Studio\jbr\bin'
$claves    = Join-Path $env:USERPROFILE 'Documents\claves'
$llave     = Join-Path $claves 'email-alarm-upload.jks'
$raiz      = Split-Path $PSScriptRoot -Parent
$sinFirmar = Join-Path $raiz 'app\build\outputs\bundle\release\app-release.aab'
$firmado   = Join-Path $raiz 'app\build\outputs\bundle\release\email-alarm-firmado.aab'
$log       = Join-Path $env:TEMP 'firmar-email-alarm.log'

function Mensaje($texto, $error) {
    $icono = if ($error) { [System.Windows.Forms.MessageBoxIcon]::Error } else { [System.Windows.Forms.MessageBoxIcon]::Information }
    [void][System.Windows.Forms.MessageBox]::Show($texto, 'Firmar Email Alarm', 'OK', $icono)
}

function Pedir-Contrasena([bool]$crear) {
    $form = New-Object System.Windows.Forms.Form
    $form.Text = 'Firmar Email Alarm'
    $form.Size = New-Object System.Drawing.Size(460, $(if ($crear) { 330 } else { 250 }))
    $form.StartPosition = 'CenterScreen'
    $form.FormBorderStyle = 'FixedDialog'
    $form.MaximizeBox = $false
    $form.TopMost = $true
    $form.Font = New-Object System.Drawing.Font('Segoe UI', 10)

    $info = New-Object System.Windows.Forms.Label
    $info.Location = '20,15'; $info.Size = '410,60'
    $info.Text = if ($crear) {
        "Elegi una contrasena para la llave de subida.`nMinimo 8 caracteres. Solo letras sin tilde y numeros.`nAnotala antes en un lugar seguro: no se puede recuperar."
    } else {
        "La llave ya existe. Escribi su contrasena para firmar."
    }
    $form.Controls.Add($info)

    $l1 = New-Object System.Windows.Forms.Label
    $l1.Location = '20,85'; $l1.Size = '410,22'; $l1.Text = 'Contrasena'
    $form.Controls.Add($l1)
    $t1 = New-Object System.Windows.Forms.TextBox
    $t1.Location = '20,108'; $t1.Size = '400,28'; $t1.UseSystemPasswordChar = $true
    $form.Controls.Add($t1)

    $t2 = $null
    $y = 145
    if ($crear) {
        $l2 = New-Object System.Windows.Forms.Label
        $l2.Location = '20,145'; $l2.Size = '410,22'; $l2.Text = 'Repetila'
        $form.Controls.Add($l2)
        $t2 = New-Object System.Windows.Forms.TextBox
        $t2.Location = '20,168'; $t2.Size = '400,28'; $t2.UseSystemPasswordChar = $true
        $form.Controls.Add($t2)
        $y = 205
    }

    $mostrar = New-Object System.Windows.Forms.CheckBox
    $mostrar.Location = "20,$y"; $mostrar.Size = '200,24'; $mostrar.Text = 'Mostrar contrasena'
    $mostrar.Add_CheckedChanged({
        $t1.UseSystemPasswordChar = -not $mostrar.Checked
        if ($t2) { $t2.UseSystemPasswordChar = -not $mostrar.Checked }
    })
    $form.Controls.Add($mostrar)

    $ok = New-Object System.Windows.Forms.Button
    $ok.Location = "240,$($y + 35)"; $ok.Size = '90,32'; $ok.Text = 'Aceptar'
    $ok.DialogResult = 'OK'
    $form.AcceptButton = $ok
    $form.Controls.Add($ok)
    $cancel = New-Object System.Windows.Forms.Button
    $cancel.Location = "335,$($y + 35)"; $cancel.Size = '90,32'; $cancel.Text = 'Cancelar'
    $cancel.DialogResult = 'Cancel'
    $form.CancelButton = $cancel
    $form.Controls.Add($cancel)

    while ($true) {
        $t1.Select()
        if ($form.ShowDialog() -ne 'OK') { return $null }
        $p = $t1.Text
        if ($crear) {
            if ($p.Length -lt 8) { Mensaje 'La contrasena tiene que tener al menos 8 caracteres.' $true; continue }
            if ($p -notmatch '^[A-Za-z0-9]+$') { Mensaje 'Usa solo letras sin tilde (sin enie) y numeros. Otros caracteres a veces fallan en Windows.' $true; continue }
            if ($p -cne $t2.Text) { Mensaje 'Las dos contrasenas no coinciden. Tilda "Mostrar contrasena" para revisarlas.' $true; continue }
        } elseif ($p.Length -eq 0) { continue }
        return $p
    }
}

"=== $(Get-Date) ===" | Out-File $log -Encoding utf8

if (-not (Test-Path $sinFirmar)) {
    Mensaje "No encuentro el bundle sin firmar:`n$sinFirmar`n`nHay que generarlo primero (gradlew bundleRelease)." $true
    exit 1
}
New-Item -ItemType Directory -Force $claves | Out-Null

$crear = -not (Test-Path $llave)
$pass = Pedir-Contrasena $crear
if (-not $pass) { "Cancelado por el usuario" | Out-File $log -Append -Encoding utf8; exit 1 }

$env:EMAIL_ALARM_PASS = $pass
$pass = $null
try {
    if ($crear) {
        "--- keytool ---" | Out-File $log -Append -Encoding utf8
        & "$jbr\keytool.exe" -genkeypair -keystore $llave -keyalg RSA -keysize 2048 `
            -validity 10000 -alias upload -dname 'CN=Luciano Reverberi, C=US' `
            -storepass:env EMAIL_ALARM_PASS 2>&1 | Out-File $log -Append -Encoding utf8
        if ($LASTEXITCODE -ne 0 -or -not (Test-Path $llave)) {
            Mensaje "No se pudo crear la llave. El detalle quedo en:`n$log" $true
            exit 1
        }
    }

    "--- jarsigner ---" | Out-File $log -Append -Encoding utf8
    & "$jbr\jarsigner.exe" -keystore $llave -storepass:env EMAIL_ALARM_PASS `
        -signedjar $firmado $sinFirmar upload 2>&1 | Out-File $log -Append -Encoding utf8
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path $firmado)) {
        Mensaje "No se pudo firmar el bundle. Si la llave ya existia, puede ser la contrasena.`nEl detalle quedo en:`n$log" $true
        exit 1
    }
}
finally {
    Remove-Item Env:\EMAIL_ALARM_PASS -ErrorAction SilentlyContinue
}

"OK" | Out-File $log -Append -Encoding utf8
Mensaje "LISTO. Bundle firmado.`n`nIMPORTANTE: guarda una copia de la llave y su contrasena en un lugar seguro (gestor de contrasenas o pendrive):`n$llave" $false
