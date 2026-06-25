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

    public GuildChannelScreen(Screen parent, String guildId, String guildName) {
        super(Text.literal(guildName));
        this.parent = parent;
        this.guildId = guildId;
        this.guildName = guildName;
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("§7←"),
            btn -> client.setScreen(parent))
            .dimensions(8, 8, 24, 18).build());

        DiscordMC.discord.fetchChannels(guildId).thenAccept(list -> {
            channels = list;
            loading = false;
        });
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xFF36393F);
        ctx.fill(0, 0, 200, height, 0xFF2F3136);
        ctx.fill(0, 0, 3, height, 0xFF5865F2);

        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§f" + guildName), 10, 10, 0xFFDCDDDE);
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§8TEXT CHANNELS"), 10, 30, 0xFF8E9297);

        if (loading) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7Loading..."), 100, height / 2, 0xFF8E9297);
            super.render(ctx, mx, my, delta); return;
        }

        int y = 48 - scroll;
        for (JsonObject ch : channels) {
            if (y > 28 && y < height - 10) {
                boolean hov = mx >= 4 && mx <= 196 && my >= y && my <= y + 20;
                ctx.fill(4, y, 196, y + 18, hov ? 0xFF40444B : 0);
                String name = ch.get("name").getAsString();
                ctx.drawTextWithShadow(textRenderer,
                    Text.literal("§8# §7" + name), 10, y + 4, 0xFF8E9297);
            }
            y += 22;
        }
        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (mx >= 4 && mx <= 196) {
            int y = 48 - scroll;
            for (JsonObject ch : channels) {
                if (my >= y && my <= y + 18) {
                    client.setScreen(new ChatScreen(this, ch.get("id").getAsString(), ch.get("name").getAsString()));
                    return true;
                }
                y += 22;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll = Math.max(0, scroll - (int)(v * 8)); return true;
    }
}
