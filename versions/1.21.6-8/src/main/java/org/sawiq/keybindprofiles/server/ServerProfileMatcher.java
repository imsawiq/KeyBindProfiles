package org.sawiq.keybindprofiles.server;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ServerProfileMatcher {
    private ServerProfileMatcher() {
    }

    public static String getCurrentServerAddress(MinecraftClient client) {
        if (client.isInSingleplayer()) {
            return "singleplayer";
        }

        ServerInfo serverInfo = client.getCurrentServerEntry();
        if (serverInfo != null && serverInfo.address != null && !serverInfo.address.isBlank()) {
            return serverInfo.address;
        }

        return null;
    }

    public static String findMatchingProfile(
            String serverAddress,
            Map<String, List<String>> profileAutoSwitchServers,
            Set<String> existingProfiles
    ) {
        List<String> profileNames = new ArrayList<>(profileAutoSwitchServers.keySet());
        profileNames.sort(String.CASE_INSENSITIVE_ORDER);

        for (String profileName : profileNames) {
            if (!existingProfiles.contains(profileName)) {
                continue;
            }

            List<String> patterns = profileAutoSwitchServers.get(profileName);
            if (matchesAnyPattern(serverAddress, patterns)) {
                return profileName;
            }
        }
        return null;
    }

    public static String normalizeServerPattern(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceFirst("^[a-z]+://", "");
        int slashIndex = normalized.indexOf('/');
        return slashIndex >= 0 ? normalized.substring(0, slashIndex) : normalized;
    }

    private static boolean matchesAnyPattern(String serverAddress, List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }

        for (String pattern : patterns) {
            if (serverMatches(serverAddress, pattern)) {
                return true;
            }
        }
        return false;
    }

    private static boolean serverMatches(String serverAddress, String pattern) {
        String server = normalizeServerPattern(serverAddress);
        String expected = normalizeServerPattern(pattern);
        if (server.isEmpty() || expected.isEmpty()) {
            return false;
        }

        if (expected.indexOf('*') >= 0) {
            String regex = "\\Q" + expected.replace("*", "\\E.*\\Q") + "\\E";
            return server.matches(regex);
        }

        if (server.equals(expected)) {
            return true;
        }

        String serverHost = stripPort(server);
        String expectedHost = stripPort(expected);
        boolean expectedHasPort = expected.lastIndexOf(':') > -1;
        return !expectedHasPort && (serverHost.equals(expectedHost) || serverHost.endsWith("." + expectedHost));
    }

    private static String stripPort(String address) {
        int portIndex = address.lastIndexOf(':');
        return portIndex > -1 ? address.substring(0, portIndex) : address;
    }
}
