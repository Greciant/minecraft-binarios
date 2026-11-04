package com.devilfruits.fruits.supa_supa.client.vfx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxAudio;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.VfxSound;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.core.physics.PhysicsStructure;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.physics.PhysicsWorldBridge;
import com.devilfruits.fruits.supa_supa.SupaClientConfig;
import com.devilfruits.fruits.supa_supa.SupaSounds;
import com.devilfruits.fruits.supa_supa.slash.SlashPlane;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SlashSeamCinematic extends Cinematic {
   private static final int MAX_SEGMENTS = 512;
   private static final int FADE = 16;
   private static final int WAIT = 60;
   private static final double PAUSE_RADIUS = 48.0;
   private final ClientLevel level;
   private final SlashPlane plane;
   private final Vec3 start;
   private final double startA;
   private final double speed;
   private final int glideTicks;
   private final long seed;
   private final int index;
   private final RandomSource random;
   private final float[] seg = new float[2560];
   private int segments;
   private boolean valid;
   private SlashSeamCinematic.Phase phase = SlashSeamCinematic.Phase.GROW;
   private int phaseAge;
   private int pauseTicks = 40;
   private int dust = 9075298;
   private double nearA;
   private Vec3 world;
   @Nullable
   private VfxSound cut;

   public SlashSeamCinematic(ClientLevel level, int user, Vec3 start, Vec3 velocity, int glideTicks, long seed, int index, SlashPlane plane) {
      super(new VfxAnchor(user, start), VfxParams.NONE);
      this.level = level;
      this.start = start;
      this.plane = plane;
      this.startA = start.get(plane.along());
      this.speed = velocity.length();
      this.glideTicks = Math.max(1, glideTicks);
      this.seed = seed;
      this.index = index;
      this.random = RandomSource.create(seed ^ (long)index);
      Vec3 w = PhysicsWorld.bridge().renderPosition(level, start, 1.0F);
      this.world = w != null ? w : start;
      this.valid = w != null && this.outline();
   }

   public boolean valid() {
      return this.valid;
   }

   public long seed() {
      return this.seed;
   }

   public int index() {
      return this.index;
   }

   public boolean paused() {
      return this.phase != SlashSeamCinematic.Phase.GROW;
   }

   public void pause(int ticks) {
      if (this.phase == SlashSeamCinematic.Phase.GROW) {
         this.phase = SlashSeamCinematic.Phase.PAUSE;
         this.phaseAge = 0;
         this.pauseTicks = ticks;
         Vec3 at = this.bladeWorld();
         VfxSound.play(SupaSounds.SEAM_EXIT.get(), null, at, false, a -> 1.0F, a -> 1.0F, 40, () -> true);
         float fit = Mth.clamp(29.0F / (float)Math.max(1, ticks), 0.6F, 1.6F);
         VfxSound.tracking(
            SupaSounds.SEAM_TENSION.get(),
            () -> this.world,
            false,
            a -> 0.9F,
            a -> fit,
            ticks + 4,
            () -> this.phase == SlashSeamCinematic.Phase.PAUSE && !this.isDone()
         );
      }
   }

   public void split(boolean cut) {
      if (this.phase != SlashSeamCinematic.Phase.SPLIT && this.phase != SlashSeamCinematic.Phase.FADE) {
         this.phase = cut ? SlashSeamCinematic.Phase.SPLIT : SlashSeamCinematic.Phase.FADE;
         this.phaseAge = 0;
         if (cut) {
            VfxSound.play(SupaSounds.SEAM_SPLIT.get(), null, this.world, false, a -> 1.0F, a -> 1.0F, 80, () -> true);
            double d = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceTo(this.world);
            if (d < 48.0) {
               VfxPost.flash(16054527, 0.45F * (float)(1.0 - d / 48.0));
               CameraDirector.shake(0.35F, 0.1F, 1.4F, 12.0F);
            }

            for (int i = 0; i < VfxSettings.count(60.0F); i++) {
               Vec3[] at = this.seamPoint(true);
               if (at == null) {
                  break;
               }

               Vec3 n = at[1];
               VfxParticles.emit(
                     SupaLook.DUST,
                     at[0].x,
                     at[0].y,
                     at[0].z,
                     n.x * 0.04 + (this.random.nextDouble() - 0.5) * 0.03,
                     n.y * 0.04 + 0.01,
                     n.z * 0.04 + (this.random.nextDouble() - 0.5) * 0.03
                  )
                  .tint(this.dust)
                  .size(0.8F)
                  .alpha(0.6F);
               if (i % 4 == 0) {
                  VfxParticles.emit(SupaLook.GLINT, at[0].x, at[0].y, at[0].z, 0.0, 0.0, 0.0).size(1.2F);
               }
            }
         }
      }
   }

   private BlockPos cell(int a, int s, int layer) {
      int[] c = new int[3];
      c[this.plane.along().ordinal()] = a;
      c[this.plane.span().ordinal()] = s;
      c[this.plane.across().ordinal()] = layer;
      return new BlockPos(c[0], c[1], c[2]);
   }

   private boolean outline() {
      PhysicsStructure s = PhysicsWorld.bridge().structureAt(this.level, BlockPos.containing(this.start));
      AABB b = s == null ? null : s.localBounds();
      if (b == null) {
         return false;
      } else {
         int a0 = Mth.floor(b.min(this.plane.along()));
         int a1 = Mth.floor(b.max(this.plane.along()));
         int y0 = Mth.floor(b.min(this.plane.span()));
         int y1 = Mth.floor(b.max(this.plane.span()));
         int sa = Mth.floor(this.startA);
         a0 = Math.max(a0, sa - 160);
         a1 = Math.min(a1, sa + 160);
         y1 = Math.min(y1, y0 + 96);
         int na = a1 - a0;
         int ny = y1 - y0;
         if (na > 0 && ny > 0) {
            boolean[] occ = new boolean[na * ny];
            int c = this.plane.boundary();
            boolean any = false;

            for (int a = 0; a < na; a++) {
               for (int y = 0; y < ny; y++) {
                  BlockState lo = this.level.getBlockState(this.cell(a0 + a, y0 + y, c - 1));
                  BlockState hi = this.level.getBlockState(this.cell(a0 + a, y0 + y, c));
                  if (!lo.isAir() || !hi.isAir()) {
                     occ[a * ny + y] = true;
                     if (!any) {
                        BlockPos p = this.cell(a0 + a, y0 + y, lo.isAir() ? c : c - 1);
                        this.dust = (lo.isAir() ? hi : lo).getMapColor(this.level, p).col;
                        any = true;
                     }
                  }
               }
            }

            if (!any) {
               return false;
            } else {
               for (int yx = 0; yx < ny; yx++) {
                  for (int face = 0; face < 2; face++) {
                     int ny2 = face == 0 ? yx + 1 : yx - 1;
                     int run = -1;

                     for (int a = 0; a <= na; a++) {
                        boolean edge = a < na && occ[a * ny + yx] && (ny2 < 0 || ny2 >= ny || !occ[a * ny + ny2]);
                        if (edge && run < 0) {
                           run = a;
                        } else if (!edge && run >= 0) {
                           float h = (float)(y0 + yx + (face == 0 ? 1 : 0));
                           this.add((float)(a0 + run), h, (float)(a0 + a), h, face);
                           run = -1;
                        }
                     }
                  }
               }

               for (int ax = 0; ax < na; ax++) {
                  for (int face = 2; face < 4; face++) {
                     int na2 = face == 2 ? ax + 1 : ax - 1;
                     int run = -1;

                     for (int yx = 0; yx <= ny; yx++) {
                        boolean edge = yx < ny && occ[ax * ny + yx] && (na2 < 0 || na2 >= na || !occ[na2 * ny + yx]);
                        if (edge && run < 0) {
                           run = yx;
                        } else if (!edge && run >= 0) {
                           float at = (float)(a0 + ax + (face == 2 ? 1 : 0));
                           this.add(at, (float)(y0 + run), at, (float)(y0 + yx), face);
                           run = -1;
                        }
                     }
                  }
               }

               this.nearA = this.plane.sign() > 0 ? Double.MAX_VALUE : -Double.MAX_VALUE;

               for (int i = 0; i < this.segments; i++) {
                  double ax = (double)Math.min(this.seg[i * 5], this.seg[i * 5 + 2]);
                  double b2 = (double)Math.max(this.seg[i * 5], this.seg[i * 5 + 2]);
                  this.nearA = this.plane.sign() > 0 ? Math.min(this.nearA, ax) : Math.max(this.nearA, b2);
               }

               return this.segments > 0;
            }
         } else {
            return false;
         }
      }
   }

   private double[] lit(float partial) {
      double p = this.bladeA(partial);
      double back = Math.abs(this.startA - this.nearA);
      double near = this.startA - (double)this.plane.sign() * Math.min(back, (double)((float)this.age + partial) * 2.5);
      return new double[]{Math.min(near, p), Math.max(near, p)};
   }

   private void add(float a0, float y0, float a1, float y1, int face) {
      if (this.segments < 512) {
         int i = this.segments++ * 5;
         this.seg[i] = a0;
         this.seg[i + 1] = y0;
         this.seg[i + 2] = a1;
         this.seg[i + 3] = y1;
         this.seg[i + 4] = (float)face;
      }
   }

   private double bladeA(float partial) {
      double t = this.phase == SlashSeamCinematic.Phase.GROW ? (double)Math.min((float)this.glideTicks, (float)this.age + partial) : (double)this.glideTicks;
      return this.startA + (double)this.plane.sign() * this.speed * t;
   }

   private Vec3 local(double a, double s) {
      double[] c = new double[3];
      c[this.plane.along().ordinal()] = a;
      c[this.plane.span().ordinal()] = s;
      c[this.plane.across().ordinal()] = (double)this.plane.boundary();
      return new Vec3(c[0], c[1], c[2]);
   }

   private Vec3 faceNormal(int face) {
      Axis axis = face < 2 ? this.plane.span() : this.plane.along();
      int s = face % 2 == 0 ? 1 : -1;
      return Vec3.atLowerCornerOf(Direction.fromAxisAndDirection(axis, s > 0 ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE).getNormal());
   }

   @Nullable
   private Vec3[] seamPoint(boolean whole) {
      if (this.segments == 0) {
         return null;
      } else {
         PhysicsWorldBridge bridge = PhysicsWorld.bridge();
         double[] range = this.lit(0.0F);
         double lo = whole ? -1.0E9 : range[0];
         double hi = whole ? 1.0E9 : range[1];

         for (int tries = 0; tries < 6; tries++) {
            int i = this.random.nextInt(this.segments) * 5;
            double a0 = Math.max(lo, (double)Math.min(this.seg[i], this.seg[i + 2]));
            double a1 = Math.min(hi, (double)Math.max(this.seg[i], this.seg[i + 2]));
            if (!(a1 < a0 - 1.0E-6)) {
               double t = this.random.nextDouble();
               Vec3 l = this.local(a0 + (a1 - a0) * t, (double)this.seg[i + 1] + (double)(this.seg[i + 3] - this.seg[i + 1]) * t);
               Vec3 w = bridge.renderPosition(this.level, l, 1.0F);
               Vec3 n = bridge.renderDirection(this.level, l, this.faceNormal((int)this.seg[i + 4]), 1.0F);
               if (w != null && n != null) {
                  return new Vec3[]{w, n};
               }
            }
         }

         return null;
      }
   }

   @Override
   public void tick(ClientLevel lvl) {
      super.tick(lvl);
      this.phaseAge++;
      Vec3 w = PhysicsWorld.bridge().renderPosition(this.level, this.start, 1.0F);
      if (w == null) {
         this.finish();
      } else {
         this.world = w;
         double d = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceTo(this.world);
         boolean fancy = VfxSettings.tier() != CoreClientConfig.CinematicQuality.LOW;
         switch (this.phase) {
            case GROW:
               if (this.cut == null) {
                  VfxSound.play(SupaSounds.SEAM_BITE.get(), null, this.bladeWorld(), false, a -> 1.0F, a -> 1.0F, 30, () -> true);
                  this.cut = VfxSound.tracking(
                     SupaSounds.SEAM_CUT.get(),
                     this::bladeWorld,
                     true,
                     a -> Math.min(1.0F, (float)a / 3.0F) * 0.85F,
                     a -> 0.85F + 0.4F * Math.min(1.0F, (float)a / (float)this.glideTicks),
                     this.glideTicks + 6,
                     () -> this.phase == SlashSeamCinematic.Phase.GROW && !this.isDone()
                  );
               }

               if (d < 24.0) {
                  CameraDirector.dolly(0.975F, 0.04F);
               }

               double p = this.bladeA(0.0F);

               for (int i = 0; i < VfxSettings.count(fancy ? 5.0F : 2.0F); i++) {
                  Vec3[] at = this.nearBlade(p);
                  if (at == null) {
                     break;
                  }

                  Vec3 n = at[1];
                  VfxParticles.emit(
                     SupaLook.SPARK,
                     at[0].x,
                     at[0].y,
                     at[0].z,
                     n.x * 0.12 + (this.random.nextDouble() - 0.5) * 0.12,
                     n.y * 0.12 + 0.05 + this.random.nextDouble() * 0.1,
                     n.z * 0.12 + (this.random.nextDouble() - 0.5) * 0.12
                  );
               }

               if (this.age > this.glideTicks + 60) {
                  this.phase = SlashSeamCinematic.Phase.FADE;
                  this.phaseAge = 0;
               }
               break;
            case PAUSE:
               float k = Math.min(1.0F, (float)this.phaseAge / 6.0F) * Math.min(1.0F, Math.max(0.0F, (float)(this.pauseTicks + 4 - this.phaseAge) / 6.0F));
               if ((Boolean)SupaClientConfig.CINEMATIC_PAUSE.get() && d < 48.0) {
                  float near = (float)Mth.clamp((48.0 - d) / 32.0, 0.0, 1.0) * k;
                  CameraDirector.dolly(1.0F - (VfxSettings.reduceFlashing() ? 0.025F : 0.05F) * near, 0.08F);
                  VfxPost.desaturate(0.2F * near);
                  VfxPost.bloomBoost(0.6F * near);
                  VfxAudio.duck(0.6F * near);
               }

               if (fancy) {
                  for (int i = 0; i < VfxSettings.count(3.0F); i++) {
                     Vec3[] at = this.seamPoint(false);
                     if (at != null) {
                        VfxParticles.emit(SupaLook.GLINT, at[0].x, at[0].y, at[0].z, 0.0, 0.0, 0.0).size(0.6F + this.random.nextFloat() * 0.6F);
                     }
                  }

                  if (this.age % 2 == 0) {
                     Vec3[] at = this.seamPoint(false);
                     if (at != null) {
                        Vec3 n = at[1];
                        VfxParticles.emit(SupaLook.EMBER, at[0].x, at[0].y, at[0].z, n.x * 0.02, n.y * 0.02 + 0.004, n.z * 0.02);
                     }
                  }
               }

               if (this.phaseAge > this.pauseTicks + 60) {
                  this.phase = SlashSeamCinematic.Phase.FADE;
                  this.phaseAge = 0;
               }
               break;
            case SPLIT:
            case FADE:
               if (this.phaseAge > 16 + (this.phase == SlashSeamCinematic.Phase.FADE ? 6 : 0)) {
                  this.finish();
               }
         }
      }
   }

   @Nullable
   private Vec3[] nearBlade(double p) {
      PhysicsWorldBridge bridge = PhysicsWorld.bridge();

      for (int tries = 0; tries < 8; tries++) {
         int i = this.random.nextInt(this.segments) * 5;
         double a0 = (double)Math.min(this.seg[i], this.seg[i + 2]);
         double a1 = (double)Math.max(this.seg[i], this.seg[i + 2]);
         Vec3 l;
         if (a1 - a0 > 0.001) {
            if (p < a0 || p > a1) {
               continue;
            }

            l = this.local(p, (double)this.seg[i + 1]);
         } else {
            if (Math.abs(a0 - p) > 0.7) {
               continue;
            }

            l = this.local(a0, (double)this.seg[i + 1] + (double)(this.seg[i + 3] - this.seg[i + 1]) * this.random.nextDouble());
         }

         Vec3 w = bridge.renderPosition(this.level, l, 1.0F);
         Vec3 n = bridge.renderDirection(this.level, l, this.faceNormal((int)this.seg[i + 4]), 1.0F);
         if (w != null && n != null) {
            return new Vec3[]{w, n};
         }
      }

      return null;
   }

   private Vec3 bladeWorld() {
      Vec3 w = PhysicsWorld.bridge()
         .renderPosition(this.level, this.local(this.bladeA(0.0F), this.start.get(this.plane.span()) + (double)(2 * this.plane.spanSign())), 1.0F);
      return w != null ? w : this.world;
   }

   @Override
   public void render(VfxFrame f) {
      PhysicsWorldBridge bridge = PhysicsWorld.bridge();
      float pt = f.partial;
      Vec3 o = bridge.renderPosition(this.level, this.start, pt);
      Vec3 ex = bridge.renderDirection(this.level, this.start, new Vec3(1.0, 0.0, 0.0), pt);
      Vec3 ey = bridge.renderDirection(this.level, this.start, new Vec3(0.0, 1.0, 0.0), pt);
      Vec3 ez = bridge.renderDirection(this.level, this.start, new Vec3(0.0, 0.0, 1.0), pt);
      if (o != null && ex != null && ey != null && ez != null) {
         double p = this.bladeA(pt);
         boolean whole = this.phase == SlashSeamCinematic.Phase.SPLIT;
         double[] range = this.lit(pt);
         double lo = whole ? -1.0E9 : range[0];
         double hi = whole ? 1.0E9 : range[1];
         float t = f.time;
         float alpha;
         float width;
         float heat;
         switch (this.phase) {
            case GROW:
               alpha = 1.0F;
               width = 0.11F;
               heat = 1.1F;
               break;
            case PAUSE:
               float pulse = 0.5F + 0.5F * Mth.sin(((float)this.phaseAge + pt) * 0.45F);
               alpha = 0.85F + 0.15F * pulse;
               width = 0.1F + 0.04F * pulse;
               heat = 0.95F + 0.35F * pulse;
               break;
            case SPLIT: {
               float k = Math.max(0.0F, 1.0F - ((float)this.phaseAge + pt) / 16.0F);
               alpha = k;
               width = 0.07F + 0.2F * k * k;
               heat = 0.7F + 1.0F * k;
               break;
            }
            default: {
               float k = Math.max(0.0F, 1.0F - ((float)this.phaseAge + pt) / 22.0F);
               alpha = 0.8F * k;
               width = 0.06F;
               heat = 0.8F * k + 0.2F;
            }
         }

         if (!(alpha <= 0.01F)) {
            for (int s = 0; s < this.segments; s++) {
               int i = s * 5;
               double sa0 = (double)this.seg[i];
               double sa1 = (double)this.seg[i + 2];
               boolean horizontal = Math.abs(sa1 - sa0) > 0.001;
               double a0;
               double a1;
               if (horizontal) {
                  a0 = Math.max(lo, Math.min(sa0, sa1));
                  a1 = Math.min(hi, Math.max(sa0, sa1));
                  if (a1 <= a0 + 0.02) {
                     continue;
                  }
               } else {
                  if (sa0 < lo - 0.01 || sa0 > hi + 0.01) {
                     continue;
                  }

                  a1 = sa0;
                  a0 = sa0;
               }

               double dist = this.phase == SlashSeamCinematic.Phase.GROW ? Math.min(Math.abs(p - a0), Math.abs(p - a1)) : 3.0;
               float h = heat * (this.phase == SlashSeamCinematic.Phase.GROW ? 0.72F + 0.55F * (float)Math.exp(-dist / 1.5) : 1.0F);
               Vec3 n = this.faceNormal((int)this.seg[i + 4]).scale(0.035);
               Vec3 la = this.local(a0, (double)this.seg[i + 1]).add(n);
               Vec3 lb = this.local(a1, (double)this.seg[i + 3]).add(n);
               Vec3 wa = this.map(o, ex, ey, ez, la);
               Vec3 wb = this.map(o, ex, ey, ez, lb);
               Cuts.bar(f, wa.x, wa.y, wa.z, wb.x, wb.y, wb.z, width * 3.5F, h * 0.75F, alpha * 0.3F, t * 0.2F + (float)s * 0.53F);
               Cuts.bar(f, wa.x, wa.y, wa.z, wb.x, wb.y, wb.z, width, h, alpha, t * 0.3F + (float)s * 0.37F);
            }
         }
      }
   }

   private Vec3 map(Vec3 o, Vec3 ex, Vec3 ey, Vec3 ez, Vec3 local) {
      double dx = local.x - this.start.x;
      double dy = local.y - this.start.y;
      double dz = local.z - this.start.z;
      return new Vec3(o.x + ex.x * dx + ey.x * dy + ez.x * dz, o.y + ex.y * dx + ey.y * dy + ez.y * dz, o.z + ex.z * dx + ey.z * dy + ez.z * dz);
   }

   @Override
   public void removed() {
      if (this.cut != null) {
         this.cut.end();
      }
   }

   @Override
   public double cullDistance() {
      return 160.0;
   }

   @Override
   public double x() {
      return this.world.x;
   }

   @Override
   public double y() {
      return this.world.y;
   }

   @Override
   public double z() {
      return this.world.z;
   }

   private static enum Phase {
      GROW,
      PAUSE,
      SPLIT,
      FADE;
   }
}
