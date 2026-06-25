package com.abhaythemaster.discordmc.gui;

import com.abhaythemaster.discordmc.DiscordMC;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;

public class DMListScreen extends Screen {
    private final Screen parent;
    private List<JsonObject> dms = new ArrayList<>();
    private int scroll = 0;
    private boolean loading = true;

    private static final int C_BG     = 0xFF202225;
    private static final int C_PANEL  = 0xFF2F3136;
    private static final int C_CARD   = 0xFF36393F;
    private static final int C_ACCENT = 0xFF5865F2;
    private static final int C_TEXT   = 0xFFDCDDDE;
    private static final int C_SUB    = 0xFF8E9297;
    private static final int C_GREEN  = 0xFF3BA55D;

    public DMListScreen(Screen parent) {
        super(Text.literal("Direct Messages"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("§7←"),
            btn -> client.setScreen(parent))
            .dimensions(8, 8, 24, 18).build());

        // Fetch DMs
        loading = true;
        DiscordMC.discord.fetchDMs().thenAccept(list -> {
            dms = list;
            loading = false;
        });
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, C_BG);

        // Sidebar
        ctx.fill(0, 0, 200, height, C_PANEL);
        ctx.fill(0, 0, 3, height, C_ACCENT);

        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§bDIRECT MESSAGES"), 12, 36, C_SUB);

        if (loading) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7Loading..."), 100, height / 2, C_SUB);
            super.render(ctx, mx, my, delta);
            return;
        }

        if (dms.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7Koi DM nahi"), 100, height / 2, C_SUB);
            super.render(ctx, mx, my, delta);
            return;
        }

        int y = 55 - scroll;
        for (JsonObject dm : dms) {
            if (y > 30 && y < height - 10) {
                boolean hov = mx >= 4 && mx <= 196 && my >= y && my <= y + 36;
                ctx.fill(4, y, 196, y + 34, hov ? C_CARD : 0);

                // Avatar placeholder
                ctx.fill(8, y + 5, 28, y + 25, C_ACCENT);

                // Username
                String name = "Unknown";
                if (dm.has("recipients") && dm.getAsJsonArray("recipients").size() > 0) {
                    name = dm.getAsJsonArray("recipients").get(0)
                        .getAsJsonObject().get("username").getAsString();
                }
                ctx.drawTextWithShadow(textRenderer,
                    Text.literal("§f" + name), 34, y + 8, C_TEXT);

                // Last message preview
                if (dm.has("last_message_id") && !dm.get("last_message_id").isJsonNull()) {
                    ctx.drawTextWithShadow(textRenderer,
                        Text.literal("§8Click to open"), 34, y + 20, C_SUB);
                }
            }
            y += 38;
        }

        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (mx >= 4 && mx <= 196) {
            int y = 55 - scroll;
            for (JsonObject dm : dms) {
                if (my >= y && my <= y + 34) {
                    String chId = dm.get("id").getAsString();
                    String name = "Unknown";
                    if (dm.has("recipients") && dm.getAsJsonArray("recipients").size() > 0)
                        name = dm.getAsJsonArray("recipients").get(0).getAsJsonObject().get("username").getAsString();
                    client.setScreen(new ChatScreen(this, chId, name));
                    return true;
                }
                y += 38;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll = Math.max(0, scroll - (int)(v * 8)); return true;
    }
}
