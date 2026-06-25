package com.abhaythemaster.discordmc.gui;

import com.abhaythemaster.discordmc.DiscordMC;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class DiscordMainScreen extends Screen {
    private final Screen parent;

    private static final int C_BG     = 0xFF202225;
    private static final int C_PANEL  = 0xFF2F3136;
    private static final int C_ACCENT = 0xFF5865F2;
    private static final int C_TEXT   = 0xFFDCDDDE;
    private static final int C_SUB    = 0xFF8E9297;

    public DiscordMainScreen(Screen parent) {
        super(Text.literal("Discord"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (!DiscordMC.discord.isLoggedIn()) {
            addDrawableChild(ButtonWidget.builder(
                Text.literal("§bLogin with Token"),
                btn -> client.setScreen(new TokenLoginScreen(this)))
                .dimensions(width / 2 - 80, height / 2 - 10, 160, 20)
                .build());
            return;
        }

        // DMs button
        addDrawableChild(ButtonWidget.builder(
            Text.literal("§f💬 Direct Messages"),
            btn -> client.setScreen(new DMListScreen(this)))
            .dimensions(width / 2 - 120, height / 2 - 30, 240, 24)
            .build());

        // Servers button
        addDrawableChild(ButtonWidget.builder(
            Text.literal("§f🏠 Servers"),
            btn -> client.setScreen(new ServerListScreen(this)))
            .dimensions(width / 2 - 120, height / 2, 240, 24)
            .build());

        // Logout
        addDrawableChild(ButtonWidget.builder(
            Text.literal("§cLogout"),
            btn -> { DiscordMC.discord.logout(); client.setScreen(new DiscordMainScreen(parent)); })
            .dimensions(width / 2 - 50, height / 2 + 60, 100, 18)
            .build());

        // Back
        addDrawableChild(ButtonWidget.builder(
            Text.literal("§7← Back"),
            btn -> client.setScreen(parent))
            .dimensions(8, 8, 60, 18)
            .build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, C_BG);
        int px = width / 2 - 150, pw = 300;
        ctx.fill(px, 0, px + pw, height, C_PANEL);
        ctx.fill(px, 0, px + pw, 3, C_ACCENT);

        // Discord logo area
        ctx.fill(width / 2 - 30, 20, width / 2 + 30, 50, C_ACCENT);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§fD"), width / 2, 28, 0xFFFFFFFF);

        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§bDiscord"), width / 2, 58, C_ACCENT);

        if (DiscordMC.discord.isLoggedIn()) {
            String name = DiscordMC.discord.getSelfUser().get("username").getAsString();
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7Logged in as §f" + name), width / 2, 74, C_SUB);
        } else {
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7Login to continue"), width / 2, 74, C_SUB);
        }
        super.render(ctx, mx, my, delta);
    }
}
