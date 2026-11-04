package com.awakenedmod.awakened.content.ruin;

import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.LoopSound;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.terrain.CarveReads;
import com.awakenedmod.awakened.core.terrain.RemovedBlock;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class RuinCollapseControl {
   private final Map<UUID, RuinCollapseControl.Cast> casts = new ConcurrentHashMap<>();

   void begin(ServerPlayer player, ServerLevel level, RuinCrater.Planner planner, CarveReads reads, double radius, int depth, float burstPerStack) {
      Vec3 center = Vec3.atBottomCenterOf(player.blockPosition());
      long now = level.getGameTime();
      this.casts
         .put(player.getUUID(), new RuinCollapseControl.Cast(level, center, radius, depth, planner, reads, now, burstPerStack, RuinGroundFx.rumbleKey(player)));
      double reach = radius * 1.12 + 2.0;
      int ground = player.blockPosition().getY() - 1;
      AABB area = new AABB(center.x - reach, (double)(ground - depth - 2), center.z - reach, center.x + reach, (double)(ground + 32 + 4), center.z + reach);
      RuinFallGuard.hold(player, level, area, now + 11L + 50L + 80L);
   }

   boolean tick(ServerPlayer player) {
      RuinCollapseControl.Cast cast = this.casts.get(player.getUUID());
      if (cast == null) {
         return true;
      } else if (player.serverLevel() != cast.level) {
         this.clear(player);
         return true;
      } else {
         int t = (int)(cast.level.getGameTime() - cast.start);
         int contact = 11;
         if (cast.planner != null) {
            int ticksLeft = contact - 1 - t;
            if (ticksLeft > 0) {
               int left = cast.planner.left();
               cast.planner.scan((left + ticksLeft - 1) / ticksLeft);
            } else if (!this.launch(player, cast, contact - t)) {
               this.clear(player);
               return false;
            }
         }

         int spread = 50;
         if (t < contact) {
            RuinGroundFx.collapseTell(cast.level, player, cast.center, cast.radius, (float)t / (float)contact);
            return true;
         } else {
            int ring = t - contact;
            if (ring == 0) {
               RuinGroundFx.collapseSlam(cast.level, player, cast.center, cast.radius, cast.depth);
            }

            if (ring == spread / 3) {
               RuinGroundFx.collapseSettle(cast.level, cast.center, cast.radius, cast.depth);
            }

            boolean spreading = ring <= spread || cast.carving();
            if (spreading) {
               RuinFallGuard.extend(player, cast.level.getGameTime() + 80L);
            }

            if (ring <= spread) {
               RuinGroundFx.collapseRumble(cast.level, cast.rumbleKey, cast.center, Math.min(1.0F, (float)ring / (float)spread));
            } else if (!spreading && !cast.awarded) {
               cast.awarded = true;
               if (!cast.caughtBodies.isEmpty()) {
                  MasterySystem.award(player, MasteryReason.HIT, cast.caughtBodies);
                  RuinFeats.collapsed(player, cast.caughtBodies);
               }
            }

            land(player, cast);
            if (!spreading && cast.fallers.isEmpty()) {
               this.casts.remove(player.getUUID());
            }

            return true;
         }
      }
   }

   private boolean launch(final ServerPlayer player, RuinCollapseControl.Cast cast, int toContact) {
      RuinCrater crater = cast.planner.finish();
      CarveReads reads = cast.reads;
      cast.planner = null;
      cast.reads = null;
      final ServerLevel level = cast.level;
      final Vec3 center = cast.center;
      final int groundY = crater.groundY();
      final double radius = crater.radius();
      final Map<Long, Integer> floors = crater.restingFloors();
      RuinCarve carve = new RuinCarve().keep(crater.pillar()).hold(300).rise(140).reads(reads);
      int offset = Math.max(0, toContact);
      crater.waves().forEach((tick, blocks) -> carve.wave(offset + tick, blocks));
      RuinCarve.Run run = carve.start(level, player, new RuinCarve.Listener() {
         @Override
         public void wave(List<RemovedBlock> blocks) {
            RuinGroundFx.collapseWave(level, blocks, center, groundY, floors);
            RuinCollapseControl.this.removed(player, blocks);
         }

         @Override
         public void layer(int y, List<RemovedBlock> blocks) {
            RuinGroundFx.collapseRise(level, y, blocks);
         }

         @Override
         public void done() {
            RuinGroundFx.collapseClosed(level, center, radius);
         }
      });
      if (run.planned() == 0) {
         return false;
      } else {
         cast.run = run;
         cast.groundY = groundY;
         cast.topY = crater.topY();
         return true;
      }
   }

   void removed(ServerPlayer player, List<RemovedBlock> blocks) {
      RuinCollapseControl.Cast cast = this.casts.get(player.getUUID());
      if (cast != null && !blocks.isEmpty()) {
         LongSet gone = new LongOpenHashSet(blocks.size());

         for (RemovedBlock block : blocks) {
            gone.add(block.pos().asLong());
         }

         catchStanding(player, cast, gone);
      }
   }

   private static void catchStanding(ServerPlayer player, RuinCollapseControl.Cast cast, LongSet gone) {
      double reach = cast.radius * 1.12 + 1.0;
      AABB area = new AABB(
         cast.center.x - reach,
         (double)(cast.groundY - cast.depth - 2),
         cast.center.z - reach,
         cast.center.x + reach,
         (double)(cast.topY + 3),
         cast.center.z + reach
      );
      List<LivingEntity> found = cast.level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive() && !cast.caught.contains(e.getId()));

      for (LivingEntity body : Harm.hostilesOnly(player, found)) {
         if (stoodOn(body, gone)) {
            cast.caught.add(body.getId());
            if (RuinStacks.add(body, 10, player, body.position())) {
               cast.caughtBodies.add(body);
               cast.fallers.add(new RuinCollapseControl.Faller(body));
            }
         }
      }
   }

   private static boolean stoodOn(LivingEntity body, LongSet gone) {
      int x = body.getBlockX();
      int z = body.getBlockZ();
      int under = Mth.floor(body.getY() - 0.001);

      for (int y = under; y >= under - 1; y--) {
         double above = body.getY() - (double)(y + 1);
         if (above >= -0.5 && above <= 1.0 && gone.contains(BlockPos.asLong(x, y, z))) {
            return true;
         }
      }

      return false;
   }

   private static void land(ServerPlayer player, RuinCollapseControl.Cast cast) {
      int paid = 0;
      Iterator<RuinCollapseControl.Faller> it = cast.fallers.iterator();

      while (it.hasNext()) {
         RuinCollapseControl.Faller f = it.next();
         if (f.body.isAlive() && !f.body.isRemoved() && f.body.level() == cast.level) {
            f.age++;
            if (!f.body.onGround()) {
               f.airborne = true;
            }

            if (f.airborne && f.body.onGround() || f.age >= 80) {
               if (RuinStacks.payout(f.body, cast.burstPerStack) > 0.0F) {
                  paid++;
               }

               RuinGroundFx.collapseLanding(cast.level, f.body);
               it.remove();
            }
         } else {
            it.remove();
         }
      }

      if (paid > 0) {
         RuinGroundFx.collapsePaid(player, paid);
      }
   }

   void clear(ServerPlayer player) {
      RuinCollapseControl.Cast cast = this.casts.remove(player.getUUID());
      if (cast != null) {
         if (cast.level.getGameTime() - cast.start < 11L) {
            if (cast.run != null) {
               cast.run.cancel();
            }

            RuinFallGuard.drop(player);
            RuinGroundFx.collapseCalledOff(player);
         } else if (cast.run != null) {
            cast.run.stop();
         }

         LoopSound.stopAt(cast.level, cast.rumbleKey);
      }
   }

   private static final class Cast {
      final ServerLevel level;
      final Vec3 center;
      final double radius;
      final int depth;
      final long start;
      final float burstPerStack;
      final String rumbleKey;
      @Nullable
      RuinCrater.Planner planner;
      @Nullable
      CarveReads reads;
      @Nullable
      RuinCarve.Run run;
      int groundY;
      int topY;
      final Set<Integer> caught = new HashSet<>();
      final List<LivingEntity> caughtBodies = new ArrayList<>();
      final List<RuinCollapseControl.Faller> fallers = new ArrayList<>();
      boolean awarded;

      Cast(
         ServerLevel level,
         Vec3 center,
         double radius,
         int depth,
         RuinCrater.Planner planner,
         CarveReads reads,
         long start,
         float burstPerStack,
         String rumbleKey
      ) {
         this.level = level;
         this.center = center;
         this.radius = radius;
         this.depth = depth;
         this.planner = planner;
         this.reads = reads;
         this.start = start;
         this.burstPerStack = burstPerStack;
         this.rumbleKey = rumbleKey;
      }

      boolean carving() {
         return this.run != null && this.run.carving();
      }
   }

   private static final class Faller {
      final LivingEntity body;
      int age;
      boolean airborne;

      Faller(LivingEntity body) {
         this.body = body;
      }
   }
}
