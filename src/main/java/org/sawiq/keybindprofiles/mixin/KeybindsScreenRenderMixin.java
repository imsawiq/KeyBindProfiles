package org.sawiq.keybindprofiles.mixin;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import org.sawiq.keybindprofiles.KeyBindProfiles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyBindsScreen.class)
public abstract class KeybindsScreenRenderMixin extends Screen {
    protected KeybindsScreenRenderMixin(Component title) {
        super(title);
    }

    @Inject(method = "addFooter", at = @At("TAIL"))
    private void keybindprofiles$addFooterButton(CallbackInfo ci) {
        Screen thisScreen = (Screen) (Object) this;
        int buttonX = thisScreen.width / 2 - 155 - 8 - 150;
        int buttonY = thisScreen.height - 27;

        addRenderableWidget(Button.builder(
                Component.translatable("keybindprofiles.open"),
                button -> KeyBindProfiles.openConfigScreen(thisScreen)
        ).bounds(buttonX, buttonY, 150, 20).build());
    }
}
