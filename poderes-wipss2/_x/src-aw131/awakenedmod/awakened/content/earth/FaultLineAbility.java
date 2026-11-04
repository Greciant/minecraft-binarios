package com.awakenedmod.awakened.content.earth;

import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.Terrain;
import com.awakenedmod.awakened.core.net.FaultSteerPayload;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class FaultLineAbility extends Ability {
   private static final float DAMAGE = 9.0F;
   private static final float KNOCK_UP_BASE = 0.7F;
   private static final float KNOCK_BACK = 0.5F;
   private static final double STEP = 1.1;
   private static final double START_OFFSET = 1.0;
   private static final int STEPS = 12;
   private static final double STEER_RATE = 0.35;
   private static final double SPIKE_W = 1.0;
   private static final double SPIKE_H = 1.7;
   private static final int SPIKE_LIFE = 22;
   private static final double HIT_RADIUS = 1.6;
   private static final int CLIMB = 2;
   private static final int DROP = 4;
   private static final int CROWN_SPIKES = 8;
   private static final double CROWN_RADIUS = 2.6;
   private static final double CROWN_H = 2.6;
   private static final int CROWN_LIFE = 30;
   private static final float CROWN_DAMAGE = 12.0F;
   private static final float CROWN_UP = 0.85F;
   private static final double CROWN_BACK = 0.7;
   private final Map<UUID, FaultLineAbility.Wave> waves = this.playerState();

   public FaultLineAbility() {
      super("fault_line", 28.0F, 5);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(9.0).build();
   }

   @Override
   public boolean requiresFootingWhenRealistic() {
      return true;
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      Vec3 look = player.getLookAngle();
      Vec3 dir = new Vec3(look.x, 0.0, look.z);
      dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : dir.normalize();
      EarthMaterial mat = EarthMaterial.under(player);
      int steps = Math.round(MasteryScaling.scaled(12.0F, player));
      boolean steered = player.isShiftKeyDown();
      Vec3 ahead = player.position().add(dir.x * 1.0, 0.0, dir.z * 1.0);
      double originY = Terrain.floorNear(player.serverLevel(), ahead.x, ahead.z, player.getY(), 2, 4);
      Vec3 origin = Double.isNaN(originY) ? ahead : new Vec3(ahead.x, originY, ahead.z);
      FaultLineAbility.Wave prev = this.waves.put(player.getUUID(), new FaultLineAbility.Wave(origin, dir, mat, steps, steered));
      if (prev != null && prev.steered && !steered) {
         setSteerClient(player, false);
      }

      if (steered) {
         setSteerClient(player, true);
      }

      EarthFx.faultStomp(player.serverLevel(), player.position(), mat);
      return true;
   }

   @Override
   public void serverTick(ServerPlayer player) {
      FaultLineAbility.Wave w = this.waves.get(player.getUUID());
      if (w != null) {
         ServerLevel level = player.serverLevel();
         if (++w.step > w.steps) {
            this.endWave(player, w);
         } else {
            if (w.steered) {
               steerToward(player, w);
            }

            Vec3 prev = w.front;
            double fx = prev.x + w.dir.x * 1.1;
            double fz = prev.z + w.dir.z * 1.1;
            double fy = Terrain.floorNear(level, fx, fz, prev.y, 2, 4);
            if (Double.isNaN(fy)) {
               this.endWave(player, w);
            } else {
               Vec3 front = new Vec3(fx, fy, fz);
               w.front = front;
               Block block = w.mat.state().getBlock();
               EarthFx.faultTear(level, prev, front, w.mat);
               double hMain = 1.7 + level.random.nextDouble() * 1.0;
               StoneConstructEntity.spawn(level, front, block, 1.0F, (float)hMain, 22, level.random.nextFloat() * 90.0F);
               EarthFx.faultSpike(level, front, w.mat);
               Vec3 side = new Vec3(-w.dir.z, 0.0, w.dir.x);

               for (int s = -1; s <= 1; s += 2) {
                  double off = 0.5 + level.random.nextDouble() * 0.4;
                  double px = fx + side.x * off * (double)s;
                  double pz = fz + side.z * off * (double)s;
                  double hShard = 0.85 + level.random.nextDouble() * 0.7;
                  double wShard = 1.0 * (0.5 + level.random.nextDouble() * 0.4);
                  double py = Terrain.floorNear(level, px, pz, fy, 1, 2);
                  if (!Double.isNaN(py)) {
                     StoneConstructEntity.spawn(level, new Vec3(px, py, pz), block, (float)wShard, (float)hShard, 22, level.random.nextFloat() * 90.0F, false);
                  }
               }

               for (LivingEntity e : level.getEntitiesOfClass(
                  LivingEntity.class, new AABB(front, front).inflate(1.6, 2.0, 1.6), ex -> ex != player && ex.isAlive() && Harm.allowed(player, ex)
               )) {
                  if (w.hit.add(e.getUUID())) {
                     boolean dealt = Dmg.hurt(e, level, Dmg.by(player, DamageTypes.FALLING_BLOCK), MasteryScaling.scaledDamage(9.0F, player, this.balanceKey()));
                     double up = Math.max(e.getDeltaMovement().y, 0.7F * w.mat.liftFactor());
                     e.setDeltaMovement(e.getDeltaMovement().x + w.dir.x * 0.5, up, e.getDeltaMovement().z + w.dir.z * 0.5);
                     e.hurtMarked = true;
                     w.mat.stagger(player, e);
                     if (dealt) {
                        MasterySystem.award(player, MasteryReason.HIT, e);
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public void onSecondaryAction(ServerPlayer player, int action) {
      if (action == 6) {
         FaultLineAbility.Wave w = this.waves.get(player.getUUID());
         if (w != null && w.steered) {
            this.detonate(player, w);
            this.endWave(player, w);
         }
      }
   }

   private void detonate(ServerPlayer player, FaultLineAbility.Wave w) {
      ServerLevel level = player.serverLevel();
      Vec3 c = w.front;
      Block block = w.mat.state().getBlock();

      for (int i = 0; i < 8; i++) {
         double a = (Math.PI * 2) * (double)i / 8.0;
         double px = c.x + Math.cos(a) * 2.6;
         double pz = c.z + Math.sin(a) * 2.6;
         double h = 2.6 + level.random.nextDouble() * 0.8;
         double py = Terrain.floorNear(level, px, pz, c.y, 2, 4);
         if (!Double.isNaN(py)) {
            StoneConstructEntity.spawn(level, new Vec3(px, py, pz), block, 1.0F, (float)h, 30, level.random.nextFloat() * 90.0F);
         }
      }

      EarthFx.faultCrown(level, c, w.mat);

      for (LivingEntity e : level.getEntitiesOfClass(
         LivingEntity.class, new AABB(c, c).inflate(3.6, 2.5, 3.6), ex -> ex != player && ex.isAlive() && Harm.allowed(player, ex)
      )) {
         boolean dealt = Dmg.hurt(e, level, Dmg.by(player, DamageTypes.FALLING_BLOCK), MasteryScaling.scaledDamage(12.0F, player, this.balanceKey()));
         Vec3 away = e.position().subtract(c);
         away = away.lengthSqr() > 1.0E-4 ? away.normalize() : w.dir;
         double up = Math.max(e.getDeltaMovement().y, 0.85F * w.mat.liftFactor());
         e.setDeltaMovement(away.x * 0.7, up, away.z * 0.7);
         e.hurtMarked = true;
         w.mat.stagger(player, e);
         if (dealt) {
            MasterySystem.award(player, MasteryReason.HIT, e);
         }
      }
   }

   private static void steerToward(ServerPlayer player, FaultLineAbility.Wave w) {
      Vec3 look = player.getLookAngle();
      Vec3 want = new Vec3(look.x, 0.0, look.z);
      if (!(want.lengthSqr() < 1.0E-4)) {
         want = want.normalize();
         Vec3 bent = w.dir.add(want.subtract(w.dir).scale(0.35));
         if (bent.lengthSqr() > 1.0E-4) {
            w.dir = bent.normalize();
         }
      }
   }

   private void endWave(ServerPlayer player, FaultLineAbility.Wave w) {
      this.waves.remove(player.getUUID());
      if (w.steered) {
         setSteerClient(player, false);
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      FaultLineAbility.Wave w = this.waves.get(player.getUUID());
      if (w != null && w.steered) {
         setSteerClient(player, false);
      }

      super.clearPlayer(player);
   }

   private static void setSteerClient(ServerPlayer player, boolean live) {
      PacketDistributor.sendToPlayer(player, new FaultSteerPayload(live), new CustomPacketPayload[0]);
   }

   private static final class Wave {
      final Vec3 origin;
      Vec3 dir;
      Vec3 front;
      final EarthMaterial mat;
      final int steps;
      final boolean steered;
      final Set<UUID> hit = new HashSet<>();
      int step;

      Wave(Vec3 origin, Vec3 dir, EarthMaterial mat, int steps, boolean steered) {
         this.origin = origin;
         this.dir = dir;
         this.front = origin;
         this.mat = mat;
         this.steps = steps;
         this.steered = steered;
      }
   }
}
