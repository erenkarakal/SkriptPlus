package me.eren.skriptplus.utils2;

import java.util.List;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import ch.njol.skript.lang.Statement;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;

public class SkriptUtils {

    /**
     * @param statement the statement
     * @return the parse time, in millis
     */
    public static long getParseTime(String statement) {
        long start = System.currentTimeMillis();
        Statement.parse(statement, null);
        long end = System.currentTimeMillis();
        return end - start;
    }

    /**
     * @return Skript's enabled dependencies
     */
    public static List<String> getEnabledDependencies() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        return pluginManager.getPlugin("Skript").getDescription().getSoftDepend().stream()
                .filter(pluginManager::isPluginEnabled)
                .toList();
    }

    /**
     * @return Skript's addons
     */
    public static List<String> getEnabledAddons() {
        return Skript.getAddons().stream()
                .map(SkriptAddon::getName)
                .toList();
    }

}
