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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class KeyBindProfileScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget profileNameField;
    private TextFieldWidget searchField;
    private TextFieldWidget serverInputField;
    private final ProfileListWidget profileListWidget = new ProfileListWidget();
    private final ServerListWidget serverListWidget = new ServerListWidget();
    private ButtonWidget createButton;
    private ButtonWidget applyButton;
    private ButtonWidget renameButton;
    private ButtonWidget deleteButton;
    private ButtonWidget openFolderButton;
    private ButtonWidget addServerButton;
    private String selectedProfile = null;
    private int scrollOffset = 0;
    private final ScreenStatusMessage statusMessage = new ScreenStatusMessage();
    private final ProfileHotkeyCapture hotkeyCapture = new ProfileHotkeyCapture();

    public KeyBindProfileScreen(Screen parent) {
        super(Text.translatable("keybindprofiles.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        KeyBindProfiles.reloadProfilesFromDirectory();

        KeyBindProfileScreenLayout layout = layout();
        int leftX = layout.leftPanelX();
        int rightX = layout.rightPanelX();
        int fieldY = KeyBindProfileScreenLayout.CONTENT_TOP;

        profileNameField = new TextFieldWidget(textRenderer, leftX, fieldY, KeyBindProfileScreenLayout.FIELD_WIDTH, KeyBindProfileScreenLayout.BUTTON_HEIGHT, Text.translatable("keybindprofiles.profile_name"));
        profileNameField.setMaxLength(32);
        addDrawableChild(profileNameField);

        searchField = new TextFieldWidget(textRenderer, leftX, fieldY + KeyBindProfileScreenLayout.LABELED_FIELD_SPACING, KeyBindProfileScreenLayout.LEFT_PANEL_WIDTH, KeyBindProfileScreenLayout.BUTTON_HEIGHT, Text.translatable("keybindprofiles.search"));
        searchField.setMaxLength(64);
        searchField.setChangedListener(value -> {
            scrollOffset = 0;
            refreshProfileList();
        });
        addDrawableChild(searchField);

        openFolderButton = ButtonWidget.builder(Text.literal("📁"), button -> {
            if (KeyBindProfiles.openProfilesFolder()) {
                showStatus("keybindprofiles.status.folder_opened");
            } else {
                showStatus("keybindprofiles.status.folder_open_failed");
            }
        }).dimensions(layout.folderButtonX(), 10, 20, 20).build();
        addDrawableChild(openFolderButton);

        createButton = ButtonWidget.builder(Text.translatable("keybindprofiles.create"), button -> createProfile())
                .dimensions(leftX + KeyBindProfileScreenLayout.FIELD_WIDTH + 8, fieldY, 130, KeyBindProfileScreenLayout.BUTTON_HEIGHT)
                .build();
        addDrawableChild(createButton);

        applyButton = ButtonWidget.builder(Text.translatable("keybindprofiles.apply"), button -> applySelectedProfile())
                .dimensions(rightX, KeyBindProfileScreenLayout.CONTENT_TOP, KeyBindProfileScreenLayout.RIGHT_PANEL_WIDTH, KeyBindProfileScreenLayout.BUTTON_HEIGHT)
                .build();
        addDrawableChild(applyButton);

        renameButton = ButtonWidget.builder(Text.translatable("keybindprofiles.rename"), button -> renameSelectedProfile())
                .dimensions(rightX, KeyBindProfileScreenLayout.CONTENT_TOP + KeyBindProfileScreenLayout.BUTTON_SPACING, KeyBindProfileScreenLayout.RIGHT_PANEL_WIDTH, KeyBindProfileScreenLayout.BUTTON_HEIGHT)
                .build();
        addDrawableChild(renameButton);

        deleteButton = ButtonWidget.builder(Text.translatable("keybindprofiles.delete"), button -> deleteSelectedProfile())
                .dimensions(rightX, KeyBindProfileScreenLayout.CONTENT_TOP + KeyBindProfileScreenLayout.BUTTON_SPACING * 2, KeyBindProfileScreenLayout.RIGHT_PANEL_WIDTH, KeyBindProfileScreenLayout.BUTTON_HEIGHT)
                .build();
        addDrawableChild(deleteButton);

        serverInputField = new TextFieldWidget(textRenderer, rightX, layout.serverInputY(), 196, KeyBindProfileScreenLayout.BUTTON_HEIGHT, Text.translatable("keybindprofiles.server_address"));
        serverInputField.setMaxLength(128);
        addDrawableChild(serverInputField);

        addServerButton = ButtonWidget.builder(Text.translatable("keybindprofiles.add_server"), button -> {
            addServerToSelectedProfile();
        }).dimensions(rightX + 204, layout.serverInputY(), 96, KeyBindProfileScreenLayout.BUTTON_HEIGHT).build();
        addDrawableChild(addServerButton);

        ButtonWidget doneButton = ButtonWidget.builder(Text.translatable("gui.done"), button -> returnToParent())
                .dimensions(layout.doneButtonX(), layout.doneButtonY(), 200, KeyBindProfileScreenLayout.BUTTON_HEIGHT)
                .build();
        addDrawableChild(doneButton);

        if (selectedProfile != null && KeyBindProfiles.PROFILES.containsKey(selectedProfile)) {
            profileNameField.setText(selectedProfile);
        } else {
            selectedProfile = null;
        }

        refreshProfileList();
        refreshServerList();
        updateActionButtons();
    }

    private void createProfile() {
        String name = profileNameField.getText().trim();
        if (name.isEmpty()) {
            showStatus("keybindprofiles.status.profile_name_required");
            return;
        }

        if (KeyBindProfiles.PROFILES.containsKey(name)) {
            showStatus("keybindprofiles.status.profile_exists", name);
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) {
            return;
        }

        KeyBindProfiles.saveProfile(name, client.options.allKeys);
        KeyBindProfiles.reloadProfilesFromDirectory();
        selectProfile(name);
        searchField.setText("");
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

        String newName = profileNameField.getText().trim();
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (keyMap == null || client == null || client.options == null) {
            return;
        }

        List<String> hotkeys = KeyBindProfiles.getProfileHotkey(oldName);
        List<String> autoSwitchServers = KeyBindProfiles.getProfileAutoSwitchServers(oldName);

        KeyBindProfiles.deleteProfile(oldName);
        KeyBinding[] renamedBindings = cloneBindingsWithProfileKeys(client.options.allKeys, keyMap);
        KeyBindProfiles.saveProfile(newName, renamedBindings);
        restoreProfileMetadata(newName, hotkeys, autoSwitchServers);
        selectProfile(newName);
        refreshProfileList();
        refreshServerList();
        showStatus("keybindprofiles.status.profile_renamed", newName);

        if (Objects.equals(KeyBindProfiles.getCurrentProfile(), oldName)) {
            KeyBindProfiles.saveCurrentProfile(newName);
            this.init(client, this.width, this.height);
        }
    }

    private KeyBinding[] cloneBindingsWithProfileKeys(KeyBinding[] bindings, Map<String, String> keyMap) {
        KeyBinding[] clonedBindings = bindings.clone();
        for (KeyBinding binding : clonedBindings) {
            applySavedKey(binding, keyMap);
        }
        return clonedBindings;
    }

    private void applySavedKey(KeyBinding binding, Map<String, String> keyMap) {
        String savedKey = keyMap.get(binding.getTranslationKey());
        if (savedKey == null) {
            return;
        }

        try {
            binding.setBoundKey(InputUtil.fromTranslationKey(savedKey));
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
        profileNameField.setText("");
        serverInputField.setText("");
        refreshProfileList();
        refreshServerList();
        showStatus("keybindprofiles.status.profile_deleted", deletedProfile);

        if (Objects.equals(KeyBindProfiles.getCurrentProfile(), deletedProfile)) {
            this.init(client, this.width, this.height);
        }
    }

    private void selectProfile(String profileName) {
        selectedProfile = profileName;
        profileNameField.setText(profileName);
        serverInputField.setText("");
    }

    private void refreshParentKeybindsScreen() {
        if (!(parent instanceof KeybindsScreen keybindsScreen)) {
            return;
        }

        KeybindsScreenNavigation.refreshControlsList(keybindsScreen);
        this.init(client, this.width, this.height);
    }

    private void returnToParent() {
        if (client == null) {
            return;
        }

        if (parent instanceof KeybindsScreen originalKeybindsScreen) {
            client.setScreen(KeybindsScreenNavigation.createFreshKeybindsScreen(originalKeybindsScreen));
            return;
        }

        client.setScreen(parent);
    }

    private KeyBindProfileScreenLayout layout() {
        return new KeyBindProfileScreenLayout(width, height);
    }

    void addButton(ButtonWidget button) {
        addDrawableChild(button);
    }

    void removeButton(ButtonWidget button) {
        remove(button);
    }

    private void addServerToSelectedProfile() {
        if (selectedProfile == null) {
            showStatus("keybindprofiles.status.select_profile");
            return;
        }

        String server = serverInputField.getText().trim();
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
        serverInputField.setText("");
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputUtil.GLFW_KEY_ENTER && serverInputField != null && serverInputField.isFocused()) {
            addServerToSelectedProfile();
            return true;
        }
        if (keyCode == InputUtil.GLFW_KEY_ENTER && profileNameField != null && profileNameField.isFocused()) {
            createButton.onPress();
            return true;
        }

        if (hotkeyCapture.handleKeyPressed(keyCode, scanCode)) {
            refreshProfileList();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hotkeyCapture.handleMouseClicked(button)) {
            refreshProfileList();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
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

        if (mouseX >= listX &&
                mouseX <= listX + listWidth &&
                mouseY >= layout.listTop() &&
                mouseY <= layout.listBottom()) {

            int listHeight = layout.listHeight();
            int totalHeight = profileListWidget.getVisibleProfileCount(searchField) * KeyBindProfileScreenLayout.BUTTON_SPACING;

            if (totalHeight > listHeight) {
                int maxOffset = Math.max(0, totalHeight - listHeight);
                scrollOffset = (int) Math.max(0, Math.min(scrollOffset - (int)(vertical * KeyBindProfileScreenLayout.BUTTON_SPACING), maxOffset));
                refreshProfileList();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        KeyBindProfileScreenLayout layout = layout();
        int listX = layout.leftPanelX();
        int listRight = listX + KeyBindProfileScreenLayout.PROFILE_BUTTON_WIDTH + KeyBindProfileScreenLayout.HOTKEY_BUTTON_WIDTH + 8;
        context.fill(listX - 5, layout.listTop() - 5, listRight + 5, layout.listBottom() + 5, 0x40000000);

        super.render(context, mouseX, mouseY, delta);

        String currentProfileName = KeyBindProfiles.getCurrentProfile();
        Text fullProfileText;
        if (currentProfileName != null) {
            fullProfileText = Text.translatable("keybindprofiles.applied_profile", currentProfileName);
        } else {
            fullProfileText = Text.translatable("keybindprofiles.applied_profile", Text.translatable("options.off"));
        }
        context.drawText(textRenderer, fullProfileText, 10, 10, 0xFFFFFF, false);
        context.drawText(textRenderer, Text.translatable("keybindprofiles.profile_name"), profileNameField.getX(), profileNameField.getY() - 11, 0xA0A0A0, false);
        context.drawText(textRenderer, Text.translatable("keybindprofiles.search"), searchField.getX(), searchField.getY() - 11, 0xA0A0A0, false);
        context.drawText(textRenderer, Text.translatable("keybindprofiles.server_address"), serverInputField.getX(), serverInputField.getY() - 11, 0xA0A0A0, false);
        context.drawText(textRenderer, Text.translatable("keybindprofiles.auto_switch_servers"), layout.rightPanelX(), layout.serverListTop() - 11, 0xA0A0A0, false);

        if (selectedProfile != null) {
            List<String> servers = KeyBindProfiles.getProfileAutoSwitchServers(selectedProfile);
            if (servers == null || servers.isEmpty()) {
                context.drawText(textRenderer, Text.translatable("keybindprofiles.no_servers"), layout.rightPanelX(), layout.serverListTop() + 5, 0x777777, false);
            }
        }

        Text statusText = statusMessage.getVisibleText();
        if (statusText != null) {
            int statusX = (width - textRenderer.getWidth(statusText)) / 2;
            context.drawTextWithShadow(textRenderer, statusText, statusX, 24, 0xFFFF55);
        }

        if (hotkeyCapture.isCapturing()) {
            Text hint = Text.translatable("keybindprofiles.hotkey_hint");
            int hintX = (width - textRenderer.getWidth(hint)) / 2;
            context.drawText(textRenderer, hint, hintX, layout.listBottom() - 12, 0xFFFF55, true);
        }
    }
}
