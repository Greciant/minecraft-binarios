package com.devilfruits.core.poneglyph;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PoneglyphFaceBlockEntity extends BlockEntity {
   @Nullable
   private ResourceLocation poneglyph;

   public PoneglyphFaceBlockEntity(BlockPos pos, BlockState state) {
      super(Poneglyphs.PONEGLYPH_FACE_BE.get(), pos, state);
   }

   @Nullable
   public ResourceLocation poneglyph() {
      return this.poneglyph;
   }

   public void setPoneglyph(@Nullable ResourceLocation id) {
      this.poneglyph = id;
      this.setChanged();
   }

   protected void saveAdditional(CompoundTag tag, Provider registries) {
      super.saveAdditional(tag, registries);
      if (this.poneglyph != null) {
         tag.putString("poneglyph", this.poneglyph.toString());
      }
   }

   protected void loadAdditional(CompoundTag tag, Provider registries) {
      super.loadAdditional(tag, registries);
      this.poneglyph = tag.contains("poneglyph") ? ResourceLocation.tryParse(tag.getString("poneglyph")) : null;
   }
}
