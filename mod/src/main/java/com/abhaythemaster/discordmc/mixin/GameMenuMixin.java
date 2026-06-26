package com.abhaythemaster.discordmc.mixin;

import com.abhaythemaster.discordmc.gui.DiscordMainScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuMixin extends net.minecraft.client.gui.screen.Screen {
    protected GameMenuMixin() { super(Text.literal("")); }

    @Inject(at = @At("RETURN"), method = "initWidgets")
    private void addDiscordButton(CallbackInfo ci) {
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("§b⚡ Discord"),
            btn -> this.client.setScreen(new DiscordMainScreen(this)))
            .dimensions(this.width / 2 - 102, this.height / 4 + 168, 204, 20)
            .build());
    }
}
