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
import net.minecraft.text.Text;
import org.sawiq.keybindprofiles.gui.KeyBindProfileScreen;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class KeyBindProfiles implements ClientModInitializer {
	public static final String MOD_ID = "keybindprofiles";
	// профили хранятся как имя -> (ключ -> значение)
	public static final Map<String, Map<String, String>> PROFILES = new HashMap<>();
	private static File configFile;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static KeyBinding openProfileScreenKey;
	private static String currentProfile = null;

	@Override
	public void onInitializeClient() {
		// регаем кнопку открытия
		openProfileScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.keybindprofiles.open",
				InputUtil.Type.KEYSYM,
				InputUtil.GLFW_KEY_O,
				"key.categories.misc"
		));

		// слушатель на кнопку
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openProfileScreenKey.wasPressed()) {
				openConfigScreen(null);
			}
		});

		// грузим профили и текущий при старте клиента
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			loadProfiles();
			currentProfile = loadCurrentProfile();
			if (currentProfile != null && PROFILES.containsKey(currentProfile)) {
				applyProfile(currentProfile);
			}
		});
	}

	public static void openConfigScreen(Screen parent) {
		MinecraftClient.getInstance().setScreen(new KeyBindProfileScreen(parent));
	}

	// путь к конфигу
	private static File getConfigFile() {
		if (configFile == null) {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client != null && client.runDirectory != null) {
				configFile = new File(client.runDirectory, "config/keybindprofiles.json");
			}
		}
		return configFile;
	}

	// сохраняем все данные
	private static void saveConfigData() {
		File configFile = getConfigFile();
		if (configFile == null) return;

		try {
			Map<String, Object> configToSave = new HashMap<>(PROFILES);
			configToSave.put("currentProfile", currentProfile);

			File parentDir = configFile.getParentFile();
			if (parentDir != null && !parentDir.exists()) {
				parentDir.mkdirs();
			}

			try (FileWriter writer = new FileWriter(configFile)) {
				GSON.toJson(configToSave, writer);
			}
		} catch (IOException e) {
			// тихо глотаем ошибки записи
		}
	}

	// грузим все данные
	private static void loadConfigData() {
		File configFile = getConfigFile();
		if (configFile == null || !configFile.exists()) {
			return;
		}

		try (FileReader reader = new FileReader(configFile)) {
			Type type = new TypeToken<Map<String, Object>>(){}.getType();
			Map<String, Object> loaded = GSON.fromJson(reader, type);

			if (loaded != null) {
				PROFILES.clear();
				String loadedCurrentProfile = null;

				for (Map.Entry<String, Object> entry : loaded.entrySet()) {
					if ("currentProfile".equals(entry.getKey())) {
						if (entry.getValue() instanceof String) {
							loadedCurrentProfile = (String) entry.getValue();
						}
					} else if (entry.getValue() instanceof Map) {
						try {
							@SuppressWarnings("unchecked")
							Map<String, String> keyMap = (Map<String, String>) entry.getValue();
							PROFILES.put(entry.getKey(), keyMap);
						} catch (ClassCastException e) {
							// пропускаем кривой профиль
						}
					}
				}
				currentProfile = loadedCurrentProfile;
			}
		} catch (IOException e) {
			// тихо глотаем ошибки чтения
		}
	}

	public static void saveProfile(String name, KeyBinding[] bindings) {
		Objects.requireNonNull(name, "имя профиля не может быть null");
		Objects.requireNonNull(bindings, "биндинги не могут быть null");

		Map<String, String> keyMap = new HashMap<>();
		for (KeyBinding binding : bindings) {
			if (binding != null) {
				keyMap.put(binding.getTranslationKey(), binding.getBoundKeyTranslationKey());
			}
		}
		PROFILES.put(name, keyMap);
		saveConfigData();
	}

	public static void applyProfile(String name) {
		Map<String, String> keyMap = PROFILES.get(name);
		if (keyMap == null) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.options == null) {
			return;
		}

		for (KeyBinding binding : client.options.allKeys) {
			if (binding != null) {
				String savedKey = keyMap.get(binding.getTranslationKey());
				if (savedKey != null) {
					try {
						InputUtil.Key inputKey = InputUtil.fromTranslationKey(savedKey);
						binding.setBoundKey(inputKey);
					} catch (Exception e) {
						// оставляем старый, если не смогли распарсить
					}
				}
			}
		}
		KeyBinding.updateKeysByCode();

		try {
			client.options.write();
		} catch (Exception e) {
			// не удалось сохранить в основной конфиг
		}

		currentProfile = name;
		saveCurrentProfile(name);
	}

	public static void deleteProfile(String name) {
		Objects.requireNonNull(name, "имя профиля не может быть null");

		if (PROFILES.remove(name) != null) {
			if (currentProfile != null && currentProfile.equals(name)) {
				currentProfile = null;
				saveCurrentProfile(null);
			}
			saveConfigData();
		}
	}

	public static void loadProfiles() {
		loadConfigData();
	}

	public static void saveCurrentProfile(String profile) {
		currentProfile = profile;
		saveConfigData();
	}

	public static String loadCurrentProfile() {
		return currentProfile;
	}

	public static String getCurrentProfile() {
		return currentProfile;
	}
}