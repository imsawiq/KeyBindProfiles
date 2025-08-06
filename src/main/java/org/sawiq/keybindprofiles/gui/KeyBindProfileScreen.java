package org.sawiq.keybindprofiles.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
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
    private final List<ButtonWidget> profileButtons = new ArrayList<>();
    private ButtonWidget createButton;
    private ButtonWidget applyButton;
    private ButtonWidget renameButton;
    private ButtonWidget deleteButton;
    private String selectedProfile = null;
    private int scrollOffset = 0;
    private static final int START_Y = 50;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 24;
    private static final int BUTTON_WIDTH = 200;
    private static final int FOOTER_HEIGHT = 130;

    public KeyBindProfileScreen(Screen parent) {
        super(Text.translatable("keybindprofiles.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        profileNameField = new TextFieldWidget(textRenderer, (width - BUTTON_WIDTH) / 2, 20, BUTTON_WIDTH, 20, Text.translatable("keybindprofiles.profile_name"));
        profileNameField.setMaxLength(32);
        addDrawableChild(profileNameField);

        int buttonY = height - FOOTER_HEIGHT + 20;

        createButton = ButtonWidget.builder(Text.translatable("keybindprofiles.create"), button -> {
            String name = profileNameField.getText().trim();
            if (!name.isEmpty() && !KeyBindProfiles.PROFILES.containsKey(name)) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.options != null) {
                    KeyBindProfiles.saveProfile(name, client.options.allKeys);
                    refreshProfileList();
                    profileNameField.setText("");
                }
            }
        }).dimensions((width / 2) - 155, buttonY, 150, BUTTON_HEIGHT).build();
        addDrawableChild(createButton);

        applyButton = ButtonWidget.builder(Text.translatable("keybindprofiles.apply"), button -> {
            if (selectedProfile != null) {
                KeyBindProfiles.applyProfile(selectedProfile);
                if (parent instanceof KeybindsScreen) {
                    KeybindsScreen keybindsScreen = (KeybindsScreen) parent;
                    try {
                        MinecraftClient mcClient = MinecraftClient.getInstance();
                        if (mcClient != null) {
                            Field controlsListField = KeybindsScreen.class.getDeclaredField("controlsList");
                            controlsListField.setAccessible(true);
                            Object controlsList = controlsListField.get(keybindsScreen);

                            if (controlsList != null) {
                                Method updateMethod = controlsList.getClass().getMethod("update");
                                updateMethod.invoke(controlsList);
                            }
                        }
                    } catch (Exception e) {
                        // если рефлексия не сработала, пробуем пересоздать экран
                        try {
                            MinecraftClient mcClient = MinecraftClient.getInstance();
                            if (mcClient != null) {
                                keybindsScreen.init(mcClient, keybindsScreen.width, keybindsScreen.height);
                            }
                        } catch (Exception initException) {
                            // ну не сработало, ладно
                        }
                    }
                }
                this.init(client, this.width, this.height);
            }
        }).dimensions((width / 2) + 5, buttonY, 150, BUTTON_HEIGHT).build();
        addDrawableChild(applyButton);

        renameButton = ButtonWidget.builder(Text.translatable("keybindprofiles.rename"), button -> {
            if (selectedProfile != null && !profileNameField.getText().trim().isEmpty()) {
                String newName = profileNameField.getText().trim();
                if (!newName.equals(selectedProfile)) {
                    if (!KeyBindProfiles.PROFILES.containsKey(newName)) {
                        Map<String, String> keyMap = KeyBindProfiles.PROFILES.get(selectedProfile);
                        if (keyMap != null) {
                            KeyBindProfiles.deleteProfile(selectedProfile);
                            MinecraftClient client = MinecraftClient.getInstance();
                            if (client != null && client.options != null) {
                                KeyBinding[] newBindings = client.options.allKeys.clone();
                                for (KeyBinding kb : newBindings) {
                                    String key = kb.getTranslationKey();
                                    if (keyMap.containsKey(key)) {
                                        try {
                                            kb.setBoundKey(InputUtil.fromTranslationKey(keyMap.get(key)));
                                        } catch (Exception e) {
                                            // оставляем как есть
                                        }
                                    }
                                }
                                KeyBindProfiles.saveProfile(newName, newBindings);
                                selectedProfile = newName;
                                refreshProfileList();
                                if (Objects.equals(KeyBindProfiles.getCurrentProfile(), selectedProfile)) {
                                    KeyBindProfiles.saveCurrentProfile(newName);
                                    this.init(client, this.width, this.height);
                                }
                            }
                        } else {
                            KeyBindProfiles.deleteProfile(selectedProfile);
                        }
                    }
                }
            }
        }).dimensions((width / 2) - 155, buttonY + BUTTON_SPACING, 150, BUTTON_HEIGHT).build();
        addDrawableChild(renameButton);

        deleteButton = ButtonWidget.builder(Text.translatable("keybindprofiles.delete"), button -> {
            if (selectedProfile != null) {
                KeyBindProfiles.deleteProfile(selectedProfile);
                selectedProfile = null;
                profileNameField.setText("");
                refreshProfileList();
                if (Objects.equals(KeyBindProfiles.getCurrentProfile(), selectedProfile)) {
                    this.init(client, this.width, this.height);
                }
            }
        }).dimensions((width / 2) + 5, buttonY + BUTTON_SPACING, 150, BUTTON_HEIGHT).build();
        addDrawableChild(deleteButton);

        ButtonWidget doneButton = ButtonWidget.builder(Text.translatable("gui.done"), button -> {
            if (parent instanceof KeybindsScreen) {
                KeybindsScreen originalKeybindsScreen = (KeybindsScreen) parent;
                MinecraftClient mcClient = MinecraftClient.getInstance();
                if (mcClient != null && mcClient.options != null) {
                    try {
                        Field parentField = Screen.class.getDeclaredField("parent");
                        parentField.setAccessible(true);
                        Screen originalParent = (Screen) parentField.get(originalKeybindsScreen);
                        KeybindsScreen newKeybindsScreen = new KeybindsScreen(originalParent, mcClient.options);
                        client.setScreen(newKeybindsScreen);
                    } catch (Exception e) {
                        KeybindsScreen newKeybindsScreen = new KeybindsScreen(null, mcClient.options);
                        client.setScreen(newKeybindsScreen);
                    }
                } else {
                    client.setScreen(null);
                }
            } else {
                if (parent != null) {
                    client.setScreen(parent);
                } else {
                    client.setScreen(null);
                }
            }
        }).dimensions((width / 2) - 100, height - 30, 200, BUTTON_HEIGHT).build();
        addDrawableChild(doneButton);

        refreshProfileList();
    }

    public void refreshProfileList() {
        profileButtons.forEach(this::remove);
        profileButtons.clear();

        int listHeight = height - FOOTER_HEIGHT - START_Y;
        int totalHeight = KeyBindProfiles.PROFILES.size() * BUTTON_SPACING;
        int maxOffset = Math.max(0, totalHeight - listHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxOffset));

        int y = START_Y - scrollOffset;
        int buttonIndex = 0;
        for (String profile : KeyBindProfiles.PROFILES.keySet()) {
            int buttonY = START_Y + buttonIndex * BUTTON_SPACING - scrollOffset;
            if (buttonY + BUTTON_HEIGHT > START_Y && buttonY < height - FOOTER_HEIGHT) {
                String profileName = profile;
                ButtonWidget button = ButtonWidget.builder(Text.literal(profileName), b -> {
                    selectedProfile = profileName;
                    profileNameField.setText(profileName);
                    profileButtons.forEach(btn -> btn.active = !btn.getMessage().getString().equals(profileName));
                    b.active = false;
                }).dimensions((width - BUTTON_WIDTH) / 2, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build();

                button.active = !profileName.equals(selectedProfile);
                profileButtons.add(button);
                addDrawableChild(button);
            }
            buttonIndex++;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (mouseX >= (width - BUTTON_WIDTH) / 2.0 &&
                mouseX <= (width + BUTTON_WIDTH) / 2.0 &&
                mouseY >= START_Y &&
                mouseY <= height - FOOTER_HEIGHT) {

            int listHeight = height - FOOTER_HEIGHT - START_Y;
            int totalHeight = KeyBindProfiles.PROFILES.size() * BUTTON_SPACING;
            if (totalHeight > listHeight) {
                int maxOffset = Math.max(0, totalHeight - listHeight);
                scrollOffset = (int) Math.max(0, Math.min(scrollOffset - (int)(vertical * BUTTON_SPACING), maxOffset));
                refreshProfileList();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        //бг
        this.renderBackground(context, mouseX, mouseY, delta);
        context.fill((width - BUTTON_WIDTH) / 2 - 5, START_Y - 5, (width + BUTTON_WIDTH) / 2 + 5, height - 130 + 5, 0x40000000);
        super.render(context, mouseX, mouseY, delta);
        String currentProfileName = KeyBindProfiles.getCurrentProfile();
        Text fullProfileText;
        if (currentProfileName != null) {
            fullProfileText = Text.translatable("keybindprofiles.applied_profile", currentProfileName);
        } else {
            fullProfileText = Text.translatable("keybindprofiles.applied_profile", Text.translatable("options.off"));
        }
        context.drawText(textRenderer, fullProfileText, 10, 10, 0xFFFFFF, false);
    }
}