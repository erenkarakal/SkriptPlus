package me.eren.skriptplus;

import me.eren.skriptplus.services.AddonService;
import me.eren.skriptplus.utils.Version;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class UpdateChecker {

    private static BukkitTask updateCheckerTask;
    public static final Map<Plugin, Version> latestVersions = new HashMap<>();
    /**
     * Whether the update check is currently running.
     */
    public static boolean isRunning = false;

    public static void start() {
        long interval = SkriptPlus.getInstance().getConfig().getLong("update-checker.interval", 60L) * 60 * 20;
        updateCheckerTask = Bukkit.getScheduler()
                .runTaskTimerAsynchronously(SkriptPlus.getInstance(), UpdateChecker::runUpdateChecker, 0L, interval);
    }

    public static void stop() {
        if (!updateCheckerTask.isCancelled())
            updateCheckerTask.cancel();
    }

    /**
     * Deletes the cache and re-checks the latest versions.
     */
    public static void runUpdateChecker() {
        isRunning = true;
        latestVersions.clear();
        updateAddonsList().completeOnTimeout(null, 5, TimeUnit.SECONDS).join();
        SkriptPlus.getInstance().reloadConfig();
        Configuration config = SkriptPlus.getInstance().getConfig();
        Set<String> addons = config.getConfigurationSection("addons").getKeys(false);

        if (config.getBoolean("update-checker.log-messages", false))
            SkriptPlus.log("Running the update checker...");

        int timeout = SkriptPlus.getInstance().getConfig().getInt("update-checker.timeout", 5);

        for (String addon : addons) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(addon);
            if (plugin == null)
                continue;

            String id = config.getString("addons." + addon + ".id");
            String service = config.getString("addons." + addon + ".service");

            AddonService addonService = SkriptPlus.getAddonService(service);
            String latestVer = addonService.getLatestVersion(id)
                    .completeOnTimeout(null, timeout, TimeUnit.SECONDS)
                    .join();

            latestVersions.put(plugin, new Version(latestVer));
        }

        if (config.getBoolean("update-checker.log-messages", false))
            SkriptPlus.log("Update checker complete!");

        isRunning = false;
    }

    /**
     * Fetches the latest config.yml from GitHub and adds any missing addons to the config.
     */
    private static CompletableFuture<Void> updateAddonsList() {
        HttpClient client = SkriptPlus.getHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://github.com/erenkarakal/SkriptPlus/raw/refs/heads/master/src/main/resources/config.yml"))
                .header("Accept", "application/json")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    try {
                        YamlConfiguration config = new YamlConfiguration();
                        config.loadFromString(response.body());
                        FileConfiguration skpConfig = SkriptPlus.getInstance().getConfig();
                        for (String key : config.getConfigurationSection("addons").getKeys(false)) {
                            if (!skpConfig.contains("addons." + key))
                                skpConfig.set("addons." + key, config.getConfigurationSection("addons." + key));
                        }
                    } catch (InvalidConfigurationException ignored) {}
                });
    }

}
