package org.sawiq.keybindprofiles.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;

import java.lang.reflect.Field;

final class KeybindsScreenNavigation {
    private KeybindsScreenNavigation() {
    }

    static void refreshControlsList(KeyBindsScreen keybindsScreen) {
        try {
            Field keyBindsListField = KeyBindsScreen.class.getDeclaredField("keyBindsList");
            keyBindsListField.setAccessible(true);
            Object keyBindsList = keyBindsListField.get(keybindsScreen);
            if (keyBindsList != null) {
                keyBindsList.getClass().getMethod("resetMappingAndUpdateButtons").invoke(keyBindsList);
                keyBindsList.getClass().getMethod("refreshEntries").invoke(keyBindsList);
            }
        } catch (ReflectiveOperationException e) {
            reinitialize(keybindsScreen);
        }
    }

    static Screen createFreshKeybindsScreen(KeyBindsScreen originalKeybindsScreen) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.options == null) {
            return null;
        }

        try {
            Field parentField = Screen.class.getDeclaredField("parent");
            parentField.setAccessible(true);
            Screen originalParent = (Screen) parentField.get(originalKeybindsScreen);
            return new KeyBindsScreen(originalParent, client.options);
        } catch (ReflectiveOperationException e) {
            return new KeyBindsScreen(null, client.options);
        }
    }

    private static void reinitialize(KeyBindsScreen keybindsScreen) {
        keybindsScreen.init(keybindsScreen.width, keybindsScreen.height);
    }
}
