package com.devilfruits.fruits.supa_supa.slash;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsConfig;
import com.devilfruits.core.physics.PhysicsStructure;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.physics.StructureBlock;
import com.devilfruits.core.physics.StructureForces;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.fruits.supa_supa.SupaFx;
import com.devilfruits.fruits.supa_supa.SupaSupa;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class SlashStructures {
   private static final long MAX_SCAN = 262144L;
   private static final int PROTECTION_SAMPLES = 24;
   private static final double TIP = 0.7;
   private static final int SEPARATE_TICKS = 12;
   private static final int MAX_KICKS = 4;
   private final SuperSlash slash;
   private final Set<UUID> met = new HashSet<>();
   @Nullable
   private SlashStructures.Glide glide;
   private int glides;
   private boolean exiting;
   private int exitTick;
   private double exitFrom;
   private double exitLeft;

   SlashStructures(SuperSlash slash) {
      this.slash = slash;
   }

   boolean gliding() {
      return this.glide != null;
   }

   boolean exiting() {
      return this.exiting;
   }

   double speed(double full) {
      return this.exiting ? SlashPlane.exitSpeed(this.exitTick, this.exitFrom, full) : full;
   }

   boolean exitStep(double travelled) {
      this.exitLeft -= travelled;
      return this.exitLeft <= 1.0E-6;
   }

   void exitTicked() {
      this.exitTick++;
   }

   boolean enter(Vec3 base) {
      if (!PhysicsWorld.active()) {
         return false;
      } else {
         int h = this.slash.height();
         Vec3 up = this.slash.up();
         ImpactShape blade = ImpactShape.capsule(base.add(up.scale(0.3)), base.add(up.scale((double)h - 0.3)), 0.6);
         List<StructureBlock> touched = PhysicsWorld.blocksIn(this.slash.level(), blade, sx -> !this.met.contains(sx.id()), 1);
         if (touched.isEmpty()) {
            return false;
         } else {
            PhysicsStructure s = touched.get(0).structure();
            this.met.add(s.id());
            AbilityContext ctx = this.slash.context();
            SlashStructures.Survey v = survey(this.slash.owner(), ctx, s, base, this.slash.dir(), up);
            if (v.refusal() != SlashPlane.Refusal.NONE) {
               DevilFruits.LOGGER.debug("Super Slash flies through structure {}: {}", s.id(), v.refusal());
               if (v.refusal() == SlashPlane.Refusal.TOO_BIG) {
                  this.slash
                     .owner()
                     .displayClientMessage(
                        Component.translatable("message.devilfruits.supa_supa.slash_too_big", new Object[]{(int)ctx.extra("maxSplitBlocks")}), true
                     );
               } else if (v.refusal() == SlashPlane.Refusal.PROTECTED) {
                  this.slash.owner().displayClientMessage(Component.translatable("message.devilfruits.supa_supa.slash_protected"), true);
               }

               return false;
            } else {
               double length = v.plane().glideLength(v.start().get(v.plane().along()), v.against()) + 0.5;
               double speed = SlashPlane.glideSpeed(length, ctx.extra("structureGlideSpeed"), (int)ctx.extra("maxGlideTicks"), ctx.extra("cutAnimationSpeed"));
               this.glide = new SlashStructures.Glide(s, v.plane(), v.start(), speed, SlashPlane.glideTicks(length, speed), this.glides++, new int[1]);
               this.exiting = false;
               ServerFx.sendAround(
                  this.slash.level(),
                  base,
                  160.0,
                  SupaFx.SLASH_GLIDE,
                  this.slash.owner().getId(),
                  v.start(),
                  v.plane().travel().scale(speed),
                  (float)this.glide.ticks(),
                  this.slash.seed(),
                  v.plane().pack(this.glide.index())
               );
               return true;
            }
         }
      }
   }

   static SlashStructures.Survey survey(ServerPlayer owner, AbilityContext ctx, PhysicsStructure s, Vec3 worldBase, Vec3 worldDir, Vec3 worldUp) {
      Vec3 local = s.toLocal(worldBase);
      Vec3 centre = s.centerOfMassWorld() != null ? s.centerOfMassWorld() : s.worldBounds().getCenter();
      Vec3 aimed = worldBase.add(worldDir.scale(Math.max(0.0, centre.subtract(worldBase).dot(worldDir))));
      SlashPlane plane = SlashPlane.of(s.dirToLocal(worldDir), s.dirToLocal(worldUp), s.toLocal(aimed));
      Vec3 start = plane.onPlane(local);
      boolean enabled = ctx.flag("splitStructures")
         && PhysicsConfig.enabledFor(SupaSupa.ID)
         && (Boolean)PhysicsConfig.BLOCK_DAMAGE.get()
         && ctx.blockDamageAllowed();
      if (!enabled) {
         return new SlashStructures.Survey(s, plane, start, SlashPlane.Refusal.DISABLED, 0, 0, 0, List.of());
      } else {
         int max = (int)ctx.extra("maxSplitBlocks");
         SlashStructures.Scan scan = scan(ctx.level(), s, plane, max, false);
         if (scan == null) {
            return new SlashStructures.Survey(s, plane, start, SlashPlane.Refusal.TOO_BIG, 0, 0, 0, List.of());
         } else {
            boolean allowed = scan.blocks() > max || !scan.solidMinus() || !scan.solidPlus() || allowed(ctx.level(), owner, s, scan.against());
            SlashPlane.Refusal refusal = SlashPlane.check(true, allowed, scan.blocks(), max, scan.solidMinus(), scan.solidPlus());
            return new SlashStructures.Survey(s, plane, start, refusal, scan.blocks(), scan.minus(), scan.plus(), scan.against());
         }
      }
   }

   private static boolean allowed(ServerLevel level, ServerPlayer owner, PhysicsStructure s, List<BlockPos> against) {
      if (!BlockImpactService.allowed(owner, s)) {
         return false;
      } else {
         int step = Math.max(1, against.size() / 24);

         for (int i = 0; i < against.size(); i += step) {
            if (!BlockImpactService.mayAlter(level, owner, s, against.get(i))) {
               return false;
            }
         }

         return true;
      }
   }

   @Nullable
   private static SlashStructures.Scan scan(ServerLevel level, PhysicsStructure s, SlashPlane plane, int max, boolean collect) {
      AABB b = s.localBounds();
      if (b == null) {
         return new SlashStructures.Scan(0, 0, 0, false, false, List.of(), collect ? List.of() : null);
      } else {
         int x0 = Mth.floor(b.minX);
         int y0 = Mth.floor(b.minY);
         int z0 = Mth.floor(b.minZ);
         int x1 = Mth.floor(b.maxX);
         int y1 = Mth.floor(b.maxY);
         int z1 = Mth.floor(b.maxZ);
         if ((long)(x1 - x0) * (long)(y1 - y0) * (long)(z1 - z0) > 262144L) {
            return null;
         } else {
            int blocks = 0;
            int minus = 0;
            int plus = 0;
            boolean solidMinus = false;
            boolean solidPlus = false;
            List<BlockPos> against = new ArrayList<>();
            List<BlockPos> all = collect ? new ArrayList<>() : null;
            MutableBlockPos m = new MutableBlockPos();

            for (int x = x0; x < x1; x++) {
               for (int y = y0; y < y1; y++) {
                  for (int z = z0; z < z1; z++) {
                     BlockState state = level.getBlockState(m.set(x, y, z));
                     if (!state.isAir()) {
                        if (++blocks > max) {
                           return new SlashStructures.Scan(blocks, minus, plus, solidMinus, solidPlus, against, all);
                        }

                        boolean solid = !state.getCollisionShape(level, m).isEmpty();
                        if (plane.side(m) < 0) {
                           minus++;
                           solidMinus |= solid;
                        } else {
                           plus++;
                           solidPlus |= solid;
                        }

                        if (plane.against(m) && against.size() < 4096) {
                           against.add(m.immutable());
                        }

                        if (all != null) {
                           all.add(m.immutable());
                        }
                     }
                  }
               }
            }

            return new SlashStructures.Scan(blocks, minus, plus, solidMinus, solidPlus, against, all);
         }
      }
   }

   void glideTick() {
      SlashStructures.Glide g = this.glide;
      PhysicsStructure s = g.s();
      if (!s.isValid()) {
         this.exit(g, this.slash.base(), this.slash.dir());
      } else {
         int age = ++g.age()[0];
         Vec3 local = g.start().add(g.plane().travel().scale(g.speed() * (double)Math.min(age, g.ticks())));
         Vec3 bottom = s.toWorld(local);
         Vec3 top = s.toWorld(local.add(g.plane().spanDir().scale((double)this.slash.height())));
         Vec3 travel = s.dirToWorld(g.plane().travel());
         this.slash.hitBlade(bottom, top, travel);
         this.slash.place(bottom);
         if (age >= g.ticks()) {
            Vec3 flat = new Vec3(travel.x, 0.0, travel.z);
            this.exit(g, bottom, SuperSlash.aim(travel, flat.lengthSqr() < 1.0E-6 ? this.slash.dir() : flat.normalize()));
         }
      }
   }

   private void exit(SlashStructures.Glide g, Vec3 at, Vec3 dir) {
      this.glide = null;
      AbilityContext ctx = this.slash.context();
      this.slash.redirect(at, dir);
      this.exiting = true;
      this.exitTick = 0;
      this.exitFrom = g.speed();
      this.exitLeft = ctx.extra("exitDistance");
      int delay = SlashPlane.pauseTicks((int)ctx.extra("splitDelayTicks"), ctx.extra("cutAnimationSpeed"));
      ServerFx.sendNear(
         this.slash.level(),
         SupaFx.SLASH_EXIT,
         this.slash.owner().getId(),
         at,
         dir.scale(this.slash.speed()),
         (float)this.exitLeft,
         this.slash.seed(),
         delay,
         160.0
      );
      if (g.s().isValid()) {
         SlashStructures.Split split = new SlashStructures.Split(
            this.slash.level(), this.slash.owner(), ctx, g.s(), g.plane(), g.start(), this.slash.seed(), g.index()
         );
         ServerScheduler.later(this.slash.level(), delay, split::run);
      }
   }

   void flown() {
      this.glide = null;
   }

   private static record Glide(PhysicsStructure s, SlashPlane plane, Vec3 start, double speed, int ticks, int index, int[] age) {
   }

   private static record Scan(int blocks, int minus, int plus, boolean solidMinus, boolean solidPlus, List<BlockPos> against, @Nullable List<BlockPos> all) {
   }

   private static record Split(ServerLevel level, ServerPlayer owner, AbilityContext ctx, PhysicsStructure s, SlashPlane plane, Vec3 seam, long seed, int index) {
      void run() {
         if (this.s.isValid()) {
            Vec3 at = this.s.toWorld(this.seam);
            int max = (int)this.ctx.extra("maxSplitBlocks");
            SlashStructures.Scan scan = SlashStructures.scan(this.level, this.s, this.plane, max, true);
            boolean ok = scan != null
               && scan.all() != null
               && !BlockImpactService.holdsUnbreakable(this.s)
               && SlashPlane.check(true, true, scan.blocks(), max, scan.solidMinus(), scan.solidPlus()) == SlashPlane.Refusal.NONE;
            PhysicsStructure piece = null;
            SlashPlane.Partition parts = null;
            if (ok) {
               parts = this.plane.partition(scan.all());
               piece = PhysicsWorld.bridge().split(this.level, this.s, parts.smaller());
            }

            ServerFx.sendAround(
               this.level, at, 160.0, SupaFx.SEAM_SPLIT, this.owner.getId(), this.seam, this.plane.normal(), piece != null ? 1.0F : 0.0F, this.seed, this.index
            );
            if (piece != null) {
               DevilFruits.LOGGER
                  .info(
                     "Super Slash cut structure {} ({} blocks, mass {}) along {}={}: {} blocks moved into {} (mass {})",
                     new Object[]{
                        this.s.id(),
                        scan.blocks(),
                        String.format("%.1f", this.s.mass()),
                        this.plane.across(),
                        this.plane.boundary(),
                        parts.smaller().size(),
                        piece.id(),
                        String.format("%.1f", piece.mass())
                     }
                  );
               PhysicsStructure moved = piece;
               int side = parts.smallerSide();
               int[] kicks = new int[]{0};
               ServerScheduler.schedule(this.level, 1, age -> this.separate(moved, side, age, kicks));
            }
         }
      }

      private boolean separate(PhysicsStructure piece, int side, int age, int[] kicks) {
         double speed = this.ctx.extra("separationSpeed");
         double ma = this.s.mass();
         double mb = piece.mass();
         if (age < 12 && kicks[0] < 4 && !(speed <= 0.0) && !(ma <= 0.0) && !(mb <= 0.0) && this.s.isValid() && piece.isValid()) {
            Vec3 n = this.s.dirToWorld(this.plane.normal()).scale((double)side).normalize();
            double now = piece.velocityAt(centre(piece)).subtract(this.s.velocityAt(centre(this.s))).dot(n);
            double missing = speed - now;
            if (missing < speed * 0.4) {
               return false;
            } else {
               Vec3 dvPiece = n.scale(missing * ma / (ma + mb));
               Vec3 dvRest = n.scale(-missing * mb / (ma + mb));
               double share = Math.min(StructureForces.allowance(piece, dvPiece.length()), StructureForces.allowance(this.s, dvRest.length()));
               if (share <= 0.0) {
                  return false;
               } else {
                  Vec3 up = this.s.dirToWorld(new Vec3(0.0, 1.0, 0.0)).normalize();
                  double tip = 0.7 * share * missing / speed;
                  kicks[0]++;
                  StructureForces.kick(piece, dvPiece.scale(share), up.cross(n).scale(tip));
                  StructureForces.kick(this.s, dvRest.scale(share), up.cross(n.scale(-1.0)).scale(tip));
                  DevilFruits.LOGGER
                     .debug("Super Slash halves: {} m/s apart, {} m/s added", String.format("%.1f", now), String.format("%.1f", missing * share));
                  return false;
               }
            }
         } else {
            return true;
         }
      }

      private static Vec3 centre(PhysicsStructure s) {
         Vec3 c = s.centerOfMassWorld();
         return c != null ? c : s.worldBounds().getCenter();
      }
   }

   static record Survey(PhysicsStructure s, SlashPlane plane, Vec3 start, SlashPlane.Refusal refusal, int blocks, int minus, int plus, List<BlockPos> against) {
   }
}
