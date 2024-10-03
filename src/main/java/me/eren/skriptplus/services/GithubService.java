package me.eren.skriptplus.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.eren.skriptplus.utils.FileUtils;
import me.eren.skriptplus.SkriptPlus;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class GithubService implements AddonService {

    @Override
    public CompletableFuture<String> getLatestVersion(String repo) {
        HttpClient client = SkriptPlus.getHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/" + repo + "/releases/latest"))
                .header("Accept", "application/json")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                        SkriptPlus.log("Failed to fetch latest version: " + response.statusCode() + " Response: " + response.body());
                    }
                    Gson gson = new Gson();
                    JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                    if (!json.has("tag_name"))
                        return null;
                    return json.get("tag_name").getAsString();
                });
    }

    @Override
    public CompletableFuture<Boolean> download(String repo) {
        HttpClient client = SkriptPlus.getHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/" + repo + "/releases/latest"))
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
                    if (!json.has("tag_name")) return false;

                    JsonObject assetsTag = json.getAsJsonArray("assets").get(0).getAsJsonObject();
                    String downloadLink = assetsTag.get("browser_download_url").getAsString();
                    String fileName = assetsTag.get("name").getAsString();

                    HttpRequest downloadRequest = HttpRequest.newBuilder()
                            .uri(URI.create(downloadLink))
                            .header("Accept", "application/octet-stream")
                            .GET()
                            .build();

                    return client.sendAsync(downloadRequest, HttpResponse.BodyHandlers.ofInputStream())
                            .thenApply(downloadResponse -> {
                                if (downloadResponse.statusCode() != HttpURLConnection.HTTP_OK) {
                                    SkriptPlus.log("Failed to download file: " + downloadResponse.statusCode());
                                    return false;
                                }
                                return FileUtils.downloadFile(
                                        downloadResponse.uri(),
                                        SkriptPlus.getInstance().getDataFolder().getParent() + "/" + fileName
                                ).join();
                            }).join();
                });
    }
}
