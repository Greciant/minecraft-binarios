package com.awakenedmod.awakened.content.command.vocab.effect;

import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.core.Attrs;
import com.awakenedmod.awakened.core.AwId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;

public final class ReachesFurtherEffect implements Effect {
   private static final ResourceLocation BLOCK_REACH_ID = AwId.of("command_reaches_further_block");
   private static final ResourceLocation ENTITY_REACH_ID = AwId.of("command_reaches_further_entity");
   private static final Map<UUID, ReachesFurtherEffect.Claim> CLAIMS = new ConcurrentHashMap<>();

   @Override
   public String id() {
      return "reaches_further";
   }

   @Override
   public boolean hostile() {
      return false;
   }

   @Override
   public int unlockLevel() {
      return 5;
   }

   @Override
   public double defaultWeight() {
      return 20.0;
   }

   @Override
   public boolean hasLevel() {
      return true;
   }

   @Override
   public String numberUnit() {
      return "awakened.command.unit.blocks";
   }

   @Override
   public boolean apply(ServerPlayer caster, LivingEntity target, int level, int age) {
      if (!(target instanceof Player)) {
         return false;
      } else {
         double blocks = CLAIMS.computeIfAbsent(target.getUUID(), id -> new ReachesFurtherEffect.Claim())
            .ask(target.level().getGameTime(), (double)Math.max(1, level));
         boolean fresh = !carries(target, blocks);
         Attrs.set(target, Attributes.BLOCK_INTERACTION_RANGE, BLOCK_REACH_ID, blocks, Operation.ADD_VALUE);
         Attrs.set(target, Attributes.ENTITY_INTERACTION_RANGE, ENTITY_REACH_ID, blocks, Operation.ADD_VALUE);
         return fresh;
      }
   }

   private static boolean carries(LivingEntity target, double blocks) {
      AttributeInstance inst = target.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
      AttributeModifier current = inst == null ? null : inst.getModifier(BLOCK_REACH_ID);
      return current != null && Math.abs(current.amount() - blocks) < 1.0E-6;
   }

   @Override
   public void release(LivingEntity target) {
      CLAIMS.remove(target.getUUID());
      Attrs.set(target, Attributes.BLOCK_INTERACTION_RANGE, BLOCK_REACH_ID, 0.0, Operation.ADD_VALUE);
      Attrs.set(target, Attributes.ENTITY_INTERACTION_RANGE, ENTITY_REACH_ID, 0.0, Operation.ADD_VALUE);
   }

   @Override
   public boolean playersOnly() {
      return true;
   }

   private static final class Claim {
      long tick = Long.MIN_VALUE;
      double now;
      double last;

      double ask(long gameTime, double blocks) {
         if (gameTime != this.tick) {
            this.last = this.tick == gameTime - 1L ? this.now : 0.0;
            this.now = 0.0;
            this.tick = gameTime;
         }

         this.now = Math.max(this.now, blocks);
         return Math.max(this.now, this.last);
      }
   }
}
