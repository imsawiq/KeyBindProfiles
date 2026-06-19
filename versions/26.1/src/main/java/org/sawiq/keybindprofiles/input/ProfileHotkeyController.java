package org.sawiq.keybindprofiles.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
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

    public void tick(Minecraft client) {
        if (client == null || client.player == null) {
            return;
        }

        for (Map.Entry<String, List<String>> entry : profileService.profileHotkeys().entrySet()) {
            checkProfileHotkey(entry.getKey(), entry.getValue(), client);
        }
    }

    private void checkProfileHotkey(String profileName, List<String> keys, Minecraft client) {
        if (keys == null || keys.isEmpty()) {
            return;
        }

        String hotkeyId = profileName + "_" + keys;
        if (areAllKeysPressed(keys, client)) {
            applyProfile(profileName, hotkeyId);
        } else {
            pressedHotkeys.remove(hotkeyId);
        }
    }

    private boolean areAllKeysPressed(List<String> keys, Minecraft client) {
        for (String translationKey : keys) {
            if (!isHotkeyPressed(client, translationKey)) {
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

    private boolean isHotkeyPressed(Minecraft client, String translationKey) {
        if (translationKey == null || translationKey.isEmpty()) {
            return false;
        }

        InputConstants.Key key = parseInputKey(translationKey);
        if (key == null) {
            return false;
        }

        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(client.getWindow().handle(), key.getValue()) == GLFW.GLFW_PRESS;
        }
        return InputConstants.isKeyDown(client.getWindow(), key.getValue());
    }

    private InputConstants.Key parseInputKey(String translationKey) {
        try {
            return InputConstants.getKey(translationKey);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
