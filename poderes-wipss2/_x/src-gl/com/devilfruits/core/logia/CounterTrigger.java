package com.devilfruits.core.logia;

import com.devilfruits.core.rules.Weakness;
import com.devilfruits.registry.DFTags;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public record CounterTrigger(
   CounterTrigger.Kind kind, @Nullable TagKey<Block> blocks, @Nullable TagKey<DamageType> damageTypes, @Nullable Holder<MobEffect> effect, double radius
) {
   public static CounterTrigger of(CounterTrigger.Kind kind) {
      return new CounterTrigger(kind, null, null, null, 0.0);
   }

   public static CounterTrigger seastoneNear(double radius) {
      return new CounterTrigger(CounterTrigger.Kind.SEASTONE_NEAR, null, null, null, radius);
   }

   public static CounterTrigger standingOn(TagKey<Block> blocks) {
      return new CounterTrigger(CounterTrigger.Kind.BLOCK, blocks, null, null, 0.0);
   }

   public static CounterTrigger damage(TagKey<DamageType> types) {
      return new CounterTrigger(CounterTrigger.Kind.DAMAGE_TYPE, null, types, null, 0.0);
   }

   public static CounterTrigger effect(Holder<MobEffect> effect) {
      return new CounterTrigger(CounterTrigger.Kind.EFFECT, null, null, effect, 0.0);
   }

   public boolean isEvent() {
      return this.kind == CounterTrigger.Kind.SPLASH_WATER || this.kind == CounterTrigger.Kind.WATER_BUCKET || this.kind == CounterTrigger.Kind.DAMAGE_TYPE;
   }

   public boolean test(Player player) {
      Level level = player.level();

      return switch (this.kind) {
         case IN_WATER -> player.isInWater()
         || player.isUnderWater()
         || level.getFluidState(player.blockPosition()).is(FluidTags.WATER)
         || level.getFluidState(BlockPos.containing(player.getX(), player.getEyeY(), player.getZ())).is(FluidTags.WATER);
         case SUBMERGED -> Weakness.isSubmerged(player);
         case RAIN -> level.isRainingAt(BlockPos.containing(player.getX(), player.getEyeY(), player.getZ())) || level.isRainingAt(player.blockPosition());
         case SPLASH_WATER, WATER_BUCKET, DAMAGE_TYPE -> false;
         case SEASTONE_TOUCH -> Weakness.touchesSeastone(player);
         case SEASTONE_NEAR -> seastoneWithin(player, this.radius);
         case BLOCK -> this.blocks != null && (player.getBlockStateOn().is(this.blocks) || level.getBlockState(player.blockPosition()).is(this.blocks));
         case EFFECT -> this.effect != null && player.hasEffect(this.effect);
      };
   }

   private static boolean seastoneWithin(Player player, double radius) {
      int r = (int)Math.ceil(radius);
      BlockPos centre = player.blockPosition();
      double r2 = radius * radius;

      for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-r, -r, -r), centre.offset(r, r, r))) {
         if (pos.distSqr(centre) <= r2 && player.level().getBlockState(pos).is(DFTags.SEASTONE_BLOCKS)) {
            return true;
         }
      }

      return false;
   }

   public static enum Kind {
      IN_WATER,
      SUBMERGED,
      RAIN,
      SPLASH_WATER,
      WATER_BUCKET,
      SEASTONE_TOUCH,
      SEASTONE_NEAR,
      BLOCK,
      DAMAGE_TYPE,
      EFFECT;
   }
}
