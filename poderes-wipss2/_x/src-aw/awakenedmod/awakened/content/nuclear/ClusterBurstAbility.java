package com.awakenedmod.awakened.content.nuclear;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class ClusterBurstAbility extends Ability {
   private static final float COST = 14.0F;
   private static final int SPRAY_TICKS = 5;
   private static final int BLASTS_PER_TICK = 2;
   private static final double CONE_LENGTH = 9.0;
   private static final double CONE_HALF_ANGLE = Math.toRadians(34.0);
   private static final double BLAST_RADIUS = 2.2;
   private static final float BLAST_DAMAGE = 5.0F;
   private static final double KNOCKBACK = 0.55;
   private final Map<UUID, int[]> spraying = this.activeState();
   private final Map<UUID, Set<UUID>> swept = this.playerState();

   public ClusterBurstAbility() {
      super("cluster_burst", 14.0F, 0);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(5.0).build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.spraying.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         this.spraying.put(player.getUUID(), new int[]{5, player.tickCount});
         this.swept.put(player.getUUID(), new HashSet<>());
         Fx.sound(player.serverLevel(), player.position(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 0.8F);
         PowerService.playAnimation(player, "cluster_burst");
         return true;
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      int[] state = this.spraying.get(player.getUUID());
      if (state != null) {
         ServerLevel level = player.serverLevel();
         Vec3 eye = player.getEyePosition();
         Vec3 look = player.getLookAngle();
         Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0));
         if (right.lengthSqr() < 1.0E-4) {
            right = new Vec3(1.0, 0.0, 0.0);
         }

         right = right.normalize();
         Vec3 up = right.cross(look).normalize();
         int frame = 5 - state[0];
         DamageSource src = Dmg.by(player, DamageTypes.MAGIC);
         Set<UUID> caught = this.swept.get(player.getUUID());
         boolean hit = false;
         LivingEntity threat = null;

         for (int i = 0; i < 2; i++) {
            int seed = state[1] * 131 + frame * 17 + i * 7;
            double yaw = rand(seed) * CONE_HALF_ANGLE;
            double pitch = rand(seed + 3) * CONE_HALF_ANGLE * 0.7;
            double dist = 3.0 + (rand(seed + 5) * 0.5 + 0.5) * 6.0;
            Vec3 dir = look.add(right.scale(Math.tan(yaw))).add(up.scale(Math.tan(pitch))).normalize();
            Vec3 point = eye.add(dir.scale(dist));
            NuclearFx.boom(level, point, 2.2, NuclearFx.Kind.CLUSTER);

            for (LivingEntity victim : AimUtil.livingAround(player, point, 2.2)) {
               Dmg.hurt(victim, level, src, MasteryScaling.scaledDamage(5.0F, player, this.balanceKey()));
               Vec3 push = victim.position().subtract(player.position());
               if (push.lengthSqr() < 1.0E-4) {
                  push = look;
               }

               push = push.normalize();
               Harm.push(player, victim, push.x * 0.55, 0.3, push.z * 0.55);
               hit = true;
               if (Foes.counts(player, victim)) {
                  if (threat == null) {
                     threat = victim;
                  }

                  if (caught != null) {
                     caught.add(victim.getUUID());
                  }
               }
            }
         }

         if (hit) {
            MasterySystem.award(player, MasteryReason.HIT, threat);
         }

         if (--state[0] <= 0) {
            this.spraying.remove(player.getUUID());
            this.creditSweep(player);
         }
      }
   }

   private void creditSweep(ServerPlayer player) {
      Set<UUID> caught = this.swept.remove(player.getUUID());
      if (caught != null && caught.size() >= 3) {
         QuestSystem.feat(player, "nuclear_cluster_sweep");
      }
   }

   private static double rand(int seed) {
      int h = seed * -1640531535;
      h ^= h >>> 15;
      h *= -2048144777;
      h ^= h >>> 13;
      return (double)(h >>> 8 & 65535) / 32768.0 - 1.0;
   }
}
