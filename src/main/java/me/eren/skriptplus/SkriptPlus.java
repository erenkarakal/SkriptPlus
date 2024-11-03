package me.eren.skriptplus;

import ch.njol.skript.Skript;
import me.eren.skriptplus.services.AddonService;
import me.eren.skriptplus.services.GithubService;
import me.eren.skriptplus.services.SkUnityService;
import me.eren.skriptplus.utils.Metrics;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.Map;

public final class SkriptPlus extends JavaPlugin {

    private static SkriptPlus instance;
    public static final String PREFIX = "<white>[<gold>Skript<yellow>+<white>] ";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final Map<String, AddonService> services = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        loadAddonServices();
        new Metrics(this, 19422);
        saveDefaultConfig();
        getCommand("skp").setExecutor(new SkpCommand());
        if (getConfig().getBoolean("overwrite-command", true))
            getServer().getPluginManager().registerEvents(new CommandListener(), this);
        Skript.registerAddon(this);
        UpdateChecker.start();
    }

    @Override
    public void onDisable() {
        UpdateChecker.stop();
    }

    public static SkriptPlus getInstance() {
        return instance;
    }

    public static HttpClient getHttpClient() {
        return httpClient;
    }

    private static void loadAddonServices() {
        services.put("github", new GithubService());
        services.put("skunity", new SkUnityService());
    }

    public static AddonService getAddonService(String service) {
        if (!services.containsKey(service))
            throw new IllegalArgumentException("Service '" + service + "' doesn't exist.");
        return services.get(service);
    }

    public static void log(String message) {
        Bukkit.getConsoleSender().sendMessage(
                MiniMessage.miniMessage().deserialize(PREFIX + message)
        );
    }

}
