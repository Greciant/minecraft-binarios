package com.awakenedmod.awakened.content.earth;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public final class GeoSeismic {
   private static final String ID = "geokinetic";
   private static final String VAR = "geo_sense";
   private static final double BASE_RANGE = 12.0;
   private static final double RANGE_PER_MASTERY = 1.5;

   private GeoSeismic() {
   }

   public static boolean active(PowerData data) {
      return data.var("geo_sense", 0.0F) > 0.5F;
   }

   public static void toggle(ServerPlayer player) {
      if (player != null && "geokinetic".equals(PowerService.data(player).powerId())) {
         PowerData data = PowerService.data(player);
         boolean now = !active(data);
         data.setVar("geo_sense", now ? 1.0F : 0.0F);
         player.displayClientMessage(Component.translatable(now ? "geokinetic.sense.on" : "geokinetic.sense.off"), true);
      }
   }

   public static void tick(ServerPlayer player, PowerData data) {
      if (active(data)) {
         ServerLevel level = player.serverLevel();
         double r = 12.0 + (double)data.masteryLevel() * 1.5;

         for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(r), ex -> ex != player && ex.isAlive())) {
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 10, 0, false, false, false));
         }
      }
   }
}
