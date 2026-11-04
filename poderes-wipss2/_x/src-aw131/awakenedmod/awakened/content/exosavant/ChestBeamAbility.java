package com.awakenedmod.awakened.content.exosavant;

import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.LoopSound;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class ChestBeamAbility extends Ability {
   private static final String HUM = "chest_beam_hum";
   private static final int UNLOCK = 5;
   private static final double RANGE = 24.0;
   private static final float DAMAGE_PER_TICK = 0.55F;
   private static final float STAMINA_PER_TICK = 1.5F;
   private static final ResourceLocation ROOT_SPEED_ID = AwId.of("chest_beam_root_speed");
   private static final ResourceLocation ROOT_JUMP_ID = AwId.of("chest_beam_root_jump");
   private final Map<UUID, Boolean> channel = this.activeState();
   private static final double TILT_FOLLOW = 0.65;
   private static final double TILT_MAX_DEG = 55.0;
   private static final double REACTOR_UP = 0.4;
   private static final double REACTOR_OUT = 0.2;

   public ChestBeamAbility() {
      super("chest_beam", 0.0F, 5);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().value("damage", AbilityScaling.Unit.RAW, 11.0).build();
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      return 0.0F;
   }

   @Override
   public String costLabel() {
      return (int)this.billed(30.0F) + "/s";
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (!ExoLoadout.usable(player, ExoPiece.CHESTPLATE)) {
         return false;
      } else if (this.channel.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         PowerData data = PowerService.data(player);
         if (!data.infiniteStamina() && data.stamina() < this.billed(1.5F)) {
            return this.fail(player, "awakened.feedback.no_stamina", new Object[0]);
         } else {
            this.channel.put(player.getUUID(), Boolean.TRUE);
            root(player, true);
            PowerService.playAnimation(player, "chest_beam");
            Fx.sound(player.serverLevel(), player.position(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.8F);
            return true;
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      if (this.channel.containsKey(player.getUUID())) {
         if (ExoSuit.isSuited(player) && ExoLoadout.hasPiece(player, ExoPiece.CHESTPLATE)) {
            PowerData data = PowerService.data(player);
            if (!data.infiniteStamina() && data.stamina() < this.billed(1.5F)) {
               this.stop(player);
            } else {
               ServerLevel level = player.serverLevel();
               long gameTime = level.getGameTime();
               StaminaSystem.drainSustained(data, gameTime, this.billed(1.5F));
               PowerService.sync(player);
               Vec3 eye = player.getEyePosition();
               Vec3 aimDir = player.getViewVector(1.0F);
               LivingEntity locked = ExoTags.primaryTarget(player);
               if (locked != null) {
                  Vec3 toTarget = locked.getBoundingBox().getCenter().subtract(eye);
                  if (toTarget.lengthSqr() > 1.0E-6) {
                     aimDir = toTarget.normalize();
                  }
               }

               Vec3 reach = eye.add(aimDir.scale(24.0));
               BlockHitResult blocked = level.clip(new ClipContext(eye, reach, Block.COLLIDER, Fluid.NONE, player));
               Vec3 end = blocked.getType() == Type.BLOCK ? blocked.getLocation() : reach;
               Vec3 chest = reactorPos(player, aimDir);
               float damage = MasteryScaling.scaledDamage(0.55F, player, this.balanceKey());

               for (LivingEntity victim : entitiesOnLine(player, eye, end)) {
                  if (Dmg.hurtRapid(victim, level, Dmg.by(player, DamageTypes.MAGIC), damage)) {
                     ExoTags.applyTaggedBonus(player, victim, level, damage);
                  }
               }

               Fx.sphereShell(level, ExoFx.dust(), chest, 0.25, 6);
               Fx.debrisFan(level, ParticleTypes.CRIT, end, 2, 15.0, 60.0, 0.25, 0.5);
               if (blocked.getType() == Type.BLOCK && gameTime % 2L == 0L) {
                  Fx.debrisFan(level, ParticleTypes.LAVA, end, 1, 10.0, 40.0, 0.15, 0.35);
               }

               LoopSound.keep(player, "chest_beam_hum", SoundEvents.CONDUIT_AMBIENT, SoundSource.PLAYERS, 0.5F, 0.6F);
            }
         } else {
            this.stop(player);
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      this.stop(player);
   }

   private void stop(ServerPlayer player) {
      if (this.channel.remove(player.getUUID()) != null) {
         root(player, false);
         PowerService.stopAnimation(player);
         LoopSound.stop(player, "chest_beam_hum");
         Fx.sound(player.serverLevel(), player.position(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5F, 1.4F);
      }
   }

   private static void root(ServerPlayer player, boolean on) {
      if (on) {
         ExoSuit.setModifier(player, Attributes.MOVEMENT_SPEED, ROOT_SPEED_ID, -1.0, Operation.ADD_MULTIPLIED_TOTAL);
         ExoSuit.setModifier(player, Attributes.JUMP_STRENGTH, ROOT_JUMP_ID, -1.0, Operation.ADD_MULTIPLIED_TOTAL);
      } else {
         ExoSuit.removeModifier(player, Attributes.MOVEMENT_SPEED, ROOT_SPEED_ID);
         ExoSuit.removeModifier(player, Attributes.JUMP_STRENGTH, ROOT_JUMP_ID);
      }
   }

   private static Vec3 reactorPos(ServerPlayer player, Vec3 aimDir) {
      double tilt = Math.toRadians(Math.max(-55.0, Math.min(55.0, (double)player.getXRot() * 0.65)));
      Vec3 fwdH = Vec3.directionFromRotation(0.0F, player.getYRot());
      Vec3 hip = player.getEyePosition().add(0.0, -0.87, 0.0);
      return hip.add(fwdH.scale(0.4 * Math.sin(tilt))).add(0.0, 0.4 * Math.cos(tilt), 0.0).add(aimDir.scale(0.2));
   }

   private static List<LivingEntity> entitiesOnLine(ServerPlayer player, Vec3 eye, Vec3 end) {
      AABB box = new AABB(eye, end).inflate(1.0);
      List<LivingEntity> hits = new ArrayList<>();

      for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class, box, c -> c != player && c.isAlive())) {
         if (e.getBoundingBox().inflate(0.3).clip(eye, end).isPresent()) {
            hits.add(e);
         }
      }

      return hits;
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      if (this.channel.remove(player.getUUID()) != null) {
         root(player, false);
         PowerService.stopAnimation(player);
         LoopSound.stop(player, "chest_beam_hum");
      }

      super.clearPlayer(player);
   }
}
