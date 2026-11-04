package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.awakenedmod.awakened.core.Lab;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class WaterComposites {
   static final float BASE_ALPHA = 0.8F;
   private static final float FOAM_BASE = 0.5F;
   static final float SHEET_WATER = 0.35F;
   static final float SHEET_SHEEN = 0.6F;
   private static final float PUDDLE_WATER = 0.75F;
   private static final float PUDDLE_DEPTH = 0.09F;
   private static final float SPRAY_WHITE = 0.4F;
   private static final Lab.Value CUBE = Lab.CLIENT.def("hydro.shell.cube", 1.0, 0.5, 2.0, 0.05);
   private static final Lab.Value SPRAY = Lab.CLIENT.def("hydro.shell.spray", 1.0, 0.0, 3.0, 0.05);

   private WaterComposites() {
   }

   public static void register() {
      VfxComposites.register(VfxEntity.Kind.WATER_SPLASH, WaterComposites::splash);
      VfxComposites.register(VfxEntity.Kind.WATER_RING, WaterComposites::ring);
      VfxComposites.register(VfxEntity.Kind.WATER_PUDDLE, WaterComposites::puddle);
      VfxComposites.register(VfxEntity.Kind.WATER_STREAM, WaterComposites::stream);
   }

   private static WaterMesh.Paint paint(VfxRenderState st, float fade) {
      return WaterMesh.Paint.of(st.color, 0.8F * st.intensity * fade, st.lightCoords);
   }

   private static float tailFade(float life, float tail) {
      return life < 1.0F - tail ? 1.0F : Math.max(0.0F, (1.0F - life) / tail);
   }

   private static float cube(float s, float base, float per, float max) {
      return Math.min(max, base + per * s) * CUBE.getF();
   }

   private static void splash(PoseStack ps, VfxDraw.Sink sink, VfxRenderState st, float life) {
      float s = st.scale;
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         float a = 0.8F * st.intensity;
         Vector3f n = normal(st);
         int count = Math.round((6.0F + 12.0F * s) * SPRAY.getF());
         if (count > 0) {
            float out = 0.1F + 0.15F * s;
            WaterCubeSpray.burstOnce(
               level,
               "water_splash" + st.seed,
               st.worldPos.add((double)(n.x * out), (double)(n.y * out), (double)(n.z * out)),
               new Vec3((double)n.x, (double)n.y, (double)n.z).scale((double)(0.08F + 0.1F * s)),
               WaterCubeSpray.Burst.of(count, 0.07F + 0.07F * s, cube(s, 0.1F, 0.2F, 0.34F))
                  .scattered(0.15F * s)
                  .lifted(0.1F + 0.1F * s)
                  .lasting(26)
                  .spinning(16.0F),
               new WaterCubeSpray.Look(st.color, a, a * 0.4F)
            );
         }
      }

      float fade = tailFade(life, 0.4F);
      if (!(fade <= 0.01F)) {
         WaterMesh.Paint water = paint(st, fade);
         WaterMesh.Paint foam = water.foam(water.opacity() * 0.5F);
         float cube = cube(s, 0.1F, 0.16F, 0.3F);
         float steps = Mth.sin((float) Math.PI * Math.min(1.0F, life * 1.3F)) * (1.5F + 2.5F * s);
         float h = cube * ((float)Mth.floor(steps) + WaterBlocks.snapStep(steps - (float)Mth.floor(steps), 3.0F));
         float r = s * (0.3F + 0.35F * life);
         float age = st.ageInTicks;
         int seed = st.seed;
         WaterFramePass.queue(ps, (pose, vc) -> WaterBlocks.ring(pose, vc, water, foam, r, cube, h, 0.02F, 0.4F, age, seed));
      }
   }

   private static void ring(PoseStack ps, VfxDraw.Sink sink, VfxRenderState st, float life) {
      float fade = (float)Math.pow((double)(1.0F - life), 1.2F);
      if (!(fade <= 0.02F)) {
         float e = 1.0F - (1.0F - life) * (1.0F - life);
         float s = st.scale;
         float radius = s * (0.3F + 0.9F * e);
         float cube = cube(s, 0.12F, 0.08F, 0.3F);
         float h = cube * (1.3F - 0.9F * life);
         float age = st.ageInTicks;
         int seed = st.seed;
         WaterMesh.Paint water = paint(st, fade);
         WaterMesh.Paint foam = water.foam(water.opacity() * 0.5F);
         WaterFramePass.queue(ps, (pose, vc) -> WaterBlocks.ring(pose, vc, water, foam, radius, cube, h, 0.03F, 0.3F, age, seed));
      }
   }

   private static void puddle(PoseStack ps, VfxDraw.Sink sink, VfxRenderState st, float life) {
      float fade = tailFade(life, 0.3F);
      if (!(fade <= 0.01F)) {
         float s = st.scale;
         float radius = s * (0.55F + 0.45F * Math.min(1.0F, life * 6.0F));
         float cube = cube(s, 0.18F, 0.1F, 0.34F);
         float drain = AnimEnvelopes.smoothstep(0.3F, 1.0F, life);
         WaterCubeGrid g = WaterBlocks.puddle(radius, cube, 0.09F, 0.35F, st.seed);
         if (drain > 0.0F) {
            for (int k = 0; k < g.cols(); k++) {
               for (int r = 0; r < g.rows(); r++) {
                  float h = g.height(k, r);
                  if (h > 0.0F) {
                     float out = Mth.sqrt(g.x(k) * g.x(k) + g.z(r) * g.z(r)) / Math.max(0.01F, radius);
                     g.set(k, r, h - drain * 0.09F * (1.0F + 1.2F * out));
                  }
               }
            }
         }

         WaterMesh.Paint water = paint(st, fade);
         WaterMesh.Paint plain = water.alpha(water.opacity() * 0.75F);
         WaterMesh.Paint sheen = water.sheen(water.opacity() * 0.6F);
         WaterFramePass.queue(ps, (pose, vc) -> {
            g.drawWater(pose, vc, plain, false);
            WaterFramePass.foam(pose, vc, (fp, fvc) -> g.drawFoam(fp, fvc, sheen, (k, rx, h) -> 0.5F));
         });
      }
   }

   private static void stream(PoseStack ps, VfxDraw.Sink sink, VfxRenderState st, float life) {
      Vec3 off = st.secondary;
      float len = (float)off.length();
      if (!(len < 0.05F)) {
         final float head = Math.min(1.0F, life * 1.6F);
         final float tail = Math.max(0.0F, life * 1.6F - 0.6F);
         if (!(head - tail < 0.02F)) {
            float fade = tailFade(life, 0.25F);
            WaterMesh.Paint water = paint(st, fade);
            WaterMesh.Paint fleck = water.foam(water.opacity() * 0.35F);
            Vector3f to = new Vector3f((float)off.x, (float)off.y, (float)off.z);
            Vector3f bow = new Vector3f(0.0F, Math.min(1.3F, len * 0.3F), 0.0F);
            final WaterMesh.Spine arc = WaterMesh.Spine.arc(new Vector3f(), to, bow, t -> 0.0F);
            final float r = st.scale * CUBE.getF();
            float age = st.ageInTicks;
            int seed = st.seed;
            WaterMesh.Spine slug = new WaterMesh.Spine() {
               @Override
               public Vector3f center(float t) {
                  return arc.center(Mth.lerp(t, tail, head));
               }

               @Override
               public float radius(float t) {
                  return r * (0.6F + 0.4F * Mth.sin((float) Math.PI * t)) * Mth.lerp(t, 1.0F, 0.6F);
               }
            };
            float along = len * (head - tail) * 1.05F;
            Vector3f cam = new Matrix4f(ps.last().pose()).invert().transformPosition(new Vector3f());
            WaterFramePass.queue(
               ps,
               (pose, vc) -> WaterBlocks.chain(
                     pose,
                     vc,
                     water,
                     fleck,
                     WaterBlocks.Chain.along(slug, along, r * 1.7F).flowing(age * 0.2F).rough(0.02F, 25.0F).foamed(0.15F).seeded(seed),
                     p -> AnimEnvelopes.smoothstep(0.8F, 2.0F, p.distance(cam))
                  )
            );
         }
      }
   }

   private static Vector3f normal(VfxRenderState st) {
      return new Quaternionf()
         .rotationY(st.yaw * (float) (Math.PI / 180.0))
         .rotateX(st.pitch * (float) (Math.PI / 180.0))
         .transform(new Vector3f(0.0F, 1.0F, 0.0F));
   }
}
