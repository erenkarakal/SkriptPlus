package me.eren.skriptplus.services;

import java.util.concurrent.CompletableFuture;

public interface AddonService {

    CompletableFuture<String> getLatestVersion(String resourceID);
    CompletableFuture<Boolean> download(String resourceID);

}
