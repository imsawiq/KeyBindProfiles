package org.sawiq.keybindprofiles.notification;

public final class ProfileNotification {
    private static final long NOTIFICATION_DURATION_MS = 3000;

    private String profileName;
    private long shownAt;

    public void show(String profileName) {
        this.profileName = profileName;
        this.shownAt = System.currentTimeMillis();
    }

    public String getVisibleProfileName() {
        if (profileName == null) {
            return null;
        }

        boolean isVisible = System.currentTimeMillis() - shownAt < NOTIFICATION_DURATION_MS;
        return isVisible ? profileName : null;
    }
}
