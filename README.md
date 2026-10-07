# proyecto_esb

1. Estructura de Directorios en el IFS (/opt/bankesb/)
La ruta base recomendada en el IFS es /opt/bankesb/ (o /apps/bankesb/). Esta estructura facilita los permisos de seguridad y la integración con comandos nativos de IBM i:

<img width="744" height="539" alt="imagen" src="https://github.com/user-attachments/assets/9e1bee34-1df4-4b24-ae65-b11ac668ac13" />

2. Estructura de Paquetes/Módulos dentro del bank-esb-core.jar
Dentro del archivo JAR principal, el código Java 17 debe estructurarse modularmente por capas de responsabilidad para facilitar el mantenimiento y la escalabilidad del bus:

<img width="738" height="807" alt="imagen" src="https://github.com/user-attachments/assets/9510b0f4-c63f-48fb-a93e-fb557cdfedaf" />

3. Configuración del Entorno de Ejecución en IBM i
A. Archivo de Parámetros (/opt/bankesb/config/application.properties)

<img width="738" height="477" alt="imagen" src="https://github.com/user-attachments/assets/9bfffbe2-abcb-4e9a-8c50-7494e00da7df" />

ESTRUCTURA DE DATOS
1. Tablas Nativas de Prueba en DB2/400 (SYS_ACCOUNTS y SYS_TRANSACTIONS)
Para ejecutar la lógica, se asume la existencia de las tablas de cuentas y movimientos en la biblioteca de negocio (ej. ESBLIB):

<img width="747" height="463" alt="imagen" src="https://github.com/user-attachments/assets/24a94837-c2ef-460d-ab69-7fa5d52d1555" />







