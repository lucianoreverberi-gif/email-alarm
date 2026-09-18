@echo off
rem Crea la llave de subida (solo la primera vez) y firma el bundle de release.
rem Uso, desde la raiz del proyecto:   .\play\firmar.bat
rem
rem Las contrasenas se escriben en la terminal cuando las piden. No se guardan
rem en ningun archivo. La llave vive FUERA del proyecto, en Documents\claves,
rem para que sea imposible subirla a GitHub por error.
rem
rem Antes de correrlo, generar el bundle:   gradlew bundleRelease

setlocal
set "JBR=C:\Program Files\Android\Android Studio\jbr\bin"
set "CLAVES=%USERPROFILE%\Documents\claves"
set "LLAVE=%CLAVES%\email-alarm-upload.jks"
set "SIN_FIRMAR=%~dp0..\app\build\outputs\bundle\release\app-release.aab"
set "FIRMADO=%~dp0..\app\build\outputs\bundle\release\email-alarm-firmado.aab"

if not exist "%SIN_FIRMAR%" (
  echo No encuentro el bundle sin firmar. Corre primero: gradlew bundleRelease
  goto error
)

if not exist "%CLAVES%" mkdir "%CLAVES%"

if not exist "%LLAVE%" (
  echo.
  echo === Paso 1 de 2: crear la llave de subida ===
  echo Te va a pedir una contrasena DOS veces. Mientras la escribis no se ve nada:
  echo es normal, la terminal la oculta. Despues de cada una apreta Enter.
  echo.
  "%JBR%\keytool.exe" -genkeypair -keystore "%LLAVE%" -keyalg RSA -keysize 2048 -validity 10000 -alias upload -dname "CN=Luciano Reverberi, C=US"
  if errorlevel 1 goto error
  echo.
  echo Llave creada: %LLAVE%
) else (
  echo La llave ya existe, no se crea otra: %LLAVE%
)

echo.
echo === Paso 2 de 2: firmar el bundle ===
echo Te va a pedir la misma contrasena, una vez.
echo.
"%JBR%\jarsigner.exe" -keystore "%LLAVE%" -signedjar "%FIRMADO%" "%SIN_FIRMAR%" upload
if errorlevel 1 goto error

echo.
echo ============================================================
echo  LISTO. Bundle firmado:
echo  %FIRMADO%
echo.
echo  IMPORTANTE: guarda una copia de la llave y su contrasena en
echo  un lugar seguro (gestor de contrasenas, pendrive):
echo  %LLAVE%
echo ============================================================
goto :eof

:error
echo.
echo Algo fallo. Copia lo que dice arriba y pasaselo a Claude.
exit /b 1
