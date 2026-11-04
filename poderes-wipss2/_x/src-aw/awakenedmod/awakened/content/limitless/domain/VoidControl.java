package com.awakenedmod.awakened.content.limitless.domain;

import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.TeamTimeLock;
import com.awakenedmod.awakened.core.TickFreeze;
import com.awakenedmod.awakened.core.rules.PvpRules;
import com.awakenedmod.awakened.core.voidview.VoidZones;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nullable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;

@EventBusSubscriber(
   modid = "awakened"
)
public final class VoidControl {
   private static final List<VoidControl.Field> FIELDS = new CopyOnWriteArrayList<>();
   private static final String TEAM_HELD = "awakened.team.time_held_void";

   private VoidControl() {
   }

   private static Vec3 centreFor(ServerPlayer caster) {
      return caster.position().add(0.0, 1.2, 0.0);
   }

   public static boolean wouldOverlap(ServerPlayer caster, double radius) {
      return VoidZones.overlaps(false, caster.level().dimension(), centreFor(caster), radius);
   }

   public static void open(ServerPlayer caster, double radius) {
      ServerLevel level = (ServerLevel)caster.level();
      Vec3 centre = centreFor(caster);
      VoidAnchorEntity anchor = VoidAnchorEntity.spawn(level, centre, radius, caster.getYRot(), caster.getId());
      VoidControl.Field field = new VoidControl.Field(anchor, level, caster.getUUID(), centre, radius);
      FIELDS.add(field);
      VoidZones.put(false, anchor.getId(), new VoidZones.Zone(level.dimension(), centre, radius));
      field.gather(caster, level.getGameTime());
      VoidFx.opened(level, centre);
   }

   @Nullable
   private static VoidControl.Field holding(UUID caster) {
      for (VoidControl.Field f : FIELDS) {
         if (f.caster.equals(caster) && !f.cracked) {
            return f;
         }
      }

      return null;
   }

   public static boolean isUp(ServerPlayer caster) {
      return holding(caster.getUUID()) != null;
   }

   public static boolean inIntro(ServerPlayer caster) {
      VoidControl.Field f = holding(caster.getUUID());
      return f != null && f.level.getGameTime() < f.cast + 44L;
   }

   public static void endEarly(UUID caster) {
      VoidControl.Field f = holding(caster);
      if (f != null) {
         f.crack();
      }
   }

   public static boolean isHeld(Entity body) {
      return !FIELDS.isEmpty() && VoidBody.of(body).held();
   }

   public static long sinceIntro(ServerPlayer caster) {
      VoidControl.Field f = holding(caster.getUUID());
      return f == null ? 0L : Math.max(0L, f.level.getGameTime() - f.cast - 44L);
   }

   public static boolean caughtBy(ServerPlayer caster, Entity victim) {
      VoidControl.Field f = holding(caster.getUUID());
      return f != null && (f.mobs.containsKey(victim.getId()) || f.foes.contains(victim.getUUID()));
   }

