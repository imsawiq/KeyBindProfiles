package org.sawiq.keybindprofiles.gui;

final class KeyBindProfileScreenLayout {
    static final int CONTENT_TOP = 42;
    static final int CONTENT_WIDTH = 760;
    static final int LEFT_PANEL_WIDTH = 430;
    static final int RIGHT_PANEL_WIDTH = 300;
    static final int BUTTON_HEIGHT = 20;
    static final int BUTTON_SPACING = 24;
    static final int LABELED_FIELD_SPACING = 38;
    static final int FIELD_WIDTH = 292;
    static final int PROFILE_BUTTON_WIDTH = 310;
    static final int HOTKEY_BUTTON_WIDTH = 104;
    static final int SERVER_ROW_HEIGHT = 20;
    static final int SERVER_ROW_SPACING = 24;

    private static final int PANEL_GAP = 16;
    private static final int SCREEN_MARGIN = 12;
    private static final int BOTTOM_RESERVED_HEIGHT = 36;

    private final int screenWidth;
    private final int screenHeight;

    KeyBindProfileScreenLayout(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    int contentX() {
        return Math.max(SCREEN_MARGIN, (screenWidth - CONTENT_WIDTH) / 2);
    }

    int leftPanelX() {
        return contentX();
    }

    int rightPanelX() {
        return contentX() + LEFT_PANEL_WIDTH + PANEL_GAP;
    }

    int listTop() {
        return CONTENT_TOP + LABELED_FIELD_SPACING + BUTTON_SPACING + 12;
    }

    int listBottom() {
        return Math.max(listTop(), screenHeight - BOTTOM_RESERVED_HEIGHT);
    }

    int listHeight() {
        return Math.max(0, listBottom() - listTop());
    }

    int serverInputY() {
        return CONTENT_TOP + BUTTON_SPACING * 4 + 4;
    }

    int serverListTop() {
        return serverInputY() + BUTTON_SPACING + 16;
    }

    int doneButtonX() {
        return (screenWidth / 2) - 100;
    }

    int doneButtonY() {
        return screenHeight - 26;
    }

    int folderButtonX() {
        return screenWidth - 30;
    }
}
