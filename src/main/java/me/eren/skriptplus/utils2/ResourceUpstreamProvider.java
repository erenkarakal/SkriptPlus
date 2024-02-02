package me.eren.skriptplus.utils2;

import java.io.File;
import java.util.concurrent.CompletableFuture;

public class ResourceUpstreamProvider {

    public enum ReleaseChannel {
        LATEST("latest"),
        LATEST_STABLE("latest stable"),
        LATEST_SNAPSHOT("latest snapshot");

        public final String name;

        ReleaseChannel(String name) {
            this.name = name;
        }
    }

    public enum Provider {
        GITHUB("GitHub", "insert-api-url"),
        SKUNITY("skUnity", "insert-api-url"),
        SPIGOT("Spigot", "insert-api-url"),
        HANGAR("Hangar", "insert-api-url");

        private final String name, url;

        Provider(String name, String url) {
            this.name = name;
            this.url = url;
        }
    }

    private Provider provider;

    public ResourceUpstreamProvider(Provider provider) {
        this.provider = provider;
    }

    /**
     * Downloads the latest stable release.
     * @param destination the destination file
     */
    public void download(File destination) {
        download(ReleaseChannel.LATEST_STABLE, destination);
    }

    /**
     * Downloads the latest release of a release channel.
     * @param releaseChannel the release channel
     * @param destination the file destination
     */
    public void download(ReleaseChannel releaseChannel, File destination) {
        CompletableFuture.runAsync(() -> {
            switch (provider) {
                case GITHUB -> {
                }
                case SKUNITY -> {
                }
                case SPIGOT -> {
                }
                case HANGAR -> {
                }
                default -> throw new IllegalArgumentException("Unexpected provider: " + provider);
            }
        });
    }

}
