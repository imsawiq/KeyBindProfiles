package org.sawiq.keybindprofiles.profile;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.sawiq.keybindprofiles.KeyBindProfiles;
import org.sawiq.keybindprofiles.storage.ProfileFileStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ProfileService {
    private final ProfileFileStore fileStore;
    private final Map<String, Map<String, String>> profiles = new HashMap<>();
    private final Map<String, List<String>> profileHotkeys = new HashMap<>();
    private final Map<String, List<String>> profileAutoSwitchServers = new HashMap<>();

    private String currentProfile;
    private Runnable autoSwitchResetCallback = () -> {
    };

    public ProfileService(ProfileFileStore fileStore) {
        this.fileStore = fileStore;
    }

    public Map<String, Map<String, String>> profiles() {
        return profiles;
    }

    public Map<String, List<String>> profileHotkeys() {
        return profileHotkeys;
    }

    public Map<String, List<String>> profileAutoSwitchServers() {
        return profileAutoSwitchServers;
    }

    public void setAutoSwitchResetCallback(Runnable autoSwitchResetCallback) {
        this.autoSwitchResetCallback = autoSwitchResetCallback == null ? () -> {
        } : autoSwitchResetCallback;
    }

    public void loadProfiles() {
        fileStore.loadProfiles(profiles, profileHotkeys, profileAutoSwitchServers);
    }

    public void reloadProfiles() {
        profiles.clear();
        profileHotkeys.clear();
        profileAutoSwitchServers.clear();
        loadProfiles();
    }

    public void loadCurrentProfile() {
        currentProfile = fileStore.loadCurrentProfile();
        if (currentProfile != null && profiles.containsKey(currentProfile)) {
            applyProfile(currentProfile);
        }
    }

    public void saveProfile(String name, KeyBinding[] bindings) {
        fileStore.saveProfile(name, bindings, profiles);
        exportProfile(name);
    }

    public void applyProfile(String name) {
        Map<String, String> keyMap = profiles.get(name);
        MinecraftClient client = MinecraftClient.getInstance();
        if (keyMap == null || client == null || client.options == null) {
            return;
        }

        applyKeyBindings(client.options.allKeys, keyMap);
        KeyBinding.updateKeysByCode();
        releaseAllKeys(client.options.allKeys);
        writeOptions(client);

        currentProfile = name;
        saveCurrentProfile(name);
    }

    public void deleteProfile(String name) {
        Objects.requireNonNull(name, "Profile name cannot be null");
        if (profiles.remove(name) == null) {
            return;
        }

        profileHotkeys.remove(name);
        profileAutoSwitchServers.remove(name);
        fileStore.deleteProfileFile(name);

        if (Objects.equals(currentProfile, name)) {
            saveCurrentProfile(null);
        }
    }

    public void exportProfile(String name) {
        fileStore.exportProfile(name, profiles, profileHotkeys, profileAutoSwitchServers);
    }

    public void setProfileHotkey(String profileName, List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            profileHotkeys.remove(profileName);
        } else {
            profileHotkeys.put(profileName, new ArrayList<>(keys));
        }
        exportProfile(profileName);
    }

    public List<String> getProfileHotkey(String profileName) {
        return profileHotkeys.get(profileName);
    }

    public void setProfileAutoSwitchServers(String profileName, List<String> servers) {
        List<String> normalizedServers = normalizeServerList(servers);
        if (normalizedServers.isEmpty()) {
            profileAutoSwitchServers.remove(profileName);
        } else {
            profileAutoSwitchServers.put(profileName, normalizedServers);
        }

        exportProfile(profileName);
        autoSwitchResetCallback.run();
    }

    public List<String> getProfileAutoSwitchServers(String profileName) {
        return profileAutoSwitchServers.get(profileName);
    }

    public void saveCurrentProfile(String profile) {
        currentProfile = profile;
        fileStore.saveCurrentProfile(profile);
    }

    public String getCurrentProfile() {
        return currentProfile;
    }

    public boolean openProfilesFolder() {
        return fileStore.openProfilesFolder();
    }

    private void applyKeyBindings(KeyBinding[] bindings, Map<String, String> keyMap) {
        for (KeyBinding binding : bindings) {
            applyKeyBinding(binding, keyMap);
        }
    }

    private void applyKeyBinding(KeyBinding binding, Map<String, String> keyMap) {
        if (binding == null) {
            return;
        }

        String savedKey = keyMap.get(binding.getTranslationKey());
        if (savedKey == null) {
            return;
        }

        try {
            binding.setBoundKey(InputUtil.fromTranslationKey(savedKey));
        } catch (IllegalArgumentException ignored) {
            // Invalid key values from old or manually edited profile files are ignored.
        }
    }

    private void releaseAllKeys(KeyBinding[] bindings) {
        for (KeyBinding binding : bindings) {
            if (binding != null) {
                binding.setPressed(false);
            }
        }
    }

    private void writeOptions(MinecraftClient client) {
        try {
            client.options.write();
        } catch (RuntimeException e) {
            KeyBindProfiles.LOGGER.error("Failed to write Minecraft options after applying keybind profile", e);
        }
    }

    private List<String> normalizeServerList(List<String> servers) {
        List<String> normalizedServers = new ArrayList<>();
        if (servers == null) {
            return normalizedServers;
        }

        for (String server : servers) {
            addUniqueNonBlankServer(normalizedServers, server);
        }
        return normalizedServers;
    }

    private void addUniqueNonBlankServer(List<String> servers, String server) {
        if (server == null) {
            return;
        }

        String trimmed = server.trim();
        if (!trimmed.isEmpty() && !servers.contains(trimmed)) {
            servers.add(trimmed);
        }
    }
}
