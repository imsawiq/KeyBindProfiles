package org.sawiq.keybindprofiles;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.sawiq.keybindprofiles.gui.KeyBindProfileScreen;
import org.lwjgl.glfw.GLFW;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class KeyBindProfiles implements ClientModInitializer {
    public static final Map<String, Map<String, String>> PROFILES = new HashMap<>();
    public static final Map<String, List<String>> PROFILE_HOTKEYS = new HashMap<>();

    private static File profilesDir;
    private static File currentProfileFile;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static KeyMapping openProfileScreenKey;
    private static String currentProfile;

    private static String notificationText;
    private static long notificationTime;
    private static final long NOTIFICATION_DURATION = 3000L;

    private static final Set<String> pressedHotkeys = new HashSet<>();

    @Override
    public void onInitializeClient() {
        openProfileScreenKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.keybindprofiles.open",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_O,
                KeyMapping.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openProfileScreenKey.consumeClick()) {
                openConfigScreen(null);
            }
            checkProfileHotkeys();
        });

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            loadProfiles();
            currentProfile = loadCurrentProfile();
            if (currentProfile != null && PROFILES.containsKey(currentProfile)) {
                applyProfile(currentProfile);
            }
        });
    }

    private static void checkProfileHotkeys() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        for (Map.Entry<String, List<String>> entry : PROFILE_HOTKEYS.entrySet()) {
            String profileName = entry.getKey();
            List<String> keys = entry.getValue();
            if (keys == null || keys.isEmpty()) {
                continue;
            }

            boolean allPressed = true;
            for (String savedKey : keys) {
                if (!isHotkeyPressed(client, savedKey)) {
                    allPressed = false;
                    break;
                }
            }

            String hotkeyId = profileName + "_" + keys;
            if (allPressed) {
                if (!pressedHotkeys.contains(hotkeyId) && !profileName.equals(currentProfile)) {
                    applyProfile(profileName);
                    showNotification(profileName);
                    pressedHotkeys.add(hotkeyId);
                }
            } else {
                pressedHotkeys.remove(hotkeyId);
            }
        }
    }

    private static boolean isHotkeyPressed(Minecraft client, String savedKey) {
        if (savedKey == null || savedKey.isEmpty()) {
            return false;
        }

        InputConstants.Key key;
        try {
            key = InputConstants.getKey(savedKey);
        } catch (Exception e) {
            return false;
        }

        if (key == null) {
            return false;
        }

        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(client.getWindow().handle(), key.getValue()) == GLFW.GLFW_PRESS;
        }

        return InputConstants.isKeyDown(client.getWindow(), key.getValue());
    }

    public static void showNotification(String profileName) {
        notificationText = profileName;
        notificationTime = System.currentTimeMillis();
    }

    public static String getNotificationText() {
        if (notificationText != null && System.currentTimeMillis() - notificationTime < NOTIFICATION_DURATION) {
            return notificationText;
        }
        return null;
    }

    public static void openConfigScreen(Screen parent) {
        Minecraft.getInstance().setScreen(new KeyBindProfileScreen(parent));
    }

    private static File getProfilesDir() {
        if (profilesDir == null) {
            Minecraft client = Minecraft.getInstance();
            if (client.gameDirectory != null) {
                profilesDir = new File(client.gameDirectory, "config/keybindprofiles");
                if (!profilesDir.exists() && !profilesDir.mkdirs()) {
                    System.err.println("Failed to create profiles directory");
                }
            }
        }
        return profilesDir;
    }

    private static File getCurrentProfileFile() {
        if (currentProfileFile == null) {
            File dir = getProfilesDir();
            if (dir != null) {
                currentProfileFile = new File(dir, "current_profile.txt");
            }
        }
        return currentProfileFile;
    }

    private static void loadProfilesFromDirectory() {
        File dir = getProfilesDir();
        if (dir == null || !dir.exists()) {
            return;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".kbp"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, Object>>() { }.getType();
                Map<String, Object> data = GSON.fromJson(reader, type);

                if (data == null || !data.containsKey("name") || !data.containsKey("keybindings")) {
                    continue;
                }

                String name = (String) data.get("name");
                @SuppressWarnings("unchecked")
                Map<String, String> bindings = (Map<String, String>) data.get("keybindings");
                PROFILES.put(name, bindings);

                if (data.containsKey("hotkeys") && data.get("hotkeys") instanceof List<?> hotkeys) {
                    List<String> hotkeyKeys = new ArrayList<>();
                    for (Object value : hotkeys) {
                        if (value instanceof String keyName) {
                            hotkeyKeys.add(keyName);
                        } else if (value instanceof Number legacyKeyCode) {
                            hotkeyKeys.add(InputConstants.Type.KEYSYM.getOrCreate(legacyKeyCode.intValue()).getName());
                        }
                    }
                    if (!hotkeyKeys.isEmpty()) {
                        PROFILE_HOTKEYS.put(name, hotkeyKeys);
                    }
                }
            } catch (IOException e) {
                System.err.println("Failed to load profile from " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    public static void reloadProfilesFromDirectory() {
        PROFILES.clear();
        PROFILE_HOTKEYS.clear();
        loadProfilesFromDirectory();
    }

    public static void saveProfile(String name, KeyMapping[] bindings) {
        Objects.requireNonNull(name, "Profile name cannot be null");
        Objects.requireNonNull(bindings, "Bindings cannot be null");

        Map<String, String> keyMap = new HashMap<>();
        for (KeyMapping binding : bindings) {
            if (binding != null) {
                keyMap.put(binding.getName(), binding.saveString());
            }
        }

        PROFILES.put(name, keyMap);
        exportProfile(name);
    }

    public static void applyProfile(String name) {
        Map<String, String> keyMap = PROFILES.get(name);
        if (keyMap == null) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.options == null) {
            return;
        }

        for (KeyMapping binding : client.options.keyMappings) {
            if (binding == null) {
                continue;
            }

            String savedKey = keyMap.get(binding.getName());
            if (savedKey == null) {
                continue;
            }

            try {
                binding.setKey(InputConstants.getKey(savedKey));
            } catch (Exception ignored) {
                // Ignore invalid legacy keys in imported profiles.
            }
        }

        KeyMapping.resetMapping();
        KeyMapping.releaseAll();
        client.options.save();

        currentProfile = name;
        saveCurrentProfile(name);
    }

    public static void deleteProfile(String name) {
        Objects.requireNonNull(name, "Profile name cannot be null");

        if (PROFILES.remove(name) != null) {
            PROFILE_HOTKEYS.remove(name);
            if (Objects.equals(currentProfile, name)) {
                currentProfile = null;
                saveCurrentProfile(null);
            }
        }

        File dir = getProfilesDir();
        if (dir == null) {
            return;
        }

        File file = new File(dir, name + ".kbp");
        if (file.exists() && !file.delete()) {
            System.err.println("Failed to delete profile file: " + file.getName());
        }
    }

    public static void exportProfile(String name) {
        Map<String, String> keyMap = PROFILES.get(name);
        if (keyMap == null) {
            return;
        }

        File dir = getProfilesDir();
        if (dir == null) {
            return;
        }

        Map<String, Object> exportData = new HashMap<>();
        exportData.put("name", name);
        exportData.put("keybindings", keyMap);

        if (PROFILE_HOTKEYS.containsKey(name)) {
            exportData.put("hotkeys", PROFILE_HOTKEYS.get(name));
        }

        File exportFile = new File(dir, name + ".kbp");
        try (FileWriter writer = new FileWriter(exportFile)) {
            GSON.toJson(exportData, writer);
        } catch (IOException e) {
            System.err.println("Failed to export profile " + name + ": " + e.getMessage());
        }
    }

    public static void setProfileHotkey(String profileName, List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            PROFILE_HOTKEYS.remove(profileName);
        } else {
            PROFILE_HOTKEYS.put(profileName, new ArrayList<>(keys));
        }
        exportProfile(profileName);
    }

    public static List<String> getProfileHotkey(String profileName) {
        return PROFILE_HOTKEYS.get(profileName);
    }

    public static void loadProfiles() {
        loadProfilesFromDirectory();
    }

    public static void saveCurrentProfile(String profile) {
        currentProfile = profile;
        File file = getCurrentProfileFile();
        if (file == null) {
            return;
        }

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(profile != null ? profile : "");
        } catch (IOException e) {
            System.err.println("Failed to save current profile: " + e.getMessage());
        }
    }

    public static String loadCurrentProfile() {
        File file = getCurrentProfileFile();
        if (file == null || !file.exists()) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            return line != null && !line.trim().isEmpty() ? line.trim() : null;
        } catch (IOException e) {
            System.err.println("Failed to load current profile: " + e.getMessage());
            return null;
        }
    }

    public static String getCurrentProfile() {
        return currentProfile;
    }

    public static void openProfilesFolder() {
        File dir = getProfilesDir();
        if (dir == null) {
            return;
        }

        if (!dir.exists() && !dir.mkdirs()) {
            System.err.println("Failed to create profiles directory");
            return;
        }

        try {
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[]{"explorer.exe", dir.getAbsolutePath()});
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", dir.getAbsolutePath()});
            } else if (os.contains("nix") || os.contains("nux")) {
                Runtime.getRuntime().exec(new String[]{"xdg-open", dir.getAbsolutePath()});
            } else if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(dir);
            }
        } catch (IOException e) {
            System.err.println("Failed to open profiles folder: " + e.getMessage());
        }
    }
}
