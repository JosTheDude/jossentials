package gg.jos.jossentials.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class ColorUtil {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private ColorUtil() {
    }

    public static Component mini(String input) {
        if (input == null) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(input).decoration(TextDecoration.ITALIC, false);
    }
}
