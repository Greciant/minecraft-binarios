package com.awakenedmod.awakened.content.shadow;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.core.AbilityGate;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.TickFreeze;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class ShadowSqueezeAbility extends Ability {
   private static final float COST = 10.0F;
   private static final float HOLD_COST = 0.6F;
   private static final int UNLOCK = 3;
   private static final double RANGE = 16.0;
   private static final double KEEP_RANGE = 22.0;
   private static final double LIFT = 1.6;
   private static final float DOT_DIM = 0.6F;
   private static final float DOT_DARK = 2.2F;
   private static final int DOT_INTERVAL = 10;
   private static final Set<UUID> SILENCED = ConcurrentHashMap.newKeySet();
   private final Map<UUID, ShadowSqueezeAbility.Squeeze> active = this.activeState();

   public ShadowSqueezeAbility() {
      super("shadow_squeeze", 10.0F, 3);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boostsDamage().build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.active.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         LivingEntity target = AimUtil.aimedLivingEntity(player, 16.0);
         if (target == null || !Harm.allowed(player, target)) {
            return this.fail(player, "awakened.shadow.squeeze_no_target", new Object[0]);
         } else if (!Harm.controls(player, target)) {
            return this.fail(player, "awakened.feedback.pvp_control_off", new Object[0]);
         } else {
            ServerLevel level = player.serverLevel();
            Vec3 lockPos = target.position().add(0.0, 1.6, 0.0);
            ShadowSqueezeAbility.Squeeze c = new ShadowSqueezeAbility.Squeeze(target, lockPos);
            if (!(target instanceof Player)) {
               TickFreeze.freeze(target);
            }

            this.active.put(player.getUUID(), c);
            this.spawnCoils(player, c);
            this.grip(player, c);
            ShadowFx.chokeSeize(level, target.position().add(0.0, (double)target.getBbHeight() * 0.6, 0.0));
            MasterySystem.award(player, MasteryReason.HIT, target);
            return true;
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      ShadowSqueezeAbility.Squeeze c = this.active.get(player.getUUID());
      if (c != null) {
         ServerLevel level = player.serverLevel();
         LivingEntity target = c.target;
         boolean gone = !target.isAlive() || target.isRemoved();
         if (!gone && !(target.distanceToSqr(player) > 484.0)) {
            PowerData data = PowerService.data(player);
            long gameTime = level.getGameTime();
            StaminaSystem.suppressRegen(data, gameTime);
            if (!data.infiniteStamina()) {
               float drain = this.billed(0.6F);
               if (data.stamina() < drain) {
                  this.stop(player);
                  return;
               }

               StaminaSystem.drainSustained(data, gameTime, drain);
            }

            this.grip(player, c);
            ShadowFx.chokeGrip(level, target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0), (double)target.getBbHeight());
            c.dotTimer++;
            if (c.dotTimer >= 10) {
               c.dotTimer = 0;
               float perSecond = ShadowDark.scaleAt(level, target.position(), 0.6F, 2.2F);
               float dmg = MasteryScaling.scaledDamage(perSecond * 0.5F, player, this.balanceKey());
               Dmg.hurt(target, level, Dmg.by(player, DamageTypes.MAGIC), dmg);
            }

            if (player.tickCount % 4 == 0) {
               PowerService.sync(player);
            }
         } else {
            if (gone && Foes.defeatedBy(target, player)) {
               QuestSystem.feat(player, "shadow_choke_kill");
            }

            this.stop(player);
         }
      }
   }

   private void grip(ServerPlayer player, ShadowSqueezeAbility.Squeeze c) {
      LivingEntity target = c.target;
      target.fallDistance = 0.0F;
      target.hurtMarked = true;
      if (target instanceof ServerPlayer victim) {
         Harm.effect(player, victim, new MobEffectInstance(MobEffects.LEVITATION, 8, 1, false, false));
         Harm.effect(player, victim, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 12, 250, false, false));
         Harm.effect(player, victim, new MobEffectInstance(MobEffects.WEAKNESS, 12, 4, false, false));
         SILENCED.add(victim.getUUID());
      } else {
         target.setDeltaMovement(0.0, 0.0, 0.0);
         target.setPos(c.lockPos.x, c.lockPos.y, c.lockPos.z);
      }

      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6, 3, false, false));
      this.placeCoils(c);
   }

   private void spawnCoils(ServerPlayer player, ShadowSqueezeAbility.Squeeze c) {
      LivingEntity target = c.target;
      ShadowShapeEntity coils = new ShadowShapeEntity(AwakenedEntities.SHADOW_SHAPE.get(), player.serverLevel());
      float r = Math.max(0.45F, target.getBbWidth() * 0.62F);
      coils.configure(ShadowShapeEntity.Shape.COILS, r, 5386362, 210, 0);
      coils.setHeight(Math.max(0.8F, target.getBbHeight() * 0.95F));
      c.coils = coils;
      this.placeCoils(c);
      player.serverLevel().addFreshEntity(coils);
   }

   private void placeCoils(ShadowSqueezeAbility.Squeeze c) {
      if (c.coils != null) {
         LivingEntity target = c.target;
         c.coils.setPos(target.getX(), target.getY() + (double)target.getBbHeight() * 0.5, target.getZ());
      }
   }

   private void stop(ServerPlayer player) {
      ShadowSqueezeAbility.Squeeze c = this.active.remove(player.getUUID());
      if (c != null) {
         if (c.target instanceof ServerPlayer victim) {
            SILENCED.remove(victim.getUUID());
         } else if (!(c.target instanceof Player)) {
            TickFreeze.release(c.target);
         }

         if (c.coils != null) {
            c.coils.discard();
            c.coils = null;
         }

         ServerLevel var5 = player.serverLevel();
         if (var5 instanceof ServerLevel) {
            ShadowFx.murk(var5, c.target.position().add(0.0, (double)c.target.getBbHeight() * 0.5, 0.0), 14, 0.5);
         }

         PowerService.sync(player);
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      this.stop(player);
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      this.stop(player);
      super.clearPlayer(player);
   }

   static {
      AbilityGate.register(p -> SILENCED.contains(p.getUUID()));
   }

   private static final class Squeeze {
      private final LivingEntity target;
      private final Vec3 lockPos;
      private ShadowShapeEntity coils;
      private int dotTimer;

      private Squeeze(LivingEntity target, Vec3 lockPos) {
         this.target = target;
         this.lockPos = lockPos;
      }
   }
}
