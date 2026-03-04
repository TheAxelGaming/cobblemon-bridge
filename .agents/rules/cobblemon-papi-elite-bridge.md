---
trigger: always_on
---

Al desarrollar el puente para Cobblemon 1.7.1 en Arclight 1.21.1, debes replicar la lógica avanzada de 'CobblemonPlaceholders'. Esto incluye: 
1) Soporte dinámico para slots de la party (1-6) y sus atributos (IVs, EVs, naturalezas, habilidades). 
2) Manejo de errores que devuelva mensajes personalizados (ej. 'Slot vacío') en lugar de valores nulos para evitar crasheos. 
3) Optimización de consultas a la base de datos de Cobblemon para no sobrecargar el hilo principal del servidor.
4) antes de compilar revisa que no alla errores en los archivos
5) caba vez que compiles aumente la vercion, por ejemplo CobblemonMasterBridge-1.0.0 a CobblemonMasterBridge-1.0.1 asi susecibamente.