package com.frostsmp.autogg;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "autogg")
public final class AutoGgConfig implements ConfigData {

    @ConfigEntry.Category("command")
    @ConfigEntry.Gui.Tooltip(count = 5)
    public String commandTemplate = "/msg {player} {message}";

    @ConfigEntry.Category("command")
    @ConfigEntry.Gui.Tooltip
    public String message = "GG";

    @ConfigEntry.Category("command")
    @ConfigEntry.Gui.Tooltip(count = 2)
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public SendMode sendMode = SendMode.COMMAND;

    @ConfigEntry.Category("targeting")
    @ConfigEntry.Gui.Tooltip(count = 2)
    @ConfigEntry.BoundedDiscrete(min = 0, max = 600)
    public int hitMemorySeconds = 120;

    @ConfigEntry.Category("targeting")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 256)
    public int maxDistance = 0;

    @ConfigEntry.Category("targeting")
    @ConfigEntry.Gui.Tooltip
    public boolean requirePlayerOnline = false;

    @ConfigEntry.Category("targeting")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean allowLastAttackerFallback = true;

    @ConfigEntry.Category("targeting")
    @ConfigEntry.Gui.Tooltip
    public double spearLateralMax = 1.0;

    @ConfigEntry.Category("antispam")
    @ConfigEntry.Gui.Tooltip
    public boolean antiSpamEnabled = true;

    @ConfigEntry.Category("antispam")
    @ConfigEntry.Gui.Tooltip(count = 2)
    @ConfigEntry.BoundedDiscrete(min = 0, max = 60)
    public int cooldownSeconds = 3;

    @ConfigEntry.Category("antispam")
    @ConfigEntry.Gui.Tooltip(count = 2)
    @ConfigEntry.BoundedDiscrete(min = 0, max = 10)
    public int maxSendsPerHit = 1;

    @ConfigEntry.Category("feedback")
    public boolean feedbackEnabled = true;

    public enum SendMode {
        COMMAND,
        CHAT
    }
}
