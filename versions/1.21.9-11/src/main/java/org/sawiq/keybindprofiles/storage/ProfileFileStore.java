package org.sawiq.keybindprofiles.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Util;
import org.sawiq.keybindprofiles.KeyBindProfiles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ProfileFileStore {
    private static final String CONFIG_DIRECTORY = "config/keybindprofiles";
    private static final String CURRENT_PROFILE_FILE = "current_profile.txt";
    private static final String PROFILE_EXTENSION = ".kbp";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private File profilesDir;
    private File currentProfileFile;

    public void loadProfiles(
            Map<String, Map<String, String>> profiles,
            Map<String, List<String>> profileHotkeys,
            Map<String, List<String>> profileAutoSwitchServers
    ) {
        File dir = getProfilesDir();
        if (dir == null || !dir.exists()) {
            return;
        }

        File[] files = dir.listFiles((ignored, name) -> name.endsWith(PROFILE_EXTENSION));
        if (files == null) {
            return;
        }

        for (File file : files) {
            readProfileFile(file, profiles, profileHotkeys, profileAutoSwitchServers);
        }
    }

    public void saveProfile(String name, KeyBinding[] bindings, Map<String, Map<String, String>> profiles) {
        Objects.requireNonNull(name, "Profile name cannot be null");
        Objects.requireNonNull(bindings, "Bindings cannot be null");

        Map<String, String> keyMap = new HashMap<>();
        for (KeyBinding binding : bindings) {
            if (binding != null) {
                keyMap.put(binding.getId(), binding.getBoundKeyTranslationKey());
            }
        }

        profiles.put(name, keyMap);
    }

    public void exportProfile(
            String name,
            Map<String, Map<String, String>> profiles,
            Map<String, List<String>> profileHotkeys,
            Map<String, List<String>> profileAutoSwitchServers
    ) {
        Map<String, String> keyMap = profiles.get(name);
        File dir = getProfilesDir();
        if (keyMap == null || dir == null) {
            return;
        }

        Map<String, Object> exportData = new HashMap<>();
        exportData.put("name", name);
        exportData.put("keybindings", keyMap);

        if (profileHotkeys.containsKey(name)) {
            exportData.put("hotkeys", profileHotkeys.get(name));
        }

        if (profileAutoSwitchServers.containsKey(name)) {
            exportData.put("autoSwitchServers", profileAutoSwitchServers.get(name));
        }

        File exportFile = new File(dir, name + PROFILE_EXTENSION);
        try (FileWriter writer = new FileWriter(exportFile)) {
            gson.toJson(exportData, writer);
        } catch (IOException e) {
            KeyBindProfiles.LOGGER.error("Failed to export keybind profile '{}'", name, e);
        }
    }

    public void deleteProfileFile(String name) {
        Objects.requireNonNull(name, "Profile name cannot be null");

        File dir = getProfilesDir();
        if (dir == null) {
            return;
        }

        File profileFile = new File(dir, name + PROFILE_EXTENSION);
        if (profileFile.exists() && !profileFile.delete()) {
            KeyBindProfiles.LOGGER.error("Failed to delete keybind profile file '{}'", profileFile.getAbsolutePath());
        }
    }

    public void saveCurrentProfile(String profile) {
        File file = getCurrentProfileFile();
        if (file == null) {
            return;
        }

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(profile != null ? profile : "");
        } catch (IOException e) {
            KeyBindProfiles.LOGGER.error("Failed to save current keybind profile '{}'", profile, e);
        }
    }

    public String loadCurrentProfile() {
        File file = getCurrentProfileFile();
        if (file == null || !file.exists()) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            return line != null && !line.trim().isEmpty() ? line.trim() : null;
        } catch (IOException e) {
            KeyBindProfiles.LOGGER.error("Failed to load current keybind profile", e);
            return null;
        }
    }

    public boolean openProfilesFolder() {
        File dir = getProfilesDir();
        if (dir == null) {
            return false;
        }

        try {
            Util.getOperatingSystem().open(dir);
            return true;
        } catch (RuntimeException e) {
            KeyBindProfiles.LOGGER.error("Failed to open keybind profiles folder '{}'", dir.getAbsolutePath(), e);
            return false;
        }
    }

    private void readProfileFile(
            File file,
            Map<String, Map<String, String>> profiles,
            Map<String, List<String>> profileHotkeys,
            Map<String, List<String>> profileAutoSwitchServers
    ) {
        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> data = gson.fromJson(reader, type);
            if (!isValidProfileData(data)) {
                return;
            }

            String name = (String) data.get("name");
            @SuppressWarnings("unchecked")
            Map<String, String> bindings = (Map<String, String>) data.get("keybindings");
            profiles.put(name, bindings);

            List<String> hotkeys = readHotkeys(data.get("hotkeys"));
            if (!hotkeys.isEmpty()) {
                profileHotkeys.put(name, hotkeys);
            }

            List<String> autoSwitchServers = readStringList(data.get("autoSwitchServers"));
            if (!autoSwitchServers.isEmpty()) {
                profileAutoSwitchServers.put(name, autoSwitchServers);
            }
        } catch (IOException | JsonSyntaxException | ClassCastException e) {
            KeyBindProfiles.LOGGER.error("Failed to read keybind profile file '{}'", file.getAbsolutePath(), e);
        }
    }

    private boolean isValidProfileData(Map<String, Object> data) {
        return data != null
                && data.get("name") instanceof String
                && data.get("keybindings") instanceof Map<?, ?>;
    }

    private List<String> readHotkeys(Object value) {
        List<String> hotkeys = new ArrayList<>();
        if (!(value instanceof List<?> rawHotkeys)) {
            return hotkeys;
        }

        for (Object item : rawHotkeys) {
            if (item instanceof String key) {
                hotkeys.add(key);
            } else if (item instanceof Number code) {
                hotkeys.add(InputUtil.fromKeyCode(new KeyInput(code.intValue(), -1, 0)).getTranslationKey());
            }
        }
        return hotkeys;
    }

    private List<String> readStringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List<?> items) {
            for (Object item : items) {
                addNonBlankString(result, item);
            }
        } else {
            addNonBlankString(result, value);
        }
        return result;
    }

    private void addNonBlankString(List<String> values, Object value) {
        if (!(value instanceof String text)) {
            return;
        }

        String trimmed = text.trim();
        if (!trimmed.isEmpty()) {
            values.add(trimmed);
        }
    }

    private File getProfilesDir() {
        if (profilesDir != null) {
            return profilesDir;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.runDirectory == null) {
            return null;
        }

        profilesDir = new File(client.runDirectory, CONFIG_DIRECTORY);
        if (!profilesDir.exists() && !profilesDir.mkdirs()) {
            KeyBindProfiles.LOGGER.error("Failed to create keybind profile directory '{}'", profilesDir.getAbsolutePath());
        }
        return profilesDir;
    }

    private File getCurrentProfileFile() {
        if (currentProfileFile != null) {
            return currentProfileFile;
        }

        File dir = getProfilesDir();
        if (dir == null) {
            return null;
        }

        currentProfileFile = new File(dir, CURRENT_PROFILE_FILE);
        return currentProfileFile;
    }
}
