package com.devilfruits.fruits.suke_suke.clear;

import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.PhysicsStructure;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.physics.StructureBlock;
import com.devilfruits.fruits.suke_suke.SukeFx;
import com.devilfruits.fruits.suke_suke.SukeNetwork;
import com.devilfruits.fruits.suke_suke.SukeSounds;
import com.devilfruits.fruits.suke_suke.SukeSuke;
import com.devilfruits.fruits.suke_suke.SukeTriggers;
import com.devilfruits.registry.DFAttachments;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.fml.ModList;

public final class ShipCloak {
   public static final String SABLE = "sable";
   private static final Map<UUID, ShipCloak.Hidden> SHIPS = new LinkedHashMap<>();
   private static ShipCloak.World world = new ShipCloak.PhysicsShips();

   public static ShipCloak.World world() {
      return world;
   }

   public static void world(@Nullable ShipCloak.World fake) {
      world = (ShipCloak.World)(fake == null ? new ShipCloak.PhysicsShips() : fake);
   }

   public static boolean hidden(UUID ship) {
      return SHIPS.containsKey(ship);
   }

   public static long endOf(UUID ship) {
      ShipCloak.Hidden h = SHIPS.get(ship);
      return h == null ? -1L : h.end;
   }

   public static Set<Entity> riders(UUID ship) {
      ShipCloak.Hidden h = SHIPS.get(ship);
      return h == null ? Set.of() : Set.copyOf(h.riders);
   }

   public static double seconds(int mastery) {
      double t = (double)Math.max(0, Math.min(2, mastery - 8)) / 2.0;
      return SukeSuke.setting("shipSecondsMastery8") + (SukeSuke.setting("shipSecondsMastery10") - SukeSuke.setting("shipSecondsMastery8")) * t;
   }

   @Nullable
   public static ShipCloak.Ship target(ServerPlayer player) {
      ShipCloak.Ship s = world.under(player);
      return s != null ? s : world.aimed(player, SukeSuke.setting("shipReach"));
   }

   public static void start(ServerPlayer caster, ShipCloak.Ship ship, int ticks) {
      ServerLevel level = caster.serverLevel();
      long now = level.getGameTime();
      ShipCloak.Hidden h = new ShipCloak.Hidden(ship, level.dimension(), caster.getUUID(), now, now + (long)ticks);
      SHIPS.put(ship.id(), h);
      SukeNetwork.toLevel(level, h.payload(true, now));
      ServerFx.send(caster, SukeFx.SHIP_CAST, caster.position(), Vec3.ZERO, (float)ticks, 0L, 0);
      Vec3 c = ship.worldBounds().getCenter();
      level.playSound(null, c.x, c.y, c.z, SukeSounds.SHIP_VANISH.get(), SoundSource.PLAYERS, 2.0F, 1.0F);
      SukeTriggers.GHOST_SHIP.get().trigger(caster);
      updateRiders(level, h);
   }

   public static List<UUID> hiddenBy(ServerPlayer caster) {
      List<UUID> out = new ArrayList<>();

      for (ShipCloak.Hidden h : SHIPS.values()) {
         if (h.caster.equals(caster.getUUID())) {
            out.add(h.ship.id());
         }
      }

      return out;
   }

   public static void stop(MinecraftServer server, UUID ship) {
      ShipCloak.Hidden h = SHIPS.remove(ship);
      if (h != null) {
         finish(server.getLevel(h.dim), h);
      }
   }

   public static void stopAll(MinecraftServer server) {
      for (UUID id : List.copyOf(SHIPS.keySet())) {
         stop(server, id);
      }
   }

   public static void tick(MinecraftServer server) {
      if (!SHIPS.isEmpty()) {
         for (ShipCloak.Hidden h : List.copyOf(SHIPS.values())) {
            ServerLevel level = server.getLevel(h.dim);
            if (level != null && h.ship.valid() && level.getGameTime() < h.end) {
               updateRiders(level, h);
            } else {
               SHIPS.remove(h.ship.id());
               finish(level, h);
            }
         }
      }
   }

   private static void updateRiders(ServerLevel level, ShipCloak.Hidden h) {
      Set<Entity> now = new HashSet<>();

      for (Entity e : world.nearby(level, h.ship)) {
         if (e.isAlive()) {
            if (e instanceof ServerPlayer) {
               ServerPlayer p = (ServerPlayer)e;
               if (p.isSpectator()) {
                  continue;
               }
            }

            if (world.on(e, h.ship)) {
               now.add(e);
            }
         }
      }

      for (Entity ex : List.copyOf(h.riders)) {
         if (!now.contains(ex)) {
            h.riders.remove(ex);
            offShip(ex);
         }
      }

      for (Entity exx : now) {
         if (h.riders.add(exx) && !Clear.invisible(exx)) {
            Clear.vanish(exx, h.caster, true);
         }
      }
   }

