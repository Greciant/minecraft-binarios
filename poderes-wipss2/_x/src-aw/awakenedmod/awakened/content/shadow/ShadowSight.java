package com.awakenedmod.awakened.content.shadow;

import com.awakenedmod.awakened.core.QuietEffect;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.PassiveToggle;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

public final class ShadowSight {
   public static final PassiveToggle TOGGLE = new PassiveToggle(
      "shadow_sight", "awakened.screen.setting_shadow_sight", "awakened.screen.setting_shadow_sight_desc", true
   );
   private static final int DURATION = 400;
   private static final int REFRESH_TICKS = 20;
   private static final QuietEffect SIGHT = new QuietEffect(MobEffects.NIGHT_VISION, 400, 20);

   private ShadowSight() {
   }

   public static void tick(ServerPlayer player, PowerData data) {
      if (TOGGLE.enabled(data)) {
         SIGHT.keep(player);
      } else {
         SIGHT.clear(player);
      }
   }

   public static void clear(ServerPlayer player) {
      SIGHT.clear(player);
   }
}
