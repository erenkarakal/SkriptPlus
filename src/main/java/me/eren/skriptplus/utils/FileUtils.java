package me.eren.skriptplus.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.eren.skriptplus.SkriptPlus;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;

public class FileUtils {

    private static final String MCLOGS_API = "https://api.mclo.gs/1/log";
    private static final Method GET_FILE;

    static {
        try {
            GET_FILE = JavaPlugin.class.getDeclaredMethod("getFile");
            GET_FILE.setAccessible(true);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException("getFile() method doesn't exist, SkriptPlus needs an update!!!", ex);
        }
    }

    /**
     * @param plugin The plugin to get the file of.
     * @return The .jar file of the plugin.
     */
    public static File getFileOfPlugin(Plugin plugin) {
        try {
            return (File) GET_FILE.invoke(plugin);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            SkriptPlus.log("Couldn't get the file of a plugin. " + ex.getMessage());
            return null;
        }
    }

    /**
     * @param plugin The plugin to delete.
     * @return Whether the plugin was successfully deleted.
     */
    public static boolean deletePlugin(Plugin plugin) {
        File pluginFile = getFileOfPlugin(plugin);
        if (pluginFile == null)
            return false;
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
                    try (InputStream inputStream = response.body();
                         OutputStream outputStream = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                    return true;
                } else {
                    SkriptPlus.log("Failed to download file: " + response.statusCode());
                    return false;
                }
            } catch (IOException | InterruptedException ex) {
                SkriptPlus.log("Failed to download a file. " + ex.getMessage());
                return false;
            }
        });
    }

    /**
     * Uploads the contents of a File to mclo.gs API
     * @param file The file to upload the contents of
     * @return The link if the upload was successful otherwise null.
     */
    public static CompletableFuture<String> uploadFile(File file) {
        try {
            String fileContent = Files.readString(file.toPath());
            String encodedContent = URLEncoder.encode(fileContent, StandardCharsets.UTF_8);
            String postData = "content=" + encodedContent;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MCLOGS_API))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(postData))
                    .build();

            return SkriptPlus.getHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (!(response.statusCode() == HttpURLConnection.HTTP_OK)) {
                            SkriptPlus.log("Failed to upload file: " + response.statusCode());
                            return null;
                        }
                        Gson gson = new Gson();
                        JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                        if (json.has("url")) {
                            return json.get("url").getAsString();
                        }
                        return null;
                    });

        } catch (IOException ex) {
            SkriptPlus.log("Failed to upload file: " + ex.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

}
