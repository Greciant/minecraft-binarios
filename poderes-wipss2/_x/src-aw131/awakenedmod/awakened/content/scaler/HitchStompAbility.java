package com.awakenedmod.awakened.content.scaler;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class HitchStompAbility extends Ability {
   private static final int UNLOCK = 6;
   private static final float HITCH_COST = 4.0F;
   private static final float STOMP_COST = 9.0F;
   private static final double HITCH_RANGE = 8.0;
   private static final double HITCH_CONE_DEG = 12.0;
   private static final double MOUNT_MIN_HEIGHT_RATIO = 1.5;
   private static final int STOMP_WINDUP_TICKS = 8;
   private static final double STOMP_RADIUS_BRUTE = 4.0;
   private static final double STOMP_RADIUS_TITAN = 6.0;
   private static final double STOMP_RADIUS_COLOSSUS = 8.0;
   private static final float STOMP_DAMAGE_BRUTE = 5.0F;
   private static final float STOMP_DAMAGE_TITAN = 8.0F;
   private static final float STOMP_DAMAGE_COLOSSUS = 12.0F;
   private static final double STOMP_KNOCKBACK = 0.7;
   private static final double STOMP_KNOCKUP_TITAN = 0.5;
   private static final double STOMP_KNOCKUP_COLOSSUS = 0.8;
   private static final float HEIGHT_AMP_CAP = 20.0F;
   private static final float HEIGHT_AMP_DIVISOR = 8.0F;
   private static final float HEIGHT_AMP_RADIUS_SHARE = 0.3F;
   private static final float FALL_ABSORB = 0.3F;
   private final Map<UUID, HitchStompAbility.Stomp> stomps = this.playerState();

   public HitchStompAbility() {
      super("hitch_stomp", 9.0F, 6);
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
   public float staminaCost(ServerPlayer player) {
      if (HitchControl.isLatched(player)) {
         return 0.0F;
      } else {
         float scale = Scaling.stage(PowerService.data(player)).scale();
         if (scale <= SizeStage.SMALL.scale()) {
            return 4.0F;
         } else {
            return scale >= SizeStage.BRUTE.scale() ? 9.0F : 0.0F;
         }
      }
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (HitchControl.isLatched(player)) {
         HitchControl.detach(player, true);
         return true;
      } else {
         float scale = Scaling.stage(PowerService.data(player)).scale();
         if (scale <= SizeStage.SMALL.scale()) {
            return this.tryHitch(player);
         } else {
            return scale >= SizeStage.BRUTE.scale() ? this.startStomp(player) : this.fail(player, "awakened.scaler.commit_utility", new Object[0]);
         }
      }
   }

   private boolean tryHitch(ServerPlayer player) {
      AimUtil.SoftPick pick = AimUtil.aimedLivingSoftPick(player, 8.0, 12.0, e -> (double)e.getBbHeight() > (double)player.getBbHeight() * 1.5);
      if (!pick.found()) {
         return this.fail(player, pick.rejected() ? "awakened.scaler.too_big_to_hitch" : "awakened.scaler.hitch_no_target", new Object[0]);
      } else {
         LivingEntity mount = pick.target();
         if (!HitchControl.begin(player, mount)) {
            return this.fail(player, "awakened.scaler.no_mount", new Object[0]);
         } else {
            ServerLevel level = player.serverLevel();
            Vec3 top = mount.position().add(0.0, (double)mount.getBbHeight(), 0.0);
            Fx.burst(level, ParticleTypes.CLOUD, top, 6, 0.25, 0.03);
            Fx.sound(level, mount.position(), SoundEvents.SLIME_ATTACK, SoundSource.PLAYERS, 0.7F, 1.4F);
            return true;
         }
      }
   }

   private boolean startStomp(ServerPlayer player) {
      if (this.stomps.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.scaler.stomp_busy", new Object[0]);
      } else {
         float heightAmp = 1.0F + Math.min(player.fallDistance, 20.0F) / 8.0F;
         this.stomps.put(player.getUUID(), new HitchStompAbility.Stomp(8, heightAmp));
         PowerService.playAnimation(player, "scaler_stomp");
         Fx.sound(player.serverLevel(), player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 0.5F);
         return true;
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      HitchControl.tick(player);
      HitchStompAbility.Stomp stomp = this.stomps.get(player.getUUID());
      if (stomp != null && --stomp.ticksLeft <= 0) {
         this.stomps.remove(player.getUUID());
         this.slam(player, stomp.heightAmp);
      }
   }

   private void slam(ServerPlayer player, float heightAmp) {
      SizeStage stage = Scaling.stage(PowerService.data(player));
      if (stage.scale() < SizeStage.BRUTE.scale()) {
         Fx.burst(player.serverLevel(), ParticleTypes.CLOUD, player.position(), 6, 0.4, 0.04);
      } else {
         double radius = stage == SizeStage.COLOSSUS ? 8.0 : (stage == SizeStage.TITAN ? 6.0 : 4.0);
         float damage = stage == SizeStage.COLOSSUS ? 12.0F : (stage == SizeStage.TITAN ? 8.0F : 5.0F);
         double knockUp = stage == SizeStage.COLOSSUS ? 0.8 : (stage == SizeStage.TITAN ? 0.5 : 0.0);
         radius *= (double)(1.0F + (heightAmp - 1.0F) * 0.3F);
         damage *= heightAmp;
         List<LivingEntity> struck = ScalerFx.shockwave(
            player, player.position(), radius, MasteryScaling.scaledDamage(damage, player, this.balanceKey()), 0.7, knockUp, stage.scale()
         );
         player.fallDistance *= 0.3F;
         if (!struck.isEmpty()) {
            MasterySystem.award(player, MasteryReason.HIT, struck);
            int foes = 0;

            for (LivingEntity victim : struck) {
               if (Foes.counts(player, victim)) {
                  foes++;
               }
            }

            if (foes >= 3) {
               QuestSystem.feat(player, "scaler_stomp_crowd");
            }
         }
      }
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      HitchControl.detach(player, false);
      this.stomps.remove(player.getUUID());
      super.clearPlayer(player);
   }

   private static final class Stomp {
      private int ticksLeft;
      private final float heightAmp;

      private Stomp(int ticksLeft, float heightAmp) {
         this.ticksLeft = ticksLeft;
         this.heightAmp = heightAmp;
      }
   }
}
