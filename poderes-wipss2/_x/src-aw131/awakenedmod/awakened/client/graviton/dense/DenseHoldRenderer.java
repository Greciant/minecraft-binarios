package com.awakenedmod.awakened.client.graviton.dense;

import com.awakenedmod.awakened.client.render.FxLayer;
import com.awakenedmod.awakened.client.render.FxLayers;
import com.awakenedmod.awakened.client.render.SteeredParticle;
import com.awakenedmod.awakened.content.graviton.DenseAbility;
import com.awakenedmod.awakened.content.graviton.DenseVisual;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;

@EventBusSubscriber(
   modid = "awakened",
   value = {Dist.CLIENT}
)
public final class DenseHoldRenderer {
   private static final int PALE = 16769732;
   private static final int WARM = 16747069;
   private static final int DARK = 1971474;
   private static final int RING_SEGS = 72;
   private static final int GROUND_RINGS = 3;
   private static final int CHEST_RINGS = 2;
   private static final float PERIOD_EMPTY = 24.0F;
   private static final float PERIOD_FULL = 16.0F;
   private static final float RING_END = 0.4F;
   private static final double RING_CORE = 0.035;
   private static final double RING_SOFT = 0.16;
   private static final float FADE_IN = 4.0F;
   private static final float FADE_OUT = 4.0F;
   private static final float HIT_TICKS = 6.0F;
   private static final Map<Integer, DenseHoldRenderer.Local> LOCALS = new HashMap<>();
   private static ClientLevel memoryLevel;

   private DenseHoldRenderer() {
   }

