package com.awakenedmod.awakened.client.screen.wheel;

import com.awakenedmod.awakened.client.screen.RadialWheelScreen;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public interface WheelSkin {
   void layout(int var1, int var2, int var3);

   int hover(double var1, double var3);

   void draw(GuiGraphics var1, Font var2, List<RadialWheelScreen.RadialOption> var3, int var4);
}
