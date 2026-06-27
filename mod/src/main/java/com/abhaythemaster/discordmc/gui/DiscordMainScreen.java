package com.abhaythemaster.discordmc.gui;

import com.abhaythemaster.discordmc.DiscordMC;
import com.abhaythemaster.discordmc.util.AvatarCache;
import com.abhaythemaster.discordmc.util.Anim;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class DiscordMainScreen extends Screen {

    private final Screen parent;

    // ── Glassmorphism palette ──────────────────────────────────────────────
    // Base layers (multiple semi-transparent = glass effect)
    private static final int G_BASE   = 0x180D1828; // ultra-thin base
    private static final int G_L1     = 0x2A0F1E35; // glass layer 1
    private static final int G_L2     = 0x3A122240; // glass layer 2
    private static final int G_L3     = 0x4A14264A; // glass layer 3 (thicker)
    private static final int G_PANEL  = 0x5515263F; // panel glass
    private static final int G_CARD   = 0x661A2E4A; // card glass
    private static final int G_DARK   = 0xAA080E1A; // darker glass
    private static final int G_GLOW   = 0x2200D4FF; // cyan glow layer

    // Accent & text
    private static final int CYAN     = 0xFF00D4FF;
    private static final int CYAN2    = 0xFF0099BB;
    private static final int CYAN_DIM = 0x4400D4FF;
    private static final int CYAN_MID = 0x8800D4FF;
    private static final int GREEN    = 0xFF00FF9D;
    private static final int YELLOW   = 0xFFFFCC00;
    private static final int RED_C    = 0xFFFF5252;
    private static final int OFFLINE  = 0xFF37474F;
    private static final int TEXT_W   = 0xFFECEFF1;
    private static final int TEXT_G   = 0xFF78909C;
    private static final int WHITE    = 0xFFFFFFFF;
    private static final int BLACK    = 0xFF000000;

    // Layout
    private static final int GW = 58, SW = 215, TH = 46, IH = 42;

    // State
    private final List<JsonObject> dms     = new ArrayList<>();
    private final List<JsonObject> servers = new ArrayList<>();
    private boolean loading = false;
    private int dmScroll = 0, msgScroll = 0;
    private String openChId = null, openChName = null;
    private final List<JsonObject> messages = new ArrayList<>();
    private boolean inChat = false, inLogin = false;
    private TextFieldWidget tokenField, chatField;
    private String loginMsg = "";
    private int loginMsgColor = TEXT_G;
    private int hovDM = -1;

    // Branding
    private static final String BRAND = "§8Made by §bAbhayTheMaster";

    public DiscordMainScreen(Screen parent) {
        super(Text.literal("Discord"));
        this.parent = parent;
    }

    // No background blur from Minecraft
    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {}

    @Override
    protected void init() {
        Anim.reset("login_in");
        Anim.reset("sidebar_in");

        if (!DiscordMC.discord.isLoggedIn()) {
            inLogin = true;
            int fw = 268, fx = width/2 - fw/2, fy = height/2 + 8;
            tokenField = new TextFieldWidget(textRenderer, fx, fy, fw, 22, Text.literal(""));
            tokenField.setPlaceholder(Text.literal("§8Paste your Discord token..."));
            tokenField.setMaxLength(200);
            addDrawableChild(tokenField);
            setInitialFocus(tokenField);
            return;
        }
        inLogin = false;
        if (dms.isEmpty() && !loading) {
            loading = true;
            DiscordMC.discord.fetchDMs().thenAccept(l -> { synchronized(dms){ dms.clear(); dms.addAll(l); } loading = false; });
            DiscordMC.discord.fetchGuilds().thenAccept(l -> { synchronized(servers){ servers.clear(); servers.addAll(l); } });
        }
        if (inChat && openChId != null) {
            int fx = GW+SW+10, fw = width-GW-SW-56;
            chatField = new TextFieldWidget(textRenderer, fx, height-IH+11, fw, 20, Text.literal(""));
            chatField.setPlaceholder(Text.literal("§8Message #" + openChName + "..."));
            chatField.setMaxLength(2000);
            addDrawableChild(chatField);
            setInitialFocus(chatField);
            DiscordMC.discord.onMessage(m -> {
                if (m.get("channel_id").getAsString().equals(openChId)) {
                    synchronized(messages) { messages.add(m); }
                    autoScroll();
                }
            });
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        int W = width, H = height;

        // ── Full background: deep space dark ─────────────────────────────
        ctx.fill(0, 0, W, H, 0xFF060A14);

        // Background glass layers (simulate frosted glass depth)
        ctx.fill(0, 0, W, H, G_BASE);
        ctx.fill(0, 0, W/3, H, G_L1);       // left tint
        ctx.fill(W*2/3, 0, W, H, G_L1);     // right tint

        // Subtle grid dots
        for (int x = 0; x < W; x += 40) for (int y = 0; y < H; y += 40)
            ctx.fill(x, y, x+1, y+1, 0x0CFFFFFF);

        // Animated top glow line
        float pulse = Anim.pulse(0.5f);
        int glowAlpha = (int)(80 + pulse * 60);
        ctx.fill(0, 0, W, 1, (glowAlpha << 24) | 0x00D4FF);
        ctx.fill(0, 1, W, 2, ((glowAlpha/3) << 24) | 0x00D4FF);

        // Bottom brand line
        ctx.fill(0, H-16, W, H, G_L1);
        ctx.fill(0, H-16, W, H-15, 0x3300D4FF);
        ctx.drawTextWithShadow(textRenderer, Text.literal(BRAND), 8, H-11, TEXT_G);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§8Discord MC §7v1.0"), W-80, H-11, TEXT_G);

        if (inLogin) { renderLogin(ctx, mx, my, pulse); super.render(ctx, mx, my, delta); return; }

        // Sidebar slide-in animation
        float sAnim = Anim.lerp("sidebar_in", 1f, 8f);
        int sOff = (int)((1f - sAnim) * -(GW+SW));

        renderGuildBar(ctx, mx, my, sOff, pulse);
        renderSidebar(ctx, mx, my, sOff, pulse);
        renderMain(ctx, mx, my, pulse);

        super.render(ctx, mx, my, delta);
    }

    // ── Glassmorphic Login ────────────────────────────────────────────────
    private void renderLogin(DrawContext ctx, int mx, int my, float pulse) {
        float anim = Anim.lerp("login_in", 1f, 6f);
        int pw = 350, ph = 280;
        int px = width/2 - pw/2, py = (int)(height/2 - ph/2 - (1f-anim)*20);
        int alpha = (int)(anim * 255);
        if (alpha <= 0) return;

        // Outer ambient glow (multiple layers = bloom effect)
        fillAlpha(ctx, px-20, py-20, px+pw+20, py+ph+20, 0x0500D4FF, alpha);
        fillAlpha(ctx, px-12, py-12, px+pw+12, py+ph+12, 0x0A00D4FF, alpha);
        fillAlpha(ctx, px-6, py-6, px+pw+6, py+ph+6, 0x1500D4FF, alpha);

        // Glass card — 4 layers for depth
        fillAlpha(ctx, px, py, px+pw, py+ph, G_L1, alpha);
        fillAlpha(ctx, px, py, px+pw, py+ph, G_L2, alpha);
        fillAlpha(ctx, px, py, px+pw, py+ph, G_PANEL, alpha);

        // Top specular highlight (glass shine)
        fillAlpha(ctx, px+2, py+2, px+pw-2, py+6, 0x18FFFFFF, alpha);

        // Animated cyan top border
        int borderAlpha = (int)((0.6f + pulse*0.4f) * alpha);
        fillAlpha(ctx, px, py, px+pw, py+2, CYAN, borderAlpha);
        fillAlpha(ctx, px, py+2, px+pw, py+3, CYAN_DIM, borderAlpha);

        // Side borders
        fillAlpha(ctx, px, py, px+1, py+ph, CYAN_MID, alpha/2);
        fillAlpha(ctx, px+pw-1, py, px+pw, py+ph, CYAN_MID, alpha/2);
        fillAlpha(ctx, px, py+ph-1, px+pw, py+ph, CYAN_MID, alpha/3);

        // DC Icon — glassmorphic circle
        int ix = width/2-28, iy = py+18;
        fillAlpha(ctx, ix-3, iy-3, ix+59, iy+59, CYAN_DIM, alpha);
        fillAlpha(ctx, ix, iy, ix+56, iy+56, G_DARK, alpha);
        fillAlpha(ctx, ix+1, iy+1, ix+55, iy+55, G_GLOW, alpha);
        // Shine on icon
        fillAlpha(ctx, ix+2, iy+2, ix+54, iy+8, 0x15FFFFFF, alpha);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§b§lDC"), width/2, iy+19, blendAlpha(CYAN, alpha));

        // Texts
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§f§lWelcome to Discord MC"), width/2, py+90, blendAlpha(TEXT_W, alpha));
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§7Enter your token to login"), width/2, py+104, blendAlpha(TEXT_G, alpha));

        // TOKEN label with cyan dot
        ctx.drawTextWithShadow(textRenderer, Text.literal("§b● §8TOKEN"), px+18, py+122, blendAlpha(CYAN, alpha));

        // Input glass field
        fillAlpha(ctx, px+14, py+134, px+pw-14, py+160, G_DARK, alpha);
        fillAlpha(ctx, px+14, py+134, px+pw-14, py+135, CYAN, alpha);
        fillAlpha(ctx, px+14, py+159, px+pw-14, py+160, CYAN_DIM, alpha);
        // Specular
        fillAlpha(ctx, px+15, py+135, px+pw-15, py+139, 0x08FFFFFF, alpha);

        // Status
        if (!loginMsg.isEmpty())
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(loginMsg), width/2, py+168, blendAlpha(loginMsgColor, alpha));

        // Login button — animated glow
        int bx = width/2-95, by = py+180;
        boolean hov = mx>=bx && mx<=bx+190 && my>=by && my<=by+30;
        float btnAnim = Anim.lerp("btn_hov", hov?1f:0f, 10f);

        // Button glow layers
        int glowA = (int)(btnAnim * 80);
        if (glowA > 0) {
            fillAlpha(ctx, bx-4, by-4, bx+194, by+34, CYAN, glowA/3);
            fillAlpha(ctx, bx-2, by-2, bx+192, by+32, CYAN, glowA/2);
        }
        // Button body glass
        int btnColor = blendInt(0xFF006680, 0xFF0099BB, btnAnim);
        fillAlpha(ctx, bx, by, bx+190, by+30, btnColor, alpha);
        // Shine
        fillAlpha(ctx, bx, by, bx+190, by+4, 0x25FFFFFF, alpha);
        fillAlpha(ctx, bx, by, bx+190, by+1, CYAN, alpha);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§f§l⚡  Login"), width/2, by+11, blendAlpha(WHITE, alpha));

        // Back link
        boolean hb = mx>=width/2-55 && mx<=width/2+55 && my>=py+ph-22 && my<=py+ph-4;
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal(hb ? "§b← Back to Minecraft" : "§8← Back"),
            width/2, py+ph-18, blendAlpha(hb?CYAN:TEXT_G, alpha));
    }

    // ── Guild Bar ─────────────────────────────────────────────────────────
    private void renderGuildBar(DrawContext ctx, int mx, int my, int sOff, float pulse) {
        int ox = sOff; // slide offset
        // Glass bg — multiple layers
        ctx.fill(ox, 0, ox+GW, height, 0xFF060A14);
        ctx.fill(ox, 0, ox+GW, height, G_L1);
        ctx.fill(ox, 0, ox+GW, height, G_L2);
        // Right border glow
        ctx.fill(ox+GW-1, 0, ox+GW, height, 0x4400D4FF);
        ctx.fill(ox+GW-2, 0, ox+GW-1, height, 0x1500D4FF);

        // Home button
        boolean hh = mx>=ox+4 && mx<=ox+GW-4 && my>=8 && my<=52;
        float hAnim = Anim.lerp("home_hov", hh?1f:0f, 10f);
        if (hAnim > 0) ctx.fill(ox+2, 6, ox+GW-2, 54, blendAlpha(CYAN_DIM, (int)(hAnim*255)));
        ctx.fill(ox+4, 8, ox+GW-4, 52, G_PANEL);
        ctx.fill(ox+4, 8, ox+GW-4, G_CARD, G_CARD); // extra glass
        ctx.fill(ox+4, 8, ox+GW-4, 9, blendAlpha(CYAN, (int)((0.5f+hAnim*0.5f)*255)));
        ctx.fill(ox+4, 9, ox+GW-4, 12, 0x10FFFFFF); // specular
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§b⌂"), ox+GW/2, 21, hh?CYAN:CYAN2);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§8DMs"), ox+GW/2, 34, TEXT_G);

        // Pulsing divider
        int divAlpha = (int)(60 + pulse*40);
        ctx.fill(ox+8, 58, ox+GW-8, 59, (divAlpha<<24)|0x00D4FF);

        // Server icons
        int sy = 64;
        synchronized(servers) {
            for (int i = 0; i < Math.min(servers.size(), 7); i++) {
                JsonObject s = servers.get(i);
                boolean sh = mx>=ox+4 && mx<=ox+GW-4 && my>=sy && my<=sy+40;
                float sAnim = Anim.lerp("srv_"+i, sh?1f:0f, 10f);

                if (sAnim>0) ctx.fill(ox+2, sy-2, ox+GW-2, sy+42, blendAlpha(CYAN_DIM,(int)(sAnim*200)));
                ctx.fill(ox+4, sy, ox+GW-4, sy+40, G_PANEL);
                ctx.fill(ox+4, sy, ox+GW-4, sy+1, blendAlpha(CYAN,(int)((0.3f+sAnim*0.7f)*255)));
                ctx.fill(ox+4, sy+1, ox+GW-4, sy+5, 0x10FFFFFF);

                // Server icon area
                ctx.fill(ox+9, sy+5, ox+GW-9, sy+35, G_DARK);
                ctx.fill(ox+10, sy+6, ox+GW-10, sy+34, G_GLOW);

                // Try server icon or letter
                String name = s.get("name").getAsString().trim();
                String iconHash = s.has("icon") && !s.get("icon").isJsonNull() ? s.get("icon").getAsString() : "";
                String gId = s.get("id").getAsString();
                Identifier icon = AvatarCache.getGuild(gId, iconHash);
                if (icon != null) {
                    ctx.drawTexture(icon, ox+10, sy+7, 0, 0, GW-20, 26, GW-20, 26);
                } else {
                    String abbr = name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase();
                    ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§b"+abbr), ox+GW/2, sy+14, sh?CYAN:CYAN2);
                }
                sy += 44;
            }
        }
    }

    // ── Sidebar ───────────────────────────────────────────────────────────
    private void renderSidebar(DrawContext ctx, int mx, int my, int sOff, float pulse) {
        int sx = sOff + GW;
        // Glass layers
        ctx.fill(sx, 0, sx+SW, height, 0xFF07091200);
        ctx.fill(sx, 0, sx+SW, height, G_L1);
        ctx.fill(sx, 0, sx+SW, height, G_L2);
        ctx.fill(sx, 0, sx+SW, height, G_L3);
        // Right border
        ctx.fill(sx+SW-1, 0, sx+SW, height, 0x5500D4FF);
        ctx.fill(sx+SW-2, 0, sx+SW-1, height, 0x1800D4FF);

        // Header glass bar
        ctx.fill(sx, 0, sx+SW, TH, G_DARK);
        ctx.fill(sx, TH-1, sx+SW, TH, 0x8800D4FF);
        ctx.fill(sx, TH-2, sx+SW, TH-1, 0x2200D4FF);
        ctx.fill(sx, 0, sx+SW, 3, 0x1FFFFFFF); // top specular
        ctx.drawTextWithShadow(textRenderer, Text.literal("§b§l💬  §fDirect Messages"), sx+10, TH/2-4, TEXT_W);

        // Section label with pulse
        int lblA = (int)(150 + pulse*50);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§8▸  MESSAGES"), sx+10, TH+7, blendAlpha(TEXT_G, lblA));

        if (loading) {
            // Loading pulse animation
            int loadAlpha = (int)(150 + pulse*100);
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§b⟳ §7Loading..."), sx+SW/2, height/2, blendAlpha(CYAN, loadAlpha));
            renderUserPanel(ctx, sx, pulse);
            return;
        }

        // DM list
        int y = TH+22 - dmScroll;
        int idx = 0;
        synchronized(dms) {
            for (JsonObject dm : dms) {
                if (y+46 < TH+22 || y > height-58) { y+=48; idx++; continue; }
                String name = getDMName(dm);
                String uid  = getDMUserId(dm);
                String avHash = getDMAvatarHash(dm);
                boolean active = inChat && openChId!=null && openChId.equals(dm.get("id").getAsString());
                boolean hov = mx>=sx+2 && mx<=sx+SW-2 && my>=y && my<=y+44;

                float rowAnim = Anim.lerp("dm_"+idx, (hov||active)?1f:0f, 10f);

                // Row glass card
                if (rowAnim > 0 || active) {
                    int cardA = (int)((active?0.8f:rowAnim*0.5f)*255);
                    ctx.fill(sx+1, y, sx+SW-1, y+44, blendAlpha(G_CARD, cardA));
                    if (active) ctx.fill(sx+1, y, sx+SW-1, y+44, blendAlpha(CYAN_DIM, (int)(rowAnim*100)));
                }

                // Active left bar with glow
                if (active) {
                    ctx.fill(sx+1, y+4, sx+4, y+40, CYAN);
                    ctx.fill(sx+4, y+4, sx+8, y+40, CYAN_DIM);
                }

                // Card borders
                if (hov || active) {
                    ctx.fill(sx+1, y, sx+SW-1, y+1, blendAlpha(CYAN, (int)(rowAnim*150)));
                    ctx.fill(sx+1, y+43, sx+SW-1, y+44, blendAlpha(CYAN_DIM, (int)(rowAnim*80)));
                }

                // Avatar — glassmorphic circle
                int ax = sx+10, ay = y+8;
                // Glow ring
                if (active || hov) ctx.fill(ax-2, ay-2, ax+30, ay+30, blendAlpha(CYAN_DIM,(int)(rowAnim*200)));
                // Glass bg
                ctx.fill(ax, ay, ax+28, ay+28, G_DARK);
                ctx.fill(ax+1, ay+1, ax+27, ay+27, G_GLOW);
                ctx.fill(ax+1, ay+1, ax+27, ay+5, 0x12FFFFFF); // specular

                // Load avatar texture
                Identifier avTex = AvatarCache.get(uid, avHash);
                if (avTex != null) {
                    // Draw circular avatar (approximate with square)
                    ctx.drawTexture(avTex, ax+1, ay+1, 0, 0, 26, 26, 26, 26);
                } else {
                    // Fallback: initial letter
                    ctx.drawCenteredTextWithShadow(textRenderer,
                        Text.literal("§b§l" + name.substring(0,1).toUpperCase()),
                        ax+14, ay+9, active?CYAN:CYAN2);
                }

                // Presence dot with glow
                String pres = DiscordMC.discord.getPresence(uid);
                int pColor = presColor(pres);
                int dotPulse = pres.equals("online") ? (int)(200+pulse*55) : 255;
                ctx.fill(ax+19, ay+19, ax+28, ay+28, G_DARK);
                ctx.fill(ax+20, ay+20, ax+27, ay+27, blendAlpha(pColor, dotPulse));
                if (!pres.equals("offline") && !pres.isEmpty())
                    ctx.fill(ax+18, ay+18, ax+29, ay+29, blendAlpha(pColor, (int)(pulse*60)));

                // Name + status
                int tx = ax+34;
                ctx.drawTextWithShadow(textRenderer,
                    Text.literal((active?"§f§l":hov?"§b§l":"§7") + clip(name,15)),
                    tx, y+10, active?TEXT_W:TEXT_G);
                ctx.drawTextWithShadow(textRenderer, Text.literal(presLabel(pres)), tx, y+24, pColor);

                y+=48; idx++;
            }
        }
        if (dms.isEmpty() && !loading) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§7No DMs found"), sx+SW/2, height/2, TEXT_G);
        }
        renderUserPanel(ctx, sx, pulse);
    }

    private void renderUserPanel(DrawContext ctx, int sx, float pulse) {
        ctx.fill(sx, height-56, sx+SW, height, G_DARK);
        ctx.fill(sx, height-56, sx+SW, height-55, 0x6600D4FF);
        ctx.fill(sx, height-55, sx+SW, height-54, CYAN_DIM);
        ctx.fill(sx, height-56, sx+SW, height, G_L2);

        if (DiscordMC.discord.getSelfUser()==null) return;
        JsonObject me = DiscordMC.discord.getSelfUser();
        String name = me.get("username").getAsString();
        String myId = me.get("id").getAsString();
        String myAvHash = me.has("avatar") && !me.get("avatar").isJsonNull() ? me.get("avatar").getAsString() : "";

        int ax = sx+8, ay = height-48;
        ctx.fill(ax-2, ay-2, ax+36, ay+36, blendAlpha(CYAN_DIM, (int)(150+pulse*60)));
        ctx.fill(ax, ay, ax+34, ay+34, G_DARK);
        ctx.fill(ax+1, ay+1, ax+33, ay+33, G_GLOW);
        ctx.fill(ax+1, ay+1, ax+33, ay+5, 0x12FFFFFF);

        Identifier myAv = AvatarCache.get(myId, myAvHash);
        if (myAv != null) ctx.drawTexture(myAv, ax+1, ay+1, 0, 0, 32, 32, 32, 32);
        else ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§b§l"+name.substring(0,1).toUpperCase()), ax+17, ay+12, CYAN);

        // Online dot glow
        int dotA = (int)(200+pulse*55);
        ctx.fill(ax+24, ay+24, ax+34, ay+34, G_DARK);
        ctx.fill(ax+25, ay+25, ax+33, ay+33, blendAlpha(GREEN, dotA));
        ctx.fill(ax+23, ay+23, ax+35, ay+35, blendAlpha(GREEN, (int)(pulse*50)));

        ctx.drawTextWithShadow(textRenderer, Text.literal("§f§l"+clip(name,13)), sx+50, height-46, TEXT_W);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§a⬤ §aOnline"), sx+50, height-32, GREEN);

        // Settings icon hint
        ctx.drawTextWithShadow(textRenderer, Text.literal("§8⚙"), sx+SW-16, height-40, TEXT_G);
    }

    // ── Main Content ──────────────────────────────────────────────────────
    private void renderMain(DrawContext ctx, int mx, int my, float pulse) {
        int cx = GW+SW, cw = width-cx;

        // Background glass
        ctx.fill(cx, 0, width, height, 0xFF070912);
        ctx.fill(cx, 0, width, height, G_L1);

        // Top bar glass
        ctx.fill(cx, 0, width, TH, G_DARK);
        ctx.fill(cx, 0, width, TH, G_L2);
        ctx.fill(cx, TH-1, width, TH, 0x8800D4FF);
        ctx.fill(cx, TH-2, width, TH-1, 0x2200D4FF);
        ctx.fill(cx, 0, width, 3, 0x10FFFFFF);

        if (!inChat) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("§b§l💬  §fDMs"), cx+14, TH/2-4, TEXT_W);

            // Empty state glassmorphic card
            int ew=200, eh=100, ex=cx+cw/2-ew/2, ey=height/2-eh/2;
            ctx.fill(ex-6, ey-6, ex+ew+6, ey+eh+6, blendAlpha(CYAN_DIM,(int)(80+pulse*40)));
            ctx.fill(ex, ey, ex+ew, ey+eh, G_PANEL);
            ctx.fill(ex, ey, ex+ew, ey+eh, G_L2);
            ctx.fill(ex, ey, ex+ew, ey+2, blendAlpha(CYAN,(int)(200+pulse*55)));
            ctx.fill(ex, ey+1, ex+ew, ey+5, 0x10FFFFFF);
            ctx.fill(ex, ey, ex+1, ey+eh, CYAN_DIM);
            ctx.fill(ex+ew-1, ey, ex+ew, ey+eh, CYAN_DIM);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§b#"), cx+cw/2, ey+22, blendAlpha(CYAN,(int)(200+pulse*55)));
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§f§lSelect a DM"), cx+cw/2, ey+44, TEXT_W);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§8Choose from the left sidebar"), cx+cw/2, ey+58, TEXT_G);
            return;
        }

        // Chat header
        ctx.drawTextWithShadow(textRenderer, Text.literal("§b# §f§l"+openChName), cx+14, TH/2-4, TEXT_W);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§8[ESC=back]"), width-68, TH/2-4, TEXT_G);

        // Messages area
        ctx.fill(cx, TH, width, height-IH, 0xFF070912);
        ctx.fill(cx, TH, width, height-IH, G_L1);

        // Subtle left border on chat
        ctx.fill(cx, TH, cx+1, height-IH, CYAN_DIM);

        synchronized(messages) {
            if (messages.isEmpty()) {
                ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§8No messages yet..."), cx+cw/2, height/2, TEXT_G);
            } else {
                int y = TH+10 - msgScroll;
                for (JsonObject msg : messages) {
                    if (y+42<TH || y>height-IH) { y+=42; continue; }
                    String author = msg.has("author") ? msg.getAsJsonObject("author").get("username").getAsString() : "?";
                    String content = msg.has("content") ? msg.get("content").getAsString() : "";
                    String aId = msg.has("author") ? msg.getAsJsonObject("author").get("id").getAsString() : "";
                    String aHash = msg.has("author") && msg.getAsJsonObject("author").has("avatar")
                        && !msg.getAsJsonObject("author").get("avatar").isJsonNull()
                        ? msg.getAsJsonObject("author").get("avatar").getAsString() : "";
                    boolean self = DiscordMC.discord.getSelfUser()!=null &&
                        aId.equals(DiscordMC.discord.getSelfUser().get("id").getAsString());

                    // Message avatar
                    int ax=cx+8, ay=y;
                    ctx.fill(ax, ay, ax+26, ay+26, G_DARK);
                    ctx.fill(ax+1, ay+1, ax+25, ay+25, G_GLOW);
                    ctx.fill(ax+1, ay+1, ax+25, ay+5, 0x0FFFFFFF);

                    Identifier avTex = AvatarCache.get(aId, aHash);
                    if (avTex != null) ctx.drawTexture(avTex, ax+1, ay+1, 0, 0, 24, 24, 24, 24);
                    else ctx.drawCenteredTextWithShadow(textRenderer,
                        Text.literal("§b"+author.substring(0,1).toUpperCase()), ax+13, ay+8, self?CYAN:0xFFFFAA00);

                    // Author name + message
                    ctx.drawTextWithShadow(textRenderer,
                        Text.literal((self?"§b§l":"§e§l")+author), cx+40, y, self?CYAN:0xFFFFCC00);
                    int maxC = Math.max(20,(cw-52)/6);
                    ctx.drawTextWithShadow(textRenderer,
                        Text.literal("§f"+(content.length()>maxC?content.substring(0,maxC):content)),
                        cx+40, y+12, TEXT_W);
                    if (content.length()>maxC)
                        ctx.drawTextWithShadow(textRenderer,
                            Text.literal("§7"+content.substring(maxC,Math.min(maxC*2,content.length()))),
                            cx+40, y+22, TEXT_G);
                    y+=42;
                }
            }
        }

        // Input bar glassmorphic
        ctx.fill(cx, height-IH, width, height, G_DARK);
        ctx.fill(cx, height-IH, width, height, G_L2);
        ctx.fill(cx, height-IH, width, height-IH+1, 0x8800D4FF);
        ctx.fill(cx, height-IH+1, width, height-IH+2, CYAN_DIM);

        // Input field glass
        ctx.fill(cx+8, height-IH+10, width-42, height-10, G_DARK);
        ctx.fill(cx+9, height-IH+11, width-43, height-11, G_GLOW);
        ctx.fill(cx+9, height-IH+11, width-43, height-IH+14, 0x0CFFFFFF);
        ctx.fill(cx+8, height-IH+10, cx+9, height-10, CYAN_DIM);

        // Send button glass
        int sbx=width-38, sby=height-IH+9;
        ctx.fill(sbx, sby, sbx+30, sby+23, G_PANEL);
        ctx.fill(sbx, sby, sbx+30, sby+1, CYAN_DIM);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§b§l↵"), sbx+15, sby+8, CYAN);
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private String getDMName(JsonObject dm) {
        try { return dm.getAsJsonArray("recipients").get(0).getAsJsonObject().get("username").getAsString(); }
        catch (Exception e) { return "Unknown"; }
    }
    private String getDMUserId(JsonObject dm) {
        try { return dm.getAsJsonArray("recipients").get(0).getAsJsonObject().get("id").getAsString(); }
        catch (Exception e) { return ""; }
    }
    private String getDMAvatarHash(JsonObject dm) {
        try {
            JsonObject recipient = dm.getAsJsonArray("recipients").get(0).getAsJsonObject();
            return recipient.has("avatar") && !recipient.get("avatar").isJsonNull()
                ? recipient.get("avatar").getAsString() : "";
        } catch (Exception e) { return ""; }
    }
    private int presColor(String p) { return switch(p){ case"online"->GREEN; case"idle"->YELLOW; case"dnd"->RED_C; default->OFFLINE; }; }
    private String presLabel(String p) { return switch(p){ case"online"->"§a⬤ Online"; case"idle"->"§e⬤ Idle"; case"dnd"->"§c⬤ DnD"; default->"§8○ Offline"; }; }
    private String clip(String s, int max) { return s.length()>max?s.substring(0,max-2)+"..":s; }
    private void autoScroll() { synchronized(messages){ msgScroll=Math.max(0,messages.size()*42-(height-TH-IH-10)); } }

    // Alpha blend helpers
    private int blendAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }
    private int blendInt(int c1, int c2, float t) {
        int r1=(c1>>16)&0xFF, g1=(c1>>8)&0xFF, b1=c1&0xFF;
        int r2=(c2>>16)&0xFF, g2=(c2>>8)&0xFF, b2=c2&0xFF;
        return 0xFF000000 | ((int)(r1+(r2-r1)*t))<<16 | ((int)(g1+(g2-g1)*t))<<8 | (int)(b1+(b2-b1)*t);
    }
    private void fillAlpha(DrawContext ctx, int x1, int y1, int x2, int y2, int color, int alpha) {
        if (alpha <= 0) return;
        ctx.fill(x1, y1, x2, y2, blendAlpha(color, Math.min(255, ((color>>24)&0xFF) * alpha / 255)));
    }

    // ── Events ────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (inLogin) {
            int py = height/2-140;
            if (mx>=width/2-95 && mx<=width/2+95 && my>=py+180 && my<=py+210) { doLogin(); return true; }
            if (my>=py+258) { client.setScreen(parent); return true; }
            return super.mouseClicked(mx, my, btn);
        }
        if (mx>=4 && mx<=GW-4 && my>=8 && my<=52) { inChat=false; openChId=null; clearAndInit(); return true; }
        if (mx>=GW+2 && mx<=GW+SW-2) {
            int y=TH+22-dmScroll;
            synchronized(dms) {
                for (JsonObject dm : dms) {
                    if (my>=y && my<=y+44) { openChat(dm.get("id").getAsString(), getDMName(dm)); return true; }
                    y+=48;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if (key==257||key==335) {
            if (inLogin) { doLogin(); return true; }
            if (inChat && chatField!=null) {
                String t=chatField.getText().trim();
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
        String t=tokenField.getText().trim();
        if (t.isEmpty()) { loginMsg="§cToken required!"; loginMsgColor=RED_C; return; }
        loginMsg="§7Connecting..."; loginMsgColor=TEXT_G;
        DiscordMC.discord.loginWithToken(t).thenAccept(ok -> client.execute(() -> {
            if (ok) { inLogin=false; clearAndInit(); }
            else { loginMsg="§cInvalid token!"; loginMsgColor=RED_C; }
        }));
    }

    private void openChat(String id, String name) {
        openChId=id; openChName=name; inChat=true;
        synchronized(messages){ messages.clear(); } msgScroll=0;
        DiscordMC.discord.fetchMessages(id).thenAccept(msgs -> { synchronized(messages){ messages.addAll(msgs); } autoScroll(); });
        clearAndInit();
    }

    @Override public boolean shouldPause() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }
}
