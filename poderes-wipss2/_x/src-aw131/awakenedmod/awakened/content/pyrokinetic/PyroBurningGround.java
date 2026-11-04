package com.awakenedmod.awakened.content.pyrokinetic;

import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.caster.Caster;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;

public final class PyroBurningGround {
   private static final double RADIUS = 1.8;
   private static final int LIFETIME = 100;
   public static final float BASE_DAMAGE_PER_SECOND = 2.0F;
   private static final int MAX_STACK = 2;
   private static final int IGNITE_SECONDS = 2;
   private static final int DAMAGE_INTERVAL = 20;
   private static final int PHASE_STEP = 10;
   private static final double REACH_UP = 2.0;
   private static final double REACH_DOWN = 0.5;
   private static final int FLAME_INTERVAL = 6;
   private static final int FLAME_LICKS = 1;
   private static final double LICK_LENGTH = 0.7;
   private static final double LICK_WIDTH = 0.45;
   private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
   private static final List<PyroBurningGround.Patch> PATCHES = new ArrayList<>();

   private PyroBurningGround() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(PyroBurningGround::onServerTick);
      gameBus.addListener(PyroBurningGround::onServerStopped);
   }

   public static void plant(Caster caster, ServerLevel level, Vec3 center, FlameTone tone, float damagePerSecond) {
      PATCHES.add(new PyroBurningGround.Patch(caster, level.dimension(), center, tone, damagePerSecond));
   }

   public static void clear(Entity casterBody) {
      UUID id = casterBody.getUUID();
      PATCHES.removeIf(patch -> patch.casterId.equals(id));
   }

   @Nullable
   public static Vec3 burningAt(ServerLevel level, Vec3 pos, double margin) {
      double reach = (1.8 + margin) * (1.8 + margin);

      for (PyroBurningGround.Patch patch : PATCHES) {
         Vec3 c = patch.center;
         if (patch.dimension == level.dimension() && !(pos.y < c.y - 0.5) && !(pos.y > c.y + 2.0)) {
            double dx = pos.x - c.x;
            double dz = pos.z - c.z;
            if (dx * dx + dz * dz <= reach) {
               return c;
            }
         }
      }

      return null;
   }

   public static int patchesNear(ServerLevel level, Vec3 pos, double radius) {
      int count = 0;

      for (PyroBurningGround.Patch patch : PATCHES) {
         if (patch.dimension == level.dimension() && patch.center.distanceToSqr(pos) <= radius * radius) {
            count++;
         }
      }

      return count;
   }

   private static void onServerStopped(ServerStoppedEvent event) {
      PATCHES.clear();
   }

   private static void onServerTick(Post event) {
      if (!PATCHES.isEmpty()) {
         MinecraftServer server = event.getServer();
         boolean drawing = server.getTickCount() % 6 == 0;
         Set<PyroBurningGround.Cell> drawn = (Set<PyroBurningGround.Cell>)(drawing ? new HashSet<>() : Set.of());
         Map<Long, PyroBurningGround.Bill> bills = new LinkedHashMap<>();
         Map<UUID, Boolean> billing = new HashMap<>();

         for (int i = PATCHES.size() - 1; i >= 0; i--) {
            PyroBurningGround.Patch patch = PATCHES.get(i);
            ServerLevel level = server.getLevel(patch.dimension);
            Caster caster = patch.caster;
            if (level != null && !caster.body().isRemoved() && ++patch.age <= 100) {
               if (drawing && drawn.add(patch.cell())) {
                  draw(level, patch);
               }

               if (billing.computeIfAbsent(patch.casterId, id -> onBeat(server, caster.body()))) {
                  collect(level, caster, patch, bills);
               }
            } else {
               PATCHES.remove(i);
            }
         }

         for (PyroBurningGround.Bill bill : bills.values()) {
            bill.apply();
         }
      }
   }

   private static boolean onBeat(MinecraftServer server, Entity caster) {
      return Math.floorMod(server.getTickCount() + casterPhase(server, caster), 20) == 0;
   }

   private static int casterPhase(MinecraftServer server, Entity caster) {
      if (!(caster instanceof ServerPlayer)) {
         return Math.floorMod(caster.getId(), 2) * 10;
      } else {
         UUID id = caster.getUUID();
         List<ServerPlayer> players = server.getPlayerList().getPlayers();

         for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getUUID().equals(id)) {
               return i * 10;
            }
         }

         return 0;
      }
   }

   private static void draw(ServerLevel level, PyroBurningGround.Patch patch) {
      for (int i = 0; i < 1; i++) {
         double angle = level.random.nextDouble() * Math.PI * 2.0;
         double r = Math.sqrt(level.random.nextDouble()) * 1.8;
         Vec3 at = patch.center.add(Math.cos(angle) * r, 0.05, Math.sin(angle) * r);
         PyroFlames.stream(level, at, UP, 0.7, 0.45, patch.tone);
      }
   }

   private static void collect(ServerLevel level, Caster caster, PyroBurningGround.Patch patch, Map<Long, PyroBurningGround.Bill> bills) {
      Vec3 c = patch.center;
      AABB box = new AABB(c.x - 1.8, c.y - 0.5, c.z - 1.8, c.x + 1.8, c.y + 2.0, c.z + 1.8);
      LivingEntity body = caster.body();

      for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != body && inside(e, c) && caster.canHarm(e))) {
         bills.computeIfAbsent(billKey(caster, victim), key -> new PyroBurningGround.Bill(caster, victim, level)).add(patch.damagePerSecond);
      }
   }

   private static long billKey(Caster caster, LivingEntity victim) {
      return (long)caster.body().getId() << 32 | (long)victim.getId() & 4294967295L;
   }

   private static boolean inside(LivingEntity entity, Vec3 center) {
      double dx = entity.getX() - center.x;
      double dz = entity.getZ() - center.z;
      return dx * dx + dz * dz <= 3.24;
   }

   private static final class Bill {
      private final Caster caster;
      private final LivingEntity victim;
      private final ServerLevel level;
      private float damage;
      private int stacks;

      private Bill(Caster caster, LivingEntity victim, ServerLevel level) {
         this.caster = caster;
         this.victim = victim;
         this.level = level;
      }

      private void add(float damagePerSecond) {
         if (this.stacks < 2) {
            this.stacks++;
            this.damage += damagePerSecond;
         }
      }

      private void apply() {
         if (this.victim.isAlive() && this.caster.body().isAlive()) {
            this.victim.igniteForSeconds(2.0F);
            Dmg.hurt(this.victim, this.level, this.caster.damageSource(DamageTypes.ON_FIRE), this.damage);
            this.caster.onHit(this.victim);
         }
      }
   }

   private static record Cell(ResourceKey<Level> dimension, int x, int y, int z) {
   }

   private static final class Patch {
      private final Caster caster;
      private final UUID casterId;
      private final ResourceKey<Level> dimension;
      private final Vec3 center;
      private final FlameTone tone;
      private final float damagePerSecond;
      private int age;

      private Patch(Caster caster, ResourceKey<Level> dimension, Vec3 center, FlameTone tone, float damagePerSecond) {
         this.caster = caster;
         this.casterId = caster.body().getUUID();
         this.dimension = dimension;
         this.center = center;
         this.tone = tone;
         this.damagePerSecond = damagePerSecond;
      }

      private PyroBurningGround.Cell cell() {
         return new PyroBurningGround.Cell(this.dimension, Mth.floor(this.center.x), Mth.floor(this.center.y), Mth.floor(this.center.z));
      }
   }
}
