package org.sawiq.keybindprofiles;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.sawiq.keybindprofiles.gui.ControlsScreenProfileButton;
import org.sawiq.keybindprofiles.gui.KeyBindProfileScreen;
import org.sawiq.keybindprofiles.input.ProfileHotkeyController;
import org.sawiq.keybindprofiles.notification.ProfileNotification;
import org.sawiq.keybindprofiles.profile.ProfileService;
import org.sawiq.keybindprofiles.server.ServerAutoSwitchController;
import org.sawiq.keybindprofiles.storage.ProfileFileStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class KeyBindProfiles implements ClientModInitializer {
    public static final String MOD_ID = "keybindprofiles";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final ProfileNotification NOTIFICATION = new ProfileNotification();
    private static final ProfileService PROFILE_SERVICE = new ProfileService(new ProfileFileStore());
    private static final ProfileHotkeyController HOTKEY_CONTROLLER = new ProfileHotkeyController(PROFILE_SERVICE, NOTIFICATION);
    private static final ServerAutoSwitchController AUTO_SWITCH_CONTROLLER = new ServerAutoSwitchController(PROFILE_SERVICE, NOTIFICATION);

    public static final Map<String, Map<String, String>> PROFILES = PROFILE_SERVICE.profiles();
    public static final Map<String, List<String>> PROFILE_HOTKEYS = PROFILE_SERVICE.profileHotkeys();
    public static final Map<String, List<String>> PROFILE_AUTO_SWITCH_SERVERS = PROFILE_SERVICE.profileAutoSwitchServers();

    private static KeyBinding openProfileScreenKey;

    @Override
    public void onInitializeClient() {
        PROFILE_SERVICE.setAutoSwitchResetCallback(AUTO_SWITCH_CONTROLLER::reset);
        registerOpenScreenKey();
        registerClientEvents();
        registerConnectionEvents();
        registerControlsScreenButton();
        loadProfilesOnClientStart();
    }

    public static void openConfigScreen(Screen parent) {
        MinecraftClient.getInstance().setScreen(new KeyBindProfileScreen(parent));
    }

    public static void reloadProfilesFromDirectory() {
        PROFILE_SERVICE.reloadProfiles();
    }

    public static void saveProfile(String name, KeyBinding[] bindings) {
        PROFILE_SERVICE.saveProfile(name, bindings);
    }

    public static void applyProfile(String name) {
        PROFILE_SERVICE.applyProfile(name);
    }

    public static void deleteProfile(String name) {
        PROFILE_SERVICE.deleteProfile(name);
    }

    public static void exportProfile(String name) {
        PROFILE_SERVICE.exportProfile(name);
    }

    public static void setProfileHotkey(String profileName, List<String> keys) {
        PROFILE_SERVICE.setProfileHotkey(profileName, keys);
    }

    public static List<String> getProfileHotkey(String profileName) {
        return PROFILE_SERVICE.getProfileHotkey(profileName);
    }

    public static void setProfileAutoSwitchServers(String profileName, List<String> servers) {
        PROFILE_SERVICE.setProfileAutoSwitchServers(profileName, servers);
    }

    public static List<String> getProfileAutoSwitchServers(String profileName) {
        return PROFILE_SERVICE.getProfileAutoSwitchServers(profileName);
    }

    public static void loadProfiles() {
        PROFILE_SERVICE.loadProfiles();
    }

    public static void saveCurrentProfile(String profile) {
        PROFILE_SERVICE.saveCurrentProfile(profile);
    }

    public static String getCurrentProfile() {
        return PROFILE_SERVICE.getCurrentProfile();
    }

    public static boolean openProfilesFolder() {
        return PROFILE_SERVICE.openProfilesFolder();
    }

    public static void showNotification(String profileName) {
        NOTIFICATION.show(profileName);
    }

    public static String getNotificationText() {
        return NOTIFICATION.getVisibleProfileName();
    }

    private static void registerOpenScreenKey() {
        openProfileScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.keybindprofiles.open",
                InputUtil.Type.KEYSYM,
                InputUtil.GLFW_KEY_O,
                "key.categories.misc"
        ));
    }

    private static void registerClientEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            openProfileScreenWhenKeyPressed();
            HOTKEY_CONTROLLER.tick(client);
            AUTO_SWITCH_CONTROLLER.tick(client);
        });
    }

    private static void registerConnectionEvents() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            AUTO_SWITCH_CONTROLLER.reset();
            AUTO_SWITCH_CONTROLLER.tick(client);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            AUTO_SWITCH_CONTROLLER.reset();
        });
    }

    private static void registerControlsScreenButton() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof KeybindsScreen) {
                ControlsScreenProfileButton.addOrReplace(screen, scaledWidth, scaledHeight);
            }
        });
    }

    private static void loadProfilesOnClientStart() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            PROFILE_SERVICE.loadProfiles();
            PROFILE_SERVICE.loadCurrentProfile();
        });
    }

    private static void openProfileScreenWhenKeyPressed() {
        while (openProfileScreenKey.wasPressed()) {
            openConfigScreen(null);
        }
    }
}
