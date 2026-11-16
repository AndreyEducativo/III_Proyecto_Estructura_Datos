#!/usr/bin/env bash
# =============================================================================
# run.sh - Compila y ejecuta Social UNA en Linux / macOS.
#
# Requisito único: un JDK 17 o superior (JAVA_HOME o "java" en el PATH).
# Maven NO hace falta: se usa el Maven Wrapper (./mvnw), que lo descarga solo
# la primera vez.
# =============================================================================

# -e: detenerse ante el primer error; -u: error si se usa una variable no definida.
set -eu

# Trabajar siempre desde la carpeta del proyecto para que data/ y target/ se
# resuelvan en el lugar correcto aunque el script se llame desde otra ruta.
cd "$(dirname "$0")"

# Elegir Java: JAVA_HOME tiene prioridad (mismo criterio que usa mvnw).
if [ -n "${JAVA_HOME:-}" ]; then
    JAVA_EXE="$JAVA_HOME/bin/java"
else
    JAVA_EXE="java"
fi

# Verificar la versión: "java -version" imprime p. ej. version "17.0.2" o "1.8.0_511".
# Se toma el número antes del primer punto (1.8 -> 1, que corresponde a Java 8).
VERSION_JAVA=$("$JAVA_EXE" -version 2>&1 | awk -F'"' '/version/ {print $2; exit}')
if [ -z "$VERSION_JAVA" ]; then
    echo "[ERROR] No se encontró Java. Instale un JDK 17 o superior." >&2
    exit 1
fi
MAYOR_JAVA="${VERSION_JAVA%%.*}"
if [ "$MAYOR_JAVA" -lt 17 ]; then
    echo "[ERROR] Se encontró Java $VERSION_JAVA, pero el proyecto requiere Java 17 o superior." >&2
    exit 1
fi

# Compilar y empaquetar (sin pruebas para arrancar rápido; pruebas: ./mvnw test).
sh ./mvnw -q -DskipTests package

# Ejecutar con -ea (assertions habilitadas, modo desarrollo) y UTF-8.
"$JAVA_EXE" -ea -Dfile.encoding=UTF-8 -jar target/social-una.jar
