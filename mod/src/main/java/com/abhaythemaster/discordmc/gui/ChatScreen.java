package com.abhaythemaster.discordmc.gui;

import com.abhaythemaster.discordmc.DiscordMC;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;

public class ChatScreen extends Screen {
    private final Screen parent;
    private final String channelId;
    private final String channelName;
    private TextFieldWidget input;
    private List<JsonObject> messages = new ArrayList<>();
    private boolean loading = true;
    private int scroll = 0;

    private static final int C_BG    = 0xFF36393F;
    private static final int C_PANEL = 0xFF2F3136;
    private static final int C_INPUT = 0xFF40444B;
    private static final int C_TEXT  = 0xFFDCDDDE;
    private static final int C_SUB   = 0xFF8E9297;
    private static final int C_NAME  = 0xFF5865F2;

    public ChatScreen(Screen parent, String channelId, String channelName) {
        super(Text.literal("#" + channelName));
        this.parent = parent;
        this.channelId = channelId;
        this.channelName = channelName;
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("§7←"),
            btn -> client.setScreen(parent))
            .dimensions(8, 8, 24, 18).build());

        input = new TextFieldWidget(textRenderer,
            36, height - 28, width - 80, 18, Text.literal(""));
        input.setPlaceholder(Text.literal("§8Message #" + channelName));
        input.setMaxLength(2000);
        addDrawableChild(input);
        setInitialFocus(input);

        addDrawableChild(ButtonWidget.builder(Text.literal("§b▶"),
            btn -> sendMessage())
            .dimensions(width - 38, height - 29, 30, 20).build());

        // Load messages
        loading = true;
        DiscordMC.discord.fetchMessages(channelId).thenAccept(msgs -> {
            messages = msgs;
            loading = false;
            scroll = Math.max(0, messages.size() * 22 - (height - 60));
        });

        // Real-time messages
        DiscordMC.discord.onMessage(msg -> {
            if (msg.get("channel_id").getAsString().equals(channelId)) {
                messages.add(msg);
                scroll = Math.max(0, messages.size() * 22 - (height - 60));
            }
        });
    }

    private void sendMessage() {
        String txt = input.getText().trim();
        if (txt.isEmpty()) return;
        DiscordMC.discord.sendMessage(channelId, txt);
        input.setText("");
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, C_BG);

        // Top bar
        ctx.fill(0, 0, width, 30, C_PANEL);
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§7# §f" + channelName), 40, 10, C_TEXT);

        // Messages area
        ctx.fill(0, 30, width, height - 35, C_BG);

        if (loading) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7Loading messages..."), width / 2, height / 2, C_SUB);
        } else if (messages.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7Koi message nahi"), width / 2, height / 2, C_SUB);
        } else {
            int y = 36 - scroll;
            for (JsonObject msg : messages) {
                if (y > 30 && y < height - 35) {
                    String author = "Unknown";
                    if (msg.has("author"))
                        author = msg.getAsJsonObject("author").get("username").getAsString();
                    String content = msg.has("content") ? msg.get("content").getAsString() : "";

                    ctx.drawTextWithShadow(textRenderer,
                        Text.literal("§b" + author + " §8·"), 8, y, C_NAME);
                    // Truncate long messages
                    if (content.length() > 60) content = content.substring(0, 60) + "...";
                    ctx.drawTextWithShadow(textRenderer,
                        Text.literal("§f" + content), 8, y + 10, C_TEXT);
                }
                y += 22;
            }
        }

        // Input bar
        ctx.fill(0, height - 35, width, height, C_PANEL);
        ctx.fill(4, height - 30, width - 42, height - 6, C_INPUT);

        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean keyPressed(int k, int s, int m) {
        if (k == 257 || k == 335) { sendMessage(); return true; }
        return super.keyPressed(k, s, m);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll = Math.max(0, scroll - (int)(v * 8)); return true;
    }
}
