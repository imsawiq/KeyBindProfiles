package org.sawiq.keybindprofiles.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.sawiq.keybindprofiles.KeyBindProfiles;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class KeyBindProfileScreen extends Screen {

    private final Screen parent;

    private TextFieldWidget profileNameField;

    private final List<ProfileButtonPair> profileButtonPairs = new ArrayList<>();
    private String selectedProfile = null;
    private int scrollOffset = 0;

    private static final int START_Y = 50;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 24;
    private static final int PROFILE_BUTTON_WIDTH = 250;
    private static final int HOTKEY_BUTTON_WIDTH = 60;
    private static final int FOOTER_HEIGHT = 90;

    private String capturingHotkeyFor = null;
    private final List<Integer> capturedKeys = new ArrayList<>();

    private static class ProfileButtonPair {
        final ButtonWidget profileButton;
        final ButtonWidget hotkeyButton;
        final String profileName;

        ProfileButtonPair(ButtonWidget profileButton, ButtonWidget hotkeyButton, String profileName) {
            this.profileButton = profileButton;
            this.hotkeyButton = hotkeyButton;
            this.profileName = profileName;
        }
    }

    public KeyBindProfileScreen(Screen parent) {
        super(Text.translatable("keybindprofiles.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // перезагружаем профили когда открываем меню
        KeyBindProfiles.reloadProfilesFromDirectory();

        final MinecraftClient client = MinecraftClient.getInstance();

        // поле ввода имени профиля
        profileNameField = new TextFieldWidget(
                textRenderer,
                (width - PROFILE_BUTTON_WIDTH) / 2,
                20,
                PROFILE_BUTTON_WIDTH,
                20,
                Text.translatable("keybindprofiles.profile_name")
        );
        profileNameField.setMaxLength(32);
        addDrawableChild(profileNameField);

        // кнопка открытия папки в правом верхнем углу
        ButtonWidget openFolderButton = ButtonWidget.builder(
                Text.literal("📁"),
                button -> KeyBindProfiles.openProfilesFolder()
        ).dimensions(width - 30, 10, 20, 20).build();
        addDrawableChild(openFolderButton);

        int buttonY = height - FOOTER_HEIGHT + 10;

        // кнопка создания профиля
        ButtonWidget createButton = ButtonWidget.builder(
                Text.translatable("keybindprofiles.create"),
                button -> {
                    String name = profileNameField.getText().trim();
                    if (!name.isEmpty() && !KeyBindProfiles.PROFILES.containsKey(name)) {
                        if (client.options != null) {
                            KeyBindProfiles.saveProfile(name, client.options.allKeys);
                            refreshProfileList();
                            profileNameField.setText("");
                        }
                    }
                }
        ).dimensions((width / 2) - 155, buttonY, 150, BUTTON_HEIGHT).build();
        addDrawableChild(createButton);

        // кнопка применения профиля
        ButtonWidget applyButton = ButtonWidget.builder(
                Text.translatable("keybindprofiles.apply"),
                button -> {
                    if (selectedProfile != null) {
                        KeyBindProfiles.applyProfile(selectedProfile);

                        // если открыли из меню управления, обновляем его
                        if (parent instanceof KeybindsScreen keybindsScreen) {
                            try {
                                Field controlsListField = KeybindsScreen.class.getDeclaredField("controlsList");
                                controlsListField.setAccessible(true);
                                Object controlsList = controlsListField.get(keybindsScreen);

                                if (controlsList != null) {
                                    Method updateMethod = controlsList.getClass().getMethod("update");
                                    updateMethod.invoke(controlsList);
                                }
                            } catch (Exception e) {
                                try {
                                    // ВАЖНО: в 1.21.11 вызываем init(width,height), без MinecraftClient
                                    keybindsScreen.init(keybindsScreen.width, keybindsScreen.height);
                                } catch (Exception ignored) {
                                    // ну не вышло
                                }
                            }
                        }

                        // перерисуем этот экран тоже (кнопки/подписи)
                        this.init(this.width, this.height);
                    }
                }
        ).dimensions((width / 2) + 5, buttonY, 150, BUTTON_HEIGHT).build();
        addDrawableChild(applyButton);

        // кнопка переименования
        ButtonWidget renameButton = ButtonWidget.builder(
                Text.translatable("keybindprofiles.rename"),
                button -> {
                    if (selectedProfile != null && !profileNameField.getText().trim().isEmpty()) {
                        String newName = profileNameField.getText().trim();

                        if (!newName.equals(selectedProfile) && !KeyBindProfiles.PROFILES.containsKey(newName)) {
                            Map<String, String> keyMap = KeyBindProfiles.PROFILES.get(selectedProfile);
                            if (keyMap != null && client.options != null) {
                                List<Integer> hotkeys = KeyBindProfiles.getProfileHotkey(selectedProfile);

                                KeyBindProfiles.deleteProfile(selectedProfile);

                                KeyBinding[] newBindings = client.options.allKeys.clone();
                                for (KeyBinding kb : newBindings) {
                                    String id = kb.getId();
                                    if (keyMap.containsKey(id)) {
                                        try {
                                            kb.setBoundKey(InputUtil.fromTranslationKey(keyMap.get(id)));
                                        } catch (Exception ignored) {
                                            // пропускаем
                                        }
                                    }
                                }

                                KeyBindProfiles.saveProfile(newName, newBindings);

                                if (hotkeys != null) {
                                    KeyBindProfiles.setProfileHotkey(newName, hotkeys);
                                }

                                selectedProfile = newName;
                                refreshProfileList();

                                if (Objects.equals(KeyBindProfiles.getCurrentProfile(), selectedProfile)) {
                                    KeyBindProfiles.saveCurrentProfile(newName);
                                }

                                this.init(this.width, this.height);
                            }
                        }
                    }
                }
        ).dimensions((width / 2) - 155, buttonY + BUTTON_SPACING, 150, BUTTON_HEIGHT).build();
        addDrawableChild(renameButton);

        // кнопка удаления
        ButtonWidget deleteButton = ButtonWidget.builder(
                Text.translatable("keybindprofiles.delete"),
                button -> {
                    if (selectedProfile != null) {
                        boolean wasCurrentProfile = Objects.equals(KeyBindProfiles.getCurrentProfile(), selectedProfile);

                        KeyBindProfiles.deleteProfile(selectedProfile);

                        selectedProfile = null;
                        profileNameField.setText("");
                        refreshProfileList();

                        if (wasCurrentProfile) {
                            this.init(this.width, this.height);
                        }
                    }
                }
        ).dimensions((width / 2) + 5, buttonY + BUTTON_SPACING, 150, BUTTON_HEIGHT).build();
        addDrawableChild(deleteButton);

        // кнопка выхода
        ButtonWidget doneButton = ButtonWidget.builder(
                Text.translatable("gui.done"),
                button -> {
                    if (parent instanceof KeybindsScreen originalKeybindsScreen) {
                        if (client.options != null) {
                            Screen newScreen;
                            try {
                                Field parentField = Screen.class.getDeclaredField("parent");
                                parentField.setAccessible(true);
                                Screen originalParent = (Screen) parentField.get(originalKeybindsScreen);
                                newScreen = new KeybindsScreen(originalParent, client.options);
                            } catch (Exception e) {
                                newScreen = new KeybindsScreen(null, client.options);
                            }
                            client.setScreen(newScreen);
                        } else {
                            client.setScreen(null);
                        }
                    } else {
                        client.setScreen(parent);
                    }
                }
        ).dimensions((width / 2) - 100, height - 30, 200, BUTTON_HEIGHT).build();
        addDrawableChild(doneButton);

        refreshProfileList();
    }

    private Text getHotkeyButtonText(String profileName) {
        // если сейчас захватываем клавиши для этого профиля
        if (capturingHotkeyFor != null && capturingHotkeyFor.equals(profileName)) {
            if (capturedKeys.isEmpty()) return Text.literal("...");

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < capturedKeys.size(); i++) {
                KeyInput keyInput = new KeyInput(capturedKeys.get(i), -1, 0);
                sb.append(InputUtil.fromKeyCode(keyInput).getLocalizedText().getString());
                if (i < capturedKeys.size() - 1) sb.append("+");
            }
            return Text.literal(sb.toString());
        }

        // показываем текущие hotkeys
        List<Integer> keys = KeyBindProfiles.getProfileHotkey(profileName);
        if (keys == null || keys.isEmpty()) return Text.literal("-");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            KeyInput keyInput = new KeyInput(keys.get(i), -1, 0);
            sb.append(InputUtil.fromKeyCode(keyInput).getLocalizedText().getString());
            if (i < keys.size() - 1) sb.append("+");
        }
        return Text.literal(sb.toString());
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        // если захватываем клавиши
        if (capturingHotkeyFor != null) {
            int keyCode = input.key();

            // ESC = отмена
            if (keyCode == InputUtil.GLFW_KEY_ESCAPE) {
                capturingHotkeyFor = null;
                capturedKeys.clear();
                refreshProfileList();
                return true;
            }

            // BACKSPACE = удалить hotkey
            if (keyCode == InputUtil.GLFW_KEY_BACKSPACE) {
                KeyBindProfiles.setProfileHotkey(capturingHotkeyFor, null);
                capturingHotkeyFor = null;
                capturedKeys.clear();
                refreshProfileList();
                return true;
            }

            // ENTER = сохранить
            if (keyCode == InputUtil.GLFW_KEY_ENTER) {
                if (!capturedKeys.isEmpty()) {
                    KeyBindProfiles.setProfileHotkey(capturingHotkeyFor, new ArrayList<>(capturedKeys));
                }
                capturingHotkeyFor = null;
                capturedKeys.clear();
                refreshProfileList();
                return true;
            }

            // добавляем клавишу (макс 2)
            if (!capturedKeys.contains(keyCode) && capturedKeys.size() < 2) {
                capturedKeys.add(keyCode);
                refreshProfileList();
            }
            return true;
        }

        return super.keyPressed(input);
    }

    public void refreshProfileList() {
        // удаляем старые кнопки
        for (ProfileButtonPair pair : profileButtonPairs) {
            remove(pair.profileButton);
            remove(pair.hotkeyButton);
        }
        profileButtonPairs.clear();

        int listHeight = height - FOOTER_HEIGHT - START_Y;
        int totalHeight = KeyBindProfiles.PROFILES.size() * BUTTON_SPACING;
        int maxOffset = Math.max(0, totalHeight - listHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxOffset));

        int buttonIndex = 0;
        for (String profile : KeyBindProfiles.PROFILES.keySet()) {
            int buttonY = START_Y + buttonIndex * BUTTON_SPACING - scrollOffset;

            // рендерим только видимые кнопки
            if (buttonY + BUTTON_HEIGHT > START_Y && buttonY < height - FOOTER_HEIGHT) {
                ButtonWidget profileButton = ButtonWidget.builder(
                        Text.literal(profile),
                        b -> {
                            selectedProfile = profile;
                            profileNameField.setText(profile);

                            // обновляем активность всех кнопок
                            for (ProfileButtonPair pair : profileButtonPairs) {
                                pair.profileButton.active = !pair.profileName.equals(selectedProfile);
                            }
                        }
                ).dimensions(
                        (width - PROFILE_BUTTON_WIDTH - HOTKEY_BUTTON_WIDTH - 5) / 2,
                        buttonY,
                        PROFILE_BUTTON_WIDTH,
                        BUTTON_HEIGHT
                ).build();

                profileButton.active = !profile.equals(selectedProfile);

                ButtonWidget hotkeyButton = ButtonWidget.builder(
                        getHotkeyButtonText(profile),
                        b -> {
                            if (capturingHotkeyFor != null && capturingHotkeyFor.equals(profile)) {
                                // сохраняем
                                if (!capturedKeys.isEmpty()) {
                                    KeyBindProfiles.setProfileHotkey(profile, new ArrayList<>(capturedKeys));
                                }
                                capturingHotkeyFor = null;
                                capturedKeys.clear();
                            } else {
                                // начинаем захват
                                capturingHotkeyFor = profile;
                                capturedKeys.clear();
                            }
                            refreshProfileList();
                        }
                ).dimensions(
                        (width - PROFILE_BUTTON_WIDTH - HOTKEY_BUTTON_WIDTH - 5) / 2 + PROFILE_BUTTON_WIDTH + 5,
                        buttonY,
                        HOTKEY_BUTTON_WIDTH,
                        BUTTON_HEIGHT
                ).build();

                ProfileButtonPair pair = new ProfileButtonPair(profileButton, hotkeyButton, profile);
                profileButtonPairs.add(pair);
                addDrawableChild(profileButton);
                addDrawableChild(hotkeyButton);
            }

            buttonIndex++;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int centerX = (width - PROFILE_BUTTON_WIDTH - HOTKEY_BUTTON_WIDTH - 5) / 2;

        // скроллим только если курсор в области списка
        if (mouseX >= centerX
                && mouseX <= centerX + PROFILE_BUTTON_WIDTH + HOTKEY_BUTTON_WIDTH + 5
                && mouseY >= START_Y
                && mouseY <= height - FOOTER_HEIGHT) {

            int listHeight = height - FOOTER_HEIGHT - START_Y;
            int totalHeight = KeyBindProfiles.PROFILES.size() * BUTTON_SPACING;

            if (totalHeight > listHeight) {
                int maxOffset = Math.max(0, totalHeight - listHeight);
                scrollOffset = Math.max(0, Math.min(scrollOffset - (int) (vertical * BUTTON_SPACING), maxOffset));
                refreshProfileList();
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderDarkening(context);

        // темный фон для списка профилей
        int centerX = (width - PROFILE_BUTTON_WIDTH - HOTKEY_BUTTON_WIDTH - 5) / 2;
        context.fill(
                centerX - 5,
                START_Y - 5,
                centerX + PROFILE_BUTTON_WIDTH + HOTKEY_BUTTON_WIDTH + 10,
                height - FOOTER_HEIGHT + 5,
                0x40000000
        );

        super.render(context, mouseX, mouseY, delta);

        // текущий профиль вверху слева
        String currentProfileName = KeyBindProfiles.getCurrentProfile();
        Text fullProfileText = (currentProfileName != null)
                ? Text.translatable("keybindprofiles.applied_profile", currentProfileName)
                : Text.translatable("keybindprofiles.applied_profile", Text.translatable("options.off"));

        context.drawTextWithShadow(textRenderer, fullProfileText, 10, 10, 0xFFFFFFFF);

        // подсказка при захвате клавиш
        if (capturingHotkeyFor != null) {
            Text hint = Text.translatable("keybindprofiles.hotkey_hint");
            int hintX = (width - textRenderer.getWidth(hint)) / 2;
            context.drawTextWithShadow(textRenderer, hint, hintX, height - FOOTER_HEIGHT - 20, 0xFFFFFF55);
        }
    }
}
