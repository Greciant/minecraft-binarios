package com.awakenedmod.awakened.content.stormcaller;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class TeslaBeamAbility extends Ability {
   private static final float UPFRONT_COST = 6.0F;
   private static final int UNLOCK = 3;
   private static final float DRAIN_PER_TICK = 0.5F;
   static final double RANGE = 18.0;
   private static final double BEAM_RADIUS = 1.1;
   private static final int DAMAGE_INTERVAL = 4;
   private static final int VISUAL_INTERVAL = 3;
   private static final float DAMAGE_PER_PULSE = 4.0F;
   private static final int SYNC_INTERVAL = 5;
   private final Map<UUID, TeslaBeamAbility.Beam> beams = this.activeState();

   public TeslaBeamAbility() {
      super("tesla_beam", 6.0F, 3);
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      return 0.0F;
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(4.0).build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.beams.containsKey(player.getUUID())) {
         return true;
      } else if (!this.charge(player, 6.0F)) {
         return this.fail(player, "awakened.feedback.no_stamina", new Object[0]);
      } else {
         this.beams.put(player.getUUID(), new TeslaBeamAbility.Beam());
         TeslaVoice.start(player);
         PowerService.playAnimation(player, "tesla_channel");
         PowerService.sync(player);
         return true;
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      this.end(player);
   }

   @Override
   public void serverTick(ServerPlayer player) {
      TeslaBeamAbility.Beam beam = this.beams.get(player.getUUID());
      if (beam != null) {
         ServerLevel level = player.serverLevel();
         PowerData data = PowerService.data(player);
         if (!data.infiniteStamina()) {
            float remaining = StaminaSystem.drainSustained(data, level.getGameTime(), this.billed(0.5F));
            if (remaining <= 0.0F) {
               this.end(player);
               return;
            }

            if (player.tickCount % 5 == 0) {
               PowerService.sync(player);
            }
         }

         Vec3 eye = player.getEyePosition();
         Vec3 dir = player.getViewVector(1.0F).normalize();
         Vec3 far = eye.add(dir.scale(18.0));
         BlockHitResult hit = level.clip(new ClipContext(eye, far, Block.COLLIDER, Fluid.NONE, player));
         Vec3 end = hit.getType() != Type.MISS ? hit.getLocation() : far;
         int t = beam.tick++;
         if (t % 3 == 0) {
            drawBeam(level, AimUtil.handPosition(player, false), end);
            drawBeam(level, AimUtil.handPosition(player, true), end);
         }

         TeslaVoice.Cue cue = new TeslaVoice.Cue(player, level, eye, end, t, beam.clock);
         TeslaVoice.body(cue);
         if (t % 4 == 0) {
            float damage = MasteryScaling.scaledDamage(4.0F, player, this.balanceKey());
            boolean hitAny = false;
            LivingEntity threat = null;
            AABB box = new AABB(eye, end).inflate(1.6);

            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive() && Harm.allowed(player, e))) {
               double d = distToSegment(target.getBoundingBox().getCenter(), eye, end);
               if (d <= 1.1 + (double)target.getBbWidth() * 0.5) {
                  Dmg.hurt(target, level, Dmg.by(player, DamageTypes.LIGHTNING_BOLT), damage);
                  target.hurtMarked = true;
                  hitAny = true;
                  if (threat == null && Foes.counts(player, target)) {
                     threat = target;
                  }
               }
            }

            if (hitAny) {
               MasterySystem.award(player, MasteryReason.HIT, threat);
            }

            TeslaVoice.hit(cue, hitAny);
         }
      }
   }

   private void end(ServerPlayer player) {
      if (this.beams.remove(player.getUUID()) != null) {
         PowerService.stopAnimation(player);
         TeslaVoice.end(player);
         PowerService.sync(player);
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      if (this.beams.remove(player.getUUID()) != null) {
         PowerService.stopAnimation(player);
         TeslaVoice.silence(player);
      }

      super.clearPlayer(player);
   }

   private static void drawBeam(ServerLevel level, Vec3 a, Vec3 b) {
      StormFx.beam(level, a, b);
   }

   private static double distToSegment(Vec3 p, Vec3 a, Vec3 b) {
      Vec3 ab = b.subtract(a);
      double len2 = ab.lengthSqr();
      if (len2 < 1.0E-6) {
         return p.distanceTo(a);
      } else {
         double t = Math.max(0.0, Math.min(1.0, p.subtract(a).dot(ab) / len2));
         return p.distanceTo(a.add(ab.scale(t)));
      }
   }

   private static final class Beam {
      private int tick;
      private final TeslaVoice.Clock clock = new TeslaVoice.Clock();
   }
}
