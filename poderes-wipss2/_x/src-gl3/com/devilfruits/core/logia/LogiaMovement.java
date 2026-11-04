package com.devilfruits.core.logia;

import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.registry.DFAttachments;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class LogiaMovement {
   private static final List<LogiaMovementMode> MODES = new ArrayList<>();
   private static final Map<ResourceLocation, Integer> INDEX = new HashMap<>();
   private static final Map<UUID, LogiaMovement.Session> SESSIONS = new HashMap<>();

   public static synchronized void register(LogiaMovementMode mode) {
      if (INDEX.containsKey(mode.id())) {
         throw new IllegalStateException("Logia movement mode " + mode.id() + " registered twice");
      } else {
         INDEX.put(mode.id(), MODES.size());
         MODES.add(mode);
      }
   }

   public static List<LogiaMovementMode> modes() {
      return Collections.unmodifiableList(MODES);
   }

   public static int index(@Nullable ResourceLocation id) {
      Integer i = id == null ? null : INDEX.get(id);
      return i == null ? -1 : i;
   }

   @Nullable
   public static LogiaMovementMode byIndex(int index) {
      return index >= 0 && index < MODES.size() ? MODES.get(index) : null;
   }

   @Nullable
   public static LogiaMovement.Session session(ServerPlayer player) {
      return SESSIONS.get(player.getUUID());
   }

   public static boolean start(ServerPlayer player, LogiaMovementMode mode) {
      if (INDEX.containsKey(mode.id()) && !SESSIONS.containsKey(player.getUUID()) && mode.canStart(player)) {
         PlayerFruitData data = DFAttachments.data(player);
         if (data.logia().state() != LogiaState.ELEMENTAL_BODY) {
            if (mode.forcesBody()) {
               if (!LogiaManager.bodyReady(player) || player.isSpectator()) {
                  return false;
               }

               data.logia().setState(LogiaState.ELEMENTAL_BODY);
               data.logia().setLimbs(63);
               data.logia().setPartialTicks(0);
            } else if (!LogiaManager.setElemental(player, true)) {
               return false;
            }
         }

         LogiaMovement.Session session = new LogiaMovement.Session(mode, player.position(), player.level().getGameTime());
         SESSIONS.put(player.getUUID(), session);
         data.logia().setMovementMode(mode.id());
         mode.start(player, session);
         LogiaManager.sync(player, data);
         return true;
      } else {
         return false;
      }
   }

   static void tick(ServerPlayer player) {
      LogiaMovement.Session session = SESSIONS.get(player.getUUID());
      if (session != null) {
         session.ticks++;
         LogiaMovementMode mode = session.mode;
         PlayerFruitData data = DFAttachments.data(player);
         if (mode.drainPerSecond() > 0.0F && !AbilityManager.freeEnergy(player)) {
            data.setEnergy(Math.max(0.0F, data.energy() - mode.drainPerSecond() / 20.0F));
         }

         if (LogiaReform.fits(player, player.position(), mode.reformInFluid())) {
            session.lastSafe = player.position();
         }

         boolean allowed = data.logia().state() == LogiaState.ELEMENTAL_BODY
            && session.ticks <= mode.maxTicks()
            && (data.energy() > 0.0F || mode.forcesBody())
            && player.isAlive();
         if (!allowed) {
            LogiaManager.beginReform(player);
         } else if (!mode.tick(player, session)) {
            boolean stay = mode.staysElemental(player, session);
            end(player, true);
            if (!stay) {
               LogiaManager.beginReform(player);
            }
         }
      }
   }

   public static void end(ServerPlayer player, boolean normal) {
      LogiaMovement.Session session = SESSIONS.remove(player.getUUID());
      if (session != null) {
         PlayerFruitData data = DFAttachments.data(player);
         data.logia().setMovementMode(null);
         LogiaMovementMode mode = session.mode;
         Vec3 here = player.position();
         Vec3 wanted = mode.reformTarget(player, session);
         if (here.distanceToSqr(wanted) < 1.0 && LogiaReform.fits(player, here, mode.reformInFluid())) {
            player.fallDistance = 0.0F;
            mode.end(player, session, !normal);
         } else {
            Optional<Vec3> spot = LogiaReform.find(player, here, wanted, 2, mode.reformInFluid());
            if (spot.isEmpty() && !wanted.equals(here)) {
               spot = LogiaReform.find(player, here, here, 2, mode.reformInFluid());
            }

            Vec3 at = spot.orElse(session.lastSafe);
            if (at.distanceToSqr(here) > 1.0E-4) {
               LogiaReform.place(player, at);
            }

            player.fallDistance = 0.0F;
            mode.end(player, session, !normal);
         }
      }
   }

   public static void forget(ServerPlayer player) {
      SESSIONS.remove(player.getUUID());
   }

   private LogiaMovement() {
   }

   static {
      register(LogiaDash.MODE);
   }

   public static final class Session {
      public final LogiaMovementMode mode;
      public final Vec3 start;
      public final long startTick;
      public int ticks;
      public Vec3 lastSafe;
      public final Map<String, Object> data = new HashMap<>();

      Session(LogiaMovementMode mode, Vec3 start, long startTick) {
         this.mode = mode;
         this.start = start;
         this.startTick = startTick;
         this.lastSafe = start;
      }
   }
}
