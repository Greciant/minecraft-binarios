package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class KineticComposites {
   private static final int ENV_STEPS = 4;
   private static final float CUBE_ALPHA = 150.0F;
   private static final float DERIVED_MID = 0.35F;
   private static final float TIP_LIFT = 0.6F;
   private static final float HUE_SPREAD = 12.0F;
   private static final float JITTER_WINDOW_TICKS = 2.0F;
   private static final float GOLDEN = 2.39996F;
   private static final float FRONT_CUBES_PER_BLOCK = 7.0F;
   private static final int FRONT_CUBES_MIN = 10;
   private static final int FRONT_CUBES_MAX = 56;
   private static final float FRONT_HALF_MIN = 0.11F;
   private static final float FRONT_HALF_SPAN = 0.09F;
   private static final float FRONT_HALF_CAP = 0.34F;
   private static final float FRONT_START = 0.12F;
   private static final float FRONT_HOP_MIN = 0.1F;
   private static final float FRONT_HOP_SPAN = 0.22F;
   private static final float FRONT_POP_AT = 0.94F;
   private static final int FRONT_DISCS = 6;
   private static final float FRONT_DISC_FRAC = 0.32F;
   private static final int FLOOR_SCAN_UP = 1;
   private static final int FLOOR_SCAN_DOWN = 5;
   private static final float ARC_SEGS_PER_BLOCK = 5.0F;
   private static final int ARC_SEGS_MIN = 3;
   private static final int ARC_SEGS_MAX = 9;
   private static final float ARC_JITTER = 0.18F;
   private static final float ARC_JITTER_LEN_FRAC = 0.3F;
   private static final float ARC_HALF = 0.05F;
   private static final float ARC_HALF_SPAN = 0.5F;
   private static final float ARC_STEP_HALVES = 1.7F;
   private static final float ARC_FORK_LEN = 0.35F;
   private static final Map<Long, VoxelFlameDraw.Ladders> LADDERS = new HashMap<>();
   private static final Vector3f ORIGIN = new Vector3f();

   private KineticComposites() {
   }

   public static void register() {
      VoxelSpriteFireDrawer.register(VfxEntity.Kind.KINETIC_VOXEL_SPRITE, VfxEntity.Kind.KINETIC_VOXEL_INRUSH, KineticComposites::palette);
      VfxComposites.register(VfxEntity.Kind.KINETIC_CUBE_FRONT, KineticComposites::cubeFront);
      VfxComposites.register(VfxEntity.Kind.KINETIC_PIXEL_ARC, KineticComposites::pixelArc);
   }

   static VoxelFlameDraw.Palette palette(VfxRenderState state) {
      return ladders(state.color, state.color2);
   }

   static VoxelFlameDraw.Ladders ladders(int body, int color2) {
      long key = (long)(body & 16777215) << 32 | (long)color2 & 4294967295L;
      VoxelFlameDraw.Ladders l = LADDERS.get(key);
      if (l == null) {
         int mid = color2 == -1 ? VfxColor.lighten(body, 0.35F) : color2;
         l = VoxelFlameDraw.ladders(body, mid, VfxColor.lighten(mid, 0.6F), 12.0F);
         LADDERS.put(key, l);
      }

      return l;
   }

   private static void cubeFront(PoseStack ps, VfxDraw.Sink sink, VfxRenderState state, float life) {
      float lifeC = Mth.clamp(life, 0.0F, 1.0F);
      float age = state.ageInTicks - (float)state.delay;
      float envRaw = Math.min(1.0F, lifeC * 5.0F) * (float)Math.pow((double)(1.0F - lifeC), 0.8);
      float env = (float)Math.round(envRaw * 4.0F) / 4.0F;
      float discEnv = Math.min(1.0F, lifeC * 4.0F) * (1.0F - lifeC);
      int aCube = VfxDraw.ia(150.0F * env, state.intensity);
      int aDisc = VfxDraw.ia(48.0F * discEnv, state.intensity);
      if (aCube > 1 || aDisc > 1) {
         float radius = Math.abs(state.scale);
         float thick = state.secondary.x <= 0.0 ? 1.0F : (float)state.secondary.x;
         boolean surface = state.secondary.y > 0.0;
         int seed = state.seed;
         int cubes = Mth.clamp(Math.round(radius * 7.0F * thick), 10, 56);
         VoxelFlameDraw.Palette ladders = ladders(state.color, state.color2);
         int[] tiers = ladders.base();
         Quaternionf q = VfxCam.orientation();
         if (surface) {
            q = new Quaternionf().rotationY(state.yaw * (float) (Math.PI / 180.0)).rotateX(state.pitch * (float) (Math.PI / 180.0)).conjugate().mul(q);
         }

         Vector3f right = q.transform(new Vector3f(1.0F, 0.0F, 0.0F));
         Vector3f up = q.transform(new Vector3f(0.0F, 1.0F, 0.0F));
         ClientLevel level = surface ? null : Minecraft.getInstance().level;
         double wx = state.worldPos.x;
         double wy = state.worldPos.y;
         double wz = state.worldPos.z;
         KineticComposites.FloorSeats seats = floorSeats(level, state.seed, cubes);
         float ease = 1.0F - (1.0F - lifeC) * (1.0F - lifeC);
         float[] px = new float[cubes];
         float[] py = new float[cubes];
         float[] pz = new float[cubes];
         float[] half = new float[cubes];

         for (int c = 0; c < cubes; c++) {
            float ang = (float)c * 2.39996F + VoxelFlameDraw.noise01(seed, c, 1) * 0.7F;
            float speed = 0.85F + 0.3F * VoxelFlameDraw.noise01(seed, c, 2);
            float r0 = radius * 0.12F * (0.5F + VoxelFlameDraw.noise01(seed, c, 3));
            float r = r0 + (radius - r0) * Math.min(1.0F, ease * speed);
            float h = Math.min(0.34F, (0.11F + 0.09F * VoxelFlameDraw.noise01(seed, c, 4)) * thick);
            float hop = (0.1F + 0.22F * VoxelFlameDraw.noise01(seed, c, 5)) * thick * Mth.sin(lifeC * (float) Math.PI);
            float x = Mth.cos(ang) * r;
            float z = Mth.sin(ang) * r;
            float floor = level == null ? 0.0F : seats.seat(level, c, wx + (double)x, wy, wz + (double)z) - (float)wy;
            px[c] = x;
            py[c] = floor + h + hop;
            pz[c] = z;
            half[c] = h;
         }

         float ringR = radius * ease;
         VfxDraw.flame(ps, sink, (pose, vc) -> {
            for (int c = 0; c < cubes; c++) {
               float u = Math.min(1.0F, ease * (0.85F + 0.3F * VoxelFlameDraw.noise01(seed, c, 2)));
               if (!(u >= 0.94F) || !(lifeC > 0.5F)) {
                  int tier = Math.min(3, (int)(lifeC * 4.0F));
                  int a = (int)((float)aCube * (1.0F - 0.18F * (float)tier));
                  if (a > 1) {
                     int[] cube = ladders.pick(seed + c);
                     int axis = (int)(VoxelFlameDraw.noise01(seed, c, 6) * 3.0F) % 3;
                     float spin = (0.18F + 0.22F * VoxelFlameDraw.noise01(seed, c, 7)) * (VoxelFlameDraw.noise01(seed, c, 8) > 0.5F ? 1.0F : -1.0F);
                     float angx = VoxelFlameDraw.noise01(seed, c, 9) * 6.2832F + age * spin;
                     VoxelFlameDraw.cube(pose, vc, px[c], py[c], pz[c], half[c] * (1.0F - 0.12F * (float)tier), axis, angx, a, cube[3 - tier]);
                  }
               }
            }

            if (aDisc > 1) {
               float discR = radius * 0.22F * thick * (0.7F + 0.6F * ease);

               for (int m = 0; m < 6; m++) {
                  float am = (float)((Math.PI * 2) * (double)m / 6.0) + (float)(seed & 31) * 0.05F;
                  int rgb = VoxelFlameDraw.gradient(tiers, 1.0F - lifeC);
                  VoxelFlameDraw.disc(pose, vc, Mth.cos(am) * ringR * 0.9F, 0.25F * thick, Mth.sin(am) * ringR * 0.9F, right, up, discR, aDisc, rgb);
               }
            }
         });
      }
   }

   static KineticComposites.FloorSeats floorSeats(ClientLevel level, int entityId, int cubes) {
      return level != null && level.getEntity(entityId) instanceof VfxEntity shell
         ? shell.drawerMemo(KineticComposites.FloorSeats.class, () -> new KineticComposites.FloorSeats(cubes))
         : new KineticComposites.FloorSeats(0);
   }

   static double floorTop(ClientLevel level, double x, double y, double z) {
      return floorTop(level, x, y, z, 1, 5);
   }

   static double floorTop(ClientLevel level, double x, double y, double z, int up, int down) {
      MutableBlockPos pos = BlockPos.containing(x, y + (double)up, z).mutable();

      for (int i = 0; i <= up + down; i++) {
         BlockState st = level.getBlockState(pos);
         if (!st.isAir()) {
            VoxelShape shape = st.getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
               return (double)pos.getY() + shape.max(Axis.Y);
            }
         }

         pos.move(0, -1, 0);
      }

      return y;
   }

   private static void pixelArc(PoseStack ps, VfxDraw.Sink sink, VfxRenderState state, float life) {
      float lifeC = Mth.clamp(life, 0.0F, 1.0F);
      float age = state.ageInTicks - (float)state.delay;
      Vector3f end = new Vector3f((float)state.secondary.x, (float)state.secondary.y, (float)state.secondary.z);
      float len = end.length();
      if (!(len < 0.03F)) {
         float envRaw = 1.0F - lifeC * lifeC;
         float env = (float)Math.round(envRaw * 4.0F) / 4.0F;
         int aCube = VfxDraw.ia(150.0F * env, state.intensity);
         if (aCube > 1) {
            int window = state.seed * 31 + (int)(age / 2.0F);
            int seed = state.seed;
            float s = Math.max(0.05F, state.scale);
            int segs = Mth.clamp(Math.round(len * 5.0F), 3, 9);
            float jitter = Math.min(0.18F, len * 0.3F);
            Vector3f[] joints = VelocityStreaks.boltJoints(ORIGIN, end, segs, jitter, window);
            if (joints != null) {
               VoxelFlameDraw.Palette ladders = ladders(state.color, state.color2);
               int forkJoint = segs > 2 ? 1 + (int)(VelocityStreaks.noise(window, 1, 5) * (float)(segs - 1)) : -1;
               Vector3f[] fork = null;
               if (forkJoint > 0) {
                  double phi = (double)VelocityStreaks.noise(window, 2, 5) * Math.PI * 2.0;
                  Vector3f axis = new Vector3f(end).normalize();
                  Vector3f p1 = VelocityStreaks.perpendicular(axis);
                  Vector3f p2 = new Vector3f(axis).cross(p1).normalize();
                  Vector3f dir = new Vector3f(p1)
                     .mul((float)Math.cos(phi))
                     .add(new Vector3f(p2).mul((float)Math.sin(phi)))
                     .add(new Vector3f(axis).mul(0.5F))
                     .normalize();
                  Vector3f tip = new Vector3f(dir).mul(len * 0.35F).add(joints[forkJoint]);
                  fork = VelocityStreaks.boltJoints(joints[forkJoint], tip, 2, jitter * 0.6F, window * 7919);
               }

               Vector3f[] forkPts = fork;
               VfxDraw.flame(ps, sink, (pose, vc) -> {
                  int lane = chain(pose, vc, joints, s, seed, window, age, aCube, ladders, 0, false);
                  if (forkPts != null) {
                     chain(pose, vc, forkPts, s * 0.7F, seed, window, age, (int)((float)aCube * 0.7F), ladders, lane, true);
                  }
               });
            }
         }
      }
   }

   private static int chain(
      Pose pose,
      VertexConsumer vc,
      Vector3f[] joints,
      float s,
      int seed,
      int window,
      float age,
      int aCube,
      VoxelFlameDraw.Palette ladders,
      int lane,
      boolean cool
   ) {
      Vector3f p = new Vector3f();

      for (int i = 1; i < joints.length; i++) {
         Vector3f a = joints[i - 1];
         Vector3f b = joints[i];
         float segLen = a.distance(b);
         float half = 0.05F * s;
         int n = Math.max(1, Mth.ceil(segLen / (half * 1.7F)));
         int k = 0;

         while (k < n) {
            float t = ((float)k + 0.5F) / (float)n;
            p.set(a).lerp(b, t);
            float h = half * (1.0F + 0.5F * (VoxelFlameDraw.noise01(window, lane, 11) - 0.5F));
            int[] cube = ladders.pick(seed + lane);
            int tier = (VoxelFlameDraw.noise01(window, lane, 12) < 0.35F ? 2 : 3) - (cool ? 1 : 0);
            int axis = (int)(VoxelFlameDraw.noise01(seed, lane, 13) * 3.0F) % 3;
            float ang = VoxelFlameDraw.noise01(window, lane, 14) * 6.2832F + age * 0.3F;
            VoxelFlameDraw.cube(pose, vc, p.x, p.y, p.z, h, axis, ang, aCube, cube[tier]);
            k++;
            lane++;
         }
      }

      return lane;
   }

   static final class FloorSeats {
      private static final long UNSET = Long.MIN_VALUE;
      private final long[] column;
      private final float[] seat;
      private final int up;
      private final int down;

      FloorSeats(int cubes) {
         this(cubes, 1, 5);
      }

      FloorSeats(int cubes, int up, int down) {
         this.column = new long[cubes];
         this.seat = new float[cubes];
         this.up = up;
         this.down = down;
         Arrays.fill(this.column, Long.MIN_VALUE);
      }

      float seat(ClientLevel level, int c, double x, double y, double z) {
         long col = (long)Mth.floor(x) << 32 | (long)Mth.floor(z) & 4294967295L;
         if (c >= this.column.length) {
            return (float)KineticComposites.floorTop(level, x, y, z, this.up, this.down);
         } else {
            if (this.column[c] != col) {
               this.column[c] = col;
               this.seat[c] = (float)KineticComposites.floorTop(level, x, y, z, this.up, this.down);
            }

            return this.seat[c];
         }
      }
   }
}
