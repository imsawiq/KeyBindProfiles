package org.sawiq.keybindprofiles.mixin;

import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenInvoker {

    @Invoker("addDrawableChild")
    <T extends Element & Selectable> T keybindprofiles$invokeAddDrawableChild(T drawableElement);
}