   @SubscribeEvent
   public static void onRender(RenderLevelStageEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && event.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS) {
         float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
         draw(mc, event.getPoseStack(), event.getCamera().getPosition(), pt);
      }
   }

   private static void draw(Minecraft mc, PoseStack poseStack, Vec3 cam, float pt) {
      if (memoryLevel != mc.level) {
         LOCALS.clear();
         memoryLevel = mc.level;
      }

      long gameTime = mc.level.getGameTime();
      float t = (float)(gameTime % 100000L) + pt;
      Set<Integer> alive = new HashSet<>();
      List<DenseHoldRenderer.Frame> frames = new ArrayList<>();

      for (Player p : mc.level.players()) {
         DenseVisual vis = DenseVisual.of(p);
         DenseHoldRenderer.Local local = LOCALS.get(p.getId());
         if (vis.active() || local != null) {
            if (local == null) {
               local = new DenseHoldRenderer.Local();
               LOCALS.put(p.getId(), local);
            }

            DenseHoldRenderer.Frame f = step(local, vis, t, p, pt);
            if (f == null) {
               LOCALS.remove(p.getId());
            } else {
               alive.add(p.getId());
               frames.add(f);
               if (vis.active() && !mc.isPaused() && local.gritTick != gameTime) {
                  local.gritTick = gameTime;
                  grit(mc, mc.level, f, t);
               }
            }
         }
      }

      LOCALS.keySet().retainAll(alive);
      if (!frames.isEmpty()) {
         BufferSource buffers = mc.renderBuffers().bufferSource();
         poseStack.pushPose();
         poseStack.translate(-cam.x, -cam.y, -cam.z);
         Pose pose = poseStack.last();
         VertexConsumer dark = FxLayers.begin(buffers, FxLayer.PAINT);

         for (DenseHoldRenderer.Frame f : frames) {
            stain(pose, dark, mc.level, f);
         }

         FxLayers.end(buffers, FxLayer.PAINT);
         FxLayer glow = FxLayer.LIGHT;
         VertexConsumer add = FxLayers.begin(buffers, glow);

         for (DenseHoldRenderer.Frame f : frames) {
            upLight(pose, add, mc.level, f);
            groundRings(pose, add, mc.level, f, t);
            if (!f.firstPerson) {
               chestRings(pose, add, f, t);
            }
         }

         FxLayers.end(buffers, glow);
         poseStack.popPose();
      }
   }

   private static DenseHoldRenderer.Frame step(DenseHoldRenderer.Local l, DenseVisual vis, float t, Player p, float pt) {
      float dt = Float.isNaN(l.lastT) ? 0.0F : Mth.clamp(t - l.lastT, 0.0F, 5.0F);
      l.lastT = t;
      float targetRadius = (float)DenseAbility.releaseRadius((float)vis.stored());
      if (Float.isNaN(l.radius)) {
         l.radius = targetRadius;
      }

      if (vis.active()) {
         if (l.seenHits != Integer.MIN_VALUE && vis.hits() != l.seenHits) {
            l.hitAt = t;
         }

         l.seenHits = vis.hits();
         l.presence = Math.min(1.0F, l.presence + dt / 4.0F);
         float ease = 1.0F - (float)Math.exp((double)(-dt * 0.35F));
         l.shown = l.shown + (vis.chargeFraction() - l.shown) * ease;
         l.radius = l.radius + (targetRadius - l.radius) * ease;
      } else {
         l.presence -= dt / 4.0F;
         if (l.presence <= 0.0F) {
            return null;
         }
      }

      float u = (t - l.hitAt) / 6.0F;
      float hit = u >= 0.0F && u < 1.0F ? u : -1.0F;
      Minecraft mc = Minecraft.getInstance();
      boolean fp = p == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
      return new DenseHoldRenderer.Frame(p.getPosition(pt), l.radius, l.shown, l.presence, hit, vis.active() && vis.low(), fp);
   }

   private static float period(DenseHoldRenderer.Frame f) {
      return Mth.lerp(f.charge, 24.0F, 16.0F);
   }

   private static float progress(float t, float period, int i, int n) {
      float u = (t / period + (float)i / (float)n) % 1.0F;
      return u < 0.0F ? u + 1.0F : u;
   }

   private static float groundRadius(DenseHoldRenderer.Frame f, float u) {
      float r = Mth.lerp(u * u, f.radius, 0.4F);
      if (f.hit >= 0.0F) {
         r -= 0.3F * (1.0F - f.hit) * (1.0F - f.hit) * (0.4F + 0.6F * (1.0F - u));
      }

      return Math.max(0.2F, r);
   }

   private static int ringColor(DenseHoldRenderer.Frame f, float t) {
      return f.low && Mth.sin(t * (float) (Math.PI * 2.0 / 5.0)) > 0.0F ? 16747069 : 16769732;
   }

   private static void groundRings(Pose pose, VertexConsumer vc, ClientLevel level, DenseHoldRenderer.Frame f, float t) {
      float period = period(f);
      int rgb = ringColor(f, t);
      float base = (0.3F + 0.15F * f.charge) * f.presence * (f.low ? 1.3F : 1.0F);

      for (int i = 0; i < 3; i++) {
         float u = progress(t, period, i, 3);
         float a = base * Mth.clamp(u / 0.12F, 0.0F, 1.0F) * Mth.clamp((1.0F - u) / 0.2F, 0.0F, 1.0F);
         if (!(a <= 0.01F)) {
            float r = groundRadius(f, u);
            groundBand(pose, vc, level, f.feet, (double)r, a, rgb);
         }
      }
   }

   private static void groundBand(Pose pose, VertexConsumer vc, ClientLevel level, Vec3 feet, double r, float alpha, int rgb) {
      int segs = Mth.clamp((int)Math.round(r * 9.0), 16, 72);
      double[] ys = new double[segs + 1];

      for (int s = 0; s <= segs; s++) {
         double a = (Math.PI * 2) * (double)s / (double)segs;
         double y = groundY(level, feet.x + Math.cos(a) * r, feet.z + Math.sin(a) * r, feet.y);
         ys[s] = Double.isNaN(y) ? Double.NaN : y;
      }

      double[] radii = new double[]{r - 0.035 - 0.16, r - 0.035, r + 0.035, r + 0.035 + 0.16};
      float[] alphas = new float[]{0.0F, alpha, alpha, 0.0F};

      for (int s = 0; s < segs; s++) {
         if (!Double.isNaN(ys[s]) && !Double.isNaN(ys[s + 1]) && !(Math.abs(ys[s] - ys[s + 1]) > 0.6)) {
            double a0 = (Math.PI * 2) * (double)s / (double)segs;
            double a1 = (Math.PI * 2) * (double)(s + 1) / (double)segs;

            for (int k = 0; k < 3; k++) {
               double ri = Math.max(0.0, radii[k]);
               double ro = Math.max(0.0, radii[k + 1]);
               DenseHoldRenderer.V i0 = flat(feet, a0, ri, ys[s] + 0.03, argb(alphas[k], rgb));
               DenseHoldRenderer.V i1 = flat(feet, a1, ri, ys[s + 1] + 0.03, argb(alphas[k], rgb));
               DenseHoldRenderer.V o0 = flat(feet, a0, ro, ys[s] + 0.03, argb(alphas[k + 1], rgb));
               DenseHoldRenderer.V o1 = flat(feet, a1, ro, ys[s + 1] + 0.03, argb(alphas[k + 1], rgb));
               quad(pose, vc, i0, i1, o1, o0);
               quad(pose, vc, o0, o1, i1, i0);
            }
         }
      }
   }

   private static void chestRings(Pose pose, VertexConsumer vc, DenseHoldRenderer.Frame f, float t) {
      float period = period(f);
      int rgb = ringColor(f, t);
      double y = f.feet.y + 1.05;

      for (int i = 0; i < 2; i++) {
         float u = progress(t, period, i, 2);
         float r = Mth.lerp(u * u, 1.6F, 0.5F);
         float a = (0.18F + 0.12F * f.charge) * f.presence * Mth.clamp(u / 0.2F, 0.0F, 1.0F) * Mth.clamp((1.0F - u) / 0.25F, 0.0F, 1.0F);
         airBand(pose, vc, f.feet, y + 0.1 * (double)i, (double)r, 0.03, a, rgb);
      }

      if (f.hit >= 0.0F) {
         float r = Mth.lerp(f.hit, 1.4F, 0.3F);
         float a = 0.6F * (1.0F - f.hit) * f.presence;
         airBand(pose, vc, f.feet, y + 0.1, (double)r, 0.05, a, 16769732);
      }
   }

   private static void airBand(Pose pose, VertexConsumer vc, Vec3 feet, double y, double r, double w, float alpha, int rgb) {
      if (!(alpha <= 0.01F)) {
         int segs = 40;

         for (int s = 0; s < segs; s++) {
            double a0 = (Math.PI * 2) * (double)s / (double)segs;
            double a1 = (Math.PI * 2) * (double)(s + 1) / (double)segs;
            DenseHoldRenderer.V i0 = flat(feet, a0, r - w, y, argb(0.0F, rgb));
            DenseHoldRenderer.V i1 = flat(feet, a1, r - w, y, argb(0.0F, rgb));
            DenseHoldRenderer.V m0 = flat(feet, a0, r, y, argb(alpha, rgb));
            DenseHoldRenderer.V m1 = flat(feet, a1, r, y, argb(alpha, rgb));
            DenseHoldRenderer.V o0 = flat(feet, a0, r + w * 2.0, y, argb(0.0F, rgb));
            DenseHoldRenderer.V o1 = flat(feet, a1, r + w * 2.0, y, argb(0.0F, rgb));
            quad(pose, vc, i0, i1, m1, m0);
            quad(pose, vc, m0, m1, i1, i0);
            quad(pose, vc, m0, m1, o1, o0);
            quad(pose, vc, o0, o1, m1, m0);
         }
      }
   }

   private static void grit(Minecraft mc, ClientLevel level, DenseHoldRenderer.Frame f, float t) {
      float period = period(f);
      float perRing = (1.0F + 2.0F * f.charge) / 3.0F;

      for (int i = 0; i < 3; i++) {
         float u = progress(t, period, i, 3);
         if (!(u < 0.08F) && !(u > 0.85F) && !(level.random.nextFloat() > perRing)) {
            float r = groundRadius(f, u);
            double a = level.random.nextDouble() * Math.PI * 2.0;
            double x = f.feet.x + Math.cos(a) * (double)r;
            double z = f.feet.z + Math.sin(a) * (double)r;
            BlockPos pos = BlockPos.containing(x, f.feet.y - 0.2, z);
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
               pos = pos.below();
               state = level.getBlockState(pos);
            }

            double y = groundY(level, x, z, f.feet.y);
            if (!state.isAir() && state.getFluidState().isEmpty() && !Double.isNaN(y)) {
               double speed = Mth.clamp(2.0 * (double)u * (double)(f.radius - 0.4F) / (double)period * 0.7, 0.05, 0.35);
               SteeredParticle.block(state, new Vec3(x, y + 0.05, z), new Vec3(-Math.cos(a) * speed, 0.06, -Math.sin(a) * speed));
            }
         }
      }
   }

   private static void stain(Pose pose, VertexConsumer vc, ClientLevel level, DenseHoldRenderer.Frame f) {
      double y = groundY(level, f.feet.x, f.feet.z, f.feet.y);
      if (!Double.isNaN(y)) {
         float a = (0.35F + 0.25F * f.charge) * f.presence;
         disc(pose, vc, f.feet, y + 0.012, 0.8 + 1.0 * (double)f.charge, argb(a, 1971474), argb(0.0F, 1971474), false);
      }
   }

   private static void upLight(Pose pose, VertexConsumer vc, ClientLevel level, DenseHoldRenderer.Frame f) {
      float k = Mth.clamp((f.charge - 0.7F) / 0.3F, 0.0F, 1.0F);
      if (!(k <= 0.0F)) {
         double y = groundY(level, f.feet.x, f.feet.z, f.feet.y);
         if (!Double.isNaN(y)) {
            disc(pose, vc, f.feet, y + 0.02, 0.9 + 0.5 * (double)k, argb(0.12F * k * f.presence, 16747069), argb(0.0F, 16747069), true);
         }
      }
   }

   private static void disc(Pose pose, VertexConsumer vc, Vec3 c, double y, double r, int mid, int edge, boolean bothWindings) {
      DenseHoldRenderer.V m = new DenseHoldRenderer.V(c.x, y, c.z, mid);
      int segs = 24;

      for (int i = 0; i < segs; i++) {
         double a0 = (Math.PI * 2) * (double)i / (double)segs;
         double a1 = (Math.PI * 2) * (double)(i + 1) / (double)segs;
         DenseHoldRenderer.V e0 = flat(c, a0, r, y, edge);
         DenseHoldRenderer.V e1 = flat(c, a1, r, y, edge);
         tri(pose, vc, m, e0, e1);
         if (bothWindings) {
            tri(pose, vc, m, e1, e0);
         }
      }
   }

   private static double groundY(ClientLevel level, double x, double z, double nearY) {
      MutableBlockPos pos = BlockPos.containing(x, nearY + 0.6, z).mutable();

      for (int i = 0; i < 4; i++) {
         BlockState state = level.getBlockState(pos);
         if (!state.isAir()) {
            VoxelShape shape = state.getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
               double top = (double)pos.getY() + shape.max(Axis.Y);
               if (top <= nearY + 0.7) {
                  return top;
               }

               return Double.NaN;
            }
         }

         pos.move(0, -1, 0);
      }

      return Double.NaN;
   }

   private static DenseHoldRenderer.V flat(Vec3 c, double a, double r, double y, int color) {
      return new DenseHoldRenderer.V(c.x + Math.cos(a) * r, y, c.z + Math.sin(a) * r, color);
   }

   private static void quad(Pose pose, VertexConsumer vc, DenseHoldRenderer.V a, DenseHoldRenderer.V b, DenseHoldRenderer.V c, DenseHoldRenderer.V d) {
      tri(pose, vc, a, b, c);
      tri(pose, vc, a, c, d);
   }

   private static void tri(Pose pose, VertexConsumer vc, DenseHoldRenderer.V a, DenseHoldRenderer.V b, DenseHoldRenderer.V c) {
      vertex(pose, vc, a);
      vertex(pose, vc, b);
      vertex(pose, vc, c);
   }

   private static void vertex(Pose pose, VertexConsumer vc, DenseHoldRenderer.V v) {
      vc.addVertex(pose, v.x, v.y, v.z).setColor(v.color);
   }

   private static int argb(float alpha, int rgb) {
      int a = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
      return a << 24 | rgb & 16777215;
   }

   private static record Frame(Vec3 feet, float radius, float charge, float presence, float hit, boolean low, boolean firstPerson) {
   }

   private static final class Local {
      int seenHits = Integer.MIN_VALUE;
      float hitAt = -1000.0F;
      float shown;
      float radius = Float.NaN;
      float presence;
      float lastT = Float.NaN;
      long gritTick = Long.MIN_VALUE;
   }

   private static record V(float x, float y, float z, int color) {
      V(double x, double y, double z, int color) {
         this((float)x, (float)y, (float)z, color);
      }
   }
}
