package org.sawiq.keybindprofiles.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.sawiq.keybindprofiles.KeyBindProfiles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeybindsScreen.class)
public abstract class KeybindsScreenRenderMixin {

    @Unique
    private boolean keybindprofiles$buttonAdded = false;

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("HEAD"), require = 0)
    private void keybindprofiles$render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (keybindprofiles$buttonAdded) return;

        Screen thisScreen = (Screen) (Object) this;

        int buttonX = thisScreen.width / 2 - 155 - 8 - 150;
        int buttonY = thisScreen.height - 26;

        ButtonWidget profileButton = ButtonWidget.builder(
                Text.translatable("keybindprofiles.open"),
                button -> KeyBindProfiles.openConfigScreen(thisScreen)
        ).dimensions(buttonX, buttonY, 150, 20).build();

        ((ScreenInvoker) thisScreen).keybindprofiles$invokeAddDrawableChild(profileButton);
        keybindprofiles$buttonAdded = true;
    }
}
