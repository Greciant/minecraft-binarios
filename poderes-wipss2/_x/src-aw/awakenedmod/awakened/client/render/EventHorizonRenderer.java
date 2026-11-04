package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.client.EventHorizonClient;
import com.awakenedmod.awakened.client.compat.ShaderPacks;
import com.awakenedmod.awakened.content.graviton.EventHorizonEntity;
import com.awakenedmod.awakened.content.graviton.EventHorizonVisuals;
import com.awakenedmod.awakened.core.Lab;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class EventHorizonRenderer {
   private static final Lab.Value CORE = Lab.CLIENT.def("graviton.hole.core", 2.2, 0.5, 6.0, 0.05);
   private static final Lab.Value DISK_IN = Lab.CLIENT.def("graviton.hole.disk.in", 2.5, 0.5, 8.0, 0.05);
   private static final Lab.Value DISK_OUT = Lab.CLIENT.def("graviton.hole.disk.out", 9.5, 2.0, 20.0, 0.1);
   private static final Lab.Value DISK_STRIPS = Lab.CLIENT.def("graviton.hole.disk.strips", 40.0, 6.0, 90.0, 1.0);
   private static final Lab.Value DISK_SEGS = Lab.CLIENT.def("graviton.hole.disk.segs", 96.0, 24.0, 200.0, 1.0);
   private static final Lab.Value DISK_ALPHA = Lab.CLIENT.def("graviton.hole.disk.alpha", 1.0, 0.0, 2.0, 0.05);
   private static final Lab.Value DISK_GLOW = Lab.CLIENT.def("graviton.hole.disk.glow", 0.55, 0.0, 2.0, 0.05);
   private static final Lab.Value BANDS = Lab.CLIENT.def("graviton.hole.disk.bands", 0.3, 0.0, 1.0, 0.05);
   private static final Lab.Value BEAMING = Lab.CLIENT.def("graviton.hole.disk.beaming", 0.45, 0.0, 1.0, 0.05);
   private static final Lab.Value SHIFT = Lab.CLIENT.def("graviton.hole.disk.shift", 0.18, 0.0, 0.6, 0.02);
   private static final Lab.Value SPIN = Lab.CLIENT.def("graviton.hole.spin", 1.0, 0.0, 5.0, 0.05);
   private static final Lab.Value FRONT_THICK = Lab.CLIENT.def("graviton.hole.front.thick", 0.44, 0.0, 1.2, 0.02);
   private static final Lab.Value FRONT_ALPHA = Lab.CLIENT.def("graviton.hole.front.alpha", 1.0, 0.0, 2.0, 0.05);
   private static final Lab.Value FRONT_FADE = Lab.CLIENT.def("graviton.hole.front.fade", 0.5, 0.05, 1.0, 0.05);
   private static final Lab.Value RING_WIDTH = Lab.CLIENT.def("graviton.hole.ring.width", 0.06, 0.01, 0.6, 0.01);
   private static final Lab.Value RING_ALPHA = Lab.CLIENT.def("graviton.hole.ring.alpha", 1.0, 0.0, 2.0, 0.05);
   private static final Lab.Value ARC_TOP = Lab.CLIENT.def("graviton.hole.arc.top", 0.75, 0.0, 1.5, 0.05);
   private static final Lab.Value ARC_BOTTOM = Lab.CLIENT.def("graviton.hole.arc.bottom", 0.32, 0.0, 1.5, 0.05);
   private static final Lab.Value ARC_ALPHA = Lab.CLIENT.def("graviton.hole.arc.alpha", 1.0, 0.0, 2.0, 0.05);
   private static final Lab.Value ARC_FACE = Lab.CLIENT.def("graviton.hole.arc.face", 0.16, 0.0, 1.0, 0.02);
   private static final Lab.Value HAZE = Lab.CLIENT.def("graviton.hole.haze", 0.6, 0.0, 2.0, 0.05);
   private static final Lab.Value TEAL = Lab.CLIENT.def("graviton.hole.teal", 0.4, 0.0, 1.0, 0.05);
   private static final Lab.Value STARS = Lab.CLIENT.def("graviton.hole.stars", 80.0, 0.0, 300.0, 1.0);
   private static final Lab.Value STREAKS = Lab.CLIENT.def("graviton.hole.streaks", 30.0, 0.0, 100.0, 1.0);
   private static final Lab.Value FLASH_RADIUS = Lab.CLIENT.def("graviton.hole.flash.radius", 9.0, 1.0, 30.0, 0.5);
   private static final Lab.Value SHOCK_RADIUS = Lab.CLIENT.def("graviton.hole.shock.radius", 22.0, 2.0, 60.0, 1.0);
   private static final float[][] RAMP = new float[][]{
      {0.0F, 255.0F, 250.0F, 240.0F},
      {0.15F, 255.0F, 236.0F, 208.0F},
      {0.35F, 255.0F, 208.0F, 156.0F},
      {0.6F, 250.0F, 152.0F, 88.0F},
      {0.8F, 214.0F, 92.0F, 40.0F},
      {1.0F, 120.0F, 36.0F, 14.0F}
   };
   private static final int RAMP_STEPS = 64;
   private static final float TWO_PI = (float) (Math.PI * 2);
   private static final float SPIN_BASE = 0.3F;
   private static final int COARSE_CELLS = 14;
   private static final float SYM_BAND = 0.12F;
   private static final int FINE_CELLS = 60;
   private static float coreRadiusSeen = 1.75F;
   private static final int STAMP_MIN_ALPHA = 150;

   private EventHorizonRenderer() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(event -> {
         Stage stage = ShaderPacks.active() ? Stage.AFTER_TRANSLUCENT_BLOCKS : Stage.AFTER_WEATHER;
         if (event.getStage() == stage) {
            draw(event);
         }
      });
   }

   public static float coreRadius() {
      return coreRadiusSeen;
   }

   private static void draw(RenderLevelStageEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
         Vec3 cam = event.getCamera().getPosition();
         Quaternionf orientation = event.getCamera().rotation();
         List<EventHorizonEntity> holes = EventHorizonClient.holes(pt);
         if (!holes.isEmpty()) {
            coreRadiusSeen = CORE.getF();
            float time = (float)(mc.level.getGameTime() % 100000L) + pt;
            PoseStack ps = event.getPoseStack();
            BufferSource buffers = mc.renderBuffers().bufferSource();

            for (EventHorizonEntity hole : holes) {
               EventHorizonRenderer.Scene scene = new EventHorizonRenderer.Scene(hole, cam, orientation, pt, time);
               scene.draw(ps, buffers);
            }
         }
      }
   }

   private static float smooth(float x) {
      float c = Mth.clamp(x, 0.0F, 1.0F);
      return c * c * (3.0F - 2.0F * c);
   }

   private static float cellNoise(int seed, float angle, int cells) {
      float x = angle / (float) (Math.PI * 2) * (float)cells;
      int c0 = Mth.floor(x);
      float f = x - (float)c0;
      f = f * f * (3.0F - 2.0F * f);
      int i0 = Math.floorMod(c0, cells);
      int i1 = Math.floorMod(c0 + 1, cells);
      float n0 = VfxGeometry.noise01(seed, i0, 5);
      float n1 = VfxGeometry.noise01(seed, i1, 5);
      return n0 + (n1 - n0) * f;
   }

   private static int rampColor(float u) {
      for (int i = 1; i < RAMP.length; i++) {
         if (u <= RAMP[i][0]) {
            float f = (u - RAMP[i - 1][0]) / (RAMP[i][0] - RAMP[i - 1][0]);
            int r = Math.round(Mth.lerp(f, RAMP[i - 1][1], RAMP[i][1]));
            int g = Math.round(Mth.lerp(f, RAMP[i - 1][2], RAMP[i][2]));
            int b = Math.round(Mth.lerp(f, RAMP[i - 1][3], RAMP[i][3]));
            return argbRgb(r, g, b);
         }
      }

      return argbRgb(Math.round(RAMP[RAMP.length - 1][1]), Math.round(RAMP[RAMP.length - 1][2]), Math.round(RAMP[RAMP.length - 1][3]));
   }

   private static int tint(int rgb, float bright, float alpha) {
      int r = Math.min(255, Math.round((float)(rgb >> 16 & 0xFF) * bright));
      int g = Math.min(255, Math.round((float)(rgb >> 8 & 0xFF) * bright));
      int b = Math.min(255, Math.round((float)(rgb & 0xFF) * bright));
      int a = Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F);
      return a << 24 | r << 16 | g << 8 | b;
   }

   private static int argbRgb(int r, int g, int b) {
      return clamp(r) << 16 | clamp(g) << 8 | clamp(b);
   }

   private static int argb(int a, int r, int g, int b) {
      return clamp(a) << 24 | clamp(r) << 16 | clamp(g) << 8 | clamp(b);
   }

   private static int clamp(int v) {
      return v < 0 ? 0 : Math.min(255, v);
   }

   private static final class Scene {
      final EventHorizonVisuals.Phase p;
      final Vec3 center;
      final Vec3 camOffset;
      final float time;
      final float groundDrop;
      final float rc;
      final float rcFull;
      final float rIn;
      final float rOut;
      final float bright;
      final float presence;
      final float spinExtra;
      final Vector3f u;
      final Vector3f v;
      final Vector3f n;
      final Vector3f view;
      final float elevation;
      final float sym;
      final float faceBlend;
      final float edge;
      final Vector3f farDir;
      final Vector3f lIn;
      final boolean[] farSeg;
      final Vector3f l;
      final Vector3f w;
      final Vector3f camRight;
      final Vector3f camUp;
      final float sigma = 1.0F;
      final float diskAlpha;
      final float diskGlow;
      final float bands;
      final float beaming;
      final float shift;
      final float spin;
      final float frontThick;
      final float frontAlpha;
      final float ringWidth;
      final float ringAlpha;
      final float arcTop;
      final float arcBottom;
      final float arcAlpha;
      final float arcFace;
      final float haze;
      final float teal;
      final int strips;
      final int segs;
      final int stars;
      final int streaks;
      final int[] ramp = new int[65];

      Scene(EventHorizonEntity hole, Vec3 cam, Quaternionf orientation, float pt, float time) {
         this.p = hole.phase(pt);
         this.center = hole.getPosition(pt);
         this.camOffset = cam.subtract(this.center);
         this.time = time;
         this.groundDrop = hole.groundDrop();
         this.diskAlpha = EventHorizonRenderer.DISK_ALPHA.getF();
         this.diskGlow = EventHorizonRenderer.DISK_GLOW.getF();
         this.bands = EventHorizonRenderer.BANDS.getF();
         this.beaming = EventHorizonRenderer.BEAMING.getF();
         this.shift = EventHorizonRenderer.SHIFT.getF();
         this.spin = EventHorizonRenderer.SPIN.getF();
         this.frontThick = EventHorizonRenderer.FRONT_THICK.getF();
         this.frontAlpha = EventHorizonRenderer.FRONT_ALPHA.getF();
         this.ringWidth = EventHorizonRenderer.RING_WIDTH.getF();
         this.ringAlpha = EventHorizonRenderer.RING_ALPHA.getF();
         this.arcTop = EventHorizonRenderer.ARC_TOP.getF();
         this.arcBottom = EventHorizonRenderer.ARC_BOTTOM.getF();
         this.arcAlpha = EventHorizonRenderer.ARC_ALPHA.getF();
         this.arcFace = EventHorizonRenderer.ARC_FACE.getF();
         this.haze = EventHorizonRenderer.HAZE.getF();
         this.teal = EventHorizonRenderer.TEAL.getF();
         this.strips = EventHorizonRenderer.DISK_STRIPS.getI();
         this.segs = EventHorizonRenderer.DISK_SEGS.getI();
         this.stars = EventHorizonRenderer.STARS.getI();
         this.streaks = EventHorizonRenderer.STREAKS.getI();

         for (int i = 0; i <= 64; i++) {
            this.ramp[i] = EventHorizonRenderer.rampColor((float)i / 64.0F);
         }

         this.rcFull = EventHorizonRenderer.CORE.getF();
         float diskIn = EventHorizonRenderer.DISK_IN.getF();
         float diskOut = EventHorizonRenderer.DISK_OUT.getF();
         float openE = 1.0F - (1.0F - this.p.open()) * (1.0F - this.p.open()) * (1.0F - this.p.open());
         float ce = this.p.collapse() * this.p.collapse();
         float coreNow = this.rcFull * openE * (1.0F - ce);
         float inNow = Mth.lerp(ce, Math.max(diskIn * openE, coreNow * 1.15F), 0.3F);
         float outNow = Mth.lerp(ce, inNow + (diskOut - diskIn) * (float)Math.pow((double)openE, 1.5), 1.4F);
         this.rc = coreNow;
         this.rIn = inNow;
         this.rOut = Math.max(outNow, inNow + 0.05F);
         float breathe = 0.9F + 0.1F * this.p.sustain() + 0.06F * Mth.sin(time * 0.25F);
         this.bright = breathe * (0.35F + 0.65F * this.p.open()) * (1.0F + 2.2F * this.p.collapse());
         this.presence = openE * (1.0F - (float)Math.pow((double)this.p.collapse(), 6.0));
         this.spinExtra = 9.0F * ce;
         Vec3[] frame = hole.diskFrame();
         this.u = new Vector3f((float)frame[0].x, (float)frame[0].y, (float)frame[0].z);
         this.v = new Vector3f((float)frame[1].x, (float)frame[1].y, (float)frame[1].z);
         this.n = new Vector3f((float)frame[2].x, (float)frame[2].y, (float)frame[2].z);
         Vector3f toHole = new Vector3f((float)(-this.camOffset.x), (float)(-this.camOffset.y), (float)(-this.camOffset.z));
         if (toHole.lengthSquared() < 1.0E-6F) {
            toHole.set(this.v);
         }

         this.view = toHole.normalize();
         this.camRight = orientation.transform(new Vector3f(1.0F, 0.0F, 0.0F));
         this.camUp = orientation.transform(new Vector3f(0.0F, 1.0F, 0.0F));
         float nv = this.n.dot(this.view);
         float side = -nv;
         this.elevation = Math.abs(nv);
         this.sym = 1.0F - EventHorizonRenderer.smooth(this.elevation / 0.12F);
         this.faceBlend = EventHorizonRenderer.smooth((this.elevation - 0.1F) / 0.8F);
         this.edge = 1.0F - EventHorizonRenderer.smooth(this.elevation / EventHorizonRenderer.FRONT_FADE.getF());
         Vector3f proj = new Vector3f(this.n).sub(new Vector3f(this.view).mul(nv));
         if (proj.lengthSquared() < 1.0E-4F) {
            proj.set(this.camUp).sub(new Vector3f(this.view).mul(this.camUp.dot(this.view)));
         }

         proj.normalize();
         if (side < 0.0F) {
            proj.negate();
         }

         this.w = proj;
         this.l = new Vector3f(this.view).cross(this.w).normalize();
         Vector3f inView = new Vector3f(this.view).sub(new Vector3f(this.n).mul(nv));
         this.farDir = inView.lengthSquared() > 1.0E-4F ? inView.normalize() : new Vector3f(this.v);
         Vector3f lPlane = new Vector3f(this.l).sub(new Vector3f(this.n).mul(this.n.dot(this.l)));
         this.lIn = lPlane.lengthSquared() > 1.0E-4F ? lPlane.normalize() : new Vector3f(this.u);
         this.farSeg = new boolean[this.segs];

         for (int j = 0; j < this.segs; j++) {
            float phi = (float) (Math.PI * 2) * ((float)j + 0.5F) / (float)this.segs;
            float c = Mth.cos(phi);
            float s = Mth.sin(phi);
            this.farSeg[j] = (this.u.x * c + this.v.x * s) * this.farDir.x
                  + (this.u.y * c + this.v.y * s) * this.farDir.y
                  + (this.u.z * c + this.v.z * s) * this.farDir.z
               > 0.0F;
         }
      }

      void draw(PoseStack ps, BufferSource buffers) {
         ps.pushPose();
         ps.translate(-this.camOffset.x, -this.camOffset.y, -this.camOffset.z);
         Pose pose = ps.last();
         if (this.rc > 0.01F) {
            EventHorizonRenderer.Tris solid = new EventHorizonRenderer.Tris(pose, FxLayers.begin(buffers, FxLayer.SOLID), false);
            sphere(solid, this.rc, 22, 44, EventHorizonRenderer.argb(255, 0, 0, 0), EventHorizonRenderer.argb(255, 0, 0, 0));
            FxLayers.end(buffers, FxLayer.SOLID);
         }

         EventHorizonRenderer.Tris paint = new EventHorizonRenderer.Tris(pose, FxLayers.begin(buffers, FxLayer.PAINT), false);
         if (this.presence > 0.01F) {
            this.backdrop(paint);
            boolean split = this.faceBlend < 0.5F;
            this.disk(paint, false, split ? 1 : 0);
            this.arcs(paint, false);
            this.photonRing(paint, false);
            if (split) {
               this.disk(paint, false, 2);
            }

            this.frontBand(paint, false);
         }

         if (this.p.collapse() > 0.5F && this.p.flash() < 1.0F) {
            this.point(paint, false);
         }

         if (this.p.flash() > 0.0F && this.p.flash() < 1.0F) {
            this.flash(paint, false);
            this.shockRing(paint, false);
         }

         FxLayers.end(buffers, FxLayer.PAINT);
         EventHorizonRenderer.Tris glow = new EventHorizonRenderer.Tris(pose, FxLayers.begin(buffers, FxLayer.LIGHT), true);
         if (this.presence > 0.01F) {
            this.disk(glow, true, 0);
            this.arcs(glow, true);
            this.frontBand(glow, true);
            this.photonRing(glow, true);
            this.halo(glow);
            this.stars(glow);
            this.streaks(glow);
         }

         if (this.p.open() < 1.0F) {
            this.ripple(glow);
         }

         if (this.p.collapse() > 0.5F && this.p.flash() < 1.0F) {
            this.point(glow, true);
         }

         if (this.p.flash() > 0.0F && this.p.flash() < 1.0F) {
            this.flash(glow, true);
            this.shockRing(glow, true);
         }

         FxLayers.end(buffers, FxLayer.LIGHT);
         RenderType stamp = FxLayers.depthStamp();
         if (stamp != null && this.presence > 0.01F) {
            float behindCore = (float)this.camOffset.length() + this.rc + 0.5F;
            EventHorizonRenderer.Tris depth = new EventHorizonRenderer.Tris(pose, FxLayers.pushedBack(buffers.getBuffer(stamp), behindCore), false, 150);
            this.disk(depth, false, 0);
            this.arcs(depth, false);
            this.photonRing(depth, false);
            this.frontBand(depth, false);
            buffers.endBatch(stamp);
         }

         ps.popPose();
      }

      private float swirl(int i, float phi, float r) {
         float omega = 1.0F * this.spin * 0.3F / (float)Math.pow((double)Math.max(r, 0.3F), 1.5);
         float a = phi - omega * this.time - 1.0F * this.spinExtra;
         float coarse = EventHorizonRenderer.cellNoise(i, a, 14);
         float fine = EventHorizonRenderer.cellNoise(i * 7 + 3, a, 60);
         float tone = 0.84F + 0.32F * VfxGeometry.noise01(i, 91, 7);
         return tone * (1.0F + this.bands * (1.25F * coarse + 0.75F * fine - 1.0F));
      }

      private float toward(float phi) {
         float tx = -Mth.sin(phi);
         float ty = Mth.cos(phi);
         float wx = 1.0F * (this.u.x * tx + this.v.x * ty);
         float wy = 1.0F * (this.u.y * tx + this.v.y * ty);
         float wz = 1.0F * (this.u.z * tx + this.v.z * ty);
         return -(wx * this.view.x + wy * this.view.y + wz * this.view.z);
      }

      private float toward(float dx, float dy, float dz) {
         float tx = this.n.y * dz - this.n.z * dy;
         float ty = this.n.z * dx - this.n.x * dz;
         float tz = this.n.x * dy - this.n.y * dx;
         return -1.0F * (tx * this.view.x + ty * this.view.y + tz * this.view.z);
      }

      private float beam(float toward) {
         return Math.max(0.2F, 1.0F + this.beaming * toward);
      }

      private void disk(EventHorizonRenderer.Tris t, boolean light, int part) {
         int rings = this.strips + 1;
         int cols = this.segs + 1;
         float[] px = new float[rings * cols];
         float[] py = new float[rings * cols];
         float[] pz = new float[rings * cols];
         int[] col = new int[rings * cols];
         float span = Math.max(0.05F, this.rOut - this.rIn);
         float glowSpan = 0.5F;

         for (int i = 0; i < rings; i++) {
            float ui = (float)i / (float)this.strips;
            float r = this.rIn + span * (float)Math.pow((double)ui, 1.45);
            float uRamp = (r - this.rIn) / span;
            float aPaint = uRamp < 0.7F ? 1.0F : 1.0F - (float)Math.pow((double)((uRamp - 0.7F) / 0.3F), 1.3);
            float aGlow = Math.max(0.0F, 1.0F - uRamp / glowSpan);
            aGlow = aGlow * aGlow * (float)Math.sqrt((double)aGlow);

            for (int j = 0; j < cols; j++) {
               float phi = (float) (Math.PI * 2) * (float)(j % this.segs) / (float)this.segs;
               float c = Mth.cos(phi);
               float s = Mth.sin(phi);
               int k = i * cols + j;
               px[k] = this.u.x * r * c + this.v.x * r * s;
               py[k] = this.u.y * r * c + this.v.y * r * s;
               pz[k] = this.u.z * r * c + this.v.z * r * s;
               float tw = this.toward(phi);
               float b = this.swirl(i, phi, r) * this.beam(tw) * this.bright;
               float a = light ? aGlow * this.diskGlow * 0.55F * this.presence : aPaint * this.diskAlpha * this.presence;
               col[k] = EventHorizonRenderer.tint(this.ramp(uRamp - this.shift * tw), b, a);
            }
         }

         for (int i = 0; i < this.strips && (!light || col[i * cols] >>> 24 != 0); i++) {
            for (int j = 0; j < this.segs; j++) {
               if (part == 0 || this.farSeg[j] == (part == 1)) {
                  int a = i * cols + j;
                  int b = a + 1;
                  int c = a + cols + 1;
                  int d = a + cols;
                  t.quad(px[a], py[a], pz[a], col[a], px[b], py[b], pz[b], col[b], px[c], py[c], pz[c], col[c], px[d], py[d], pz[d], col[d]);
               }
            }
         }
      }

      private void frontBand(EventHorizonRenderer.Tris t, boolean light) {
         if (!(this.frontAlpha <= 0.0F) && !(this.rc <= 0.05F) && !(this.edge <= 0.01F)) {
            int n = 56;
            int rows = 4;
            float depth = this.rc * 1.12F;
            float ox = -this.view.x * depth;
            float oy = -this.view.y * depth;
            float oz = -this.view.z * depth;
            float[] px = new float[(n + 1) * (rows + 1)];
            float[] py = new float[px.length];
            float[] pz = new float[px.length];
            int[] col = new int[px.length];
            float[] rowOff = new float[]{-1.0F, -0.55F, 0.0F, 0.55F, 1.0F};
            float[] rowA = new float[]{0.0F, 0.85F, 1.0F, 0.85F, 0.0F};

            for (int i = 0; i <= n; i++) {
               float s = -this.rOut + 2.0F * this.rOut * (float)i / (float)n;
               float as = Math.abs(s);
               float dip = (float)Math.pow((double)Math.max(0.0F, 1.0F - as / (2.2F * this.rc)), 1.2);
               float yc = -0.42F * this.rc * dip * (1.0F - this.sym);
               float h = this.rc * (this.frontThick * this.edge * (float)Math.pow((double)Math.max(0.0F, 1.0F - as / this.rOut), 1.5) + 0.02F);
               float uRamp = Mth.clamp((as - 2.0F * this.rc) / Math.max(0.1F, this.rOut - 2.0F * this.rc), 0.0F, 1.0F) * 0.8F;
               float dx = this.lIn.x * s - this.farDir.x * this.rIn;
               float dy = this.lIn.y * s - this.farDir.y * this.rIn;
               float dz = this.lIn.z * s - this.farDir.z * this.rIn;
               float dl = (float)Math.sqrt((double)(dx * dx + dy * dy + dz * dz));
               float tw = this.toward(dx / dl, dy / dl, dz / dl);
               float streak = 0.92F
                  + 0.16F * EventHorizonRenderer.cellNoise(401, s / this.rc * 0.9F - this.time * 0.02F, 24)
                  + 0.06F * EventHorizonRenderer.cellNoise(402, s / this.rc * 2.7F - this.time * 0.035F, 48);
               float b = streak * this.beam(tw) * this.bright;
               float aBase = this.presence * this.frontAlpha * this.edge * (light ? 0.45F * (1.0F - 0.6F * uRamp) : 0.97F * (1.0F - uRamp * uRamp));

               for (int k = 0; k <= rows; k++) {
                  float y = yc + rowOff[k] * h;
                  int idx = i * (rows + 1) + k;
                  px[idx] = ox + this.l.x * s + this.w.x * y;
                  py[idx] = oy + this.l.y * s + this.w.y * y;
                  pz[idx] = oz + this.l.z * s + this.w.z * y;
                  float uu = Math.min(1.0F, uRamp + 0.18F * Math.abs(rowOff[k])) - this.shift * tw;
                  col[idx] = EventHorizonRenderer.tint(this.ramp(uu), b, aBase * rowA[k]);
               }
            }

            for (int i = 0; i < n; i++) {
               for (int k = 0; k < rows; k++) {
                  int a = i * (rows + 1) + k;
                  int b = a + 1;
                  int c = a + rows + 2;
                  int d = a + rows + 1;
                  t.quad(px[a], py[a], pz[a], col[a], px[b], py[b], pz[b], col[b], px[c], py[c], pz[c], col[c], px[d], py[d], pz[d], col[d]);
               }
            }
         }
      }

      private void arcs(EventHorizonRenderer.Tris t, boolean light) {
         if (!(this.rc <= 0.05F)) {
            float fade = this.arcAlpha * this.presence;
            if (!(fade <= 0.01F)) {
               int segsA = 128;
               int rows = 7;
               float ri = this.rc;
               float even = 0.5F * (this.arcTop + this.arcBottom);
               float top = Mth.lerp(this.sym, this.arcTop, even);
               float bottom = Mth.lerp(this.sym, this.arcBottom, even);
               float[] px = new float[(segsA + 1) * (rows + 1)];
               float[] py = new float[px.length];
               float[] pz = new float[px.length];
               int[] col = new int[px.length];

               for (int j = 0; j <= segsA; j++) {
                  float psi = (float) (Math.PI * 2) * (float)(j % segsA) / (float)segsA;
                  float sw = Mth.cos(psi);
                  float sl = Mth.sin(psi);
                  float edgeThick = 0.1F
                     + top * (float)Math.pow((double)Math.max(0.0F, sw), 0.6)
                     + bottom * (float)Math.pow((double)Math.max(0.0F, -sw), 0.6)
                     + 0.5F * (float)Math.pow((double)Math.abs(sl), 6.0);
                  float thick = Mth.lerp(this.faceBlend, edgeThick, this.arcFace);
                  float ro = ri + this.rc * thick;
                  float aw = Math.abs(sw);
                  float dx = this.farDir.x * aw + this.lIn.x * sl;
                  float dy = this.farDir.y * aw + this.lIn.y * sl;
                  float dz = this.farDir.z * aw + this.lIn.z * sl;
                  float dl = (float)Math.sqrt((double)(dx * dx + dy * dy + dz * dz));
                  float tw = dl > 1.0E-4F ? 0.5F * this.toward(dx / dl, dy / dl, dz / dl) : 0.0F;
                  float swirlB = 0.9F + 0.2F * EventHorizonRenderer.cellNoise(211, psi - this.time * 0.012F, 16);
                  float b = swirlB * this.beam(tw) * this.bright;
                  float weight = sw >= 0.0F ? 1.0F : Mth.lerp(this.sym, 0.85F, 1.0F);

                  for (int k = 0; k <= rows; k++) {
                     float tt = (float)k / (float)rows;
                     float r = ri + (ro - ri) * (float)Math.pow((double)tt, 1.15);
                     int idx = j * (rows + 1) + k;
                     px[idx] = this.w.x * r * sw + this.l.x * r * sl;
                     py[idx] = this.w.y * r * sw + this.l.y * r * sl;
                     pz[idx] = this.w.z * r * sw + this.l.z * r * sl;
                     float uRamp = tt * 0.38F - this.shift * tw;
                     float a = fade * weight * (light ? 0.5F * (1.0F - tt * tt) : 0.97F * (1.0F - (float)Math.pow((double)tt, 5.0)));
                     col[idx] = EventHorizonRenderer.tint(this.ramp(uRamp), b, a);
                  }
               }

               for (int j = 0; j < segsA; j++) {
                  for (int k = 0; k < rows; k++) {
                     int a = j * (rows + 1) + k;
                     int b = a + 1;
                     int c = a + rows + 2;
                     int d = a + rows + 1;
                     t.quad(px[a], py[a], pz[a], col[a], px[b], py[b], pz[b], col[b], px[c], py[c], pz[c], col[c], px[d], py[d], pz[d], col[d]);
                  }
               }
            }
         }
      }

      private void photonRing(EventHorizonRenderer.Tris t, boolean light) {
         if (!(this.rc <= 0.05F)) {
            float a = this.ringAlpha * this.presence;
            int inner = EventHorizonRenderer.tint(EventHorizonRenderer.argbRgb(255, 252, 240), this.bright * 1.2F, a);
            int mid = EventHorizonRenderer.tint(EventHorizonRenderer.argbRgb(255, 240, 205), this.bright * 1.1F, light ? 0.7F * a : 0.8F * a);
            int outer = EventHorizonRenderer.tint(EventHorizonRenderer.argbRgb(255, 200, 140), this.bright, 0.0F);
            float r0 = this.rc * 0.995F;
            float r1 = this.rc * (1.0F + this.ringWidth * 0.5F);
            float r2 = this.rc * (1.0F + this.ringWidth);
            this.screenAnnulus(t, r0, r1, inner, mid, 96);
            this.screenAnnulus(t, r1, r2, mid, outer, 96);
         }
      }

      private void halo(EventHorizonRenderer.Tris t) {
         if (!(this.rc <= 0.05F) && !(this.haze <= 0.0F)) {
            float a = this.haze * this.presence;
            int in = EventHorizonRenderer.tint(EventHorizonRenderer.argbRgb(255, 190, 130), this.bright, 0.26F * a);
            int mid = EventHorizonRenderer.tint(EventHorizonRenderer.argbRgb(240, 120, 60), this.bright, 0.1F * a);
            int out = EventHorizonRenderer.tint(EventHorizonRenderer.argbRgb(180, 60, 30), this.bright, 0.0F);
            this.screenAnnulus(t, this.rc * 1.3F, this.rc * 2.4F, in, mid, 64);
            this.screenAnnulus(t, this.rc * 2.4F, this.rOut * 1.15F, mid, out, 64);
         }
      }

      private void backdrop(EventHorizonRenderer.Tris t) {
         if (!(this.teal <= 0.0F)) {
            float depth = this.rcFull * 1.6F;
            float ox = this.view.x * depth;
            float oy = this.view.y * depth;
            float oz = this.view.z * depth;
            int mid = EventHorizonRenderer.argb(Math.round(110.0F * this.teal * this.presence), 8, 42, 50);
            int rim = EventHorizonRenderer.argb(0, 8, 42, 50);
            int segsB = 40;
            float r = this.rOut * 1.5F;

            for (int j = 0; j < segsB; j++) {
               float a0 = (float) (Math.PI * 2) * (float)j / (float)segsB;
               float a1 = (float) (Math.PI * 2) * (float)(j + 1) / (float)segsB;
               float c0 = Mth.cos(a0) * r;
               float s0 = Mth.sin(a0) * r;
               float c1 = Mth.cos(a1) * r;
               float s1 = Mth.sin(a1) * r;
               t.tri(
                  ox,
                  oy,
                  oz,
                  mid,
                  ox + this.camRight.x * c0 + this.camUp.x * s0,
                  oy + this.camRight.y * c0 + this.camUp.y * s0,
                  oz + this.camRight.z * c0 + this.camUp.z * s0,
                  rim,
                  ox + this.camRight.x * c1 + this.camUp.x * s1,
                  oy + this.camRight.y * c1 + this.camUp.y * s1,
                  oz + this.camRight.z * c1 + this.camUp.z * s1,
                  rim
               );
            }
         }
      }

      private void screenAnnulus(EventHorizonRenderer.Tris t, float r0, float r1, int c0, int c1, int segsR) {
         for (int j = 0; j < segsR; j++) {
            float a0 = (float) (Math.PI * 2) * (float)j / (float)segsR;
            float a1 = (float) (Math.PI * 2) * (float)(j + 1) / (float)segsR;
            float ca = Mth.cos(a0);
            float sa = Mth.sin(a0);
            float cb = Mth.cos(a1);
            float sb = Mth.sin(a1);
            t.quad(
               this.l.x * r0 * ca + this.w.x * r0 * sa,
               this.l.y * r0 * ca + this.w.y * r0 * sa,
               this.l.z * r0 * ca + this.w.z * r0 * sa,
               c0,
               this.l.x * r0 * cb + this.w.x * r0 * sb,
               this.l.y * r0 * cb + this.w.y * r0 * sb,
               this.l.z * r0 * cb + this.w.z * r0 * sb,
               c0,
               this.l.x * r1 * cb + this.w.x * r1 * sb,
               this.l.y * r1 * cb + this.w.y * r1 * sb,
               this.l.z * r1 * cb + this.w.z * r1 * sb,
               c1,
               this.l.x * r1 * ca + this.w.x * r1 * sa,
               this.l.y * r1 * ca + this.w.y * r1 * sa,
               this.l.z * r1 * ca + this.w.z * r1 * sa,
               c1
            );
         }
      }

      private void stars(EventHorizonRenderer.Tris t) {
         if (this.stars > 0) {
            float a = 0.9F * this.presence;

            for (int k = 0; k < this.stars; k++) {
               float h1 = VfxGeometry.noise01(k, 1, 1);
               float h2 = VfxGeometry.noise01(k, 2, 1);
               float h3 = VfxGeometry.noise01(k, 3, 1);
               float h4 = VfxGeometry.noise01(k, 4, 1);
               float h5 = VfxGeometry.noise01(k, 5, 1);
               float theta = (float) (Math.PI * 2) * h1;
               float cz = 2.0F * h2 - 1.0F;
               float sz = (float)Math.sqrt((double)Math.max(0.0F, 1.0F - cz * cz));
               float r = this.rOut * (1.15F + 0.9F * h3);
               float x = sz * Mth.cos(theta) * r;
               float y = cz * r * 0.7F;
               float z = sz * Mth.sin(theta) * r;
               float twinkle = 0.45F + 0.55F * (0.5F + 0.5F * Mth.sin(this.time * (0.18F + 0.25F * h4) + h5 * (float) (Math.PI * 2)));
               float size = 0.045F + 0.06F * h5;
               int c = EventHorizonRenderer.argb(Math.round(255.0F * a * twinkle * (0.35F + 0.65F * h4)), 235, 240, 255);
               this.billboard(t, x, y, z, size, c);
            }
         }
      }

      private void streaks(EventHorizonRenderer.Tris t) {
         if (this.streaks > 0) {
            float r0 = this.rOut * 1.45F;

            for (int k = 0; k < this.streaks; k++) {
               float h1 = VfxGeometry.noise01(k, 11, 3);
               float h2 = VfxGeometry.noise01(k, 12, 3);
               float h3 = VfxGeometry.noise01(k, 13, 3);
               float h4 = VfxGeometry.noise01(k, 14, 3);
               float period = 70.0F + 70.0F * h1;
               float cycle = this.time / period + h2;
               int lap = (int)Math.floor((double)cycle);
               float s = cycle - (float)lap;
               float h5 = VfxGeometry.noise01(k, 15, lap);
               float phi0 = (float) (Math.PI * 2) * h3 + (float)lap * 2.399F;
               float sTail = Math.max(0.0F, s - 0.009F);
               float[] head = this.spiralPoint(s, r0, phi0, h4, h5);
               float[] tail = this.spiralPoint(sTail, r0, phi0, h4, h5);
               float in = Mth.clamp(s / 0.15F, 0.0F, 1.0F);
               float out = Mth.clamp((1.0F - s) / 0.08F, 0.0F, 1.0F);
               float a = this.presence * in * out * (0.55F + 0.45F * h1);
               float width = 0.025F + 0.05F * (1.0F - s) + 0.025F * h5;
               this.ribbon(
                  t,
                  tail[0],
                  tail[1],
                  tail[2],
                  head[0],
                  head[1],
                  head[2],
                  width,
                  EventHorizonRenderer.argb(0, 255, 160, 80),
                  EventHorizonRenderer.argb(Math.round(230.0F * a), 255, 236, 200)
               );
            }
         }
      }

      private float[] spiralPoint(float s, float r0, float phi0, float zScatter, float turnsJitter) {
         float r = r0 + (this.rIn * 0.96F - r0) * (float)Math.pow((double)s, 1.8);
         float phi = phi0 + 1.0F * (2.2F + 1.2F * turnsJitter) * (float) (Math.PI * 2) * (float)Math.pow((double)s, 2.2);
         float z = (zScatter - 0.5F) * 1.4F * (1.0F - s) * (1.0F - s);
         float c = Mth.cos(phi);
         float sn = Mth.sin(phi);
         return new float[]{
            this.u.x * r * c + this.v.x * r * sn + this.n.x * z,
            this.u.y * r * c + this.v.y * r * sn + this.n.y * z,
            this.u.z * r * c + this.v.z * r * sn + this.n.z * z
         };
      }

      private void ripple(EventHorizonRenderer.Tris t) {
         float o = this.p.open();
         float e = 1.0F - (1.0F - o) * (1.0F - o);
         float r = this.rOut * 1.8F * e + 0.2F;
         float a = (float)Math.pow((double)(1.0F - o), 1.3) * 0.7F;
         int in = EventHorizonRenderer.argb(Math.round(255.0F * a), 200, 235, 240);
         int out = EventHorizonRenderer.argb(0, 120, 200, 220);
         this.screenAnnulus(t, r, r + 0.5F + 1.2F * e, in, out, 64);
         this.screenAnnulus(t, Math.max(0.0F, r - 0.5F), r, out, in, 64);
      }

      private void point(EventHorizonRenderer.Tris t, boolean light) {
         float c = Mth.clamp((this.p.collapse() - 0.5F) / 0.5F, 0.0F, 1.0F);
         float r = 0.25F + 1.1F * c;
         this.bloom(t, 0.0F, r, light ? 0.55F * c : 0.9F * c * c, EventHorizonRenderer.argbRgb(255, 250, 240));
      }

      private void flash(EventHorizonRenderer.Tris t, boolean light) {
         float f = this.p.flash();
         float r = 0.6F + EventHorizonRenderer.FLASH_RADIUS.getF() * (float)Math.sqrt((double)f);
         float a = light ? (float)Math.pow((double)(1.0F - f), 1.6) * 0.5F : (float)Math.pow((double)(1.0F - f), 2.2) * 0.95F;
         this.bloom(t, 0.0F, r, a, EventHorizonRenderer.argbRgb(255, 248, 232));
         if (light) {
            float rr = EventHorizonRenderer.FLASH_RADIUS.getF() * 1.6F * (float)Math.pow((double)f, 0.7) + 0.5F;
            int ring = EventHorizonRenderer.argb(Math.round(200.0F * (1.0F - f)), 255, 236, 200);
            int none = EventHorizonRenderer.argb(0, 255, 200, 140);
            this.screenAnnulus(t, Math.max(0.0F, rr - 0.35F), rr, none, ring, 64);
            this.screenAnnulus(t, rr, rr + 0.5F + f, ring, none, 64);
         }
      }

      private void shockRing(EventHorizonRenderer.Tris t, boolean light) {
         float f = this.p.flash();
         float y = -this.groundDrop + 0.06F;
         float r = EventHorizonRenderer.SHOCK_RADIUS.getF() * (float)Math.pow((double)f, 0.6);
         float widthR = 1.2F + 2.5F * f;
         float a = (float)Math.pow((double)(1.0F - f), 1.3);
         int none = EventHorizonRenderer.argb(0, 255, 120, 60);
         if (light) {
            int hot = EventHorizonRenderer.argb(Math.round(230.0F * a), 255, 240, 210);
            flatRing(t, y, Math.max(0.0F, r - widthR * 0.35F), r, none, hot, 64);
            flatRing(t, y, r, r + widthR, hot, none, 64);
         } else {
            int warm = EventHorizonRenderer.argb(Math.round(150.0F * a), 255, 170, 80);
            flatRing(t, y, Math.max(0.0F, r - widthR * 0.25F), r, none, warm, 64);
            flatRing(t, y, r, r + widthR * 0.7F, warm, none, 64);
         }
      }

      private void bloom(EventHorizonRenderer.Tris t, float depth, float r, float alpha, int rgb) {
         int rings = 6;
         int segsB = 48;
         float ox = -this.view.x * depth;
         float oy = -this.view.y * depth;
         float oz = -this.view.z * depth;
         int[] cols = new int[rings + 1];
         float[] rad = new float[rings + 1];

         for (int k = 0; k <= rings; k++) {
            float q = (float)k / (float)rings;
            float fall = 1.0F - q * q;
            cols[k] = EventHorizonRenderer.tint(rgb, 1.0F, alpha * fall * fall);
            rad[k] = r * q;
         }

         for (int j = 0; j < segsB; j++) {
            float a0 = (float) (Math.PI * 2) * (float)j / (float)segsB;
            float a1 = (float) (Math.PI * 2) * (float)(j + 1) / (float)segsB;
            float ca = Mth.cos(a0);
            float sa = Mth.sin(a0);
            float cb = Mth.cos(a1);
            float sb = Mth.sin(a1);

            for (int k = 0; k < rings; k++) {
               float r0 = rad[k];
               float r1 = rad[k + 1];
               t.quad(
                  ox + this.camRight.x * r0 * ca + this.camUp.x * r0 * sa,
                  oy + this.camRight.y * r0 * ca + this.camUp.y * r0 * sa,
                  oz + this.camRight.z * r0 * ca + this.camUp.z * r0 * sa,
                  cols[k],
                  ox + this.camRight.x * r0 * cb + this.camUp.x * r0 * sb,
                  oy + this.camRight.y * r0 * cb + this.camUp.y * r0 * sb,
                  oz + this.camRight.z * r0 * cb + this.camUp.z * r0 * sb,
                  cols[k],
                  ox + this.camRight.x * r1 * cb + this.camUp.x * r1 * sb,
                  oy + this.camRight.y * r1 * cb + this.camUp.y * r1 * sb,
                  oz + this.camRight.z * r1 * cb + this.camUp.z * r1 * sb,
                  cols[k + 1],
                  ox + this.camRight.x * r1 * ca + this.camUp.x * r1 * sa,
                  oy + this.camRight.y * r1 * ca + this.camUp.y * r1 * sa,
                  oz + this.camRight.z * r1 * ca + this.camUp.z * r1 * sa,
                  cols[k + 1]
               );
            }
         }
      }

      private static void sphere(EventHorizonRenderer.Tris t, float radius, int rings, int segsS, int colorTop, int colorBottom) {
         for (int i = 0; i < rings; i++) {
            double t0 = Math.PI * (double)i / (double)rings;
            double t1 = Math.PI * (double)(i + 1) / (double)rings;
            float y0 = (float)Math.cos(t0) * radius;
            float y1 = (float)Math.cos(t1) * radius;
            float r0 = (float)Math.sin(t0) * radius;
            float r1 = (float)Math.sin(t1) * radius;
            int c0 = i < rings / 2 ? colorTop : colorBottom;

            for (int j = 0; j < segsS; j++) {
               double p0 = (Math.PI * 2) * (double)j / (double)segsS;
               double p1 = (Math.PI * 2) * (double)(j + 1) / (double)segsS;
               float ca = (float)Math.cos(p0);
               float sa = (float)Math.sin(p0);
               float cb = (float)Math.cos(p1);
               float sb = (float)Math.sin(p1);
               t.quad(r0 * ca, y0, r0 * sa, c0, r1 * ca, y1, r1 * sa, c0, r1 * cb, y1, r1 * sb, c0, r0 * cb, y0, r0 * sb, c0);
            }
         }
      }

      private static void flatRing(EventHorizonRenderer.Tris t, float y, float r0, float r1, int c0, int c1, int segsR) {
         for (int j = 0; j < segsR; j++) {
            float a0 = (float) (Math.PI * 2) * (float)j / (float)segsR;
            float a1 = (float) (Math.PI * 2) * (float)(j + 1) / (float)segsR;
            float ca = Mth.cos(a0);
            float sa = Mth.sin(a0);
            float cb = Mth.cos(a1);
            float sb = Mth.sin(a1);
            t.quad(r0 * ca, y, r0 * sa, c0, r0 * cb, y, r0 * sb, c0, r1 * cb, y, r1 * sb, c1, r1 * ca, y, r1 * sa, c1);
         }
      }

      private void billboard(EventHorizonRenderer.Tris t, float x, float y, float z, float half, int color) {
         float rx = this.camRight.x * half;
         float ry = this.camRight.y * half;
         float rz = this.camRight.z * half;
         float ux = this.camUp.x * half;
         float uy = this.camUp.y * half;
         float uz = this.camUp.z * half;
         t.quad(
            x - rx - ux,
            y - ry - uy,
            z - rz - uz,
            color,
            x + rx - ux,
            y + ry - uy,
            z + rz - uz,
            color,
            x + rx + ux,
            y + ry + uy,
            z + rz + uz,
            color,
            x - rx + ux,
            y - ry + uy,
            z - rz + uz,
            color
         );
      }

      private void ribbon(EventHorizonRenderer.Tris t, float ax, float ay, float az, float bx, float by, float bz, float half, int ca, int cb) {
         float dx = bx - ax;
         float dy = by - ay;
         float dz = bz - az;
         float px = dy * this.view.z - dz * this.view.y;
         float py = dz * this.view.x - dx * this.view.z;
         float pz = dx * this.view.y - dy * this.view.x;
         float len = (float)Math.sqrt((double)(px * px + py * py + pz * pz));
         if (len < 1.0E-5F) {
            px = this.camUp.x;
            py = this.camUp.y;
            pz = this.camUp.z;
            len = 1.0F;
         }

         px *= half / len;
         py *= half / len;
         pz *= half / len;
         t.quad(ax - px, ay - py, az - pz, ca, bx - px, by - py, bz - pz, cb, bx + px, by + py, bz + pz, cb, ax + px, ay + py, az + pz, ca);
      }

      private int ramp(float u) {
         int i = Math.round(Mth.clamp(u, 0.0F, 1.0F) * 64.0F);
         return this.ramp[i];
      }
   }

   private static final class Tris {
      final Pose pose;
      final VertexConsumer vc;
      final boolean twoSided;
      final int minAlpha;

      Tris(Pose pose, VertexConsumer vc, boolean twoSided) {
         this(pose, vc, twoSided, 0);
      }

      Tris(Pose pose, VertexConsumer vc, boolean twoSided, int minAlpha) {
         this.pose = pose;
         this.vc = vc;
         this.twoSided = twoSided;
         this.minAlpha = minAlpha;
      }

      void tri(float ax, float ay, float az, int ca, float bx, float by, float bz, int cb, float cx, float cy, float cz, int cc) {
         if (this.minAlpha <= 0 || Math.min(ca >>> 24, Math.min(cb >>> 24, cc >>> 24)) >= this.minAlpha) {
            this.vc.addVertex(this.pose, ax, ay, az).setColor(ca);
            this.vc.addVertex(this.pose, bx, by, bz).setColor(cb);
            this.vc.addVertex(this.pose, cx, cy, cz).setColor(cc);
            if (this.twoSided) {
               this.vc.addVertex(this.pose, ax, ay, az).setColor(ca);
               this.vc.addVertex(this.pose, cx, cy, cz).setColor(cc);
               this.vc.addVertex(this.pose, bx, by, bz).setColor(cb);
            }
         }
      }

      void quad(
         float ax, float ay, float az, int ca, float bx, float by, float bz, int cb, float cx, float cy, float cz, int cc, float dx, float dy, float dz, int cd
      ) {
         this.tri(ax, ay, az, ca, bx, by, bz, cb, cx, cy, cz, cc);
         this.tri(ax, ay, az, ca, cx, cy, cz, cc, dx, dy, dz, cd);
      }
   }
}
