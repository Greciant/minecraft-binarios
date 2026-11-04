package com.awakenedmod.awakened.content.nuclear;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.LoopSound;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class HowitzerAbility extends Ability {
   private static final int UNLOCK = 6;
   private static final float COST = 20.0F;
   public static final int MAX_CHARGE_TICKS = 32;
   private static final double MIN_RANGE = 30.0;
   private static final double MAX_RANGE = 64.0;
   private static final double MIN_RADIUS = 3.0;
   private static final double MAX_RADIUS = 6.0;
   private static final float MIN_DAMAGE = 8.0F;
   private static final float MAX_DAMAGE = 22.0F;
   private static final double KNOCKBACK = 0.9;
   private static final double ORB_FORWARD = 0.6;
   private static final double ORB_DOWN = 0.55;
   private final Map<UUID, int[]> charging = this.activeState();

   public HowitzerAbility() {
      super("howitzer", 20.0F, 6);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boostsDamage().build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.charging.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         this.charging.put(player.getUUID(), new int[]{0});
         Fx.sound(player.serverLevel(), player.position(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6F, 0.5F);
         return true;
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      int[] state = this.charging.get(player.getUUID());
      if (state != null) {
         state[0] = Math.min(32, state[0] + 1);
         float f = (float)state[0] / 32.0F;
         LoopSound.keep(player, "howitzer_charge", SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.5F, 0.5F + f);
         if (state[0] >= 32) {
            this.charging.remove(player.getUUID());
            this.fire(player, 1.0F);
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      int[] state = this.charging.remove(player.getUUID());
      if (state != null) {
         this.fire(player, (float)state[0] / 32.0F);
      }
   }

   private void fire(ServerPlayer player, float f) {
      ServerLevel level = player.serverLevel();
      LoopSound.stop(player, "howitzer_charge");
      PowerService.playAnimation(player, "howitzer_fire");
      double range = lerp(30.0, 64.0, f);
      Vec3 muzzle = muzzle(player);
      Vec3 impact = firstImpact(player, range);
      NuclearFx.beam(level, muzzle, impact);
      double radius = lerp(3.0, 6.0, f);
      NuclearFx.boom(level, impact, radius, NuclearFx.Kind.COMBAT);
      NuclearFx.screenShake(level, impact, 18.0, 8 + (int)(f * 6.0F));
      Fx.sound(level, muzzle, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
      DamageSource src = Dmg.by(player, DamageTypes.MAGIC);
      boolean hit = false;
      LivingEntity threat = null;
      boolean killedAtFullCharge = false;
      boolean fullCharge = f >= 1.0F;

      for (LivingEntity victim : Harm.only(player, AimUtil.livingAround(player, impact, radius))) {
         Dmg.hurt(victim, level, src, MasteryScaling.scaledDamage(lerp(8.0F, 22.0F, f), player, this.balanceKey()));
         if (fullCharge && Foes.defeatedBy(victim, player)) {
            killedAtFullCharge = true;
         }

         Vec3 push = victim.position().subtract(impact);
         if (push.lengthSqr() < 1.0E-4) {
            push = new Vec3(0.0, 1.0, 0.0);
         }

         push = push.normalize();
         Harm.push(player, victim, push.x * 0.9, 0.45, push.z * 0.9);
         hit = true;
         if (threat == null && Foes.counts(player, victim)) {
            threat = victim;
         }
      }

      if (hit) {
         MasterySystem.award(player, MasteryReason.HIT, threat);
      }

      if (killedAtFullCharge) {
         QuestSystem.feat(player, "nuclear_direct_hit");
      }
   }

   private static Vec3 firstImpact(ServerPlayer player, double range) {
      Vec3 eye = player.getEyePosition();
      Vec3 blockHit = AimUtil.aimedPosition(player, range);
      Vec3 impact = blockHit;
      double bestSqr = eye.distanceToSqr(blockHit);
      AABB box = new AABB(eye, blockHit).inflate(1.0);

      for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class, box, x -> x != player && x.isAlive())) {
         Optional<Vec3> clip = e.getBoundingBox().inflate(0.3).clip(eye, blockHit);
         if (clip.isPresent()) {
            double d = eye.distanceToSqr(clip.get());
            if (d < bestSqr) {
               bestSqr = d;
               impact = clip.get();
            }
         }
      }

      return impact;
   }

   public static Vec3 muzzle(LivingEntity body) {
      return body.getEyePosition().add(body.getViewVector(1.0F).scale(0.6)).subtract(body.getUpVector(1.0F).scale(0.55));
   }

   private static double lerp(double a, double b, float f) {
      return a + (b - a) * (double)f;
   }

   private static float lerp(float a, float b, float f) {
      return a + (b - a) * f;
   }
}