   private static void offShip(Entity e) {
      if (Clear.of(e).has(4)) {
         Clear.appear(e);
      }
   }

   private static void finish(@Nullable ServerLevel level, ShipCloak.Hidden h) {
      for (Entity e : h.riders) {
         offShip(e);
      }

      h.riders.clear();
      if (level != null) {
         SukeNetwork.toLevel(level, h.payload(false, level.getGameTime()));
         if (h.ship.valid()) {
            Vec3 c = h.ship.worldBounds().getCenter();
            level.playSound(null, c.x, c.y, c.z, SukeSounds.SHIP_APPEAR.get(), SoundSource.PLAYERS, 2.0F, 1.0F);
         }
      }
   }

   public static void syncTo(ServerPlayer player) {
      for (ShipCloak.Hidden h : SHIPS.values()) {
         if (h.dim == player.level().dimension()) {
            SukeNetwork.toPlayer(player, h.payload(true, 0L));
         }
      }
   }

   public static void clear() {
      SHIPS.clear();
   }

   public static Component commandTest(ServerPlayer player, PhysicsStructure s, StructureBlock nearest) {
      ShipCloak.Ship ship = ShipCloak.PhysicsShips.wrap(s);
      int mastery = DFAttachments.data(player).mastery();
      int ticks = (int)Math.round(seconds(Math.max(8, mastery)) * 20.0);
      start(player, ship, ticks);
      return Component.literal("Ship " + s.id() + " invisible for " + ticks / 20 + " s (riders: " + riders(ship.id()).size() + ")");
   }

   private ShipCloak() {
   }

   private static final class Hidden {
      final ShipCloak.Ship ship;
      final ResourceKey<Level> dim;
      final UUID caster;
      final long start;
      final long end;
      final Set<Entity> riders = new HashSet<>();

      Hidden(ShipCloak.Ship ship, ResourceKey<Level> dim, UUID caster, long start, long end) {
         this.ship = ship;
         this.dim = dim;
         this.caster = caster;
         this.start = start;
         this.end = end;
      }

      SukeNetwork.ShipPayload payload(boolean on, long now) {
         return on
            ? new SukeNetwork.ShipPayload(this.ship.id(), this.caster, this.start, this.end)
            : new SukeNetwork.ShipPayload(this.ship.id(), this.caster, now, -1L);
      }
   }

   private static final class PhysicsShips implements ShipCloak.World {
      static ShipCloak.Ship wrap(final PhysicsStructure s) {
         return new ShipCloak.Ship() {
            @Override
            public UUID id() {
               return s.id();
            }

            @Override
            public boolean valid() {
               return s.isValid();
            }

            @Override
            public AABB worldBounds() {
               return s.worldBounds();
            }
         };
      }

      @Override
      public boolean available() {
         return ModList.get().isLoaded("sable") && PhysicsWorld.active();
      }

      @Nullable
      @Override
      public ShipCloak.Ship under(Entity entity) {
         PhysicsStructure s = PhysicsWorld.structureUnder(entity);
         return s == null ? null : wrap(s);
      }

      @Nullable
      @Override
      public ShipCloak.Ship aimed(ServerPlayer player, double reach) {
         Vec3 eye = player.getEyePosition();
         Vec3 to = eye.add(player.getLookAngle().scale(reach));
         BlockHitResult hit = player.level().clip(new ClipContext(eye, to, Block.OUTLINE, Fluid.NONE, player));
         if (hit.getType() != Type.BLOCK) {
            return null;
         } else {
            PhysicsStructure s = PhysicsWorld.structureAt(player.level(), hit.getBlockPos());
            return s == null ? null : wrap(s);
         }
      }

      @Override
      public boolean on(Entity entity, ShipCloak.Ship ship) {
         PhysicsStructure s = PhysicsWorld.structureUnder(entity);
         return s != null && s.id().equals(ship.id());
      }

      @Override
      public List<Entity> nearby(ServerLevel level, ShipCloak.Ship ship) {
         return new ArrayList<>(level.getEntities((Entity)null, ship.worldBounds().inflate(1.0, 3.0, 1.0), e -> e instanceof LivingEntity));
      }
   }

   public interface Ship {
      UUID id();

      boolean valid();

      AABB worldBounds();
   }

   public interface World {
      boolean available();

      @Nullable
      ShipCloak.Ship under(Entity var1);

      @Nullable
      ShipCloak.Ship aimed(ServerPlayer var1, double var2);

      boolean on(Entity var1, ShipCloak.Ship var2);

      List<Entity> nearby(ServerLevel var1, ShipCloak.Ship var2);
   }
}
