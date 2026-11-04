package com.devilfruits.mixin;

import com.devilfruits.core.poneglyph.Knowledge;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({CraftingMenu.class})
public abstract class CraftingMenuMixin {
   @WrapOperation(
      method = {"slotChangedCraftingGrid"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/inventory/ResultContainer;setRecipeUsed(Lnet/minecraft/world/level/Level;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/crafting/RecipeHolder;)Z"
      )}
   )
   private static boolean devilfruits$knowledgeGate(
      ResultContainer result, Level level, ServerPlayer player, RecipeHolder<?> recipe, Operation<Boolean> original
   ) {
      return Knowledge.allows(player, recipe.value()) && (Boolean)original.call(new Object[]{result, level, player, recipe});
   }
}
