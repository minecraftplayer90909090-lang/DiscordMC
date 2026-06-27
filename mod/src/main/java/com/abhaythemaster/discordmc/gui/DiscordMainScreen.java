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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DiscordMainScreen extends Screen {

    private final Screen parent;

    // ── Glassmorphic palette ──────────────────────────────────────────────
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
    private static final int BADGE    = 0xFFFF5252;

    // Layout
    private static final int GW = 58;
    private static final int SW = 215;
    private static final int TH = 46;
    private static final int IH = 42;
    private static final int UP = 56; // user panel height

    // State
    private final List<JsonObject> dms = new ArrayList<>();
    private boolean loading = false;
    private int dmScroll = 0, msgScroll = 0;
    private boolean msgScrollLocked = true;
    private String openChId = null, openChName = null;
    private final List<JsonObject> messages = new ArrayList<>();
    private boolean inChat = false, inLogin = false;
    private TextFieldWidget tokenField, chatField;
    private String loginMsg = "";
    private int loginMsgColor = TEXT_G;
    private long lastSentTime = 0;
    private String lastSentText = "";

    // Unread badges: dmId -> count
    private final Map<String, Integer> unreadMap = new ConcurrentHashMap<>();
    // Typing indicator
    private boolean someoneTyping = false;
    private String typingName = "";
    private long typingUntil = 0;

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
            int fw=268, fx=width/2-fw/2, fy=height/2+8;
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
                synchronized(dms){ dms.clear(); dms.addAll(l); }
                loading = false;
            });
        }

        if (inChat && openChId!=null) {
            int fx=GW+SW+10, fw=width-GW-SW-56;
            chatField = new TextFieldWidget(textRenderer, fx, height-IH+11, fw, 20, Text.literal(""));
            chatField.setPlaceholder(Text.literal("§8Message #"+openChName+"..."));
            chatField.setMaxLength(2000);
            addDrawableChild(chatField);
            setInitialFocus(chatField);
            DiscordMC.discord.onMessage(m -> {
                if (m.get("channel_id").getAsString().equals(openChId)) {
                    synchronized(messages){ messages.add(m); }
                    if (msgScrollLocked) autoScroll();
                } else {
                    // Increment unread for other DMs
                    String chId = m.get("channel_id").getAsString();
                    unreadMap.merge(chId, 1, Integer::sum);
                }
            });
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        float pulse = Anim.pulse(0.5f);
        ctx.fill(0, 0, width, height, DARK_BG);
        ctx.fill(0, 0, width, height, G_L1);
        for (int x=0;x<width;x+=40) for (int y=0;y<height;y+=40) ctx.fill(x,y,x+1,y+1,0x0CFFFFFF);
        int ga=(int)(80+pulse*60);
        ctx.fill(0,0,width,1,(ga<<24)|0x00D4FF);
        ctx.fill(0,1,width,2,((ga/3)<<24)|0x00D4FF);

        if (inLogin){ renderLogin(ctx,mx,my,pulse); super.render(ctx,mx,my,delta); return; }

        float sAnim = Anim.lerp("sidebar_in",1f,8f);
        int sOff = (int)((1f-sAnim)*-(GW+SW));

        renderGuildBar(ctx,mx,my,sOff,pulse);
        renderSidebar(ctx,mx,my,sOff,pulse);
        renderMain(ctx,mx,my,pulse);
        super.render(ctx,mx,my,delta);
    }

    // ── Rounded rect helper ───────────────────────────────────────────────────
    // Simulates rounded corners by cutting corner pixels with bg color
    private void roundedRect(DrawContext ctx, int x, int y, int w, int h, int color, int r) {
        if (r<=0){ ctx.fill(x,y,x+w,y+h,color); return; }
        ctx.fill(x+r, y, x+w-r, y+h, color);   // center vertical
        ctx.fill(x, y+r, x+r, y+h-r, color);    // left
        ctx.fill(x+w-r, y+r, x+w, y+h-r, color); // right
        // corners (1px steps for r=2)
        for (int i=0;i<r;i++) {
            int len = r-i-1;
            ctx.fill(x+i, y+r-1-i, x+i+1, y+r-i, color);
            ctx.fill(x+w-i-1, y+r-1-i, x+w-i, y+r-i, color);
            ctx.fill(x+i, y+h-r+i, x+i+1, y+h-r+i+1, color);
            ctx.fill(x+w-i-1, y+h-r+i, x+w-i, y+h-r+i+1, color);
        }
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    private void renderLogin(DrawContext ctx, int mx, int my, float pulse) {
        float anim=Anim.lerp("login_in",1f,6f);
        int pw=350, ph=280, px=width/2-pw/2, py=(int)(height/2-ph/2-(1f-anim)*20);
        int alpha=(int)(anim*255); if(alpha<=0) return;

        fa(ctx,px-8,py-8,px+pw+8,py+ph+8,0x0500D4FF,alpha);
        fa(ctx,px-4,py-4,px+pw+4,py+ph+4,0x1200D4FF,alpha);
        fa(ctx,px,py,px+pw,py+ph,G_L1,alpha);
        fa(ctx,px,py,px+pw,py+ph,G_L2,alpha);
        fa(ctx,px,py,px+pw,py+ph,G_PANEL,alpha);
        fa(ctx,px+1,py+2,px+pw-1,py+6,0x15FFFFFF,alpha);
        int bA=(int)((0.7f+pulse*0.3f)*alpha);
        fa(ctx,px,py,px+pw,py+2,CYAN,bA);
        fa(ctx,px,py,px+1,py+ph,CYAN_DIM,alpha/2);
        fa(ctx,px+pw-1,py,px+pw,py+ph,CYAN_DIM,alpha/2);

        int ix=width/2-26,iy=py+18;
        fa(ctx,ix-3,iy-3,ix+55,iy+55,CYAN_DIM,alpha);
        fa(ctx,ix,iy,ix+52,iy+52,G_DARK,alpha);
        fa(ctx,ix+1,iy+1,ix+51,iy+51,G_GLOW,alpha);
        ctx.drawCenteredTextWithShadow(textRenderer,Text.literal("§b§lDC"),width/2,iy+19,ba(CYAN,alpha));

        ctx.drawCenteredTextWithShadow(textRenderer,Text.literal("§f§lWelcome to Discord MC"),width/2,py+90,ba(TEXT_W,alpha));
        ctx.drawCenteredTextWithShadow(textRenderer,Text.literal("§7Login with your Discord token"),width/2,py+104,ba(TEXT_G,alpha));
        ctx.drawTextWithShadow(textRenderer,Text.literal("§b▸ TOKEN"),px+18,py+122,ba(CYAN,alpha));
        fa(ctx,px+14,py+134,px+pw-14,py+160,G_DARK,alpha);
        fa(ctx,px+14,py+134,px+pw-14,py+135,CYAN,alpha);

        if(!loginMsg.isEmpty())
            ctx.drawCenteredTextWithShadow(textRenderer,Text.literal(loginMsg),width/2,py+168,ba(loginMsgColor,alpha));

        int bx=width/2-95,by=py+180;
        boolean hov=mx>=bx&&mx<=bx+190&&my>=by&&my<=by+30;
        float bAnim=Anim.lerp("btn_hov",hov?1f:0f,10f);
        if(bAnim>0) fa(ctx,bx-3,by-3,bx+193,by+33,CYAN,(int)(bAnim*60));
        fa(ctx,bx,by,bx+190,by+30,bi(0xFF005566,0xFF0099BB,bAnim),alpha);
        fa(ctx,bx,by,bx+190,by+3,0x20FFFFFF,alpha);
        fa(ctx,bx,by,bx+190,by+1,CYAN,alpha);
        ctx.drawCenteredTextWithShadow(textRenderer,Text.literal("§f§l⚡  Login"),width/2,by+11,ba(WHITE,alpha));

        boolean hb=mx>=width/2-55&&mx<=width/2+55&&my>=py+ph-22&&my<=py+ph-4;
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal(hb?"§b← Back":"§8← Back"),width/2,py+ph-18,ba(hb?CYAN:TEXT_G,alpha));
    }

    // ── Guild Bar ─────────────────────────────────────────────────────────────
    private void renderGuildBar(DrawContext ctx, int mx, int my, int sOff, float pulse) {
        int ox=sOff;
        ctx.fill(ox,0,ox+GW,height,DARK_BG);
        ctx.fill(ox,0,ox+GW,height,G_L1);
        ctx.fill(ox,0,ox+GW,height,G_L2);
        ctx.fill(ox+GW-1,0,ox+GW,height,0x4400D4FF);

        boolean hh=mx>=ox+4&&mx<=ox+GW-4&&my>=8&&my<=52;
        float hA=Anim.lerp("home_hov",hh?1f:0f,10f);
        if(hA>0) fa(ctx,ox+2,6,ox+GW-2,54,CYAN,(int)(hA*60));
        roundedRect(ctx,ox+4,8,GW-8,44,G_PANEL,3);
        fa(ctx,ox+4,8,ox+GW-4,10,0x18FFFFFF,255);
        ctx.fill(ox+4,8,ox+GW-4,9,ba(CYAN,(int)((0.5f+hA*0.5f)*255)));
        ctx.drawCenteredTextWithShadow(textRenderer,Text.literal("§b⌂"),ox+GW/2,21,hh?CYAN:CYAN2);
        ctx.drawCenteredTextWithShadow(textRenderer,Text.literal("§8DMs"),ox+GW/2,34,TEXT_G);

        // Bottom version tag
        ctx.drawCenteredTextWithShadow(textRenderer,Text.literal("§b§lDMC"),ox+GW/2,height-38,CYAN2);
        ctx.drawCenteredTextWithShadow(textRenderer,Text.literal("§8v1.0"),ox+GW/2,height-26,TEXT_G);
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────
    private void renderSidebar(DrawContext ctx, int mx, int my, int sOff, float pulse) {
        int sx=sOff+GW;
        ctx.fill(sx,0,sx+SW,height,DARK_BG);
        ctx.fill(sx,0,sx+SW,height,G_L1);
        ctx.fill(sx,0,sx+SW,height,G_L2);
        ctx.fill(sx,0,sx+SW,height,G_L3);
        ctx.fill(sx+SW-1,0,sx+SW,height,0x5500D4FF);

        // Header
        ctx.fill(sx,0,sx+SW,TH,G_DARK);
        ctx.fill(sx,TH-1,sx+SW,TH,0x8800D4FF);
        ctx.fill(sx,0,sx+SW,2,0x10FFFFFF);

        // Online count badge in header
        int onlineCount=0;
        synchronized(dms){
            for(JsonObject dm:dms){
                String uid=getDMUserId(dm);
                if("online".equals(DiscordMC.discord.getPresence(uid))) onlineCount++;
            }
        }
        ctx.drawTextWithShadow(textRenderer,Text.literal("§b§l💬  §fDMs"),sx+10,TH/2-6,TEXT_W);
        if(onlineCount>0){
            // Green online badge
            String badge="§a"+onlineCount+" online";
            ctx.drawTextWithShadow(textRenderer,Text.literal(badge),sx+10,TH/2+4,GREEN);
        }

        // Section label
        ctx.drawTextWithShadow(textRenderer,Text.literal("§8▸  MESSAGES"),sx+10,TH+7,TEXT_G);

        if(loading){
            int la=(int)(150+pulse*100);
            ctx.drawCenteredTextWithShadow(textRenderer,Text.literal("§b⟳ §7Loading..."),sx+SW/2,height/2,ba(CYAN,la));
            renderUserPanel(ctx,sx,pulse);
            return;
        }

        // DM list — clip to [TH+20 .. height-UP-4] so no overlap with user panel
        int listTop = TH+20;
        int listBottom = height-UP-4;
        int y=listTop-dmScroll;
        int idx=0;
        synchronized(dms){
            for(JsonObject dm:dms){
                int cardH=50;
                if(y+cardH<listTop||y>listBottom){ y+=cardH+2; idx++; continue; }

                String name=getDMName(dm);
                String uid=getDMUserId(dm);
                String avHash=getDMAvatarHash(dm);
                String dmId=dm.get("id").getAsString();
                boolean active=inChat&&openChId!=null&&openChId.equals(dmId);
                boolean hov=mx>=sx+4&&mx<=sx+SW-4&&my>=y&&my<=y+cardH;
                float rowA=Anim.lerp("dm_"+idx,(hov||active)?1f:0f,10f);
                int unread=unreadMap.getOrDefault(dmId,0);

                // Rounded glass card
                int cardColor = active ? 0x771A3A6A : (hov ? 0x551A2E5A : G_CARD);
                roundedRect(ctx,sx+4,y,SW-8,cardH,cardColor,3);
                fa(ctx,sx+4,y,sx+SW-4,y+2,0x10FFFFFF,255); // top specular

                // Glow border on hover/active
                if(rowA>0){
                    ctx.fill(sx+4,y,sx+SW-4,y+1,ba(CYAN,(int)(rowA*120)));
                    ctx.fill(sx+4,y+cardH-1,sx+SW-4,y+cardH,ba(CYAN_DIM,(int)(rowA*60)));
                }

                // Active left bar
                if(active){
                    ctx.fill(sx+4,y+4,sx+7,y+cardH-4,CYAN);
                    ctx.fill(sx+7,y+4,sx+10,y+cardH-4,CYAN_DIM);
                }

                // Avatar — rounded
                int ax=sx+12, ay=y+9;
                if(active||hov) fa(ctx,ax-2,ay-2,ax+34,ay+34,CYAN_DIM,(int)(rowA*180));
                roundedRect(ctx,ax,ay,30,30,G_DARK,3);
                ctx.fill(ax+1,ay+1,ax+29,ay+5,0x12FFFFFF);

                Identifier avTex=AvatarCache.get(uid,avHash);
                if(avTex!=null) ctx.drawTexture(avTex,ax+1,ay+1,0,0,28,28,28,28);
                else ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("§b§l"+name.substring(0,1).toUpperCase()),ax+15,ay+10,active?CYAN:CYAN2);

                // Presence dot — bottom right of avatar
                String pres=DiscordMC.discord.getPresence(uid);
                int pColor=presColor(pres);
                int dotAlpha=pres.equals("online")?(int)(200+pulse*55):200;
                roundedRect(ctx,ax+21,ay+21,10,10,DARK_BG,2);
                roundedRect(ctx,ax+22,ay+22,8,8,ba(pColor,dotAlpha),2);
                if(!pres.equals("offline")&&!pres.isEmpty())
                    fa(ctx,ax+20,ay+20,ax+32,ay+32,ba(pColor,(int)(pulse*40)),255);

                // Name + status
                int tx=ax+36;
                ctx.drawTextWithShadow(textRenderer,
                    Text.literal((active?"§f§l":hov?"§b§l":"§7")+clip(name,14)),
                    tx,y+11,active?TEXT_W:TEXT_G);
                ctx.drawTextWithShadow(textRenderer,Text.literal(presLabel(pres)),tx,y+25,pColor);

                // Unread badge (red pill)
                if(unread>0){
                    String ubStr=unread>99?"99+":String.valueOf(unread);
                    int bw=ubStr.length()*6+6;
                    int bx2=sx+SW-8-bw, by2=y+18;
                    roundedRect(ctx,bx2,by2,bw,12,BADGE,3);
                    ctx.drawTextWithShadow(textRenderer,Text.literal("§f"+ubStr),bx2+3,by2+2,WHITE);
                }

                y+=cardH+2; idx++;
            }
        }

        if(dms.isEmpty()&&!loading)
            ctx.drawCenteredTextWithShadow(textRenderer,Text.literal("§7No DMs"),sx+SW/2,(listTop+listBottom)/2,TEXT_G);

        renderUserPanel(ctx,sx,pulse);
    }

    // ── User Panel — fixed at bottom, NO overlap ──────────────────────────────
    private void renderUserPanel(DrawContext ctx, int sx, float pulse) {
        // Solid separator line first
        ctx.fill(sx,height-UP,sx+SW,height-UP+1,0x6600D4FF);
        // Dark glass bg
        ctx.fill(sx,height-UP+1,sx+SW,height,G_DARK);
        ctx.fill(sx,height-UP+1,sx+SW,height,G_L2);

        if(DiscordMC.discord.getSelfUser()==null) return;
        JsonObject me=DiscordMC.discord.getSelfUser();
        String name=me.get("username").getAsString();
        String myId=me.get("id").getAsString();
        String myAv=me.has("avatar")&&!me.get("avatar").isJsonNull()?me.get("avatar").getAsString():"";

        int ax=sx+8, ay=height-UP+8;
        // Avatar glow
        fa(ctx,ax-2,ay-2,ax+38,ay+38,ba(CYAN_DIM,(int)(140+pulse*60)),255);
        roundedRect(ctx,ax,ay,34,34,G_DARK,4);
        ctx.fill(ax+1,ay+1,ax+33,ay+5,0x12FFFFFF);

        Identifier myAvTex=AvatarCache.get(myId,myAv);
        if(myAvTex!=null) ctx.drawTexture(myAvTex,ax+1,ay+1,0,0,32,32,32,32);
        else ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§b§l"+name.substring(0,1).toUpperCase()),ax+17,ay+13,CYAN);

        // Online dot
        int dotA=(int)(200+pulse*55);
        roundedRect(ctx,ax+24,ay+24,11,11,DARK_BG,3);
        roundedRect(ctx,ax+25,ay+25,9,9,ba(GREEN,dotA),3);
        fa(ctx,ax+23,ay+23,ax+36,ay+36,ba(GREEN,(int)(pulse*40)),255);

        ctx.drawTextWithShadow(textRenderer,Text.literal("§f§l"+clip(name,13)),sx+50,height-UP+12,TEXT_W);
        ctx.drawTextWithShadow(textRenderer,Text.literal("§a⬤ §aOnline"),sx+50,height-UP+25,GREEN);
    }

    // ── Main Content ──────────────────────────────────────────────────────────
    private void renderMain(DrawContext ctx, int mx, int my, float pulse) {
        int cx=GW+SW, cw=width-cx;
        ctx.fill(cx,0,width,height,DARK_BG);
        ctx.fill(cx,0,width,height,G_L1);

        // Topbar
        ctx.fill(cx,0,width,TH,G_DARK);
        ctx.fill(cx,0,width,TH,G_L2);
        ctx.fill(cx,TH-1,width,TH,0x8800D4FF);
        ctx.fill(cx,0,width,2,0x10FFFFFF);

        if(!inChat){
            ctx.drawTextWithShadow(textRenderer,Text.literal("§b§l💬  §fDMs"),cx+14,TH/2-4,TEXT_W);
            int ew=200,eh=100,ex=cx+cw/2-ew/2,ey=height/2-eh/2;
            fa(ctx,ex-6,ey-6,ex+ew+6,ey+eh+6,ba(CYAN_DIM,(int)(80+pulse*40)),255);
            roundedRect(ctx,ex,ey,ew,eh,G_PANEL,4);
            roundedRect(ctx,ex,ey,ew,eh,G_L2,4);
            ctx.fill(ex,ey,ex+ew,ey+2,ba(CYAN,(int)(200+pulse*55)));
            ctx.fill(ex+1,ey+2,ex+ew-1,ey+6,0x10FFFFFF);
            ctx.drawCenteredTextWithShadow(textRenderer,Text.literal("§b§l#"),cx+cw/2,ey+22,ba(CYAN,(int)(200+pulse*55)));
            ctx.drawCenteredTextWithShadow(textRenderer,Text.literal("§f§lSelect a DM"),cx+cw/2,ey+44,TEXT_W);
            ctx.drawCenteredTextWithShadow(textRenderer,Text.literal("§8Choose from the left sidebar"),cx+cw/2,ey+58,TEXT_G);
            return;
        }

        // Chat header with avatar
        String openUid="";
        String openAvHash="";
        synchronized(dms){
            for(JsonObject dm:dms){
                if(dm.get("id").getAsString().equals(openChId)){
                    openUid=getDMUserId(dm);
                    openAvHash=getDMAvatarHash(dm);
                    break;
                }
            }
        }
        // Header avatar
        int hax=cx+10,hay=TH/2-11;
        roundedRect(ctx,hax,hay,22,22,G_DARK,3);
        Identifier hAvTex=AvatarCache.get(openUid,openAvHash);
        if(hAvTex!=null) ctx.drawTexture(hAvTex,hax+1,hay+1,0,0,20,20,20,20);
        ctx.drawTextWithShadow(textRenderer,Text.literal("§f§l"+openChName),cx+36,TH/2-4,TEXT_W);

        // Presence in header
        String hPres=DiscordMC.discord.getPresence(openUid);
        int hpColor=presColor(hPres);
        ctx.drawTextWithShadow(textRenderer,Text.literal(presLabel(hPres)),cx+36,TH/2+6,hpColor);

        // Messages area
        ctx.fill(cx,TH,width,height-IH,DARK_BG);
        ctx.fill(cx,TH,width,height-IH,G_L1);
        ctx.fill(cx,TH,cx+1,height-IH,CYAN_DIM);

        // Typing indicator
        if(someoneTyping&&System.currentTimeMillis()<typingUntil){
            int ta=(int)(150+pulse*80);
            ctx.drawTextWithShadow(textRenderer,
                Text.literal("§7"+typingName+" is typing§8..."),
                cx+14,height-IH-12,ba(TEXT_G,ta));
        }

        synchronized(messages){
            if(messages.isEmpty()){
                ctx.drawCenteredTextWithShadow(textRenderer,Text.literal("§8No messages yet..."),cx+cw/2,height/2,TEXT_G);
            } else {
                int totalH=messages.size()*46;
                int visH=height-TH-IH;
                int maxScroll=Math.max(0,totalH-visH+10);
                msgScroll=Math.max(0,Math.min(msgScroll,maxScroll));

                int y=TH+10-msgScroll;
                for(JsonObject msg:messages){
                    if(y+46<TH||y>height-IH){ y+=46; continue; }
                    String author=msg.has("author")?msg.getAsJsonObject("author").get("username").getAsString():"?";
                    String content=msg.has("content")?msg.get("content").getAsString():"";
                    String aId=msg.has("author")?msg.getAsJsonObject("author").get("id").getAsString():"";
                    String aHash=msg.has("author")&&msg.getAsJsonObject("author").has("avatar")
                        &&!msg.getAsJsonObject("author").get("avatar").isJsonNull()
                        ?msg.getAsJsonObject("author").get("avatar").getAsString():"";
                    boolean self=DiscordMC.discord.getSelfUser()!=null&&
                        aId.equals(DiscordMC.discord.getSelfUser().get("id").getAsString());
                    boolean isLastSent=self&&content.equals(lastSentText)&&System.currentTimeMillis()-lastSentTime<1500;
                    float sentA=isLastSent?Anim.lerp("msg_sent",1f,8f):1f;

                    // Message row bg for sent animation
                    if(isLastSent) fa(ctx,cx+4,y-1,width-4,y+45,ba(CYAN_DIM,(int)((1f-sentA)*60)),255);

                    // Rounded avatar
                    int ax=cx+8,ay=y+3;
                    if(isLastSent) fa(ctx,ax-2,ay-2,ax+28,ay+28,CYAN_DIM,(int)(sentA*100));
                    roundedRect(ctx,ax,ay,24,24,G_DARK,3);
                    ctx.fill(ax+1,ay+1,ax+23,ay+5,0x0FFFFFFF);

                    Identifier avTex=AvatarCache.get(aId,aHash);
                    if(avTex!=null) ctx.drawTexture(avTex,ax+1,ay+1,0,0,22,22,22,22);
                    else ctx.drawCenteredTextWithShadow(textRenderer,
                        Text.literal("§b"+author.substring(0,1).toUpperCase()),ax+12,ay+8,self?CYAN:0xFFFFAA00);

                    // Author + content
                    ctx.drawTextWithShadow(textRenderer,
                        Text.literal((self?"§b§l":"§e§l")+author),cx+38,y+3,self?CYAN:0xFFFFCC00);
                    int maxC=Math.max(20,(cw-52)/6);
                    ctx.drawTextWithShadow(textRenderer,
                        Text.literal("§f"+(content.length()>maxC?content.substring(0,maxC):content)),
                        cx+38,y+15,TEXT_W);
                    if(content.length()>maxC)
                        ctx.drawTextWithShadow(textRenderer,
                            Text.literal("§7"+content.substring(maxC,Math.min(maxC*2,content.length()))),
                            cx+38,y+25,TEXT_G);

                    y+=46;
                }
            }
        }

        // Input bar — rounded
        ctx.fill(cx,height-IH,width,height,G_DARK);
        ctx.fill(cx,height-IH,width,height,G_L2);
        ctx.fill(cx,height-IH,width,height-IH+1,0x8800D4FF);
        roundedRect(ctx,cx+8,height-IH+8,width-cx-44,IH-16,G_DARK,4);
        ctx.fill(cx+9,height-IH+9,width-37,height-IH+13,0x0CFFFFFF);
        ctx.fill(cx+8,height-IH+8,cx+9,height-8,CYAN_DIM);

        // Send button — rounded
        int sbx=width-36,sby=height-IH+7;
        roundedRect(ctx,sbx,sby,28,IH-14,G_PANEL,4);
        ctx.fill(sbx,sby,sbx+28,sby+2,CYAN_DIM);
        ctx.drawCenteredTextWithShadow(textRenderer,Text.literal("§b§l↵"),sbx+14,sby+(IH-14)/2-3,CYAN);
    }

    // ── Color helpers ─────────────────────────────────────────────────────────
    private int ba(int color,int alpha){ return (color&0x00FFFFFF)|(Math.min(255,alpha)<<24); }
    private int bi(int c1,int c2,float t){
        int r1=(c1>>16)&0xFF,g1=(c1>>8)&0xFF,b1=c1&0xFF;
        int r2=(c2>>16)&0xFF,g2=(c2>>8)&0xFF,b2=c2&0xFF;
        return 0xFF000000|((int)(r1+(r2-r1)*t))<<16|((int)(g1+(g2-g1)*t))<<8|(int)(b1+(b2-b1)*t);
    }
    private void fa(DrawContext ctx,int x1,int y1,int x2,int y2,int color,int alpha){
        if(alpha<=0||x2<=x1||y2<=y1) return;
        int baseA=(color>>24)&0xFF;
        int finalA=baseA>0?Math.min(255,baseA*alpha/255):Math.min(255,alpha);
        ctx.fill(x1,y1,x2,y2,(color&0x00FFFFFF)|(finalA<<24));
    }
    private int presColor(String p){ return switch(p){ case"online"->GREEN; case"idle"->YELLOW; case"dnd"->RED_C; default->OFFLINE; }; }
    private String presLabel(String p){ return switch(p){ case"online"->"§a⬤ Online"; case"idle"->"§e⬤ Idle"; case"dnd"->"§c⬤ DnD"; default->"§8○ Offline"; }; }
    private String getDMName(JsonObject dm){ try{ return dm.getAsJsonArray("recipients").get(0).getAsJsonObject().get("username").getAsString(); }catch(Exception e){ return "Unknown"; } }
    private String getDMUserId(JsonObject dm){ try{ return dm.getAsJsonArray("recipients").get(0).getAsJsonObject().get("id").getAsString(); }catch(Exception e){ return ""; } }
    private String getDMAvatarHash(JsonObject dm){ try{ JsonObject r=dm.getAsJsonArray("recipients").get(0).getAsJsonObject(); return r.has("avatar")&&!r.get("avatar").isJsonNull()?r.get("avatar").getAsString():""; }catch(Exception e){ return ""; } }
    private String clip(String s,int max){ return s.length()>max?s.substring(0,max-2)+"..":s; }
    private void autoScroll(){ synchronized(messages){ int t=messages.size()*46,v=height-TH-IH; msgScroll=Math.max(0,t-v+10); } }

    // ── Events ────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mx,double my,int btn){
        if(inLogin){
            int py=height/2-140;
            if(mx>=width/2-95&&mx<=width/2+95&&my>=py+180&&my<=py+210){ doLogin(); return true; }
            if(my>=py+258){ client.setScreen(parent); return true; }
            return super.mouseClicked(mx,my,btn);
        }
        if(mx>=4&&mx<=GW-4&&my>=8&&my<=52){ inChat=false; openChId=null; clearAndInit(); return true; }
        if(mx>=GW+4&&mx<=GW+SW-4){
            int listTop=TH+20;
            int y=listTop-dmScroll;
            synchronized(dms){
                for(JsonObject dm:dms){
                    if(my>=y&&my<=y+50&&my>=listTop){
                        String dmId=dm.get("id").getAsString();
                        unreadMap.remove(dmId); // clear unread on open
                        openChat(dmId,getDMName(dm)); return true;
                    }
                    y+=52;
                }
            }
        }
        if(inChat&&mx>=width-36&&mx<=width-8&&my>=height-IH+7&&my<=height-7){ sendMessage(); return true; }
        return super.mouseClicked(mx,my,btn);
    }

    @Override
    public boolean keyPressed(int key,int scan,int mod){
        if(key==257||key==335){
            if(inLogin){ doLogin(); return true; }
            if(inChat){ sendMessage(); return true; }
        }
        if(key==256){
            if(inChat){ inChat=false; openChId=null; clearAndInit(); return true; }
            client.setScreen(parent); return true;
        }
        return super.keyPressed(key,scan,mod);
    }

    @Override
    public boolean mouseScrolled(double mx,double my,double h,double v){
        if(mx<GW+SW){
            dmScroll=Math.max(0,dmScroll-(int)(v*10));
        } else if(inChat){
            msgScrollLocked=false;
            msgScroll=Math.max(0,msgScroll-(int)(v*10));
            synchronized(messages){
                int totalH=messages.size()*46,visH=height-TH-IH;
                if(msgScroll>=Math.max(0,totalH-visH+10)) msgScrollLocked=true;
            }
        }
        return true;
    }

    private void sendMessage(){
        if(chatField==null||openChId==null) return;
        String t=chatField.getText().trim();
        if(t.isEmpty()) return;
        JsonObject myMsg=new JsonObject();
        myMsg.addProperty("channel_id",openChId);
        myMsg.addProperty("content",t);
        JsonObject author=new JsonObject();
        if(DiscordMC.discord.getSelfUser()!=null){
            author.addProperty("id",DiscordMC.discord.getSelfUser().get("id").getAsString());
            author.addProperty("username",DiscordMC.discord.getSelfUser().get("username").getAsString());
            if(DiscordMC.discord.getSelfUser().has("avatar"))
                author.add("avatar",DiscordMC.discord.getSelfUser().get("avatar"));
        }
        myMsg.add("author",author);
        synchronized(messages){ messages.add(myMsg); }
        lastSentText=t; lastSentTime=System.currentTimeMillis();
        Anim.reset("msg_sent");
        msgScrollLocked=true; autoScroll();
        chatField.setText("");
        DiscordMC.discord.sendMessage(openChId,t);
    }

    private void doLogin(){
        if(tokenField==null) return;
        String t=tokenField.getText().trim();
        if(t.isEmpty()){ loginMsg="§cToken required!"; loginMsgColor=RED_C; return; }
        loginMsg="§7Connecting..."; loginMsgColor=TEXT_G;
        DiscordMC.discord.loginWithToken(t).thenAccept(ok->client.execute(()->{
            if(ok){ inLogin=false; clearAndInit(); }
            else{ loginMsg="§cInvalid token!"; loginMsgColor=RED_C; }
        }));
    }

    private void openChat(String id,String name){
        openChId=id; openChName=name; inChat=true;
        synchronized(messages){ messages.clear(); } msgScroll=0; msgScrollLocked=true;
        DiscordMC.discord.fetchMessages(id).thenAccept(msgs->{
            synchronized(messages){ messages.addAll(msgs); }
            autoScroll();
        });
        clearAndInit();
    }

    @Override public boolean shouldPause(){ return false; }
    @Override public boolean shouldCloseOnEsc(){ return false; }
}
