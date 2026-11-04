package com.awakenedmod.awakened.content.summoning;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TraceableEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSplitEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent.Post;
import net.neoforged.neoforge.event.tick.EntityTickEvent.Pre;

public final class SummonChain {
   private static Mob summoning;
   private static long summoningTick;
   private static Mob finalized;
   private static boolean finalizedIsHostileSpawn;

   private SummonChain() {
   }

   static void register(IEventBus gameBus) {
      gameBus.addListener(SummonChain::onPreTick);
      gameBus.addListener(SummonChain::onPostTick);
      gameBus.addListener(SummonChain::onFinalizeSpawn);
      gameBus.addListener(SummonChain::onSplit);
   }

   static void adoptIfSummonedByMinion(Mob child) {
      Mob summoner = summonerOf(child);
      if (summoner != null) {
         MinionSupport.adopt(summoner, child);
      }
   }

   private static Mob summonerOf(Mob child) {
      if (child instanceof TraceableEntity traceable && traceable.getOwner() != null) {
         return traceable.getOwner() instanceof Mob declared ? declared : null;
      }

      if (spawnedForItsOwnReasons(child)) {
         return null;
      } else {
         return summoning != null && summoningTick == child.level().getGameTime() ? summoning : null;
      }
   }

   private static boolean spawnedForItsOwnReasons(Mob child) {
      return child == finalized && finalizedIsHostileSpawn;
   }

   private static void onFinalizeSpawn(FinalizeSpawnEvent event) {
      finalized = event.getEntity();
      finalizedIsHostileSpawn = event.getSpawnType() == MobSpawnType.REINFORCEMENT;
   }

   private static void onPreTick(Pre event) {
      if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide()) {
         if (summoningTick != mob.level().getGameTime()) {
            summoning = null;
         }

         MinionData data = SummonAttachments.minion(mob);
         if (data.isMinion() && !data.conjured()) {
            summoning = mob;
            summoningTick = mob.level().getGameTime();
         }

         return;
      }
   }

   private static void onPostTick(Post event) {
      if (!event.getEntity().level().isClientSide()) {
         summoning = null;
         finalized = null;
      }
   }

   private static void onSplit(MobSplitEvent event) {
      for (Mob child : event.getChildren()) {
         MinionSupport.adopt(event.getParent(), child);
      }
   }
}
