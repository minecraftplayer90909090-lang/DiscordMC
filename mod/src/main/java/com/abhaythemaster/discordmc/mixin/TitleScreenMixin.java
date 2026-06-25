package com.abhaythemaster.discordmc.mixin;

import com.abhaythemaster.discordmc.gui.DiscordMainScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends net.minecraft.client.gui.screen.Screen {

    protected TitleScreenMixin() {
        super(Text.literal(""));
    }

    @Inject(at = @At("RETURN"), method = "init")
    private void addDiscordButton(CallbackInfo ci) {
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("§5⚡ Discord"),
            btn -> MinecraftClient.getInstance().setScreen(
                new DiscordMainScreen(MinecraftClient.getInstance().currentScreen)))
            .dimensions(this.width / 2 - 100, this.height / 4 + 120, 200, 20)
            .build());
    }
}
