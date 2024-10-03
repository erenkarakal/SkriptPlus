package me.eren.skriptplus;

import ch.njol.skript.Skript;
import me.eren.skriptplus.services.AddonService;
import me.eren.skriptplus.utils.FileUtils;
import me.eren.skriptplus.utils.SkriptUtils;
import me.eren.skriptplus.utils.Version;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SkpCommand implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            send(sender, "<gray>==============[ <gold>Skript<yellow>+ <white>Info <gray>]==============");
            send(sender, "<yellow>/sk addon <download/delete/update> <addon-name>");
            send(sender, "<yellow>/sk info");
            return true;
        }

        if (args[0].equalsIgnoreCase("addon")) {
            if (args.length < 2) {
                send(sender, "Correct usage: <yellow>/skp addon <download/delete/update> <addon-name>", true);
                return true;
            }
            if (args.length < 3) {
                send(sender, "Enter an addon name.", true);
                return true;
            }
            String addon = args[2];

            if (args[1].equalsIgnoreCase("download")) {
                if (Bukkit.getPluginManager().isPluginEnabled(addon)) {
                    send(sender, "Addon is already downloaded.", true);
                    return true;
                }

                ConfigurationSection skpConfig = SkriptPlus.getInstance().getConfig().getConfigurationSection("addons");
                if (!skpConfig.isSet(addon)) {
                    send(sender, "Addon doesn't exist in the config.", true);
                    return true;
                }

                String id = skpConfig.getConfigurationSection(addon).getString("id");
                String service = skpConfig.getConfigurationSection(addon).getString("service");

                AddonService addonService = SkriptPlus.getAddonService(service);
                addonService.download(id).thenAccept(success -> {
                    if (success)
                        send(sender, "Successfully downloaded the addon", true);
                    else
                        send(sender, "Something went wrong while downloading the addon.", true);
                });
            } else if (args[1].equalsIgnoreCase("delete")) {
                if (!Bukkit.getPluginManager().isPluginEnabled(addon)) {
                    send(sender, "Addon is already deleted.", true);
                    return true;
                }

                if (FileUtils.deletePlugin(Bukkit.getPluginManager().getPlugin(addon)))
                    send(sender, "Deleted the addon", true);
                else
                    send(sender, "Something went wrong while deleting the addon.", true);
            } else if (args[1].equalsIgnoreCase("update")) {
                if (!Bukkit.getPluginManager().isPluginEnabled(addon)) {
                    send(sender, "Addon isn't installed.", true);
                    return true;
                }

                ConfigurationSection skpConfig = SkriptPlus.getInstance().getConfig().getConfigurationSection("addons");
                if (!skpConfig.isSet(addon)) {
                    send(sender, "Addon doesn't exist in the config.", true);
                    return true;
                }

                if (!FileUtils.deletePlugin(Bukkit.getPluginManager().getPlugin(addon))) {
                    send(sender, "Something went wrong while updating the addon. Couldn't delete the plugin.", true);
                    return true;
                }

                String id = skpConfig.getConfigurationSection(addon).getString("id");
                String service = skpConfig.getConfigurationSection(addon).getString("service");

                AddonService addonService = SkriptPlus.getAddonService(service);
                addonService.download(id).thenAccept(success -> {
                    if (success)
                        send(sender, "Successfully updated the addon", true);
                    else
                        send(sender, "Something went wrong while updating the addon. Couldn't download the file.", true);
                });
            }
        } else if (args[0].equalsIgnoreCase("info")) {
            if (UpdateChecker.isRunning) {
                send(sender, "The update checker is currently running. Please try again in a few seconds.", true);
                return true;
            }

            List<Component> dependencyMessages = SkriptUtils.getEnabledDependencies().stream()
                    .map(this::checkPluginVersion)
                    .collect(Collectors.toList());

            List<Component> addonMessages = SkriptUtils.getEnabledAddons().stream()
                    .map(this::checkPluginVersion)
                    .collect(Collectors.toList());

            send(sender, "<gray>==============[ <gold>Skript<yellow>+ <white>Info <gray>]==============");
            send(sender, checkPluginVersion(Skript.getInstance()).append(miniMessage(" (" + Skript.getInstance().getUpdater().getCurrentRelease().flavor + ")")));
            send(sender, "Server Version: <yellow>" + Bukkit.getVersion());
            send(sender, ""); // newlines look very ugly in console, send an empty message instead
            send(sender, "Addons [" + addonMessages.size() + "]");
            addonMessages.forEach(sender::sendMessage);
            send(sender, "");
            send(sender, "Dependencies [" + dependencyMessages.size() + "]");
            dependencyMessages.forEach(sender::sendMessage);
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 0) {
            return List.of("info", "addon");
        }

        if (args[0].equalsIgnoreCase("addon")) {
            if (args.length == 1) {
                return List.of("delete", "download", "update");
            } else if (args.length == 2) {
                Set<String> keys = SkriptPlus.getInstance().getConfig().getConfigurationSection("addons").getKeys(false);
                return new ArrayList<>(keys);
            }
        }
        return List.of();
    }

    private Component miniMessage(String message) {
        return MiniMessage.miniMessage().deserialize(message);
    }

    private void send(CommandSender sender, Component component) {
        sender.sendMessage(component);
    }

    private void send(CommandSender sender, String message) {
        send(sender, message, false);
    }

    private void send(CommandSender sender, String message, Boolean showPrefix) {
        if (showPrefix)
            message = SkriptPlus.PREFIX + message;
        sender.sendMessage(miniMessage(message));
    }

    private Component checkPluginVersion(Plugin plugin) {
        Version currentVer = new Version(plugin.getDescription().getVersion());
        Version latestVer = UpdateChecker.latestVersions.get(plugin);
        if (latestVer == null || latestVer.version == null) { // unknown
            return miniMessage("<gray>[<gold><bold>?</bold><gray>] <white>" + plugin.getName() + " <gray>(" + currentVer + ")")
                    .hoverEvent(miniMessage("<gold>Latest version is unknown. Could be due to 3 reasons:</gold><br>" +
                            "1) Update check timed out.<br>" +
                            "2) SkriptPlus doesn't have this addon added in the config file.<br>" +
                            "3) You didn't put an SkUnity API key in the config (if the plugin requires one)."));
        } else if (latestVer.isLargerThan(currentVer)) { // outdated
            return miniMessage("<gray>[<red>❌<gray>] <white>" + plugin.getName() + " <gray>(" + currentVer + " -> " + latestVer + ")")
                    .hoverEvent(miniMessage("<red>Plugin is outdated."));
        } else { // (hopefully) up to date
            return miniMessage("<gray>[<green>✔<gray>] <white>" + plugin.getName() + " <gray>(" + currentVer + ")")
                    .hoverEvent(miniMessage("<green>Plugin is up to date."));
        }
    }

}
