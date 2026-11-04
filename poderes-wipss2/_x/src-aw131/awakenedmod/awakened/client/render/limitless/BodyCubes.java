package com.awakenedmod.awakened.client.render.limitless;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.model.AgeableHierarchicalModel;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelPart.Cube;
import net.minecraft.client.model.geom.ModelPart.Visitor;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class BodyCubes {
   public static final int[][] EDGES = new int[][]{{0, 1}, {2, 3}, {4, 5}, {6, 7}, {0, 2}, {1, 3}, {4, 6}, {5, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
   public static final int[][] FACES = new int[][]{{0, 2, 6, 4}, {1, 3, 7, 5}, {0, 1, 5, 4}, {2, 3, 7, 6}, {0, 1, 3, 2}, {4, 5, 7, 6}};
   @Nullable
   private static final Method RENDERER_SCALE = method(LivingEntityRenderer.class, "scale", LivingEntity.class, PoseStack.class, float.class);
   @Nullable
   private static final Method RENDERER_BOB = method(LivingEntityRenderer.class, "getBob", LivingEntity.class, float.class);
   @Nullable
   private static final Method HEAD_PARTS = method(AgeableListModel.class, "headParts");
   @Nullable
   private static final Method BODY_PARTS = method(AgeableListModel.class, "bodyParts");
   @Nullable
   private static final Field SCALE_HEAD = field(AgeableListModel.class, "scaleHead");
   private static final Field[] AGEABLE_LIST_BABY = fields(
      AgeableListModel.class, "babyHeadScale", "babyYHeadOffset", "babyZHeadOffset", "babyBodyScale", "bodyYOffset"
   );
   private static final Field[] AGEABLE_HIERARCHICAL_BABY = fields(AgeableHierarchicalModel.class, "youngScaleFactor", "bodyYOffset");

   private BodyCubes() {
   }

   public static int forEach(LimitlessFrame f, LivingEntity entity, BodyCubes.Sink sink) {
      Vec3 at = entity.getPosition(f.partialTick);
      PoseStack ps = new PoseStack();
      ps.translate((float)(at.x - f.cam.x), (float)(at.y - f.cam.y), (float)(at.z - f.cam.z));
      BodyCubes.Collector c = new BodyCubes.Collector(sink);
      if (!pose(f, entity, ps, c)) {
         boundingBox(f, entity, sink);
         return 1;
      } else {
         return c.count;
      }
   }

   public static List<Vector3f[]> collect(LimitlessFrame f, LivingEntity entity) {
      List<Vector3f[]> out = new ArrayList<>();
      forEach(f, entity, (c, i) -> {
         Vector3f[] copy = new Vector3f[8];

         for (int k = 0; k < 8; k++) {
            copy[k] = new Vector3f(c[k]);
         }

         out.add(copy);
      });
      return out;
   }

   public static Vector3f lerp(Vector3f[] c, float u, float v, float w, Vector3f out) {
      float x = 0.0F;
      float y = 0.0F;
      float z = 0.0F;

      for (int i = 0; i < 8; i++) {
         float k = ((i & 1) == 0 ? 1.0F - u : u) * ((i >> 1 & 1) == 0 ? 1.0F - v : v) * ((i >> 2 & 1) == 0 ? 1.0F - w : w);
         x += c[i].x * k;
         y += c[i].y * k;
         z += c[i].z * k;
      }

      return out.set(x, y, z);
   }

   public static void frontFaces(LimitlessMesh m, Vector3f[] c, int col) {
      for (int face = 0; face < 6; face++) {
         if (faces(c, face)) {
            int[] q = FACES[face];
            m.quad(c[q[0]], col, c[q[1]], col, c[q[2]], col, c[q[3]], col);
         }
      }
   }

   public static Vector3f[] inflate(Vector3f[] c, float by, Vector3f[] out) {
      Vector3f mid = centre(c, new Vector3f());

      for (int i = 0; i < 8; i++) {
         Vector3f d = new Vector3f(c[i]).sub(mid);
         float len = d.length();
         out[i] = len < 1.0E-5F ? new Vector3f(c[i]) : d.mul((len + by * 1.732F) / len).add(mid);
      }

      return out;
   }

   public static Vector3f centre(Vector3f[] corners, Vector3f out) {
      return out.set(corners[0]).add(corners[7]).mul(0.5F);
   }

   public static boolean faces(Vector3f[] c, int face) {
      int[] q = FACES[face];
      Vector3f a = c[q[0]];
      Vector3f b = c[q[1]];
      Vector3f d = c[q[3]];
      float ux = b.x - a.x;
      float uy = b.y - a.y;
      float uz = b.z - a.z;
      float vx = d.x - a.x;
      float vy = d.y - a.y;
      float vz = d.z - a.z;
      float nx = uy * vz - uz * vy;
      float ny = uz * vx - ux * vz;
      float nz = ux * vy - uy * vx;
      float mx = (c[0].x + c[7].x) * 0.5F;
      float my = (c[0].y + c[7].y) * 0.5F;
      float mz = (c[0].z + c[7].z) * 0.5F;
      float fx = (a.x + c[q[2]].x) * 0.5F;
      float fy = (a.y + c[q[2]].y) * 0.5F;
      float fz = (a.z + c[q[2]].z) * 0.5F;
      if (nx * (fx - mx) + ny * (fy - my) + nz * (fz - mz) < 0.0F) {
         nx = -nx;
         ny = -ny;
         nz = -nz;
      }

      return -(nx * fx + ny * fy + nz * fz) > 0.0F;
   }

   public static boolean outline(Vector3f[] c, int edge) {
      int[] e = EDGES[edge];
      int axis = Integer.numberOfTrailingZeros(e[0] ^ e[1]);
      int f1 = 0;
      int f2 = 0;
      int k = 0;

      for (int ax = 0; ax < 3; ax++) {
         if (ax != axis) {
            int face = ax * 2 + (e[0] >> ax & 1);
            if (k++ == 0) {
               f1 = face;
            } else {
               f2 = face;
            }
         }
      }

      return faces(c, f1) != faces(c, f2);
   }

   private static void boundingBox(LimitlessFrame f, LivingEntity entity, BodyCubes.Sink sink) {
      AABB b = entity.getBoundingBox();
      Vec3 at = entity.getPosition(f.partialTick);
      double dx = at.x - entity.getX();
      double dy = at.y - entity.getY();
      double dz = at.z - entity.getZ();
      Vector3f[] c = new Vector3f[8];

      for (int i = 0; i < 8; i++) {
         c[i] = new Vector3f(
            (float)(((i & 1) == 0 ? b.minX : b.maxX) + dx - f.cam.x),
            (float)(((i >> 1 & 1) == 0 ? b.minY : b.maxY) + dy - f.cam.y),
            (float)(((i >> 2 & 1) == 0 ? b.minZ : b.maxZ) + dz - f.cam.z)
         );
      }

      sink.box(c, 0);
   }

   private static boolean pose(LimitlessFrame f, LivingEntity entity, PoseStack ps, BodyCubes.Collector c) {
      if (f.mc.getEntityRenderDispatcher().getRenderer(entity) instanceof LivingEntityRenderer lr) {
         EntityModel model = lr.getModel();
         float pt = f.partialTick;
         float bodyRot = Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);
         float head = Mth.wrapDegrees(Mth.rotLerp(pt, entity.yHeadRotO, entity.yHeadRot) - bodyRot);
         float pitch = Mth.lerp(pt, entity.xRotO, entity.getXRot());
         float speed = 0.0F;
         float walk = 0.0F;
         if (entity.isAlive()) {
            speed = Math.min(1.0F, entity.walkAnimation.speed(pt));
            walk = entity.walkAnimation.position(pt) * (entity.isBaby() ? 3.0F : 1.0F);
         }

         model.attackTime = entity.getAttackAnim(pt);
         model.riding = entity.isPassenger();
         model.young = entity.isBaby();
         model.prepareMobModel(entity, walk, speed, pt);
         float bob = invoke(RENDERER_BOB, lr, entity, pt) instanceof Float v ? v : (float)entity.tickCount + pt;
         model.setupAnim(entity, walk, speed, bob, head, pitch);
         float s = entity.getScale();
         ps.scale(s, s, s);
         ps.mulPose(Axis.YP.rotationDegrees(180.0F - bodyRot));
         ps.scale(-1.0F, -1.0F, 1.0F);
         invoke(RENDERER_SCALE, lr, entity, ps, pt);
         ps.translate(0.0F, -1.501F, 0.0F);
         return visitParts(model, ps, c);
      } else {
         return false;
      }
   }

   private static boolean visitParts(EntityModel<?> model, PoseStack ps, BodyCubes.Collector c) {
      if (!(model instanceof HierarchicalModel<?> h)) {
         if (model instanceof ListModel<?> l) {
            l.parts().forEach(p -> p.visit(ps, c));
            return true;
         } else if (model instanceof AgeableListModel && HEAD_PARTS != null && BODY_PARTS != null) {
            Iterable<ModelPart> headParts;
            Iterable<ModelPart> bodyParts;
            try {
               headParts = (Iterable<ModelPart>)HEAD_PARTS.invoke(model);
               bodyParts = (Iterable<ModelPart>)BODY_PARTS.invoke(model);
            } catch (RuntimeException | ReflectiveOperationException var7) {
               return false;
            }

            float[] baby = model.young ? floats(AGEABLE_LIST_BABY, model) : null;
            ps.pushPose();
            if (baby != null) {
               if (Boolean.TRUE.equals(read(SCALE_HEAD, model))) {
                  float k = 1.5F / baby[0];
                  ps.scale(k, k, k);
               }

               ps.translate(0.0F, baby[1] / 16.0F, baby[2] / 16.0F);
            }

            headParts.forEach(p -> p.visit(ps, c));
            ps.popPose();
            ps.pushPose();
            if (baby != null) {
               float k = 1.0F / baby[3];
               ps.scale(k, k, k);
               ps.translate(0.0F, baby[4] / 16.0F, 0.0F);
            }

            bodyParts.forEach(p -> p.visit(ps, c));
            ps.popPose();
            return true;
         } else {
            return false;
         }
      } else {
         float[] babyx = model.young && h instanceof AgeableHierarchicalModel ? floats(AGEABLE_HIERARCHICAL_BABY, h) : null;
         ps.pushPose();
         if (babyx != null) {
            ps.scale(babyx[0], babyx[0], babyx[0]);
            ps.translate(0.0F, babyx[1] / 16.0F, 0.0F);
         }

         h.root().visit(ps, c);
         ps.popPose();
         return true;
      }
   }

   private static Field[] fields(Class<?> owner, String... names) {
      Field[] out = new Field[names.length];

      for (int i = 0; i < names.length; i++) {
         out[i] = field(owner, names[i]);
      }

      return out;
   }

   @Nullable
   private static float[] floats(Field[] fields, Object target) {
      float[] out = new float[fields.length];

      for (int i = 0; i < fields.length; i++) {
         if (!(read(fields[i], target) instanceof Float v)) {
            return null;
         }

         out[i] = v;
      }

      return out;
   }

   @Nullable
   private static Field field(Class<?> owner, String name) {
      for (Class<?> k = owner; k != null && k != Object.class; k = k.getSuperclass()) {
         try {
            Field f = k.getDeclaredField(name);
            f.setAccessible(true);
            return f;
         } catch (RuntimeException | ReflectiveOperationException var4) {
         }
      }

      return null;
   }

   @Nullable
   private static Method method(Class<?> owner, String name, Class<?>... args) {
      try {
         Method m = owner.getDeclaredMethod(name, args);
         m.setAccessible(true);
         return m;
      } catch (RuntimeException | ReflectiveOperationException var4) {
         return null;
      }
   }

   @Nullable
   private static Object read(@Nullable Field f, Object target) {
      if (f == null) {
         return null;
      } else {
         try {
            return f.get(target);
         } catch (RuntimeException | ReflectiveOperationException var3) {
            return null;
         }
      }
   }

   @Nullable
   private static Object invoke(@Nullable Method m, Object target, Object... args) {
      if (m == null) {
         return null;
      } else {
         try {
            return m.invoke(target, args);
         } catch (RuntimeException | ReflectiveOperationException var4) {
            return null;
         }
      }
   }

   private static final class Collector implements Visitor {
      final BodyCubes.Sink sink;
      final Vector3f[] corners = new Vector3f[8];
      final List<float[]> seen = new ArrayList<>();
      int count;

      Collector(BodyCubes.Sink sink) {
         this.sink = sink;

         for (int i = 0; i < 8; i++) {
            this.corners[i] = new Vector3f();
         }
      }

      public void visit(Pose pose, String path, int index, Cube cube) {
         Matrix4f m = pose.pose();

         for (int i = 0; i < 8; i++) {
            float x = ((i & 1) == 0 ? cube.minX : cube.maxX) / 16.0F;
            float y = ((i >> 1 & 1) == 0 ? cube.minY : cube.maxY) / 16.0F;
            float z = ((i >> 2 & 1) == 0 ? cube.minZ : cube.maxZ) / 16.0F;
            m.transformPosition(x, y, z, this.corners[i]);
         }

         float[] key = new float[]{this.corners[0].x, this.corners[0].y, this.corners[0].z, this.corners[7].x, this.corners[7].y, this.corners[7].z};

         for (float[] s : this.seen) {
            if (same(s, key)) {
               return;
            }
         }

         this.seen.add(key);
         this.sink.box(this.corners, this.count++);
      }

      private static boolean same(float[] a, float[] b) {
         for (int i = 0; i < a.length; i++) {
            if (Math.abs(a[i] - b[i]) > 0.001F) {
               return false;
            }
         }

         return true;
      }
   }

   @FunctionalInterface
   public interface Sink {
      void box(Vector3f[] var1, int var2);
   }
}
