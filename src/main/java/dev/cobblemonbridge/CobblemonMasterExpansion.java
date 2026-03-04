package dev.cobblemonbridge;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;
import org.bukkit.entity.Player;

public class CobblemonMasterExpansion extends PlaceholderExpansion {

    private final CobblemonMasterBridge plugin;

    public CobblemonMasterExpansion(CobblemonMasterBridge plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "cobblemon";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        // [MODULE A] - Species info (Global)
        if (params.startsWith("species_")) {
            return handleSpeciesPlaceholders(params);
        }

        // Requiere jugador activo para los siguientes módulos
        if (player == null || !player.isOnline()) {
            return "";
        }

        // [MODULE B] - Party info
        if (params.startsWith("party_slot_")) {
            return handlePartyPlaceholders(player, params);
        }

        // [MODULE C] - Pokédex info
        if (params.startsWith("pokedex_")) {
            return handlePokedexPlaceholders(player, params);
        }

        // [MODULE D] - Statistics (BattlePass)
        if (params.startsWith("stats_")) {
            return handleStatsPlaceholders(player, params);
        }

        return null;
    }

    // --- MODULE A: SPECIES ---
    private String handleSpeciesPlaceholders(String params) {
        // cobblemon_species_types_<especie>
        // cobblemon_species_abilities_<especie>

        String[] parts = params.split("_", 3);
        if (parts.length < 3)
            return null;

        String type = parts[1];
        String speciesName = parts[2];

        return executeWithFabricContext(() -> {
            Species species = PokemonSpecies.getByName(speciesName.toLowerCase());

            if (species == null) {
                return PluginConfig.getInvalidSpeciesMessage();
            }

            if ("types".equals(type)) {
                StringBuilder sb = new StringBuilder();
                for (com.cobblemon.mod.common.api.types.ElementalType t : species.getTypes()) {
                    if (sb.length() > 0)
                        sb.append("/");
                    sb.append(t.getName().substring(0, 1).toUpperCase()).append(t.getName().substring(1));
                }
                return sb.toString();
            } else if ("abilities".equals(type)) {
                // Fix generic mismatch: getMapping() returns Map<AbilitySlot, List<Ability>>
                // We want to flatten the list of lists and then map to names
                return species.getAbilities().getMapping().values().stream()
                        .flatMap(List::stream) // Flatten the List<Ability> from each slot
                        .map(a -> a.getTemplate().getName())
                        .map(name -> (String) (name.substring(0, 1).toUpperCase()
                                + name.substring(1).replace("_", " ")))
                        .collect(Collectors.joining(", "));
            }

            return null;
        }, PluginConfig.getInvalidSpeciesMessage());
    }

