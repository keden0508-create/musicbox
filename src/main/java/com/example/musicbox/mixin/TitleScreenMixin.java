package com.example.musicbox.mixin;

import com.example.musicbox.MusicScreen;
import com.example.musicbox.TrackLibrary;
import com.example.musicbox.Util2;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void musicbox$addButton(CallbackInfo ci) {
        this.addRenderableWidget(Button.builder(Component.literal("♪ 음악"), b -> {
            TrackLibrary.refresh();
            Util2.openScreen(new MusicScreen(this));
        }).bounds(6, 6, 56, 20).build());
    }
}
