package com.abhaythemaster.discordmc.gui;

import com.abhaythemaster.discordmc.DiscordMC;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class TokenLoginScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget tokenField;
    private String status = "";
    private int statusColor = 0xFFAAAAAA;

    public TokenLoginScreen(Screen parent) {
        super(Text.literal("Login - Discord MC"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        tokenField = new TextFieldWidget(textRenderer,
            width / 2 - 140, height / 2 - 10, 280, 20, Text.literal(""));
        tokenField.setPlaceholder(Text.literal("§7Apna Discord token paste karo..."));
        tokenField.setMaxLength(100);
        addDrawableChild(tokenField);
        setInitialFocus(tokenField);

        addDrawableChild(ButtonWidget.builder(Text.literal("§bLogin"), btn -> {
            String t = tokenField.getText().trim();
            if (t.isEmpty()) { status = "§cToken daalo!"; statusColor = 0xFFFF4444; return; }
            status = "§7Connecting..."; statusColor = 0xFFAAAAAA;
            DiscordMC.discord.loginWithToken(t).thenAccept(ok -> {
                if (ok) {
                    status = "§aLogin successful!";
                    statusColor = 0xFF00FF88;
                    client.execute(() -> client.setScreen(new DiscordMainScreen(parent)));
                } else {
                    status = "§cInvalid token!";
                    statusColor = 0xFFFF4444;
                }
            });
        }).dimensions(width / 2 - 40, height / 2 + 18, 80, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("§7Back"),
            btn -> client.setScreen(parent))
            .dimensions(width / 2 - 25, height / 2 + 46, 50, 18).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xFF202225);
        int px = width / 2 - 160;
        ctx.fill(px, height / 2 - 60, px + 320, height / 2 + 72, 0xFF2F3136);
        ctx.fill(px, height / 2 - 60, px + 320, height / 2 - 57, 0xFF5865F2);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§bDiscord Login"), width / 2, height / 2 - 50, 0xFF5865F2);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§7Token se login karo"), width / 2, height / 2 - 36, 0xFF8E9297);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§8Token kaise milega? Discord > Settings > Advanced > Developer Mode ON > then use token grabber"), 
            width / 2, height / 2 - 24, 0xFF666666);
        if (!status.isEmpty())
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(status), width / 2, height / 2 + 42, statusColor);
        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean keyPressed(int k, int s, int m) {
        if (k == 257) {
            String t = tokenField.getText().trim();
            if (!t.isEmpty()) DiscordMC.discord.loginWithToken(t);
            return true;
        }
        return super.keyPressed(k, s, m);
    }
}
