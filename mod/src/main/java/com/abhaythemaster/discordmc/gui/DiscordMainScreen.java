package com.abhaythemaster.discordmc.gui;

import com.abhaythemaster.discordmc.DiscordMC;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class DiscordMainScreen extends Screen {
    private final Screen parent;

    // Colors
    private static final int BG        = 0xFF0A0E1A;
    private static final int COL1      = 0xFF060910; // guild bar
    private static final int COL2      = 0xFF0D1520; // sidebar
    private static final int COL3      = 0xFF111827; // main area
    private static final int TOPBAR    = 0xFF0D1520;
    private static final int ACCENT    = 0xFF00D4FF;
    private static final int ACCENT2   = 0xFF0099CC;
    private static final int ACCENT_DIM= 0x3300D4FF;
    private static final int GREEN     = 0xFF00E676;
    private static final int YELLOW    = 0xFFFFCC00;
    private static final int RED_C     = 0xFFFF5252;
    private static final int OFFLINE   = 0xFF546E7A;
    private static final int TEXT_HI   = 0xFFECEFF1;
    private static final int TEXT_LO   = 0xFF78909C;
    private static final int WHITE     = 0xFFFFFFFF;
    private static final int DIVIDER   = 0xFF1A2535;
    private static final int INPUT_BG  = 0xFF0A1628;
    private static final int CARD_HOV  = 0xFF0F1E30;
    private static final int CARD_ACT  = 0xFF142840;

    // Layout — NO OVERLAP
    private static final int GW = 56;  // guild bar width
    private static final int SW = 200; // sidebar width
    private static final int TH = 44;  // top bar height
    private static final int IH = 38;  // input bar height

    private List<JsonObject> dms      = new ArrayList<>();
    private List<JsonObject> servers  = new ArrayList<>();
    private boolean loading           = false;
    private int dmScroll              = 0;
    private String openChId           = null;
    private String openChName         = null;
    private List<JsonObject> messages = new ArrayList<>();
    private int msgScroll             = 0;
    private boolean inChat            = false;
    private boolean inLogin           = false;

    private TextFieldWidget tokenField;
    private TextFieldWidget chatField;
    private String loginMsg           = "";
    private int loginMsgColor         = TEXT_LO;

    public DiscordMainScreen(Screen parent) {
        super(Text.literal("Discord"));
        this.parent = parent;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, BG);
    }

    @Override
    protected void init() {
        if (!DiscordMC.discord.isLoggedIn()) {
            inLogin = true;
            int fw = 260, fx = width / 2 - fw / 2, fy = height / 2 + 4;
            tokenField = new TextFieldWidget(textRenderer, fx, fy, fw, 20, Text.literal(""));
            tokenField.setPlaceholder(Text.literal("§8Token paste karo..."));
            tokenField.setMaxLength(200);
            addDrawableChild(tokenField);
            setInitialFocus(tokenField);
            return;
        }
        inLogin = false;
        if (dms.isEmpty() && !loading) {
            loading = true;
            DiscordMC.discord.fetchDMs().thenAccept(l -> { dms = l; loading = false; });
            DiscordMC.discord.fetchGuilds().thenAccept(l -> servers = l);
        }
        if (inChat) {
            int fx = GW + SW + 8, fw = width - GW - SW - 16;
            chatField = new TextFieldWidget(textRenderer, fx, height - IH + 9, fw, 20, Text.literal(""));
            chatField.setPlaceholder(Text.literal("§8Message #" + openChName + "..."));
            chatField.setMaxLength(2000);
            addDrawableChild(chatField);
            setInitialFocus(chatField);
            DiscordMC.discord.onMessage(m -> {
                if (m.get("channel_id").getAsString().equals(openChId)) {
                    messages.add(m);
                    autoScroll();
                }
            });
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Base
        ctx.fill(0, 0, width, height, BG);

        if (inLogin) { renderLogin(ctx, mx, my); super.render(ctx, mx, my, delta); return; }

        renderGuildBar(ctx, mx, my);
        renderSidebar(ctx, mx, my);
        renderMain(ctx, mx, my);

        super.render(ctx, mx, my, delta);
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    private void renderLogin(DrawContext ctx, int mx, int my) {
        int pw = 320, ph = 240;
        int px = width / 2 - pw / 2, py = height / 2 - ph / 2;

        // Outer glow
        ctx.fill(px-4, py-4, px+pw+4, py+ph+4, 0x1500D4FF);
        // Card
        ctx.fill(px, py, px+pw, py+ph, COL2);
        // Top cyan bar
        ctx.fill(px, py, px+pw, py+3, ACCENT);
        // Left cyan bar
        ctx.fill(px, py, px+1, py+ph, ACCENT);

        // Icon
        int ix = width/2-22, iy = py+16;
        ctx.fill(ix, iy, ix+44, iy+44, 0xFF060910);
        ctx.fill(ix+2, iy+2, ix+42, iy+42, ACCENT_DIM);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§b§lDC"), width/2, iy+16, ACCENT);

        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§f§lDiscord MC"), width/2, py+70, TEXT_HI);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§7Login with your token"), width/2, py+84, TEXT_LO);

        // Token label
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§bTOKEN"), px+20, py+100, ACCENT);
        // Token field bg
        ctx.fill(px+16, py+112, px+pw-16, py+136, INPUT_BG);
        ctx.fill(px+16, py+136, px+pw-16, py+137, ACCENT);

        // Status
        if (!loginMsg.isEmpty())
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(loginMsg), width/2, py+144, loginMsgColor);

        // Button
        int bx = width/2-70, by = py+156;
        boolean hov = mx>=bx && mx<=bx+140 && my>=by && my<=by+24;
        ctx.fill(bx, by, bx+140, by+24, hov ? ACCENT : ACCENT2);
        ctx.fill(bx, by, bx+140, by+1, 0x5500FFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§0§lLogin"), width/2, by+8, WHITE);

        // Back
        boolean hb = mx>=width/2-40 && mx<=width/2+40 && my>=py+ph-18 && my<=py+ph-4;
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal(hb ? "§b← Back" : "§8← Back"), width/2, py+ph-16, hb?ACCENT:TEXT_LO);
    }

    // ── Guild Bar (leftmost, fixed width GW) ─────────────────────────────────
    private void renderGuildBar(DrawContext ctx, int mx, int my) {
        ctx.fill(0, 0, GW, height, COL1);
        // Right border
        ctx.fill(GW-1, 0, GW, height, DIVIDER);

        // Home/DMs button
        boolean hh = mx>=4 && mx<=GW-4 && my>=8 && my<=46;
        ctx.fill(4, 8, GW-4, 46, hh||!inChat ? CARD_ACT : COL1);
        if (hh||!inChat) ctx.fill(4, 8, GW-4, 9, ACCENT);
        // House icon
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§b#"), GW/2, 18, hh ? ACCENT : TEXT_LO);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§8DMs"), GW/2, 30, TEXT_LO);

        // Divider
        ctx.fill(8, 52, GW-8, 53, DIVIDER);

        // Server icons
        int sy = 58;
        for (int i = 0; i < Math.min(servers.size(), 7); i++) {
            JsonObject s = servers.get(i);
            boolean sh = mx>=4 && mx<=GW-4 && my>=sy && my<=sy+36;
            ctx.fill(4, sy, GW-4, sy+36, sh ? CARD_ACT : COL1);
            if (sh) ctx.fill(4, sy, GW-4, sy+1, ACCENT);
            // Icon bg
            ctx.fill(8, sy+4, GW-8, sy+32, INPUT_BG);
            // Abbr
            String abbr = getServerAbbr(s.get("name").getAsString());
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§b" + abbr), GW/2, sy+12, sh ? ACCENT : ACCENT2);
            sy += 40;
        }
    }

    // ── Sidebar (DM list) ─────────────────────────────────────────────────────
    private void renderSidebar(DrawContext ctx, int mx, int my) {
        // Sidebar starts at GW, ends at GW+SW
        ctx.fill(GW, 0, GW+SW, height, COL2);
        ctx.fill(GW+SW-1, 0, GW+SW, height, DIVIDER);

        // Header
        ctx.fill(GW, 0, GW+SW, TH, COL1);
        ctx.fill(GW, TH-1, GW+SW, TH, ACCENT);
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§f§lDirect Messages"), GW+10, TH/2-4, TEXT_HI);

        // Section label
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§8MESSAGES"), GW+10, TH+8, TEXT_LO);

        if (loading) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7Loading..."), GW+SW/2, height/2, TEXT_LO);
            renderUserPanel(ctx);
            return;
        }

        int y = TH+22 - dmScroll;
        for (JsonObject dm : dms) {
            if (y+40 < TH+22 || y > height-52) { y+=42; continue; }
            String name = getDMName(dm);
            String uid  = getDMUserId(dm);
            boolean active = inChat && openChId!=null && openChId.equals(dm.get("id").getAsString());
            boolean hov = mx>=GW+2 && mx<=GW+SW-2 && my>=y && my<=y+40;

            // Row background
            if (active) {
                ctx.fill(GW+2, y, GW+SW-2, y+40, CARD_ACT);
                ctx.fill(GW+2, y, GW+4, y+40, ACCENT); // active indicator
            } else if (hov) {
                ctx.fill(GW+2, y, GW+SW-2, y+40, CARD_HOV);
            }

            // Avatar — fixed size box, no overlap
            int ax = GW+8, ay = y+6;
            ctx.fill(ax, ay, ax+28, ay+28, INPUT_BG);
            ctx.fill(ax+1, ay+1, ax+27, ay+27, ACCENT_DIM);
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§b" + name.substring(0,1).toUpperCase()),
                ax+14, ay+9, active ? ACCENT : ACCENT2);

            // Presence dot (8x8, bottom right of avatar)
            int pColor = presenceColor(DiscordMC.discord.getPresence(uid));
            ctx.fill(ax+20, ay+20, ax+28, ay+28, COL2); // border
            ctx.fill(ax+21, ay+21, ax+27, ay+27, pColor);

            // Name + status — starts AFTER avatar (ax+28+6 = ax+34)
            int tx = ax+34;
            ctx.drawTextWithShadow(textRenderer,
                Text.literal((active?"§f§l":"§7") + clip(name, 16)),
                tx, y+9, active ? TEXT_HI : TEXT_LO);
            ctx.drawTextWithShadow(textRenderer,
                Text.literal(presenceLabel(DiscordMC.discord.getPresence(uid))),
                tx, y+22, pColor);

            y += 42;
        }

        if (dms.isEmpty())
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7No DMs"), GW+SW/2, height/2, TEXT_LO);

        renderUserPanel(ctx);
    }

    private void renderUserPanel(DrawContext ctx) {
        ctx.fill(GW, height-52, GW+SW, height, COL1);
        ctx.fill(GW, height-52, GW+SW, height-51, DIVIDER);

        if (DiscordMC.discord.getSelfUser()==null) return;
        String name = DiscordMC.discord.getSelfUser().get("username").getAsString();

        // Avatar
        int ax = GW+8, ay = height-44;
        ctx.fill(ax, ay, ax+32, ay+32, INPUT_BG);
        ctx.fill(ax+1, ay+1, ax+31, ay+31, ACCENT_DIM);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§b" + name.substring(0,1).toUpperCase()),
            ax+16, ay+11, ACCENT);
        // Online dot
        ctx.fill(ax+23, ay+23, ax+32, ay+32, COL1);
        ctx.fill(ax+24, ay+24, ax+31, ay+31, GREEN);

        // Name + status
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§f" + clip(name,13)), GW+48, height-42, TEXT_HI);
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§aOnline"), GW+48, height-28, GREEN);
    }

    // ── Main Content ──────────────────────────────────────────────────────────
    private void renderMain(DrawContext ctx, int mx, int my) {
        int cx = GW+SW;
        int cw = width-cx;

        ctx.fill(cx, 0, width, height, COL3);

        // Top bar
        ctx.fill(cx, 0, width, TH, TOPBAR);
        ctx.fill(cx, TH-1, width, TH, ACCENT);

        if (!inChat) {
            // Empty state
            ctx.drawTextWithShadow(textRenderer,
                Text.literal("§f§lDMs"), cx+14, TH/2-4, TEXT_HI);
            int ey = height/2-30;
            ctx.fill(cx+cw/2-50, ey, cx+cw/2+50, ey+60, COL2);
            ctx.fill(cx+cw/2-50, ey, cx+cw/2+50, ey+2, ACCENT);
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§b#"), cx+cw/2, ey+16, ACCENT);
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§fSelect a DM"), cx+cw/2, ey+32, TEXT_HI);
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§8from the left"), cx+cw/2, ey+44, TEXT_LO);
            return;
        }

        // Chat header
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§b# §f§l" + openChName), cx+14, TH/2-4, TEXT_HI);
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§8ESC"), width-28, TH/2-4, TEXT_LO);

        // Messages area
        ctx.fill(cx, TH, width, height-IH, COL3);

        if (messages.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7No messages"), cx+cw/2, height/2, TEXT_LO);
        } else {
            int y = TH+8 - msgScroll;
            for (JsonObject msg : messages) {
                if (y+36<TH || y>height-IH) { y+=38; continue; }
                String author  = msg.has("author") ? msg.getAsJsonObject("author").get("username").getAsString() : "?";
                String content = msg.has("content") ? msg.get("content").getAsString() : "";
                String aId     = msg.has("author") ? msg.getAsJsonObject("author").get("id").getAsString() : "";
                boolean self   = DiscordMC.discord.getSelfUser()!=null
                              && aId.equals(DiscordMC.discord.getSelfUser().get("id").getAsString());

                // Mini avatar
                int ax = cx+8, ay = y+1;
                ctx.fill(ax, ay, ax+22, ay+22, INPUT_BG);
                ctx.fill(ax+1, ay+1, ax+21, ay+21, ACCENT_DIM);
                ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("§b" + author.substring(0,1).toUpperCase()),
                    ax+11, ay+7, ACCENT);

                // Author name
                ctx.drawTextWithShadow(textRenderer,
                    Text.literal((self?"§b§l":"§e§l") + author), cx+36, y, self?ACCENT:0xFFFFCC00);

                // Content — wrap if too long
                int maxW = cw-48;
                int charsPerLine = Math.max(20, maxW/6);
                if (content.length() <= charsPerLine) {
                    ctx.drawTextWithShadow(textRenderer,
                        Text.literal("§f" + content), cx+36, y+12, TEXT_HI);
                } else {
                    ctx.drawTextWithShadow(textRenderer,
                        Text.literal("§f" + content.substring(0, charsPerLine)), cx+36, y+12, TEXT_HI);
                    if (content.length() > charsPerLine)
                        ctx.drawTextWithShadow(textRenderer,
                            Text.literal("§7" + content.substring(charsPerLine, Math.min(charsPerLine*2, content.length()))),
                            cx+36, y+22, TEXT_LO);
                }
                y += 38;
            }
        }

        // Input bar
        ctx.fill(cx, height-IH, width, height, TOPBAR);
        ctx.fill(cx, height-IH, width, height-IH+1, ACCENT);
        ctx.fill(cx+6, height-IH+8, width-6, height-8, INPUT_BG);
        ctx.fill(cx+6, height-IH+8, width-6, height-IH+9, ACCENT);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String getDMName(JsonObject dm) {
        try { return dm.getAsJsonArray("recipients").get(0).getAsJsonObject().get("username").getAsString(); }
        catch (Exception e) { return "Unknown"; }
    }
    private String getDMUserId(JsonObject dm) {
        try { return dm.getAsJsonArray("recipients").get(0).getAsJsonObject().get("id").getAsString(); }
        catch (Exception e) { return ""; }
    }
    private int presenceColor(String p) {
        return switch(p) { case "online"->"".isEmpty()?GREEN:GREEN; case "idle"->YELLOW; case "dnd"->RED_C; default->OFFLINE; };
    }
    private String presenceLabel(String p) {
        return switch(p) { case "online"->"§aOnline"; case "idle"->"§eIdle"; case "dnd"->"§cDnD"; default->"§8Offline"; };
    }
    private String getServerAbbr(String name) {
        // Take first letter of first word only — no overlap
        String clean = name.trim();
        return clean.isEmpty() ? "?" : String.valueOf(clean.charAt(0)).toUpperCase();
    }
    private String clip(String s, int max) {
        return s.length()>max ? s.substring(0,max-2)+".." : s;
    }
    private void autoScroll() {
        msgScroll = Math.max(0, messages.size()*38-(height-TH-IH-10));
    }

    // ── Events ────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (inLogin) {
            int py = height/2-120;
            // Login button
            if (mx>=width/2-70 && mx<=width/2+70 && my>=py+156 && my<=py+180) { doLogin(); return true; }
            // Back
            if (my>=py+222) { client.setScreen(parent); return true; }
            return super.mouseClicked(mx, my, btn);
        }
        // Home
        if (mx>=4 && mx<=GW-4 && my>=8 && my<=46) {
            inChat=false; openChId=null; clearAndInit(); return true;
        }
        // DM click
        if (mx>=GW+2 && mx<=GW+SW-2) {
            int y = TH+22-dmScroll;
            for (JsonObject dm : dms) {
                if (my>=y && my<=y+40) {
                    openChat(dm.get("id").getAsString(), getDMName(dm)); return true;
                }
                y+=42;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if ((key==257||key==335)) {
            if (inLogin) { doLogin(); return true; }
            if (inChat && chatField!=null) {
                String t = chatField.getText().trim();
                if (!t.isEmpty()) { DiscordMC.discord.sendMessage(openChId, t); chatField.setText(""); }
                return true;
            }
        }
        if (key==256) {
            if (inChat) { inChat=false; openChId=null; clearAndInit(); return true; }
            client.setScreen(parent); return true;
        }
        return super.keyPressed(key, scan, mod);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (mx<GW+SW) dmScroll = Math.max(0, dmScroll-(int)(v*10));
        else if (inChat) msgScroll = Math.max(0, msgScroll-(int)(v*10));
        return true;
    }

    private void doLogin() {
        if (tokenField==null) return;
        String t = tokenField.getText().trim();
        if (t.isEmpty()) { loginMsg="§cToken daalo!"; loginMsgColor=RED_C; return; }
        loginMsg="§7Connecting..."; loginMsgColor=TEXT_LO;
        DiscordMC.discord.loginWithToken(t).thenAccept(ok -> client.execute(() -> {
            if (ok) { inLogin=false; clearAndInit(); }
            else { loginMsg="§cInvalid token!"; loginMsgColor=RED_C; }
        }));
    }

    private void openChat(String id, String name) {
        openChId=id; openChName=name; inChat=true;
        messages.clear(); msgScroll=0;
        DiscordMC.discord.fetchMessages(id).thenAccept(msgs -> { messages=msgs; autoScroll(); });
        clearAndInit();
    }

    @Override public boolean shouldPause() { return false; }
}
