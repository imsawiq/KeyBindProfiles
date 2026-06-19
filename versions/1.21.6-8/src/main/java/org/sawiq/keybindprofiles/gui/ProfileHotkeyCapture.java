package org.sawiq.keybindprofiles.gui;

import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.sawiq.keybindprofiles.KeyBindProfiles;

import java.util.ArrayList;
import java.util.List;

final class ProfileHotkeyCapture {
    private static final int MAX_KEYS = 2;

    private String profileName;
    private final List<String> capturedKeys = new ArrayList<>();

    boolean isCapturing() {
        return profileName != null;
    }

    boolean isCapturing(String profileName) {
        return this.profileName != null && this.profileName.equals(profileName);
    }

    Text getButtonText(String profileName) {
        if (isCapturing(profileName)) {
            return capturedKeys.isEmpty() ? Text.literal("...") : Text.literal(formatKeys(capturedKeys));
        }

        List<String> keys = KeyBindProfiles.getProfileHotkey(profileName);
        return keys == null || keys.isEmpty() ? Text.literal("-") : Text.literal(formatKeys(keys));
    }

    void toggle(String profileName) {
        if (isCapturing(profileName)) {
            saveCapturedKeys();
            clear();
            return;
        }

        this.profileName = profileName;
        capturedKeys.clear();
    }

    boolean handleKeyPressed(int keyCode, int scanCode) {
        if (!isCapturing()) {
            return false;
        }

        if (keyCode == InputUtil.GLFW_KEY_ESCAPE) {
            clear();
            return true;
        }

        if (keyCode == InputUtil.GLFW_KEY_BACKSPACE) {
            KeyBindProfiles.setProfileHotkey(profileName, null);
            clear();
            return true;
        }

        if (keyCode == InputUtil.GLFW_KEY_ENTER) {
            saveCapturedKeys();
            clear();
            return true;
        }

        addCapturedKey(InputUtil.fromKeyCode(keyCode, scanCode).getTranslationKey());
        return true;
    }

    boolean handleMouseClicked(int button) {
        if (!isCapturing()) {
            return false;
        }

        addCapturedKey(InputUtil.Type.MOUSE.createFromCode(button).getTranslationKey());
        return true;
    }

    private void addCapturedKey(String translationKey) {
        if (!capturedKeys.contains(translationKey) && capturedKeys.size() < MAX_KEYS) {
            capturedKeys.add(translationKey);
        }
    }

    private void saveCapturedKeys() {
        if (!capturedKeys.isEmpty()) {
            KeyBindProfiles.setProfileHotkey(profileName, new ArrayList<>(capturedKeys));
        }
    }

    private void clear() {
        profileName = null;
        capturedKeys.clear();
    }

    private String formatKeys(List<String> keys) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            text.append(InputUtil.fromTranslationKey(keys.get(i)).getLocalizedText().getString());
            if (i < keys.size() - 1) {
                text.append("+");
            }
        }
        return text.toString();
    }
}
