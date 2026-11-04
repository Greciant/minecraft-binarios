package com.devilfruits.core.logia;

import com.devilfruits.core.physics.PhysicsConfig;
import com.devilfruits.core.physics.PhysicsWorld;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record LogiaEnvironment(
   @Nullable TagKey<Block> sourceBlocks,
   @Nullable TagKey<Biome> sourceBiomes,
   double sourceRegen,
   @Nullable TagKey<Biome> opposedBiomes,
   double opposedRegen,
   boolean rainOpposes
) {
   public static final LogiaEnvironment NONE = new LogiaEnvironment(null, null, 1.0, null, 1.0, false);

   public boolean atSource(Player player) {
      if (this.sourceBlocks == null
         || !player.getBlockStateOn().is(this.sourceBlocks) && !player.level().getBlockState(player.blockPosition()).is(this.sourceBlocks)) {
         if (this.sourceBlocks != null && (Boolean)PhysicsConfig.SAND_ON_STRUCTURES.get()) {
            BlockState deck = PhysicsWorld.blockUnder(player, 0.2);
            if (deck != null && deck.is(this.sourceBlocks)) {
               return true;
            }
         }

         return this.sourceBiomes != null && player.level().getBiome(player.blockPosition()).is(this.sourceBiomes);
      } else {
         return true;
      }
   }

   public boolean opposed(Player player) {
      return this.opposedBiomes != null && player.level().getBiome(player.blockPosition()).is(this.opposedBiomes)
         ? true
         : this.rainOpposes && player.level().isRainingAt(BlockPos.containing(player.getX(), player.getEyeY(), player.getZ()));
   }
}
