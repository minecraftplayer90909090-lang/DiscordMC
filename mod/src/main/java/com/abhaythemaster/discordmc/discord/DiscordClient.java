package com.abhaythemaster.discordmc.discord;

import com.abhaythemaster.discordmc.DiscordMC;
import com.google.gson.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class DiscordClient {
    private static final String API = "https://discord.com/api/v10";
    private static final String CONFIG = "config/discordmc_token.json";
    private static final Gson GSON = new Gson();
    private final HttpClient http = HttpClient.newHttpClient();

    private String token = null;
    private JsonObject selfUser = null;
    private WebSocket gatewayWs = null;
    private boolean connected = false;
    private int heartbeatInterval = 0;
    private Integer lastSeq = null;

    private final List<JsonObject> dmChannels = new CopyOnWriteArrayList<>();
    private final List<JsonObject> guilds = new CopyOnWriteArrayList<>();
    private final Map<String, List<JsonObject>> channelMessages = new ConcurrentHashMap<>();

    // Online presence: discord_id -> status
    private final Map<String, String> presenceMap = new ConcurrentHashMap<>();

    private Consumer<JsonObject> messageCallback;
    private Runnable onReadyCallback;

    public DiscordClient() { loadToken(); }

    public CompletableFuture<Boolean> loginWithToken(String userToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API + "/users/@me"))
                    .header("Authorization", userToken)
                    .GET().build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    selfUser = GSON.fromJson(resp.body(), JsonObject.class);
                    token = userToken;
                    saveToken();
                    connectGateway();
                    return true;
                }
            } catch (Exception e) {
                DiscordMC.LOGGER.error("Login failed: " + e.getMessage());
            }
            return false;
        });
    }

    public void logout() {
        token = null; selfUser = null; connected = false;
        dmChannels.clear(); guilds.clear(); presenceMap.clear();
        if (gatewayWs != null) gatewayWs.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
        try { Files.deleteIfExists(Path.of(CONFIG)); } catch (IOException ignored) {}
    }

    private void connectGateway() {
        try {
            gatewayWs = HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create("wss://gateway.discord.gg/?v=10&encoding=json"),
                    new GatewayListener())
                .get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            DiscordMC.LOGGER.error("Gateway failed: " + e.getMessage());
        }
    }

    private void identify() {
        JsonObject pkt = new JsonObject();
        pkt.addProperty("op", 2);
        JsonObject d = new JsonObject();
        d.addProperty("token", token);
        // Request presence updates
        d.addProperty("intents", 37377 | (1 << 8));
        JsonObject props = new JsonObject();
        props.addProperty("os", "android");
        props.addProperty("browser", "Discord Android");
        props.addProperty("device", "Discord Android");
        d.add("properties", props);
        // Request presence
        JsonObject presence = new JsonObject();
        presence.addProperty("status", "online");
        presence.addProperty("afk", false);
        d.add("presence", presence);
        pkt.add("d", d);
        gatewayWs.sendText(GSON.toJson(pkt), true);
    }

    private void startHeartbeat() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            if (gatewayWs != null && connected) {
                JsonObject hb = new JsonObject();
                hb.addProperty("op", 1);
                if (lastSeq != null) hb.addProperty("d", lastSeq);
                else hb.add("d", JsonNull.INSTANCE);
                gatewayWs.sendText(GSON.toJson(hb), true);
            }
        }, heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);
    }

    public CompletableFuture<List<JsonObject>> fetchDMs() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API + "/users/@me/channels"))
                    .header("Authorization", token).GET().build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                JsonArray arr = GSON.fromJson(resp.body(), JsonArray.class);
                dmChannels.clear();
                for (JsonElement el : arr) {
                    JsonObject ch = el.getAsJsonObject();
                    if (ch.get("type").getAsInt() == 1) dmChannels.add(ch);
                }
                return new ArrayList<>(dmChannels);
            } catch (Exception e) { return new ArrayList<>(); }
        });
    }

    public CompletableFuture<List<JsonObject>> fetchMessages(String channelId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API + "/channels/" + channelId + "/messages?limit=50"))
                    .header("Authorization", token).GET().build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                JsonArray arr = GSON.fromJson(resp.body(), JsonArray.class);
                List<JsonObject> msgs = new ArrayList<>();
                for (JsonElement el : arr) msgs.add(el.getAsJsonObject());
                Collections.reverse(msgs);
                channelMessages.put(channelId, msgs);
                return msgs;
            } catch (Exception e) { return new ArrayList<>(); }
        });
    }

    public CompletableFuture<Boolean> sendMessage(String channelId, String content) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("content", content);
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API + "/channels/" + channelId + "/messages"))
                    .header("Authorization", token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body))).build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                return resp.statusCode() == 200;
            } catch (Exception e) { return false; }
        });
    }

    public CompletableFuture<List<JsonObject>> fetchGuilds() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API + "/users/@me/guilds"))
                    .header("Authorization", token).GET().build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                JsonArray arr = GSON.fromJson(resp.body(), JsonArray.class);
                guilds.clear();
                for (JsonElement el : arr) guilds.add(el.getAsJsonObject());
                return new ArrayList<>(guilds);
            } catch (Exception e) { return new ArrayList<>(); }
        });
    }

    public String getAvatarUrl(String userId, String avatarHash) {
        if (avatarHash == null || avatarHash.isEmpty()) return null;
        return "https://cdn.discordapp.com/avatars/" + userId + "/" + avatarHash + ".png?size=32";
    }

    public String getGuildIconUrl(String guildId, String iconHash) {
        if (iconHash == null || iconHash.isEmpty()) return null;
        return "https://cdn.discordapp.com/icons/" + guildId + "/" + iconHash + ".png?size=32";
    }

    // Presence: online/idle/dnd/offline
    public String getPresence(String userId) {
        return presenceMap.getOrDefault(userId, "offline");
    }

    private void handleGateway(String raw) {
        try {
            JsonObject pkt = GSON.fromJson(raw, JsonObject.class);
            int op = pkt.get("op").getAsInt();
            if (pkt.has("s") && !pkt.get("s").isJsonNull())
                lastSeq = pkt.get("s").getAsInt();

            switch (op) {
                case 10 -> {
                    heartbeatInterval = pkt.getAsJsonObject("d").get("heartbeat_interval").getAsInt();
                    startHeartbeat();
                    identify();
                }
                case 0 -> {
                    String t = pkt.get("t").getAsString();
                    JsonObject d = pkt.getAsJsonObject("d");
                    switch (t) {
                        case "READY" -> {
                            connected = true;
                            selfUser = d.getAsJsonObject("user");
                            // Initial presences from READY
                            if (d.has("presences")) {
                                for (JsonElement pe : d.getAsJsonArray("presences")) {
                                    JsonObject p = pe.getAsJsonObject();
                                    String uid = p.getAsJsonObject("user").get("id").getAsString();
                                    String status = p.get("status").getAsString();
                                    presenceMap.put(uid, status);
                                }
                            }
                            fetchDMs(); fetchGuilds();
                            if (onReadyCallback != null) onReadyCallback.run();
                        }
                        case "PRESENCE_UPDATE" -> {
                            // Real presence updates
                            if (d.has("user") && d.has("status")) {
                                String uid = d.getAsJsonObject("user").get("id").getAsString();
                                String status = d.get("status").getAsString();
                                presenceMap.put(uid, status);
                            }
                        }
                        case "MESSAGE_CREATE" -> {
                            if (messageCallback != null) messageCallback.accept(d);
                            String chId = d.get("channel_id").getAsString();
                            if (channelMessages.containsKey(chId))
                                channelMessages.get(chId).add(d);
                        }
                    }
                }
                case 11 -> {} // Heartbeat ACK
            }
        } catch (Exception e) {
            DiscordMC.LOGGER.error("Gateway error: " + e.getMessage());
        }
    }

    private void saveToken() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("token", token);
            Files.createDirectories(Path.of("config"));
            Files.writeString(Path.of(CONFIG), GSON.toJson(o));
        } catch (IOException e) { DiscordMC.LOGGER.error("Save failed"); }
    }

    private void loadToken() {
        try {
            Path p = Path.of(CONFIG);
            if (!Files.exists(p)) return;
            JsonObject o = GSON.fromJson(Files.readString(p), JsonObject.class);
            token = o.get("token").getAsString();
            loginWithToken(token);
        } catch (Exception ignored) {}
    }

    public void onMessage(Consumer<JsonObject> cb) { this.messageCallback = cb; }
    public void onReady(Runnable cb) { this.onReadyCallback = cb; }
    public boolean isLoggedIn() { return token != null && selfUser != null; }
    public JsonObject getSelfUser() { return selfUser; }
    public String getToken() { return token; }
    public List<JsonObject> getCachedDMs() { return dmChannels; }
    public List<JsonObject> getCachedGuilds() { return guilds; }

    private class GatewayListener implements WebSocket.Listener {
        private final StringBuilder buf = new StringBuilder();
        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            buf.append(data);
            if (last) { handleGateway(buf.toString()); buf.setLength(0); }
            return WebSocket.Listener.super.onText(ws, data, last);
        }
        @Override
        public CompletionStage<?> onClose(WebSocket ws, int code, String reason) {
            connected = false;
            return WebSocket.Listener.super.onClose(ws, code, reason);
        }
        @Override
        public void onError(WebSocket ws, Throwable e) {
            connected = false;
            DiscordMC.LOGGER.error("WS error: " + e.getMessage());
        }
    }
}
