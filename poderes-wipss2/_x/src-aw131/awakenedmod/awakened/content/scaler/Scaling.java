package com.awakenedmod.awakened.content.scaler;

import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Headroom;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class Scaling {
   private static final String K_STAGE = "scaler_stage";
   private static final String K_SCALE = "scaler_scale";
   public static final String K_COMPRESS = "scaler_compress";
   private static final float COMPRESS_DEPTH = 0.65F;
   private static final float COMPRESS_MIN_SCALE = 0.07F;
   private static final String K_PREV_LEVEL = "scaler_prev_level";
   private static final String K_NEXT_SNAP = "scaler_next_snap";
   private static final String K_STEP_ACC = "scaler_step_acc";
   private static final float SMOOTH_RATE = 0.35F;
   private static final float SNAP_EPSILON = 0.005F;
   private static final double REACH_PER_SCALE = 1.6;
   private static final double REACH_FLOOR_DELTA = -2.0;
   private static final float FALL_IMMUNE_AT = 0.5F;
   private static final double FALL_IMMUNE_SAFE_DISTANCE = 1024.0;
   private static final double JUMP_TITAN = 0.1;
   private static final double JUMP_COLOSSUS = 0.2;
   private static final double CAMERA_PER_SCALE = 1.2;
   public static final float SPECK_PERCEPTION_MAX = 0.15F;
   public static final float MITE_PERCEPTION_MAX = 0.3F;
   public static final double MITE_VISIBILITY = 0.3;
   private static final double AGGRO_CLEAR_RADIUS = 24.0;
   private static final int AGGRO_SWEEP_INTERVAL = 10;
   private static final int EXHAUST_SNAP_INTERVAL = 15;
   private static final int HEADROOM_CHECK_INTERVAL = 5;
   public static final int UNLOCK_DISTANCE_2_LEVEL = 6;
   public static final int UNLOCK_DISTANCE_3_LEVEL = 14;
   private static final float TITAN_STEP_BLOCKS = 4.0F;
   private static final float COLOSSUS_STEP_BLOCKS = 3.0F;
   private static final double STEP_TELEPORT_SQR = 4.0;
   private static final ResourceLocation SCALE_ID = AwId.of("scaler_scale");
   private static final ResourceLocation SPEED_ID = AwId.of("scaler_speed");
   private static final ResourceLocation ATTACK_ID = AwId.of("scaler_attack");
   private static final ResourceLocation ATTACK_KB_ID = AwId.of("scaler_attack_knockback");
   private static final ResourceLocation KB_RESIST_ID = AwId.of("scaler_knockback_resist");
   private static final ResourceLocation STEP_ID = AwId.of("scaler_step_height");
   private static final ResourceLocation REACH_ID = AwId.of("scaler_reach");
   private static final ResourceLocation BLOCK_REACH_ID = AwId.of("scaler_block_reach");
   private static final ResourceLocation SAFE_FALL_ID = AwId.of("scaler_safe_fall");
   private static final ResourceLocation FALL_MULT_ID = AwId.of("scaler_fall_mult");
   private static final ResourceLocation JUMP_ID = AwId.of("scaler_jump");
   private static final Map<UUID, Float> LAST_APPLIED = new ConcurrentHashMap<>();
   private static final Map<UUID, Vec3> LAST_STEP_POS = new ConcurrentHashMap<>();
   private static final Set<UUID> SPECKS = ConcurrentHashMap.newKeySet();
   private static final Set<UUID> MITES = ConcurrentHashMap.newKeySet();

   private Scaling() {
   }

   public static SizeStage stage(PowerData data) {
      return SizeStage.byOrdinal(Math.round(data.var("scaler_stage", (float)SizeStage.NORMAL.ordinal())));
   }

   public static void setStage(PowerData data, SizeStage stage) {
      data.setVar("scaler_stage", (float)stage.ordinal());
   }

   public static float smoothedScale(PowerData data) {
      return data.var("scaler_scale", 1.0F);
   }

   public static int maxUnlockedDistance(PowerData data) {
      int level = data.masteryLevel();
      if (level >= 14) {
         return 3;
      } else {
         return level >= 6 ? 2 : 1;
      }
   }

   public static boolean isUntargetable(LivingEntity target) {
      return target instanceof Player && SPECKS.contains(target.getUUID());
   }

   public static boolean isSpeck(LivingEntity entity) {
      return entity instanceof Player && SPECKS.contains(entity.getUUID());
   }

   public static boolean isMite(LivingEntity entity) {
      return entity instanceof Player && MITES.contains(entity.getUUID());
   }

   public static void update(ServerPlayer player, PowerData data) {
      if (player.isAlive()) {
         long gameTime = player.serverLevel().getGameTime();
         SizeStage stage = stage(data);
         if (stage.scale() > 1.0F && player.tickCount % 5 == 0 && !Headroom.fits(player, (double)stage.scale())) {
            stage = collapseOneStage(player, data, stage, "awakened.scaler.too_tight");
         }

         if (stage != SizeStage.NORMAL && stage.drainPerTick() > 0.0F && !data.infiniteStamina() && !HitchControl.isLatched(player)) {
            float left = StaminaSystem.drainSustained(
               data, gameTime, StaminaSystem.effectiveCost(stage.drainPerTick(), SizeChangeAbility.class.getSimpleName())
            );
            if (left <= 0.0F && gameTime >= (long)data.var("scaler_next_snap", 0.0F)) {
               data.setVar("scaler_next_snap", (float)(gameTime + 15L));
               stage = collapseOneStage(player, data, stage, "awakened.scaler.exhausted");
            }
         }

         float cur = smoothedScale(data);
         float target = stage.scale();
         float compress = data.var("scaler_compress", 0.0F);
         if (compress > 0.0F) {
            target = Math.max(0.07F, target * (1.0F - 0.65F * compress));
         }

         cur += (target - cur) * 0.35F;
         if (Math.abs(target - cur) < 0.005F) {
            cur = target;
         }

         data.setVar("scaler_scale", cur);
         applyAttributes(player, stage, cur);
         updatePerception(player, cur);
         tickFootsteps(player, data, stage);
         announceUnlockGates(player, data);
      }
   }

   private static void tickFootsteps(ServerPlayer player, PowerData data, SizeStage stage) {
      if (stage.scale() < SizeStage.TITAN.scale()) {
         if (!LAST_STEP_POS.isEmpty()) {
            LAST_STEP_POS.remove(player.getUUID());
         }
      } else {
         Vec3 pos = player.position();
         Vec3 last = LAST_STEP_POS.put(player.getUUID(), pos);
         if (last != null && player.onGround()) {
            double movedSqr = new Vec3(pos.x - last.x, 0.0, pos.z - last.z).lengthSqr();
            if (!(movedSqr < 1.0E-6) && !(movedSqr > 4.0)) {
               boolean colossus = stage == SizeStage.COLOSSUS;
               float interval = colossus ? 3.0F : 4.0F;
               float acc = data.var("scaler_step_acc", 0.0F) + (float)Math.sqrt(movedSqr);
               if (acc >= interval) {
                  acc = 0.0F;
                  ServerLevel level = player.serverLevel();
                  Fx.sound(level, pos, SoundEvents.RAVAGER_STEP, SoundSource.PLAYERS, colossus ? 1.2F : 0.7F, colossus ? 0.55F : 0.75F);
                  ScalerFx.screenShake(level, pos, colossus ? 8.0 : 5.0, colossus ? 3 : 2);
               }

               data.setVar("scaler_step_acc", acc);
            }
         }
      }
   }

   private static SizeStage collapseOneStage(ServerPlayer player, PowerData data, SizeStage from, String reasonKey) {
      SizeStage to = from.towardNormal();
      if (to == from) {
         return from;
      } else {
         setStage(data, to);
         ScalerFx.shift(player, to.scale() > from.scale(), (double)Math.max(from.scale(), to.scale()));
         player.displayClientMessage(Component.translatable(reasonKey), true);
         PowerService.sync(player);
         return to;
      }
   }

   private static void applyAttributes(ServerPlayer player, SizeStage stage, float scale) {
      float key = (float)Math.round(scale * 200.0F) + (float)stage.ordinal() * 10000.0F;
      Float last = LAST_APPLIED.get(player.getUUID());
      if (last == null || last != key) {
         LAST_APPLIED.put(player.getUUID(), key);
         setModifier(player, Attributes.SCALE, SCALE_ID, (double)scale - 1.0, Operation.ADD_VALUE);
         setModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, (double)stage.speedMult() - 1.0, Operation.ADD_MULTIPLIED_TOTAL);
         setModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_ID, (double)stage.damageDealtMult() - 1.0, Operation.ADD_MULTIPLIED_TOTAL);
         setModifier(player, Attributes.ATTACK_KNOCKBACK, ATTACK_KB_ID, stage.attackKnockbackBonus(), Operation.ADD_VALUE);
         setModifier(player, Attributes.KNOCKBACK_RESISTANCE, KB_RESIST_ID, stage.knockbackResist(), Operation.ADD_VALUE);
         setModifier(player, Attributes.STEP_HEIGHT, STEP_ID, stage.stepHeightBonus(), Operation.ADD_VALUE);
         double reach = Math.max(((double)scale - 1.0) * 1.6, -2.0);
         setModifier(player, Attributes.ENTITY_INTERACTION_RANGE, REACH_ID, reach, Operation.ADD_VALUE);
         setModifier(player, Attributes.BLOCK_INTERACTION_RANGE, BLOCK_REACH_ID, reach, Operation.ADD_VALUE);
         boolean fallImmune = stage.scale() <= 0.5F;
         setModifier(player, Attributes.SAFE_FALL_DISTANCE, SAFE_FALL_ID, fallImmune ? 1024.0 : 0.0, Operation.ADD_VALUE);
         setModifier(player, Attributes.FALL_DAMAGE_MULTIPLIER, FALL_MULT_ID, fallImmune ? -1.0 : 0.0, Operation.ADD_MULTIPLIED_TOTAL);
         double jump = stage == SizeStage.TITAN ? 0.1 : (stage == SizeStage.COLOSSUS ? 0.2 : 0.0);
         setModifier(player, Attributes.JUMP_STRENGTH, JUMP_ID, jump, Operation.ADD_VALUE);
      }
   }

   static void setModifier(LivingEntity entity, Holder<Attribute> attr, ResourceLocation id, double amount, Operation op) {
      AttributeInstance inst = entity.getAttribute(attr);
      if (inst != null) {
         inst.removeModifier(id);
         if (Math.abs(amount) > 1.0E-4) {
            inst.addTransientModifier(new AttributeModifier(id, amount, op));
         }
      }
   }

   static void removeModifier(LivingEntity entity, Holder<Attribute> attr, ResourceLocation id) {
      AttributeInstance inst = entity.getAttribute(attr);
      if (inst != null) {
         inst.removeModifier(id);
      }
   }

   private static void updatePerception(ServerPlayer player, float scale) {
      UUID id = player.getUUID();
      if (scale <= 0.15F) {
         SPECKS.add(id);
         if (player.tickCount % 10 == 0) {
            ServerLevel level = player.serverLevel();

            for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(24.0), m -> m.getTarget() == player)) {
               mob.setTarget(null);
            }
         }
      } else {
         SPECKS.remove(id);
      }

      if (scale <= 0.3F) {
         MITES.add(id);
      } else {
         MITES.remove(id);
      }
   }

   private static void announceUnlockGates(ServerPlayer player, PowerData data) {
      int level = data.masteryLevel();
      int prev = Math.round(data.var("scaler_prev_level", -1.0F));
      if (prev != level) {
         data.setVar("scaler_prev_level", (float)level);
         if (prev >= 0) {
            if (prev < 6 && level >= 6) {
               PowerService.announceUnlock(player, Component.translatable("awakened.scaler.unlock.range2"), PowerService.Unlock.FEATURE);
            }

            if (prev < 14 && level >= 14) {
               PowerService.announceUnlock(player, Component.translatable("awakened.scaler.unlock.range3"), PowerService.Unlock.FEATURE);
            }
         }
      }
   }

   public static void clear(ServerPlayer player) {
      removeModifier(player, Attributes.SCALE, SCALE_ID);
      removeModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID);
      removeModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_ID);
      removeModifier(player, Attributes.ATTACK_KNOCKBACK, ATTACK_KB_ID);
      removeModifier(player, Attributes.KNOCKBACK_RESISTANCE, KB_RESIST_ID);
      removeModifier(player, Attributes.STEP_HEIGHT, STEP_ID);
      removeModifier(player, Attributes.ENTITY_INTERACTION_RANGE, REACH_ID);
      removeModifier(player, Attributes.BLOCK_INTERACTION_RANGE, BLOCK_REACH_ID);
      removeModifier(player, Attributes.SAFE_FALL_DISTANCE, SAFE_FALL_ID);
      removeModifier(player, Attributes.FALL_DAMAGE_MULTIPLIER, FALL_MULT_ID);
      removeModifier(player, Attributes.JUMP_STRENGTH, JUMP_ID);
      LAST_APPLIED.remove(player.getUUID());
      LAST_STEP_POS.remove(player.getUUID());
      SPECKS.remove(player.getUUID());
      MITES.remove(player.getUUID());
      PowerData data = PowerService.data(player);
      data.setVar("scaler_stage", (float)SizeStage.NORMAL.ordinal());
      data.setVar("scaler_scale", 1.0F);
      data.setVar("scaler_compress", 0.0F);
      data.setVar("scaler_next_snap", 0.0F);
      data.setVar("scaler_step_acc", 0.0F);
   }
}
