package com.cobbleplay.bridge;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;

import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/**
 * ══════════════════════════════════════════════════════════════════════════════
 * CobblemonBridgeExpansion — Expansión PlaceholderAPI
 * ══════════════════════════════════════════════════════════════════════════════
 *
 * Expone TODOS los datos de Cobblemon como placeholders PAPI para uso con
 * BattlePass y cualquier plugin compatible con PlaceholderAPI en Arclight.
 *
 * Identificador: %cobblemonbridge_...%
 *
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │ MÓDULO POKÉDEX │
 * │ pokedex_caught → Total de Pokémon capturados │
 * │ pokedex_seen → Total de Pokémon vistos │
 * │ pokedex_percentage → Porcentaje de completitud │
 * │ pokedex_shinies → Total de shinies registrados │
 * ├─────────────────────────────────────────────────────────────────────┤
 * │ MÓDULO PARTY (Slots 1-6) │
 * │ party_slot_X_species → Nombre de la especie │
 * │ party_slot_X_level → Nivel │
 * │ party_slot_X_friendship→ Nivel de amistad │
 * │ party_slot_X_shiny → true / false │
 * │ party_slot_X_gender → Male / Female / Genderless │
 * │ party_slot_X_nature → Nombre de la naturaleza │
 * │ party_slot_X_ability → Nombre de la habilidad │
 * │ party_slot_X_held_item → Nombre del objeto equipado o "None" │
 * │ party_slot_X_ivs_total → Suma total de IVs (0-186) │
 * │ party_slot_X_evs_total → Suma total de EVs (0-510) │
 * │ party_slot_X_ivs_{stat}→ IV individual (hp/atk/def/spatk/...) │
 * │ party_slot_X_evs_{stat}→ EV individual (hp/atk/def/spatk/...) │
 * ├─────────────────────────────────────────────────────────────────────┤
 * │ MÓDULO ESTADÍSTICAS │
 * │ stats_battles_won → Victorias totales │
 * │ stats_battles_lost → Derrotas totales │
 * │ stats_win_rate → Ratio de victoria (%) │
 * └─────────────────────────────────────────────────────────────────────┘
 *
 * Seguridad: Cada consulta implementa null-checks agresivos y try-catch
 * para evitar NullPointerException. Si un dato no está disponible,
 * devuelve "None" (textos) o "0" (números).
 *
 * ══════════════════════════════════════════════════════════════════════════════
 */
public class CobblemonBridgeExpansion extends PlaceholderExpansion {

    // ══════════════════════════════════════════════════════════════
    // METADATOS DE LA EXPANSIÓN
    // ══════════════════════════════════════════════════════════════

    @Override
    public @NotNull String getIdentifier() {
        return "cobblemonbridge";
    }

    @Override
    public @NotNull String getAuthor() {
        return "CobblePlay";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    /**
     * Mantener la expansión registrada incluso si el plugin se recarga.
     */
    @Override
    public boolean persist() {
        return true;
    }

    /**
     * No requerir un plugin específico para registrar — el mod se
     * encarga de la disponibilidad de Cobblemon.
     */
    @Override
    public boolean canRegister() {
        return true;
    }

    // ══════════════════════════════════════════════════════════════
    // ROUTER PRINCIPAL DE PLACEHOLDERS
    // ══════════════════════════════════════════════════════════════

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String identifier) {
        // ── Protección: jugador nulo ──
        if (player == null) {
            return "N/A";
        }

        try {
            // ── MÓDULO POKÉDEX ──
            if (identifier.startsWith("pokedex_")) {
                return handlePokedex(player, identifier);
            }

            // ── MÓDULO PARTY ──
            if (identifier.startsWith("party_slot_")) {
                return handleParty(player, identifier);
            }

            // ── MÓDULO ESTADÍSTICAS ──
            if (identifier.startsWith("stats_")) {
                return handleStats(player, identifier);
            }

        } catch (Exception e) {
            // Protección global: cualquier error no capturado devuelve "0"
            CobblemonBridgeMod.LOGGER.debug(
                    "[CobblemonBridge] Error procesando placeholder '{}': {}",
                    identifier, e.getMessage());
            return "0";
        }

        // Placeholder no reconocido — devolver null para que PAPI lo ignore
        return null;
    }

    // ══════════════════════════════════════════════════════════════
    // MÓDULO A: POKÉDEX
    // ══════════════════════════════════════════════════════════════

