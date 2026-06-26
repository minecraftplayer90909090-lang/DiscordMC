package com.abhaythemaster.discordmc.gui;

import com.abhaythemaster.discordmc.DiscordMC;
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

    // Discord colors
    private static final int BG          = 0xFF202225;
    private static final int SIDEBAR     = 0xFF2F3136;
    private static final int HOVER       = 0xFF36393F;
    private static final int ACCENT      = 0xFF5865F2;
    private static final int GREEN       = 0xFF3BA55D;
    private static final int TEXT        = 0xFFDCDDDE;
    private static final int MUTED       = 0xFF8E9297;
    private static final int WHITE       = 0xFFFFFFFF;
    private static final int RED         = 0xFFED4245;

    // State
    private enum Tab { HOME, DMS, SERVERS, LOGIN }
    private Tab currentTab = Tab.HOME;
    private List<JsonObject> dms = new ArrayList<>();
    private List<JsonObject> servers = new ArrayList<>();
    private boolean loading = false;
    private int listScroll = 0;
    private int hoveredItem = -1;

    // Login state
    private TextFieldWidget tokenField;
    private String loginStatus = "";
    private int loginStatusColor = MUTED;

    // Chat state
    private String openChannelId = null;
    private String openChannelName = null;
    private List<JsonObject> messages = new ArrayList<>();
    private TextFieldWidget chatInput;
    private int chatScroll = 0;
    private boolean inChat = false;

    // Layout constants
    private static final int GUILDBAR_W = 48;
    private static final int SIDEBAR_W  = 200;
    private static final int TOPBAR_H   = 48;

    public DiscordMainScreen(Screen parent) {
        super(Text.literal("Discord"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (!DiscordMC.discord.isLoggedIn()) {
            currentTab = Tab.LOGIN;
            tokenField = new TextFieldWidget(textRenderer,
                width / 2 - 150, height / 2, 300, 20, Text.literal(""));
            tokenField.setPlaceholder(Text.literal("§8Token paste karo..."));
            tokenField.setMaxLength(200);
            addDrawableChild(tokenField);
            setInitialFocus(tokenField);
        } else {
            // Load DMs on init
            if (currentTab == Tab.DMS || currentTab == Tab.HOME) {
                loadDMs();
            }
            if (inChat) {
                chatInput = new TextFieldWidget(textRenderer,
                    GUILDBAR_W + SIDEBAR_W + 8, height - 28,
                    width - GUILDBAR_W - SIDEBAR_W - 50, 18, Text.literal(""));
                chatInput.setPlaceholder(Text.literal("§8Message #" + openChannelName));
                chatInput.setMaxLength(2000);
                addDrawableChild(chatInput);
                setInitialFocus(chatInput);
            }
        }
    }

    private void loadDMs() {
        loading = true;
        dms.clear();
        DiscordMC.discord.fetchDMs().thenAccept(list -> {
            dms = list;
            loading = false;
        });
    }

    private void loadServers() {
        loading = true;
        servers.clear();
        DiscordMC.discord.fetchGuilds().thenAccept(list -> {
            servers = list;
            loading = false;
        });
    }

    private void openChat(String channelId, String channelName) {
        openChannelId = channelId;
        openChannelName = channelName;
        inChat = true;
        messages.clear();
        chatScroll = 0;
        DiscordMC.discord.fetchMessages(channelId).thenAccept(msgs -> {
            messages = msgs;
            chatScroll = Math.max(0, messages.size() * 20 - (height - TOPBAR_H - 40));
        });
        DiscordMC.discord.onMessage(msg -> {
            if (openChannelId != null && msg.get("channel_id").getAsString().equals(openChannelId)) {
                messages.add(msg);
                chatScroll = Math.max(0, messages.size() * 20 - (height - TOPBAR_H - 40));
            }
        });
        clearAndInit();
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Full background
        ctx.fill(0, 0, width, height, BG);

        if (!DiscordMC.discord.isLoggedIn()) {
            renderLoginScreen(ctx, mx, my);
            super.render(ctx, mx, my, delta);
            return;
        }

        // ── Guild/Server icon bar (left column) ──────────────────────────────
        renderGuildBar(ctx, mx, my);

        // ── Sidebar ───────────────────────────────────────────────────────────
        renderSidebar(ctx, mx, my);

        // ── Main content area ─────────────────────────────────────────────────
        int contentX = GUILDBAR_W + SIDEBAR_W;
        int contentW = width - contentX;

        // Top bar
        ctx.fill(contentX, 0, width, TOPBAR_H, SIDEBAR);
        ctx.fill(contentX, TOPBAR_H - 1, width, TOPBAR_H, 0xFF1E2124);

        if (inChat && openChannelName != null) {
            // Channel header
            ctx.fill(contentX + 8, 16, contentX + 12, 32, ACCENT);
            ctx.drawTextWithShadow(textRenderer,
                Text.literal("§f# " + openChannelName), contentX + 20, 19, TEXT);
            // Back button area
            ctx.fill(contentX + 2, 12, contentX + 8, 36, 0);
            renderChatArea(ctx, contentX, contentW, mx, my);
        } else {
            ctx.drawTextWithShadow(textRenderer,
                Text.literal("§f" + getTabTitle()), contentX + 16, 18, TEXT);
            renderContentArea(ctx, contentX, contentW, mx, my);
        }

        super.render(ctx, mx, my, delta);
    }

    private void renderLoginScreen(DrawContext ctx, int mx, int my) {
        // Center panel
        int pw = 340, ph = 200;
        int px = width / 2 - pw / 2, py = height / 2 - ph / 2;

        ctx.fill(px, py, px + pw, py + ph, SIDEBAR);
        ctx.fill(px, py, px + pw, py + 3, ACCENT);

        // Discord logo box
        ctx.fill(width / 2 - 20, py + 16, width / 2 + 20, py + 56, ACCENT);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§fD"), width / 2, py + 30, WHITE);

        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§f§lWelcome back!"), width / 2, py + 64, WHITE);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§7Login with your Discord token"), width / 2, py + 76, MUTED);

        // Token field label
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§8TOKEN"), px + 20, py + 96, MUTED);

        // Token field bg
        ctx.fill(px + 16, py + 108, px + pw - 16, py + 132, BG);

        if (!loginStatus.isEmpty())
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(loginStatus), width / 2, py + 140, loginStatusColor);

        // Login button
        boolean hovBtn = mx >= width / 2 - 60 && mx <= width / 2 + 60
                      && my >= py + 152 && my <= py + 172;
        ctx.fill(width / 2 - 60, py + 152, width / 2 + 60, py + 172,
            hovBtn ? 0xFF4752C4 : ACCENT);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§fLogin"), width / 2, py + 158, WHITE);

        // Back
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§7← Back to Minecraft"), width / 2, py + ph - 12, MUTED);
    }

    private void renderGuildBar(DrawContext ctx, int mx, int my) {
        ctx.fill(0, 0, GUILDBAR_W, height, 0xFF1E2124);

        // Home button
        boolean homeHov = mx >= 8 && mx <= 40 && my >= 8 && my <= 40;
        ctx.fill(8, 8, 40, 40, homeHov || currentTab == Tab.HOME || currentTab == Tab.DMS ? ACCENT : SIDEBAR);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§f⌂"), 24, 18, WHITE);

        // Separator
        ctx.fill(12, 46, 36, 48, MUTED);

        // Server icons
        if (!servers.isEmpty()) {
            int sy = 54;
            for (int i = 0; i < Math.min(servers.size(), 8); i++) {
                JsonObject s = servers.get(i);
                boolean hov = mx >= 8 && mx <= 40 && my >= sy && my <= sy + 32;
                ctx.fill(8, sy, 40, sy + 32, hov ? ACCENT : HOVER);
                String name = s.get("name").getAsString();
                String abbr = name.replaceAll("[^A-Za-z0-9]", "").substring(0, Math.min(2, name.replaceAll("[^A-Za-z0-9]", "").length())).toUpperCase();
                if (abbr.isEmpty()) abbr = "S";
                ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§f" + abbr), 24, sy + 10, WHITE);
                sy += 36;
            }
        }
    }

    private void renderSidebar(DrawContext ctx, int mx, int my) {
        ctx.fill(GUILDBAR_W, 0, GUILDBAR_W + SIDEBAR_W, height, SIDEBAR);

        // User avatar + name at bottom
        ctx.fill(GUILDBAR_W, height - 52, GUILDBAR_W + SIDEBAR_W, height, 0xFF292B2F);
        if (DiscordMC.discord.getSelfUser() != null) {
            String uname = DiscordMC.discord.getSelfUser().get("username").getAsString();
            ctx.fill(GUILDBAR_W + 8, height - 44, GUILDBAR_W + 40, height - 12, ACCENT);
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§f" + uname.substring(0, Math.min(2, uname.length())).toUpperCase()),
                GUILDBAR_W + 24, height - 34, WHITE);
            ctx.drawTextWithShadow(textRenderer,
                Text.literal("§f" + uname), GUILDBAR_W + 48, height - 38, TEXT);
            ctx.drawTextWithShadow(textRenderer,
                Text.literal("§a● Online"), GUILDBAR_W + 48, height - 26, GREEN);
        }

        // Tab headers
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§8DIRECT MESSAGES"),
            GUILDBAR_W + 12, 16, MUTED);

        // DM list
        if (loading) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7Loading..."), GUILDBAR_W + SIDEBAR_W / 2, 80, MUTED);
        } else {
            int y = 36 - listScroll;
            for (int i = 0; i < dms.size(); i++) {
                JsonObject dm = dms.get(i);
                if (y > 28 && y < height - 56) {
                    String name = getDMName(dm);
                    boolean hov = mx >= GUILDBAR_W + 4 && mx <= GUILDBAR_W + SIDEBAR_W - 4
                               && my >= y && my <= y + 34;
                    boolean active = inChat && openChannelId != null
                                  && openChannelId.equals(dm.get("id").getAsString());

                    ctx.fill(GUILDBAR_W + 4, y, GUILDBAR_W + SIDEBAR_W - 4, y + 32,
                        active ? HOVER : (hov ? 0xFF34373C : 0));

                    // Avatar circle
                    ctx.fill(GUILDBAR_W + 10, y + 6, GUILDBAR_W + 28, y + 26, ACCENT);
                    ctx.drawCenteredTextWithShadow(textRenderer,
                        Text.literal("§f" + name.substring(0, Math.min(1, name.length())).toUpperCase()),
                        GUILDBAR_W + 19, y + 11, WHITE);

                    ctx.drawTextWithShadow(textRenderer,
                        Text.literal("§f" + (name.length() > 18 ? name.substring(0, 18) + ".." : name)),
                        GUILDBAR_W + 34, y + 11, hov || active ? TEXT : MUTED);
                }
                y += 36;
            }
        }
    }

    private void renderContentArea(DrawContext ctx, int contentX, int contentW, int mx, int my) {
        ctx.fill(contentX, TOPBAR_H, width, height, BG);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§7Select a DM from the left"), contentX + contentW / 2, height / 2, MUTED);
    }

    private void renderChatArea(DrawContext ctx, int contentX, int contentW, int mx, int my) {
        ctx.fill(contentX, TOPBAR_H, width, height, BG);

        // Messages
        int msgY = TOPBAR_H + 8 - chatScroll;
        for (JsonObject msg : messages) {
            if (msgY > TOPBAR_H && msgY < height - 40) {
                String author = "Unknown";
                if (msg.has("author"))
                    author = msg.getAsJsonObject("author").get("username").getAsString();
                String content = msg.has("content") ? msg.get("content").getAsString() : "";

                // Avatar
                ctx.fill(contentX + 8, msgY, contentX + 24, msgY + 16, ACCENT);
                ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("§f" + author.substring(0, Math.min(1, author.length())).toUpperCase()),
                    contentX + 16, msgY + 3, WHITE);

                // Username
                ctx.drawTextWithShadow(textRenderer,
                    Text.literal("§f§l" + author), contentX + 30, msgY, TEXT);

                // Content (wrap long text)
                if (content.length() > 70) {
                    ctx.drawTextWithShadow(textRenderer,
                        Text.literal("§7" + content.substring(0, 70)), contentX + 30, msgY + 10, TEXT);
                    ctx.drawTextWithShadow(textRenderer,
                        Text.literal("§7" + content.substring(70, Math.min(140, content.length()))),
                        contentX + 30, msgY + 20, TEXT);
                } else {
                    ctx.drawTextWithShadow(textRenderer,
                        Text.literal("§7" + content), contentX + 30, msgY + 10, TEXT);
                }
            }
            msgY += 32;
        }

        // Chat input bar
        ctx.fill(contentX, height - 40, width, height, SIDEBAR);
        ctx.fill(contentX + 8, height - 32, width - 8, height - 8, 0xFF40444B);
    }

    private String getDMName(JsonObject dm) {
        if (dm.has("recipients") && dm.getAsJsonArray("recipients").size() > 0)
            return dm.getAsJsonArray("recipients").get(0).getAsJsonObject().get("username").getAsString();
        return "Unknown";
    }

    private String getTabTitle() {
        return switch (currentTab) {
            case DMS -> "Direct Messages";
            case SERVERS -> "Servers";
            default -> "Discord";
        };
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (!DiscordMC.discord.isLoggedIn()) {
            // Login button
            int py = height / 2 - 100;
            if (mx >= width / 2 - 60 && mx <= width / 2 + 60 && my >= py + 152 && my <= py + 172) {
                String t = tokenField != null ? tokenField.getText().trim() : "";
                if (!t.isEmpty()) {
                    loginStatus = "§7Connecting...";
                    loginStatusColor = MUTED;
                    DiscordMC.discord.loginWithToken(t).thenAccept(ok -> {
                        if (ok) {
                            client.execute(() -> {
                                currentTab = Tab.HOME;
                                inChat = false;
                                clearAndInit();
                                loadDMs();
                                loadServers();
                            });
                        } else {
                            loginStatus = "§cInvalid token!";
                            loginStatusColor = RED;
                        }
                    });
                }
                return true;
            }
            // Back
            if (my >= height / 2 + 86 && my <= height / 2 + 100) {
                client.setScreen(parent); return true;
            }
            return super.mouseClicked(mx, my, btn);
        }

        // Home button
        if (mx >= 8 && mx <= 40 && my >= 8 && my <= 40) {
            currentTab = Tab.HOME;
            inChat = false;
            clearAndInit();
            loadDMs();
            return true;
        }

        // Server icons in guild bar
        if (!servers.isEmpty() && mx >= 8 && mx <= 40) {
            int sy = 54;
            for (JsonObject s : servers) {
                if (my >= sy && my <= sy + 32) {
                    currentTab = Tab.SERVERS;
                    inChat = false;
                    clearAndInit();
                    return true;
                }
                sy += 36;
            }
        }

        // DM list clicks
        if (mx >= GUILDBAR_W + 4 && mx <= GUILDBAR_W + SIDEBAR_W - 4) {
            int y = 36 - listScroll;
            for (JsonObject dm : dms) {
                if (my >= y && my <= y + 32) {
                    openChat(dm.get("id").getAsString(), getDMName(dm));
                    return true;
                }
                y += 36;
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        // Enter to send message
        if ((key == 257 || key == 335) && inChat && chatInput != null) {
            String txt = chatInput.getText().trim();
            if (!txt.isEmpty()) {
                DiscordMC.discord.sendMessage(openChannelId, txt);
                chatInput.setText("");
            }
            return true;
        }
        // Escape
        if (key == 256) {
            if (inChat) {
                inChat = false;
                openChannelId = null;
                clearAndInit();
            } else {
                client.setScreen(parent);
            }
            return true;
        }
        return super.keyPressed(key, scan, mod);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (mx < GUILDBAR_W + SIDEBAR_W) {
            listScroll = Math.max(0, listScroll - (int)(v * 8));
        } else if (inChat) {
            chatScroll = Math.max(0, chatScroll - (int)(v * 8));
        }
        return true;
    }

    @Override
    public boolean shouldPause() { return false; }
}
