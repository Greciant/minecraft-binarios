package com.awakenedmod.awakened.content.pocket;

import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class RecursiveDropAbility extends Ability {
   private static final float COST = 18.0F;
   private static final int UNLOCK = 6;
   private static final double RANGE = 22.0;
   private static final int LOOPS = 8;
   private static final float DAMAGE_PER_LOOP = 2.0F;
   private static final double DROP_HEIGHT = 4.0;
   private static final double MIN_DROP_HEIGHT = 1.6;
   private static final double DISK_RADIUS = 1.2;
   private static final int MAW_LIFETIME = 300;
   private static final int LOOP_TIMEOUT = 30;
   private static final double CATCH_DEPTH = 0.15;
   private final Map<UUID, RecursiveDropAbility.Drop> drops = this.activeState();

   public RecursiveDropAbility() {
      super("recursive_drop", 18.0F, 6);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(2.0).build();
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.drops.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         LivingEntity target = AimUtil.aimedLivingEntity(player, 22.0, e -> Harm.allowed(player, e));
         if (target == null) {
            return this.fail(player, "awakened.pocket.drop_no_target", new Object[0]);
         } else if (!Harm.controls(player, target)) {
            return this.fail(player, "awakened.feedback.pvp_control_off", new Object[0]);
         } else {
            ServerLevel level = player.serverLevel();
            Vec3 anchor = target.position();
            double height = clearDropHeight(level, target);
            RecursiveDropAbility.Drop drop = new RecursiveDropAbility.Drop(
               target,
               anchor,
               height,
               8,
               RiftFx.flatMaw(level, anchor.add(0.0, 0.1, 0.0), 1.2, 300),
               RiftFx.flatMaw(level, anchor.add(0.0, height, 0.0), 1.2, 300)
            );
            this.drops.put(player.getUUID(), drop);
            dropFromSky(level, target, drop);
            Fx.sound(level, anchor, SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.075F, 0.8F);
            player.swing(InteractionHand.MAIN_HAND);
            MasterySystem.award(player, MasteryReason.HIT, target);
            return true;
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      RecursiveDropAbility.Drop drop = this.drops.get(player.getUUID());
      if (drop != null) {
         LivingEntity target = drop.target;
         if (target.isAlive() && !target.isRemoved()) {
            ServerLevel level = player.serverLevel();
            Vec3 pos = target.position();
            double var10001 = (drop.anchor.x - pos.x) * 0.4;
            double var10003 = drop.anchor.z - pos.z;
            target.setDeltaMovement(var10001, target.getDeltaMovement().y, var10003 * 0.4);
            target.hurtMarked = true;
            target.fallDistance = 0.0F;
            drop.sinceCatch++;
            if (pos.y <= drop.anchor.y + 0.15 || drop.sinceCatch > 30) {
               this.catchAtFloor(player, level, drop);
            }
         } else {
            this.creditLoopKill(player, drop);
            this.end(player, drop);
         }
      }
   }

   private void catchAtFloor(ServerPlayer player, ServerLevel level, RecursiveDropAbility.Drop drop) {
      Dmg.hurt(drop.target, level, player.damageSources().playerAttack(player), MasteryScaling.scaledDamage(2.0F, player, this.balanceKey()));
      Fx.burst(level, ParticleTypes.PORTAL, drop.anchor.add(0.0, 0.2, 0.0), 22, 0.5, 0.2);
      Fx.burst(level, ParticleTypes.REVERSE_PORTAL, drop.anchor.add(0.0, 0.2, 0.0), 12, 0.4, 0.1);
      Fx.sound(level, drop.anchor, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.02F, 1.3F);
      drop.sinceCatch = 0;
      if (this.creditLoopKill(player, drop)) {
         this.end(player, drop);
      } else {
         if (--drop.loopsLeft <= 0) {
            this.end(player, drop);
         } else {
            dropFromSky(level, drop.target, drop);
         }
      }
   }

   private boolean creditLoopKill(ServerPlayer player, RecursiveDropAbility.Drop drop) {
      if (!Foes.defeatedBy(drop.target, player)) {
         return false;
      } else {
         QuestSystem.feat(player, "rift_dropped_out");
         return true;
      }
   }

   private static void dropFromSky(ServerLevel level, LivingEntity target, RecursiveDropAbility.Drop drop) {
      double feetY = drop.anchor.y + Math.max(0.3, drop.height - (double)target.getBbHeight() - 0.05);
      target.teleportTo(drop.anchor.x, feetY, drop.anchor.z);
      target.setDeltaMovement(0.0, -0.2, 0.0);
      target.hurtMarked = true;
      target.fallDistance = 0.0F;
      Fx.burst(level, ParticleTypes.PORTAL, drop.anchor.add(0.0, drop.height, 0.0), 18, 0.5, 0.15);
      Fx.sound(level, drop.anchor, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.05F, 0.8F);
   }

   private void end(ServerPlayer player, RecursiveDropAbility.Drop drop) {
      this.drops.remove(player.getUUID());
      drop.closeMaws();
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      RecursiveDropAbility.Drop drop = this.drops.get(player.getUUID());
      if (drop != null) {
         drop.closeMaws();
      }

      super.clearPlayer(player);
   }

   private static double clearDropHeight(ServerLevel level, LivingEntity target) {
      for (double h = 4.0; h >= 1.6; h -= 0.5) {
         if (level.noCollision(target, target.getBoundingBox().move(0.0, Math.max(0.3, h - (double)target.getBbHeight()), 0.0))) {
            return h;
         }
      }

      return 1.6;
   }

   private static final class Drop {
      private final LivingEntity target;
      private final Vec3 anchor;
      private final double height;
      private final VfxEntity floorMaw;
      private final VfxEntity skyMaw;
      private int loopsLeft;
      private int sinceCatch;

      private Drop(LivingEntity target, Vec3 anchor, double height, int loopsLeft, VfxEntity floorMaw, VfxEntity skyMaw) {
         this.target = target;
         this.anchor = anchor;
         this.height = height;
         this.loopsLeft = loopsLeft;
         this.floorMaw = floorMaw;
         this.skyMaw = skyMaw;
      }

      private void closeMaws() {
         if (this.floorMaw.isAlive()) {
            this.floorMaw.discard();
         }

         if (this.skyMaw.isAlive()) {
            this.skyMaw.discard();
         }
      }
   }
}
