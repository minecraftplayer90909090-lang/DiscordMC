package com.abhaythemaster.discordmc.gui;

import com.abhaythemaster.discordmc.DiscordMC;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;

public class GuildChannelScreen extends Screen {
    private final Screen parent;
    private final String guildId, guildName;
    private List<JsonObject> channels = new ArrayList<>();
    private boolean loading = true;
    private int scroll = 0;

    private static final int C_BG     = 0xFF0A0E1A;
    private static final int C_PANEL  = 0xFF0C1220;
    private static final int C_ACCENT = 0xFF00E5FF;
    private static final int C_MUTED  = 0xFF6080AA;
    private static final int C_TEXT   = 0xFFE0F0FF;
    private static final int C_HOVER  = 0xFF081628;
    private static final int C_BORDER = 0x4400E5FF;

    public GuildChannelScreen(Screen parent, String guildId, String guildName) {
        super(Text.literal(guildName));
        this.parent = parent;
        this.guildId = guildId;
        this.guildName = guildName;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, C_BG);
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(
            Text.literal("§b←"), btn -> client.setScreen(parent))
            .dimensions(8, 8, 24, 18).build());

        // Fetch channels using REST API directly
        loading = true;
        channels.clear();
        fetchGuildChannels();
    }

    private void fetchGuildChannels() {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                java.net.http.HttpClient http = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://discord.com/api/v10/guilds/" + guildId + "/channels"))
                    .header("Authorization", DiscordMC.discord.getToken())
                    .GET().build();
                java.net.http.HttpResponse<String> resp = http.send(req,
                    java.net.http.HttpResponse.BodyHandlers.ofString());
                com.google.gson.JsonArray arr = new com.google.gson.Gson()
                    .fromJson(resp.body(), com.google.gson.JsonArray.class);
                channels.clear();
                for (com.google.gson.JsonElement el : arr) {
                    com.google.gson.JsonObject ch = el.getAsJsonObject();
                    // Type 0 = text channel
                    if (ch.get("type").getAsInt() == 0) channels.add(ch);
                }
                loading = false;
            } catch (Exception e) {
                DiscordMC.LOGGER.error("Fetch channels failed: " + e.getMessage());
                loading = false;
            }
        });
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, C_BG);

        // Sidebar
        ctx.fill(0, 0, 200, height, C_PANEL);
        ctx.fill(199, 0, 200, height, C_BORDER);

        // Guild name header
        ctx.fill(0, 0, 200, 36, 0xFF080C16);
        ctx.fill(0, 36, 200, 37, C_BORDER);
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§b§l" + truncate(guildName, 16)), 36, 12, C_ACCENT);

        // Channels label
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§8TEXT CHANNELS"), 10, 44, C_MUTED);

        if (loading) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7Loading..."), 100, height / 2, C_MUTED);
        } else if (channels.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7No text channels"), 100, height / 2, C_MUTED);
        } else {
            int y = 58 - scroll;
            for (JsonObject ch : channels) {
                if (y > 42 && y < height - 10) {
                    boolean hov = mx >= 4 && mx <= 196 && my >= y && my <= y + 22;
                    if (hov) ctx.fill(4, y, 196, y + 22, C_HOVER);
                    ctx.drawTextWithShadow(textRenderer,
                        Text.literal((hov ? "§b" : "§8") + "# §7" + ch.get("name").getAsString()),
                        10, y + 6, C_TEXT);
                }
                y += 24;
            }
        }

        // Main area hint
        ctx.fill(200, 0, width, height, C_BG);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§7Select a channel"), 200 + (width - 200) / 2, height / 2, C_MUTED);

        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (mx >= 4 && mx <= 196) {
            int y = 58 - scroll;
            for (JsonObject ch : channels) {
                if (my >= y && my <= y + 22) {
                    client.setScreen(new ChatScreen(this,
                        ch.get("id").getAsString(),
                        ch.get("name").getAsString()));
                    return true;
                }
                y += 24;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll = Math.max(0, scroll - (int)(v * 8));
        return true;
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 2) + ".." : s;
    }
}
