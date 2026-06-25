package com.abhaythemaster.discordmc;

import com.abhaythemaster.discordmc.discord.DiscordClient;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscordMC implements ClientModInitializer {
    public static final String MOD_ID = "discordmc";
    public static final Logger LOGGER = LoggerFactory.getLogger("Discord MC");
    public static DiscordClient discord = new DiscordClient();

    @Override
    public void onInitializeClient() {
        LOGGER.info("[DiscordMC] Loaded! Open Discord from title screen.");
    }
}
