package de.farbrisus.zoomi.owo;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Modmenu;
import io.wispforest.owo.config.annotation.RangeConstraint;

@Modmenu(modId = "zoomi")
@Config(name = "Zoomi", wrapperName = "ZoomiConfig")
public class Configuration {
    public boolean SmoothCamera = false;
    @RangeConstraint(min = 0.1f, max = 1.0f)
    public double DefaultZoomLevel = 0.2f;
    public double DefaultZoomLevel_NEW = 20;
    public boolean ShowKeyConflict = true;
}
