package org.sawiq.keybindprofiles;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.sawiq.keybindprofiles.gui.KeyBindProfileScreen;

import java.awt.Desktop;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class KeyBindProfiles implements ClientModInitializer {
    // тут все профили и их кейбинды
    public static final Map<String, Map<String, String>> PROFILES = new HashMap<>();

    // тут горячие клавиши для профилей
    public static final Map<String, List<Integer>> PROFILE_HOTKEYS = new HashMap<>();

    private static File profilesDir;
    private static File currentProfileFile;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static KeyBinding openProfileScreenKey;
    private static String currentProfile = null;

    // для показа уведомлений типа "профиль применен"
    private static String notificationText = null;
    private static long notificationTime = 0;
    private static final long NOTIFICATION_DURATION = 3000;

    // чтобы не спамить переключениями когда клавиша зажата
    private static final Set<String> pressedHotkeys = new HashSet<>();

    @Override
    public void onInitializeClient() {
        // регаем кнопку открытия меню
        openProfileScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.keybindprofiles.open",
                InputUtil.Type.KEYSYM,
                InputUtil.GLFW_KEY_O,
                KeyBinding.Category.MISC
        ));

        // каждый тик проверяем нажатия
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openProfileScreenKey.wasPressed()) {
                openConfigScreen(null);
            }
            checkProfileHotkeys();
        });

        // при старте грузим профили
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            loadProfiles();
            currentProfile = loadCurrentProfile();
            if (currentProfile != null && PROFILES.containsKey(currentProfile)) {
                applyProfile(currentProfile);
            }
        });
    }

    // проверяем нажаты ли горячие клавиши профилей
    private static void checkProfileHotkeys() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;

        for (Map.Entry<String, List<Integer>> entry : PROFILE_HOTKEYS.entrySet()) {
            String profileName = entry.getKey();
            List<Integer> keys = entry.getValue();
            if (keys == null || keys.isEmpty()) continue;

            // все клавиши нажаты?
            boolean allPressed = true;
            for (Integer keyCode : keys) {
                if (!InputUtil.isKeyPressed(client.getWindow(), keyCode)) {
                    allPressed = false;
                    break;
                }
            }

            String hotkeyId = profileName + "_" + keys;
            if (allPressed) {
                // применяем только если еще не нажимали и это не текущий профиль
                if (!pressedHotkeys.contains(hotkeyId) && !profileName.equals(currentProfile)) {
                    applyProfile(profileName);
                    showNotification(profileName);
                    pressedHotkeys.add(hotkeyId);
                }
            } else {
                // отпустили клавиши
                pressedHotkeys.remove(hotkeyId);
            }
        }
    }

    // показать уведомление
    public static void showNotification(String profileName) {
        notificationText = profileName;
        notificationTime = System.currentTimeMillis();
    }

    // получить текст уведомления если еще не истекло время
    public static String getNotificationText() {
        if (notificationText != null && System.currentTimeMillis() - notificationTime < NOTIFICATION_DURATION) {
            return notificationText;
        }
        return null;
    }

    // открыть меню профилей
    public static void openConfigScreen(Screen parent) {
        MinecraftClient.getInstance().setScreen(new KeyBindProfileScreen(parent));
    }

    // получить папку с профилями
    private static File getProfilesDir() {
        if (profilesDir == null) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.runDirectory != null) {
                profilesDir = new File(client.runDirectory, "config/keybindprofiles");
                if (!profilesDir.exists()) {
                    boolean created = profilesDir.mkdirs();
                    if (!created) {
                        System.err.println("Failed to create profiles directory");
                    }
                }
            }
        }
        return profilesDir;
    }

    // файл с текущим профилем
    private static File getCurrentProfileFile() {
        if (currentProfileFile == null) {
            File dir = getProfilesDir();
            if (dir != null) {
                currentProfileFile = new File(dir, "current_profile.txt");
            }
        }
        return currentProfileFile;
    }

    // загружаем все .kbp файлы из папки
    private static void loadProfilesFromDirectory() {
        File dir = getProfilesDir();
        if (dir == null || !dir.exists()) return;

        File[] files = dir.listFiles((d, name) -> name.endsWith(".kbp"));
        if (files != null) {
            for (File file : files) {
                try (FileReader reader = new FileReader(file)) {
                    Type type = new TypeToken<Map<String, Object>>(){}.getType();
                    Map<String, Object> data = GSON.fromJson(reader, type);

                    if (data != null && data.containsKey("name") && data.containsKey("keybindings")) {
                        String name = (String) data.get("name");
                        @SuppressWarnings("unchecked")
                        Map<String, String> bindings = (Map<String, String>) data.get("keybindings");
                        PROFILES.put(name, bindings);

                        // грузим hotkeys если есть
                        if (data.containsKey("hotkeys") && data.get("hotkeys") instanceof List<?>) {
                            @SuppressWarnings("unchecked")
                            List<Double> hotkeys = (List<Double>) data.get("hotkeys");
                            List<Integer> intKeys = new ArrayList<>();
                            for (Double d : hotkeys) {
                                intKeys.add(d.intValue());
                            }
                            PROFILE_HOTKEYS.put(name, intKeys);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Failed to load profile from " + file.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    // перезагрузить профили (вызывается при открытии меню)
    public static void reloadProfilesFromDirectory() {
        PROFILES.clear();
        PROFILE_HOTKEYS.clear();
        loadProfilesFromDirectory();
    }

    // сохранить профиль
    public static void saveProfile(String name, KeyBinding[] bindings) {
        Objects.requireNonNull(name, "Profile name cannot be null");
        Objects.requireNonNull(bindings, "Bindings cannot be null");

        Map<String, String> keyMap = new HashMap<>();
        for (KeyBinding binding : bindings) {
            if (binding != null) {
                keyMap.put(binding.getId(), binding.getBoundKeyTranslationKey());
            }
        }

        PROFILES.put(name, keyMap);
        exportProfile(name);
    }

    // применить профиль
    public static void applyProfile(String name) {
        Map<String, String> keyMap = PROFILES.get(name);
        if (keyMap == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options == null) return;

        // меняем все кейбинды
        for (KeyBinding binding : client.options.allKeys) {
            if (binding != null) {
                String savedKey = keyMap.get(binding.getId());
                if (savedKey != null) {
                    try {
                        InputUtil.Key inputKey = InputUtil.fromTranslationKey(savedKey);
                        binding.setBoundKey(inputKey);
                    } catch (Exception e) {
                        // пропускаем кривые клавиши
                    }
                }
            }
        }
        KeyBinding.updateKeysByCode();

        // сохраняем в options.txt
        client.options.write();

        currentProfile = name;
        saveCurrentProfile(name);
    }

    // удалить профиль
    public static void deleteProfile(String name) {
        Objects.requireNonNull(name, "Profile name cannot be null");

        if (PROFILES.remove(name) != null) {
            PROFILE_HOTKEYS.remove(name);
            if (currentProfile != null && currentProfile.equals(name)) {
                currentProfile = null;
                saveCurrentProfile(null);
            }
        }

        // удаляем .kbp файл
        File dir = getProfilesDir();
        if (dir != null) {
            File file = new File(dir, name + ".kbp");
            if (file.exists()) {
                boolean deleted = file.delete();
                if (!deleted) {
                    System.err.println("Failed to delete profile file: " + file.getName());
                }
            }
        }
    }

    // экспорт профиля в .kbp
    public static void exportProfile(String name) {
        Map<String, String> keyMap = PROFILES.get(name);
        if (keyMap == null) return;

        File dir = getProfilesDir();
        if (dir == null) return;

        Map<String, Object> exportData = new HashMap<>();
        exportData.put("name", name);
        exportData.put("keybindings", keyMap);

        // добавляем hotkeys если есть
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

    // установить горячую клавишу для профиля
    public static void setProfileHotkey(String profileName, List<Integer> keys) {
        if (keys == null || keys.isEmpty()) {
            PROFILE_HOTKEYS.remove(profileName);
        } else {
            PROFILE_HOTKEYS.put(profileName, new ArrayList<>(keys));
        }
        exportProfile(profileName);
    }

    public static List<Integer> getProfileHotkey(String profileName) {
        return PROFILE_HOTKEYS.get(profileName);
    }

    // загрузить все профили
    public static void loadProfiles() {
        loadProfilesFromDirectory();
    }

    // сохранить какой профиль сейчас активен
    public static void saveCurrentProfile(String profile) {
        currentProfile = profile;
        File file = getCurrentProfileFile();
        if (file == null) return;

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(profile != null ? profile : "");
        } catch (IOException e) {
            System.err.println("Failed to save current profile: " + e.getMessage());
        }
    }

    // загрузить текущий профиль
    public static String loadCurrentProfile() {
        File file = getCurrentProfileFile();
        if (file == null || !file.exists()) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            return (line != null && !line.trim().isEmpty()) ? line.trim() : null;
        } catch (IOException e) {
            System.err.println("Failed to load current profile: " + e.getMessage());
            return null;
        }
    }

    public static String getCurrentProfile() {
        return currentProfile;
    }

    // открыть папку с профилями
    public static void openProfilesFolder() {
        File dir = getProfilesDir();
        if (dir == null) return;

        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                System.err.println("Failed to create profiles directory");
                return;
            }
        }

        try {
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                // Windows
                Runtime.getRuntime().exec(new String[]{"explorer.exe", dir.getAbsolutePath()});
            } else if (os.contains("mac")) {
                // macOS
                Runtime.getRuntime().exec(new String[]{"open", dir.getAbsolutePath()});
            } else if (os.contains("nix") || os.contains("nux")) {
                // Linux
                Runtime.getRuntime().exec(new String[]{"xdg-open", dir.getAbsolutePath()});
            } else {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(dir);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to open profiles folder: " + e.getMessage());
        }
    }
}
