package com.awakenedmod.awakened.content.shadow;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ShadowDark {
   public static final int MELD_EJECT_LIGHT = 8;

   private ShadowDark() {
   }

   public static float darkness(Level level, BlockPos pos) {
      int light = level.getMaxLocalRawBrightness(pos);
      return Mth.clamp(1.0F - (float)light / 15.0F, 0.0F, 1.0F);
   }

   public static float darkness(Level level, Vec3 pos) {
      return darkness(level, BlockPos.containing(pos));
   }

   public static float darkness(ServerPlayer player) {
      return Mth.clamp(1.0F - (float)lightAt(player) / 15.0F, 0.0F, 1.0F);
   }

   public static int lightAt(Entity body) {
      Level level = body.level();
      return Math.max(level.getMaxLocalRawBrightness(body.blockPosition()), level.getMaxLocalRawBrightness(BlockPos.containing(body.getEyePosition())));
   }

   public static float scale(ServerPlayer player, float dim, float dark) {
      return Mth.lerp(darkness(player), dim, dark);
   }

   public static float scaleAt(Level level, Vec3 pos, float dim, float dark) {
      return Mth.lerp(darkness(level, pos), dim, dark);
   }
}
