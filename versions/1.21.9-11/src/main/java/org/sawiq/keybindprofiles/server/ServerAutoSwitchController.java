package org.sawiq.keybindprofiles.server;

import net.minecraft.client.MinecraftClient;
import org.sawiq.keybindprofiles.notification.ProfileNotification;
import org.sawiq.keybindprofiles.profile.ProfileService;

public final class ServerAutoSwitchController {
    private final ProfileService profileService;
    private final ProfileNotification notification;

    private String lastAutoSwitchServer;

    public ServerAutoSwitchController(ProfileService profileService, ProfileNotification notification) {
        this.profileService = profileService;
        this.notification = notification;
    }

    public void reset() {
        lastAutoSwitchServer = null;
    }

    public void tick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            return;
        }

        String serverAddress = ServerProfileMatcher.getCurrentServerAddress(client);
        if (serverAddress == null || serverAddress.isBlank()) {
            return;
        }

        String normalizedServer = ServerProfileMatcher.normalizeServerPattern(serverAddress);
        if (normalizedServer.equals(lastAutoSwitchServer)) {
            return;
        }

        lastAutoSwitchServer = normalizedServer;
        applyMatchingProfile(serverAddress);
    }

    private void applyMatchingProfile(String serverAddress) {
        String profileName = ServerProfileMatcher.findMatchingProfile(
                serverAddress,
                profileService.profileAutoSwitchServers(),
                profileService.profiles().keySet()
        );
        if (profileName == null || profileName.equals(profileService.getCurrentProfile())) {
            return;
        }

        profileService.applyProfile(profileName);
        notification.show(profileName);
    }
}
