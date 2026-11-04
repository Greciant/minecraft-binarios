package com.awakenedmod.awakened.content.stormcaller;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.net.ThunderfallAimPayload;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ThunderfallAbility extends Ability {
   private static final float UPFRONT_COST = 35.0F;
   private static final int UNLOCK = 7;
   private static final int LIFT_HEIGHT = 18;
   private static final int MIN_LIFT = 5;
   private static final int MARK_MAX_TICKS = 100;
   private static final float DRAIN_PER_TICK = 0.3F;
   private static final double AIM_RANGE = 48.0;
   private static final double STRIKE_RADIUS = 4.0;
   private static final float STRIKE_DAMAGE = 12.0F;
   private static final int BOLTS = 6;
   private static final int IFRAME_TICKS = 15;
   private static final int CANCEL_GRACE_TICKS = 4;
   private final Map<UUID, ThunderfallAbility.Mark> marking = this.activeState();

   public ThunderfallAbility() {
      super("thunderfall", 35.0F, 7);
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      return 0.0F;
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(12.0).build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.marking.containsKey(player.getUUID())) {
         this.commit(player);
         return true;
      } else {
         ServerLevel level = player.serverLevel();
         int lift = clearHeightAbove(player, 18);
         if (lift < 5) {
            return this.fail(player, "awakened.stormcaller.no_sky", new Object[0]);
         } else {
            PowerData data = PowerService.data(player);
            float upfront = this.billed(35.0F);
            if (!data.infiniteStamina() && data.stamina() < upfront) {
               return this.fail(player, "awakened.feedback.no_stamina", new Object[0]);
            } else {
               if (!data.infiniteStamina()) {
                  data.setStamina(data.stamina() - upfront);
               }

               Fx.burst(level, ParticleTypes.ELECTRIC_SPARK, player.position().add(0.0, 1.0, 0.0), 40, 0.4, 0.2);
               StormSupport.strike(level, player, player.position(), 0.0F, 0.0);
               player.teleportTo(player.getX(), player.getY() + (double)lift, player.getZ());
               player.setDeltaMovement(Vec3.ZERO);
               player.hurtMarked = true;
               player.resetFallDistance();
               player.invulnerableTime = Math.max(player.invulnerableTime, 15);
               this.marking.put(player.getUUID(), new ThunderfallAbility.Mark());
               PowerService.playAnimation(player, "thunderfall_rise");
               PacketDistributor.sendToPlayer(player, new ThunderfallAimPayload(true), new CustomPacketPayload[0]);
               Fx.sound(level, player.position(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0F, 1.4F);
               player.displayClientMessage(Component.translatable("awakened.stormcaller.thunderfall_aim").withStyle(ChatFormatting.AQUA), true);
               PowerService.sync(player);
               return true;
            }
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      ThunderfallAbility.Mark mark = this.marking.get(player.getUUID());
      if (mark != null) {
         ServerLevel level = player.serverLevel();
         Vec3 v = player.getDeltaMovement();
         player.setDeltaMovement(v.x, 0.0, v.z);
         player.hurtMarked = true;
         player.resetFallDistance();
         PowerData data = PowerService.data(player);
         if (!data.infiniteStamina()) {
            if (StaminaSystem.drainSustained(data, level.getGameTime(), this.billed(0.3F)) <= 0.0F) {
               this.commit(player);
               return;
            }

            if (player.tickCount % 5 == 0) {
               PowerService.sync(player);
            }
         }

         if (mark.ticks > 4 && player.isShiftKeyDown()) {
            this.cancel(player);
         } else {
            mark.aim = aimedGround(player);
            if (mark.aim != null) {
               drawRing(level, mark.aim);
               if (mark.ticks % 4 == 0) {
                  StormFx.aimRing(level, mark.aim, 4.0);
               }
            }

            if (++mark.ticks >= 100) {
               this.commit(player);
            }
         }
      }
   }

   private void cancel(ServerPlayer player) {
      if (this.marking.remove(player.getUUID()) != null) {
         PacketDistributor.sendToPlayer(player, new ThunderfallAimPayload(false), new CustomPacketPayload[0]);
         player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, false));
         player.resetFallDistance();
         Fx.sound(player.serverLevel(), player.position(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6F, 1.2F);
         player.displayClientMessage(Component.translatable("awakened.stormcaller.thunderfall_cancel").withStyle(ChatFormatting.AQUA), true);
         PowerService.sync(player);
      }
   }

   private void commit(ServerPlayer player) {
      ThunderfallAbility.Mark mark = this.marking.remove(player.getUUID());
      if (mark != null) {
         PacketDistributor.sendToPlayer(player, new ThunderfallAimPayload(false), new CustomPacketPayload[0]);
         ServerLevel level = player.serverLevel();
         Vec3 target = mark.aim != null ? mark.aim : groundBelow(player);
         player.teleportTo(target.x, target.y, target.z);
         player.setDeltaMovement(Vec3.ZERO);
         player.hurtMarked = true;
         player.resetFallDistance();
         player.invulnerableTime = Math.max(player.invulnerableTime, 15);
         float damage = MasteryScaling.scaledDamage(12.0F, player, this.balanceKey());
         StormSupport.Result crash = StormSupport.strikeResult(level, player, target, damage, 4.0);

         for (int i = 0; i < 6; i++) {
            double a = (Math.PI * 2) * (double)i / 6.0;
            double r = 4.0 * (0.4 + 0.6 * (double)(i % 2));
            StormSupport.strike(level, player, target.add(Math.cos(a) * r, 0.0, Math.sin(a) * r), 0.0F, 0.0);
         }

         StormFx.sparks(level, target.add(0.0, 0.5, 0.0), 24, 2.0);
         PowerService.playAnimation(player, "thunderfall_strike");
         Fx.sound(level, target, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.2F, 0.7F);
         Fx.sound(level, target, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.2F, 0.9F);
         if (crash.threats() > 0) {
            MasterySystem.award(player, MasteryReason.HIT);
         }

         if (crash.threats() >= 3) {
            QuestSystem.feat(player, "storm_airstrike");
         }

         PowerService.sync(player);
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      if (this.marking.containsKey(player.getUUID())) {
         PacketDistributor.sendToPlayer(player, new ThunderfallAimPayload(false), new CustomPacketPayload[0]);
      }

      super.clearPlayer(player);
   }

   private static int clearHeightAbove(ServerPlayer player, int max) {
      ServerLevel level = player.serverLevel();
      int h = 0;
      int i = 1;

      while (i <= max && level.noCollision(player, player.getBoundingBox().move(0.0, (double)i, 0.0))) {
         h = i++;
      }

      return h;
   }

   private static Vec3 aimedGround(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 eye = player.getEyePosition();
      Vec3 far = eye.add(player.getViewVector(1.0F).scale(48.0));
      BlockHitResult hit = level.clip(new ClipContext(eye, far, Block.COLLIDER, Fluid.NONE, player));
      if (hit.getType() != Type.MISS) {
         return hit.getLocation();
      } else {
         BlockHitResult down = level.clip(new ClipContext(far, far.add(0.0, -160.0, 0.0), Block.COLLIDER, Fluid.NONE, player));
         return down.getType() != Type.MISS ? down.getLocation() : null;
      }
   }

   private static Vec3 groundBelow(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 from = player.position();
      BlockHitResult hit = level.clip(new ClipContext(from, from.add(0.0, -80.0, 0.0), Block.COLLIDER, Fluid.NONE, player));
      return hit.getType() != Type.MISS ? hit.getLocation() : from;
   }

   private static void drawRing(ServerLevel level, Vec3 center) {
      int points = 16;

      for (int i = 0; i < points; i++) {
         double a = (Math.PI * 2) * (double)i / (double)points;
         Vec3 p = center.add(Math.cos(a) * 4.0, 0.1, Math.sin(a) * 4.0);
         Fx.burst(level, ParticleTypes.ELECTRIC_SPARK, p, 1, 0.0, 0.0);
      }
   }

   private static final class Mark {
      private int ticks;
      private Vec3 aim;
   }
}
