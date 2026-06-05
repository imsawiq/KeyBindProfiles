package org.sawiq.keybindprofiles.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class KeybindsScreenNavigation {
    private KeybindsScreenNavigation() {
    }

    static void refreshControlsList(KeybindsScreen keybindsScreen) {
        try {
            Field controlsListField = KeybindsScreen.class.getDeclaredField("controlsList");
            controlsListField.setAccessible(true);
            Object controlsList = controlsListField.get(keybindsScreen);
            if (controlsList != null) {
                Method updateMethod = controlsList.getClass().getMethod("update");
                updateMethod.invoke(controlsList);
            }
        } catch (ReflectiveOperationException e) {
            reinitialize(keybindsScreen);
        }
    }

    static Screen createFreshKeybindsScreen(KeybindsScreen originalKeybindsScreen) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) {
            return null;
        }

        try {
            Field parentField = Screen.class.getDeclaredField("parent");
            parentField.setAccessible(true);
            Screen originalParent = (Screen) parentField.get(originalKeybindsScreen);
            return new KeybindsScreen(originalParent, client.options);
        } catch (ReflectiveOperationException e) {
            return new KeybindsScreen(null, client.options);
        }
    }

    private static void reinitialize(KeybindsScreen keybindsScreen) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            keybindsScreen.init(client, keybindsScreen.width, keybindsScreen.height);
        }
    }
}
