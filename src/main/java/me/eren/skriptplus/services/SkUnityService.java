package me.eren.skriptplus.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.eren.skriptplus.SkriptPlus;
import me.eren.skriptplus.utils.FileUtils;
import org.bukkit.Bukkit;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class SkUnityService implements AddonService {

    private static final String API_KEY;

    static {
        API_KEY = Bukkit.getPluginManager().getPlugin("SkriptPlus").getConfig().getString("update-checker.skunity-api-key", "disabled");
        if (!API_KEY.equalsIgnoreCase("disabled") && API_KEY.length() != 32)
            throw new RuntimeException("SkUnity API key is not 32 characters long. Are you sure you entered a valid key? " +
                    "You should set it to \"disabled\" if you don't want to use the SkUnity service.");
    }

    @Override
    public CompletableFuture<String> getLatestVersion(String resourceID) {
        if (API_KEY == null || API_KEY.equalsIgnoreCase("disabled")) {
            return CompletableFuture.completedFuture(null);
        }

        HttpClient client = SkriptPlus.getHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.skunity.com/v1/" + API_KEY + "/resources/versions/" + resourceID))
                .header("Accept", "application/json")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                        SkriptPlus.log("Failed to fetch latest version: " + response.statusCode() + " Response: " + response.body());
                        return null;
                    }
                    Gson gson = new Gson();
                    JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                    if (!json.has("result"))
                        return null;
                    return json.get("result").getAsJsonArray()
                            .get(0).getAsJsonObject()
                            .get("version_string").getAsString();
                });
    }

    @Override
    public CompletableFuture<Boolean> download(String resourceID) {
        if (API_KEY == null || API_KEY.equalsIgnoreCase("disabled")) {
            return CompletableFuture.completedFuture(null);
        }

        HttpClient client = SkriptPlus.getHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.skunity.com/v1/" + API_KEY + "/resources/download/" + resourceID + "?filename=true"))
                .header("Accept", "application/json")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                        SkriptPlus.log("Failed to download file: " + response.statusCode() + " Response: " + response.body());
                        return false;
                    }
                    Gson gson = new Gson();
                    JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                    if (!json.has("response") || !json.get("response").getAsString().equalsIgnoreCase("forward"))
                        return null;

                    String downloadLink = json.get("result").getAsJsonObject()
                            .get("link").getAsString();
                    String fileName = json.get("result").getAsJsonObject()
                            .get("result").getAsJsonObject()
                            .get("filename").getAsString();
                    return FileUtils.downloadFile(
                            URI.create(downloadLink),
                            SkriptPlus.getInstance().getDataFolder().getParent() + "/" + fileName
                    ).join();
                });
    }

}
