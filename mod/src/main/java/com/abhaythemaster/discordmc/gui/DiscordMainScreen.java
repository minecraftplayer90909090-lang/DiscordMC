package com.abhaythemaster.discordmc.gui;

import com.abhaythemaster.discordmc.DiscordMC;
import com.google.gson.JsonObject;
import dev.isxander.yacl3.gui.utils.GuiUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class DiscordMainScreen extends Screen {
    private final Screen parent;

    // Discord dark theme
    private static final int C_BG      = 0xFF202225;
    private static final int C_SIDEBAR = 0xFF2F3136;
    private static final int C_HOVER   = 0xFF36393F;
    private static final int C_ACCENT  = 0xFF5865F2;
    private static final int C_GREEN   = 0xFF3BA55D;
    private static final int C_TEXT    = 0xFFDCDDDE;
    private static final int C_MUTED   = 0xFF8E9297;
    private static final int C_INPUT   = 0xFF40444B;
    private static final int C_RED     = 0xFFED4245;
    private static final int C_WHITE   = 0xFFFFFFFF;
    private static final int C_DARK    = 0xFF1E2124;
    private static final int C_CARD    = 0xFF34373C;

    // Layout
    private static final int GUILDBAR = 68;
    private static final int SIDEBAR  = 230;
    private static final int TOPBAR   = 44;

    // State
    private List<JsonObject> dms = new ArrayList<>();
    private List<JsonObject> servers = new ArrayList<>();
    private boolean loadingDMs = false;
    private int listScroll = 0;
    private String openChId = null, openChName = null;
    private List<JsonObject> messages = new ArrayList<>();
    private int chatScroll = 0;
    private boolean inChat = false;

    // Login
    private boolean inLogin = false;
    private TextFieldWidget tokenField;
    private String loginMsg = "";
    private int loginMsgColor = C_MUTED;

    // Chat input
    private TextFieldWidget chatInputWidget;

    public DiscordMainScreen(Screen parent) {
        super(Text.literal("Discord"));
        this.parent = parent;
    }

    // ── YACL trick: override renderBackground to remove blur ─────────────────
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, C_BG);
    }

    @Override
    protected void init() {
        if (!DiscordMC.discord.isLoggedIn()) {
            inLogin = true;
            tokenField = new TextFieldWidget(textRenderer,
                width / 2 - 140, height / 2 + 10, 280, 22, Text.literal(""));
            tokenField.setPlaceholder(Text.literal("§8Your Discord token..."));
            tokenField.setMaxLength(200);
            addDrawableChild(tokenField);
            setInitialFocus(tokenField);
            return;
        }

        inLogin = false;

        if (dms.isEmpty() && !loadingDMs) {
            loadingDMs = true;
            DiscordMC.discord.fetchDMs().thenAccept(list -> { dms = list; loadingDMs = false; });
            DiscordMC.discord.fetchGuilds().thenAccept(list -> servers = list);
        }

        if (inChat && openChId != null) {
            chatInputWidget = new TextFieldWidget(textRenderer,
                GUILDBAR + SIDEBAR + 16, height - 30,
                width - GUILDBAR - SIDEBAR - 24, 20, Text.literal(""));
            chatInputWidget.setPlaceholder(Text.literal("§8Message #" + openChName + "..."));
            chatInputWidget.setMaxLength(2000);
            addDrawableChild(chatInputWidget);
            setInitialFocus(chatInputWidget);

            DiscordMC.discord.onMessage(msg -> {
                if (msg.get("channel_id").getAsString().equals(openChId)) {
                    messages.add(msg);
                    scrollToBottom();
                }
            });
        }
    }

    private void scrollToBottom() {
        chatScroll = Math.max(0, messages.size() * 38 - (height - TOPBAR - 50));
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Background already drawn in renderBackground
        ctx.fill(0, 0, width, height, C_BG);

        if (inLogin) { renderLogin(ctx, mx, my); super.render(ctx, mx, my, delta); return; }

        renderGuildBar(ctx, mx, my);
        renderSidebar(ctx, mx, my);
        renderMainArea(ctx, mx, my);

        super.render(ctx, mx, my, delta);
    }

    private void renderLogin(DrawContext ctx, int mx, int my) {
        int pw = 360, ph = 240;
        int px = width / 2 - pw / 2, py = height / 2 - ph / 2 - 10;

        // Shadow
        ctx.fill(px + 4, py + 4, px + pw + 4, py + ph + 4, 0x44000000);
        // Panel
        ctx.fill(px, py, px + pw, py + ph, C_SIDEBAR);
        // Top accent
        ctx.fill(px, py, px + pw, py + 4, C_ACCENT);

        // Discord icon box
        int ix = width / 2 - 26, iy = py + 18;
        ctx.fill(ix, iy, ix + 52, iy + 52, C_ACCENT);
        // Inner icon bg
        ctx.fill(ix + 4, iy + 4, ix + 48, iy + 48, 0xFF4752C4);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§f§lD"), width / 2, iy + 20, C_WHITE);

        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§f§lWelcome back!"), width / 2, py + 82, C_WHITE);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§7Enter your Discord token"), width / 2, py + 96, C_MUTED);

        // Token label
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§8TOKEN"), px + 20, py + 114, C_MUTED);

        // Token input bg (YACL style - rounded look with border)
        ctx.fill(px + 16, py + 126, px + pw - 16, py + 152, C_DARK);
        ctx.fill(px + 17, py + 127, px + pw - 17, py + 151, C_INPUT);

        // Status
        if (!loginMsg.isEmpty())
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(loginMsg), width / 2, py + 158, loginMsgColor);

        // Login button
        int bx = width / 2 - 80, by = py + 170;
        boolean hovLogin = mx >= bx && mx <= bx + 160 && my >= by && my <= by + 26;
        ctx.fill(bx, by, bx + 160, by + 26, hovLogin ? 0xFF4752C4 : C_ACCENT);
        // Button shine
        ctx.fill(bx, by, bx + 160, by + 1, 0x22FFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§f§lLogin"), width / 2, by + 9, C_WHITE);

        // Back link
        boolean hovBack = mx >= width / 2 - 50 && mx <= width / 2 + 50
                       && my >= py + ph - 18 && my <= py + ph - 4;
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal(hovBack ? "§b← Back to Minecraft" : "§8← Back to Minecraft"),
            width / 2, py + ph - 16, hovBack ? 0xFF00B0F4 : C_MUTED);
    }

    private void renderGuildBar(DrawContext ctx, int mx, int my) {
        ctx.fill(0, 0, GUILDBAR, height, C_DARK);

        // Home button
        boolean homeHov = mx >= 10 && mx <= 58 && my >= 10 && my <= 52;
        // Pill indicator
        if (!inChat || true) ctx.fill(0, 20, 3, 42, C_WHITE);
        ctx.fill(10, 10, 58, 52, homeHov ? C_ACCENT : (inChat ? C_SIDEBAR : C_ACCENT));
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§f⌂"), 34, 24, C_WHITE);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§8DMs"), 34, 36, C_MUTED);

        // Divider
        ctx.fill(14, 58, 54, 60, C_SIDEBAR);

        // Server icons
        int sy = 66;
        for (int i = 0; i < Math.min(servers.size(), 6); i++) {
            JsonObject s = servers.get(i);
            boolean sh = mx >= 10 && mx <= 58 && my >= sy && my <= sy + 42;
            // Shadow
            ctx.fill(12, sy + 2, 60, sy + 44, 0x33000000);
            ctx.fill(10, sy, 58, sy + 42, sh ? C_ACCENT : C_HOVER);
            // Shine
            ctx.fill(10, sy, 58, sy + 1, 0x22FFFFFF);
            String name = s.get("name").getAsString();
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§f§l" + getAbbr(name)), 34, sy + 15, C_WHITE);
            sy += 48;
        }
    }

    private void renderSidebar(DrawContext ctx, int mx, int my) {
        ctx.fill(GUILDBAR, 0, GUILDBAR + SIDEBAR, height, C_SIDEBAR);

        // Search bar
        ctx.fill(GUILDBAR + 8, 8, GUILDBAR + SIDEBAR - 8, 34, C_INPUT);
        ctx.fill(GUILDBAR + 9, 9, GUILDBAR + SIDEBAR - 9, 33, C_DARK);
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§8🔍 Find a conversation"), GUILDBAR + 14, 16, C_MUTED);

        // Header
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§8DIRECT MESSAGES"), GUILDBAR + 10, 42, C_MUTED);

        // DM entries
        int y = 58 - listScroll;
        for (int i = 0; i < dms.size(); i++) {
            if (y + 44 < 58 || y > height - 60) { y += 46; continue; }
            JsonObject dm = dms.get(i);
            String name = getDMName(dm);
            boolean active = inChat && dm.get("id").getAsString().equals(openChId);
            boolean hov = mx >= GUILDBAR + 4 && mx <= GUILDBAR + SIDEBAR - 4
                       && my >= y && my <= y + 42;

            // Row bg
            if (active) ctx.fill(GUILDBAR + 2, y, GUILDBAR + SIDEBAR - 2, y + 42, C_HOVER);
            else if (hov) ctx.fill(GUILDBAR + 4, y, GUILDBAR + SIDEBAR - 4, y + 42, C_CARD);

            // Active indicator
            if (active) ctx.fill(GUILDBAR + 2, y + 8, GUILDBAR + 4, y + 34, C_WHITE);

            // Avatar circle (YACL style rounded)
            int ax = GUILDBAR + 14, ay = y + 9;
            ctx.fill(ax, ay, ax + 26, ay + 26, 0xFF3C4270);
            ctx.fill(ax + 1, ay + 1, ax + 25, ay + 25, C_ACCENT);
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§f§l" + name.substring(0, 1).toUpperCase()),
                ax + 13, ay + 9, C_WHITE);

            // Online dot
            ctx.fill(ax + 18, ay + 18, ax + 26, ay + 26, C_DARK);
            ctx.fill(ax + 19, ay + 19, ax + 25, ay + 25, C_GREEN);

            // Name + status
            ctx.drawTextWithShadow(textRenderer,
                Text.literal((active || hov ? "§f§l" : "§7") + truncate(name, 18)),
                GUILDBAR + 46, y + 11, active ? C_WHITE : C_TEXT);
            ctx.drawTextWithShadow(textRenderer,
                Text.literal("§aOnline"), GUILDBAR + 46, y + 24, C_GREEN);

            y += 46;
        }

        if (loadingDMs)
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7Loading..."), GUILDBAR + SIDEBAR / 2, 100, C_MUTED);

        // User panel
        renderUserPanel(ctx);
    }

    private void renderUserPanel(DrawContext ctx) {
        ctx.fill(GUILDBAR, height - 58, GUILDBAR + SIDEBAR, height, 0xFF292B2F);
        ctx.fill(GUILDBAR, height - 58, GUILDBAR + SIDEBAR, height - 57, C_DARK);

        if (DiscordMC.discord.getSelfUser() == null) return;
        String uname = DiscordMC.discord.getSelfUser().get("username").getAsString();

        // Avatar
        ctx.fill(GUILDBAR + 8, height - 50, GUILDBAR + 42, height - 16, 0xFF3C4270);
        ctx.fill(GUILDBAR + 9, height - 49, GUILDBAR + 41, height - 17, C_ACCENT);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§f§l" + uname.substring(0, 1).toUpperCase()),
            GUILDBAR + 25, height - 37, C_WHITE);
        // Online dot
        ctx.fill(GUILDBAR + 32, height - 25, GUILDBAR + 42, height - 16, 0xFF292B2F);
        ctx.fill(GUILDBAR + 33, height - 24, GUILDBAR + 41, height - 17, C_GREEN);

        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§f§l" + truncate(uname, 14)), GUILDBAR + 50, height - 46, C_WHITE);
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§a● Online"), GUILDBAR + 50, height - 32, C_GREEN);
    }

    private void renderMainArea(DrawContext ctx, int mx, int my) {
        int cx = GUILDBAR + SIDEBAR;
        int cw = width - cx;

        ctx.fill(cx, 0, width, height, C_BG);
        ctx.fill(cx, 0, width, TOPBAR, C_SIDEBAR);
        ctx.fill(cx, TOPBAR, width, TOPBAR + 1, C_DARK);

        if (!inChat) {
            ctx.fill(cx, TOPBAR, width, height, C_BG);
            // Empty state icon
            ctx.fill(cx + cw / 2 - 30, height / 2 - 50, cx + cw / 2 + 30, height / 2 - 10, C_SIDEBAR);
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§f§l💬"), cx + cw / 2, height / 2 - 40, C_MUTED);
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§f§lSelect a conversation"), cx + cw / 2, height / 2 + 4, C_TEXT);
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7Pick a DM from the left sidebar"), cx + cw / 2, height / 2 + 18, C_MUTED);
            ctx.drawTextWithShadow(textRenderer,
                Text.literal("§f§lDirect Messages"), cx + 14, 14, C_WHITE);
            return;
        }

        // Chat header
        ctx.fill(cx + 4, 14, cx + 8, 30, C_ACCENT);
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§f§l# " + openChName), cx + 14, 14, C_WHITE);
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§8ESC = back"), width - 80, 16, C_MUTED);

        // Messages
        ctx.fill(cx, TOPBAR, width, height - 40, C_BG);

        if (messages.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7No messages yet"), cx + cw / 2, height / 2, C_MUTED);
        } else {
            int y = TOPBAR + 8 - chatScroll;
            for (JsonObject msg : messages) {
                if (y + 40 < TOPBAR || y > height - 40) { y += 38; continue; }

                String author = msg.has("author")
                    ? msg.getAsJsonObject("author").get("username").getAsString()
                    : "Unknown";
                String content = msg.has("content") ? msg.get("content").getAsString() : "";

                // Avatar
                ctx.fill(cx + 8, y, cx + 32, y + 24, 0xFF3C4270);
                ctx.fill(cx + 9, y + 1, cx + 31, y + 23, C_ACCENT);
                ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("§f" + author.substring(0, 1).toUpperCase()),
                    cx + 20, y + 8, C_WHITE);

                // Author + message
                ctx.drawTextWithShadow(textRenderer,
                    Text.literal("§b§l" + author), cx + 38, y, C_ACCENT);

                // Wrap content
                int maxLen = (cw - 60) / 6;
                if (content.length() <= maxLen) {
                    ctx.drawTextWithShadow(textRenderer,
                        Text.literal("§f" + content), cx + 38, y + 12, C_TEXT);
                } else {
                    ctx.drawTextWithShadow(textRenderer,
                        Text.literal("§f" + content.substring(0, maxLen)), cx + 38, y + 12, C_TEXT);
                    if (content.length() > maxLen)
                        ctx.drawTextWithShadow(textRenderer,
                            Text.literal("§f" + content.substring(maxLen, Math.min(maxLen * 2, content.length()))),
                            cx + 38, y + 22, C_TEXT);
                }

                y += 38;
            }
        }

        // Input bar
        ctx.fill(cx, height - 40, width, height, C_SIDEBAR);
        ctx.fill(cx + 8, height - 33, width - 8, height - 7, C_DARK);
        ctx.fill(cx + 9, height - 32, width - 9, height - 8, C_INPUT);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getDMName(JsonObject dm) {
        try {
            if (dm.has("recipients") && dm.getAsJsonArray("recipients").size() > 0)
                return dm.getAsJsonArray("recipients").get(0).getAsJsonObject().get("username").getAsString();
        } catch (Exception ignored) {}
        return "Unknown";
    }

    private String getAbbr(String name) {
        StringBuilder sb = new StringBuilder();
        for (String w : name.split("[ _\\-]"))
            if (!w.isEmpty()) { sb.append(w.charAt(0)); if (sb.length() >= 2) break; }
        return sb.length() > 0 ? sb.toString().toUpperCase() : "?";
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 2) + ".." : s;
    }

    // ── Events ────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (inLogin) {
            int py = height / 2 - 130;
            if (mx >= width / 2 - 80 && mx <= width / 2 + 80 && my >= py + 170 && my <= py + 196) {
                doLogin(); return true;
            }
            if (my >= py + 224) { client.setScreen(parent); return true; }
            return super.mouseClicked(mx, my, btn);
        }

        if (mx >= 10 && mx <= 58 && my >= 10 && my <= 52) {
            inChat = false; openChId = null; clearAndInit(); return true;
        }

        if (mx >= GUILDBAR + 4 && mx <= GUILDBAR + SIDEBAR - 4) {
            int y = 58 - listScroll;
            for (JsonObject dm : dms) {
                if (my >= y && my <= y + 42) {
                    openChat(dm.get("id").getAsString(), getDMName(dm)); return true;
                }
                y += 46;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if (key == 257 || key == 335) {
            if (inLogin) { doLogin(); return true; }
            if (inChat && chatInputWidget != null) {
                String txt = chatInputWidget.getText().trim();
                if (!txt.isEmpty()) {
                    DiscordMC.discord.sendMessage(openChId, txt);
                    chatInputWidget.setText("");
                }
                return true;
            }
        }
        if (key == 256) {
            if (inChat) { inChat = false; openChId = null; clearAndInit(); return true; }
            client.setScreen(parent); return true;
        }
        return super.keyPressed(key, scan, mod);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (mx < GUILDBAR + SIDEBAR) listScroll = Math.max(0, listScroll - (int)(v * 10));
        else if (inChat) chatScroll = Math.max(0, chatScroll - (int)(v * 10));
        return true;
    }

    private void doLogin() {
        if (tokenField == null) return;
        String t = tokenField.getText().trim();
        if (t.isEmpty()) { loginMsg = "§cToken cannot be empty!"; loginMsgColor = C_RED; return; }
        loginMsg = "§7Connecting..."; loginMsgColor = C_MUTED;
        DiscordMC.discord.loginWithToken(t).thenAccept(ok -> client.execute(() -> {
            if (ok) { inLogin = false; clearAndInit(); }
            else { loginMsg = "§cInvalid token!"; loginMsgColor = C_RED; }
        }));
    }

    private void openChat(String chId, String chName) {
        openChId = chId; openChName = chName; inChat = true;
        messages.clear(); chatScroll = 0;
        DiscordMC.discord.fetchMessages(chId).thenAccept(msgs -> {
            messages = msgs; scrollToBottom();
        });
        clearAndInit();
    }

    @Override public boolean shouldPause() { return false; }
}
