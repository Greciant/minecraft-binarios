package com.devilfruits.fruits.kage_kage.shadow;

import com.devilfruits.fruits.kage_kage.KageKage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class Sunburn {
   public static boolean exposed(boolean day, boolean seesSky, boolean wet, float brightness) {
      return day && seesSky && !wet && brightness > 0.5F;
   }

   public static boolean exposed(LivingEntity e) {
      BlockPos eye = BlockPos.containing(e.getX(), e.getEyeY(), e.getZ());
      boolean wet = e.isInWaterRainOrBubble() || e.isInPowderSnow || e.wasInPowderSnow;
      return exposed(e.level().isDay(), e.level().canSeeSky(eye), wet, brightness(e));
   }

   private static float brightness(LivingEntity e) {
      BlockPos eye = BlockPos.containing(e.getX(), e.getEyeY(), e.getZ());
      return e.level().hasChunkAt(eye) ? e.level().getLightLevelDependentMagicValue(eye) : 0.0F;
   }

   public static void tick(LivingEntity e) {
      if (e.isAlive() && !e.fireImmune() && (!(e instanceof Player p) || !p.isCreative() && !p.isSpectator()) && exposed(e)) {
         float f = brightness(e);
         if (!(e.getRandom().nextFloat() * 30.0F >= (f - 0.4F) * 2.0F)) {
            ItemStack helmet = e.getItemBySlot(EquipmentSlot.HEAD);
            if (!helmet.isEmpty() && KageKage.flag("helmetProtects")) {
               if (helmet.isDamageableItem()) {
                  helmet.hurtAndBreak(e.getRandom().nextInt(2), e, EquipmentSlot.HEAD);
               }
            } else {
               e.igniteForSeconds((float)KageKage.setting("sunburnSeconds"));
            }
         }
      }
   }

   private Sunburn() {
   }
}
