package com.abhaythemaster.discordmc.gui;

import com.abhaythemaster.discordmc.DiscordMC;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;

public class ServerListScreen extends Screen {
    private final Screen parent;
    private List<JsonObject> servers = new ArrayList<>();
    private boolean loading = true;
    private int scroll = 0;

    private static final int C_BG     = 0xFF202225;
    private static final int C_PANEL  = 0xFF2F3136;
    private static final int C_ACCENT = 0xFF5865F2;
    private static final int C_TEXT   = 0xFFDCDDDE;
    private static final int C_SUB    = 0xFF8E9297;

    public ServerListScreen(Screen parent) {
        super(Text.literal("Servers"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("§7←"),
            btn -> client.setScreen(parent))
            .dimensions(8, 8, 24, 18).build());

        loading = true;
        DiscordMC.discord.fetchGuilds().thenAccept(list -> {
            servers = list;
            loading = false;
        });
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, C_BG);
        ctx.fill(0, 0, 72, height, C_PANEL);
        ctx.fill(0, 0, 3, height, C_ACCENT);

        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§bServers"), 36, 16, C_SUB);

        if (loading) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7Loading..."), 36, height / 2, C_SUB);
            super.render(ctx, mx, my, delta); return;
        }

        int y = 36 - scroll;
        for (JsonObject s : servers) {
            if (y > 30 && y < height - 10) {
                boolean hov = mx >= 4 && mx <= 68 && my >= y && my <= y + 40;
                ctx.fill(8, y, 64, y + 36, hov ? 0xFF5865F2 : C_ACCENT);
                String name = s.get("name").getAsString();
                String abbr = name.length() >= 2 ? name.substring(0, 2).toUpperCase() : name;
                ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("§f" + abbr), 36, y + 12, 0xFFFFFFFF);
            }
            y += 44;
        }

        // Server name on right side
        ctx.fill(72, 0, width, height, 0xFF36393F);
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§7Select a server from left"), 82, 16, C_SUB);

        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (mx >= 4 && mx <= 68) {
            int y = 36 - scroll;
            for (JsonObject s : servers) {
                if (my >= y && my <= y + 36) {
                    String guildId = s.get("id").getAsString();
                    String guildName = s.get("name").getAsString();
                    client.setScreen(new GuildChannelScreen(this, guildId, guildName));
                    return true;
                }
                y += 44;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll = Math.max(0, scroll - (int)(v * 8)); return true;
    }
}
