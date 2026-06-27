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

    // Colors
    private static final int G_L1     = 0x2A0F1E35;
    private static final int G_L2     = 0x3A122240;
    private static final int G_L3     = 0x4A14264A;
    private static final int G_PANEL  = 0x5515263F;
    private static final int G_CARD   = 0x661A2E4A;
    private static final int G_DARK   = 0xAA080E1A;
    private static final int G_GLOW   = 0x2200D4FF;
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
    private static final int DARK_BG  = 0xFF060A14;

    // Layout — NO servers, just DMs
    private static final int GW = 58;  // guild bar (home only)
    private static final int SW = 215; // sidebar
    private static final int TH = 46;  // topbar
    private static final int IH = 42;  // input bar

    // State
    private final List<JsonObject> dms = new ArrayList<>();
    private boolean loading = false;
    private int dmScroll = 0, msgScroll = 0;
    private boolean msgScrollLocked = true; // auto-scroll to bottom
    private String openChId = null, openChName = null;
    private final List<JsonObject> messages = new ArrayList<>();
    private boolean inChat = false, inLogin = false;
    private TextFieldWidget tokenField, chatField;
    private String loginMsg = "";
    private int loginMsgColor = TEXT_G;

    // Sending animation
    private long lastSentTime = 0;
    private String lastSentText = "";

    public DiscordMainScreen(Screen parent) {
        super(Text.literal("Discord"));
        this.parent = parent;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {}

    @Override
    protected void init() {
        Anim.reset("login_in");
        Anim.reset("sidebar_in");

        if (!DiscordMC.discord.isLoggedIn()) {
            inLogin = true;
            int fw = 268, fx = width/2-fw/2, fy = height/2+8;
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
            DiscordMC.discord.fetchDMs().thenAccept(l -> {
                synchronized(dms) { dms.clear(); dms.addAll(l); }
                loading = false;
            });
        }

        if (inChat && openChId != null) {
            int fx = GW+SW+10, fw = width-GW-SW-56;
            chatField = new TextFieldWidget(textRenderer, fx, height-IH+11, fw, 20, Text.literal(""));
            chatField.setPlaceholder(Text.literal("§8Message #" + openChName + "..."));
            chatField.setMaxLength(2000);
            addDrawableChild(chatField);
            setInitialFocus(chatField);

            // Real-time incoming messages
            DiscordMC.discord.onMessage(m -> {
                String chId = m.get("channel_id").getAsString();
                if (chId.equals(openChId)) {
                    synchronized(messages) { messages.add(m); }
                    if (msgScrollLocked) autoScroll();
                }
            });
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        float pulse = Anim.pulse(0.5f);

        // Deep dark bg
        ctx.fill(0, 0, width, height, DARK_BG);
        ctx.fill(0, 0, width, height, G_L1);

        // Grid
        for (int x=0; x<width; x+=40) for (int y=0; y<height; y+=40)
            ctx.fill(x, y, x+1, y+1, 0x0CFFFFFF);

        // Top glow
        int ga = (int)(80+pulse*60);
        ctx.fill(0, 0, width, 1, (ga<<24)|0x00D4FF);
        ctx.fill(0, 1, width, 2, ((ga/3)<<24)|0x00D4FF);

        if (inLogin) { renderLogin(ctx, mx, my, pulse); super.render(ctx, mx, my, delta); return; }

        float sAnim = Anim.lerp("sidebar_in", 1f, 8f);
        int sOff = (int)((1f-sAnim)*-(GW+SW));

        renderGuildBar(ctx, mx, my, sOff, pulse);
        renderSidebar(ctx, mx, my, sOff, pulse);
        renderMain(ctx, mx, my, pulse);

        super.render(ctx, mx, my, delta);
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    private void renderLogin(DrawContext ctx, int mx, int my, float pulse) {
        float anim = Anim.lerp("login_in", 1f, 6f);
        int pw=350, ph=280, px=width/2-pw/2;
        int py = (int)(height/2-ph/2-(1f-anim)*20);
        int alpha = (int)(anim*255); if (alpha<=0) return;

        fa(ctx, px-8, py-8, px+pw+8, py+ph+8, 0x0500D4FF, alpha);
        fa(ctx, px-4, py-4, px+pw+4, py+ph+4, 0x1200D4FF, alpha);
        fa(ctx, px, py, px+pw, py+ph, G_L1, alpha);
        fa(ctx, px, py, px+pw, py+ph, G_L2, alpha);
        fa(ctx, px, py, px+pw, py+ph, G_PANEL, alpha);
        fa(ctx, px+1, py+1, px+pw-1, py+6, 0x15FFFFFF, alpha);
        int bA = (int)((0.7f+pulse*0.3f)*alpha);
        fa(ctx, px, py, px+pw, py+2, CYAN, bA);
        fa(ctx, px, py, px+1, py+ph, CYAN_DIM, alpha/2);
        fa(ctx, px+pw-1, py, px+pw, py+ph, CYAN_DIM, alpha/2);

        int ix=width/2-26, iy=py+18;
        fa(ctx, ix-3, iy-3, ix+55, iy+55, CYAN_DIM, alpha);
        fa(ctx, ix, iy, ix+52, iy+52, G_DARK, alpha);
        fa(ctx, ix+1, iy+1, ix+51, iy+51, G_GLOW, alpha);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§b§lDC"), width/2, iy+19, ba(CYAN, alpha));

        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§f§lWelcome to Discord MC"), width/2, py+90, ba(TEXT_W, alpha));
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§7Login with your Discord token"), width/2, py+104, ba(TEXT_G, alpha));
        ctx.drawTextWithShadow(textRenderer, Text.literal("§b▸ TOKEN"), px+18, py+122, ba(CYAN, alpha));

        fa(ctx, px+14, py+134, px+pw-14, py+160, G_DARK, alpha);
        fa(ctx, px+14, py+134, px+pw-14, py+135, CYAN, alpha);

        if (!loginMsg.isEmpty())
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(loginMsg), width/2, py+168, ba(loginMsgColor, alpha));

        int bx=width/2-95, by=py+180;
        boolean hov = mx>=bx && mx<=bx+190 && my>=by && my<=by+30;
        float bAnim = Anim.lerp("btn_hov", hov?1f:0f, 10f);
        if (bAnim>0) fa(ctx, bx-3, by-3, bx+193, by+33, CYAN, (int)(bAnim*60));
        fa(ctx, bx, by, bx+190, by+30, bi(0xFF005566, 0xFF0099BB, bAnim), alpha);
        fa(ctx, bx, by, bx+190, by+3, 0x20FFFFFF, alpha);
        fa(ctx, bx, by, bx+190, by+1, CYAN, alpha);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§f§l⚡  Login"), width/2, by+11, ba(WHITE, alpha));

        boolean hb = mx>=width/2-55 && mx<=width/2+55 && my>=py+ph-22 && my<=py+ph-4;
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal(hb?"§b← Back to Minecraft":"§8← Back"),
            width/2, py+ph-18, ba(hb?CYAN:TEXT_G, alpha));
    }

    // ── Guild Bar — HOME ONLY, no servers ────────────────────────────────────
    private void renderGuildBar(DrawContext ctx, int mx, int my, int sOff, float pulse) {
        int ox = sOff;
        ctx.fill(ox, 0, ox+GW, height, DARK_BG);
        ctx.fill(ox, 0, ox+GW, height, G_L1);
        ctx.fill(ox, 0, ox+GW, height, G_L2);
        ctx.fill(ox+GW-1, 0, ox+GW, height, 0x4400D4FF);

        // Home/DMs button only
        boolean hh = mx>=ox+4 && mx<=ox+GW-4 && my>=8 && my<=52;
        float hA = Anim.lerp("home_hov", hh?1f:0f, 10f);
        if (hA>0) ctx.fill(ox+2, 6, ox+GW-2, 54, ba(CYAN_DIM, (int)(hA*255)));
        ctx.fill(ox+4, 8, ox+GW-4, 52, G_PANEL);
        ctx.fill(ox+4, 8, ox+GW-4, 9, ba(CYAN, (int)((0.5f+hA*0.5f)*255)));
        ctx.fill(ox+4, 9, ox+GW-4, 13, 0x10FFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§b⌂"), ox+GW/2, 21, hh?CYAN:CYAN2);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§8DMs"), ox+GW/2, 34, TEXT_G);

        // Bottom branding in guild bar
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§b§lDMC"), ox+GW/2, height-40, CYAN2);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§8v1.0"), ox+GW/2, height-28, TEXT_G);
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────
    private void renderSidebar(DrawContext ctx, int mx, int my, int sOff, float pulse) {
        int sx = sOff+GW;
        ctx.fill(sx, 0, sx+SW, height, DARK_BG);
        ctx.fill(sx, 0, sx+SW, height, G_L1);
        ctx.fill(sx, 0, sx+SW, height, G_L2);
        ctx.fill(sx, 0, sx+SW, height, G_L3);
        ctx.fill(sx+SW-1, 0, sx+SW, height, 0x5500D4FF);

        // Header
        ctx.fill(sx, 0, sx+SW, TH, G_DARK);
        ctx.fill(sx, TH-1, sx+SW, TH, 0x8800D4FF);
        ctx.fill(sx, 0, sx+SW, 2, 0x10FFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§b§l💬  §fDirect Messages"), sx+10, TH/2-4, TEXT_W);

        // Section label
        ctx.drawTextWithShadow(textRenderer, Text.literal("§8▸  MESSAGES"), sx+10, TH+7, TEXT_G);

        if (loading) {
            int la = (int)(150+pulse*100);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§b⟳ §7Loading..."), sx+SW/2, height/2, ba(CYAN, la));
            renderUserPanel(ctx, sx, pulse);
            return;
        }

        // DM list
        int y = TH+22 - dmScroll;
        int idx = 0;
        synchronized(dms) {
            for (JsonObject dm : dms) {
                if (y+46<TH+22 || y>height-58) { y+=48; idx++; continue; }
                String name = getDMName(dm);
                String uid  = getDMUserId(dm);
                String avHash = getDMAvatarHash(dm);
                boolean active = inChat && openChId!=null && openChId.equals(dm.get("id").getAsString());
                boolean hov = mx>=sx+2 && mx<=sx+SW-2 && my>=y && my<=y+44;
                float rowA = Anim.lerp("dm_"+idx, (hov||active)?1f:0f, 10f);

                // Glass card
                if (rowA>0||active) {
                    fa(ctx, sx+1, y, sx+SW-1, y+44, G_CARD, (int)((active?0.8f:rowA*0.5f)*255));
                    if (active) fa(ctx, sx+1, y, sx+SW-1, y+44, CYAN_DIM, (int)(rowA*80));
                }
                if (active) {
                    ctx.fill(sx+1, y+4, sx+4, y+40, CYAN);
                    ctx.fill(sx+4, y+4, sx+8, y+40, ba(CYAN_DIM, 180));
                }
                if (hov||active) {
                    ctx.fill(sx+1, y, sx+SW-1, y+1, ba(CYAN, (int)(rowA*150)));
                    ctx.fill(sx+1, y+43, sx+SW-1, y+44, ba(CYAN_DIM, (int)(rowA*80)));
                }

                // Avatar
                int ax=sx+10, ay=y+8;
                if (active||hov) fa(ctx, ax-2, ay-2, ax+30, ay+30, CYAN_DIM, (int)(rowA*200));
                ctx.fill(ax, ay, ax+28, ay+28, G_DARK);
                ctx.fill(ax+1, ay+1, ax+27, ay+27, G_GLOW);
                ctx.fill(ax+1, ay+1, ax+27, ay+5, 0x12FFFFFF);

                Identifier avTex = AvatarCache.get(uid, avHash);
                if (avTex!=null) ctx.drawTexture(avTex, ax+1, ay+1, 0, 0, 26, 26, 26, 26);
                else ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("§b§l"+name.substring(0,1).toUpperCase()), ax+14, ay+9, active?CYAN:CYAN2);

                // Presence dot
                String pres = DiscordMC.discord.getPresence(uid);
                int pColor = presColor(pres);
                int dotP = pres.equals("online")?(int)(200+pulse*55):200;
                ctx.fill(ax+19, ay+19, ax+28, ay+28, G_DARK);
                ctx.fill(ax+20, ay+20, ax+27, ay+27, ba(pColor, dotP));
                if (!pres.equals("offline")&&!pres.isEmpty())
                    ctx.fill(ax+18, ay+18, ax+29, ay+29, ba(pColor, (int)(pulse*50)));

                // Name + status
                ctx.drawTextWithShadow(textRenderer,
                    Text.literal((active?"§f§l":hov?"§b§l":"§7")+clip(name,15)),
                    ax+34, y+10, active?TEXT_W:TEXT_G);
                ctx.drawTextWithShadow(textRenderer, Text.literal(presLabel(pres)), ax+34, y+24, pColor);

                y+=48; idx++;
            }
        }

        if (dms.isEmpty()&&!loading)
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§7No DMs"), sx+SW/2, height/2, TEXT_G);

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
        String myAv = me.has("avatar")&&!me.get("avatar").isJsonNull()?me.get("avatar").getAsString():"";

        int ax=sx+8, ay=height-48;
        fa(ctx, ax-2, ay-2, ax+36, ay+36, ba(CYAN_DIM,(int)(150+pulse*60)), 255);
        ctx.fill(ax, ay, ax+34, ay+34, G_DARK);
        ctx.fill(ax+1, ay+1, ax+33, ay+33, G_GLOW);
        ctx.fill(ax+1, ay+1, ax+33, ay+5, 0x12FFFFFF);

        Identifier myAvTex = AvatarCache.get(myId, myAv);
        if (myAvTex!=null) ctx.drawTexture(myAvTex, ax+1, ay+1, 0, 0, 32, 32, 32, 32);
        else ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§b§l"+name.substring(0,1).toUpperCase()), ax+17, ay+12, CYAN);

        int dotA = (int)(200+pulse*55);
        ctx.fill(ax+24, ay+24, ax+34, ay+34, G_DARK);
        ctx.fill(ax+25, ay+25, ax+33, ay+33, ba(GREEN, dotA));
        ctx.fill(ax+23, ay+23, ax+35, ay+35, ba(GREEN, (int)(pulse*50)));

        ctx.drawTextWithShadow(textRenderer, Text.literal("§f§l"+clip(name,13)), sx+50, height-46, TEXT_W);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§a⬤ §aOnline"), sx+50, height-32, GREEN);

        // Made by — bottom of user panel
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§8Made by §bAbhayTheMaster"), sx+8, height-16, TEXT_G);
    }

    // ── Main Content ──────────────────────────────────────────────────────────
    private void renderMain(DrawContext ctx, int mx, int my, float pulse) {
        int cx=GW+SW, cw=width-cx;
        ctx.fill(cx, 0, width, height, DARK_BG);
        ctx.fill(cx, 0, width, height, G_L1);

        // Topbar
        ctx.fill(cx, 0, width, TH, G_DARK);
        ctx.fill(cx, 0, width, TH, G_L2);
        ctx.fill(cx, TH-1, width, TH, 0x8800D4FF);
        ctx.fill(cx, 0, width, 2, 0x10FFFFFF);

        if (!inChat) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("§b§l💬  §fDMs"), cx+14, TH/2-4, TEXT_W);
            int ew=200, eh=100, ex=cx+cw/2-ew/2, ey=height/2-eh/2;
            fa(ctx, ex-6, ey-6, ex+ew+6, ey+eh+6, ba(CYAN_DIM,(int)(80+pulse*40)), 255);
            ctx.fill(ex, ey, ex+ew, ey+eh, G_PANEL);
            ctx.fill(ex, ey, ex+ew, ey+eh, G_L2);
            ctx.fill(ex, ey, ex+ew, ey+2, ba(CYAN,(int)(200+pulse*55)));
            ctx.fill(ex, ey+1, ex+ew, ey+5, 0x10FFFFFF);
            ctx.fill(ex, ey, ex+1, ey+eh, CYAN_DIM);
            ctx.fill(ex+ew-1, ey, ex+ew, ey+eh, CYAN_DIM);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§b#"), cx+cw/2, ey+22, ba(CYAN,(int)(200+pulse*55)));
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§f§lSelect a DM"), cx+cw/2, ey+44, TEXT_W);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§8Choose from the left sidebar"), cx+cw/2, ey+58, TEXT_G);
            return;
        }

        // Chat header — NO "ESC=back"
        ctx.drawTextWithShadow(textRenderer, Text.literal("§b# §f§l"+openChName), cx+14, TH/2-4, TEXT_W);

        // Messages
        ctx.fill(cx, TH, width, height-IH, DARK_BG);
        ctx.fill(cx, TH, width, height-IH, G_L1);
        ctx.fill(cx, TH, cx+1, height-IH, CYAN_DIM);

        synchronized(messages) {
            if (messages.isEmpty()) {
                ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§8No messages yet..."), cx+cw/2, height/2, TEXT_G);
            } else {
                // Calculate total content height
                int totalH = messages.size() * 44;
                int visH = height-TH-IH;
                int maxScroll = Math.max(0, totalH-visH+10);
                // Clamp scroll
                msgScroll = Math.max(0, Math.min(msgScroll, maxScroll));

                int y = TH+10 - msgScroll;
                for (JsonObject msg : messages) {
                    if (y+44<TH || y>height-IH) { y+=44; continue; }
                    String author = msg.has("author")?msg.getAsJsonObject("author").get("username").getAsString():"?";
                    String content = msg.has("content")?msg.get("content").getAsString():"";
                    String aId = msg.has("author")?msg.getAsJsonObject("author").get("id").getAsString():"";
                    String aHash = msg.has("author")&&msg.getAsJsonObject("author").has("avatar")
                        &&!msg.getAsJsonObject("author").get("avatar").isJsonNull()
                        ?msg.getAsJsonObject("author").get("avatar").getAsString():"";
                    boolean self = DiscordMC.discord.getSelfUser()!=null &&
                        aId.equals(DiscordMC.discord.getSelfUser().get("id").getAsString());

                    // Sending animation for last sent message
                    boolean isLastSent = self && content.equals(lastSentText)
                        && System.currentTimeMillis()-lastSentTime < 1000;
                    float sentAnim = isLastSent ? Anim.lerp("msg_sent", 1f, 8f) : 1f;

                    // Avatar
                    int ax=cx+8, ay=y;
                    if (isLastSent) fa(ctx, ax-2, ay-2, ax+28, ay+28, ba(CYAN_DIM,(int)(sentAnim*150)), 255);
                    ctx.fill(ax, ay, ax+26, ay+26, G_DARK);
                    ctx.fill(ax+1, ay+1, ax+25, ay+25, G_GLOW);
                    ctx.fill(ax+1, ay+1, ax+25, ay+5, 0x0FFFFFFF);

                    Identifier avTex = AvatarCache.get(aId, aHash);
                    if (avTex!=null) ctx.drawTexture(avTex, ax+1, ay+1, 0, 0, 24, 24, 24, 24);
                    else ctx.drawCenteredTextWithShadow(textRenderer,
                        Text.literal("§b"+author.substring(0,1).toUpperCase()), ax+13, ay+8, self?CYAN:0xFFFFAA00);

                    // Author
                    ctx.drawTextWithShadow(textRenderer,
                        Text.literal((self?"§b§l":"§e§l")+author), cx+40, y, self?CYAN:0xFFFFCC00);

                    // Content
                    int maxC = Math.max(20, (cw-52)/6);
                    String line1 = content.length()>maxC?content.substring(0,maxC):content;
                    String line2 = content.length()>maxC?content.substring(maxC,Math.min(maxC*2,content.length())):"";
                    ctx.drawTextWithShadow(textRenderer, Text.literal("§f"+line1), cx+40, y+12, TEXT_W);
                    if (!line2.isEmpty())
                        ctx.drawTextWithShadow(textRenderer, Text.literal("§7"+line2), cx+40, y+22, TEXT_G);

                    y+=44;
                }
            }
        }

        // Input bar
        ctx.fill(cx, height-IH, width, height, G_DARK);
        ctx.fill(cx, height-IH, width, height, G_L2);
        ctx.fill(cx, height-IH, width, height-IH+1, 0x8800D4FF);
        ctx.fill(cx+8, height-IH+9, width-42, height-9, G_DARK);
        ctx.fill(cx+9, height-IH+10, width-43, height-10, G_GLOW);
        ctx.fill(cx+9, height-IH+10, width-43, height-IH+13, 0x0CFFFFFF);
        ctx.fill(cx+8, height-IH+9, cx+9, height-9, CYAN_DIM);

        // Send button
        int sbx=width-38, sby=height-IH+8;
        ctx.fill(sbx, sby, sbx+30, sby+26, G_PANEL);
        ctx.fill(sbx, sby, sbx+30, sby+1, CYAN_DIM);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§b§l↵"), sbx+15, sby+9, CYAN);
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
    private String getDMAvatarHash(JsonObject dm) {
        try {
            JsonObject r = dm.getAsJsonArray("recipients").get(0).getAsJsonObject();
            return r.has("avatar")&&!r.get("avatar").isJsonNull()?r.get("avatar").getAsString():"";
        } catch (Exception e) { return ""; }
    }
    private int presColor(String p) { return switch(p){ case"online"->GREEN; case"idle"->YELLOW; case"dnd"->RED_C; default->OFFLINE; }; }
    private String presLabel(String p) { return switch(p){ case"online"->"§a⬤ Online"; case"idle"->"§e⬤ Idle"; case"dnd"->"§c⬤ DnD"; default->"§8○ Offline"; }; }
    private String clip(String s, int max) { return s.length()>max?s.substring(0,max-2)+"..":s; }

    private void autoScroll() {
        synchronized(messages) {
            int totalH = messages.size()*44;
            int visH = height-TH-IH;
            msgScroll = Math.max(0, totalH-visH+10);
        }
    }

    // Color helpers
    private int ba(int color, int alpha) { return (color&0x00FFFFFF)|((Math.min(255,alpha))<<24); }
    private int bi(int c1, int c2, float t) {
        int r1=(c1>>16)&0xFF, g1=(c1>>8)&0xFF, b1=c1&0xFF;
        int r2=(c2>>16)&0xFF, g2=(c2>>8)&0xFF, b2=c2&0xFF;
        return 0xFF000000|((int)(r1+(r2-r1)*t))<<16|((int)(g1+(g2-g1)*t))<<8|(int)(b1+(b2-b1)*t);
    }
    private void fa(DrawContext ctx, int x1, int y1, int x2, int y2, int color, int alpha) {
        if (alpha<=0) return;
        int baseA = (color>>24)&0xFF;
        int finalA = baseA>0 ? Math.min(255, baseA*alpha/255) : alpha;
        ctx.fill(x1, y1, x2, y2, (color&0x00FFFFFF)|(finalA<<24));
    }

    // ── Events ────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (inLogin) {
            int py = height/2-140;
            if (mx>=width/2-95 && mx<=width/2+95 && my>=py+180 && my<=py+210) { doLogin(); return true; }
            if (my>=py+258) { client.setScreen(parent); return true; }
            return super.mouseClicked(mx, my, btn);
        }
        // Home
        if (mx>=4 && mx<=GW-4 && my>=8 && my<=52) {
            inChat=false; openChId=null; clearAndInit(); return true;
        }
        // DM click
        if (mx>=GW+2 && mx<=GW+SW-2) {
            int y=TH+22-dmScroll;
            synchronized(dms) {
                for (JsonObject dm : dms) {
                    if (my>=y && my<=y+44) { openChat(dm.get("id").getAsString(), getDMName(dm)); return true; }
                    y+=48;
                }
            }
        }
        // Send button
        if (inChat && mx>=width-38 && mx<=width-8 && my>=height-IH+8 && my<=height-IH+34) {
            sendMessage(); return true;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if (key==257||key==335) {
            if (inLogin) { doLogin(); return true; }
            if (inChat) { sendMessage(); return true; }
        }
        if (key==256) {
            if (inChat) { inChat=false; openChId=null; clearAndInit(); return true; }
            client.setScreen(parent); return true;
        }
        return super.keyPressed(key, scan, mod);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (mx<GW+SW) {
            dmScroll = Math.max(0, dmScroll-(int)(v*10));
        } else if (inChat) {
            msgScrollLocked = false; // unlock auto-scroll when manually scrolling
            msgScroll = Math.max(0, msgScroll-(int)(v*10));
            // Re-lock if scrolled to bottom
            synchronized(messages) {
                int totalH = messages.size()*44;
                int visH = height-TH-IH;
                int maxScroll = Math.max(0, totalH-visH+10);
                if (msgScroll >= maxScroll) msgScrollLocked = true;
            }
        }
        return true;
    }

    private void sendMessage() {
        if (chatField==null||openChId==null) return;
        String t = chatField.getText().trim();
        if (t.isEmpty()) return;

        // Immediately add to messages (optimistic update)
        JsonObject myMsg = new JsonObject();
        myMsg.addProperty("channel_id", openChId);
        myMsg.addProperty("content", t);
        JsonObject author = new JsonObject();
        if (DiscordMC.discord.getSelfUser()!=null) {
            author.addProperty("id", DiscordMC.discord.getSelfUser().get("id").getAsString());
            author.addProperty("username", DiscordMC.discord.getSelfUser().get("username").getAsString());
            if (DiscordMC.discord.getSelfUser().has("avatar"))
                author.add("avatar", DiscordMC.discord.getSelfUser().get("avatar"));
        }
        myMsg.add("author", author);

        synchronized(messages) { messages.add(myMsg); }

        // Track for animation
        lastSentText = t;
        lastSentTime = System.currentTimeMillis();
        Anim.reset("msg_sent");

        // Auto scroll to bottom
        msgScrollLocked = true;
        autoScroll();

        chatField.setText("");

        // Send in background
        DiscordMC.discord.sendMessage(openChId, t);
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
        synchronized(messages){ messages.clear(); } msgScroll=0; msgScrollLocked=true;
        DiscordMC.discord.fetchMessages(id).thenAccept(msgs -> {
            synchronized(messages){ messages.addAll(msgs); }
            autoScroll();
        });
        clearAndInit();
    }

    @Override public boolean shouldPause() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }
}
