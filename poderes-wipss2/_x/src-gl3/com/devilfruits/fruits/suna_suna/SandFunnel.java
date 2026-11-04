package com.devilfruits.fruits.suna_suna;

import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.core.world.BlockRestoreData;
import com.devilfruits.core.world.CastBudget;
import com.devilfruits.core.world.TerrainDestruction;
import com.devilfruits.fruits.suna_suna.block.QuicksandBlock;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SandFunnel {
   public static final double SWIRL = 0.7;
   private final ServerLevel level;
   private final ServerPlayer caster;
   private final Vec3 centre;
   private final SandFunnel.Shape shape;
   private final long seed;
   private final CastBudget budget;
   private final int holdTicks;
   private final List<SandFunnel.Sunk> sunk = new ArrayList<>();
   private int age;
   private double dried;
   private boolean throatOpen;
   private boolean closed;

   private SandFunnel(ServerLevel level, ServerPlayer caster, Vec3 centre, SandFunnel.Shape shape, CastBudget budget, long seed, int holdTicks) {
      this.level = level;
      this.caster = caster;
      this.centre = centre;
      this.shape = shape;
      this.budget = budget;
      this.seed = seed;
      this.holdTicks = holdTicks;
   }

   public static SandFunnel open(ServerLevel level, ServerPlayer caster, Vec3 centre, SandFunnel.Shape shape, CastBudget budget, long seed, int holdTicks) {
      SandFunnel funnel = new SandFunnel(level, caster, centre, shape, budget, seed, holdTicks);
      funnel.send(null);
      return funnel;
   }

   public void send(@Nullable ServerPlayer only) {
      int grow = Math.max(0, this.shape.growTicks - this.age);
      Vec3 params = new Vec3(this.shape.pull, this.shape.throat, (double)(this.shape.depth + grow * 10));
      if (only != null) {
         ServerFx.sendTo(only, SunaFx.GIRASOLE, this.caster, this.centre, params, (float)this.shape.radius, this.seed, this.holdTicks - this.age);
      } else {
         ServerFx.sendNear(
            this.level,
            SunaFx.GIRASOLE,
            this.caster.getId(),
            this.centre,
            params,
            (float)this.shape.radius,
            this.seed,
            this.holdTicks,
            128.0 + this.shape.radius
         );
      }
   }

   public Vec3 centre() {
      return this.centre;
   }

   public SandFunnel.Shape shape() {
      return this.shape;
   }

   public int age() {
      return this.age;
   }

   public double radius() {
      return currentRadius(this.shape, (float)this.age);
   }

   public static double currentRadius(SandFunnel.Shape shape, float age) {
      float t = Mth.clamp(age / Math.max(1.0F, (float)shape.growTicks), 0.0F, 1.0F);
      return shape.radius * (1.0 - Math.pow(1.0 - (double)t, 3.0));
   }

   public static Vec3 pull(Vec3 centre, Vec3 at, double radius, double throat, double strength) {
      double dx = centre.x - at.x;
      double dz = centre.z - at.z;
      double d = Math.sqrt(dx * dx + dz * dz);
      if (!(d > radius + 0.5) && !(d < 0.001) && !(radius <= 0.0)) {
         double s = strength * (0.4 + 0.6 * Mth.clamp(1.0 - d / radius, 0.0, 1.0));
         if (d < throat) {
            s *= 0.35 + 0.65 * d / throat;
         }

         double ix = dx / d;
         double iz = dz / d;
         double swirl = 0.7 * Mth.clamp((d - throat) / Math.max(0.5, throat), 0.15, 1.0);
         return new Vec3(ix * s - iz * s * swirl, 0.0, iz * s + ix * s * swirl);
      } else {
         return Vec3.ZERO;
      }
   }

   public static boolean caught(Vec3 centre, Vec3 at, double radius, int depth) {
      double dy = at.y - centre.y;
      return dy > (double)(-depth) - 1.5 && dy < 2.5 && Mth.square(at.x - centre.x) + Mth.square(at.z - centre.z) <= Mth.square(radius + 0.5);
   }

   public void tick(float damage, int dehydrateSeconds) {
      if (!this.closed) {
         this.age++;
         double r = this.radius();
         if (r > this.dried + 0.5 || r >= this.shape.radius && this.dried < this.shape.radius) {
            Desiccation.dry(this.level, this.caster, this.centre, r, this.dried, Desiccation.Options.SURFACE, this.budget);
            this.dried = r;
         }

         if (!this.throatOpen && r >= this.shape.throat) {
            this.throatOpen = true;
            this.openThroat();
         }

         AABB box = new AABB(
            this.centre.x - r - 1.0,
            this.centre.y - (double)this.shape.depth - 2.0,
            this.centre.z - r - 1.0,
            this.centre.x + r + 1.0,
            this.centre.y + 3.0,
            this.centre.z + r + 1.0
         );
         boolean pulse = this.age % 20 == 0;
         boolean dry = this.age % 40 == 0 && dehydrateSeconds > 0;

         for (Entity e : this.level.getEntities(this.caster, box, ex -> ex.isAlive() && !ex.isSpectator())) {
            if (caught(this.centre, e.position(), r, this.shape.depth) && !QuicksandBlock.walksOn(e)) {
               if (!(e instanceof Player) && Targeting.canPush(this.caster, e)) {
                  Vec3 v = pull(this.centre, e.position(), r, this.shape.throat, this.shape.pull);
                  e.push(v.x, 0.0, v.z);
               }

               boolean inThroat = Mth.square(e.getX() - this.centre.x) + Mth.square(e.getZ() - this.centre.z) <= Mth.square(this.shape.throat + 0.3)
                  && e.getY() < this.centre.y + 0.2;
               if (inThroat && e instanceof LivingEntity) {
                  LivingEntity living = (LivingEntity)e;
                  if ((pulse || dry) && Targeting.canHurt(this.caster, e)) {
                     if (pulse && damage > 0.0F && e.getEyeY() < this.centre.y) {
                        Targeting.hurt(this.caster, e, damage, SunaSuna.SAND_DAMAGE);
                     }

                     if (dry) {
                        SunaEffects.dehydrate(living, dehydrateSeconds, 1, 2, this.caster);
                     }
                  }
               }
            }
         }
      }
   }

   private void openThroat() {
      if (Desiccation.changesBlocks(this.level)) {
         QuicksandBlock quicksand = (QuicksandBlock)SunaBlocks.QUICKSAND.get();
         int t = Mth.ceil(this.shape.throat);
         int cx = Mth.floor(this.centre.x);
         int cz = Mth.floor(this.centre.z);
         int top = Mth.floor(this.centre.y);

         for (int dx = -t; dx <= t; dx++) {
            for (int dz = -t; dz <= t; dz++) {
               double ox = (double)(cx + dx) + 0.5 - this.centre.x;
               double oz = (double)(cz + dz) + 0.5 - this.centre.z;
               if (!(ox * ox + oz * oz > this.shape.throat * this.shape.throat)) {
                  MutableBlockPos pos = new MutableBlockPos(cx + dx, top + 1, cz + dz);
                  BlockState state = this.level.getBlockState(pos);

                  for (int i = 0; i < 5 && (state.isAir() || state.getCollisionShape(this.level, pos).isEmpty() && !state.is(quicksand)); i++) {
                     pos.move(0, -1, 0);
                     state = this.level.getBlockState(pos);
                  }

                  for (int k = 0; k < this.shape.depth && sinks(state); k++) {
                     BlockState back = state.is(BlockTags.SAND) ? state : Blocks.SAND.defaultBlockState();
                     if (TerrainDestruction.transform(this.level, this.caster, pos, quicksand.of(state), this.budget, this.holdTicks + 200, back, 18)) {
                        this.sunk.add(new SandFunnel.Sunk(pos.immutable(), back));
                     }

                     pos.move(0, -1, 0);
                     state = this.level.getBlockState(pos);
                  }
               }
            }
         }
      }
   }

   private static boolean sinks(BlockState state) {
      return !state.hasBlockEntity() && (state.is(BlockTags.SAND) || state.is(SunaTags.DRIES_TO_SAND));
   }

   public void close() {
      if (!this.closed) {
         this.closed = true;
         if (!this.sunk.isEmpty()) {
            QuicksandBlock quicksand = (QuicksandBlock)SunaBlocks.QUICKSAND.get();
            double t = this.shape.throat + 1.0;
            AABB box = new AABB(
               this.centre.x - t, this.centre.y - (double)this.shape.depth - 2.0, this.centre.z - t, this.centre.x + t, this.centre.y + 1.0, this.centre.z + t
            );

            for (LivingEntity e : this.level.getEntitiesOfClass(LivingEntity.class, box, ex -> ex.isAlive() && !ex.isSpectator())) {
               if (!this.level.getBlockStates(e.getBoundingBox()).noneMatch(sx -> sx.is(quicksand))) {
                  double up = this.surfaceAbove(e);
                  e.teleportTo(e.getX(), up, e.getZ());
                  Targeting.launch(e, new Vec3(0.0, 0.45, 0.0));
               }
            }

            BlockRestoreData restore = BlockRestoreData.get(this.level);

            for (SandFunnel.Sunk s : this.sunk) {
               restore.settle(this.level, s.pos, quicksand, s.back, 3);
            }

            this.sunk.clear();
         }

         ServerFx.sendNear(
            this.level, SunaFx.GIRASOLE_END, this.caster.getId(), this.centre, Vec3.ZERO, (float)this.radius(), this.seed, 0, 128.0 + this.shape.radius
         );
      }
   }

   private double surfaceAbove(Entity e) {
      int x = Mth.floor(e.getX());
      int z = Mth.floor(e.getZ());
      double top = this.centre.y;

      for (SandFunnel.Sunk s : this.sunk) {
         if (s.pos.getX() == x && s.pos.getZ() == z) {
            top = Math.max(top, (double)s.pos.getY() + 1.0);
         }
      }

      return top + 0.05;
   }

   public boolean closed() {
      return this.closed;
   }

   public int throatBlocks() {
      return this.sunk.size();
   }

   public static record Shape(double radius, double throat, int depth, double pull, int growTicks) {
   }

   private static record Sunk(BlockPos pos, BlockState back) {
   }
}
