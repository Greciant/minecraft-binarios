package com.awakenedmod.awakened.content.ruin;

import com.awakenedmod.awakened.content.AwakenedSounds;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Lab;
import com.awakenedmod.awakened.core.LoopSound;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class RuinSpreadFx {
   private static final Lab.Value CELLS = Lab.SERVER.def("ruin.spread.cells", 110.0, 20.0, 300.0, 1.0);
   private static final Lab.Value CRACK_OPEN = Lab.SERVER.def("ruin.spread.crack_open", 8.0, 2.0, 20.0, 1.0);
   private static final Lab.Value DUST = Lab.SERVER.def("ruin.spread.dust", 1.0, 0.0, 3.0, 0.1);
   private static final Lab.Value ROAR_VOL = Lab.SERVER.def("ruin.spread.roar_vol", 1.1, 0.0, 3.0, 0.05);
   private static final Lab.Value ROAR_PITCH = Lab.SERVER.def("ruin.spread.roar_pitch", 1.2, 0.5, 2.0, 0.05);
   private static final double LATTICE = 0.5;
   private static final int SCAN_UP = 2;
   private static final int SCAN_DOWN = 3;
   private static final int CLOSE_TICKS = 10;
   private static final int DRIFT_EVERY = 3;
   private static final int DRIFT_CELLS = 3;
   private static final int ROAR_SWELL = 4;
   private static final int ROAR_TAIL = 10;
   private static final int CRUNCH_BUCKET = 5;
   private static final int CRUNCH_WINDOW = 3;
   private static final int SHAKE_EVERY = 6;
   private static final double PALM_CRACK_RADIUS = 1.4;
   private static final int PALM_CRACK_BLOCKS = 9;
   private static int nextCast;

   private RuinSpreadFx() {
   }

   public static void preloadLabKeys() {
   }

   static void slam(ServerLevel level, ServerPlayer caster, RuinStrip strip) {
      RuinFx.slamHand(caster);
      int open = CRACK_OPEN.getI();
      int life = RuinSpreadFront.lifetime(strip);
      palm(level, strip.apex(), Math.max(0, life - 2));
      List<RuinSpreadFx.Cell> cells = thin(cells(level, strip), strip.length(), CELLS.getI());
      int travel = strip.travel();
      List<RuinSpreadFx.Cell>[] byTick = new List[travel + 1];

      for (RuinSpreadFx.Cell cell : cells) {
         RuinCracks.sweep(level, cell.pos, 9, cell.arrive, open, Math.max(0, life - cell.arrive - open), 10);
         if (cell.plant != null) {
            RuinCracks.snap(level, cell.plant, 9, cell.arrive + open / 2, Math.max(0, life - cell.arrive - open / 2 - 1));
         }

         int t = Mth.clamp(cell.arrive, 0, travel);
         if (byTick[t] == null) {
            byTick[t] = new ArrayList<>();
         }

         byTick[t].add(cell);
      }

      String roar = "ruin_spread_" + nextCast++;
      float dust = DUST.getF();
      float volume = ROAR_VOL.getF();
      float pitch = ROAR_PITCH.getF();
      int end = travel + open + 10;
      List<RuinSpreadFx.Cell> crumbled = new ArrayList<>(cells.size());
      RuinFxClock.every(level, Math.max(end, life) + 1, t -> {
         int age = t - 1;
         if (age <= travel && byTick[age] != null) {
            wall(level, byTick[age], dust);
            crunch(level, byTick[age], age, roar.hashCode());
         }

         int crumbleAt = age - open;
         if (crumbleAt >= 0 && crumbleAt <= travel && byTick[crumbleAt] != null) {
            for (RuinSpreadFx.Cell cell : byTick[crumbleAt]) {
               crumble(level, cell);
               crumbled.add(cell);
            }
         }

         if (age < life && age % 3 == 0) {
            drift(level, crumbled);
         }

         if (age <= end) {
            roar(level, strip, roar, age, volume, pitch, end);
         }

         if (age > 0 && age <= travel && age % 6 == 0) {
            Fx.shake(level, strip.at(strip.frontAt(age), 0.0).add(0.0, 0.5, 0.0), 10.0, 0.1F, 0, 8);
         }
      });
   }

   static void touch(ServerLevel level, LivingEntity body) {
      Vec3 feet = body.position();
      level.sendParticles(ParticleTypes.WHITE_ASH, feet.x, feet.y + 0.2, feet.z, 8, 0.25, 0.1, 0.25, 0.02);
      level.sendParticles(
         ParticleTypes.ASH,
         feet.x,
         feet.y + (double)body.getBbHeight() * 0.4,
         feet.z,
         8,
         (double)body.getBbWidth() * 0.4,
         (double)body.getBbHeight() * 0.3,
         (double)body.getBbWidth() * 0.4,
         0.01
      );
      Fx.sound(level, feet, SoundEvents.TUFF_BREAK, SoundSource.PLAYERS, 0.6F, 0.55F);
   }

   static void fester(ServerLevel level, LivingEntity body) {
      Vec3 feet = body.position();
      level.sendParticles(ParticleTypes.WHITE_ASH, feet.x, feet.y + 0.15, feet.z, 4, 0.2, 0.08, 0.2, 0.015);
      level.sendParticles(ParticleTypes.ASH, feet.x, feet.y + 0.3, feet.z, 4, (double)body.getBbWidth() * 0.35, 0.2, (double)body.getBbWidth() * 0.35, 0.01);
      Fx.sound(level, feet, SoundEvents.TUFF_BREAK, SoundSource.PLAYERS, 0.3F, 0.7F);
   }

   private static void drift(ServerLevel level, List<RuinSpreadFx.Cell> crumbled) {
      if (!crumbled.isEmpty()) {
         RandomSource r = level.random;

         for (int i = 0; i < 3; i++) {
            RuinSpreadFx.Cell c = crumbled.get(r.nextInt(crumbled.size()));
            double x = (double)c.pos.getX() + r.nextDouble();
            double z = (double)c.pos.getZ() + r.nextDouble();
            level.sendParticles(ParticleTypes.ASH, x, c.top + 0.15, z, 2, 0.2, 0.1, 0.2, 0.005);
            if (r.nextInt(3) == 0) {
               level.sendParticles(ParticleTypes.WHITE_ASH, x, c.top + 0.1, z, 1, 0.1, 0.05, 0.1, 0.005);
            }
         }
      }
   }

   private static void palm(ServerLevel level, Vec3 apex, int hold) {
      RuinCracks.stamp(level, apex, 1.4, 9, hold, 9);
      RuinFx.handAsh(level, apex.add(0.0, 0.4, 0.0), 2.0F);
      Fx.Floor floor = Fx.floorUnder(level, apex.x, apex.y + 0.5, apex.z, 3);
      if (floor != null) {
         Vec3 at = new Vec3(apex.x, floor.topY(), apex.z);
         level.sendParticles(new BlockParticleOption(ParticleTypes.DUST_PILLAR, floor.state()), at.x, at.y + 0.1, at.z, 16, 0.5, 0.05, 0.5, 0.0);
         Fx.debrisFan(level, new BlockParticleOption(ParticleTypes.BLOCK, floor.state()), at.add(0.0, 0.1, 0.0), 14, 25.0, 65.0, 0.15, 0.35);
         Fx.sound(level, at, floor.state().getSoundType().getBreakSound(), SoundSource.PLAYERS, 1.0F, 0.55F);
      }

      Fx.sound(level, apex, SoundEvents.DEEPSLATE_BREAK, SoundSource.PLAYERS, 1.0F, 0.5F);
      Fx.sound(level, apex, SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 0.8F, 0.45F);
      Fx.shake(level, apex, 14.0, 0.2F, 2, 10);
   }

   private static void wall(ServerLevel level, List<RuinSpreadFx.Cell> row, float dust) {
      if (!(dust <= 0.0F)) {
         int n = Math.max(1, Math.round(dust * 2.0F));
         int i = 0;

         while (i < row.size()) {
            RuinSpreadFx.Cell c = row.get(i);
            double x = (double)c.pos.getX() + 0.5;
            double z = (double)c.pos.getZ() + 0.5;
            double y = c.top;
            level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, c.state), x, y + 0.4, z, n, 0.35, 0.2, 0.35, 0.0);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, c.state), x, y + 0.1, z, n, 0.3, 0.05, 0.3, 0.05);
            level.sendParticles(ParticleTypes.ASH, x, y + 0.6, z, n, 0.35, 0.4, 0.35, 0.02);
            level.sendParticles(ParticleTypes.WHITE_ASH, x, y + 0.3, z, 1, 0.3, 0.2, 0.3, 0.01);
            i += row.size() > 3 ? 2 : 1;
         }
      }
   }

   private static void crunch(ServerLevel level, List<RuinSpreadFx.Cell> row, int age, int salt) {
      if (!row.isEmpty() && RuinFx.beat(age, salt, 5, 3)) {
         RuinSpreadFx.Cell c = row.get(level.random.nextInt(row.size()));
         Vec3 at = Vec3.atCenterOf(c.pos);
         Fx.sound(level, at, c.state.getSoundType().getBreakSound(), SoundSource.PLAYERS, 1.0F, 0.5F + 0.1F * level.random.nextFloat());
         Fx.sound(level, at, age / 5 % 2 == 0 ? SoundEvents.DEEPSLATE_BREAK : SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 0.8F, 0.45F);
      }
   }

   private static void crumble(ServerLevel level, RuinSpreadFx.Cell c) {
      double x = (double)c.pos.getX() + 0.5;
      double z = (double)c.pos.getZ() + 0.5;
      level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, c.state), x, c.top + 0.05, z, 6, 0.3, 0.05, 0.3, 0.05);
      level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, c.state), x, c.top + 0.3, z, 2, 0.3, 0.1, 0.3, 0.0);
      if (c.plantState != null) {
         level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, c.plantState), x, c.top + 0.4, z, 5, 0.25, 0.2, 0.25, 0.03);
      }
   }

   private static void roar(ServerLevel level, RuinStrip strip, String key, int age, float volume, float pitch, int end) {
      int travel = strip.travel();
      if (age >= end) {
         LoopSound.stopAt(level, key);
      } else {
         float env;
         if (age <= travel) {
            env = Math.min(1.0F, (float)(age + 1) / 4.0F);
         } else {
            env = Math.max(0.0F, 1.0F - (float)(age - travel) / (float)(end - travel));
         }

         Vec3 at = strip.at(strip.frontAt(Math.min(age, travel)), 0.0).add(0.0, 0.6, 0.0);
         LoopSound.keepAt(level, key, at, (SoundEvent)AwakenedSounds.RUIN_RUMBLE.get(), SoundSource.PLAYERS, volume * env, pitch);
      }
   }

   private static List<RuinSpreadFx.Cell> cells(ServerLevel level, RuinStrip strip) {
      Set<Long> seen = new HashSet<>();
      List<RuinSpreadFx.Cell> cells = new ArrayList<>();
      double refY = floorRef(level, strip.apex());
      MutableBlockPos scan = new MutableBlockPos();

      for (double s = 0.25; s <= strip.length(); s += 0.5) {
         double half = strip.halfAt(s);

         for (double u = -half; u <= half + 1.0E-6; u += 0.5) {
            Vec3 p = strip.at(s, u);
            int bx = Mth.floor(p.x);
            int bz = Mth.floor(p.z);
            if (seen.add((long)bx << 32 ^ (long)bz & 4294967295L)) {
               RuinSpreadFx.Cell cell = column(level, scan, bx, bz, refY, s, strip.arrival(s));
               if (cell != null) {
                  cells.add(cell);
               }
            }
         }
      }

      return cells;
   }

   private static double floorRef(ServerLevel level, Vec3 apex) {
      MutableBlockPos scan = new MutableBlockPos();
      int top = Mth.floor(apex.y) + 1;

      for (int y = top; y >= top - 3 - 2; y--) {
         scan.set(Mth.floor(apex.x), y, Mth.floor(apex.z));
         BlockState st = level.getBlockState(scan);
         if (!st.isAir() && st.getFluidState().isEmpty() && !st.getCollisionShape(level, scan).isEmpty()) {
            return (double)y + 1.0;
         }
      }

      return apex.y;
   }

   @Nullable
   private static RuinSpreadFx.Cell column(ServerLevel level, MutableBlockPos scan, int bx, int bz, double refY, double s, int arrive) {
      int from = Mth.floor(refY) + 2;

      for (int y = from; y >= Mth.floor(refY) - 3; y--) {
         scan.set(bx, y, bz);
         BlockState st = level.getBlockState(scan);
         if (!st.isAir() && st.getFluidState().isEmpty() && !st.getCollisionShape(level, scan).isEmpty()) {
            BlockPos pos = scan.immutable();
            BlockPos above = pos.above();
            BlockState plant = level.getBlockState(above);
            if (!plant.getCollisionShape(level, above).isEmpty()) {
               return null;
            }

            boolean isPlant = !plant.isAir() && plant.getFluidState().isEmpty() && plant.getCollisionShape(level, above).isEmpty();
            return new RuinSpreadFx.Cell(pos, st, (double)y + 1.0, s, arrive, isPlant ? above : null, isPlant ? plant : null);
         }
      }

      return null;
   }

   private static List<RuinSpreadFx.Cell> thin(List<RuinSpreadFx.Cell> cells, double length, int max) {
      if (cells.size() <= max) {
         return cells;
      } else {
         List<RuinSpreadFx.Cell> kept = new ArrayList<>();

         for (RuinSpreadFx.Cell c : cells) {
            if (c.s <= length * 0.5 || (c.pos.getX() + c.pos.getZ() & 1) == 0) {
               kept.add(c);
            }
         }

         if (kept.size() <= max) {
            return kept;
         } else {
            List<RuinSpreadFx.Cell> strided = new ArrayList<>(max);
            double stride = (double)kept.size() / (double)max;

            for (double i = 0.0; i < (double)kept.size() && strided.size() < max; i += stride) {
               strided.add(kept.get((int)i));
            }

            return strided;
         }
      }
   }

   private static record Cell(BlockPos pos, BlockState state, double top, double s, int arrive, @Nullable BlockPos plant, @Nullable BlockState plantState) {
   }
}
