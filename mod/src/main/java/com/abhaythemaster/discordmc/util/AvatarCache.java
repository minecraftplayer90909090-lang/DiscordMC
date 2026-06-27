package com.abhaythemaster.discordmc.util;

import com.abhaythemaster.discordmc.DiscordMC;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;
import java.io.InputStream;
import java.net.URI;
import java.net.http.*;
import java.util.Map;
import java.util.concurrent.*;

public class AvatarCache {
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final Map<String, Identifier> cache = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> loading = new ConcurrentHashMap<>();

    public static Identifier get(String userId, String hash) {
        if (hash == null || hash.isEmpty()) return null;
        return fetch(userId + "_av_" + hash,
            "https://cdn.discordapp.com/avatars/" + userId + "/" + hash + ".png?size=64");
    }

    public static Identifier getGuild(String guildId, String hash) {
        if (hash == null || hash.isEmpty()) return null;
        return fetch(guildId + "_gi_" + hash,
            "https://cdn.discordapp.com/icons/" + guildId + "/" + hash + ".png?size=64");
    }

    private static Identifier fetch(String key, String url) {
        if (cache.containsKey(key)) return cache.get(key);
        if (loading.getOrDefault(key, false)) return null;
        loading.put(key, true);

        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url)).GET().build();
                HttpResponse<InputStream> resp = HTTP.send(req,
                    HttpResponse.BodyHandlers.ofInputStream());
                if (resp.statusCode() == 200) {
                    NativeImage img = NativeImage.read(resp.body());
                    MinecraftClient.getInstance().execute(() -> {
                        try {
                            // 1.21.1 correct API
                            NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
                            String safeKey = key.toLowerCase()
                                .replaceAll("[^a-z0-9_]", "_")
                                .substring(0, Math.min(key.length(), 80));
                            Identifier id = Identifier.of("discordmc", safeKey);
                            MinecraftClient.getInstance()
                                .getTextureManager()
                                .registerTexture(id, tex);
                            cache.put(key, id);
                            DiscordMC.LOGGER.info("[DiscordMC] Avatar loaded: " + safeKey);
                        } catch (Exception e) {
                            DiscordMC.LOGGER.error("Texture register: " + e.getMessage());
                        }
                    });
                }
            } catch (Exception e) {
                DiscordMC.LOGGER.error("Avatar fetch failed: " + e.getMessage());
            } finally {
                loading.put(key, false);
            }
        });
        return null;
    }

    public static void clear() {
        cache.clear();
        loading.clear();
    }
}
