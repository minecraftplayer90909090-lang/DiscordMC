package com.abhaythemaster.discordmc.util;

import com.abhaythemaster.discordmc.DiscordMC;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.DynamicTexture;
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
        return fetchUrl(userId + "_av_" + hash,
            "https://cdn.discordapp.com/avatars/" + userId + "/" + hash + ".png?size=64", "av");
    }

    public static Identifier getGuild(String guildId, String hash) {
        if (hash == null || hash.isEmpty()) return null;
        return fetchUrl(guildId + "_gi_" + hash,
            "https://cdn.discordapp.com/icons/" + guildId + "/" + hash + ".png?size=64", "gi");
    }

    private static Identifier fetchUrl(String key, String url, String prefix) {
        if (key.contains("null") || key.endsWith("_")) return null;
        if (cache.containsKey(key)) return cache.get(key);
        if (loading.getOrDefault(key, false)) return null;
        loading.put(key, true);
        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                HttpResponse<InputStream> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofInputStream());
                if (resp.statusCode() == 200) {
                    NativeImage img = NativeImage.read(resp.body());
                    MinecraftClient.getInstance().execute(() -> {
                        try {
                            DynamicTexture tex = new DynamicTexture(img);
                            String safeKey = (prefix + "_" + key).toLowerCase().replaceAll("[^a-z0-9_/]", "_");
                            Identifier id = Identifier.of("discordmc", safeKey);
                            MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
                            cache.put(key, id);
                        } catch (Exception e) { DiscordMC.LOGGER.error("Tex register: " + e.getMessage()); }
                    });
                }
            } catch (Exception e) { DiscordMC.LOGGER.error("Fetch: " + e.getMessage()); }
            finally { loading.put(key, false); }
        });
        return null;
    }

    public static void clear() { cache.clear(); loading.clear(); }
}
