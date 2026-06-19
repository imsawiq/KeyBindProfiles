package org.sawiq.keybindprofiles.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
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

    Component getButtonText(String profileName) {
        if (isCapturing(profileName)) {
            return capturedKeys.isEmpty() ? Component.literal("...") : Component.literal(formatKeys(capturedKeys));
        }

        List<String> keys = KeyBindProfiles.getProfileHotkey(profileName);
        return keys == null || keys.isEmpty() ? Component.literal("-") : Component.literal(formatKeys(keys));
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

    boolean handleKeyPressed(KeyEvent event) {
        if (!isCapturing()) {
            return false;
        }

        int keyCode = event.key();
        if (keyCode == InputConstants.KEY_ESCAPE) {
            clear();
            return true;
        }

        if (keyCode == InputConstants.KEY_BACKSPACE) {
            KeyBindProfiles.setProfileHotkey(profileName, null);
            clear();
            return true;
        }

        if (keyCode == InputConstants.KEY_RETURN) {
            saveCapturedKeys();
            clear();
            return true;
        }

        addCapturedKey(InputConstants.getKey(event).getName());
        return true;
    }

    boolean handleMouseClicked(int button) {
        if (!isCapturing()) {
            return false;
        }

        addCapturedKey(InputConstants.Type.MOUSE.getOrCreate(button).getName());
        return true;
    }

    private void addCapturedKey(String savedKey) {
        if (!capturedKeys.contains(savedKey) && capturedKeys.size() < MAX_KEYS) {
            capturedKeys.add(savedKey);
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
            text.append(InputConstants.getKey(keys.get(i)).getDisplayName().getString());
            if (i < keys.size() - 1) {
                text.append("+");
            }
        }
        return text.toString();
    }
}
