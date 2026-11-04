package com.devilfruits.fruits.gura_gura;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.data.LegacyData;
import com.devilfruits.core.rules.WorldFruitData;
import com.devilfruits.core.world.BlockRestoreData;
import com.devilfruits.registry.DFAttachments;
import com.devilfruits.registry.DFDataComponents;
import com.devilfruits.registry.DFItems;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

final class GuraLegacy {
   private static final String OLD = "gura_gura";

   static void register() {
      DFItems.ITEMS.addAlias(old("gura_gura_no_mi"), DevilFruits.id("gura_gura_no_mi"));
      DFItems.ITEMS.addAlias(old("bisento"), DevilFruits.id("bisento"));
      DFItems.ITEMS.addAlias(old("ability_icon"), DevilFruits.id("ability_icon"));
      DFDataComponents.COMPONENTS.addAlias(old("fruit_instance"), DevilFruits.id("fruit_instance"));
      DFDataComponents.COMPONENTS.addAlias(old("ability"), DevilFruits.id("ability"));
      DFAttachments.ATTACHMENTS.addAlias(old("power"), DevilFruits.id("player_data"));
      LegacyData.setSingleFruitOwner(GuraGura.ID);
      NeoForge.EVENT_BUS.addListener(GuraLegacy::onServerStarted);
   }

   private static ResourceLocation old(String path) {
      return ResourceLocation.fromNamespaceAndPath("gura_gura", path);
   }

   private static void onServerStarted(ServerStartedEvent event) {
      MinecraftServer server = event.getServer();
      WorldFruitData data = WorldFruitData.get(server);
      if (data.isFresh()) {
         GuraLegacy.OldFruitState old = (GuraLegacy.OldFruitState)server.overworld()
            .getDataStorage()
            .get(new Factory(GuraLegacy.OldFruitState::new, GuraLegacy.OldFruitState::load), "gura_gura_fruit");
         if (old != null) {
            data.set(GuraGura.ID, old.state, old.instance, old.owner, old.ownerName);
            DevilFruits.LOGGER.info("Imported the Gura Gura no Mi world state ({}) from a 1.x save", old.state);
         }
      }

      for (ServerLevel level : server.getAllLevels()) {
         BlockRestoreData oldRestore = (BlockRestoreData)level.getDataStorage().get(BlockRestoreData.factory(), "gura_gura_restore");
         if (oldRestore != null && oldRestore.size() > 0) {
            int count = oldRestore.size();
            BlockRestoreData.get(level).absorb(oldRestore);
            DevilFruits.LOGGER.info("Imported {} pending terrain restores in {} from a 1.x save", count, level.dimension().location());
         }
      }
   }

   private GuraLegacy() {
   }

   private static final class OldFruitState extends SavedData {
      WorldFruitData.State state = WorldFruitData.State.FREE;
      @Nullable
      UUID instance;
      @Nullable
      UUID owner;
      String ownerName = "";

      static GuraLegacy.OldFruitState load(CompoundTag tag, Provider registries) {
         GuraLegacy.OldFruitState s = new GuraLegacy.OldFruitState();

         try {
            s.state = WorldFruitData.State.valueOf(tag.getString("state"));
         } catch (IllegalArgumentException var4) {
            s.state = WorldFruitData.State.FREE;
         }

         s.instance = tag.hasUUID("instance") ? tag.getUUID("instance") : null;
         s.owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
         s.ownerName = tag.getString("owner_name");
         return s;
      }

      public CompoundTag save(CompoundTag tag, Provider registries) {
         return tag;
      }
   }
}
