package com.awakenedmod.awakened.content.command.vocab.effect;

import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.core.Attrs;
import com.awakenedmod.awakened.core.AwId;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class WeighsATonEffect implements Effect {
   private static final ResourceLocation GRAVITY_ID = AwId.of("command_weighs_a_ton");
   private static final double FLIER_PULL_PER_LEVEL = 0.03;
   private static final double HOVERING_DY = -0.05;

   @Override
   public String id() {
      return "weighs_a_ton";
   }

   @Override
   public int unlockLevel() {
      return 3;
   }

   @Override
   public double defaultWeight() {
      return 15.0;
   }

   @Override
   public boolean hasLevel() {
      return true;
   }

   @Override
   public String numberUnit() {
      return "awakened.command.unit.bare";
   }

   @Override
   public boolean apply(ServerPlayer caster, LivingEntity target, int level, int age) {
      int times = Math.max(1, level) + 1;
      Attrs.set(target, Attributes.GRAVITY, GRAVITY_ID, (double)times - 1.0, Operation.ADD_MULTIPLIED_TOTAL);
      if (target.onGround()) {
         return false;
      } else if (ignoresGravity(target)) {
         Vec3 v = target.getDeltaMovement();
         target.setDeltaMovement(v.x, v.y - 0.03 * (double)times, v.z);
         target.hurtMarked = true;
         return true;
      } else {
         return true;
      }
   }

   private static boolean ignoresGravity(LivingEntity target) {
      return !(target instanceof Player) && !target.isInWater() && !target.isInLava()
         ? target.isNoGravity() || target instanceof FlyingAnimal || target.getDeltaMovement().y > -0.05
         : false;
   }

   @Override
   public void release(LivingEntity target) {
      Attrs.set(target, Attributes.GRAVITY, GRAVITY_ID, 0.0, Operation.ADD_MULTIPLIED_TOTAL);
   }
}
