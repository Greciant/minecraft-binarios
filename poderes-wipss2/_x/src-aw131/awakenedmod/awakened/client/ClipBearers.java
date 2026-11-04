package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.npc.ClipBearer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;

public final class ClipBearers {
   private static final Int2ObjectMap<LivingEntity> TRACKED = new Int2ObjectOpenHashMap();
   private static final List<IntConsumer> FORGET = new ArrayList<>();

   private ClipBearers() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(ClipBearers::onJoin);
      gameBus.addListener(ClipBearers::onLeave);
      gameBus.addListener(ClipBearers::onLevelUnload);
   }

   public static void onForget(IntConsumer listener) {
      FORGET.add(listener);
   }

   private static void forget(int id) {
      for (IntConsumer listener : FORGET) {
         listener.accept(id);
      }
   }

   private static void onLevelUnload(Unload event) {
      if (event.getLevel().isClientSide()) {
         TRACKED.values().removeIf(e -> {
            if (e.level() != event.getLevel()) {
               return false;
            } else {
               forget(e.getId());
               return true;
            }
         });
      }
   }

   private static void onJoin(EntityJoinLevelEvent event) {
      if (event.getLevel().isClientSide() && event.getEntity() instanceof ClipBearer && event.getEntity() instanceof LivingEntity living) {
         TRACKED.put(living.getId(), living);
      }
   }

   private static void onLeave(EntityLeaveLevelEvent event) {
      if (event.getLevel().isClientSide() && TRACKED.remove(event.getEntity().getId()) != null) {
         forget(event.getEntity().getId());
      }
   }

   @Nullable
   public static ClipBearer get(int entityId) {
      if (TRACKED.isEmpty()) {
         return null;
      } else {
         LivingEntity e = (LivingEntity)TRACKED.get(entityId);
         return e != null && !e.isRemoved() && e.level() == Minecraft.getInstance().level ? (ClipBearer)e : null;
      }
   }

   public static void forEachCaster(Level level, Consumer<LivingEntity> action) {
      for (Player p : level.players()) {
         action.accept(p);
      }

      if (!TRACKED.isEmpty()) {
         ObjectIterator var4 = TRACKED.values().iterator();

         while (var4.hasNext()) {
            LivingEntity e = (LivingEntity)var4.next();
            if (e.level() == level && !e.isRemoved()) {
               action.accept(e);
            }
         }
      }
   }

   public static boolean isBearer(Entity entity) {
      return !TRACKED.isEmpty() && TRACKED.get(entity.getId()) == entity;
   }
}
