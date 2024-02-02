package me.eren.skriptplus.plugins;

import ch.njol.skript.util.Version;
import me.eren.skriptplus.utils2.ResourceUpstreamProvider;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * A wrapper object represented by Bukkit's {@link JavaPlugin}.
 */
public class Plugin {

    private final JavaPlugin javaPlugin;
    private final Version version;
    private ResourceUpstreamProvider provider;

    public Plugin(JavaPlugin javaPlugin) {
        this.javaPlugin = javaPlugin;
        PluginDescriptionFile pdf = javaPlugin.getDescription();
        version = new Version(pdf.getVersion());

        // TODO add support for multiple platforms
        provider = new ResourceUpstreamProvider(ResourceUpstreamProvider.Provider.GITHUB);
    }

    /**
     * @return Bukkit's java plugin
     */
    public JavaPlugin getJavaPlugin() {
        return javaPlugin;
    }

    /**
     * @return the version
     */
    public Version getVersion() {
        return version;
    }

}
