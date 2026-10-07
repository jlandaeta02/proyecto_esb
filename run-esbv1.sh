#!/bin/bash

# Configuración de codificación para IBM i PASE
export QIBM_PASE_DESCRIPTOR_STDIO=B80
export LANG=EN_US.UTF-8
export LC_ALL=EN_US.UTF-8

# Base del Classpath (clases compiladas)
CP="/home/JLANDAETA/bankesb/bin/out"

# Agregar automáticamente todos los .jar encontrados en el proyecto
for jar in $(find /home/JLANDAETA -name "*.jar"); do
    CP="$CP:$jar"
done

echo "===================================================="
echo "CLASSPATH CONSTRUIDO:"
echo "$CP"
echo "===================================================="

# Ejecutar verificando la clase principal
java -cp "$CP" com.bank.esb.Main
