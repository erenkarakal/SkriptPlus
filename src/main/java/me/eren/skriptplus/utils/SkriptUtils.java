package me.eren.skriptplus.utils;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.config.Config;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.skriptlang.skript.lang.script.Script;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SkriptUtils {

    private static final Method getIndentationMethod;
    private static final Method getCommentMethod;
    private static final Path dumpFolder = Paths.get(Skript.getInstance().getDataFolder().getAbsolutePath() + "/dump/");

    static {
        try {
            Class<Node> nodeClass = Node.class;
            getIndentationMethod = nodeClass.getDeclaredMethod("getIndentation");
            getIndentationMethod.setAccessible(true);

            getCommentMethod = nodeClass.getDeclaredMethod("getComment");
            getCommentMethod.setAccessible(true);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException("Error while loading script recovery.", ex);
        }
    }


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

    /**
     * Dumps all loaded scripts to a folder. Even if the script files are deleted.
     */
    public static void recoverScripts() {
        try {
            Files.createDirectories(dumpFolder);
        } catch (IOException ex) {
            throw new RuntimeException("Error while recovering scripts.", ex);
        }

        for (Script script : ScriptLoader.getLoadedScripts()) {
            Config config = script.getConfig();
            String name = config.getFileName();

            List<String> lines = new ArrayList<>();
            for (Node node : config.getMainNode()) {
                lines.addAll(loopNodes(node));
                lines.add("");
            }
            Path filePath = dumpFolder.resolve(name);
            try {
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, lines, StandardOpenOption.CREATE);
            } catch (IOException ex) {
                throw new RuntimeException("Error while recovering scripts.", ex);
            }
        }
    }

    private static List<String> loopNodes(Node mainNode) {
        String indentation = (String) safeInvoke(getIndentationMethod, mainNode, null);
        String comment = (String) safeInvoke(getCommentMethod, mainNode, null);
        String key = mainNode.getKey() == null ? "" : mainNode.getKey();

        List<String> lines = new ArrayList<>();

        if (mainNode instanceof SectionNode sectionNode) {
            if (!key.isEmpty()) {
                lines.add(indentation + key + ": " + comment);
            }
            for (Node node : sectionNode) {
                lines.addAll(loopNodes(node));
            }
        } else {
            lines.add(indentation + key + comment);
        }

        return lines;
    }

    private static Object safeInvoke(Method method, Object object, Object... args) {
        try {
            return method.invoke(object, args);
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            return null;
        }
    }

}
