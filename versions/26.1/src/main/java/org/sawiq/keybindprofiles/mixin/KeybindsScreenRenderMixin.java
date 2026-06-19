package org.sawiq.keybindprofiles.mixin;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import org.sawiq.keybindprofiles.KeyBindProfiles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(KeyBindsScreen.class)
public abstract class KeybindsScreenRenderMixin extends Screen {
    private static final int GAP = 8;
    private static final int DEFAULT_BUTTON_WIDTH = 150;
    private static final int MIN_BUTTON_WIDTH = 90;
    private static final int SCREEN_PADDING = 8;

    protected KeybindsScreenRenderMixin(Component title) {
        super(title);
    }

    @Redirect(
            method = "addFooter",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
                    ordinal = 0
            )
    )
    private LayoutElement keybindprofiles$addProfileButtonBeforeReset(LinearLayout footerButtons, LayoutElement resetButton) {
        Screen thisScreen = (Screen) (Object) this;
        int buttonWidth = calculateButtonWidth(thisScreen.width);

        footerButtons.addChild(Button.builder(
                Component.translatable("keybindprofiles.open"),
                button -> KeyBindProfiles.openConfigScreen(thisScreen)
        ).width(buttonWidth).build());

        resizeFooterButton(resetButton, buttonWidth);
        return footerButtons.addChild(resetButton);
    }

    @Redirect(
            method = "addFooter",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
                    ordinal = 1
            )
    )
    private LayoutElement keybindprofiles$resizeDoneButton(LinearLayout footerButtons, LayoutElement doneButton) {
        resizeFooterButton(doneButton, calculateButtonWidth(((Screen) (Object) this).width));
        return footerButtons.addChild(doneButton);
    }

    private int calculateButtonWidth(int screenWidth) {
        if (screenWidth >= 520) {
            return DEFAULT_BUTTON_WIDTH;
        }
        return Math.max(MIN_BUTTON_WIDTH, (screenWidth - SCREEN_PADDING * 4 - GAP * 2) / 3);
    }

    private void resizeFooterButton(LayoutElement element, int width) {
        if (element instanceof Button button) {
            button.setWidth(width);
        }
    }
}