    /**
     * Maneja todos los placeholders que empiezan con "pokedex_".
     * Usa reflexión segura para acceder al sistema de Pokédex de Cobblemon,
     * ya que la API puede variar entre versiones.
     */
    private String handlePokedex(Player player, String identifier) {
        try {
            UUID uuid = player.getUniqueId();

            // ── Obtener los datos del jugador desde Cobblemon ──
            Object playerDataManager = invocarMetodo(Cobblemon.INSTANCE, "getPlayerData");
            if (playerDataManager == null) {
                return "0";
            }

            // ── Obtener PlayerData del UUID ──
            Object playerData = invocarMetodo(playerDataManager, "get", uuid);
            if (playerData == null) {
                return "0";
            }

            // ── Acceder al Pokédex del jugador ──
            // Cobblemon 1.6.x: PlayerData tiene una propiedad "pokedex" (PokedexManager)
            Object pokedex = invocarGetter(playerData, "pokedex");
            if (pokedex == null) {
                // Fallback: intentar con "getDex" o "getPokedexData"
                pokedex = invocarGetter(playerData, "dex");
            }

            switch (identifier) {
                case "pokedex_caught": {
                    // Total de Pokémon capturados
                    Object resultado = invocarMetodoSeguro(pokedex,
                            "getCaughtCount", "caughtCount", "getCaughtSpeciesAmount");
                    return resultado != null ? String.valueOf(resultado) : "0";
                }

                case "pokedex_seen": {
                    // Total de Pokémon vistos
                    Object resultado = invocarMetodoSeguro(pokedex,
                            "getSeenCount", "seenCount", "getSeenSpeciesAmount");
                    return resultado != null ? String.valueOf(resultado) : "0";
                }

                case "pokedex_percentage": {
                    // Porcentaje de completitud de la Pokédex
                    // Intentar método directo primero
                    Object porcentaje = invocarMetodoSeguro(pokedex,
                            "getCaughtPercentage", "getCompletionPercentage", "caughtPercent");
                    if (porcentaje != null) {
                        double valor;
                        if (porcentaje instanceof Number) {
                            valor = ((Number) porcentaje).doubleValue();
                        } else {
                            valor = Double.parseDouble(String.valueOf(porcentaje));
                        }
                        return String.valueOf(Math.round(valor));
                    }

                    // Fallback: calcular manualmente caught/totalSpecies * 100
                    Object caught = invocarMetodoSeguro(pokedex,
                            "getCaughtCount", "caughtCount", "getCaughtSpeciesAmount");
                    if (caught != null) {
                        int capturados = ((Number) caught).intValue();
                        // Total de especies en Cobblemon (aproximadamente)
                        int totalEspecies = obtenerTotalEspecies();
                        if (totalEspecies > 0) {
                            double pct = (capturados * 100.0) / totalEspecies;
                            return String.valueOf(Math.round(pct));
                        }
                    }
                    return "0";
                }

                case "pokedex_shinies": {
                    // Total de shinies registrados en la Pokédex
                    Object resultado = invocarMetodoSeguro(pokedex,
                            "getShinyCaughtCount", "getShinyCount", "shinyCaughtCount");
                    return resultado != null ? String.valueOf(resultado) : "0";
                }

                default:
                    return null;
            }

        } catch (Exception e) {
            CobblemonBridgeMod.LOGGER.debug(
                    "[CobblemonBridge] Error en módulo Pokédex: {}", e.getMessage());
            return "0";
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MÓDULO B: PARTY (Equipo Pokémon — Slots 1 a 6)
    // ══════════════════════════════════════════════════════════════

    /**
     * Maneja todos los placeholders que empiezan con "party_slot_".
     * Formato: party_slot_X_propiedad
     * Donde X = 1-6 (slot del equipo, se traduce a 0-5 internamente).
     */
    private String handleParty(Player player, String identifier) {
        try {
            // ── Parsear el número de slot y la propiedad ──
            // Ejemplo: "party_slot_3_level" → slot=3, propiedad="level"
            String sinPrefijo = identifier.substring("party_slot_".length());
            int primerGuionBajo = sinPrefijo.indexOf('_');
            if (primerGuionBajo <= 0)
                return "None";

            int slotNumero;
            try {
                slotNumero = Integer.parseInt(sinPrefijo.substring(0, primerGuionBajo));
            } catch (NumberFormatException e) {
                return "None";
            }

            // Validar rango de slot (1-6)
            if (slotNumero < 1 || slotNumero > 6) {
                return "None";
            }

            String propiedad = sinPrefijo.substring(primerGuionBajo + 1);
            int slotIndex = slotNumero - 1; // Conversión a 0-indexed

            // ── Obtener el ServerPlayer de Minecraft ──
            ServerPlayer serverPlayer = obtenerServerPlayer(player);
            if (serverPlayer == null) {
                return "None";
            }

            // ── Obtener el Party del jugador ──
            PlayerPartyStore party = obtenerPartyStore(serverPlayer);
            if (party == null) {
                return "None";
            }

            // ── Obtener el Pokémon en el slot ──
            Pokemon pokemon = obtenerPokemonEnSlot(party, slotIndex);
            if (pokemon == null) {
                // Slot vacío — devolver valor seguro según el tipo de propiedad
                return esNumerico(propiedad) ? "0" : "None";
            }

            // ── Resolver la propiedad solicitada ──
            return resolverPropiedadPokemon(pokemon, propiedad);

        } catch (Exception e) {
            CobblemonBridgeMod.LOGGER.debug(
                    "[CobblemonBridge] Error en módulo Party: {}", e.getMessage());
            return "None";
        }
    }

    /**
     * Resuelve una propiedad específica de un Pokémon.
     * Usa switch-case ramificado para fácil extensión futura.
     */
    private String resolverPropiedadPokemon(Pokemon pokemon, String propiedad) {
        try {
            switch (propiedad) {

                // ── Datos básicos ──
                case "species":
                    return obtenerNombreEspecie(pokemon);

                case "level":
                    return String.valueOf(pokemon.getLevel());

                case "friendship":
                    return String.valueOf(pokemon.getFriendship());

                case "shiny":
                    return String.valueOf(pokemon.getShiny());

                case "gender":
                    return obtenerGenero(pokemon);

                case "nature":
                    return obtenerNaturaleza(pokemon);

                case "ability":
                    return obtenerHabilidad(pokemon);

                case "held_item":
                    return obtenerObjetoEquipado(pokemon);

                // ── IVs Totales ──
                case "ivs_total":
                    return String.valueOf(calcularIVsTotal(pokemon));

                // ── IVs Individuales ──
                case "ivs_hp":
                    return String.valueOf(obtenerIV(pokemon, Stats.HP));
                case "ivs_atk":
                    return String.valueOf(obtenerIV(pokemon, Stats.ATTACK));
                case "ivs_def":
                    return String.valueOf(obtenerIV(pokemon, Stats.DEFENCE));
                case "ivs_spatk":
                    return String.valueOf(obtenerIV(pokemon, Stats.SPECIAL_ATTACK));
                case "ivs_spdef":
                    return String.valueOf(obtenerIV(pokemon, Stats.SPECIAL_DEFENCE));
                case "ivs_spe":
                    return String.valueOf(obtenerIV(pokemon, Stats.SPEED));

                // ── EVs Totales ──
                case "evs_total":
                    return String.valueOf(calcularEVsTotal(pokemon));

                // ── EVs Individuales ──
                case "evs_hp":
                    return String.valueOf(obtenerEV(pokemon, Stats.HP));
                case "evs_atk":
                    return String.valueOf(obtenerEV(pokemon, Stats.ATTACK));
                case "evs_def":
                    return String.valueOf(obtenerEV(pokemon, Stats.DEFENCE));
                case "evs_spatk":
                    return String.valueOf(obtenerEV(pokemon, Stats.SPECIAL_ATTACK));
                case "evs_spdef":
                    return String.valueOf(obtenerEV(pokemon, Stats.SPECIAL_DEFENCE));
                case "evs_spe":
                    return String.valueOf(obtenerEV(pokemon, Stats.SPEED));

                default:
                    return "None";
            }
        } catch (Exception e) {
            CobblemonBridgeMod.LOGGER.debug(
                    "[CobblemonBridge] Error resolviendo propiedad '{}': {}",
                    propiedad, e.getMessage());
            return esNumerico(propiedad) ? "0" : "None";
        }
    }

    // ══════════════════════════════════════════════════════════════
    // MÓDULO C: ESTADÍSTICAS DE COMBATE
    // ══════════════════════════════════════════════════════════════

    /**
     * Maneja todos los placeholders que empiezan con "stats_".
     * Accede a los datos de combate del jugador en Cobblemon.
     */
    private String handleStats(Player player, String identifier) {
        try {
            UUID uuid = player.getUniqueId();

            // ── Obtener datos del jugador de Cobblemon ──
            Object playerDataManager = invocarMetodo(Cobblemon.INSTANCE, "getPlayerData");
            if (playerDataManager == null)
                return "0";

            Object playerData = invocarMetodo(playerDataManager, "get", uuid);
            if (playerData == null)
                return "0";

            switch (identifier) {
                case "stats_battles_won": {
                    Object victorias = invocarMetodoSeguro(playerData,
                            "getBattleVictoryCount", "getVictoryCount",
                            "getBattlesWon", "battleVictoryCount");
                    return victorias != null ? String.valueOf(victorias) : "0";
                }

                case "stats_battles_lost": {
                    Object derrotas = invocarMetodoSeguro(playerData,
                            "getBattleLossCount", "getLossCount",
                            "getBattlesLost", "battleLossCount");
                    return derrotas != null ? String.valueOf(derrotas) : "0";
                }

                case "stats_win_rate": {
                    // Calcular ratio: victorias / (victorias + derrotas) * 100
                    Object vObj = invocarMetodoSeguro(playerData,
                            "getBattleVictoryCount", "getVictoryCount",
                            "getBattlesWon", "battleVictoryCount");
                    Object dObj = invocarMetodoSeguro(playerData,
                            "getBattleLossCount", "getLossCount",
                            "getBattlesLost", "battleLossCount");

                    int victorias = vObj != null ? ((Number) vObj).intValue() : 0;
                    int derrotas = dObj != null ? ((Number) dObj).intValue() : 0;
                    int total = victorias + derrotas;

                    if (total == 0)
                        return "0";
                    double ratio = (victorias * 100.0) / total;
                    return String.valueOf(Math.round(ratio));
                }

                default:
                    return null;
            }

        } catch (Exception e) {
            CobblemonBridgeMod.LOGGER.debug(
                    "[CobblemonBridge] Error en módulo Stats: {}", e.getMessage());
            return "0";
        }
    }

    // ══════════════════════════════════════════════════════════════
    // HELPERS: ACCESO A DATOS POKÉMON
    // ══════════════════════════════════════════════════════════════

    /**
     * Obtiene el nombre de la especie del Pokémon.
     */
    private String obtenerNombreEspecie(Pokemon pokemon) {
        try {
            if (pokemon.getSpecies() == null)
                return "None";
            // En Cobblemon, Species tiene getName() que devuelve el nombre localizado
            String nombre = pokemon.getSpecies().getName();
            return (nombre != null && !nombre.isEmpty()) ? nombre : "None";
        } catch (Exception e) {
            return "None";
        }
    }

    /**
     * Obtiene el género del Pokémon como texto legible.
     */
    private String obtenerGenero(Pokemon pokemon) {
        try {
            Object gender = pokemon.getGender();
            if (gender == null)
                return "Genderless";
            return gender.toString();
        } catch (Exception e) {
            return "Genderless";
        }
    }

    /**
     * Obtiene el nombre de la naturaleza del Pokémon.
     */
    private String obtenerNaturaleza(Pokemon pokemon) {
        try {
            Object nature = pokemon.getNature();
            if (nature == null)
                return "None";
            // Nature tiene getName() o toString()
            Object nombre = invocarMetodoSeguro(nature, "getName", "getDisplayName");
            return nombre != null ? String.valueOf(nombre) : nature.toString();
        } catch (Exception e) {
            return "None";
        }
    }

    /**
     * Obtiene el nombre de la habilidad del Pokémon.
     */
    private String obtenerHabilidad(Pokemon pokemon) {
        try {
            Object ability = pokemon.getAbility();
            if (ability == null)
                return "None";
            // Ability o AbilityWrapper pueden tener getName(), getDisplayName(), etc.
            Object nombre = invocarMetodoSeguro(ability, "getName", "getDisplayName");
            return nombre != null ? String.valueOf(nombre) : ability.toString();
        } catch (Exception e) {
            return "None";
        }
    }

    /**
     * Obtiene el nombre del objeto equipado, o "None" si no tiene ninguno.
     */
    private String obtenerObjetoEquipado(Pokemon pokemon) {
        try {
            // En Cobblemon, heldItem() devuelve un ItemStack de Minecraft
            Object heldItem = invocarMetodo(pokemon, "heldItem");
            if (heldItem == null)
                return "None";

            // Verificar si el ItemStack está vacío
            Object isEmpty = invocarMetodo(heldItem, "isEmpty");
            if (isEmpty != null && Boolean.TRUE.equals(isEmpty)) {
                return "None";
            }

            // Obtener el nombre del item
            Object displayName = invocarMetodoSeguro(heldItem,
                    "getDisplayName", "getHoverName", "getName");
            if (displayName != null) {
                String nombre = displayName.toString();
                // Limpiar formato de texto de Minecraft (quitar códigos de color)
                nombre = nombre.replaceAll("§[0-9a-fk-or]", "");
                // Limpiar componentes de texto literal
                nombre = nombre.replaceAll("\\[|\\]|literal\\{|\\}", "");
                return nombre.isEmpty() ? "None" : nombre;
            }

            // Fallback: obtener ID del registro
            Object item = invocarMetodo(heldItem, "getItem");
            return item != null ? item.toString() : "None";

        } catch (Exception e) {
            return "None";
        }
    }

    // ══════════════════════════════════════════════════════════════
    // HELPERS: IVs y EVs
    // ══════════════════════════════════════════════════════════════

    /**
     * Obtiene el IV de una estadística específica.
     *
     * @param pokemon El Pokémon a consultar
     * @param stat    La estadística (Stats.HP, Stats.ATTACK, etc.)
     * @return Valor del IV (0-31), o 0 si no se puede obtener
     */
    private int obtenerIV(Pokemon pokemon, Stats stat) {
        try {
            Object ivs = pokemon.getIvs();
            if (ivs == null)
                return 0;

            // IVs implementa una interfaz mapa/acceso por Stats
            Object valor = invocarMetodo(ivs, "get", stat);
            if (valor == null) {
                // Fallback: intentar getOrDefault
                valor = invocarMetodo(ivs, "getOrDefault", stat, 0);
            }
            return valor != null ? ((Number) valor).intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Obtiene el EV de una estadística específica.
     *
     * @param pokemon El Pokémon a consultar
     * @param stat    La estadística
     * @return Valor del EV (0-252), o 0 si no se puede obtener
     */
    private int obtenerEV(Pokemon pokemon, Stats stat) {
        try {
            Object evs = pokemon.getEvs();
            if (evs == null)
                return 0;

            Object valor = invocarMetodo(evs, "get", stat);
            if (valor == null) {
                valor = invocarMetodo(evs, "getOrDefault", stat, 0);
            }
            return valor != null ? ((Number) valor).intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Calcula la suma total de todos los IVs (máximo teórico: 186).
     */
    private int calcularIVsTotal(Pokemon pokemon) {
        int total = 0;
        total += obtenerIV(pokemon, Stats.HP);
        total += obtenerIV(pokemon, Stats.ATTACK);
        total += obtenerIV(pokemon, Stats.DEFENCE);
        total += obtenerIV(pokemon, Stats.SPECIAL_ATTACK);
        total += obtenerIV(pokemon, Stats.SPECIAL_DEFENCE);
        total += obtenerIV(pokemon, Stats.SPEED);
        return total;
    }

    /**
     * Calcula la suma total de todos los EVs (máximo teórico: 510).
     */
    private int calcularEVsTotal(Pokemon pokemon) {
        int total = 0;
        total += obtenerEV(pokemon, Stats.HP);
        total += obtenerEV(pokemon, Stats.ATTACK);
        total += obtenerEV(pokemon, Stats.DEFENCE);
        total += obtenerEV(pokemon, Stats.SPECIAL_ATTACK);
        total += obtenerEV(pokemon, Stats.SPECIAL_DEFENCE);
        total += obtenerEV(pokemon, Stats.SPEED);
        return total;
    }

    // ══════════════════════════════════════════════════════════════
    // HELPERS: CONVERSIÓN BUKKIT → NMS → COBBLEMON
    // ══════════════════════════════════════════════════════════════

    /**
     * Convierte un Player de Bukkit al ServerPlayer de Minecraft/NMS.
     * Usa reflexión para compatibilidad con Arclight (que puede remapear
     * los paquetes de CraftBukkit).
     *
     * @param bukkitPlayer El jugador de Bukkit
     * @return El ServerPlayer de NMS, o null si falla
     */
    private ServerPlayer obtenerServerPlayer(Player bukkitPlayer) {
        try {
            // CraftPlayer.getHandle() devuelve el ServerPlayer de NMS
            Method getHandle = bukkitPlayer.getClass().getMethod("getHandle");
            Object nmsPlayer = getHandle.invoke(bukkitPlayer);

            if (nmsPlayer instanceof ServerPlayer) {
                return (ServerPlayer) nmsPlayer;
            }

            // Si el cast directo falla (Arclight puede usar clases bridge),
            // intentar cast dinámico
            return (ServerPlayer) nmsPlayer;

        } catch (Exception e) {
            CobblemonBridgeMod.LOGGER.debug(
                    "[CobblemonBridge] Error obteniendo ServerPlayer de '{}': {}",
                    bukkitPlayer.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene el PlayerPartyStore de Cobblemon para un ServerPlayer.
     * Verifica que el almacenamiento esté cargado antes de acceder.
     *
     * @param serverPlayer El jugador del servidor
     * @return El PartyStore, o null si no está disponible
     */
    private PlayerPartyStore obtenerPartyStore(ServerPlayer serverPlayer) {
        try {
            if (serverPlayer == null)
                return null;

            // Cobblemon.INSTANCE.getStorage() → PokemonStoreManager
            // PokemonStoreManager.getParty(ServerPlayer) → PlayerPartyStore
            return Cobblemon.INSTANCE.getStorage().getParty(serverPlayer);

        } catch (Exception e) {
            CobblemonBridgeMod.LOGGER.debug(
                    "[CobblemonBridge] Error obteniendo PartyStore: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene un Pokémon de un slot específico del Party.
     * Intenta múltiples métodos de acceso por compatibilidad.
     *
     * @param party El almacenamiento del equipo
     * @param slot  Índice del slot (0-5)
     * @return El Pokémon, o null si el slot está vacío
     */
    private Pokemon obtenerPokemonEnSlot(PlayerPartyStore party, int slot) {
        try {
            if (party == null || slot < 0 || slot > 5)
                return null;

            // ── Método 1: Usar toGappyList() que devuelve lista con nulls ──
            try {
                @SuppressWarnings("unchecked")
                List<Pokemon> listaGappy = (List<Pokemon>) invocarMetodo(party, "toGappyList");
                if (listaGappy != null && slot < listaGappy.size()) {
                    return listaGappy.get(slot);
                }
            } catch (Exception e1) {
                // Intentar método alternativo
            }

            // ── Método 2: Usar get(int) si existe ──
            try {
                Method getByInt = party.getClass().getMethod("get", int.class);
                Object result = getByInt.invoke(party, slot);
                return result instanceof Pokemon ? (Pokemon) result : null;
            } catch (NoSuchMethodException e2) {
                // Intentar con PartyPosition
            }

            // ── Método 3: Usar get(PartyPosition) vía reflexión ──
            try {
                Class<?> partyPosClass = Class.forName(
                        "com.cobblemon.mod.common.api.storage.party.PartyPosition");
                Object position = partyPosClass.getConstructor(int.class).newInstance(slot);
                Method getByPos = party.getClass().getMethod("get", partyPosClass.getSuperclass());

                // Si getByPos no funciona con la superclase, intentar con la clase directa
                Object result;
                try {
                    result = getByPos.invoke(party, position);
                } catch (Exception e3a) {
                    getByPos = party.getClass().getMethod("get", partyPosClass);
                    result = getByPos.invoke(party, position);
                }
                return result instanceof Pokemon ? (Pokemon) result : null;
            } catch (Exception e3) {
                // Todos los métodos fallaron
            }

            return null;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Obtiene el total de especies registradas en Cobblemon.
     * Usado para calcular el porcentaje de la Pokédex.
     */
    private int obtenerTotalEspecies() {
        try {
            // Intentar obtener el conteo total de especies desde el registro de Cobblemon
            Object speciesRegistry = invocarMetodoSeguro(Cobblemon.INSTANCE,
                    "getSpecies", "getSpeciesProvider");
            if (speciesRegistry != null) {
                Object count = invocarMetodoSeguro(speciesRegistry,
                        "getCount", "count", "size", "getSpeciesCount");
                if (count != null) {
                    return ((Number) count).intValue();
                }

                // Intentar obtener una colección y contar
                Object all = invocarMetodoSeguro(speciesRegistry,
                        "getAll", "getSpecies", "values");
                if (all instanceof java.util.Collection) {
                    return ((java.util.Collection<?>) all).size();
                }
            }

            // Fallback: valor aproximado de especies totales en Cobblemon
            return 905;
        } catch (Exception e) {
            return 905;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // UTILIDADES DE REFLEXIÓN SEGURA
    // ══════════════════════════════════════════════════════════════

    /**
     * Determina si una propiedad debería devolver un valor numérico.
     * Usado para decidir si el fallback es "0" o "None".
     */
    private boolean esNumerico(String propiedad) {
        return propiedad.contains("level") || propiedad.contains("friendship")
                || propiedad.contains("ivs") || propiedad.contains("evs")
                || propiedad.contains("total");
    }

    /**
     * Invoca un método sin argumentos en un objeto usando reflexión.
     *
     * @param objeto El objeto sobre el cual invocar
     * @param metodo El nombre del método
     * @return El resultado, o null si falla
     */
    private Object invocarMetodo(Object objeto, String metodo) {
        if (objeto == null)
            return null;
        try {
            Method m = objeto.getClass().getMethod(metodo);
            return m.invoke(objeto);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Invoca un método con un argumento UUID usando reflexión.
     */
    private Object invocarMetodo(Object objeto, String metodo, UUID uuid) {
        if (objeto == null)
            return null;
        try {
            Method m = objeto.getClass().getMethod(metodo, UUID.class);
            return m.invoke(objeto, uuid);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Invoca un método con un argumento Stats usando reflexión.
     */
    private Object invocarMetodo(Object objeto, String metodo, Stats stat) {
        if (objeto == null)
            return null;
        try {
            // Intentar primero con la clase concreta Stats
            for (Method m : objeto.getClass().getMethods()) {
                if (m.getName().equals(metodo) && m.getParameterCount() == 1) {
                    try {
                        return m.invoke(objeto, stat);
                    } catch (Exception e) {
                        // Intentar siguiente método con el mismo nombre
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Invoca un método con un argumento Stats y un valor por defecto usando
     * reflexión.
     */
    private Object invocarMetodo(Object objeto, String metodo, Stats stat, int valorDefecto) {
        if (objeto == null)
            return null;
        try {
            for (Method m : objeto.getClass().getMethods()) {
                if (m.getName().equals(metodo) && m.getParameterCount() == 2) {
                    try {
                        return m.invoke(objeto, stat, valorDefecto);
                    } catch (Exception e) {
                        // Continuar
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Invoca un getter de propiedad de Kotlin.
     * Intenta tanto "getPropiedad" como "propiedad" como "isPropiedad".
     */
    private Object invocarGetter(Object objeto, String propiedad) {
        if (objeto == null)
            return null;

        // Intentar getPropiedad()
        String getter = "get" + propiedad.substring(0, 1).toUpperCase() + propiedad.substring(1);
        Object resultado = invocarMetodo(objeto, getter);
        if (resultado != null)
            return resultado;

        // Intentar propiedad() directamente (Kotlin)
        resultado = invocarMetodo(objeto, propiedad);
        if (resultado != null)
            return resultado;

        // Intentar isPropiedad() (para booleanos)
        String isGetter = "is" + propiedad.substring(0, 1).toUpperCase() + propiedad.substring(1);
        return invocarMetodo(objeto, isGetter);
    }

    /**
     * Intenta invocar uno de múltiples nombres de método en orden.
     * Devuelve el resultado del primer método que funcione.
     *
     * Útil cuando no sabemos el nombre exacto del método en
     * la versión actual de Cobblemon.
     *
     * @param objeto  El objeto sobre el cual invocar
     * @param metodos Lista ordenada de nombres de método a intentar
     * @return El resultado del primer método exitoso, o null
     */
    private Object invocarMetodoSeguro(Object objeto, String... metodos) {
        if (objeto == null)
            return null;
        for (String metodo : metodos) {
            Object resultado = invocarMetodo(objeto, metodo);
            if (resultado != null)
                return resultado;

            // Intentar con prefijo "get" si no lo tiene
            if (!metodo.startsWith("get") && !metodo.startsWith("is")) {
                String conGet = "get" + metodo.substring(0, 1).toUpperCase() + metodo.substring(1);
                resultado = invocarMetodo(objeto, conGet);
                if (resultado != null)
                    return resultado;
            }
        }
        return null;
    }
}
