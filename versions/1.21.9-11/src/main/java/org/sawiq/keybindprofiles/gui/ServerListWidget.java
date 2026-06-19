package org.sawiq.keybindprofiles.gui;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.sawiq.keybindprofiles.KeyBindProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class ServerListWidget {
    private final List<ServerButtonPair> rows = new ArrayList<>();

    void refresh(RefreshRequest request) {
        clear(request.host());

        if (request.selectedProfile() == null) {
            request.updateActionButtons().run();
            return;
        }

        List<String> servers = KeyBindProfiles.getProfileAutoSwitchServers(request.selectedProfile());
        if (servers == null || servers.isEmpty()) {
            request.updateActionButtons().run();
            return;
        }

        addVisibleRows(request, servers);
        request.updateActionButtons().run();
    }

    private void clear(KeyBindProfileScreen host) {
        for (ServerButtonPair row : rows) {
            host.removeButton(row.serverButton());
            host.removeButton(row.removeButton());
        }
        rows.clear();
    }

    private void addVisibleRows(RefreshRequest request, List<String> servers) {
        KeyBindProfileScreenLayout layout = request.layout();
        int rowX = layout.rightPanelX();
        int rowY = layout.serverListTop();
        int maxRows = Math.max(1, (request.screenHeight() - 32 - rowY) / KeyBindProfileScreenLayout.SERVER_ROW_SPACING);

        for (int i = 0; i < servers.size() && i < maxRows; i++) {
            addRow(request, servers.get(i), rowX, rowY + i * KeyBindProfileScreenLayout.SERVER_ROW_SPACING);
        }

        if (servers.size() > maxRows) {
            request.showStatus().accept(new StatusRequest("keybindprofiles.status.server_list_trimmed", maxRows, servers.size()));
        }
    }

    private void addRow(RefreshRequest request, String server, int rowX, int rowY) {
        ButtonWidget serverButton = ButtonWidget.builder(Text.literal(server), button -> {
            request.serverInputField().setText(server);
        }).dimensions(rowX, rowY, 196, KeyBindProfileScreenLayout.SERVER_ROW_HEIGHT).build();

        ButtonWidget removeButton = ButtonWidget.builder(Text.translatable("keybindprofiles.remove_server"), button -> {
            request.removeServer().accept(server);
        }).dimensions(rowX + 204, rowY, 96, KeyBindProfileScreenLayout.SERVER_ROW_HEIGHT).build();

        rows.add(new ServerButtonPair(serverButton, removeButton));
        request.host().addButton(serverButton);
        request.host().addButton(removeButton);
    }

    private record ServerButtonPair(ButtonWidget serverButton, ButtonWidget removeButton) {
    }

    record StatusRequest(String translationKey, Object... args) {
    }

    record RefreshRequest(
            KeyBindProfileScreen host,
            KeyBindProfileScreenLayout layout,
            TextFieldWidget serverInputField,
            String selectedProfile,
            int screenHeight,
            Consumer<String> removeServer,
            Consumer<StatusRequest> showStatus,
            Runnable updateActionButtons
    ) {
    }
}
