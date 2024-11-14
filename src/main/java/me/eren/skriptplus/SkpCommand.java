package me.eren.skriptplus;

import ch.njol.skript.Skript;
import me.eren.skriptplus.services.AddonService;
import me.eren.skriptplus.utils.FileUtils;
import me.eren.skriptplus.utils.SkriptUtils;
import me.eren.skriptplus.utils.Version;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SkpCommand implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            send(sender, "<gray>==============[ <gold>Skript<yellow>+ <white>Commands <gray>]==============</gray>");
            send(sender, "<gold>/sk addon <download/delete/update> <addon-name> <gray>- <white>Manage addons.");
            send(sender, "<gold>/sk info <gray>- <white>View info about the server, skript and skript addons.");
            send(sender, "<gold>/sk check <gray>- <white>Runs the update checker.");
            send(sender, "<gold>/sk share <script> <gray>- <white>Uploads a script to a paste service.");
            return true;
        }

        if (args[0].equalsIgnoreCase("check")) {
            send(sender, "Running the update checker...", true);
            UpdateChecker.runUpdateChecker().thenRun(() -> send(sender, "Update check is complete.", true));
        }

        else if (args[0].equalsIgnoreCase("addon")) {
            if (args.length < 2) {
                send(sender, "Correct usage: <yellow>/skp addon <download/delete/update> <addon-name>", true);
                return true;
            }
            if (args.length < 3) {
                send(sender, "Enter an addon name.", true);
                return true;
            }
            String addon = args[2].toLowerCase(Locale.ENGLISH);

            if (args[1].equalsIgnoreCase("download")) {
                if (Bukkit.getPluginManager().isPluginEnabled(addon) && args[args.length - 1].equalsIgnoreCase("-f")) {
                    send(sender, "Addon is already enabled.", true);
                    send(sender, "Add '-f' at the end of the command if you want to download it anyway.");
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
                        send(sender, "Successfully downloaded the addon.", true);
                    else
                        send(sender, "Something went wrong while downloading the addon.", true);
                });

            } else if (args[1].equalsIgnoreCase("delete")) {
                if (!Bukkit.getPluginManager().isPluginEnabled(addon)) {
                    send(sender, "Addon is disabled.", true);
                    return true;
                }

                if (FileUtils.deletePlugin(Bukkit.getPluginManager().getPlugin(addon)))
                    send(sender, "Deleted the addon.", true);
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

                FileUtils.deletePlugin(Bukkit.getPluginManager().getPlugin(addon));

                String id = skpConfig.getConfigurationSection(addon).getString("id");
                String service = skpConfig.getConfigurationSection(addon).getString("service");

                AddonService addonService = SkriptPlus.getAddonService(service);
                addonService.download(id).thenAccept(success -> {
                    if (success)
                        send(sender, "Successfully updated the addon.", true);
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
                    .toList();

            List<Component> addonMessages = SkriptUtils.getEnabledAddons().stream()
                    .map(this::checkPluginVersion)
                    .toList();

            String skriptFlavor = Skript.getInstance().getUpdater().getCurrentRelease().flavor;

            send(sender, "<gray>==============[ <gold>Skript<yellow>+ <white>Info <gray>]==============");
            send(sender, "Server Version: <yellow>" + Bukkit.getVersion());
            send(sender, checkPluginVersion(Skript.getInstance()).append(miniMessage(" (" + skriptFlavor + ")")));
            send(sender, ""); // newlines look very ugly in console, send an empty message instead
            if (!addonMessages.isEmpty()) {
                send(sender, "Addons [<yellow>" + addonMessages.size() + "</yellow>]");
                addonMessages.forEach(sender::sendMessage);
            } else {
                send(sender, "No addons are installed.");
            }
            send(sender, "");
            if (!dependencyMessages.isEmpty()) {
                send(sender, "Dependencies [<yellow>" + dependencyMessages.size() + "</yellow>]");
                dependencyMessages.forEach(sender::sendMessage);
            } else {
                send(sender, "No dependencies are installed.");
            }
            send(sender, "");
            if (sender instanceof Player)
                send(sender, getLinks());

        } else if (args[0].equalsIgnoreCase("recover")) {
            send(sender, "Recovering scripts...", true);
            SkriptUtils.recoverScripts();
            send(sender, "Complete! Check your <yellow>/plugins/Skript/dump/ <white>folder.", true);
        }

        else if (args[0].equalsIgnoreCase("share")) {
            if (args.length < 2) {
                send(sender, "Enter a script name.", true);
                return true;
            }
            String scriptName = args[1];
            if (!scriptName.endsWith(".sk"))
                scriptName = scriptName + ".sk";
            Path scriptsFolder = SkriptUtils.SCRIPTS_FOLDER;
            File script = scriptsFolder.resolve(scriptName).toFile();
            if (!script.exists()) {
                send(sender, "Couldn't find a script named " + scriptName + ".", true);
                return true;
            }
            FileUtils.uploadFile(script).thenAccept((link) -> {
                if (link != null) {
                    if (sender instanceof ConsoleCommandSender)
                        send(sender, "Success! <yellow>" + link, true);
                    else
                        send(sender, "Success! Click <yellow><click:open_url:" + link + ">here <reset>to see it.", true);
                } else {
                    send(sender, "Couldn't upload the file.", true);
                }
            });
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return CommandListener.skpCommands;
        }

        if (args[0].equalsIgnoreCase("addon")) {
            if (args.length == 2) {
                return List.of("delete", "download", "update");
            } else if (args.length == 3) {
                Set<String> keys = SkriptPlus.getInstance().getConfig().getConfigurationSection("addons").getKeys(false);
                return new ArrayList<>(keys);
            }
        }

        else if (args[0].equalsIgnoreCase("share")) {
            List<String> fileList = new ArrayList<>();
            Path scriptsFolder = SkriptUtils.SCRIPTS_FOLDER;
            try {
                Files.walk(scriptsFolder)
                        .filter(Files::isRegularFile)
                        .forEach(path -> fileList.add(scriptsFolder.relativize(path).toString()));
            } catch (IOException e) {
                return List.of();
            }
            return fileList;
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
                            "3) You didn't put an skUnity API key in the config (if the plugin requires one)."));
        } else if (latestVer.isLargerThan(currentVer)) { // outdated
            return miniMessage("<gray>[<red>❌<gray>] <white>" + plugin.getName() + " <gray>(" + currentVer + " -> " + latestVer + ")")
                    .hoverEvent(miniMessage("<red>Plugin is outdated."));
        } else { // (hopefully) up to date
            return miniMessage("<gray>[<green>✔<gray>] <white>" + plugin.getName() + " <gray>(" + currentVer + ")")
                    .hoverEvent(miniMessage("<green>Plugin is up to date."));
        }
    }

    private Component getLinks() {
        Component discord = miniMessage("<blue>[Discord]")
                .clickEvent(ClickEvent.openUrl("https://discord.gg/skript"))
                .hoverEvent(miniMessage("<blue>Join the skUnity Discord server."));
        Component github = miniMessage("<white>[GitHub]")
                .clickEvent(ClickEvent.openUrl("https://github.com/SkriptLang/Skript"))
                .hoverEvent(miniMessage("Open the Skript GitHub."));
        Component docs = miniMessage("<green>[Documentation]").clickEvent(ClickEvent.openUrl("https://skripthub.net/docs"))
                .hoverEvent(miniMessage("<green>Open the SkriptHub docs."));
        return Component.join(JoinConfiguration.separator(miniMessage("   ")), discord, github, docs);
    }

}
