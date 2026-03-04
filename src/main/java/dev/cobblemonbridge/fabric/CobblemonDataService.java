package dev.cobblemonbridge.fabric;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokedex.CaughtCount;
import com.cobblemon.mod.common.api.pokedex.PokedexManager;
import java.util.UUID;
import org.bukkit.Bukkit;

public class CobblemonDataService {

    public static int getPokedexCount(UUID uuid) {
        try {
            PokedexManager pokedexManager = Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(uuid);
            if (pokedexManager != null) {
                Integer count = CaughtCount.INSTANCE.calculate(pokedexManager);
                return count != null ? count : -1;
            }
        } catch (Exception e) {
            System.out.println(
                    "[CobblemonBridge/Hybrid] Error al acceder a PokedexManager nativamente: " + e.getMessage());
        }
        return -1;
    }

    private static Object getRegistryAccess() throws Exception {
        Object craftServer = Bukkit.getServer();
        Object minecraftServer = craftServer.getClass().getMethod("getServer").invoke(craftServer);
        Class<?> registryAccessClass = Class.forName("net.minecraft.class_5455");

        for (java.lang.reflect.Method method : minecraftServer.getClass().getMethods()) {
            if (method.getParameterCount() == 0 && method.getReturnType().equals(registryAccessClass)) {
                System.out.println("[CobblemonBridge/Híbrido] Método de Registry encontrado por tipo de retorno: "
                        + method.getName());
                return method.invoke(minecraftServer);
            }
        }

        throw new NoSuchMethodException("No se encontró un método para RegistryAccess en el servidor.");
    }

    public static String getSlotNickname(UUID uuid, int slotIndex) {
        try {
            System.out.println(
                    "[CobblemonBridge/Híbrido] Conexión establecida con la firma 1.7.1 (UUID + RegistryAccess).");

            Class<?> registryAccessClass = Class.forName("net.minecraft.class_5455");
            System.out.println("[CobblemonBridge/Híbrido] Clase RegistryAccess (class_5455) cargada con éxito.");
            Object registryAccess = getRegistryAccess();

            Object storage = com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage();
            Object partyStore = storage.getClass().getMethod("getParty", UUID.class, registryAccessClass)
                    .invoke(storage, uuid, registryAccess);

            if (partyStore == null) {
                return "Empty Slot";
            }

            Object pokemon = partyStore.getClass().getMethod("get", int.class).invoke(partyStore, slotIndex);
            if (pokemon == null) {
                return "Slot_Vacio_En_Party";
            }

            // Invoca getDisplayName(false) mediante reflexión para evitar class_5250
            Object displayNameComponent = pokemon.getClass().getMethod("getDisplayName", boolean.class).invoke(pokemon,
                    false);
            return (String) displayNameComponent.getClass().getMethod("getString").invoke(displayNameComponent);

        } catch (Exception e) {
            System.out
                    .println("[CobblemonBridge/Híbrido] Error al recuperar Slot " + slotIndex + ": " + e.getMessage());
            e.printStackTrace();
            return "Error_Final";
        }
    }

    public static Integer getSlotLevel(UUID uuid, int slotIndex) {
        try {
            Class<?> registryAccessClass = Class.forName("net.minecraft.class_5455");
            Object registryAccess = getRegistryAccess();

            Object storage = com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage();
            Object partyStore = storage.getClass().getMethod("getParty", UUID.class, registryAccessClass)
                    .invoke(storage, uuid, registryAccess);
            if (partyStore == null)
                return null;

            Object pokemon = partyStore.getClass().getMethod("get", int.class).invoke(partyStore, slotIndex);
            if (pokemon == null)
                return null;

            return (Integer) pokemon.getClass().getMethod("getLevel").invoke(pokemon);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getSlotSpecies(UUID uuid, int slotIndex) {
        try {
            Class<?> registryAccessClass = Class.forName("net.minecraft.class_5455");
            Object registryAccess = getRegistryAccess();

            Object storage = com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage();
            Object partyStore = storage.getClass().getMethod("getParty", UUID.class, registryAccessClass)
                    .invoke(storage, uuid, registryAccess);
            if (partyStore == null)
                return null;

            Object pokemon = partyStore.getClass().getMethod("get", int.class).invoke(partyStore, slotIndex);
            if (pokemon == null)
                return null;

            Object species = pokemon.getClass().getMethod("getSpecies").invoke(pokemon);
            return (String) species.getClass().getMethod("getName").invoke(species);
        } catch (Exception e) {
            return null;
        }
    }
}
