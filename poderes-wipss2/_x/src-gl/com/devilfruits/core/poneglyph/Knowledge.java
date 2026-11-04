package com.devilfruits.core.poneglyph;

import com.devilfruits.core.network.DFNetwork;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.NeoForge;

public final class Knowledge {
   public static PoneglyphPlayerData.PlayerKnowledge get(Player player) {
      return (PoneglyphPlayerData.PlayerKnowledge)player.getData(Poneglyphs.KNOWLEDGE);
   }

   public static boolean knows(Player player, ResourceLocation id) {
      return get(player).knows(id);
   }

   public static Knowledge.Outcome learn(ServerPlayer player, ResourceLocation id) {
      PoneglyphData.Knowledge def = PoneglyphData.knowledge(id);
      if (def == null) {
         return new Knowledge.Outcome(Knowledge.Result.UNKNOWN);
      } else if (knows(player, id)) {
         return new Knowledge.Outcome(Knowledge.Result.ALREADY);
      } else {
         player.setData(Poneglyphs.KNOWLEDGE, get(player).plus(id));
         player.awardRecipes(recipes(player.server, id));
         NeoForge.EVENT_BUS.post(new PoneglyphApiEvents.KnowledgeLearnedEvent(player, id));
         Poneglyphs.KNOWLEDGE_LEARNED.get().trigger(player, id);
         DFNetwork.sendTo(player, new PoneglyphNetwork.KnowledgeToastPayload(id, def.nameKey(id), def.icon()));
         player.sendSystemMessage(Component.translatable("poneglyph.devilfruits.message.learned", new Object[]{name(id)}).withStyle(ChatFormatting.GOLD));

         for (ResourceLocation included : def.requires()) {
            learn(player, included);
         }

         return new Knowledge.Outcome(Knowledge.Result.LEARNED);
      }
   }

   public static boolean revoke(ServerPlayer player, ResourceLocation id) {
      if (!knows(player, id)) {
         return false;
      } else {
         player.setData(Poneglyphs.KNOWLEDGE, get(player).minus(id));
         player.resetRecipes(recipes(player.server, id));
         return true;
      }
   }

   public static void reaward(ServerPlayer player) {
      for (ResourceLocation id : get(player).learned()) {
         player.awardRecipes(recipes(player.server, id));
      }
   }

   public static List<RecipeHolder<?>> recipes(MinecraftServer server, ResourceLocation id) {
      Map<ResourceLocation, RecipeHolder<?>> out = new LinkedHashMap<>();
      PoneglyphData.Knowledge def = PoneglyphData.knowledge(id);
      if (def != null) {
         for (ResourceLocation r : def.recipes()) {
            server.getRecipeManager().byKey(r).ifPresent(hx -> out.put(hx.id(), hx));
         }
      }

      for (RecipeHolder<?> h : server.getRecipeManager().getRecipes()) {
         Recipe var7 = h.value();
         if (var7 instanceof KnowledgeRecipes.Locked) {
            KnowledgeRecipes.Locked locked = (KnowledgeRecipes.Locked)var7;
            if (locked.knowledge().equals(id)) {
               out.put(h.id(), h);
            }
         }
      }

      return new ArrayList<>(out.values());
   }

   public static boolean allows(@Nullable Player player, Recipe<?> recipe) {
      if (recipe instanceof KnowledgeRecipes.Locked locked
         && (Boolean)PoneglyphConfig.GATE_RECIPES.get()
         && !((List)PoneglyphConfig.UNGATED_KNOWLEDGE.get()).contains(locked.knowledge().toString())) {
         return player != null && knows(player, locked.knowledge());
      }

      return true;
   }

   public static Component name(ResourceLocation id) {
      PoneglyphData.Knowledge def = PoneglyphData.knowledge(id);
      return Component.translatable(def != null ? def.nameKey(id) : PoneglyphData.Knowledge.nameKey(id.getNamespace(), id.getPath()));
   }

   private Knowledge() {
   }

   public static record Outcome(Knowledge.Result result) {
   }

   public static enum Result {
      LEARNED,
      ALREADY,
      UNKNOWN;
   }
}
