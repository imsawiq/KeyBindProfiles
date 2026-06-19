package org.sawiq.keybindprofiles.input;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.sawiq.keybindprofiles.notification.ProfileNotification;
import org.sawiq.keybindprofiles.profile.ProfileService;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ProfileHotkeyController {
    private final ProfileService profileService;
    private final ProfileNotification notification;
    private final Set<String> pressedHotkeys = new HashSet<>();

    public ProfileHotkeyController(ProfileService profileService, ProfileNotification notification) {
        this.profileService = profileService;
        this.notification = notification;
    }

    public void tick(MinecraftClient client) {
        if (client == null || client.player == null) {
            return;
        }

        long windowHandle = client.getWindow().getHandle();
        for (Map.Entry<String, List<String>> entry : profileService.profileHotkeys().entrySet()) {
            checkProfileHotkey(entry.getKey(), entry.getValue(), windowHandle);
        }
    }

    private void checkProfileHotkey(String profileName, List<String> keys, long windowHandle) {
        if (keys == null || keys.isEmpty()) {
            return;
        }

        String hotkeyId = profileName + "_" + keys;
        if (areAllKeysPressed(keys, windowHandle)) {
            applyProfile(profileName, hotkeyId);
        } else {
            pressedHotkeys.remove(hotkeyId);
        }
    }

    private boolean areAllKeysPressed(List<String> keys, long windowHandle) {
        for (String translationKey : keys) {
            if (!isHotkeyPressed(windowHandle, translationKey)) {
                return false;
            }
        }
        return true;
    }

    private void applyProfile(String profileName, String hotkeyId) {
        if (pressedHotkeys.contains(hotkeyId) || profileName.equals(profileService.getCurrentProfile())) {
            return;
        }

        profileService.applyProfile(profileName);
        notification.show(profileName);
        pressedHotkeys.add(hotkeyId);
    }

    private boolean isHotkeyPressed(long windowHandle, String translationKey) {
        if (translationKey == null || translationKey.isEmpty()) {
            return false;
        }

        InputUtil.Key key = parseInputKey(translationKey);
        if (key == null) {
            return false;
        }

        if (key.getCategory() == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(windowHandle, key.getCode()) == GLFW.GLFW_PRESS;
        }
        return InputUtil.isKeyPressed(windowHandle, key.getCode());
    }

    private InputUtil.Key parseInputKey(String translationKey) {
        try {
            return InputUtil.fromTranslationKey(translationKey);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
