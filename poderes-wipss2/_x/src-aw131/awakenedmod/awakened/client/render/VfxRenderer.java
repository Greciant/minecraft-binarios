package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class VfxRenderer extends EntityRenderer<VfxEntity> {
   private static final float CRESCENT_ARC_DEG = 150.0F;

   public VfxRenderer(Context ctx) {
      super(ctx);
   }

   public ResourceLocation getTextureLocation(VfxEntity entity) {
      return null;
   }

   public boolean shouldRender(VfxEntity entity, Frustum frustum, double camX, double camY, double camZ) {
      return true;
   }

   public void render(VfxEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
      if (entity.ownerId() < 0 || !VfxOwnerHide.hidden(entity.ownerId())) {
         Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
         VfxCam.update(cam.getPosition(), cam.rotation());
         VfxRenderState state = new VfxRenderState();
         fill(state, entity);
         state.ageInTicks = (float)entity.tickCount + partialTick;
         state.lightCoords = packedLight;
         state.seed = entity.getId();
         this.draw(state, poseStack, new VfxDraw.Sink(buffers));
      }
   }

   private static void fill(VfxRenderState state, VfxEntity entity) {
      state.kind = entity.kind();
      state.scale = entity.scale();
      state.secondary = entity.secondary();
      state.lifetime = entity.lifetime();
      state.intensity = entity.intensity();
      state.color = entity.color();
      state.color2 = entity.color2();
      state.yaw = entity.yaw();
      state.pitch = entity.pitch();
      state.ownerId = entity.ownerId();
      state.ownerDim = entity.ownerDim();
      state.noDepth = entity.noDepth();
      state.delay = entity.delay();
      state.worldPos = entity.position();
   }

   private void draw(VfxRenderState state, PoseStack poseStack, VfxDraw.Sink sink) {
      float age = state.ageInTicks - (float)state.delay;
      if (!(age < 0.0F)) {
         float span = (float)(state.lifetime - state.delay);
         float life = span <= 0.0F ? 1.0F : Mth.clamp(age / span, 0.0F, 1.0F);
         float dim = VfxOwnerHide.ownerDim(state.ownerId, state.ownerDim);
         if (!(dim <= 0.01F)) {
            state.intensity *= dim;
            if (state.noDepth) {
               sink.light = FxLayer.LIGHT_THROUGH_WALLS;
            }

            boolean oriented = state.yaw != 0.0F || state.pitch != 0.0F;
            if (oriented) {
               poseStack.pushPose();
               poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
               poseStack.mulPose(Axis.XP.rotationDegrees(state.pitch));
            }

            float s = state.scale;
            float in = state.intensity;
            int col = state.color;
            int col2 = state.color2;
            switch (state.kind) {
               case SPHERE:
                  this.sphere(poseStack, sink, s, life, col, col2, in);
                  break;
               case RING:
                  this.shockRing(poseStack, sink, s, life, col, col2, in);
                  break;
               case DOME:
                  this.dome(poseStack, sink, s, life, col, col2, in);
                  break;
               case PILLAR:
                  this.pillar(poseStack, sink, s, state.secondary, life, col, col2, in);
                  break;
               case BEAM:
                  this.beam(poseStack, sink, state.secondary, s, life, col, col2, in);
                  break;
               case ARC:
                  this.arc(poseStack, sink, state.secondary, s, life, age, state.seed, col, col2, in);
                  break;
               case CRESCENT:
                  this.crescent(poseStack, sink, s, life, col, col2, in, (float)state.secondary.x);
                  break;
               case SPLAT:
                  this.splat(poseStack, sink, s, life, col, col2, in);
                  break;
               case PORTAL:
                  this.portal(poseStack, sink, s, state.secondary, age, span, col, col2, in);
                  break;
               case HELIX:
                  this.helix(poseStack, sink, s, state.secondary, life, col, col2, in);
                  break;
               case HALO:
                  this.halo(poseStack, sink, s, life, age, col, col2, in);
                  break;
               case SCORCH:
                  this.scorch(poseStack, sink, s, life, age, state.seed, col, in);
                  break;
               case PIT:
                  this.pit(poseStack, sink, s, age, span, col, in);
                  break;
               default:
                  VfxComposites.Drawer drawer = VfxComposites.get(state.kind);
                  if (drawer != null) {
                     drawer.draw(poseStack, sink, state, life);
                  }
            }

            if (oriented) {
               poseStack.popPose();
            }
         }
      }
   }

   private void sphere(PoseStack ps, VfxDraw.Sink c, float s, float life, int col, int col2, float in) {
      float grow = Math.min(1.0F, life * 3.0F);
      float r = s * (0.45F + 0.55F * grow);
      float fade = (1.0F - life) * (1.0F - life);
      int shellA = VfxDraw.ia(110.0F * fade, in);
      int coreA = VfxDraw.ia(170.0F * fade, in);
      if (coreA > 1) {
         int shell = VfxColor.argb(shellA, VfxColor.shade(col, 0.7F));
         int hot = VfxColor.argb(coreA, VfxColor.core(col, col2, 0.6F));
         VfxDraw.additive(ps, c, (pose, vc) -> {
            VfxGeometry.sphere(pose, vc, 0.0F, r, 10, 14, shell);
            VfxGeometry.sphere(pose, vc, 0.0F, r * 0.5F, 8, 12, hot);
         });
      }
   }

   private void shockRing(PoseStack ps, VfxDraw.Sink c, float s, float life, int col, int col2, float in) {
      float ease = 1.0F - (1.0F - life) * (1.0F - life);
      float rOuter = s * (0.3F + 1.7F * ease);
      float rInner = Math.max(0.0F, rOuter - s * 0.28F);
      int a = VfxDraw.ia(190.0F * (1.0F - life), in);
      if (a > 1) {
         int body = VfxColor.argb(a, col);
         int white = VfxColor.argb((int)((float)a * 0.8F), VfxColor.core(col, col2, 0.5F));
         VfxDraw.additive(ps, c, (pose, vc) -> {
            VfxGeometry.ring(pose, vc, 0.12F, rInner, rOuter, 40, body);
            VfxGeometry.ring(pose, vc, 0.14F, rInner + s * 0.08F, rOuter - s * 0.08F, 40, white);
         });
      }
   }

   private void dome(PoseStack ps, VfxDraw.Sink c, float s, float life, int col, int col2, float in) {
      float ease = 1.0F - (1.0F - life) * (1.0F - life);
      float r = s * (0.5F + 0.7F * ease);
      int a = VfxDraw.ia(120.0F * (1.0F - life), in);
      if (a > 1) {
         int body = VfxColor.argb(a, col);
         int hot = VfxColor.argb((int)((float)a * 0.7F), VfxColor.core(col, col2, 0.55F));
         VfxDraw.additive(ps, c, (pose, vc) -> {
            VfxGeometry.dome(pose, vc, 0.05F, r, 9, 18, 0.0F, 0, body);
            VfxGeometry.dome(pose, vc, 0.05F, r * 0.7F, 7, 14, 0.0F, 0, hot);
         });
      }
   }

   private void pillar(PoseStack ps, VfxDraw.Sink c, float s, Vec3 sec, float life, int col, int col2, float in) {
      float h = (float)sec.y;
      float grow = Math.min(1.0F, life * 2.5F);
      float curH = h * (0.3F + 0.7F * grow);
      float fade = (1.0F - life) * (1.0F - life);
      int a = VfxDraw.ia(150.0F * fade, in);
      if (a > 1 && !(curH <= 0.01F)) {
         int body = VfxColor.argb(a, col);
         int hot = VfxColor.argb(a, VfxColor.core(col, col2, 0.6F));
         VfxDraw.additive(ps, c, (pose, vc) -> {
            VfxGeometry.cylinder(pose, vc, 0.0F, s, curH, s * 0.85F, 18, 0.0F, 0, body);
            VfxGeometry.cylinder(pose, vc, 0.0F, s * 0.45F, curH, s * 0.38F, 14, 0.0F, 0, hot);
         });
      }
   }

   private void beam(PoseStack ps, VfxDraw.Sink c, Vec3 sec, float s, float life, int col, int col2, float in) {
      Vector3f end = new Vector3f((float)sec.x, (float)sec.y, (float)sec.z);
      float fade = 1.0F - life * life;
      int a = VfxDraw.ia(210.0F * fade, in);
      if (a > 1 && !(end.lengthSquared() < 1.0E-4F)) {
         float w = s * (0.9F + 0.3F * (1.0F - life));
         int body = VfxColor.argb(a, col);
         int hot = VfxColor.argb(a, VfxColor.core(col, col2, 0.65F));
         VfxDraw.additive(ps, c, (pose, vc) -> {
            VfxGeometry.beam(pose, vc, end, w, body);
            VfxGeometry.beam(pose, vc, end, w * 0.4F, hot);
         });
      }
   }

   private void crescent(PoseStack ps, VfxDraw.Sink c, float s, float life, int col, int col2, float in, float rollDeg) {
      float ease = 1.0F - (1.0F - life) * (1.0F - life);
      float r = s * (0.8F + 0.3F * ease);
      float thick = s * 0.42F * (0.55F + 0.45F * (1.0F - life));
      int a = VfxDraw.ia(230.0F * (1.0F - life), in);
      if (a > 1) {
         int body = VfxColor.argb(a, col);
         int hot = VfxColor.argb(a, VfxColor.core(col, col2, 0.6F));
         boolean rolled = rollDeg != 0.0F;
         if (rolled) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(rollDeg));
         }

         VfxDraw.additive(ps, c, (pose, vc) -> {
            VfxGeometry.crescent(pose, vc, r, thick, 150.0F, 26, body);
            VfxGeometry.crescent(pose, vc, r, thick * 0.4F, 150.0F, 26, hot);
         });
         if (rolled) {
            ps.popPose();
         }
      }
   }

   private void splat(PoseStack ps, VfxDraw.Sink c, float s, float life, int col, int col2, float in) {
      float ease = 1.0F - (1.0F - life) * (1.0F - life);
      float r = s * (0.6F + 0.4F * ease);
      int bodyA = VfxDraw.ia(140.0F * (1.0F - life), in);
      if (bodyA > 1) {
         int body = VfxColor.argb(bodyA, col);
         int hot = VfxColor.argb((int)((float)bodyA * 0.6F), VfxColor.core(col, col2, 0.5F));
         VfxDraw.additive(ps, c, (pose, vc) -> {
            VfxGeometry.disc(pose, vc, 0.0F, r, 22, body);
            VfxGeometry.disc(pose, vc, 0.02F, r * 0.55F, 18, hot);
         });
      }
   }

   private void portal(PoseStack ps, VfxDraw.Sink c, float s, Vec3 sec, float age, float span, int col, int col2, float in) {
      float openRaw = VfxEntity.portalOpen(age, span);
      float open = openRaw * openRaw * (3.0F - 2.0F * openRaw);
      if (!(open <= 0.02F)) {
         float ellipse = sec.x > 0.0 ? (float)sec.x : 1.35F;
         float b1 = Mth.sin(age * 0.1F);
         float breathe = 1.0F + 0.04F * b1 + 0.018F * Mth.sin(age * 0.233F + 1.7F);
         float r = s * open * breathe;
         float opening = Mth.clamp(1.0F - age / Math.max(1.0F, VfxEntity.portalOpenWindow(span)), 0.0F, 1.0F);
         float closing = Mth.clamp(1.0F - (span - age) / Math.max(1.0F, VfxEntity.portalCloseWindow(span)), 0.0F, 1.0F);
         float spin = age * 0.16F + 0.5F * Mth.sin(age * 0.045F) + 2.2F * opening * opening - 3.5F * closing * closing * closing;
         float wob = 0.045F;
         float wobPhase = age * 0.09F;
         int heartCol = VfxColor.argb(VfxDraw.ia(235.0F * open, in), VfxColor.shade(col, 0.08F));
         int mawCol = VfxColor.argb(VfxDraw.ia(210.0F * open, in), VfxColor.shade(col, 0.16F));
         int edgeCol = VfxColor.argb(VfxDraw.ia(230.0F * open, in), VfxColor.shade(col, 0.1F));
         int edgeFade = edgeCol & 16777215;
         int armTip = VfxColor.argb(VfxDraw.ia(100.0F * open, in), VfxColor.shade(col, 0.06F));
         int armRoot = armTip & 16777215;
         VfxDraw.custom(ps, c, FxLayer.SOLID, (pose, vc) -> {
            VfxGeometry.annulusXY(pose, vc, 0.0F, 0.0F, r * 0.3F, 48, ellipse, 0.045F, wobPhase, heartCol, heartCol);
            VfxGeometry.annulusXY(pose, vc, 0.0F, r * 0.3F, r * 0.62F, 48, ellipse, 0.045F, wobPhase, heartCol, mawCol);
            VfxGeometry.annulusXY(pose, vc, 0.0F, r * 0.62F, r * 0.85F, 48, ellipse, 0.045F, wobPhase, mawCol, mawCol);
            VfxGeometry.annulusXY(pose, vc, 0.0F, r * 0.85F, r * 0.97F, 48, ellipse, 0.045F, wobPhase, mawCol, edgeCol);
         });
         VfxDraw.custom(ps, c, FxLayer.PAINT, (pose, vc) -> {
            VfxGeometry.annulusXY(pose, vc, 0.0F, r * 0.97F, r * 1.1F, 48, ellipse, 0.045F, wobPhase, edgeCol, edgeFade);

            for (int arm = 0; arm < 3; arm++) {
               float start = spin + (float)arm * 2.094F + 0.35F * (float)arm * (float)arm;
               float sweep = 4.1F + 0.5F * (float)arm;
               float reach = r * (0.68F + 0.04F * (float)arm);
               float wid = r * (0.105F + 0.012F * (float)arm);

               for (int side = -1; side <= 1; side += 2) {
                  VfxGeometry.spiralXY(pose, vc, (float)side * 0.03F, r * 0.1F, reach, start, sweep, wid, 26, ellipse, armRoot, armTip);
               }
            }
         });
         int rimTint = col2 != -1 ? col2 : col;
         int rimPeak = VfxColor.argb(VfxDraw.ia((78.0F - 14.0F * b1) * open, in), rimTint);
         int rimFade = rimPeak & 16777215;
         int wispTip = VfxColor.argb(VfxDraw.ia(30.0F * open, in), rimTint);
         int wispRoot = wispTip & 16777215;
         float shimmerSpin = -age * 0.224F - 0.4F * Mth.sin(age * 0.038F + 0.8F);
         VfxDraw.additive(
            ps,
            c,
            (pose, vc) -> {
               for (int side = -1; side <= 1; side += 2) {
                  VfxGeometry.annulusXY(pose, vc, (float)side * 0.02F, r * 0.84F, r * 0.905F, 48, ellipse, 0.045F, wobPhase, rimFade, rimPeak);
                  VfxGeometry.annulusXY(pose, vc, (float)side * 0.02F, r * 0.905F, r * 0.96F, 48, ellipse, 0.045F, wobPhase, rimPeak, rimFade);

                  for (int arm = 0; arm < 2; arm++) {
                     float start = shimmerSpin + (float)arm * (float) Math.PI;
                     VfxGeometry.spiralXY(
                        pose,
                        vc,
                        (float)side * 0.05F,
                        r * 0.15F,
                        r * (0.66F + 0.06F * (float)arm),
                        start,
                        3.5F + 0.6F * (float)arm,
                        r * 0.06F,
                        22,
                        ellipse,
                        wispRoot,
                        wispTip
                     );
                  }
               }
            }
         );
      }
   }

   private void helix(PoseStack ps, VfxDraw.Sink c, float s, Vec3 sec, float life, int col, int col2, float in) {
      float height = sec.x > 0.0 ? (float)sec.x : 1.5F;
      float turns = sec.y > 0.0 ? (float)sec.y : 2.5F;
      float grow = Math.min(1.0F, life / 0.35F);
      float fade = life < 0.7F ? 1.0F : (1.0F - life) / 0.3F;
      int a = VfxDraw.ia(170.0F * fade, in);
      if (a > 1) {
         int body = VfxColor.argb(a, col);
         int hot = VfxColor.argb((int)((float)a * 0.85F), VfxColor.core(col, col2, 0.6F));
         VfxDraw.additive(ps, c, (pose, vc) -> {
            VfxGeometry.helix(pose, vc, s, height, turns, 0.0F, grow, s * 0.16F, 14, body);
            VfxGeometry.helix(pose, vc, s * 0.96F, height, turns, (float) Math.PI, grow, s * 0.08F, 14, hot);
         });
      }
   }

   private void halo(PoseStack ps, VfxDraw.Sink c, float s, float life, float age, int col, int col2, float in) {
      float envelope = Math.min(1.0F, Math.min(life / 0.2F, (1.0F - life) / 0.25F));
      int a = VfxDraw.ia(140.0F * envelope, in);
      if (a > 1) {
         float r = s * (1.0F + 0.02F * Mth.sin(age * 0.35F));
         int body = VfxColor.argb(a, col);
         int hot = VfxColor.argb((int)((float)a * 0.75F), VfxColor.core(col, col2, 0.5F));
         VfxDraw.additive(ps, c, (pose, vc) -> {
            VfxGeometry.ring(pose, vc, 0.03F, r * 0.9F, r, 48, body);
            VfxGeometry.ring(pose, vc, 0.05F, r * 0.93F, r * 0.985F, 48, hot);
         });
      }
   }

   private void scorch(PoseStack ps, VfxDraw.Sink c, float s, float life, float age, int seed, int col, float in) {
      float pop = Math.min(1.0F, age / 2.0F);
      float fade = life < 0.6F ? 1.0F : (1.0F - life) / 0.4F;
      int a = VfxDraw.ia(150.0F * pop * fade, in);
      if (a > 1) {
         int core = VfxColor.argb(a, VfxColor.shade(col, 0.1F));
         int stain = VfxColor.argb((int)((float)a * 0.55F), VfxColor.shade(col, 0.16F));
         int crack = VfxColor.argb(Math.min(255, (int)((float)a * 1.4F)), VfxColor.shade(col, 0.05F));
         VfxDraw.custom(ps, c, FxLayer.PAINT, (pose, vc) -> {
            VfxGeometry.disc(pose, vc, 0.0F, s * 0.58F, 24, core);
            VfxGeometry.ring(pose, vc, 0.0F, s * 0.52F, s * 1.0F, 24, 0.22F, seed, stain);
            VfxGeometry.cracks(pose, vc, 0.004F, s * 0.25F, s * 1.5F, 7, seed, s * 0.06F, crack);
         });
      }
   }

   private void pit(PoseStack ps, VfxDraw.Sink c, float s, float age, float span, int col, float in) {
      float openT = Math.min(1.0F, age / 2.0F);
      float open = openT * openT * (3.0F - 2.0F * openT);
      float closeT = Mth.clamp((span - age) / 4.0F, 0.0F, 1.0F);
      float r = s * open * closeT * closeT;
      if (!(r <= 0.01F)) {
         int hole = VfxColor.argb(VfxDraw.ia(245.0F, in), VfxColor.shade(col, 0.04F));
         int lip = VfxColor.argb(VfxDraw.ia(38.0F * open * closeT, in), col);
         VfxDraw.custom(ps, c, FxLayer.SOLID, (pose, vc) -> VfxGeometry.disc(pose, vc, 0.0F, r, 28, hole));
         VfxDraw.additive(ps, c, (pose, vc) -> VfxGeometry.ring(pose, vc, 0.006F, r * 0.86F, r * 1.04F, 28, lip));
      }
   }

   private void arc(PoseStack ps, VfxDraw.Sink c, Vec3 sec, float s, float life, float age, int seedBase, int col, int col2, float in) {
      Vector3f end = new Vector3f((float)sec.x, (float)sec.y, (float)sec.z);
      float fade = 1.0F - life * life;
      int a = VfxDraw.ia(165.0F * fade, in);
      if (a > 1 && !(end.lengthSquared() < 1.0E-4F)) {
         int seed = seedBase * 31 + (int)(age * 0.6F);
         int segs = Mth.clamp((int)(end.length() * 1.1F), 5, 18);
         float w = s * (0.9F + 0.4F * (1.0F - life));
         int body = VfxColor.argb(a, col);
         int hot = VfxColor.argb(a, VfxColor.core(col, col2, 0.7F));
         VfxDraw.additive(ps, c, (pose, vc) -> {
            VfxGeometry.arc(pose, vc, end, w, segs, 0.11F, seed, body);
            VfxGeometry.arc(pose, vc, end, w * 0.35F, segs, 0.11F, seed, hot);
         });
      }
   }
}
