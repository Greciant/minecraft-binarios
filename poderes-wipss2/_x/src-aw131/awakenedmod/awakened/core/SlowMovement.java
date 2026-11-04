package com.awakenedmod.awakened.core;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;

@EventBusSubscriber(
   modid = "awakened"
)
public final class SlowMovement {
   private static final ResourceLocation SPEED_ID = AwId.of("slowed_movement");
   private static final double SPRINT_JUMP_KICK = 0.2;

   private SlowMovement() {
   }

   @SubscribeEvent
   public static void onPlayerTick(Post event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         reconcile(player);
      }
   }

   private static void reconcile(ServerPlayer player) {
      int divisor = TickFreeze.divisorOf(player);
      double amount = divisor > 1 ? 1.0 / (double)divisor - 1.0 : 0.0;
      Attrs.set(player, Attributes.MOVEMENT_SPEED, SPEED_ID, amount, Operation.ADD_MULTIPLIED_TOTAL);
   }

   @SubscribeEvent
   public static void onJump(LivingJumpEvent event) {
      LivingEntity entity = event.getEntity();
      if (entity instanceof Player && entity.isSprinting()) {
         int divisor = TickFreeze.divisorOf(entity);
         if (divisor >= 2) {
            float yaw = entity.getYRot() * (float) (Math.PI / 180.0);
            double lost = 0.2 * (1.0 - 1.0 / (double)divisor);
            entity.setDeltaMovement(entity.getDeltaMovement().subtract(new Vec3((double)(-Mth.sin(yaw)) * lost, 0.0, (double)Mth.cos(yaw) * lost)));
         }
      }
   }
}
