# Cobblemon-PAPI Elite Bridge

**Cobblemon-PAPI Elite Bridge** es un puente híbrido (Fabric Mod + Bukkit Plugin) diseñado específicamente para servidores **Arclight 1.21.1** que ejecutan **Cobblemon 1.7.1**. 

Este proyecto expone datos internos del mod de Cobblemon hacia el sistema de **PlaceholderAPI** (PAPI) de manera segura, rápida y sin causar bloqueos (crasheos) o lag en el servidor, utilizando consultas optimizadas que omiten el denso ecosistema de NMS de Bukkit para acceder directamente a la capa de Fabric.

---

## 🔥 Características Principales

*   **Soporte Arclight Nativo:** Soluciona los problemas comunes de ClassLoaders entre Bukkit y Fabric al ejecutar expansiones de PAPI en servidores híbridos.
*   **Gestión Dinámica de Party (Slots 1-6):** Accede a estadísticas en vivo y atributos de cualquier Pokémon en el equipo del jugador.
*   **Manejo de Errores Anti-Crasheos:** Si un slot está vacío, el jugador está desconectado o el mod se actualiza, la API devuelve mensajes personalizables (ej: "Vacio", "Error") en lugar de arrojar valores nulos (`null`) que crashean los menús.
*   **Optimización de Rendimiento:** La obtención de datos complejos (Pokedex de jugadores, datos del PC, etc.) evita utilizar la capa Bukkit/Spigot para leer directamente de las interfaces y colecciones de Fabric.

---

## 📊 Placeholders Soportados

A continuación tienes una lista de los Placeholders que este puente proporciona (Todos inician con `%cobblemon_...%`):

### Equipo del Jugador (Party)
Extrae datos del Pokémon en un slot concreto (del 1 al 6).
*   `%cobblemon_party_slot_<1-6>_species%` - Nombre de la especie (ej. Pikachu).
*   `%cobblemon_party_slot_<1-6>_level%` - Nivel actual del Pokémon.
*   `%cobblemon_party_slot_<1-6>_nickname%` - Mote o apodo del Pokémon (Si no tiene, devuelve "Vacio").
*   `%cobblemon_party_slot_<1-6>_ability%` - Habilidad del Pokémon.
*   `%cobblemon_party_slot_<1-6>_nature%` - Naturaleza del Pokémon.
*   `%cobblemon_party_slot_<1-6>_is_shiny%` - Símbolo configurado para indicar si es Shiny o no.
*   `%cobblemon_party_slot_<1-6>_moveset%` - Lista de los movimientos actuales separados por comas.
*   `%cobblemon_party_slot_<1-6>_ivs_percent%` - Porcentaje total de IVs (0% a 100%).
*   `%cobblemon_party_slot_<1-6>_evs_total%` - Suma total de los EVs acumulados.

### Globales (Especies de Pokémon)
*   `%cobblemon_species_types_<nombre_pokemon>%` - Tipos del Pokémon (Ej. `%cobblemon_species_types_charizard%` -> Fire/Flying).
*   `%cobblemon_species_abilities_<nombre_pokemon>%` - Todas las habilidades posibles de la especie.

### Pokédex / Estadísticas Generales
*   `%cobblemon_pokedex_caught_count%` - Cantidad de variantes o especies de Pokémon atrapados.
*   `%cobblemon_pokedex_caught_percent%` - Porcentaje de la Pokédex completada respecto a los Pokémon implementados actualmente en el mod.
*   `%cobblemon_stats_caught_total%` - Número total de todos los Pokémon atrapados por el jugador (ideal para Pases de Batalla RPG).

---

## ⚙️ Instalación

1.  Asegúrate de tener un servidor híbrido en Minecraft **1.21.1** (como Arclight).
2.  Asegúrate de tener instalados **Cobblemon 1.7.1** y **PlaceholderAPI**.
3.  Coloca el archivo `.jar` compilado dentro de la carpeta `plugins/` (y si es necesario, asegúrate de que el loader o sistema de Fabric del servidor híbrido también pueda escanearlo, ya que tiene formato de Mod de Fabric interno).
4.  Inicia el servidor. La expansión de Placeholders se registrará automáticamente.

## 🛠️ Compilación

El proyecto utiliza **Gradle**. Para compilar tu propia versión o integrar cambios:

```bash
./gradlew build
```

El artefacto generado estará en `build/libs/`. Recuerda que cada vez que compiles de manera oficial debes aumentar el número de la versión (`pluginVersion`) en el archivo `gradle.properties`.
