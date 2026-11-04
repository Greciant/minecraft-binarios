package com.awakenedmod.awakened.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;

public final class CarriedLights {
   private static final int LEASE_TICKS = 1;
   private static final Map<UUID, CarriedLights.Lit> LIT = new HashMap<>();
   private static final List<CarriedLights.Stray> STRAYS = new ArrayList<>();

   private CarriedLights() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(CarriedLights::onServerTick);
      gameBus.addListener(CarriedLights::onServerStopping);
      gameBus.addListener(CarriedLights::onServerStarting);
   }

   public static boolean carry(Entity body) {
      if (body.level() instanceof ServerLevel level) {
         UUID var6 = body.getUUID();
         CarriedLights.Lit lit = LIT.get(var6);
         int now = level.getServer().getTickCount();
         BlockPos spot = spot(level, body, lit);
         if (lit != null && lit.level() == level && lit.pos().equals(spot)) {
            LIT.put(var6, new CarriedLights.Lit(level, spot, now));
            return false;
         } else {
            if (lit != null) {
               putOut(lit);
               LIT.remove(var6);
            }

            if (spot != null && LightBlocks.placeInAir(level, spot)) {
               LIT.put(var6, new CarriedLights.Lit(level, spot.immutable(), now));
               return true;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public static void drop(Entity body) {
      drop(body.getUUID());
   }

   public static void drop(UUID bodyId) {
      CarriedLights.Lit lit = LIT.remove(bodyId);
      if (lit != null) {
         putOut(lit);
      }
   }

   private static BlockPos spot(ServerLevel level, Entity body, CarriedLights.Lit lit) {
      BlockPos eyes = BlockPos.containing(body.getEyePosition());
      BlockPos[] candidates = new BlockPos[]{eyes, body.blockPosition(), eyes.above()};

      for (BlockPos pos : candidates) {
         boolean ours = lit != null && lit.level() == level && lit.pos().equals(pos);
         if (ours || level.getBlockState(pos).isAir()) {
            return pos;
         }
      }

      return null;
   }

   private static void putOut(CarriedLights.Lit lit) {
      putOut(lit.level(), lit.pos());
   }

   private static void putOut(ServerLevel level, BlockPos pos) {
      if (level.isLoaded(pos)) {
         LightBlocks.remove(level, pos);
      } else {
         STRAYS.add(new CarriedLights.Stray(level, pos));
      }
   }

   private static void onServerTick(Post event) {
      if (!LIT.isEmpty() || !STRAYS.isEmpty()) {
         int now = event.getServer().getTickCount();
         Iterator<CarriedLights.Lit> lights = LIT.values().iterator();

         while (lights.hasNext()) {
            CarriedLights.Lit lit = lights.next();
            if (now - lit.touched() > 1) {
               lights.remove();
               putOut(lit);
            }
         }

         STRAYS.removeIf(stray -> {
            if (!stray.level().isLoaded(stray.pos())) {
               return false;
            } else {
               LightBlocks.remove(stray.level(), stray.pos());
               return true;
            }
         });
      }
   }

   private static void onServerStopping(ServerStoppingEvent event) {
      for (CarriedLights.Lit lit : LIT.values()) {
         STRAYS.add(new CarriedLights.Stray(lit.level(), lit.pos()));
      }

      LIT.clear();

      for (CarriedLights.Stray stray : STRAYS) {
         stray.level().getChunkAt(stray.pos());
         LightBlocks.remove(stray.level(), stray.pos());
      }

      STRAYS.clear();
   }

   private static void onServerStarting(ServerAboutToStartEvent event) {
      LIT.clear();
      STRAYS.clear();
   }

   private static record Lit(ServerLevel level, BlockPos pos, int touched) {
   }

   private static record Stray(ServerLevel level, BlockPos pos) {
   }
}
