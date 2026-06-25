package com.abhaythemaster.discordmc.mixin;

import com.abhaythemaster.discordmc.gui.DiscordMainScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    @Inject(at = @At("RETURN"), method = "init")
    private void addDiscordButton(CallbackInfo ci) {
        TitleScreen screen = (TitleScreen)(Object)this;
        screen.addDrawableChild(ButtonWidget.builder(
            Text.literal("§5⚡ Discord"),
            btn -> screen.client.setScreen(new DiscordMainScreen(screen)))
            .dimensions(screen.width / 2 - 100, screen.height / 4 + 120, 200, 20)
            .build());
    }
}
