package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.power.Power;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class AbilityIcons {
   public static final int SIZE = 16;
   private static final Map<String, ResourceLocation> CACHE = new HashMap<>();

   private AbilityIcons() {
   }

   public static ResourceLocation of(String abilityId) {
      return CACHE.computeIfAbsent(abilityId, id -> AwId.of("textures/gui/ability/" + id + ".png"));
   }

   public static ResourceLocation of(Power power) {
      int slot = Mth.clamp(power.signatureSlot(), 0, power.abilityCount() - 1);
      return of(power.ability(slot).id());
   }

   public static void draw(GuiGraphics g, Power power, int x, int y, int size) {
      g.pose().pushPose();
      g.pose().translate((float)x, (float)y, 0.0F);
      g.pose().scale((float)size / 16.0F, (float)size / 16.0F, 1.0F);
      g.blit(of(power), 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
      g.pose().popPose();
   }
}