   @SubscribeEvent
   public static void onServerTick(Post event) {
      if (!FIELDS.isEmpty()) {
         for (VoidControl.Field f : FIELDS) {
            if (f.tick(event.getServer())) {
               FIELDS.remove(f);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLoggedOut(PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         for (VoidControl.Field f : FIELDS) {
            f.loggedOut(player);
         }
      }
   }

   @SubscribeEvent
   public static void onServerStopping(ServerStoppingEvent event) {
      for (VoidControl.Field f : FIELDS) {
         f.tearDown(event.getServer());
      }

      FIELDS.clear();
   }

   private static final class Field {
      final VoidAnchorEntity anchor;
      final ServerLevel level;
      final UUID caster;
      final Vec3 centre;
      final double radius;
      final long cast;
      final Map<Integer, LivingEntity> mobs = new HashMap<>();
      final Map<Integer, Vec3> mobEntry = new HashMap<>();
      final Map<Integer, LivingEntity> allies = new HashMap<>();
      final Map<Integer, Vec3> allyEntry = new HashMap<>();
      final Set<Integer> allyFrozen = new HashSet<>();
      final Map<UUID, VoidControl.Inside> players = new HashMap<>();
      final Set<UUID> foes = new HashSet<>();
      boolean cracked;
      boolean cleared;
      boolean mobsReleased;

      Field(VoidAnchorEntity anchor, ServerLevel level, UUID caster, Vec3 centre, double radius) {
         this.anchor = anchor;
         this.level = level;
         this.caster = caster;
         this.centre = centre;
         this.radius = radius;
         this.cast = level.getGameTime();
      }

      VoidBody body(boolean member, boolean held, boolean isCaster) {
         return new VoidBody(this.anchor.getId(), member, held, isCaster);
      }

      boolean mine(Entity body) {
         int a = VoidBody.of(body).anchor();
         return a < 0 || a == this.anchor.getId();
      }

      long crackTick() {
         return (long)(this.anchor.castTick() + this.anchor.crackAt());
      }

      boolean inside(Entity e) {
         return e.level() == this.level && e.position().add(0.0, (double)e.getBbHeight() * 0.5, 0.0).distanceToSqr(this.centre) < this.radius * this.radius;
      }

      boolean tick(MinecraftServer server) {
         long now = this.level.getGameTime();
         ServerPlayer limitless = server.getPlayerList().getPlayer(this.caster);
         if (!this.cracked) {
            if (limitless != null
               && limitless.isAlive()
               && limitless.level() == this.level
               && this.inside(limitless)
               && now < this.crackTick()
               && !this.anchor.isRemoved()) {
               this.gather(limitless, now);
               this.holdPlayers(limitless, now);
               VoidFx.hum(this.anchor, now - this.cast);
            } else {
               this.crack();
            }
         }

         if (!this.cleared) {
            this.floatMembers(server);
         }

         if (this.cracked && !this.cleared && now >= this.crackTick() + 24L) {
            this.clear(server);
         }

         if (this.cracked && !this.mobsReleased && now >= this.crackTick() + 44L) {
            this.releaseMobs();
         }

         return this.cleared && this.mobsReleased;
      }

      void gather(ServerPlayer limitless, long now) {
         AABB box = new AABB(this.centre, this.centre).inflate(this.radius);
         List<LivingEntity> around = this.level.getEntitiesOfClass(LivingEntity.class, box, ex -> ex.isAlive() && !ex.isSpectator() && this.inside(ex));
         List<LivingEntity> hostile = Harm.hostilesOnly(limitless, around);
         Set<Integer> hostileIds = new HashSet<>();

         for (LivingEntity e : hostile) {
            hostileIds.add(e.getId());
         }

         for (LivingEntity e : around) {
            if (e instanceof ServerPlayer) {
               ServerPlayer p = (ServerPlayer)e;
               if (!this.players.containsKey(p.getUUID())) {
                  this.enter(p, limitless, hostileIds.contains(p.getId()), now);
               }
            } else if (!this.mobs.containsKey(e.getId()) && !this.allies.containsKey(e.getId())) {
               if (hostileIds.contains(e.getId())) {
                  this.mobs.put(e.getId(), e);
                  this.mobEntry.put(e.getId(), e.position());
                  VoidBodies.catchMob(e, this.centre, this.body(false, true, false));
               } else if (!(e instanceof Player)) {
                  this.allies.put(e.getId(), e);
                  this.allyEntry.put(e.getId(), e.position());
                  VoidBodies.enterAlly(e, this.body(true, false, false));
               }
            }
         }
      }

      void enter(ServerPlayer p, ServerPlayer limitless, boolean hostile, long now) {
         VoidControl.Inside in = new VoidControl.Inside(p.position());
         long introEnd = this.cast + 44L;
         boolean fights = !p.isCreative() && !PvpRules.blocksControl(limitless, p);
         if (p == limitless || !fights) {
            in.heldUntil = p == limitless ? introEnd : 0L;
         } else if (hostile) {
            this.foes.add(p.getUUID());
            in.heldUntil = Long.MAX_VALUE;
         } else {
            in.heldUntil = introEnd;
         }

         this.players.put(p.getUUID(), in);
         VoidBodies.enterPlayer(p);
         VoidBody.set(p, this.body(true, false, p == limitless));
      }

      void holdPlayers(ServerPlayer limitless, long now) {
         List<ServerPlayer> heldFoes = new ArrayList<>();

         for (Entry<UUID, VoidControl.Inside> e : this.players.entrySet()) {
            if (this.foes.contains(e.getKey()) && e.getValue().heldUntil > now && this.level.getPlayerByUUID(e.getKey()) instanceof ServerPlayer p) {
               heldFoes.add(p);
            }
         }

         boolean engaged = TeamTimeLock.engaged(limitless, heldFoes);

         for (Entry<UUID, VoidControl.Inside> ex : this.players.entrySet()) {
            Player in = this.level.getPlayerByUUID(ex.getKey());
            if (in instanceof ServerPlayer) {
               ServerPlayer p = (ServerPlayer)in;
               VoidControl.Inside inx = ex.getValue();
               boolean team = engaged && p != limitless && TeamTimeLock.allied(limitless, p);
               if (team && !inx.teamHeld) {
                  TeamTimeLock.tellHeld(limitless, p, "awakened.team.time_held_void");
               }

               inx.teamHeld = team;
               this.setHeld(p, inx, inx.heldUntil > now || team);
            }
         }

         for (LivingEntity ally : this.allies.values()) {
            boolean team = engaged && ally.isAlive() && TeamTimeLock.allied(limitless, ally);
            if (team && this.allyFrozen.add(ally.getId())) {
               TickFreeze.freeze(ally);
            } else if (!team && this.allyFrozen.remove(ally.getId())) {
               TickFreeze.release(ally);
            }
         }
      }

      void setHeld(ServerPlayer p, VoidControl.Inside in, boolean held) {
         if (held) {
            VoidBodies.pin(p, in.entry);
         }

         if (held != in.held) {
            in.held = held;
            VoidBody.set(p, VoidBody.of(p).withHeld(held));
         }
      }

      void floatMembers(MinecraftServer server) {
         Iterator<Entry<UUID, VoidControl.Inside>> it = this.players.entrySet().iterator();

         while (it.hasNext()) {
            Entry<UUID, VoidControl.Inside> e = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p != null && p.level() == this.level && p.isAlive()) {
               if (!this.inside(p)) {
                  if (this.mine(p)) {
                     VoidBodies.releasePlayer(p, e.getValue().entry);
                  }

                  it.remove();
               } else {
                  VoidBodies.floatPlayer(p);
               }
            } else {
               if (p == null || this.mine(p)) {
                  VoidBodies.dropPlayer(e.getKey(), p);
               }

               it.remove();
            }
         }

         it = this.allies.entrySet().iterator();

         while (it.hasNext()) {
            Entry<Integer, LivingEntity> e = it.next();
            LivingEntity ally = e.getValue();
            if (!ally.isAlive() || ally.isRemoved() || !this.inside(ally)) {
               this.letAllyGo(ally);
               it.remove();
            }
         }
      }

      void letAllyGo(LivingEntity ally) {
         if (this.allyFrozen.remove(ally.getId())) {
            TickFreeze.release(ally);
         }

         if (this.mine(ally)) {
            VoidBodies.releaseAlly(ally, this.allyEntry.getOrDefault(ally.getId(), ally.position()));
         }
      }

      void loggedOut(ServerPlayer p) {
         VoidControl.Inside in = this.players.remove(p.getUUID());
         if (in != null && this.mine(p)) {
            VoidBodies.releasePlayer(p, in.entry);
         }
      }

      void crack() {
         if (!this.cracked) {
            this.cracked = true;
            this.anchor.crackNow();

            for (Entry<UUID, VoidControl.Inside> e : this.players.entrySet()) {
               e.getValue().held = false;
               Player var4 = this.level.getPlayerByUUID(e.getKey());
               if (var4 instanceof ServerPlayer) {
                  ServerPlayer p = (ServerPlayer)var4;
                  if (this.mine(p)) {
                     VoidBody.set(p, this.body(true, false, false));
                  }
               }
            }

            for (Integer id : this.allyFrozen) {
               if (this.allies.get(id) != null) {
                  TickFreeze.release((Entity)this.allies.get(id));
               }
            }

            this.allyFrozen.clear();
            VoidFx.closing(this.level, this.centre, this.anchor);
            ServerPlayer var7 = this.level.getServer().getPlayerList().getPlayer(this.caster);
            if (var7 instanceof ServerPlayer) {
               PowerService.sync(var7);
            }
         }
      }

      void clear(MinecraftServer server) {
         this.cleared = true;
         VoidZones.remove(false, this.anchor.getId());
         VoidFx.closed(this.level, this.centre);

         for (Entry<UUID, VoidControl.Inside> e : this.players.entrySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null || this.mine(p)) {
               if (p != null && p.level() == this.level && p.isAlive()) {
                  VoidBodies.releasePlayer(p, e.getValue().entry);
               } else {
                  VoidBodies.dropPlayer(e.getKey(), p);
               }
            }
         }

         this.players.clear();

         for (LivingEntity ally : this.allies.values()) {
            this.letAllyGo(ally);
         }

         this.allies.clear();
      }

      void releaseMobs() {
         this.mobsReleased = true;

         for (LivingEntity mob : this.mobs.values()) {
            if (this.mine(mob)) {
               VoidBodies.releaseMob(mob, this.mobEntry.getOrDefault(mob.getId(), mob.position()));
            } else {
               TickFreeze.release(mob);
            }
         }

         this.mobs.clear();
      }

      void tearDown(MinecraftServer server) {
         if (!this.cracked) {
            this.crack();
         }

         if (!this.cleared) {
            this.clear(server);
         }

         if (!this.mobsReleased) {
            this.releaseMobs();
         }

         this.anchor.discard();
      }
   }

   private static final class Inside {
      final Vec3 entry;
      long heldUntil;
      boolean teamHeld;
      boolean held;

      Inside(Vec3 entry) {
         this.entry = entry;
      }
   }
}
