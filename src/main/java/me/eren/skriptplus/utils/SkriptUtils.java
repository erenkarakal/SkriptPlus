package me.eren.skriptplus.utils;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import ch.njol.skript.lang.Statement;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public class SkriptUtils {
    public static long getParseTime(String statement) {
        long start = System.currentTimeMillis();
        Statement.parse(statement, null);
        long end = System.currentTimeMillis();
        return end - start;
    }

    public static List<String> getEnabledDependencies() {
        List<String> dependencies = new ArrayList<>();
        for(String plugin : Bukkit.getPluginManager().getPlugin("Skript").getDescription().getSoftDepend()) {
            if (Bukkit.getPluginManager().isPluginEnabled(plugin)) {
                dependencies.add(plugin);
            }
        }
        return dependencies;
    }

    public static List<String> getEnabledAddons() {
        List<String> addons = new ArrayList<>();
        for(SkriptAddon addon : Skript.getAddons()) {
            addons.add(addon.getName());
        }
        return addons;
    }


}
