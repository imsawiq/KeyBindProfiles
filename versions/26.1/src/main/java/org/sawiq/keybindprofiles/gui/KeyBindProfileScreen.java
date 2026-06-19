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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class KeyBindProfileScreen extends Screen {
    private final Screen parent;
    private EditBox profileNameField;
    private EditBox searchField;
    private EditBox serverInputField;
    private final ProfileListWidget profileListWidget = new ProfileListWidget();
    private final ServerListWidget serverListWidget = new ServerListWidget();
    private Button applyButton;
    private Button renameButton;
    private Button deleteButton;
    private Button addServerButton;
    private String selectedProfile;
    private int scrollOffset;
    private final ScreenStatusMessage statusMessage = new ScreenStatusMessage();
    private final ProfileHotkeyCapture hotkeyCapture = new ProfileHotkeyCapture();

    public KeyBindProfileScreen(Screen parent) {
        super(Component.translatable("keybindprofiles.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        KeyBindProfiles.reloadProfilesFromDirectory();

        KeyBindProfileScreenLayout layout = layout();
        int leftX = layout.leftPanelX();
        int rightX = layout.rightPanelX();
        int fieldY = KeyBindProfileScreenLayout.CONTENT_TOP;

        profileNameField = new EditBox(font, leftX, fieldY, KeyBindProfileScreenLayout.FIELD_WIDTH, KeyBindProfileScreenLayout.BUTTON_HEIGHT, Component.translatable("keybindprofiles.profile_name"));
        profileNameField.setMaxLength(32);
        addRenderableWidget(profileNameField);

        searchField = new EditBox(font, leftX, fieldY + KeyBindProfileScreenLayout.LABELED_FIELD_SPACING, KeyBindProfileScreenLayout.LEFT_PANEL_WIDTH, KeyBindProfileScreenLayout.BUTTON_HEIGHT, Component.translatable("keybindprofiles.search"));
        searchField.setMaxLength(64);
        searchField.setResponder(value -> {
            scrollOffset = 0;
            refreshProfileList();
        });
        addRenderableWidget(searchField);

        addRenderableWidget(Button.builder(Component.literal("📁"), button -> {
            showStatus(KeyBindProfiles.openProfilesFolder()
                    ? "keybindprofiles.status.folder_opened"
                    : "keybindprofiles.status.folder_open_failed");
        }).bounds(layout.folderButtonX(), 10, 20, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("keybindprofiles.create"), button -> createProfile())
                .bounds(leftX + KeyBindProfileScreenLayout.FIELD_WIDTH + 8, fieldY, 130, KeyBindProfileScreenLayout.BUTTON_HEIGHT)
                .build());

        applyButton = Button.builder(Component.translatable("keybindprofiles.apply"), button -> applySelectedProfile())
                .bounds(rightX, KeyBindProfileScreenLayout.CONTENT_TOP, KeyBindProfileScreenLayout.RIGHT_PANEL_WIDTH, KeyBindProfileScreenLayout.BUTTON_HEIGHT)
                .build();
        addRenderableWidget(applyButton);

        renameButton = Button.builder(Component.translatable("keybindprofiles.rename"), button -> renameSelectedProfile())
                .bounds(rightX, KeyBindProfileScreenLayout.CONTENT_TOP + KeyBindProfileScreenLayout.BUTTON_SPACING, KeyBindProfileScreenLayout.RIGHT_PANEL_WIDTH, KeyBindProfileScreenLayout.BUTTON_HEIGHT)
                .build();
        addRenderableWidget(renameButton);

        deleteButton = Button.builder(Component.translatable("keybindprofiles.delete"), button -> deleteSelectedProfile())
                .bounds(rightX, KeyBindProfileScreenLayout.CONTENT_TOP + KeyBindProfileScreenLayout.BUTTON_SPACING * 2, KeyBindProfileScreenLayout.RIGHT_PANEL_WIDTH, KeyBindProfileScreenLayout.BUTTON_HEIGHT)
                .build();
        addRenderableWidget(deleteButton);

        serverInputField = new EditBox(font, rightX, layout.serverInputY(), 196, KeyBindProfileScreenLayout.BUTTON_HEIGHT, Component.translatable("keybindprofiles.server_address"));
        serverInputField.setMaxLength(128);
        addRenderableWidget(serverInputField);

        addServerButton = Button.builder(Component.translatable("keybindprofiles.add_server"), button -> addServerToSelectedProfile())
                .bounds(rightX + 204, layout.serverInputY(), 96, KeyBindProfileScreenLayout.BUTTON_HEIGHT)
                .build();
        addRenderableWidget(addServerButton);

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> returnToParent())
                .bounds(layout.doneButtonX(), layout.doneButtonY(), 200, KeyBindProfileScreenLayout.BUTTON_HEIGHT)
                .build());

        if (selectedProfile != null && KeyBindProfiles.PROFILES.containsKey(selectedProfile)) {
            profileNameField.setValue(selectedProfile);
        } else {
            selectedProfile = null;
        }

        refreshProfileList();
        refreshServerList();
        updateActionButtons();
    }

    private void createProfile() {
        String name = profileNameField.getValue().trim();
        if (name.isEmpty()) {
            showStatus("keybindprofiles.status.profile_name_required");
            return;
        }

        if (KeyBindProfiles.PROFILES.containsKey(name)) {
            showStatus("keybindprofiles.status.profile_exists", name);
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.options == null) {
            return;
        }

        KeyBindProfiles.saveProfile(name, client.options.keyMappings);
        KeyBindProfiles.reloadProfilesFromDirectory();
        selectProfile(name);
        searchField.setValue("");
        scrollOffset = 0;
        refreshProfileList();
        refreshServerList();
        showStatus("keybindprofiles.status.profile_created", name);
    }

    private void applySelectedProfile() {
        if (selectedProfile == null) {
            showStatus("keybindprofiles.status.select_profile");
            return;
        }

        KeyBindProfiles.applyProfile(selectedProfile);
        refreshParentKeybindsScreen();
        showStatus("keybindprofiles.status.profile_applied", selectedProfile);
    }

    private void renameSelectedProfile() {
        if (selectedProfile == null) {
            showStatus("keybindprofiles.status.select_profile");
            return;
        }

        String newName = profileNameField.getValue().trim();
        if (newName.isEmpty()) {
            showStatus("keybindprofiles.status.profile_name_required");
            return;
        }

        if (newName.equals(selectedProfile)) {
            showStatus("keybindprofiles.status.rename_same_name");
            return;
        }

        if (KeyBindProfiles.PROFILES.containsKey(newName)) {
            showStatus("keybindprofiles.status.profile_exists", newName);
            return;
        }

        renameProfile(selectedProfile, newName);
    }

    private void renameProfile(String oldName, String newName) {
        Map<String, String> keyMap = KeyBindProfiles.PROFILES.get(oldName);
        Minecraft client = Minecraft.getInstance();
        if (keyMap == null || client == null || client.options == null) {
            return;
        }

        List<String> hotkeys = KeyBindProfiles.getProfileHotkey(oldName);
        List<String> autoSwitchServers = KeyBindProfiles.getProfileAutoSwitchServers(oldName);

        KeyBindProfiles.deleteProfile(oldName);
        KeyMapping[] renamedBindings = cloneBindingsWithProfileKeys(client.options.keyMappings, keyMap);
        KeyBindProfiles.saveProfile(newName, renamedBindings);
        restoreProfileMetadata(newName, hotkeys, autoSwitchServers);
        selectProfile(newName);
        refreshProfileList();
        refreshServerList();
        showStatus("keybindprofiles.status.profile_renamed", newName);

        if (Objects.equals(KeyBindProfiles.getCurrentProfile(), oldName)) {
            KeyBindProfiles.saveCurrentProfile(newName);
            this.init(this.width, this.height);
        }
    }

    private KeyMapping[] cloneBindingsWithProfileKeys(KeyMapping[] bindings, Map<String, String> keyMap) {
        KeyMapping[] clonedBindings = bindings.clone();
        for (KeyMapping binding : clonedBindings) {
            applySavedKey(binding, keyMap);
        }
        return clonedBindings;
    }

    private void applySavedKey(KeyMapping binding, Map<String, String> keyMap) {
        String savedKey = keyMap.get(binding.getName());
        if (savedKey == null) {
            return;
        }

        try {
            binding.setKey(InputConstants.getKey(savedKey));
        } catch (IllegalArgumentException ignored) {
            // Ignore invalid values from manually edited profile files.
        }
    }

    private void restoreProfileMetadata(String profileName, List<String> hotkeys, List<String> autoSwitchServers) {
        if (hotkeys != null) {
            KeyBindProfiles.setProfileHotkey(profileName, hotkeys);
        }

        if (autoSwitchServers != null) {
            KeyBindProfiles.setProfileAutoSwitchServers(profileName, autoSwitchServers);
        }
    }

    private void deleteSelectedProfile() {
        if (selectedProfile == null) {
            showStatus("keybindprofiles.status.select_profile");
            return;
        }

        String deletedProfile = selectedProfile;
        KeyBindProfiles.deleteProfile(deletedProfile);
        selectedProfile = null;
        profileNameField.setValue("");
        serverInputField.setValue("");
        refreshProfileList();
        refreshServerList();
        showStatus("keybindprofiles.status.profile_deleted", deletedProfile);

        if (Objects.equals(KeyBindProfiles.getCurrentProfile(), deletedProfile)) {
            this.init(this.width, this.height);
        }
    }

    private void selectProfile(String profileName) {
        selectedProfile = profileName;
        profileNameField.setValue(profileName);
        serverInputField.setValue("");
    }

    private void refreshParentKeybindsScreen() {
        if (!(parent instanceof KeyBindsScreen keybindsScreen)) {
            return;
        }

        KeybindsScreenNavigation.refreshControlsList(keybindsScreen);
        this.init(this.width, this.height);
    }

    private void returnToParent() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }

        if (parent instanceof KeyBindsScreen originalKeybindsScreen) {
            client.setScreen(KeybindsScreenNavigation.createFreshKeybindsScreen(originalKeybindsScreen));
            return;
        }

        client.setScreen(parent);
    }

    private KeyBindProfileScreenLayout layout() {
        return new KeyBindProfileScreenLayout(width, height);
    }

    void addButton(Button button) {
        addRenderableWidget(button);
    }

    void removeButton(Button button) {
        removeWidget(button);
    }

    private void addServerToSelectedProfile() {
        if (selectedProfile == null) {
            showStatus("keybindprofiles.status.select_profile");
            return;
        }

        String server = serverInputField.getValue().trim();
        if (server.isEmpty()) {
            showStatus("keybindprofiles.status.server_required");
            return;
        }

        List<String> servers = new ArrayList<>();
        List<String> existingServers = KeyBindProfiles.getProfileAutoSwitchServers(selectedProfile);
        if (existingServers != null) {
            servers.addAll(existingServers);
        }

        String normalizedServer = normalizeServer(server);
        for (String existingServer : servers) {
            if (normalizeServer(existingServer).equals(normalizedServer)) {
                showStatus("keybindprofiles.status.server_exists", server);
                return;
            }
        }

        servers.add(server);
        KeyBindProfiles.setProfileAutoSwitchServers(selectedProfile, servers);
        serverInputField.setValue("");
        refreshServerList();
        showStatus("keybindprofiles.status.server_added", server);
    }

    private String normalizeServer(String server) {
        return server.trim().toLowerCase(Locale.ROOT);
    }

    private void removeServerFromSelectedProfile(String server) {
        if (selectedProfile == null) {
            showStatus("keybindprofiles.status.select_profile");
            return;
        }

        List<String> servers = new ArrayList<>();
        List<String> existingServers = KeyBindProfiles.getProfileAutoSwitchServers(selectedProfile);
        if (existingServers != null) {
            servers.addAll(existingServers);
        }
        servers.remove(server);
        KeyBindProfiles.setProfileAutoSwitchServers(selectedProfile, servers);
        refreshServerList();
        showStatus("keybindprofiles.status.server_removed", server);
    }

    private void refreshServerList() {
        serverListWidget.refresh(new ServerListWidget.RefreshRequest(
                this,
                layout(),
                serverInputField,
                selectedProfile,
                height,
                this::removeServerFromSelectedProfile,
                status -> showStatus(status.translationKey(), status.args()),
                this::updateActionButtons
        ));
    }

    private void updateActionButtons() {
        if (applyButton == null || renameButton == null || deleteButton == null || serverInputField == null || addServerButton == null) {
            return;
        }
        boolean hasSelectedProfile = selectedProfile != null;
        applyButton.active = hasSelectedProfile;
        renameButton.active = hasSelectedProfile;
        deleteButton.active = hasSelectedProfile;
        serverInputField.active = hasSelectedProfile;
        addServerButton.active = hasSelectedProfile;
    }

    private void showStatus(String translationKey, Object... args) {
        statusMessage.show(translationKey, args);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == InputConstants.KEY_RETURN && serverInputField != null && serverInputField.isFocused()) {
            addServerToSelectedProfile();
            return true;
        }
        if (keyCode == InputConstants.KEY_RETURN && profileNameField != null && profileNameField.isFocused()) {
            createProfile();
            return true;
        }

        if (hotkeyCapture.handleKeyPressed(event)) {
            refreshProfileList();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean insideBounds) {
        if (hotkeyCapture.handleMouseClicked(event.button())) {
            refreshProfileList();
            return true;
        }
        return super.mouseClicked(event, insideBounds);
    }

    public void refreshProfileList() {
        scrollOffset = profileListWidget.refresh(new ProfileListWidget.RefreshRequest(
                this,
                layout(),
                searchField,
                selectedProfile,
                scrollOffset,
                hotkeyCapture,
                this::selectProfile,
                this::refreshProfileList,
                this::refreshServerList,
                this::updateActionButtons
        ));
        updateActionButtons();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        KeyBindProfileScreenLayout layout = layout();
        int listX = layout.leftPanelX();
        int listWidth = KeyBindProfileScreenLayout.PROFILE_BUTTON_WIDTH + KeyBindProfileScreenLayout.HOTKEY_BUTTON_WIDTH + 8;

        if (mouseX >= listX
                && mouseX <= listX + listWidth
                && mouseY >= layout.listTop()
                && mouseY <= layout.listBottom()) {

            int listHeight = layout.listHeight();
            int totalHeight = profileListWidget.getVisibleProfileCount(searchField) * KeyBindProfileScreenLayout.BUTTON_SPACING;

            if (totalHeight > listHeight) {
                int maxOffset = Math.max(0, totalHeight - listHeight);
                scrollOffset = Math.max(0, Math.min(scrollOffset - (int) (vertical * KeyBindProfileScreenLayout.BUTTON_SPACING), maxOffset));
                refreshProfileList();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        extractor.fill(0, 0, width, height, 0x66000000);

        KeyBindProfileScreenLayout layout = layout();
        int listX = layout.leftPanelX();
        int listRight = listX + KeyBindProfileScreenLayout.PROFILE_BUTTON_WIDTH + KeyBindProfileScreenLayout.HOTKEY_BUTTON_WIDTH + 8;
        extractor.fill(listX - 5, layout.listTop() - 5, listRight + 5, layout.listBottom() + 5, 0x40000000);

        super.extractRenderState(extractor, mouseX, mouseY, delta);

        String currentProfileName = KeyBindProfiles.getCurrentProfile();
        Component fullProfileText = currentProfileName != null
                ? Component.translatable("keybindprofiles.applied_profile", currentProfileName)
                : Component.translatable("keybindprofiles.applied_profile", Component.translatable("options.off"));
        extractor.text(font, fullProfileText, 10, 10, 0xFFFFFFFF, true);
        extractor.text(font, Component.translatable("keybindprofiles.profile_name"), profileNameField.getX(), profileNameField.getY() - 11, 0xFFA0A0A0, false);
        extractor.text(font, Component.translatable("keybindprofiles.search"), searchField.getX(), searchField.getY() - 11, 0xFFA0A0A0, false);
        extractor.text(font, Component.translatable("keybindprofiles.server_address"), serverInputField.getX(), serverInputField.getY() - 11, 0xFFA0A0A0, false);
        extractor.text(font, Component.translatable("keybindprofiles.auto_switch_servers"), layout.rightPanelX(), layout.serverListTop() - 11, 0xFFA0A0A0, false);

        if (selectedProfile != null) {
            List<String> servers = KeyBindProfiles.getProfileAutoSwitchServers(selectedProfile);
            if (servers == null || servers.isEmpty()) {
                extractor.text(font, Component.translatable("keybindprofiles.no_servers"), layout.rightPanelX(), layout.serverListTop() + 5, 0xFF777777, false);
            }
        }

        Component statusText = statusMessage.getVisibleText();
        if (statusText != null) {
            int statusX = (width - font.width(statusText)) / 2;
            extractor.text(font, statusText, statusX, 24, 0xFFFFFF55, true);
        }

        if (hotkeyCapture.isCapturing()) {
            Component hint = Component.translatable("keybindprofiles.hotkey_hint");
            int hintX = (width - font.width(hint)) / 2;
            extractor.text(font, hint, hintX, layout.listBottom() - 12, 0xFFFFFF55, true);
        }
    }
}
