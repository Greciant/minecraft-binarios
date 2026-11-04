package com.awakenedmod.awakened.content.chronokinetic;

import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class FlickerAbility extends Ability {
   private static final float COST = 7.0F;
   private static final int UNLOCK = 3;
   private static final double SPEED = 1.55;
   private static final double MIN_UP = 0.2;
   private static final double MOVE_THRESHOLD = 0.015;
   private static final int COYOTE_TICKS = 6;
   private static final int IFRAME_TICKS = 8;
   private static final float LOW_HEALTH = 6.0F;
   private final Map<UUID, Long> lastMoveTick = this.playerState();
   private final Map<UUID, Vec3> prevPos = this.playerState();

   public FlickerAbility() {
      super("flicker", 7.0F, 3);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boostsRange().build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      UUID id = player.getUUID();
      Long moved = this.lastMoveTick.get(id);
      boolean moving = moved != null && player.serverLevel().getGameTime() - moved <= 6L;
      if (!moving) {
         return this.fail(player, "awakened.chronokinetic.flicker_still", new Object[0]);
      } else {
         ServerLevel level = player.serverLevel();
         Vec3 dir = flatLook(player);
         double speed = MasteryScaling.scaled(1.55, player);
         double up = player.getLookAngle().y * speed + 0.2;
         Vec3 dash = new Vec3(dir.x * speed, up, dir.z * speed);
         player.setDeltaMovement(dash);
         player.hurtMarked = true;
         player.resetFallDistance();
         player.invulnerableTime = Math.max(player.invulnerableTime, 8);
         ChronoFx.flicker(level, player, ChronoFx.chest(player), dash.normalize());
         ChronoSounds.skip().play(level, player.position());
         if (player.getHealth() <= 6.0F) {
            MasterySystem.award(player, MasteryReason.SURVIVAL);
         }

         return true;
      }
   }

   private static Vec3 flatLook(ServerPlayer player) {
      Vec3 look = player.getLookAngle();
      Vec3 flat = new Vec3(look.x, 0.0, look.z);
      return flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
   }

   @Override
   public void serverTick(ServerPlayer player) {
      UUID id = player.getUUID();
      Vec3 cur = player.position();
      Vec3 prev = this.prevPos.put(id, cur);
      if (prev != null) {
         double dx = cur.x - prev.x;
         double dz = cur.z - prev.z;
         if (dx * dx + dz * dz >= 2.25E-4) {
            this.lastMoveTick.put(id, player.serverLevel().getGameTime());
         }
      }
   }
}
