package com.awakenedmod.awakened.core.quest;

import com.awakenedmod.awakened.core.curios.CuriosSlots;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;

public final class QuestPlaces {
   private static final Map<String, ResourceKey<?>> KEYS = new ConcurrentHashMap<>();

   private QuestPlaces() {
   }

   public static boolean atPlace(ServerPlayer player, String target) {
      int slash = target.indexOf(47);
      if (slash > 0 && slash != target.length() - 1) {
         String kind = target.substring(0, slash);
         ResourceLocation id = ResourceLocation.tryParse(target.substring(slash + 1));
         if (id == null) {
            return false;
         } else {
            return switch (kind) {
               case "structure" -> inStructure(player, id);
               case "biome" -> player.serverLevel().getBiome(player.blockPosition()).is(key(target, Registries.BIOME, id));
               case "dimension" -> player.serverLevel().dimension().equals(ResourceKey.create(Registries.DIMENSION, id));
               default -> false;
            };
         }
      } else {
         return false;
      }
   }

   private static boolean inStructure(ServerPlayer player, ResourceLocation id) {
      ResourceKey<Structure> key = key("structure/" + id, Registries.STRUCTURE, id);
      return player.serverLevel().structureManager().getStructureWithPieceAt(player.blockPosition(), holder -> holder.is(key)).isValid();
   }

   private static <T> ResourceKey<T> key(String cacheKey, ResourceKey<Registry<T>> registry, ResourceLocation id) {
      return (ResourceKey<T>)KEYS.computeIfAbsent(cacheKey, k -> ResourceKey.create(registry, id));
   }

   public static int countItem(ServerPlayer player, String itemId) {
      int found = 0;
      Inventory inv = player.getInventory();

      for (int i = 0; i < inv.getContainerSize(); i++) {
         found += matching(inv.getItem(i), itemId);
      }

      for (ItemStack stack : CuriosSlots.worn(player)) {
         found += matching(stack, itemId);
      }

      return found;
   }

   private static int matching(ItemStack stack, String itemId) {
      if (stack.isEmpty()) {
         return 0;
      } else {
         return itemId.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()) ? stack.getCount() : 0;
      }
   }
}
