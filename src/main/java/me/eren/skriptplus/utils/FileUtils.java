package me.eren.skriptplus.utils;

import me.eren.skriptplus.SkriptPlus;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;

public class FileUtils {

    /**
     * @param plugin The plugin to get the file of.
     * @return The .jar file of the plugin.
     */
    public static File getFileOfPlugin(Plugin plugin) {
        try {
            Method getFile = JavaPlugin.class.getDeclaredMethod("getFile");
            getFile.setAccessible(true);
            return (File) getFile.invoke(plugin);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException("Error while getting the file of a plugin.", ex);
        }
    }

    /**
     * @param plugin The plugin to delete.
     * @return Whether the plugin was successfully deleted.
     */
    public static boolean deletePlugin(Plugin plugin) {
        File pluginFile = getFileOfPlugin(plugin);
        if (pluginFile.getPath().contains(".paper-remapped")) {
            File parentDir = pluginFile.getParentFile().getParentFile();
            File siblingFile = new File(parentDir, pluginFile.getName());
            return siblingFile.delete();
        }
        return pluginFile.delete();
    }

    /**
     * @param uri The URI to download the file from.
     * @param filePath The path of the file.
     * @return Whether the file was successfully downloaded.
     */
    public static CompletableFuture<Boolean> downloadFile(URI uri, String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            HttpClient client = SkriptPlus.getHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();

            try {
                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                    Path path = Path.of(filePath);
                    Files.copy(response.body(), path, StandardCopyOption.REPLACE_EXISTING);
                    return true;
                } else {
                    SkriptPlus.log("Failed to download file: " + response.statusCode());
                    return false;
                }
            } catch (IOException | InterruptedException ex) {
                throw new RuntimeException("Error while downloading a file.", ex);
            }
        });
    }

}
