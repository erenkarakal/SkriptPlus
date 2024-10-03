package me.eren.skriptplus.utils;

import ch.njol.skript.Skript;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SkriptUtils {

    public static List<Plugin> getEnabledDependencies() {
        return Skript.getInstance().getDescription().getSoftDepend().stream()
                .map(Bukkit.getPluginManager()::getPlugin)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static List<Plugin> getEnabledAddons() {
        return Skript.getAddons().stream()
                .map(addon -> addon.plugin)
                .collect(Collectors.toList());
    }

}
