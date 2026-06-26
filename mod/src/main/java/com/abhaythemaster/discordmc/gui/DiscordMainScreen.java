package com.abhaythemaster.discordmc.gui;

import com.abhaythemaster.discordmc.DiscordMC;
import com.google.gson.JsonObject;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class DiscordMainScreen extends Screen {
    private final Screen parent;

    // Discord dark theme colors
    private static final int C_BG        = 0xFF202225;
    private static final int C_SIDEBAR   = 0xFF2F3136;
    private static final int C_HOVER     = 0xFF36393F;
    private static final int C_ACCENT    = 0xFF5865F2;
    private static final int C_GREEN     = 0xFF3BA55D;
    private static final int C_TEXT      = 0xFFDCDDDE;
    private static final int C_MUTED     = 0xFF8E9297;
    private static final int C_INPUT     = 0xFF40444B;
    private static final int C_RED       = 0xFFED4245;
    private static final int C_WHITE     = 0xFFFFFFFF;
    private static final int C_DARK      = 0xFF1E2124;
    private static final int C_CARD      = 0xFF34373C;

    // Layout
    private static final int GUILDBAR  = 68;
    private static final int SIDEBAR   = 230;
    private static final int TOPBAR    = 44;

    // State
    private List<JsonObject> dms = new ArrayList<>();
    private List<JsonObject> servers = new ArrayList<>();
    private boolean loadingDMs = false;
    private int listScroll = 0;
    private String openChId = null, openChName = null;
    private List<JsonObject> messages = new ArrayList<>();
    private int chatScroll = 0;
    private boolean inChat = false;
    private int hovDM = -1;

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

        // Load data
        if (dms.isEmpty() && !loadingDMs) {
            loadingDMs = true;
            DiscordMC.discord.fetchDMs().thenAccept(list -> { dms = list; loadingDMs = false; });
            DiscordMC.discord.fetchGuilds().thenAccept(list -> servers = list);
        }

        // Chat input
        if (inChat && openChId != null) {
            chatInputWidget = new TextFieldWidget(textRenderer,
                GUILDBAR + SIDEBAR + 16, height - 30,
                width - GUILDBAR - SIDEBAR - 24, 20, Text.literal(""));
            chatInputWidget.setPlaceholder(Text.literal("§8Message #" + openChName + "..."));
            chatInputWidget.setMaxLength(2000);
            addDrawableChild(chatInputWidget);
            setInitialFocus(chatInputWidget);

            // Real-time messages
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
        // Base background
        ctx.fill(0, 0, width, height, C_BG);

        if (inLogin) { renderLogin(ctx, mx, my); super.render(ctx, mx, my, delta); return; }

        renderGuildBar(ctx, mx, my);
        renderSidebar(ctx, mx, my);
        renderMainArea(ctx, mx, my);

        super.render(ctx, mx, my, delta);
    }

    // ── Login Screen ──────────────────────────────────────────────────────────

    private void renderLogin(DrawContext ctx, int mx, int my) {
        int pw = 360, ph = 220;
        int px = width / 2 - pw / 2, py = height / 2 - ph / 2 - 20;

        // Panel
        ctx.fill(px, py, px + pw, py + ph, C_SIDEBAR);
        // Top accent bar
        ctx.fill(px, py, px + pw, py + 3, C_ACCENT);

        // Discord icon
        int ix = width / 2 - 24, iy = py + 14;
        ctx.fill(ix, iy, ix + 48, iy + 48, C_ACCENT);
        // D letter
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§f§lD"), width / 2, iy + 17, C_WHITE);

        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§f§lWelcome back!"), width / 2, py + 72, C_WHITE);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§7Enter your Discord token to continue"), width / 2, py + 85, C_MUTED);

        // Token label
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§7TOKEN"), px + 20, py + 106, C_MUTED);

        // Token input bg
        ctx.fill(px + 16, py + 118, px + pw - 16, py + 144, C_INPUT);

        // Status message
        if (!loginMsg.isEmpty())
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(loginMsg), width / 2, py + 150, loginMsgColor);

        // Login button
        int bx = width / 2 - 70, by = py + 162;
        boolean hovLogin = mx >= bx && mx <= bx + 140 && my >= by && my <= by + 24;
        ctx.fill(bx, by, bx + 140, by + 24, hovLogin ? 0xFF4752C4 : C_ACCENT);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§f§lLogin"), width / 2, by + 8, C_WHITE);

        // Back
        boolean hovBack = mx >= width / 2 - 40 && mx <= width / 2 + 40 && my >= py + ph - 20 && my <= py + ph - 4;
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal(hovBack ? "§f← Back" : "§8← Back"), width / 2, py + ph - 18, hovBack ? C_TEXT : C_MUTED);
    }

    // ── Guild Bar (leftmost column) ───────────────────────────────────────────

    private void renderGuildBar(DrawContext ctx, int mx, int my) {
        ctx.fill(0, 0, GUILDBAR, height, C_DARK);

        // Home/DMs button
        int hov = (mx >= 10 && mx <= 58 && my >= 10 && my <= 50) ? 1 : 0;
        ctx.fill(10, 10, 58, 50, hov == 1 || !inChat ? C_ACCENT : C_SIDEBAR);
        // Home icon
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§f⌂"), 34, 24, C_WHITE);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§8DMs"), 34, 36, C_MUTED);

        // Divider
        ctx.fill(14, 56, 54, 58, C_MUTED);

        // Server icons
        int sy = 64;
        for (int i = 0; i < Math.min(servers.size(), 7); i++) {
            JsonObject s = servers.get(i);
            boolean sh = mx >= 10 && mx <= 58 && my >= sy && my <= sy + 40;
            ctx.fill(10, sy, 58, sy + 40, sh ? C_ACCENT : C_HOVER);
            String name = s.get("name").getAsString();
            String abbr = getAbbr(name);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§f" + abbr), 34, sy + 14, C_WHITE);
            sy += 46;
        }
    }

    // ── Sidebar (DM List) ─────────────────────────────────────────────────────

    private void renderSidebar(DrawContext ctx, int mx, int my) {
        ctx.fill(GUILDBAR, 0, GUILDBAR + SIDEBAR, height, C_SIDEBAR);

        // Search bar at top
        ctx.fill(GUILDBAR + 8, 8, GUILDBAR + SIDEBAR - 8, 34, C_INPUT);
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§8Find or start a conversation"), GUILDBAR + 14, 16, C_MUTED);

        // Section header
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§8DIRECT MESSAGES"), GUILDBAR + 10, 44, C_MUTED);

        // DM entries
        int y = 60 - listScroll;
        for (int i = 0; i < dms.size(); i++) {
            if (y + 40 < 60 || y > height - 56) { y += 44; continue; }
            JsonObject dm = dms.get(i);
            String name = getDMName(dm);
            boolean active = inChat && dm.get("id").getAsString().equals(openChId);
            boolean hov = mx >= GUILDBAR + 4 && mx <= GUILDBAR + SIDEBAR - 4
                       && my >= y && my <= y + 40;

            // Row bg
            ctx.fill(GUILDBAR + 4, y, GUILDBAR + SIDEBAR - 4, y + 40,
                active ? C_HOVER : (hov ? C_CARD : 0));

            // Avatar circle
            int ax = GUILDBAR + 14, ay = y + 8;
            ctx.fill(ax, ay, ax + 24, ay + 24, C_ACCENT);
            // Initial letter
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§f§l" + name.substring(0, 1).toUpperCase()),
                ax + 12, ay + 8, C_WHITE);

            // Online dot
            ctx.fill(ax + 17, ay + 17, ax + 24, ay + 24, C_GREEN);

            // Name
            ctx.drawTextWithShadow(textRenderer,
                Text.literal("§f" + truncate(name, 20)), GUILDBAR + 44, y + 10, active || hov ? C_WHITE : C_TEXT);
            ctx.drawTextWithShadow(textRenderer,
                Text.literal("§8Click to open"), GUILDBAR + 44, y + 22, C_MUTED);

            y += 44;
        }

        if (loadingDMs)
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7Loading..."), GUILDBAR + SIDEBAR / 2, 100, C_MUTED);

        // User panel at bottom
        renderUserPanel(ctx);
    }

    private void renderUserPanel(DrawContext ctx) {
        ctx.fill(GUILDBAR, height - 56, GUILDBAR + SIDEBAR, height, 0xFF292B2F);

        if (DiscordMC.discord.getSelfUser() == null) return;
        String uname = DiscordMC.discord.getSelfUser().get("username").getAsString();

        // Avatar
        ctx.fill(GUILDBAR + 8, height - 48, GUILDBAR + 40, height - 16, C_ACCENT);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§f§l" + uname.substring(0, 1).toUpperCase()),
            GUILDBAR + 24, height - 36, C_WHITE);
        // Online dot on avatar
        ctx.fill(GUILDBAR + 30, height - 24, GUILDBAR + 40, height - 16, C_GREEN);

        // Name + status
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§f§l" + truncate(uname, 16)), GUILDBAR + 48, height - 44, C_WHITE);
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§aOnline"), GUILDBAR + 48, height - 30, C_GREEN);
    }

    // ── Main Content Area ─────────────────────────────────────────────────────

    private void renderMainArea(DrawContext ctx, int mx, int my) {
        int cx = GUILDBAR + SIDEBAR;
        int cw = width - cx;

        // Top bar
        ctx.fill(cx, 0, width, TOPBAR, C_SIDEBAR);
        ctx.fill(cx, TOPBAR, width, TOPBAR + 1, C_DARK);

        if (!inChat) {
            // Empty state
            ctx.fill(cx, TOPBAR, width, height, C_BG);
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§f§lNo DM selected"), cx + cw / 2, height / 2 - 10, C_TEXT);
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7Choose a conversation from the left!"), cx + cw / 2, height / 2 + 6, C_MUTED);

            // Header
            ctx.drawTextWithShadow(textRenderer,
                Text.literal("§f§lDirect Messages"), cx + 14, 14, C_WHITE);
            return;
        }

        // Chat header
        ctx.fill(cx, 0, width, TOPBAR, C_SIDEBAR);
        ctx.fill(cx + 6, 14, cx + 10, 30, C_ACCENT);
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§f§l# " + openChName), cx + 16, 14, C_WHITE);

        // Back hint
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§8[ESC to go back]"), width - 90, 16, C_MUTED);

        // Messages area
        ctx.fill(cx, TOPBAR, width, height - 40, C_BG);

        if (messages.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7No messages yet..."), cx + cw / 2, height / 2, C_MUTED);
        } else {
            int y = TOPBAR + 8 - chatScroll;
            for (JsonObject msg : messages) {
                if (y + 38 < TOPBAR || y > height - 40) { y += 38; continue; }

                String author = "Unknown";
                if (msg.has("author"))
                    author = msg.getAsJsonObject("author").get("username").getAsString();
                String content = msg.has("content") ? msg.get("content").getAsString() : "";

                // Avatar
                ctx.fill(cx + 8, y + 2, cx + 30, y + 24, C_ACCENT);
                ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("§f" + author.substring(0, 1).toUpperCase()),
                    cx + 19, y + 9, C_WHITE);

                // Author name
                ctx.drawTextWithShadow(textRenderer,
                    Text.literal("§f§l" + author), cx + 36, y + 2, C_ACCENT);

                // Message content (wrap at 60 chars)
                if (content.length() <= 65) {
                    ctx.drawTextWithShadow(textRenderer,
                        Text.literal("§f" + content), cx + 36, y + 14, C_TEXT);
                } else {
                    ctx.drawTextWithShadow(textRenderer,
                        Text.literal("§f" + content.substring(0, 65)), cx + 36, y + 14, C_TEXT);
                    ctx.drawTextWithShadow(textRenderer,
                        Text.literal("§f" + content.substring(65, Math.min(130, content.length()))),
                        cx + 36, y + 24, C_TEXT);
                }

                y += 38;
            }
        }

        // Input bar background
        ctx.fill(cx, height - 40, width, height, C_SIDEBAR);
        ctx.fill(cx + 8, height - 32, width - 8, height - 8, C_INPUT);
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
        for (String w : name.split("[ _-]")) if (!w.isEmpty()) { sb.append(w.charAt(0)); if (sb.length() >= 2) break; }
        return sb.length() > 0 ? sb.toString().toUpperCase() : "?";
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 2) + ".." : s;
    }

    // ── Mouse & Keyboard ──────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (inLogin) {
            int py = height / 2 - 130;
            // Login button
            if (mx >= width / 2 - 70 && mx <= width / 2 + 70 && my >= py + 162 && my <= py + 186) {
                doLogin(); return true;
            }
            // Back
            if (my >= py + 202) { client.setScreen(parent); return true; }
            return super.mouseClicked(mx, my, btn);
        }

        // Home button
        if (mx >= 10 && mx <= 58 && my >= 10 && my <= 50) {
            inChat = false; openChId = null; clearAndInit(); return true;
        }

        // DM clicks
        if (mx >= GUILDBAR + 4 && mx <= GUILDBAR + SIDEBAR - 4) {
            int y = 60 - listScroll;
            for (JsonObject dm : dms) {
                if (my >= y && my <= y + 40) {
                    openChat(dm.get("id").getAsString(), getDMName(dm)); return true;
                }
                y += 44;
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if (key == 257 || key == 335) { // Enter
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
        if (key == 256) { // ESC
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
        loginMsg = "§7Connecting to Discord..."; loginMsgColor = C_MUTED;
        DiscordMC.discord.loginWithToken(t).thenAccept(ok -> client.execute(() -> {
            if (ok) { inLogin = false; clearAndInit(); }
            else { loginMsg = "§cInvalid token! Please try again."; loginMsgColor = C_RED; }
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
