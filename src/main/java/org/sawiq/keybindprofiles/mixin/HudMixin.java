package org.sawiq.keybindprofiles.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.sawiq.keybindprofiles.KeyBindProfiles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class HudMixin {
    @Shadow
    private MinecraftClient client;

    @Inject(method = "render", at = @At("TAIL"))
    private void renderProfileNotification(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        String profileName = KeyBindProfiles.getNotificationText();
        if (profileName != null && client.player != null) {
            TextRenderer textRenderer = client.textRenderer;
            Text message = Text.literal("Профиль \"" + profileName + "\" применён");

            int screenWidth = context.getScaledWindowWidth();
            int screenHeight = context.getScaledWindowHeight();

            // над хотбаром
            int x = (screenWidth - textRenderer.getWidth(message)) / 2;
            int y = screenHeight - 59;

            // 0xFF55FF55 = ARGB формат (FF = альфа, 55FF55 = зелёный)
            context.drawTextWithShadow(textRenderer, message, x, y, 0xFF55FF55);
        }
    }
}
