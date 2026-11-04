package com.awakenedmod.awakened.content.command.vocab.trigger;

import com.awakenedmod.awakened.content.command.CommandSupport;
import com.awakenedmod.awakened.content.command.vocab.Posture;
import com.awakenedmod.awakened.content.command.vocab.Trigger;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class WhileStandingStillTrigger implements Trigger {
   private static final double STILL_SPEED = 0.01;
   public static final int SETTLE_TICKS = 30;
   private static final Map<UUID, WhileStandingStillTrigger.Rest> REST = new ConcurrentHashMap<>();
   private static final Map<UUID, long[]> CLIENT_REST = new ConcurrentHashMap<>();
   private static final int CLIENT_STALE_TICKS = 10;

   @Override
   public String id() {
      return "while_standing_still";
   }

   @Override
   public int unlockLevel() {
      return 5;
   }

   @Override
   public boolean live(ServerPlayer caster) {
      return count(caster) >= 30;
   }

   @Override
   public void observe(ServerPlayer caster) {
      count(caster);
   }

   private static int count(ServerPlayer caster) {
      long now = caster.level().getGameTime();
      WhileStandingStillTrigger.Rest rest = REST.computeIfAbsent(
         caster.getUUID(), id -> new WhileStandingStillTrigger.Rest(now - 2L, caster.getX(), caster.getZ())
      );
      if (rest.tick == now) {
         return rest.still;
      } else {
         double dx = caster.getX() - rest.x;
         double dz = caster.getZ() - rest.z;
         boolean stillNow = !CommandSupport.movementInput(caster) && dx * dx + dz * dz < 1.0E-4;
         boolean consecutive = now - rest.tick == 1L;
         rest.tick = now;
         rest.x = caster.getX();
         rest.z = caster.getZ();
         rest.still = !stillNow ? 0 : Math.min(30, consecutive ? rest.still + 1 : 1);
         return rest.still;
      }
   }

   @Override
   public boolean liveOnClient(Player caster) {
      Vec3 motion = caster.getDeltaMovement();
      boolean movingNow = Math.sqrt(motion.x * motion.x + motion.z * motion.z) >= 0.01;
      long now = caster.level().getGameTime();
      if (CLIENT_REST.size() > 32) {
         CLIENT_REST.clear();
      }

      long[] rest = CLIENT_REST.computeIfAbsent(caster.getUUID(), id -> new long[]{now, now});
      if (movingNow || now - rest[0] > 10L) {
         rest[1] = now;
      }

      rest[0] = now;
      return now - rest[1] >= 30L;
   }

   @Override
   public void forget(UUID casterId) {
      REST.remove(casterId);
      CLIENT_REST.remove(casterId);
   }

   @Override
   public void forgetAll() {
      REST.clear();
      CLIENT_REST.clear();
   }

   @Override
   public Set<Posture> postures() {
      return Set.of(Posture.STILL);
   }

   private static final class Rest {
      long tick;
      int still;
      double x;
      double z;

      Rest(long tick, double x, double z) {
         this.tick = tick;
         this.x = x;
         this.z = z;
      }
   }
}
