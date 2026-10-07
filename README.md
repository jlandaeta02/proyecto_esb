# proyecto_esb

1. Estructura de Directorios en el IFS (/opt/bankesb/)
La ruta base recomendada en el IFS es /opt/bankesb/ (o /apps/bankesb/). Esta estructura facilita los permisos de seguridad y la integración con comandos nativos de IBM i:

<img width="744" height="539" alt="imagen" src="https://github.com/user-attachments/assets/9e1bee34-1df4-4b24-ae65-b11ac668ac13" />

2. Estructura de Paquetes/Módulos dentro del bank-esb-core.jar
Dentro del archivo JAR principal, el código Java 17 debe estructurarse modularmente por capas de responsabilidad para facilitar el mantenimiento y la escalabilidad del bus:

<img width="738" height="807" alt="imagen" src="https://github.com/user-attachments/assets/9510b0f4-c63f-48fb-a93e-fb557cdfedaf" />
