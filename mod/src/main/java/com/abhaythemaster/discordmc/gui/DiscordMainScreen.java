package com.abhaythemaster.discordmc.gui;

import com.abhaythemaster.discordmc.DiscordMC;
import com.google.gson.JsonObject;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DiscordMainScreen extends BaseOwoScreen<FlowLayout> {

    private final net.minecraft.client.gui.screen.Screen parent;

    // Colors
    private static final int BG        = 0xFF0A0E1A;
    private static final int GLASS     = 0xCC0D1828;
    private static final int GLASS2    = 0xAA111E30;
    private static final int ACCENT    = 0xFF00D4FF;
    private static final int ACCENT_DIM= 0x4400D4FF;
    private static final int BORDER    = 0x6600D4FF;
    private static final int GREEN     = 0xFF00FF9D;
    private static final int YELLOW    = 0xFFFFCC00;
    private static final int RED_C     = 0xFFFF5252;
    private static final int OFFLINE   = 0xFF546E7A;
    private static final int TEXT_HI   = 0xFFECEFF1;
    private static final int TEXT_LO   = 0xFF78909C;
    private static final int WHITE     = 0xFFFFFFFF;
    private static final int INPUT_BG  = 0xFF060C18;

    // Layout
    private static final int GW = 56, SW = 210, TH = 44, IH = 40;

    // State
    private List<JsonObject> dms = new ArrayList<>();
    private List<JsonObject> servers = new ArrayList<>();
    private boolean loading = false;
    private int dmScroll = 0, msgScroll = 0;
    private String openChId = null, openChName = null;
    private List<JsonObject> messages = new ArrayList<>();
    private boolean inChat = false, inLogin = false;
    private TextFieldWidget tokenField, chatField;
    private String loginMsg = "";
    private int loginMsgColor = TEXT_LO;

    public DiscordMainScreen(net.minecraft.client.gui.screen.Screen parent) {
        super(Text.literal("Discord"));
        this.parent = parent;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        root.surface(Surface.flat(BG))
            .horizontalAlignment(HorizontalAlignment.LEFT)
            .verticalAlignment(VerticalAlignment.TOP)
            .padding(Insets.none());
    }

    @Override
    protected void init() {
        super.init();

        if (!DiscordMC.discord.isLoggedIn()) {
            inLogin = true;
            int fw = 260, fx = width/2 - fw/2, fy = height/2 + 4;
            tokenField = new TextFieldWidget(textRenderer, fx, fy, fw, 22, Text.literal(""));
            tokenField.setPlaceholder(Text.literal("§8Discord token paste karo..."));
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

        if (inChat && openChId != null) {
            int fx = GW + SW + 10, fw = width - GW - SW - 18;
            chatField = new TextFieldWidget(textRenderer, fx, height - IH + 10, fw, 20, Text.literal(""));
            chatField.setPlaceholder(Text.literal("§8Message #" + openChName));
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
        // Deep dark bg
        ctx.fill(0, 0, width, height, BG);

        // Subtle grid
        for (int x = 0; x < width; x += 48) ctx.fill(x, 0, x+1, height, 0x08FFFFFF);
        for (int y = 0; y < height; y += 48) ctx.fill(0, y, width, y+1, 0x08FFFFFF);

        // Top glow
        ctx.fill(0, 0, width, 2, 0x2200D4FF);

        if (inLogin) { renderLogin(ctx, mx, my); super.render(ctx, mx, my, delta); return; }

        renderGuildBar(ctx, mx, my);
        renderSidebar(ctx, mx, my);
        renderMain(ctx, mx, my);
        super.render(ctx, mx, my, delta);
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    private void renderLogin(DrawContext ctx, int mx, int my) {
        int pw = 340, ph = 260, px = width/2 - pw/2, py = height/2 - ph/2;

        // Glow
        ctx.fill(px-8, py-8, px+pw+8, py+ph+8, 0x0800D4FF);
        ctx.fill(px-4, py-4, px+pw+4, py+ph+4, 0x1500D4FF);

        // Glass card (owo-style)
        ctx.fill(px, py, px+pw, py+ph, GLASS);
        // Cyan borders
        ctx.fill(px, py, px+pw, py+2, ACCENT);      // top
        ctx.fill(px, py, px+1, py+ph, BORDER);       // left
        ctx.fill(px+pw-1, py, px+pw, py+ph, BORDER); // right
        ctx.fill(px, py+ph-1, px+pw, py+ph, BORDER); // bottom

        // Inner glow line
        ctx.fill(px+1, py+2, px+pw-1, py+3, ACCENT_DIM);

        // DC icon
        int ix = width/2-26, iy = py+18;
        ctx.fill(ix-2, iy-2, ix+54, iy+54, ACCENT_DIM);
        ctx.fill(ix, iy, ix+52, iy+52, 0xFF060C18);
        ctx.fill(ix+1, iy+1, ix+51, iy+51, 0x2200D4FF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§b§lDC"), width/2, iy+19, ACCENT);

        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§f§lDiscord MC"), width/2, py+84, TEXT_HI);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§7Login with your Discord token"), width/2, py+98, TEXT_LO);

        // Token label
        ctx.drawTextWithShadow(textRenderer, Text.literal("§b▸ TOKEN"), px+20, py+116, ACCENT);

        // Input bg
        ctx.fill(px+16, py+128, px+pw-16, py+154, INPUT_BG);
        ctx.fill(px+16, py+128, px+pw-16, py+129, ACCENT);
        ctx.fill(px+16, py+153, px+pw-16, py+154, 0x4400D4FF);

        // Status
        if (!loginMsg.isEmpty())
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(loginMsg), width/2, py+162, loginMsgColor);

        // Login button with glow
        int bx = width/2-90, by = py+174;
        boolean hov = mx>=bx && mx<=bx+180 && my>=by && my<=by+28;
        if (hov) ctx.fill(bx-3, by-3, bx+183, by+31, ACCENT_DIM);
        ctx.fill(bx, by, bx+180, by+28, hov ? 0xFF0099BB : 0xFF007A99);
        ctx.fill(bx, by, bx+180, by+1, 0x8800FFFF);
        ctx.fill(bx, by+27, bx+180, by+28, 0x3300FFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§f§l⚡  Login"), width/2, by+10, WHITE);

        // Back
        boolean hb = mx>=width/2-50 && mx<=width/2+50 && my>=py+ph-20 && my<=py+ph-4;
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal(hb ? "§b← Back to Minecraft" : "§8← Back"), width/2, py+ph-16, hb ? ACCENT : TEXT_LO);
    }

    // ── Guild Bar ─────────────────────────────────────────────────────────────
    private void renderGuildBar(DrawContext ctx, int mx, int my) {
        // Glass sidebar
        ctx.fill(0, 0, GW, height, 0xFF070B14);
        ctx.fill(GW-1, 0, GW, height, BORDER);

        // Home button
        boolean hh = mx>=4 && mx<=GW-4 && my>=8 && my<=50;
        if (hh) ctx.fill(2, 8, GW-2, 50, ACCENT_DIM);
        ctx.fill(4, 8, GW-4, 50, hh||!inChat ? GLASS2 : 0xFF0A1020);
        ctx.fill(4, 8, GW-4, 9, hh||!inChat ? ACCENT : BORDER);
        ctx.fill(4, 49, GW-4, 50, hh||!inChat ? 0x3300D4FF : 0);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§b⌂"), GW/2, 20, hh ? ACCENT : 0xFF0077AA);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§8DMs"), GW/2, 33, TEXT_LO);

        // Divider
        ctx.fill(8, 56, GW-8, 57, BORDER);

        // Server icons
        int sy = 62;
        for (int i = 0; i < Math.min(servers.size(), 7); i++) {
            JsonObject s = servers.get(i);
            boolean sh = mx>=4 && mx<=GW-4 && my>=sy && my<=sy+38;
            if (sh) ctx.fill(2, sy-1, GW-2, sy+39, ACCENT_DIM);
            ctx.fill(4, sy, GW-4, sy+38, GLASS2);
            ctx.fill(4, sy, GW-4, sy+1, sh ? ACCENT : BORDER);
            ctx.fill(8, sy+4, GW-8, sy+34, INPUT_BG);
            ctx.fill(9, sy+5, GW-9, sy+33, 0x1500D4FF);
            String abbr = String.valueOf(s.get("name").getAsString().trim().charAt(0)).toUpperCase();
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§b" + abbr), GW/2, sy+13, sh ? ACCENT : 0xFF0077AA);
            sy += 42;
        }
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────
    private void renderSidebar(DrawContext ctx, int mx, int my) {
        ctx.fill(GW, 0, GW+SW, height, GLASS);
        ctx.fill(GW+SW-1, 0, GW+SW, height, BORDER);

        // Top header
        ctx.fill(GW, 0, GW+SW, TH, 0xFF070B14);
        ctx.fill(GW, TH-1, GW+SW, TH, ACCENT);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§b§l💬 §fDirect Messages"), GW+10, TH/2-4, TEXT_HI);

        // Section label
        ctx.drawTextWithShadow(textRenderer, Text.literal("§8▸ MESSAGES"), GW+10, TH+6, TEXT_LO);

        if (loading) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§bLoading..."), GW+SW/2, height/2, ACCENT);
            renderUserPanel(ctx);
            return;
        }

        int y = TH+20 - dmScroll;
        for (JsonObject dm : dms) {
            if (y+44 < TH+20 || y > height-56) { y+=46; continue; }
            String name = getDMName(dm);
            String uid  = getDMUserId(dm);
            boolean active = inChat && openChId!=null && openChId.equals(dm.get("id").getAsString());
            boolean hov = mx>=GW+2 && mx<=GW+SW-2 && my>=y && my<=y+42;

            // Glass card for each DM
            if (active) {
                // Active glow
                ctx.fill(GW+1, y, GW+SW-1, y+42, 0x2200D4FF);
                ctx.fill(GW+1, y, GW+SW-1, y+42, GLASS2);
                ctx.fill(GW+1, y, GW+3, y+42, ACCENT); // active bar
                ctx.fill(GW+1, y, GW+SW-1, y+1, BORDER);
                ctx.fill(GW+1, y+41, GW+SW-1, y+42, 0x2200D4FF);
            } else if (hov) {
                ctx.fill(GW+2, y, GW+SW-2, y+42, 0x1500D4FF);
                ctx.fill(GW+2, y, GW+SW-2, y+1, BORDER);
            }

            // Avatar with glow
            int ax = GW+10, ay = y+7;
            if (active || hov) ctx.fill(ax-2, ay-2, ax+30, ay+30, ACCENT_DIM);
            ctx.fill(ax, ay, ax+28, ay+28, INPUT_BG);
            ctx.fill(ax+1, ay+1, ax+27, ay+27, 0x2200D4FF);
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§b§l" + name.substring(0,1).toUpperCase()),
                ax+14, ay+9, active ? ACCENT : 0xFF0077AA);

            // Presence dot
            String pres = DiscordMC.discord.getPresence(uid);
            int pColor = presColor(pres);
            ctx.fill(ax+19, ay+19, ax+28, ay+28, GLASS); // border
            ctx.fill(ax+20, ay+20, ax+27, ay+27, pColor);
            if (!pres.equals("offline") && !pres.equals("")) {
                // Glow on dot
                ctx.fill(ax+18, ay+18, ax+29, ay+29, pColor & 0x33FFFFFF);
            }

            // Name + status
            int tx = ax+34;
            ctx.drawTextWithShadow(textRenderer,
                Text.literal((active ? "§f§l" : hov ? "§b" : "§7") + clip(name, 15)),
                tx, y+10, active ? TEXT_HI : TEXT_LO);
            ctx.drawTextWithShadow(textRenderer, Text.literal(presLabel(pres)), tx, y+24, pColor);

            y += 46;
        }

        if (dms.isEmpty() && !loading)
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§7No DMs"), GW+SW/2, height/2, TEXT_LO);

        renderUserPanel(ctx);
    }

    private void renderUserPanel(DrawContext ctx) {
        ctx.fill(GW, height-54, GW+SW, height, 0xFF070B14);
        ctx.fill(GW, height-54, GW+SW, height-53, ACCENT);

        if (DiscordMC.discord.getSelfUser()==null) return;
        String name = DiscordMC.discord.getSelfUser().get("username").getAsString();

        int ax = GW+8, ay = height-46;
        ctx.fill(ax-1, ay-1, ax+35, ay+35, ACCENT_DIM);
        ctx.fill(ax, ay, ax+34, ay+34, INPUT_BG);
        ctx.fill(ax+1, ay+1, ax+33, ay+33, 0x2200D4FF);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§b§l" + name.substring(0,1).toUpperCase()), ax+17, ay+12, ACCENT);
        ctx.fill(ax+24, ay+24, ax+34, ay+34, 0xFF070B14);
        ctx.fill(ax+25, ay+25, ax+33, ay+33, GREEN);
        ctx.fill(ax+22, ay+22, ax+36, ay+36, 0x2200FF9D);

        ctx.drawTextWithShadow(textRenderer, Text.literal("§f§l" + clip(name,13)), GW+50, height-44, TEXT_HI);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§a⬤ Online"), GW+50, height-30, GREEN);
    }

    // ── Main Content ──────────────────────────────────────────────────────────
    private void renderMain(DrawContext ctx, int mx, int my) {
        int cx = GW+SW, cw = width-cx;
        ctx.fill(cx, 0, width, height, 0xFF080C18);

        // Topbar glass
        ctx.fill(cx, 0, width, TH, GLASS);
        ctx.fill(cx, TH-1, width, TH, ACCENT);
        ctx.fill(cx, 0, width, 1, BORDER);

        if (!inChat) {
            // Empty state card
            int ew = 180, eh = 80, ex = cx+cw/2-ew/2, ey = height/2-eh/2;
            ctx.fill(ex-4, ey-4, ex+ew+4, ey+eh+4, ACCENT_DIM);
            ctx.fill(ex, ey, ex+ew, ey+eh, GLASS);
            ctx.fill(ex, ey, ex+ew, ey+2, ACCENT);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§b§l#"), cx+cw/2, ey+18, ACCENT);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§f§lSelect a DM"), cx+cw/2, ey+38, TEXT_HI);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§8from the left sidebar"), cx+cw/2, ey+52, TEXT_LO);
            ctx.drawTextWithShadow(textRenderer, Text.literal("§b§l💬 §fDMs"), cx+14, TH/2-4, TEXT_HI);
            return;
        }

        // Channel header
        ctx.drawTextWithShadow(textRenderer, Text.literal("§b# §f§l" + openChName), cx+14, TH/2-4, TEXT_HI);
        // Divider line under header
        ctx.fill(cx+8, TH-8, cx+cw-8, TH-7, BORDER);

        // Messages
        ctx.fill(cx, TH, width, height-IH, 0xFF080C18);

        if (messages.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§8No messages yet..."), cx+cw/2, height/2, TEXT_LO);
        } else {
            int y = TH+8 - msgScroll;
            for (JsonObject msg : messages) {
                if (y+40 < TH || y > height-IH) { y+=40; continue; }
                String author = msg.has("author") ? msg.getAsJsonObject("author").get("username").getAsString() : "?";
                String content = msg.has("content") ? msg.get("content").getAsString() : "";
                String aId = msg.has("author") ? msg.getAsJsonObject("author").get("id").getAsString() : "";
                boolean self = DiscordMC.discord.getSelfUser()!=null &&
                    aId.equals(DiscordMC.discord.getSelfUser().get("id").getAsString());

                // Avatar
                int ax = cx+8, ay = y;
                ctx.fill(ax, ay, ax+24, ay+24, INPUT_BG);
                ctx.fill(ax+1, ay+1, ax+23, ay+23, 0x1500D4FF);
                ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("§b" + author.substring(0,1).toUpperCase()), ax+12, ay+8, ACCENT);

                // Author + message
                ctx.drawTextWithShadow(textRenderer,
                    Text.literal((self ? "§b§l" : "§e§l") + author), cx+38, y, self ? ACCENT : 0xFFFFCC00);

                int maxC = Math.max(20, (cw-50)/6);
                ctx.drawTextWithShadow(textRenderer,
                    Text.literal("§f" + (content.length()>maxC ? content.substring(0,maxC) : content)),
                    cx+38, y+12, TEXT_HI);
                if (content.length()>maxC)
                    ctx.drawTextWithShadow(textRenderer,
                        Text.literal("§7" + content.substring(maxC, Math.min(maxC*2, content.length()))),
                        cx+38, y+22, TEXT_LO);
                y += 40;
            }
        }

        // Input bar glass
        ctx.fill(cx, height-IH, width, height, GLASS);
        ctx.fill(cx, height-IH, width, height-IH+1, ACCENT);
        ctx.fill(cx+8, height-IH+9, width-8, height-8, INPUT_BG);
        ctx.fill(cx+8, height-IH+9, width-8, height-IH+10, ACCENT);
        ctx.fill(cx+8, height-9, width-8, height-8, 0x2200D4FF);
        // Send hint
        ctx.drawTextWithShadow(textRenderer, Text.literal("§b↵"), width-20, height-IH+14, ACCENT);
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
    private int presColor(String p) {
        return switch(p) { case "online" -> GREEN; case "idle" -> YELLOW; case "dnd" -> RED_C; default -> OFFLINE; };
    }
    private String presLabel(String p) {
        return switch(p) { case "online" -> "§a⬤ Online"; case "idle" -> "§e⬤ Idle"; case "dnd" -> "§c⬤ DnD"; default -> "§8○ Offline"; };
    }
    private String clip(String s, int max) { return s.length()>max ? s.substring(0,max-2)+".." : s; }
    private void autoScroll() { msgScroll = Math.max(0, messages.size()*40-(height-TH-IH-10)); }

    // ── Events ────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (inLogin) {
            int py = height/2-130;
            if (mx>=width/2-90 && mx<=width/2+90 && my>=py+174 && my<=py+202) { doLogin(); return true; }
            if (my>=py+240) { client.setScreen(parent); return true; }
            return super.mouseClicked(mx, my, btn);
        }
        // Home
        if (mx>=4 && mx<=GW-4 && my>=8 && my<=50) { inChat=false; openChId=null; clearAndInit(); return true; }
        // DM
        if (mx>=GW+2 && mx<=GW+SW-2) {
            int y = TH+20-dmScroll;
            for (JsonObject dm : dms) {
                if (my>=y && my<=y+42) { openChat(dm.get("id").getAsString(), getDMName(dm)); return true; }
                y+=46;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if (key==257||key==335) {
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
        if (mx<GW+SW) dmScroll=Math.max(0,dmScroll-(int)(v*10));
        else if (inChat) msgScroll=Math.max(0,msgScroll-(int)(v*10));
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
    @Override public boolean shouldCloseOnEsc() { return false; }
}
