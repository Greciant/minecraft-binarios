package com.devilfruits.fruits.uo_uo_seiryu;

import com.devilfruits.core.form.FormFlags;
import com.devilfruits.core.form.FormManager;
import com.devilfruits.fruits.uo_uo_seiryu.entity.SeiryuDragonEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class DragonShells {
   public static final int SLITHER = FormFlags.fruit(0);
   public static final int TWISTING = FormFlags.fruit(1);
   private static final Map<UUID, SeiryuDragonEntity> SHELLS = new HashMap<>();

   public static void tick(ServerPlayer player) {
      SeiryuDragonEntity shell = SHELLS.get(player.getUUID());
      if (shell != null && (shell.isRemoved() || shell.level() != player.level())) {
         SHELLS.remove(player.getUUID());
         shell = null;
      }

      if (UoUoSeiryu.isIn(player, "dragon") && player.isAlive()) {
         FormFlags.set(player, 2, !FormFlags.has(player, SLITHER));
         if (player.tickCount % 5 == 0 && !SeiryuRules.fits(player.level(), player.getBoundingBox())) {
            Optional<Vec3> room = SeiryuRules.findDragonRoom(player);
            if (!room.isPresent()) {
               FormManager.revert(player, FormManager.Reason.CONDITION);
               return;
            }

            Vec3 at = room.get();
            player.teleportTo(at.x, at.y, at.z);
         }

         if (shell == null) {
            shell = SeiryuDragonEntity.create(player);
            player.serverLevel().addFreshEntity(shell);
            SHELLS.put(player.getUUID(), shell);
         }
      }
   }

   @Nullable
   public static SeiryuDragonEntity get(ServerPlayer player) {
      SeiryuDragonEntity shell = SHELLS.get(player.getUUID());
      return shell != null && !shell.isRemoved() ? shell : null;
   }

   public static void forget(UUID player) {
      SeiryuDragonEntity shell = SHELLS.remove(player);
      if (shell != null && !shell.isRemoved()) {
         shell.discard();
      }
   }

   private DragonShells() {
   }
}
