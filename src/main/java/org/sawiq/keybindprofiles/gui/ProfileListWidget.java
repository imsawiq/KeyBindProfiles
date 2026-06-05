package org.sawiq.keybindprofiles.gui;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.sawiq.keybindprofiles.KeyBindProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

final class ProfileListWidget {
    private final List<ProfileButtonPair> rows = new ArrayList<>();

    int refresh(RefreshRequest request) {
        clear(request.host());

        KeyBindProfileScreenLayout layout = request.layout();
        List<String> visibleProfiles = getVisibleProfiles(request.searchField());
        int listHeight = layout.listHeight();
        int totalHeight = visibleProfiles.size() * KeyBindProfileScreenLayout.BUTTON_SPACING;
        int maxOffset = Math.max(0, totalHeight - listHeight);
        int scrollOffset = Math.max(0, Math.min(request.scrollOffset(), maxOffset));

        int rowIndex = 0;
        for (String profileName : visibleProfiles) {
            int rowY = layout.listTop() + rowIndex * KeyBindProfileScreenLayout.BUTTON_SPACING - scrollOffset;
            addRowIfVisible(request, profileName, rowY);
            rowIndex++;
        }

        return scrollOffset;
    }

    int getVisibleProfileCount(TextFieldWidget searchField) {
        return getVisibleProfiles(searchField).size();
    }

    private void clear(KeyBindProfileScreen host) {
        for (ProfileButtonPair row : rows) {
            host.removeButton(row.profileButton());
            host.removeButton(row.hotkeyButton());
        }
        rows.clear();
    }

    private void addRowIfVisible(RefreshRequest request, String profileName, int rowY) {
        KeyBindProfileScreenLayout layout = request.layout();
        if (rowY + KeyBindProfileScreenLayout.BUTTON_HEIGHT <= layout.listTop() || rowY >= layout.listBottom()) {
            return;
        }

        int listX = layout.leftPanelX();
        ButtonWidget profileButton = ButtonWidget.builder(Text.literal(profileName), button -> {
            request.selectProfile().accept(profileName);
            setActiveProfile(request.selectedProfile());
            request.refreshServerList().run();
            request.updateActionButtons().run();
        }).dimensions(listX, rowY, KeyBindProfileScreenLayout.PROFILE_BUTTON_WIDTH, KeyBindProfileScreenLayout.BUTTON_HEIGHT).build();

        profileButton.active = !profileName.equals(request.selectedProfile());

        ButtonWidget hotkeyButton = ButtonWidget.builder(request.hotkeyCapture().getButtonText(profileName), button -> {
            request.hotkeyCapture().toggle(profileName);
            request.refreshProfileList().run();
        }).dimensions(
                listX + KeyBindProfileScreenLayout.PROFILE_BUTTON_WIDTH + 8,
                rowY,
                KeyBindProfileScreenLayout.HOTKEY_BUTTON_WIDTH,
                KeyBindProfileScreenLayout.BUTTON_HEIGHT
        ).build();

        rows.add(new ProfileButtonPair(profileButton, hotkeyButton, profileName));
        request.host().addButton(profileButton);
        request.host().addButton(hotkeyButton);
    }

    private void setActiveProfile(String selectedProfile) {
        for (ProfileButtonPair row : rows) {
            row.profileButton().active = !row.profileName().equals(selectedProfile);
        }
    }

    private List<String> getVisibleProfiles(TextFieldWidget searchField) {
        String query = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        List<String> profiles = new ArrayList<>(KeyBindProfiles.PROFILES.keySet());
        profiles.sort(String.CASE_INSENSITIVE_ORDER);

        if (query.isEmpty()) {
            return profiles;
        }

        List<String> filteredProfiles = new ArrayList<>();
        for (String profile : profiles) {
            if (profile.toLowerCase(Locale.ROOT).contains(query)) {
                filteredProfiles.add(profile);
            }
        }
        return filteredProfiles;
    }

    private record ProfileButtonPair(ButtonWidget profileButton, ButtonWidget hotkeyButton, String profileName) {
    }

    record RefreshRequest(
            KeyBindProfileScreen host,
            KeyBindProfileScreenLayout layout,
            TextFieldWidget searchField,
            String selectedProfile,
            int scrollOffset,
            ProfileHotkeyCapture hotkeyCapture,
            Consumer<String> selectProfile,
            Runnable refreshProfileList,
            Runnable refreshServerList,
            Runnable updateActionButtons
    ) {
    }
}
