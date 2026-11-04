package com.devilfruits.fruits.yomi_yomi;

import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.registry.DFAttachments;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public final class WaterRun {
   public static final int FLAG = FormFlags.fruit(1);
   public static final double CATCH_BELOW = 0.6;
   public static final double CATCH_ABOVE = 0.35;
   public static final int GRACE_TICKS = 10;
   private static final Map<Player, Long> LAST_RUN = new WeakHashMap<>();

   public static boolean form(Player player) {
      if (!YomiYomi.isUser(player)) {
         return false;
      } else {
         String form = DFAttachments.data(player).formName();
         return Vessel.SKELETON.formName().equals(form) || "soul_king".equals(form);
      }
   }

   public static double surface(Level level, double x, double y, double z) {
      for (int dy = 0; dy >= -1; dy--) {
         BlockPos pos = BlockPos.containing(x, y + (double)dy, z);
         FluidState fluid = level.getFluidState(pos);
         if (fluid.is(FluidTags.WATER) && !level.getFluidState(pos.above()).is(FluidTags.WATER)) {
            double top = (double)((float)pos.getY() + fluid.getHeight(level, pos));
            if (y >= top - 0.6 && y <= top + 0.35) {
               return top;
            }
         }
      }

      return Double.NaN;
   }

   public static boolean allowed(Player player) {
      if (form(player) && YomiYomi.canUse(player) && player.isSprinting() && !player.isPassenger() && !player.getAbilities().flying) {
         PlayerFruitData data = DFAttachments.data(player);
         if (data.energy() > 0.0F) {
            return true;
         } else {
            if (player instanceof ServerPlayer sp && AbilityManager.freeEnergy(sp)) {
               return true;
            }

            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean onWater(Player player) {
      if (!form(player) || player.isUnderWater()) {
         return false;
      } else if (FormFlags.has(player, YomiYomi.ID, FLAG)) {
         return true;
      } else {
         Long last = LAST_RUN.get(player);
         return last != null && player.level().getGameTime() - last <= 10L;
      }
   }

   public static void tick(ServerPlayer player) {
      boolean running = allowed(player) && !player.isUnderWater() && !Double.isNaN(surface(player.level(), player.getX(), player.getY(), player.getZ()));
      FormFlags.set(player, FLAG, running);
      if (running) {
         LAST_RUN.put(player, player.level().getGameTime());
         player.resetFallDistance();
         Vec3 v = player.getDeltaMovement();
         if (v.y < 0.0) {
            player.setDeltaMovement(v.x, 0.0, v.z);
         }

         if (!AbilityManager.freeEnergy(player)) {
            PlayerFruitData data = DFAttachments.data(player);
            data.setEnergy((float)((double)data.energy() - YomiYomi.setting("waterRunEnergy") / 20.0));
         }
      }
   }

   private WaterRun() {
   }
}
