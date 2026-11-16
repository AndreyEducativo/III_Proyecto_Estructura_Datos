@echo off
REM ===========================================================================
REM run.bat - Compila y ejecuta Social UNA en Windows.
REM
REM Requisito unico: un JDK 17 o superior. Se usa el de JAVA_HOME si esta
REM definido; si no, el "java" que aparezca en el PATH.
REM Maven NO hace falta: se usa el Maven Wrapper (mvnw.cmd), que lo descarga solo
REM la primera vez (requiere Internet solo en esa primera ejecucion).
REM ===========================================================================
setlocal

REM Trabajar siempre desde la carpeta del proyecto, aunque el script se ejecute
REM desde otra ruta: asi "data\" y "target\" se resuelven en el lugar correcto.
cd /d "%~dp0"

REM Elegir el ejecutable de Java: JAVA_HOME tiene prioridad (igual que en mvnw).
REM Se escribe en dos lineas (sin bloque con parentesis) porque rutas como
REM "Program Files (x86)" romperian un bloque if ( ... ).
set "JAVA_EXE=java"
if defined JAVA_HOME set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"

REM Verificar que sea Java 17 o superior. "java -version" imprime algo como
REM   java version "17.0.2"   o   java version "1.8.0_511"
REM Se toma el numero antes del primer punto: 17 -> 17, 1.8 -> 1 (Java 8 = viejo).
set "VERSION_JAVA="
for /f "tokens=3" %%v in ('"%JAVA_EXE%" -version 2^>^&1 ^| findstr /i version') do set "VERSION_JAVA=%%~v"
if not defined VERSION_JAVA (
    echo [ERROR] No se encontro Java. Instale un JDK 17 o superior y configure JAVA_HOME.
    exit /b 1
)
for /f "delims=." %%a in ("%VERSION_JAVA%") do set "MAYOR_JAVA=%%a"
if %MAYOR_JAVA% LSS 17 (
    echo [ERROR] Se encontro Java %VERSION_JAVA%, pero el proyecto requiere Java 17 o superior.
    echo         Configure JAVA_HOME apuntando a un JDK 17+.
    exit /b 1
)

REM Compilar y empaquetar el JAR. -DskipTests para arrancar rapido; las pruebas
REM se ejecutan aparte con: mvnw.cmd test
REM Se usa la ruta completa (%~dp0) porque algunos Windows tienen activa la politica
REM NoDefaultCurrentDirectoryInExePath, que impide ejecutar scripts de la carpeta
REM actual escribiendo solo su nombre.
call "%~dp0mvnw.cmd" -q -DskipTests package
if errorlevel 1 (
    echo [ERROR] La compilacion fallo. Revise los mensajes anteriores.
    exit /b 1
)

REM Ejecutar con -ea (assertions habilitadas, modo desarrollo) y UTF-8.
"%JAVA_EXE%" -ea -Dfile.encoding=UTF-8 -jar target\social-una.jar
endlocal
