package com.devilfruits.fruits.suke_suke.clear;

import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.fruits.suke_suke.SukeFx;
import com.devilfruits.fruits.suke_suke.SukeSounds;
import com.devilfruits.fruits.suke_suke.SukeSuke;
import com.devilfruits.fruits.suke_suke.SukeTriggers;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class Cloak {
   private static final Map<UUID, Cloak.Group> GROUPS = new HashMap<>();
   static Function<ServerPlayer, List<? extends Player>> nearbyPlayers = owner -> owner.serverLevel().players();

   public static void nearbyPlayers(Function<ServerPlayer, List<? extends Player>> source) {
      nearbyPlayers = source;
   }

   public static void resetNearbyPlayers() {
      nearbyPlayers = owner -> owner.serverLevel().players();
   }

   public static double soloSeconds(int mastery) {
      double t = (double)Math.max(0, Math.min(9, mastery - 1)) / 9.0;
      return SukeSuke.setting("soloSecondsMastery1") + (SukeSuke.setting("soloSecondsMastery10") - SukeSuke.setting("soloSecondsMastery1")) * t;
   }

   public static float drainPerTick(int maxEnergy, int mastery, int headcount) {
      return (float)((double)maxEnergy / (soloSeconds(mastery) * 20.0) * (double)Math.max(1, headcount));
   }

   public static boolean active(Player owner) {
      return GROUPS.containsKey(owner.getUUID());
   }

   public static int headcount(Player owner) {
      Cloak.Group g = GROUPS.get(owner.getUUID());
      return g == null ? 0 : 1 + g.members.size();
   }

   public static List<ServerPlayer> members(Player owner) {
      Cloak.Group g = GROUPS.get(owner.getUUID());
      return g == null ? List.of() : List.copyOf(g.members);
   }

   public static boolean inOtherGroup(Player member, Player owner) {
      Clear.State s = Clear.of(member);
      return s.invisible() && !s.has(4) && !s.group().equals(owner.getUUID());
   }

   public static List<ServerPlayer> candidates(ServerPlayer owner) {
      double r = SukeSuke.setting("groupRadius");
      List<ServerPlayer> out = new ArrayList<>();

      for (Player p : nearbyPlayers.apply(owner)) {
         if (p != owner && p instanceof ServerPlayer) {
            ServerPlayer sp = (ServerPlayer)p;
            if (sp.isAlive() && !sp.isSpectator() && sp.level() == owner.level() && sp.distanceToSqr(owner) <= r * r && !inOtherGroup(sp, owner)) {
               out.add(sp);
            }
         }
      }

      return out;
   }

   public static void start(ServerPlayer owner, boolean shared) {
      Cloak.Group g = GROUPS.computeIfAbsent(owner.getUUID(), k -> new Cloak.Group(owner));
      boolean fresh = !Clear.invisible(owner) || Clear.of(owner).has(4);
      Clear.vanish(owner, owner.getUUID(), false);
      if (fresh) {
         ServerFx.animate(owner, SukeSuke.id(shared ? "group_cast" : "vanish"));
         ServerFx.send(owner, SukeFx.VANISH, owner.position(), Vec3.ZERO, 1.0F, 0L, 0);
         owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), SukeSounds.VANISH.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
         SukeTriggers.VANISH.get().trigger(owner);
      }

      g.shared = shared;
      if (shared) {
         if (!fresh) {
            ServerFx.animate(owner, SukeSuke.id("group_cast"));
         }

         float radius = (float)SukeSuke.setting("groupRadius");
         ServerFx.send(owner, SukeFx.GROUP_CAST, owner.position(), Vec3.ZERO, radius, 0L, 0);
         owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), SukeSounds.GROUP_CAST.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

         for (ServerPlayer m : candidates(owner)) {
            if (!g.members.contains(m)) {
               g.members.add(m);
               Clear.vanish(m, owner.getUUID(), false);
               ServerFx.send(m, SukeFx.VANISH, owner.position(), Vec3.ZERO, 0.0F, 0L, 0);
            }
         }

         if (g.members.size() >= 2) {
            SukeTriggers.SHARED.get().trigger(owner);
         }
      } else {
         for (ServerPlayer mx : List.copyOf(g.members)) {
            drop(g, mx);
         }
      }

      loseMobs(owner.serverLevel(), true);
   }

   public static void end(Player owner) {
      Cloak.Group g = GROUPS.remove(owner.getUUID());
      if (g != null) {
         for (ServerPlayer m : List.copyOf(g.members)) {
            drop(g, m);
         }

         if (Clear.invisible(owner) && owner instanceof ServerPlayer sp && !sp.isRemoved() && sp.isAlive()) {
            ServerFx.animate(sp, SukeSuke.id("reappear"));
         }

         show(owner);
      }
   }

   public static void leave(Player member) {
      for (Cloak.Group g : List.copyOf(GROUPS.values())) {
         if (g.members.contains(member)) {
            drop(g, (ServerPlayer)member);
         }
      }
   }

   private static void drop(Cloak.Group g, ServerPlayer m) {
      g.members.remove(m);
      if (Clear.invisible(m) && !Clear.of(m).has(4) && Clear.of(m).group().equals(g.owner.getUUID())) {
         show(m);
      }
   }

   private static void show(Entity e) {
      if (Clear.invisible(e)) {
         Clear.appear(e);
         if (!e.isRemoved()) {
            ServerFx.send(e, SukeFx.APPEAR, e.position(), Vec3.ZERO, 1.0F, 0L, 0);
            e.level().playSound(null, e.getX(), e.getY(), e.getZ(), SukeSounds.APPEAR.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
         }
      }
   }

   public static boolean tick(ServerPlayer owner, PlayerFruitData data, boolean shared) {
      Cloak.Group g = GROUPS.get(owner.getUUID());
      if (g == null || !Clear.invisible(owner)) {
         start(owner, g == null ? shared : g.shared);
         g = GROUPS.get(owner.getUUID());
      }

      double leash = SukeSuke.setting("groupRadius");

      for (ServerPlayer m : List.copyOf(g.members)) {
         if (m.isRemoved()
            || !m.isAlive()
            || m.isSpectator()
            || m.level() != owner.level()
            || m.distanceToSqr(owner) > leash * leash
            || !Clear.of(m).group().equals(owner.getUUID())) {
            drop(g, m);
         }
      }

      if (!AbilityManager.freeEnergy(owner)) {
         float drain = drainPerTick(Mastery.maxEnergy(data), data.mastery(), 1 + g.members.size());
         data.setEnergy(Math.max(0.0F, data.energy() - drain));
         if (data.energy() <= 0.0F) {
            return false;
         }
      }

      if (owner.tickCount % 10 == 0) {
         loseMobs(owner.serverLevel(), false);
      }

      return true;
   }

   public static void loseMobs(ServerLevel level, boolean all) {
      double factor = SukeSuke.setting("mobVisibility");

      for (Cloak.Group g : GROUPS.values()) {
         if (g.owner.level() == level) {
            for (Mob mob : level.getEntitiesOfClass(Mob.class, g.owner.getBoundingBox().inflate(48.0))) {
               LivingEntity target = mob.getTarget();
               if (target != null && Clear.invisible(target)) {
                  double sight = Math.max(2.0, mob.getAttributeValue(Attributes.FOLLOW_RANGE) * factor);
                  boolean provoked = mob.getLastHurtByMob() == target && mob.tickCount - mob.getLastHurtByMobTimestamp() < 60;
                  if (!provoked && (all || mob.distanceToSqr(target) > sight * sight)) {
                     mob.setTarget(null);
                  }
               }
            }
         }
      }
   }

   public static void clear() {
      GROUPS.clear();
   }

   private Cloak() {
   }

   private static final class Group {
      final ServerPlayer owner;
      final List<ServerPlayer> members = new ArrayList<>();
      boolean shared;

      Group(ServerPlayer owner) {
         this.owner = owner;
      }
   }
}
