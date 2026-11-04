package com.awakenedmod.awakened.content.ruin;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

final class RuinBore {
   private static final double[] ACROSS = new double[]{-1.0, -0.5, 0.0, 0.5, 1.0};
   private static final int HALF = 1;
   private static final double STILL = 0.02;
   private final ServerLevel level;
   private final ServerPlayer player;
   private final String soundKey;
   private final LongSet requested = new LongOpenHashSet();
   private final LongSet rim = new LongOpenHashSet();
   private final List<BlockPos> pending = new ArrayList<>();
   private Vec3 head;
   private Vec3 heading;
   private int floor;
   private double sinceStep;
   private double sinceSection;
   private double travelled;
   private final double maxLength;
   private int ticks;
   private int idle;
   private boolean inRock = true;
   private BlockState lastRock = Blocks.AIR.defaultBlockState();

   RuinBore(ServerLevel level, ServerPlayer player, Vec3 start, int floor, Collection<BlockPos> opened, double maxLength) {
      this.maxLength = maxLength;
      this.level = level;
      this.player = player;
      this.soundKey = "ruin_bore_" + player.getUUID();
      this.head = start;
      this.heading = player.getLookAngle();
      this.floor = floor;
      opened.forEach(p -> this.requested.add(p.asLong()));
   }

   boolean tick() {
      Vec3 look = this.player.getLookAngle();
      Vec3 eye = this.player.getEyePosition();
      double lead = this.head.distanceTo(eye);
      double want = lead < 8.0 ? Math.min(lead + 0.3, 8.0) : lead;
      Vec3 toward = eye.add(look.scale(want)).subtract(this.head);
      double gap = toward.length();
      boolean moving = gap > 0.02;
      if (moving) {
         double step = Math.min(gap, 0.3);
         this.heading = toward.scale(1.0 / gap);
         this.head = this.head.add(this.heading.scale(step));
         this.travelled += step;
         this.sinceSection += step;
         this.sinceStep = this.sinceStep + Math.sqrt(this.heading.x * this.heading.x + this.heading.z * this.heading.z) * step;
         this.collect(look);
         this.idle++;
      }

      if (this.sinceSection >= 1.0 || this.ticks == 0) {
         this.sinceSection = 0.0;
         this.flush();
      }

      RuinBreachFx.boreTick(this.level, this.player, this.soundKey, this.head, this.ticks, moving);
      this.ticks++;
      return this.travelled < this.maxLength && this.idle < 20;
   }

   void finish() {
      this.flush();
      this.stop();
   }

   void stop() {
      RuinBreachFx.boreStop(this.level, this.soundKey);
   }

   private void collect(Vec3 look) {
      int y = Mth.floor(this.head.y);
      if (Math.abs(look.y) >= 0.8) {
         int cx = Mth.floor(this.head.x);
         int cz = Mth.floor(this.head.z);

         for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
               this.ask(new BlockPos(cx + dx, y, cz + dz));
            }
         }

         this.floor = y - 1;
         this.sinceStep = 0.0;
      } else {
         int target = Mth.floor(this.head.y - 0.1) - 1;
         if (target != this.floor && this.sinceStep >= 1.0) {
            this.floor = this.floor + Integer.signum(target - this.floor);
            this.sinceStep = 0.0;
         }

         double fx = look.x;
         double fz = look.z;
         double flat = Math.sqrt(fx * fx + fz * fz);
         double rx = flat < 1.0E-4 ? 1.0 : -fz / flat;
         double rz = flat < 1.0E-4 ? 0.0 : fx / flat;

         for (double a : ACROSS) {
            int x = Mth.floor(this.head.x + rx * a * 1.0);
            int z = Mth.floor(this.head.z + rz * a * 1.0);

            for (int h = 0; h < 3; h++) {
               this.ask(new BlockPos(x, this.floor + h, z));
            }
         }
      }
   }

   private void ask(BlockPos pos) {
      if (this.requested.add(pos.asLong())) {
         this.pending.add(pos);
      }
   }

   private void flush() {
      if (!this.pending.isEmpty()) {
         List<BlockPos> cells = new ArrayList<>(this.pending);
         this.pending.clear();
         int crack = 6;
         RuinCarve.Run run = RuinBreachAbility.carve(this.level, this.player, crack, cells, p -> this.requested.contains(p.asLong()), this.rim);
         if (run.planned() > 0) {
            this.idle = 0;
            this.inRock = true;
            this.lastRock = this.firstSolid(cells);
            RuinBreachFx.snapAhead(this.level, cells, crack);
         } else if (this.inRock
            && this.level.getBlockState(BlockPos.containing(this.head)).getCollisionShape(this.level, BlockPos.containing(this.head)).isEmpty()) {
            this.inRock = false;
            RuinBreachFx.breakthrough(this.level, this.head, this.heading, this.lastRock);
         }
      }
   }

   private BlockState firstSolid(List<BlockPos> cells) {
      for (BlockPos pos : cells) {
         BlockState state = this.level.getBlockState(pos);
         if (!state.isAir()) {
            return state;
         }
      }

      return this.lastRock;
   }
}
