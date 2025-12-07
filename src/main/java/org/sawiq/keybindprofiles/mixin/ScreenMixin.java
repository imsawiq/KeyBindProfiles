package org.sawiq.keybindprofiles.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.sawiq.keybindprofiles.KeyBindProfiles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Unique
    private boolean buttonAdded = false;

    @Shadow
    protected abstract <T extends Element & Selectable> T addDrawableChild(T drawableElement);

    @Inject(method = "init(Lnet/minecraft/client/MinecraftClient;II)V", at = @At("TAIL"), require = 0)
    private void init(MinecraftClient client, int width, int height, CallbackInfo ci) {
        if (client == null || buttonAdded) {
            return;
        }

        Screen thisScreen = (Screen) (Object) this;
        if (thisScreen instanceof KeybindsScreen) {
            int buttonX = width / 2 - 155 - 8 - 150;
            int buttonY = height - 26;

            ButtonWidget profileButton = ButtonWidget.builder(Text.translatable("keybindprofiles.open"), button ->
                    KeyBindProfiles.openConfigScreen(thisScreen)
            ).dimensions(buttonX, buttonY, 150, 20).build();

            addDrawableChild(profileButton);
            buttonAdded = true;
        }
    }
}
