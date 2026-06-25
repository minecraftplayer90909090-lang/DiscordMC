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
    private String sessionId = null;

    // Cache
    private final List<JsonObject> guilds = new CopyOnWriteArrayList<>();
    private final Map<String, List<JsonObject>> channelMessages = new ConcurrentHashMap<>();
    private final List<JsonObject> dmChannels = new CopyOnWriteArrayList<>();
    private Consumer<JsonObject> messageCallback;

    public DiscordClient() { loadToken(); }

    // ── Auth ──────────────────────────────────────────────────────────────────

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
        guilds.clear(); dmChannels.clear();
        if (gatewayWs != null) gatewayWs.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
        try { Files.deleteIfExists(Path.of(CONFIG)); } catch (IOException ignored) {}
    }

    // ── Gateway (real-time events) ────────────────────────────────────────────

    private void connectGateway() {
        try {
            HttpClient ws_client = HttpClient.newHttpClient();
            gatewayWs = ws_client.newWebSocketBuilder()
                .buildAsync(URI.create("wss://gateway.discord.gg/?v=10&encoding=json"), new GatewayListener())
                .get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            DiscordMC.LOGGER.error("Gateway failed: " + e.getMessage());
        }
    }

    private void identify() {
        JsonObject identify = new JsonObject();
        identify.addProperty("op", 2);
        JsonObject d = new JsonObject();
        d.addProperty("token", token);
        d.addProperty("intents", 37377);
        JsonObject props = new JsonObject();
        props.addProperty("os", "android");
        props.addProperty("browser", "Discord Android");
        props.addProperty("device", "Discord Android");
        d.add("properties", props);
        identify.add("d", d);
        gatewayWs.sendText(GSON.toJson(identify), true);
    }

    private void startHeartbeat() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            if (gatewayWs != null && connected) {
                JsonObject hb = new JsonObject();
                hb.addProperty("op", 1);
                if (lastSeq != null) hb.addProperty("d", lastSeq);
                else hb.add("d", JsonNull.INSTANCE);
                gatewayWs.sendText(GSON.toJson(hb), true);
            }
        }, heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);
    }

    // ── API Calls ─────────────────────────────────────────────────────────────

    public CompletableFuture<List<JsonObject>> fetchDMs() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API + "/users/@me/channels"))
                    .header("Authorization", token)
                    .GET().build();
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
                    .header("Authorization", token)
                    .GET().build();
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
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                    .build();
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
                    .header("Authorization", token)
                    .GET().build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                JsonArray arr = GSON.fromJson(resp.body(), JsonArray.class);
                guilds.clear();
                for (JsonElement el : arr) guilds.add(el.getAsJsonObject());
                return new ArrayList<>(guilds);
            } catch (Exception e) { return new ArrayList<>(); }
        });
    }

    public CompletableFuture<List<JsonObject>> fetchChannels(String guildId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API + "/guilds/" + guildId + "/channels"))
                    .header("Authorization", token)
                    .GET().build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                JsonArray arr = GSON.fromJson(resp.body(), JsonArray.class);
                List<JsonObject> channels = new ArrayList<>();
                for (JsonElement el : arr) {
                    JsonObject ch = el.getAsJsonObject();
                    if (ch.get("type").getAsInt() == 0) channels.add(ch);
                }
                return channels;
            } catch (Exception e) { return new ArrayList<>(); }
        });
    }

    // ── Storage ───────────────────────────────────────────────────────────────

    private void saveToken() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("token", token);
            Files.createDirectories(Path.of("config"));
            Files.writeString(Path.of(CONFIG), GSON.toJson(o));
        } catch (IOException e) { DiscordMC.LOGGER.error("Save failed: " + e.getMessage()); }
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
    public boolean isLoggedIn() { return token != null && selfUser != null; }
    public JsonObject getSelfUser() { return selfUser; }
    public String getToken() { return token; }
    public List<JsonObject> getCachedDMs() { return dmChannels; }
    public List<JsonObject> getCachedGuilds() { return guilds; }

    // ── Gateway Listener ──────────────────────────────────────────────────────

    private class GatewayListener implements WebSocket.Listener {
        private final StringBuilder buf = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            buf.append(data);
            if (last) {
                handleGateway(buf.toString());
                buf.setLength(0);
            }
            return WebSocket.Listener.super.onText(ws, data, last);
        }

        @Override
        public void onError(WebSocket ws, Throwable e) {
            connected = false;
            DiscordMC.LOGGER.error("Gateway error: " + e.getMessage());
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int code, String reason) {
            connected = false;
            return WebSocket.Listener.super.onClose(ws, code, reason);
        }
    }

    private void handleGateway(String raw) {
        try {
            JsonObject pkt = GSON.fromJson(raw, JsonObject.class);
            int op = pkt.get("op").getAsInt();
            if (pkt.has("s") && !pkt.get("s").isJsonNull())
                lastSeq = pkt.get("s").getAsInt();

            switch (op) {
                case 10 -> {
                    // Hello - start heartbeat + identify
                    heartbeatInterval = pkt.getAsJsonObject("d").get("heartbeat_interval").getAsInt();
                    startHeartbeat();
                    identify();
                }
                case 0 -> {
                    // Dispatch
                    String t = pkt.get("t").getAsString();
                    JsonObject d = pkt.getAsJsonObject("d");
                    switch (t) {
                        case "READY" -> {
                            connected = true;
                            sessionId = d.get("session_id").getAsString();
                            selfUser = d.getAsJsonObject("user");
                            DiscordMC.LOGGER.info("[DiscordMC] Logged in as " + selfUser.get("username").getAsString());
                            fetchDMs();
                            fetchGuilds();
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
            DiscordMC.LOGGER.error("Gateway parse error: " + e.getMessage());
        }
    }
}
