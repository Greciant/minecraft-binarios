package de.miraculixx.showmyworld;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "showmyworld")
public class PreviewConfig implements ConfigData {
    @ConfigEntry.Gui.Tooltip
    public boolean show = true;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 2000)
    public int fadeMs = 400;
}