    // --- MODULE B: PARTY ---
    private String handlePartyPlaceholders(OfflinePlayer player, String params) {
        // cobblemon_party_slot_<slot>_<attribute>
        String[] parts = params.split("_", 4);
        if (parts.length < 4)
            return null;

        int slot;
        String slotStr = parts[2];
        try {
            slot = Integer.parseInt(slotStr) - 1; // 1-6 UI -> 0-5 Array
            if (slot < 0 || slot > 5)
                return "Vacio";
        } catch (NumberFormatException e) {
            return "Vacio";
        }

        String attribute = parts[3];

        return executeWithFabricContext(() -> {

            System.out.println("[CobblemonBridge] Procesando slot: " + parts[2] + " para el jugador: "
                    + (player.getName() != null ? player.getName() : player.getUniqueId().toString()));

            // Especial Caso: Atributos migrados al Hybrid Bridge
            if (attribute.equalsIgnoreCase("nickname")) {
                String nick = dev.cobblemonbridge.fabric.CobblemonDataService.getSlotNickname(player.getUniqueId(),
                        slot);
                System.out.println("[CobblemonBridge] Resultado de la reflexión: " + nick);
                if (nick == null || nick.equals("Empty Slot"))
                    return "Vacio";
                return nick;
            }

            if (attribute.equalsIgnoreCase("level")) {
                Integer lvl = dev.cobblemonbridge.fabric.CobblemonDataService.getSlotLevel(player.getUniqueId(), slot);
                System.out.println("[CobblemonBridge] Resultado de la reflexión: " + lvl);
                return lvl == null ? "Vacio" : String.valueOf(lvl);
            }

            if (attribute.equalsIgnoreCase("species")) {
                String sp = dev.cobblemonbridge.fabric.CobblemonDataService.getSlotSpecies(player.getUniqueId(), slot);
                System.out.println("[CobblemonBridge] Resultado de la reflexión: " + sp);
                return sp == null ? "Vacio" : sp;
            }

            Object party = getPartyStore(player);
            if (party == null) {
                System.out.println("[CobblemonBridge] Resultado de la reflexión: Error_Reflexion (Party Nula)");
                return "Error_Reflexion";
            }

            Pokemon pokemon = null;
            try {
                pokemon = (Pokemon) party.getClass().getMethod("get", int.class).invoke(party, slot);
            } catch (Exception e) {
                System.out.println("[CobblemonBridge] Resultado de la reflexión: Error_Reflexion (Fallo get Pokemon)");
                return "Error_Reflexion";
            }
            if (pokemon == null) {
                System.out.println("[CobblemonBridge] Resultado de la reflexión: Vacio (Pokemon nulo)");
                return "Vacio";
            }

            try {
                String res = null;
                switch (attribute) {
                    case "ivs_percent":
                        int totalIvs = pokemon.getIvs().getOrDefault(Stats.HP) +
                                pokemon.getIvs().getOrDefault(Stats.ATTACK) +
                                pokemon.getIvs().getOrDefault(Stats.DEFENCE) +
                                pokemon.getIvs().getOrDefault(Stats.SPECIAL_ATTACK) +
                                pokemon.getIvs().getOrDefault(Stats.SPECIAL_DEFENCE) +
                                pokemon.getIvs().getOrDefault(Stats.SPEED);
                        double prev = (totalIvs / 186.0) * 100.0;
                        res = String.format("%." + PluginConfig.getIvDecimalPlaces() + "f%%", prev);
                        break;
                    case "evs_total":
                        int totalEvs = pokemon.getEvs().getOrDefault(Stats.HP) +
                                pokemon.getEvs().getOrDefault(Stats.ATTACK) +
                                pokemon.getEvs().getOrDefault(Stats.DEFENCE) +
                                pokemon.getEvs().getOrDefault(Stats.SPECIAL_ATTACK) +
                                pokemon.getEvs().getOrDefault(Stats.SPECIAL_DEFENCE) +
                                pokemon.getEvs().getOrDefault(Stats.SPEED);
                        res = String.valueOf(totalEvs);
                        break;
                    case "is_shiny":
                        res = pokemon.getShiny() ? PluginConfig.getShinySymbol() : PluginConfig.getShinyNotSymbol();
                        break;
                    case "nature":
                        Object nature = pokemon.getClass().getMethod("getNature").invoke(pokemon);
                        Object nameObj = nature.getClass().getMethod("getName").invoke(nature);
                        String path = (String) nameObj.getClass().getMethod("getPath").invoke(nameObj);
                        res = path.substring(0, 1).toUpperCase() + path.substring(1);
                        break;
                    case "ability":
                        res = pokemon.getAbility().getTemplate().getName().substring(0, 1).toUpperCase()
                                + pokemon.getAbility().getTemplate().getName().substring(1).replace("_", " ");
                        break;
                    case "moveset":
                        StringBuilder moves = new StringBuilder();
                        Object moveSet = pokemon.getClass().getMethod("getMoveSet").invoke(pokemon);
                        Iterable<?> movesList = (Iterable<?>) moveSet.getClass().getMethod("getMoves").invoke(moveSet);
                        for (Object m : movesList) {
                            if (moves.length() > 0)
                                moves.append(", ");
                            Object template = m.getClass().getMethod("getTemplate").invoke(m);
                            Object displayName = template.getClass().getMethod("getDisplayName", boolean.class)
                                    .invoke(template, false);
                            moves.append((String) displayName.getClass().getMethod("getString").invoke(displayName));
                        }
                        if (moves.length() == 0)
                            res = PluginConfig.getNoMovesMessage();
                        else
                            res = moves.toString();
                        break;
                    default:
                        res = "Vacio";
                }
                System.out.println("[CobblemonBridge] Resultado de la reflexión: " + res);
                return res;
            } catch (Exception e) {
                System.out.println(
                        "[CobblemonBridge] Resultado de la reflexión: Error_Reflexion (" + e.getMessage() + ")");
                return "Error_Reflexion";
            }
        }, "Error_Reflexion");
    }

