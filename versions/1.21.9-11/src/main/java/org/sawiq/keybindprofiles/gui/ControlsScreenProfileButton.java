package org.sawiq.keybindprofiles.gui;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.sawiq.keybindprofiles.KeyBindProfiles;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ControlsScreenProfileButton {
    private static final int BUTTON_HEIGHT = 20;
    private static final int BOTTOM_MARGIN = 26;
    private static final int GAP = 8;
    private static final int DEFAULT_BUTTON_WIDTH = 150;
    private static final int MIN_BUTTON_WIDTH = 90;
    private static final int SCREEN_PADDING = 8;

    private ControlsScreenProfileButton() {
    }

    public static void addOrReplace(Screen screen, int scaledWidth, int scaledHeight) {
        List<ClickableWidget> buttons = Screens.getButtons(screen);
        Text buttonText = Text.translatable("keybindprofiles.open");
        buttons.removeIf(button -> button.getMessage().getString().equals(buttonText.getString()));

        List<ClickableWidget> vanillaBottomButtons = findBottomButtons(buttons, scaledHeight);
        RowLayout row = calculateRowLayout(scaledWidth, scaledHeight);

        moveVanillaButtons(vanillaBottomButtons, row);
        buttons.add(ButtonWidget.builder(buttonText, button -> KeyBindProfiles.openConfigScreen(screen))
                .dimensions(row.startX(), row.y(), row.buttonWidth(), BUTTON_HEIGHT)
                .build());
    }

    private static List<ClickableWidget> findBottomButtons(List<ClickableWidget> buttons, int scaledHeight) {
        List<ClickableWidget> bottomButtons = new ArrayList<>();
        for (ClickableWidget button : buttons) {
            if (button.getY() >= scaledHeight - 32) {
                bottomButtons.add(button);
            }
        }
        bottomButtons.sort(Comparator.comparingInt(ClickableWidget::getX));
        return bottomButtons;
    }

    private static RowLayout calculateRowLayout(int scaledWidth, int scaledHeight) {
        int buttonWidth = scaledWidth >= 520
                ? DEFAULT_BUTTON_WIDTH
                : Math.max(MIN_BUTTON_WIDTH, (scaledWidth - SCREEN_PADDING * 4 - GAP * 2) / 3);
        int rowWidth = buttonWidth * 3 + GAP * 2;
        int startX = Math.max(SCREEN_PADDING, (scaledWidth - rowWidth) / 2);
        int y = Math.max(SCREEN_PADDING, scaledHeight - BOTTOM_MARGIN);
        return new RowLayout(startX, y, buttonWidth);
    }

    private static void moveVanillaButtons(List<ClickableWidget> bottomButtons, RowLayout row) {
        for (int i = 0; i < bottomButtons.size() && i < 2; i++) {
            ClickableWidget button = bottomButtons.get(i);
            button.setX(row.startX() + (i + 1) * (row.buttonWidth() + GAP));
            button.setY(row.y());
            button.setWidth(row.buttonWidth());
        }
    }

    private record RowLayout(int startX, int y, int buttonWidth) {
    }
}
