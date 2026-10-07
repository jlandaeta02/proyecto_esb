#!/bin/bash

# Configurar encoding explicito para PASE
export QIBM_PASE_DESCRIPTOR_STDIO=B80
export LANG=EN_US.UTF-8
export LC_ALL=EN_US.UTF-8

mkdir -p /home/JLANDAETA/bankesb/bin/out

# Obtener los archivos en un array sin escribir un archivo .txt en disco
FILES=$(find /home/JLANDAETA/com -type f -name "*.java")

# Compilar forzando a javac a tratar las fuentes como UTF-8 o ISO-8859-1
javac -encoding UTF-8 \
      -cp "/home/JLANDAETA/bankesb/lib/*:/home/JLANDAETA/lib/*" \
      -d /home/JLANDAETA/bankesb/bin/out \
      $FILES

echo "--- Estado de compilacion: $? ---"