    // --- MODULE C: POKÉDEX ---
    private String handlePokedexPlaceholders(OfflinePlayer player, String params) {
        return executeWithFabricContext(() -> {
            try {
                // Híbrido: Delega carga al Mod Fabric Layer. Bypass de Bukkit NMS
                int caughtCount = dev.cobblemonbridge.fabric.CobblemonDataService.getPokedexCount(player.getUniqueId());

                if (caughtCount < 0) {
                    System.out.println(
                            "[CobblemonBridge/Hybrid] Error Crítico: Datos de Pokedex Hybrid inaccesibles en 1.7.1 ("
                                    + player.getName() + "). Fallback arrojando '0'.");
                    return "0";
                }

                if (params.equals("pokedex_caught_count")) {
                    return String.valueOf(caughtCount);
                } else if (params.equals("pokedex_caught_percent")) {
                    int totalSpecies = 1; // Default
                    try {
                        // 1.7.1 API: getImplemented() is a static method on PokemonSpecies
                        Object implementedList = PokemonSpecies.class.getMethod("getImplemented").invoke(null);
                        totalSpecies = (int) implementedList.getClass().getMethod("size").invoke(implementedList);
                    } catch (Exception eStatic) {
                        try {
                            // Pre 1.7.1 API: getImplemented() is on PokemonSpecies.INSTANCE
                            Object speciesInstance = PokemonSpecies.class.getField("INSTANCE").get(null);
                            Object implementedList = speciesInstance.getClass().getMethod("getImplemented")
                                    .invoke(speciesInstance);
                            totalSpecies = (int) implementedList.getClass().getMethod("size").invoke(implementedList);
                        } catch (Exception ignores) {
                        }
                    }

                    double percent = totalSpecies > 0 ? ((double) caughtCount / totalSpecies) * 100.0 : 0.0;
                    return String.format("%." + PluginConfig.getPokedexPercentDecimalPlaces() + "f%%", percent);
                }

                // Fallback global de seguridad
                return "0";
            } catch (Exception e) {
                // If the dex crashes, we fail gracefully
                System.out.println("[CobblemonMasterBridge-Debug] Error reading Pokedex for " + player.getUniqueId()
                        + ": " + e.getMessage());
                return "0";
            }
        }, "0");
    }

    // --- MODULE D: STATISTICS (BATTLEPASS) ---
    private String handleStatsPlaceholders(OfflinePlayer offlinePlayer, String params) {
        return executeWithFabricContext(() -> {
            try {
                Player player = offlinePlayer.getPlayer();
                if (player == null)
                    return "0";

                if (params.equals("stats_caught_total")) {
                    int total = dev.cobblemonbridge.fabric.CobblemonDataService.getPokedexCount(player.getUniqueId());
                    if (total < 0)
                        total = 0; // Fallback for error state

                    System.out.println("[CobblemonBridge] Pokedex de " + player.getName() + " leída: " + total);
                    return String.valueOf(total);
                }

                return "0"; // Default for unsupported target species stats without the listener
            } catch (Exception e) {
                return "0";
            }
        }, "0");
    }

    // --- ARCLIGHT CLASSLOADER FIX (Proxy Class Loading) ---
    /**
     * Arclight mixes Bukkit and Fabric classloaders. When a Bukkit plugin (like
     * PlaceholderAPI)
     * calls our expansion, its Thread ContextClassLoader is often the Bukkit
     * PluginClassLoader.
     * When we then call `Cobblemon.INSTANCE` or related Kotlin/Fabric classes, it
     * throws a
     * ClassNotFoundException because Bukkit's classloader doesn't know about Fabric
     * mods.
     *
     * We fix this by temporarily forcing the Thread's ContextClassLoader to the one
     * that
     * loaded Cobblemon, executing our logic, and then restoring it.
     */
    private <T> T executeWithFabricContext(java.util.concurrent.Callable<T> callable, T fallback) {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        try {
            // Force Fabric classloader
            Thread.currentThread().setContextClassLoader(Cobblemon.class.getClassLoader());
            return callable.call();
        } catch (Exception e) {
            plugin.getLogger().warning("Error processing Cobblemon placeholder: " + e.getMessage());
            e.printStackTrace();
            return fallback;
        } finally {
            // Restore Bukkit context
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    // UTILS
    private Object getPartyStore(OfflinePlayer player) {
        try {
            // Apply the same surgical reflection logic from CobblemonDataService
            Object craftServer = org.bukkit.Bukkit.getServer();
            Object minecraftServer = craftServer.getClass().getMethod("getServer").invoke(craftServer);
            Class<?> registryAccessClass = Class.forName("net.minecraft.class_5455");

            Object registryAccess = null;
            for (java.lang.reflect.Method method : minecraftServer.getClass().getMethods()) {
                if (method.getParameterCount() == 0 && method.getReturnType().equals(registryAccessClass)) {
                    registryAccess = method.invoke(minecraftServer);
                    break;
                }
            }

            if (registryAccess == null) {
                return null;
            }

            Object storage = Cobblemon.INSTANCE.getClass().getMethod("getStorage").invoke(Cobblemon.INSTANCE);
            return storage.getClass().getMethod("getParty", UUID.class, registryAccessClass).invoke(storage,
                    player.getUniqueId(), registryAccess);
        } catch (Exception e) {
            return null;
        }
    }
}
