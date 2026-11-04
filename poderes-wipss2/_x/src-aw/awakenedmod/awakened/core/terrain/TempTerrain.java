package com.awakenedmod.awakened.core.terrain;

import com.awakenedmod.awakened.Config;
import com.awakenedmod.awakened.core.AwakenedConfigs;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

public final class TempTerrain {
   public static final int MAX_PLAN_BLOCKS = 262144;

   private TempTerrain() {
   }

   public static void register(IEventBus modBus, IEventBus gameBus) {
      TerrainLedger.register(modBus);
      TerrainScheduler.register(gameBus);
      PlayerEdits.register(gameBus);
      gameBus.addListener(TempTerrain::adoptOldVisualOnlyKey);
      AwakenedConfigs.hide(Config.BLOCK_BREAKING, Config.OLD_RUIN_VISUAL_ONLY_TERRAIN, Config.OLD_ALLOW_TERRAIN_DAMAGE);
   }

   public static BlockBreaking breaking() {
      return Config.SPEC.isLoaded() ? (BlockBreaking)Config.BLOCK_BREAKING.get() : BlockBreaking.RESTORE;
   }

   public static boolean visualOnly() {
      return breaking() == BlockBreaking.OFF;
   }

   public static boolean permanent() {
      return breaking() == BlockBreaking.PERMANENT;
   }

   private static void adoptOldVisualOnlyKey(ServerStartedEvent event) {
      BlockBreaking legacy = Config.OLD_RUIN_VISUAL_ONLY_TERRAIN.get()
         ? BlockBreaking.OFF
         : (Config.OLD_ALLOW_TERRAIN_DAMAGE.get() ? BlockBreaking.PERMANENT : null);
      AwakenedConfigs.adopt(Config.BLOCK_BREAKING, legacy, event.getServer(), () -> {
         AwakenedConfigs.write(Config.OLD_RUIN_VISUAL_ONLY_TERRAIN, false, event.getServer());
         AwakenedConfigs.write(Config.OLD_ALLOW_TERRAIN_DAMAGE, false, event.getServer());
      });
   }

   public static Handle carve(ServerLevel level, @Nullable ServerPlayer actor, CarvePlan plan) {
      return carve(level, actor, plan, null);
   }

   public static Handle carve(ServerLevel level, @Nullable ServerPlayer actor, CarvePlan plan, @Nullable CarveReads reads) {
      return TerrainScheduler.start(level, PlanResolver.resolve(level, actor, plan, reads), plan, visualOnly());
   }

   public static CarveReads reads(ServerLevel level, @Nullable ServerPlayer actor) {
      return new CarveReads(level, actor);
   }

   public static boolean isProbe(Event event) {
      return event instanceof TerrainProtection.Probe;
   }
}
