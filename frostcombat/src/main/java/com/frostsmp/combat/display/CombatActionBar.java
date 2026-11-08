package com.frostsmp.combat.display;

import com.frostsmp.combat.config.PluginConfig;
import com.frostsmp.combat.util.MessageUtil;
import net.kyori.adventure.text.Component;

public final class CombatActionBar {

    private static final String WORD = "COMBAT";

    private final PluginConfig config;

    public CombatActionBar(PluginConfig config) {
        this.config = config;
    }

    public Component build(int animationFrame, int secondsRemaining) {
        int highlightIndex = (animationFrame / config.getTicksPerLetter()) % WORD.length();
        StringBuilder mini = new StringBuilder();
        mini.append("<dark_gray>[</dark_gray><shadow:#000000>");
        for (int i = 0; i < WORD.length(); i++) {
            char letter = WORD.charAt(i);
            if (i == highlightIndex) {
                mini.append("<white><bold>").append(letter).append("</bold></white>");
            } else {
                mini.append("<red><bold>").append(letter).append("</bold></red>");
            }
        }
        mini.append("</shadow><dark_gray>]</dark_gray> ");
        mini.append("<gray>").append(Math.max(0, secondsRemaining)).append("s</gray>");
        return MessageUtil.parse(mini.toString());
    }
}
