package dev.cobblemonbridge;

import org.bukkit.configuration.file.FileConfiguration;

public class PluginConfig {

    private static String emptySlotMessage = "Empty Slot";
    private static String invalidSpeciesMessage = "Invalid Species";
    private static String noMovesMessage = "No Moves";
    
    private static String shinySymbol = "✦";
    private static String shinyNotSymbol = "";
    
    private static int ivDecimalPlaces = 0;
    private static int pokedexPercentDecimalPlaces = 0;

    public static void load(FileConfiguration config) {
        emptySlotMessage = config.getString("messages.empty-slot", "Empty Slot");
        invalidSpeciesMessage = config.getString("messages.invalid-species", "Invalid Species");
        noMovesMessage = config.getString("messages.no-moves", "No Moves");
        
        shinySymbol = config.getString("shiny.symbol", "✦");
        shinyNotSymbol = config.getString("shiny.not-shiny", "");
        
        ivDecimalPlaces = config.getInt("ivs.decimal-places", 0);
        pokedexPercentDecimalPlaces = config.getInt("pokedex.percent-decimal-places", 0);
    }

    public static String getEmptySlotMessage() {
        return emptySlotMessage;
    }

    public static String getInvalidSpeciesMessage() {
        return invalidSpeciesMessage;
    }

    public static String getNoMovesMessage() {
        return noMovesMessage;
    }

    public static String getShinySymbol() {
        return shinySymbol;
    }

    public static String getShinyNotSymbol() {
        return shinyNotSymbol;
    }

    public static int getIvDecimalPlaces() {
        return ivDecimalPlaces;
    }

    public static int getPokedexPercentDecimalPlaces() {
        return pokedexPercentDecimalPlaces;
    }
}
