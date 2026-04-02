package org.sawiq.keybindprofiles.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.sawiq.keybindprofiles.KeyBindProfiles;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class KeyBindProfileScreen extends Screen {
    private final Screen parent;
    private EditBox profileNameField;
    private final List<ProfileButtonPair> profileButtonPairs = new ArrayList<>();
    private String selectedProfile;
    private int scrollOffset;

    private static final int START_Y = 50;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 24;
    private static final int PROFILE_BUTTON_WIDTH = 250;
    private static final int HOTKEY_BUTTON_WIDTH = 60;
    private static final int FOOTER_HEIGHT = 90;

    private String capturingHotkeyFor;
    private final List<String> capturedKeys = new ArrayList<>();

    private static class ProfileButtonPair {
        final Button profileButton;
        final Button hotkeyButton;
        final String profileName;

        ProfileButtonPair(Button profileButton, Button hotkeyButton, String profileName) {
            this.profileButton = profileButton;
            this.hotkeyButton = hotkeyButton;
            this.profileName = profileName;
        }
    }

    public KeyBindProfileScreen(Screen parent) {
        super(Component.translatable("keybindprofiles.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        KeyBindProfiles.reloadProfilesFromDirectory();

        profileNameField = new EditBox(font, (width - PROFILE_BUTTON_WIDTH) / 2, 20, PROFILE_BUTTON_WIDTH, 20, Component.translatable("keybindprofiles.profile_name"));
        profileNameField.setMaxLength(32);
        addRenderableWidget(profileNameField);

        addRenderableWidget(Button.builder(Component.translatable("keybindprofiles.open_folder"), button ->
                KeyBindProfiles.openProfilesFolder()
        ).bounds(width - 70, 10, 60, 20).build());

        int buttonY = height - FOOTER_HEIGHT + 10;

        addRenderableWidget(Button.builder(Component.translatable("keybindprofiles.create"), button -> {
            String name = profileNameField.getValue().trim();
            if (!name.isEmpty() && !KeyBindProfiles.PROFILES.containsKey(name) && minecraft != null && minecraft.options != null) {
                KeyBindProfiles.saveProfile(name, minecraft.options.keyMappings);
                profileNameField.setValue("");
                refreshProfileList();
            }
        }).bounds((width / 2) - 155, buttonY, 150, BUTTON_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.translatable("keybindprofiles.apply"), button -> {
            if (selectedProfile != null) {
                KeyBindProfiles.applyProfile(selectedProfile);
                refreshParentKeybindsScreen();
            }
        }).bounds((width / 2) + 5, buttonY, 150, BUTTON_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.translatable("keybindprofiles.rename"), button -> {
            if (selectedProfile == null || minecraft == null || minecraft.options == null) {
                return;
            }

            String newName = profileNameField.getValue().trim();
            if (newName.isEmpty() || newName.equals(selectedProfile) || KeyBindProfiles.PROFILES.containsKey(newName)) {
                return;
            }

            Map<String, String> keyMap = KeyBindProfiles.PROFILES.get(selectedProfile);
            if (keyMap == null) {
                return;
            }

            List<String> hotkeys = KeyBindProfiles.getProfileHotkey(selectedProfile);
            KeyBindProfiles.deleteProfile(selectedProfile);

            KeyMapping[] newBindings = minecraft.options.keyMappings.clone();
            for (KeyMapping keyMapping : newBindings) {
                String savedKey = keyMap.get(keyMapping.getName());
                if (savedKey != null) {
                    try {
                        keyMapping.setKey(InputConstants.getKey(savedKey));
                    } catch (Exception ignored) {
                        // Ignore broken imported bindings during rename.
                    }
                }
            }

            KeyBindProfiles.saveProfile(newName, newBindings);
            if (hotkeys != null) {
                KeyBindProfiles.setProfileHotkey(newName, hotkeys);
            }

            if (Objects.equals(KeyBindProfiles.getCurrentProfile(), selectedProfile)) {
                KeyBindProfiles.saveCurrentProfile(newName);
            }

            selectedProfile = newName;
            refreshProfileList();
            refreshParentKeybindsScreen();
        }).bounds((width / 2) - 155, buttonY + BUTTON_SPACING, 150, BUTTON_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.translatable("keybindprofiles.delete"), button -> {
            if (selectedProfile != null) {
                KeyBindProfiles.deleteProfile(selectedProfile);
                selectedProfile = null;
                profileNameField.setValue("");
                refreshProfileList();
                refreshParentKeybindsScreen();
            }
        }).bounds((width / 2) + 5, buttonY + BUTTON_SPACING, 150, BUTTON_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> {
            if (minecraft != null) {
                minecraft.setScreen(parent);
            }
        }).bounds((width / 2) - 100, height - 30, 200, BUTTON_HEIGHT).build());

        refreshProfileList();
    }

    private void refreshParentKeybindsScreen() {
        if (parent instanceof KeyBindsScreen keyBindsScreen) {
            try {
                Field keyBindsListField = KeyBindsScreen.class.getDeclaredField("keyBindsList");
                keyBindsListField.setAccessible(true);
                Object keyBindsList = keyBindsListField.get(keyBindsScreen);
                if (keyBindsList != null) {
                    keyBindsList.getClass().getMethod("resetMappingAndUpdateButtons").invoke(keyBindsList);
                    keyBindsList.getClass().getMethod("refreshEntries").invoke(keyBindsList);
                }
            } catch (ReflectiveOperationException ignored) {
                // Fall back to rebuilding the screen if internals change again.
            }

            keyBindsScreen.init(width, height);
        }
    }

    private Component getHotkeyButtonText(String profileName) {
        if (Objects.equals(capturingHotkeyFor, profileName)) {
            if (capturedKeys.isEmpty()) {
                return Component.literal("...");
            }
            return Component.literal(joinDisplayNames(capturedKeys));
        }

        List<String> keys = KeyBindProfiles.getProfileHotkey(profileName);
        if (keys == null || keys.isEmpty()) {
            return Component.literal("-");
        }

        return Component.literal(joinDisplayNames(keys));
    }

    private String joinDisplayNames(List<String> keys) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            builder.append(InputConstants.getKey(keys.get(i)).getDisplayName().getString());
            if (i < keys.size() - 1) {
                builder.append("+");
            }
        }
        return builder.toString();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (capturingHotkeyFor != null) {
            int keyCode = event.key();

            if (keyCode == InputConstants.KEY_ESCAPE) {
                capturingHotkeyFor = null;
                capturedKeys.clear();
                refreshProfileList();
                return true;
            }

            if (keyCode == InputConstants.KEY_BACKSPACE) {
                KeyBindProfiles.setProfileHotkey(capturingHotkeyFor, null);
                capturingHotkeyFor = null;
                capturedKeys.clear();
                refreshProfileList();
                return true;
            }

            if (keyCode == InputConstants.KEY_RETURN) {
                if (!capturedKeys.isEmpty()) {
                    KeyBindProfiles.setProfileHotkey(capturingHotkeyFor, new ArrayList<>(capturedKeys));
                }
                capturingHotkeyFor = null;
                capturedKeys.clear();
                refreshProfileList();
                return true;
            }

            String savedKey = InputConstants.getKey(event).getName();
            if (!capturedKeys.contains(savedKey) && capturedKeys.size() < 2) {
                capturedKeys.add(savedKey);
                refreshProfileList();
            }
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean insideBounds) {
        if (capturingHotkeyFor != null) {
            String savedKey = InputConstants.Type.MOUSE.getOrCreate(event.button()).getName();
            if (!capturedKeys.contains(savedKey) && capturedKeys.size() < 2) {
                capturedKeys.add(savedKey);
                refreshProfileList();
            }
            return true;
        }

        return super.mouseClicked(event, insideBounds);
    }

    public void refreshProfileList() {
        for (ProfileButtonPair pair : profileButtonPairs) {
            removeWidget(pair.profileButton);
            removeWidget(pair.hotkeyButton);
        }
        profileButtonPairs.clear();

        int listHeight = height - FOOTER_HEIGHT - START_Y;
        int totalHeight = KeyBindProfiles.PROFILES.size() * BUTTON_SPACING;
        int maxOffset = Math.max(0, totalHeight - listHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxOffset));

        int buttonIndex = 0;
        for (String profile : KeyBindProfiles.PROFILES.keySet()) {
            int buttonY = START_Y + buttonIndex * BUTTON_SPACING - scrollOffset;

            if (buttonY + BUTTON_HEIGHT > START_Y && buttonY < height - FOOTER_HEIGHT) {
                Button profileButton = Button.builder(Component.literal(profile), button -> {
                    selectedProfile = profile;
                    profileNameField.setValue(profile);

                    for (ProfileButtonPair pair : profileButtonPairs) {
                        pair.profileButton.active = !pair.profileName.equals(selectedProfile);
                    }
                }).bounds((width - PROFILE_BUTTON_WIDTH - HOTKEY_BUTTON_WIDTH - 5) / 2, buttonY, PROFILE_BUTTON_WIDTH, BUTTON_HEIGHT).build();
                profileButton.active = !profile.equals(selectedProfile);

                Button hotkeyButton = Button.builder(getHotkeyButtonText(profile), button -> {
                    if (Objects.equals(capturingHotkeyFor, profile)) {
                        if (!capturedKeys.isEmpty()) {
                            KeyBindProfiles.setProfileHotkey(profile, new ArrayList<>(capturedKeys));
                        }
                        capturingHotkeyFor = null;
                        capturedKeys.clear();
                    } else {
                        capturingHotkeyFor = profile;
                        capturedKeys.clear();
                    }
                    refreshProfileList();
                }).bounds((width - PROFILE_BUTTON_WIDTH - HOTKEY_BUTTON_WIDTH - 5) / 2 + PROFILE_BUTTON_WIDTH + 5, buttonY, HOTKEY_BUTTON_WIDTH, BUTTON_HEIGHT).build();

                ProfileButtonPair pair = new ProfileButtonPair(profileButton, hotkeyButton, profile);
                profileButtonPairs.add(pair);
                addRenderableWidget(profileButton);
                addRenderableWidget(hotkeyButton);
            }

            buttonIndex++;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int centerX = (width - PROFILE_BUTTON_WIDTH - HOTKEY_BUTTON_WIDTH - 5) / 2;

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
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        extractTransparentBackground(extractor);

        int centerX = (width - PROFILE_BUTTON_WIDTH - HOTKEY_BUTTON_WIDTH - 5) / 2;
        extractor.fill(centerX - 5, START_Y - 5, centerX + PROFILE_BUTTON_WIDTH + HOTKEY_BUTTON_WIDTH + 10, height - FOOTER_HEIGHT + 5, 0x40000000);

        super.extractRenderState(extractor, mouseX, mouseY, delta);

        String currentProfileName = KeyBindProfiles.getCurrentProfile();
        Component fullProfileText = currentProfileName != null
                ? Component.translatable("keybindprofiles.applied_profile", currentProfileName)
                : Component.translatable("keybindprofiles.applied_profile", Component.translatable("options.off"));
        extractor.text(font, fullProfileText, 10, 10, 0xFFFFFFFF, true);

        if (capturingHotkeyFor != null) {
            Component hint = Component.translatable("keybindprofiles.hotkey_hint");
            int hintX = (width - font.width(hint)) / 2;
            extractor.text(font, hint, hintX, height - FOOTER_HEIGHT - 20, 0xFFFFFF55, true);
        }
    }
}
