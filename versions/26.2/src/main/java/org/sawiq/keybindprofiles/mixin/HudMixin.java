package org.sawiq.keybindprofiles.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.network.chat.Component;
import org.sawiq.keybindprofiles.KeyBindProfiles;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class HudMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private GuiRenderState guiRenderState;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void keybindprofiles$renderProfileNotification(DeltaTracker deltaTracker, boolean renderHud, boolean renderScreen, CallbackInfo ci) {
        String profileName = KeyBindProfiles.getNotificationText();
        if (profileName == null || minecraft.player == null) {
            return;
        }

        GuiGraphicsExtractor extractor = new GuiGraphicsExtractor(minecraft, guiRenderState, 0, 0);
        Font font = minecraft.font;
        Component message = Component.literal("Профиль \"" + profileName + "\" применён");

        int screenWidth = extractor.guiWidth();
        int screenHeight = extractor.guiHeight();
        int x = (screenWidth - font.width(message)) / 2;
        int y = screenHeight - 59;

        extractor.text(font, message, x, y, 0xFF55FF55, true);
    }
}
