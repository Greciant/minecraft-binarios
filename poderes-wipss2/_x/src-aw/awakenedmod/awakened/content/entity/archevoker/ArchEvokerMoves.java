package com.awakenedmod.awakened.content.entity.archevoker;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.entity.Telegraphs;
import com.awakenedmod.awakened.content.entity.ThreatMoves;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.Fx;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.phys.Vec3;

public final class ArchEvokerMoves {
   private static final ParticleOptions STAFF_MOTE = ParticleTypes.WITCH;
   private static final int FANG_COLOR = 12603647;
   private static final int BONE_COLOR = 16117984;
   public static final ArchEvokerMove TRIPLE_FANG_LINE = new ArchEvokerMove() {
      @Override
      public byte action() {
         return 1;
      }

      @Override
      public int windup() {
         return 22;
      }

      @Override
      public int recover() {
         return 16;
      }

      @Override
      public double minRange() {
         return 3.0;
      }

      @Override
      public double maxRange() {
         return 22.0;
      }

      @Override
      public int weight() {
         return 12;
      }

      @Override
      public int cooldownTicks() {
         return 50;
      }

      @Override
      public SoundEvent windupCue() {
         return SoundEvents.EVOKER_PREPARE_ATTACK;
      }

      @Override
      public SoundEvent commitCue() {
         return SoundEvents.EVOKER_CAST_SPELL;
      }

      public void windupTick(ArchEvokerEntity self, ServerLevel level, LivingEntity target, int tick) {
         Telegraphs.chargeMotes(level, ArchEvokerMoves.STAFF_MOTE, ArchEvokerMoves.staffTip(self), tick, self.currentWindup(), 1.4);
      }

      public void strike(ArchEvokerEntity self, ServerLevel level, LivingEntity target) {
         Vec3 origin = self.position();
         Vec3 aim = ArchEvokerMoves.aimOf(self, target);
         double base = Math.atan2(aim.z - origin.z, aim.x - origin.x);
         double baseY = target.getY();

         for (double off : new double[]{0.0, -0.62, 0.62}) {
            ArchEvokerMoves.fangLineAtAngle(level, self, origin, base + off, 9, 2.0, 1.6, 1, baseY);
         }

         Fx.sound(level, origin, SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 1.0F, 0.8F);
      }
   };
   public static final ArchEvokerMove FANG_WAVE = new ArchEvokerMove() {
      private final double[] radii = new double[]{3.0, 6.5, 10.0, 13.5};

      @Override
      public byte action() {
         return 2;
      }

      @Override
      public int windup() {
         return 26;
      }

      @Override
      public int recover() {
         return 18;
      }

      @Override
      public double minRange() {
         return 0.0;
      }

      @Override
      public double maxRange() {
         return 18.0;
      }

      @Override
      public int weight() {
         return 9;
      }

      @Override
      public int cooldownTicks() {
         return 90;
      }

      @Override
      public SoundEvent windupCue() {
         return SoundEvents.EVOKER_PREPARE_ATTACK;
      }

      @Override
      public SoundEvent commitCue() {
         return SoundEvents.EVOKER_CAST_SPELL;
      }

      public void windupTick(ArchEvokerEntity self, ServerLevel level, LivingEntity target, int tick) {
         Telegraphs.chargeMotes(level, ArchEvokerMoves.STAFF_MOTE, ArchEvokerMoves.staffTip(self), tick, self.currentWindup(), 1.6);
      }

      public void strike(ArchEvokerEntity self, ServerLevel level, LivingEntity target) {
         Vec3 c = self.position();
         double baseY = self.getY();

         for (int k = 0; k < this.radii.length; k++) {
            double radius = this.radii[k];
            int cnt = Mth.clamp((int)Math.round((Math.PI * 2) * radius / 2.8), 6, 12);
            ArchEvokerMoves.fangRing(level, self, c, radius, cnt, k * 5, baseY);
         }

         Vfx.ring(level, c.add(0.0, 0.1, 0.0), this.radii[this.radii.length - 1], 12603647, 16);
         Fx.sound(level, c, SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 1.3F, 0.6F);
      }
   };
   public static final ArchEvokerMove VEX_SUMMON = new ArchEvokerMove() {
      @Override
      public byte action() {
         return 3;
      }

      @Override
      public int windup() {
         return 30;
      }

      @Override
      public int recover() {
         return 20;
      }

      @Override
      public double minRange() {
         return 0.0;
      }

      @Override
      public double maxRange() {
         return 40.0;
      }

      @Override
      public int weight() {
         return 9;
      }

      @Override
      public int cooldownTicks() {
         return 420;
      }

      public boolean canStart(ArchEvokerEntity self, ServerLevel level, LivingEntity target) {
         return !self.inPhase2();
      }

      @Override
      public SoundEvent windupCue() {
         return SoundEvents.EVOKER_PREPARE_SUMMON;
      }

      @Override
      public SoundEvent commitCue() {
         return SoundEvents.EVOKER_CAST_SPELL;
      }

      public void windupTick(ArchEvokerEntity self, ServerLevel level, LivingEntity target, int tick) {
         Telegraphs.chargeMotes(level, ArchEvokerMoves.STAFF_MOTE, ArchEvokerMoves.staffTip(self), tick, self.currentWindup(), 1.6);
      }

      public void strike(ArchEvokerEntity self, ServerLevel level, LivingEntity target) {
         ArchEvokerMoves.summonVexes(self, level, 5);
      }
   };
   public static final ArchEvokerMove VEXPLOSIVES = new ArchEvokerMove() {
      @Override
      public byte action() {
         return 4;
      }

      @Override
      public int windup() {
         return 24;
      }

      @Override
      public int recover() {
         return 54;
      }

      @Override
      public double minRange() {
         return 5.0;
      }

      @Override
      public double maxRange() {
         return 40.0;
      }

      @Override
      public int weight() {
         return 9;
      }

      @Override
      public int cooldownTicks() {
         return 160;
      }

      public boolean canStart(ArchEvokerEntity self, ServerLevel level, LivingEntity target) {
         return self.inPhase2();
      }

      @Override
      public SoundEvent windupCue() {
         return SoundEvents.EVOKER_PREPARE_SUMMON;
      }

      public void windupTick(ArchEvokerEntity self, ServerLevel level, LivingEntity target, int tick) {
         Telegraphs.chargeMotes(level, ArchEvokerMoves.STAFF_MOTE, ArchEvokerMoves.staffTip(self), tick, self.currentWindup(), 1.5);
      }

      public void strike(ArchEvokerEntity self, ServerLevel level, LivingEntity target) {
         float frac = self.getHealth() / self.getMaxHealth();
         self.vexplodeShotsLeft = Mth.clamp(3 + (int)Math.floor((double)((1.0F - frac) * 5.0F)), 3, 8);
         self.vexplodeInterval = 6;
         ArchEvokerMoves.launchVexplode(self, level, target);
         self.vexplodeShotsLeft--;
      }

      public void recoverTick(ArchEvokerEntity self, ServerLevel level, @Nullable LivingEntity target, int ticksSinceStrike) {
         if (self.vexplodeShotsLeft > 0 && ticksSinceStrike % self.vexplodeInterval == 0) {
            ArchEvokerMoves.launchVexplode(self, level, target);
            self.vexplodeShotsLeft--;
         }
      }
   };
   public static final ArchEvokerMove EVOGATOR_SUMMON = new ArchEvokerMove() {
      @Override
      public byte action() {
         return 5;
      }

      @Override
      public int windup() {
         return 28;
      }

      @Override
      public int recover() {
         return 18;
      }

      @Override
      public double minRange() {
         return 0.0;
      }

      @Override
      public double maxRange() {
         return 40.0;
      }

      @Override
      public int weight() {
         return 11;
      }

      @Override
      public int cooldownTicks() {
         return 340;
      }

      public boolean canStart(ArchEvokerEntity self, ServerLevel level, LivingEntity target) {
         return self.inPhase2();
      }

      @Override
      public SoundEvent windupCue() {
         return SoundEvents.EVOKER_PREPARE_SUMMON;
      }

      public void windupTick(ArchEvokerEntity self, ServerLevel level, LivingEntity target, int tick) {
         Telegraphs.chargeMotes(level, ArchEvokerMoves.STAFF_MOTE, ArchEvokerMoves.staffTip(self), tick, self.currentWindup(), 1.5);
      }

      public void strike(ArchEvokerEntity self, ServerLevel level, LivingEntity target) {
         ArchEvokerMoves.summonEvogators(self, level, target, 2);
      }
   };
   public static final ArchEvokerMove CLONES = new ArchEvokerMove() {
      @Override
      public byte action() {
         return 6;
      }

      @Override
      public int windup() {
         return 24;
      }

      @Override
      public int recover() {
         return 16;
      }

      @Override
      public double minRange() {
         return 0.0;
      }

      @Override
      public double maxRange() {
         return 30.0;
      }

      @Override
      public int weight() {
         return 11;
      }

      @Override
      public int cooldownTicks() {
         return 380;
      }

      public boolean canStart(ArchEvokerEntity self, ServerLevel level, LivingEntity target) {
         return self.inPhase2();
      }

      @Override
      public SoundEvent windupCue() {
         return SoundEvents.EVOKER_PREPARE_SUMMON;
      }

      public void windupTick(ArchEvokerEntity self, ServerLevel level, LivingEntity target, int tick) {
         Telegraphs.chargeMotes(level, ArchEvokerMoves.STAFF_MOTE, ArchEvokerMoves.staffTip(self), tick, self.currentWindup(), 1.5);
      }

      public void strike(ArchEvokerEntity self, ServerLevel level, LivingEntity target) {
         ArchEvokerMoves.spawnClones(self, level, target, 2);
         ThreatMoves.blinkNear(self, target, 9.0, ParticleTypes.PORTAL, self::withinHome);
      }
   };
   public static final ArchEvokerMove GARGANTUAN_FANG = new ArchEvokerMove() {
      private static final double RADIUS = 3.2;

      @Override
      public byte action() {
         return 7;
      }

      @Override
      public int windup() {
         return 30;
      }

      @Override
      public int lookLockAt() {
         return 6;
      }

      @Override
      public int recover() {
         return 16;
      }

      @Override
      public double minRange() {
         return 4.0;
      }

      @Override
      public double maxRange() {
         return 24.0;
      }

      @Override
      public int weight() {
         return 8;
      }

      @Override
      public int cooldownTicks() {
         return 220;
      }

      @Override
      public double dangerRadius() {
         return 3.2;
      }

      @Override
      public boolean dangerAtTarget() {
         return true;
      }

      public boolean canStart(ArchEvokerEntity self, ServerLevel level, LivingEntity target) {
         return self.inPhase2();
      }

      @Override
      public SoundEvent windupCue() {
         return SoundEvents.EVOKER_PREPARE_ATTACK;
      }

      @Override
      public SoundEvent commitCue() {
         return SoundEvents.EVOKER_CAST_SPELL;
      }

      public void windupTick(ArchEvokerEntity self, ServerLevel level, LivingEntity target, int tick) {
         Telegraphs.chargeMotes(level, ArchEvokerMoves.STAFF_MOTE, ArchEvokerMoves.staffTip(self), tick, self.currentWindup(), 1.5);
         Vec3 aim = self.committedAim();
         if (aim != null) {
            Vec3 g = new Vec3(aim.x, target.getY() + 0.05, aim.z);
            Telegraphs.zoneMarker(level, g, 3.2, 12603647, self.currentWindup() - tick, self.currentWindup());
         }
      }

      public void strike(ArchEvokerEntity self, ServerLevel level, LivingEntity target) {
         Vec3 aim = ArchEvokerMoves.aimOf(self, target);
         Vec3 g = new Vec3(aim.x, target != null ? target.getY() : self.getY(), aim.z);
         Vfx.pillar(level, g, 1.4, 3.2, 16117984, 14);
         Vfx.dome(level, g.add(0.0, 0.2, 0.0), 3.0, 16117984, 12);
         Vfx.dustDome(level, g, 3.0, 16);
         ThreatMoves.groundErupt(level, self, g, 3.2, ArchEvokerMoves.scaled(self, 20.0F), 0.7);
         Fx.sound(level, g, SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 1.6F, 0.5F);
      }
   };
   public static final ArchEvokerMove REPULSE = new ArchEvokerMove() {
      @Override
      public byte action() {
         return 8;
      }

      @Override
      public int windup() {
         return 16;
      }

      @Override
      public int recover() {
         return 10;
      }

      @Override
      public double minRange() {
         return 0.0;
      }

      @Override
      public double maxRange() {
         return 9.0;
      }

      @Override
      public int weight() {
         return 14;
      }

      @Override
      public int cooldownTicks() {
         return 120;
      }

      public boolean canStart(ArchEvokerEntity self, ServerLevel level, LivingEntity target) {
         return !self.inPhase2();
      }

      @Override
      public double dangerRadius() {
         return 4.5;
      }

      @Override
      public SoundEvent windupCue() {
         return SoundEvents.EVOKER_PREPARE_ATTACK;
      }

      @Override
      public SoundEvent commitCue() {
         return SoundEvents.EVOKER_CAST_SPELL;
      }

      public void windupTick(ArchEvokerEntity self, ServerLevel level, LivingEntity target, int tick) {
         Telegraphs.chargeMotes(level, ArchEvokerMoves.STAFF_MOTE, ArchEvokerMoves.staffTip(self), tick, self.currentWindup(), 1.6);
      }

      public void strike(ArchEvokerEntity self, ServerLevel level, LivingEntity target) {
         Vec3 c = self.position();
         ArchEvokerMoves.fangRing(level, self, c, 4.0, 10, 4, self.getY());
         ThreatMoves.shockwave(level, self, 4.5, ArchEvokerMoves.scaled(self, 5.0F), 0.35, 0.12);
         Fx.sound(level, c, SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 1.2F, 0.7F);
      }
   };
   public static final List<ArchEvokerMove> KIT = List.of(
      TRIPLE_FANG_LINE, FANG_WAVE, VEX_SUMMON, REPULSE, VEXPLOSIVES, EVOGATOR_SUMMON, CLONES, GARGANTUAN_FANG
   );

