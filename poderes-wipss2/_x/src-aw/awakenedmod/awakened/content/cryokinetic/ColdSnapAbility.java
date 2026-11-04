package com.awakenedmod.awakened.content.cryokinetic;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.entity.projectile.KineticProjectile;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

public final class ColdSnapAbility extends Ability {
   private static final float DAMAGE = 12.0F;
   private static final float COST = 15.0F;
   private static final int FROSTBITE_STACKS = 1;
   private static final double AIM_DISTANCE = 64.0;
   private static final double AIM_ASSIST_DEG = 1.5;
   private static final float VELOCITY = 3.2F;
   private static final double KNOCKBACK = 0.45;
   private static final double TRAIL_CLEAR = 2.5;

   public ColdSnapAbility() {
      super("cold_snap", 15.0F, 3);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(12.0).build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 hand = AimUtil.handPosition(player);
      Vec3 dir = AimUtil.throwDirectionAtBody(player, hand, 64.0, 1.5);
      KineticProjectile icicle = new KineticProjectile(AwakenedEntities.ICE_SHARD.get(), player, level, new ItemStack(Items.PACKED_ICE));
      icicle.setPos(hand.x, hand.y, hand.z);
      icicle.configure(MasteryScaling.scaledDamage(12.0F, player, this.balanceKey()), 0.0F, 0.0F, 0.15F);
      icicle.setGravity(0.0);
      icicle.setCustomDetonation(true);
      icicle.setOnDetonate(pos -> {
         if (icicle.directHit() != null) {
            this.strike(player, level, icicle.directHit(), icicle.getDeltaMovement());
         } else {
            CryoFx.shardImpact(level, pos);
         }
      });
      Vec3 eye = player.getEyePosition();
      icicle.setOnTick(at -> {
         if (at.distanceToSqr(eye) > 6.25) {
            CryoFx.icicleTrail(level, at);
         }
      });
      icicle.shoot(dir.x, dir.y, dir.z, 3.2F, 0.0F);
      level.addFreshEntity(icicle);
      CryoFx.shardLaunch(level, hand.add(player.getLookAngle().scale(1.2)));
      Fx.shakeFor(player, player.getEyePosition(), 4.0, 0.03F, 1, 3);
      player.swing(InteractionHand.MAIN_HAND);
      PowerService.playAnimation(player, "cold_snap");
      return true;
   }

   private void strike(ServerPlayer player, ServerLevel level, Entity hit, Vec3 flight) {
      if ((hit instanceof PartEntity<?> part ? part.getParent() : hit) instanceof LivingEntity target && Harm.allowed(player, target)) {
         Frostbite.apply(player, target, 1);
         Vec3 push = new Vec3(flight.x, 0.0, flight.z);
         if (push.lengthSqr() > 1.0E-4) {
            push = push.normalize().scale(0.45);
            target.push(push.x, 0.18, push.z);
            target.hurtMarked = true;
         }

         CryoFx.javelinHit(level, target);
         return;
      }
   }
}
