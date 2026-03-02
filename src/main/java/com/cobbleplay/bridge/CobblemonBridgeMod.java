package com.cobbleplay.bridge;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.bukkit.Bukkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ══════════════════════════════════════════════════════════════
 * CobblemonBridge — Mod Fabric (Entrypoint Principal)
 * ══════════════════════════════════════════════════════════════
 *
 * Punto de entrada del mod. Se ejecuta al iniciar el servidor
 * y registra la expansión de PlaceholderAPI una vez que tanto
 * Cobblemon como PlaceholderAPI están completamente cargados.
 *
 * Compatible con: Arclight 1.21.1 + Cobblemon 1.6.1
 * ══════════════════════════════════════════════════════════════
 */
public class CobblemonBridgeMod implements ModInitializer {

    public static final String MOD_ID = "cobblemon-bridge";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[CobblemonBridge] Inicializando mod...");

        // ── Registrar la expansión PAPI después de que el servidor arranque ──
        // En este punto tanto Cobblemon (mod) como PlaceholderAPI (plugin Bukkit)
        // ya están completamente cargados en Arclight.
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            registrarExpansionPAPI();
        });
    }

    /**
     * Intenta registrar la expansión de PlaceholderAPI.
     * Verifica que PlaceholderAPI esté presente antes de intentar el registro.
     */
    private void registrarExpansionPAPI() {
        try {
            // Verificar que PlaceholderAPI está cargado como plugin Bukkit
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
                LOGGER.warn("[CobblemonBridge] ✗ PlaceholderAPI no encontrado. "
                        + "Asegúrate de que está instalado como plugin en Arclight.");
                return;
            }

            // Registrar la expansión
            CobblemonBridgeExpansion expansion = new CobblemonBridgeExpansion();
            boolean registrado = expansion.register();

            if (registrado) {
                LOGGER.info("[CobblemonBridge] ✓ Expansión '%cobblemonbridge_...%' registrada exitosamente.");
                LOGGER.info("[CobblemonBridge]   Módulos activos: Pokédex | Party | Estadísticas");
            } else {
                LOGGER.error("[CobblemonBridge] ✗ Fallo al registrar la expansión en PlaceholderAPI.");
            }

        } catch (NoClassDefFoundError e) {
            // PlaceholderAPI no está en el classpath (servidor sin Arclight o sin PAPI)
            LOGGER.error("[CobblemonBridge] ✗ Clases de PlaceholderAPI no encontradas. "
                    + "Este mod requiere Arclight con PlaceholderAPI instalado.", e);
        } catch (Exception e) {
            LOGGER.error("[CobblemonBridge] ✗ Error inesperado al registrar la expansión.", e);
        }
    }
}
