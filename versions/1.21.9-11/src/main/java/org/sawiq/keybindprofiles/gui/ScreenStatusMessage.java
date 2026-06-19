package org.sawiq.keybindprofiles.gui;

import net.minecraft.text.Text;

final class ScreenStatusMessage {
    private static final long DURATION_MS = 3000;

    private String translationKey;
    private Object[] args = new Object[0];
    private long expiresAt;

    void show(String translationKey, Object... args) {
        this.translationKey = translationKey;
        this.args = args == null ? new Object[0] : args;
        this.expiresAt = System.currentTimeMillis() + DURATION_MS;
    }

    Text getVisibleText() {
        if (translationKey == null || System.currentTimeMillis() >= expiresAt) {
            return null;
        }
        return Text.translatable(translationKey, args);
    }
}