   private ArchEvokerMoves() {
   }

   private static Vec3 aimOf(ArchEvokerEntity self, @Nullable LivingEntity target) {
      Vec3 a = self.committedAim();
      if (a != null) {
         return a;
      } else {
         return target != null ? target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0) : self.position().add(self.getLookAngle().scale(10.0));
      }
   }

   private static Vec3 staffTip(ArchEvokerEntity self) {
      return self.position().add(0.0, (double)self.getBbHeight() * 0.85, 0.0).add(self.getLookAngle().scale(0.5));
   }

   private static float scaled(ArchEvokerEntity self, float base) {
      return base * (1.0F + (float)(self.escalationPhase() - 1) * 0.25F);
   }

   public static void spawnFang(ServerLevel level, LivingEntity owner, double x, double y, double z, float yawRad, int warmup) {
      EvokerFangs fang = new EvokerFangs(level, x, y, z, yawRad, Math.max(0, warmup), owner);
      level.addFreshEntity(fang);
   }

   static void fangLineAtAngle(
      ServerLevel level, LivingEntity owner, Vec3 origin, double angle, int count, double startDist, double spacing, int warmupStep, double baseY
   ) {
      double cos = Math.cos(angle);
      double sin = Math.sin(angle);

      for (int i = 0; i < count; i++) {
         double d = startDist + (double)i * spacing;
         spawnFang(level, owner, origin.x + cos * d, baseY, origin.z + sin * d, (float)angle, i * warmupStep);
      }
   }

   public static void fangLineToward(ServerLevel level, LivingEntity owner, Vec3 origin, Vec3 aim, int count, double startDist, double spacing, int warmupStep) {
      double angle = Math.atan2(aim.z - origin.z, aim.x - origin.x);
      fangLineAtAngle(level, owner, origin, angle, count, startDist, spacing, warmupStep, aim.y);
   }

   static void fangRing(ServerLevel level, LivingEntity owner, Vec3 center, double radius, int count, int warmup, double baseY) {
      for (int i = 0; i < count; i++) {
         double a = (Math.PI * 2) / (double)count * (double)i;
         spawnFang(level, owner, center.x + Math.cos(a) * radius, baseY, center.z + Math.sin(a) * radius, (float)a, warmup);
      }
   }

   private static void summonVexes(ArchEvokerEntity self, ServerLevel level, int count) {
      for (int i = 0; i < count; i++) {
         Vex vex = new Vex(EntityType.VEX, level);
         if (vex != null) {
            double x = self.getX() + (self.getRandom().nextDouble() - 0.5) * 5.0;
            double z = self.getZ() + (self.getRandom().nextDouble() - 0.5) * 5.0;
            BlockPos spot = BlockPos.containing(x, self.getY() + 1.0, z);
            vex.moveTo(x, self.getY() + 1.0, z, self.getRandom().nextFloat() * 360.0F, 0.0F);
            vex.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.MOB_SUMMONED, null);
            vex.setOwner(self);
            vex.setBoundOrigin(spot);
            vex.setLimitedLife(600);
            level.addFreshEntityWithPassengers(vex);
         }
      }

      Fx.burst(level, ParticleTypes.WITCH, self.position().add(0.0, (double)self.getBbHeight() * 0.6, 0.0), 30, 1.0, 0.2);
      Fx.sound(level, self.position(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.2F, 0.9F);
   }

   private static void summonEvogators(ArchEvokerEntity self, ServerLevel level, @Nullable LivingEntity target, int count) {
      for (int i = 0; i < count; i++) {
         EvogatorEntity gator = new EvogatorEntity(AwakenedEntities.EVOGATOR.get(), level);
         if (gator != null) {
            double a = (Math.PI * 2) / (double)count * (double)i + self.getRandom().nextDouble();
            double x = self.getX() + Math.cos(a) * 2.0;
            double z = self.getZ() + Math.sin(a) * 2.0;
            gator.moveTo(x, self.getY(), z, (float)Math.toDegrees(a), 0.0F);
            gator.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(x, self.getY(), z)), MobSpawnType.MOB_SUMMONED, null);
            if (target != null) {
               gator.setTarget(target);
            }

            level.addFreshEntity(gator);
            Fx.burst(level, ParticleTypes.WITCH, gator.position().add(0.0, 0.5, 0.0), 16, 0.5, 0.15);
         }
      }

      Fx.sound(level, self.position(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.2F, 0.8F);
   }

   private static void spawnClones(ArchEvokerEntity self, ServerLevel level, @Nullable LivingEntity target, int count) {
      for (int i = 0; i < count; i++) {
         ArchEvokerCloneEntity clone = new ArchEvokerCloneEntity(AwakenedEntities.ARCHEVOKER_CLONE.get(), level);
         if (clone != null) {
            double a = (Math.PI * 2) / (double)(count + 1) * (double)(i + 1) + self.getRandom().nextDouble();
            double r = 3.0 + self.getRandom().nextDouble() * 2.0;
            double x = self.getX() + Math.cos(a) * r;
            double z = self.getZ() + Math.sin(a) * r;
            if (!self.withinHome(new Vec3(x, self.getY(), z))) {
               x = self.getX();
               z = self.getZ();
            }

            clone.moveTo(x, self.getY(), z, self.getYRot(), 0.0F);
            clone.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(x, self.getY(), z)), MobSpawnType.MOB_SUMMONED, null);
            if (target != null) {
               clone.setTarget(target);
            }

            level.addFreshEntity(clone);
            Fx.burst(level, ParticleTypes.WITCH, clone.position().add(0.0, 1.0, 0.0), 20, 0.6, 0.2);
         }
      }
   }

   private static void launchVexplode(ArchEvokerEntity self, ServerLevel level, @Nullable LivingEntity target) {
      VexplodeEntity v = new VexplodeEntity(AwakenedEntities.VEXPLODE.get(), level);
      Vec3 from = staffTip(self).add(self.getLookAngle().scale(0.6));
      Vec3 aim = target != null && target.isAlive() ? target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0) : aimOf(self, target);
      double dist = aim.subtract(from).length();
      Vec3 jitter = new Vec3((self.getRandom().nextDouble() - 0.5) * 2.0, self.getRandom().nextDouble() - 0.5, (self.getRandom().nextDouble() - 0.5) * 2.0)
         .scale(0.1 * dist);
      v.launch(self, from, aim.add(jitter), 0.85);
      level.addFreshEntity(v);
      Fx.sound(level, from, SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.0F, 1.2F);
   }
}
