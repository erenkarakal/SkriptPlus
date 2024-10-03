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

import java.io.IOException;
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
    public static volatile boolean isRunning = false;

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
    public static CompletableFuture<Void> runUpdateChecker() {
        if (isRunning) return CompletableFuture.completedFuture(null);

        isRunning = true;
        latestVersions.clear();

        return updateAddonsList()
                .completeOnTimeout(null, 5, TimeUnit.SECONDS)
                .thenRun(() -> {
                    SkriptPlus.getInstance().reloadConfig();
                    Configuration config = SkriptPlus.getInstance().getConfig();
                    Set<String> addons = config.getConfigurationSection("addons").getKeys(false);

                    if (config.getBoolean("update-checker.log-messages", false)) {
                        SkriptPlus.log("Running the update checker...");
                    }

                    int timeout = config.getInt("update-checker.timeout", 5);
                    CompletableFuture<Void> allChecks = CompletableFuture.completedFuture(null);

                    for (String addon : addons) {
                        Plugin plugin = Bukkit.getPluginManager().getPlugin(addon);
                        if (plugin == null) continue;

                        String id = config.getString("addons." + addon + ".id");
                        String service = config.getString("addons." + addon + ".service");

                        AddonService addonService = SkriptPlus.getAddonService(service);
                        allChecks = allChecks.thenCompose(ignored ->
                                addonService.getLatestVersion(id)
                                        .completeOnTimeout(null, timeout, TimeUnit.SECONDS)
                                        .thenAccept(latestVer ->
                                                latestVersions.put(plugin, new Version(latestVer))
                                        )
                        );
                    }

                    allChecks.thenRun(() -> {
                        if (config.getBoolean("update-checker.log-messages", false)) {
                            SkriptPlus.log("Update checker complete!");
                        }
                        isRunning = false;
                    });
                });
    }

    /**
     * Fetches the latest config.yml from GitHub and adds any missing addons to the config.
     */
    private static CompletableFuture<Void> updateAddonsList() {
        HttpClient client = SkriptPlus.getHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://raw.githubusercontent.com/erenkarakal/SkriptPlus/refs/heads/master/src/main/resources/config.yml"))
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
                        skpConfig.save(SkriptPlus.getInstance().getDataFolder() + "/config.yml");
                    } catch (InvalidConfigurationException | IOException ignored) {}
                });
    }

}
