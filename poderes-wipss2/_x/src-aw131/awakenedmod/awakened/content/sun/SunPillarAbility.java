package com.awakenedmod.awakened.content.sun;

import com.awakenedmod.awakened.content.AwakenedParticles;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class SunPillarAbility extends Ability {
   private static final float COST = 28.0F;
   private static final int UNLOCK = 5;
   private static final int DURATION = 200;
   private static final int PULSE = 6;
   private static final int PILLAR_REFRESH = 4;
   private static final int PILLAR_LIFE = 26;
   private static final double RANGE = 30.0;
   private static final float DENSITY = 1.7F;
   private final Map<UUID, List<SunPillarAbility.Zone>> zones = this.playerState();

   public SunPillarAbility() {
      super("sun_pillar", 28.0F, 5);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boostsDamage().build();
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      List<SunPillarAbility.Zone> active = this.zones.get(player.getUUID());
      if (active != null && !active.isEmpty()) {
         return this.fail(player, "awakened.feedback.sun_pillar_active", new Object[0]);
      } else {
         ServerLevel level = player.serverLevel();
         Vec3 aim = AimUtil.aimedPosition(player, 30.0);
         Vec3 base = AimUtil.groundedPosition(level, aim);
         PowerData data = PowerService.data(player);
         float m = Solar.scale(data, 0.5F, 1.5F);
         boolean axe = Solar.hasAxe(player);
         double radius = (axe ? 5.5 : 4.2) * (0.45 + 0.6 * (double)m);
         double height = (axe ? 36.0 : 26.0) * (0.7 + 0.5 * (double)m);
         float dmg = MasteryScaling.scaledDamage(axe ? 3.5F : 2.2F, player, this.balanceKey(), m - 1.0F);
         int burnTicks = Math.round((float)(axe ? 4 : 3) * m) * 20;
         this.zones.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(new SunPillarAbility.Zone(base, radius, height, dmg, burnTicks));
         this.erupt(level, player, base, radius, height, axe);
         return true;
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      List<SunPillarAbility.Zone> list = this.zones.get(player.getUUID());
      if (list != null) {
         ServerLevel level = player.serverLevel();
         Iterator<SunPillarAbility.Zone> it = list.iterator();

         while (it.hasNext()) {
            SunPillarAbility.Zone z = it.next();
            if (z.ticks-- <= 0) {
               it.remove();
            } else {
               this.renderPillar(level, z);
               if (z.ticks % 6 == 0) {
                  this.scorch(player, level, z);
               }
            }
         }

         if (list.isEmpty()) {
            this.zones.remove(player.getUUID());
         }
      }
   }

   private void scorch(ServerPlayer player, ServerLevel level, SunPillarAbility.Zone z) {
      for (LivingEntity e : AimUtil.livingAround(player, z.base.add(0.0, 1.5, 0.0), z.radius)) {
         if (e != player) {
            Dmg.hurt(e, level, Dmg.by(player, DamageTypes.MAGIC), z.dmg);
            if (z.burnTicks > 0 && Harm.allowed(player, e)) {
               e.setRemainingFireTicks(z.burnTicks);
            }
         }
      }
   }

   private void renderPillar(ServerLevel level, SunPillarAbility.Zone z) {
      RandomSource rnd = level.getRandom();
      int elapsed = 200 - z.ticks;
      if (elapsed % 4 == 0) {
         pillar(level, z.base, z.radius * 0.9, z.height, 16761390, 26);
         pillar(level, z.base, z.radius * 0.7, z.height * 1.04, 16720912, 26);
         pillar(level, z.base, z.radius * 0.45, z.height * 1.05, 16720912, 26);
         pillar(level, z.base, z.radius * 0.16, z.height * 1.08, 16770688, 26);
      }

      if (elapsed % 16 == 0) {
         Vfx.ring(level, z.base.add(0.0, 0.1, 0.0), z.radius * 1.3, 16761390, 18);
      }

      int motes = 6 + (int)(z.radius * 4.0);

      for (int i = 0; i < motes; i++) {
         double ang = rnd.nextDouble() * Math.PI * 2.0;
         double rr = z.radius * 0.85 * Math.sqrt(rnd.nextDouble());
         double px = z.base.x + Math.cos(ang) * rr;
         double pz = z.base.z + Math.sin(ang) * rr;
         double py = z.base.y + rnd.nextDouble() * z.height * 0.6;
         double up = 0.25 + rnd.nextDouble() * 0.5;
         level.sendParticles(AwakenedParticles.SUN_MOTE.get(), px, py, pz, 0, 0.0, up, 0.0, 1.0);
      }

      if (elapsed % 20 == 0) {
         Fx.sound(level, z.base, SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 0.8F, 0.7F);
      }
   }

   private void erupt(ServerLevel level, ServerPlayer player, Vec3 base, double radius, double height, boolean axe) {
      pillar(level, base, radius, height, 16761390, 30);
      pillar(level, base, radius * 0.7, height * 1.06, 16720912, 32);
      pillar(level, base, radius * 0.45, height * 1.08, 16720912, 32);
      pillar(level, base, radius * 0.18, height * 1.1, 16770688, 32);
      Vfx.sphere(level, base.add(0.0, 0.6, 0.0), radius * 1.2, 16720912, 14, 1.4F);
      Vfx.ring(level, base.add(0.0, 0.1, 0.0), radius * 2.0, 16761390, 18);
      Fx.burst(level, (ParticleOptions)AwakenedParticles.SUN_MOTE.get(), base.add(0.0, 0.5, 0.0), 50, radius * 0.8, 0.35);
      if (axe) {
         Fx.burst(level, (ParticleOptions)AwakenedParticles.SUN_MOTE.get(), AimUtil.handPosition(player), 12, 0.3, 0.06);
      }

      Fx.sound(level, base, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.3F, 0.6F);
      Fx.sound(level, base, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9F, 0.7F);
      Fx.sound(level, player.position(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.5F);
   }

   private static void pillar(ServerLevel level, Vec3 base, double radius, double height, int color, int lifetime) {
      Vfx.effect(VfxEntity.Kind.PILLAR, base).scale(radius).secondary(new Vec3(0.0, height, 0.0)).color(color).lifetime(lifetime).intensity(1.7F).spawn(level);
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return "sun_pillar";
   }

   private static final class Zone {
      final Vec3 base;
      final double radius;
      final double height;
      final float dmg;
      final int burnTicks;
      int ticks;

      Zone(Vec3 base, double radius, double height, float dmg, int burnTicks) {
         this.base = base;
         this.radius = radius;
         this.height = height;
         this.dmg = dmg;
         this.burnTicks = burnTicks;
         this.ticks = 200;
      }
   }
}
