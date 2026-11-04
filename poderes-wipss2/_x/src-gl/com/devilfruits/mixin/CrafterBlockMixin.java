package com.devilfruits.mixin;

import com.devilfruits.core.poneglyph.Knowledge;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.Optional;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.CrafterBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({CrafterBlock.class})
public abstract class CrafterBlockMixin {
   @ModifyReturnValue(
      method = {"getPotentialResults"},
      at = {@At("RETURN")}
   )
   private static Optional<RecipeHolder<CraftingRecipe>> devilfruits$knowledgeGate(Optional<RecipeHolder<CraftingRecipe>> found) {
      return found.filter(h -> Knowledge.allows(null, h.value()));
   }
}
