package com.devilfruits.client.limb;

import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.core.limb.LimbBands;
import com.devilfruits.core.limb.LimbPath;
import com.devilfruits.core.limb.LimbUv;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class LimbRenderer {
   private static final TexelBoxMesh MESH = new TexelBoxMesh();
   private static final float[] SEGMENTS = new float[LimbBands.capacity(1024.0F)];
   private static final LimbPath PATH = new LimbPath();
   private static final LimbFrame FRAME = new LimbFrame();
   private static final Quaternionf ROOT = new Quaternionf();
   private static final Quaternionf TURN = new Quaternionf();
   private static final Vector3f A = new Vector3f();
   private static final Vector3f B = new Vector3f();
   private static final Vector3f C = new Vector3f();
   private static final Vector3f N0 = new Vector3f();
   private static final float STEP = 4.0F;
   private static final float STEP_FINE = 2.0F;
   private static final float[] SWIRL_SEGMENTS = new float[4];

   public static void render(
      Pose pose,
      MultiBufferSource buffers,
      ResourceLocation texture,
      LimbUv.Layout skin,
      @Nullable LimbUv.Layout overlay,
      LimbPath path,
      Vector3fc n0,
      LimbSpec spec,
      int overlayCoords,
      int light0,
      int light1,
      int light2
   ) {
      boolean fine = spec.twistTurns != 0.0F
         || spec.spin != 0.0F
         || (path.count() > 2 || spec.shaper != null) && CoreClientConfig.CINEMATIC_QUALITY.get() == CoreClientConfig.CinematicQuality.ULTRA;
      float step = fine ? 2.0F : 4.0F;
      boolean faded = spec.color >>> 24 < 255;
      layer(
         pose,
         buffers.getBuffer(faded ? RenderType.entityTranslucent(texture) : RenderType.entityCutoutNoCull(texture)),
         texture,
         skin,
         path,
         n0,
         spec,
         step,
         overlayCoords,
         light0,
         light1,
         light2
      );
      if (overlay != null && spec.overlay) {
         layer(pose, buffers.getBuffer(RenderType.entityTranslucent(texture)), texture, overlay, path, n0, spec, step, overlayCoords, light0, light1, light2);
      }

      if (spec.swirl != null) {
         LimbUv.Layout tile = new LimbUv.Layout(
            16, 16, 1.0F, 0, 0, skin.width(), skin.depth(), skin.minX(), skin.minY(), skin.minZ(), skin.pivotX(), skin.pivotY(), skin.pivotZ(), 0.32F, false
         );
         SWIRL_SEGMENTS[0] = 0.0F;
         SWIRL_SEGMENTS[1] = path.length();
         SWIRL_SEGMENTS[2] = 0.0F;
         SWIRL_SEGMENTS[3] = path.length();
         MESH.sweep(path, SWIRL_SEGMENTS, 1, step, n0, spec, light0, light1, light2);
         MESH.vShift = -spec.swirlScroll;
         MESH.write(pose, buffers.getBuffer(RenderType.entityTranslucent(spec.swirl)), tile, overlayCoords, spec.swirlColor, null);
         MESH.vShift = 0.0F;
      }
   }

   private static void layer(
      Pose pose,
      VertexConsumer vc,
      ResourceLocation texture,
      LimbUv.Layout uv,
      LimbPath path,
      Vector3fc n0,
      LimbSpec spec,
      float step,
      int overlayCoords,
      int light0,
      int light1,
      int light2
   ) {
      int count = LimbBands.build(path.length(), uv.scale(), spec.fist, SkinFill.of(texture, uv), SEGMENTS);
      MESH.sweep(path, SEGMENTS, count, step, n0, spec, light0, light1, light2);
      MESH.write(pose, vc, uv, overlayCoords, spec.color, null);
   }

   static void drawPlayer(
      AbstractClientPlayer player,
      PoseStack pose,
      MultiBufferSource buffers,
      @Nullable PlayerModel<?> model,
      float partial,
      int bodyLight,
      ResourceLocation texture
   ) {
      List<Limbs.Active> limbs = Limbs.of(player);
      if (!limbs.isEmpty()) {
         int[] size = SkinSize.of(texture);
         LimbUv.Model type = player.getSkin().model() == Model.SLIM ? LimbUv.Model.SLIM : LimbUv.Model.CLASSIC;
         Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
         int overlayCoords = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
         pose.pushPose();
         pose.scale(0.0625F, 0.0625F, 0.0625F);
         LimbFrame f = FRAME;
         f.player = player;
         f.partial = partial;
         f.firstPerson = model == null;
         f.camX = cam.x;
         f.camY = cam.y;
         f.camZ = cam.z;
         f.toWorld.set(pose.last().pose());
         f.toWorld.invert(f.toPx);

         for (int i = 0; i < limbs.size(); i++) {
            Limbs.Active a = limbs.get(i);
            if (!a.done) {
               LimbSpec spec = a.spec;
               LimbUv.Layout skin = LimbUv.resolve(type, spec.side, spec.limb, size[0], size[1], LimbUv.Layer.SKIN);
               LimbUv.Layout overlay = spec.overlay && player.isModelPartShown(overlayPart(spec))
                  ? LimbUv.resolve(type, spec.side, spec.limb, size[0], size[1], LimbUv.Layer.OVERLAY)
                  : null;
               f.layout = skin;
               f.rootPosed = false;
               f.rootFree = false;
               if (model != null) {
                  ModelPart part = part(model, spec, false);
                  f.pivot.set(part.x, part.y, part.z);
                  f.posed.rotationZYX(part.zRot, part.yRot, part.xRot);
               } else {
                  boolean crouch = player.isCrouching();
                  f.pivot.set(skin.pivotX(), skin.pivotY() + (crouch && spec.limb == LimbUv.Limb.ARM ? 3.2F : 0.0F), skin.pivotZ() + 1.5F);
                  f.posed.identity();
               }

               PATH.reset().add(0.0F, 0.0F, 0.0F);
               if (!a.driver.path(f, PATH)) {
                  a.end();
               } else if (PATH.count() >= 2) {
                  root(f, skin);
                  if (f.rootFree) {
                     C.set(f.pivot);
                  } else {
                     C.set(skin.centreX(), skin.minY(), skin.centreZ());
                     ROOT.transform(C).add(f.pivot);
                  }

                  ROOT.transform(A.set(0.0F, 1.0F, 0.0F));
                  PATH.set(0, C.x, C.y, C.z).startDirection(A.x, A.y, A.z).build();
                  ROOT.transform(N0.set(1.0F, 0.0F, 0.0F));
                  int mid = light(f, PATH.length() * 0.5F, bodyLight);
                  int end = light(f, PATH.length(), bodyLight);
                  render(pose.last(), buffers, texture, skin, overlay, PATH, N0, spec, overlayCoords, bodyLight, mid, end);
               }
            }
         }

         pose.popPose();
      }
   }

   private static void root(LimbFrame f, LimbUv.Layout skin) {
      ROOT.set(f.posed);
      if (!f.rootPosed) {
         PATH.point(1, B);
         Vector3f posedAxis = f.posed.transform(A.set(0.0F, 1.0F, 0.0F));
         Vector3f from = C.set(f.pivot);

         for (int pass = 0; pass < (f.rootFree ? 1 : 2); pass++) {
            Vector3f dir = N0.set(B).sub(from);
            if (dir.lengthSquared() < 1.0E-4F) {
               return;
            }

            dir.normalize();
            TURN.rotationTo(posedAxis, dir);
            ROOT.set(TURN).mul(f.posed);
            from = ROOT.transform(C.set(skin.centreX(), skin.minY(), skin.centreZ())).add(f.pivot);
         }
      }
   }

   private static int light(LimbFrame f, float s, int fallback) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) {
         return fallback;
      } else {
         PATH.sample(s, A, null);
         f.toWorld.transformPosition(A);
         return LevelRenderer.getLightColor(level, BlockPos.containing((double)A.x + f.camX, (double)A.y + f.camY, (double)A.z + f.camZ));
      }
   }

   static ModelPart part(PlayerModel<?> model, LimbSpec spec, boolean overlay) {
      boolean right = spec.side == LimbUv.Side.RIGHT;
      if (spec.limb == LimbUv.Limb.ARM) {
         return overlay ? (right ? model.rightSleeve : model.leftSleeve) : (right ? model.rightArm : model.leftArm);
      } else {
         return overlay ? (right ? model.rightPants : model.leftPants) : (right ? model.rightLeg : model.leftLeg);
      }
   }

   private static PlayerModelPart overlayPart(LimbSpec spec) {
      boolean right = spec.side == LimbUv.Side.RIGHT;
      if (spec.limb == LimbUv.Limb.ARM) {
         return right ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE;
      } else {
         return right ? PlayerModelPart.RIGHT_PANTS_LEG : PlayerModelPart.LEFT_PANTS_LEG;
      }
   }

   private LimbRenderer() {
   }
}
