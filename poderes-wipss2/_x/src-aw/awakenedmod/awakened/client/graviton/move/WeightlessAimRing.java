package com.awakenedmod.awakened.client.graviton.move;

import com.awakenedmod.awakened.client.OwnAbilityKey;
import com.awakenedmod.awakened.client.render.FxLayer;
import com.awakenedmod.awakened.client.render.FxLayers;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Lab;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;

@EventBusSubscriber(
   modid = "awakened",
   value = {Dist.CLIENT}
)
public final class WeightlessAimRing {
   private static final int SEGMENTS = 64;
   private static final float BAND = 0.09F;
   private static final float EDGE = 0.03F;
   private static final float LIFT = 0.03F;
   private static final float PALE_ALPHA = 0.4F;
   private static final float FILL_ALPHA = 0.07F;
   private static final float DARK_ALPHA = 0.35F;
   private static final Lab.Value FORCE_PREVIEW = Lab.CLIENT.def("graviton.move.force_preview", 0.0, 0.0, 1.0, 1.0);
   private static final int NO_SPOT_REPEAT = 30;
   private static Vec3 previewAt;
   private static int weightlessHeld;
   private static int noSpotCooldown;

   private WeightlessAimRing() {
   }

   @SubscribeEvent
   static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      previewAt = null;
      if (player != null && mc.level != null) {
         tickPreview(mc, player);
      } else {
         weightlessHeld = 0;
      }
   }

   private static void tickPreview(Minecraft mc, LocalPlayer player) {
      if (noSpotCooldown > 0) {
         noSpotCooldown--;
      }

      if (!OwnAbilityKey.held("weightless") && FORCE_PREVIEW.get() < 0.5) {
         weightlessHeld = 0;
      } else if (++weightlessHeld >= 6) {
         previewAt = weightlessAim(mc.level, player);
         if (previewAt == null && noSpotCooldown == 0) {
            player.displayClientMessage(Component.translatable("awakened.graviton.no_spot"), true);
            noSpotCooldown = 30;
         }
      }
   }

   private static Vec3 weightlessAim(ClientLevel level, LocalPlayer player) {
      LivingEntity foe = AimUtil.aimedLivingSoft(player, level, 24.0, 0.0, e -> e != player && !(e instanceof ArmorStand));
      if (foe != null) {
         return foe.position();
      } else {
         HitResult hit = player.pick(24.0, 1.0F, false);
         Vec3 loc = hit.getLocation();
         if (hit.getType() == Type.BLOCK) {
            return loc;
         } else {
            MutableBlockPos pos = BlockPos.containing(loc).mutable();

            for (int i = 0; i < 6; i++) {
               if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                  return new Vec3(loc.x, (double)pos.getY() + 1.0, loc.z);
               }

               pos.move(0, -1, 0);
            }

            return null;
         }
      }
   }

   @SubscribeEvent
   static void onRender(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS) {
         draw(event.getPoseStack(), event.getCamera().getPosition());
      }
   }

   private static void draw(PoseStack poseStack, Vec3 cam) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && previewAt != null) {
         BufferSource buffers = mc.renderBuffers().bufferSource();
         poseStack.pushPose();
         poseStack.translate(-cam.x, -cam.y, -cam.z);
         Pose pose = poseStack.last();
         VertexConsumer vc = FxLayers.begin(buffers, FxLayer.PAINT);
         ring(pose, vc, mc.level, previewAt, 5.0F, 16769732, false, true);
         FxLayers.end(buffers, FxLayer.PAINT);
         poseStack.popPose();
      }
   }

   private static void ring(Pose pose, VertexConsumer vc, ClientLevel level, Vec3 c, float r, int rgb, boolean dashed, boolean fill) {
      float[] ys = new float[65];
      float[] cs = new float[65];
      float[] sn = new float[65];

      for (int i = 0; i <= 64; i++) {
         float a = (float)((Math.PI * 2) * (double)i / 64.0);
         cs[i] = Mth.cos(a);
         sn[i] = Mth.sin(a);
         ys[i] = groundY(level, c.x + (double)(cs[i] * r), c.z + (double)(sn[i] * r), c.y) + 0.03F;
      }

      float cy = groundY(level, c.x, c.z, c.y) + 0.03F;
      int dark = argb(0.35F, 1971474);
      int pale = argb(0.4F, rgb);

      for (int i = 0; i < 64; i++) {
         if (fill) {
            int f = argb(0.07F, rgb);
            vertex(pose, vc, c.x, (double)cy, c.z, f);
            vertex(pose, vc, c.x + (double)(cs[i] * (r - 0.09F)), (double)ys[i], c.z + (double)(sn[i] * (r - 0.09F)), f);
            vertex(pose, vc, c.x + (double)(cs[i + 1] * (r - 0.09F)), (double)ys[i + 1], c.z + (double)(sn[i + 1] * (r - 0.09F)), f);
         }

         if (!dashed || i % 2 != 1) {
            band(pose, vc, c, cs, sn, ys, i, r - 0.09F - 0.03F, r - 0.09F, dark);
            band(pose, vc, c, cs, sn, ys, i, r - 0.09F, r + 0.09F, pale);
            band(pose, vc, c, cs, sn, ys, i, r + 0.09F, r + 0.09F + 0.03F, dark);
         }
      }
   }

   private static void band(Pose pose, VertexConsumer vc, Vec3 c, float[] cs, float[] sn, float[] ys, int i, float r0, float r1, int color) {
      int j = i + 1;
      double ax = c.x + (double)(cs[i] * r0);
      double az = c.z + (double)(sn[i] * r0);
      double bx = c.x + (double)(cs[i] * r1);
      double bz = c.z + (double)(sn[i] * r1);
      double cx = c.x + (double)(cs[j] * r1);
      double cz = c.z + (double)(sn[j] * r1);
      double dx = c.x + (double)(cs[j] * r0);
      double dz = c.z + (double)(sn[j] * r0);
      vertex(pose, vc, ax, (double)ys[i], az, color);
      vertex(pose, vc, bx, (double)ys[i], bz, color);
      vertex(pose, vc, cx, (double)ys[j], cz, color);
      vertex(pose, vc, ax, (double)ys[i], az, color);
      vertex(pose, vc, cx, (double)ys[j], cz, color);
      vertex(pose, vc, dx, (double)ys[j], dz, color);
   }

   private static void vertex(Pose pose, VertexConsumer vc, double x, double y, double z, int color) {
      vc.addVertex(pose, (float)x, (float)y, (float)z).setColor(color);
   }

   private static int argb(float alpha, int rgb) {
      return Mth.clamp(Math.round(alpha * 255.0F), 0, 255) << 24 | rgb & 16777215;
   }

   private static float groundY(ClientLevel level, double x, double z, double nearY) {
      MutableBlockPos pos = BlockPos.containing(x, nearY + 2.0, z).mutable();

      for (int i = 0; i < 6; i++) {
         BlockState state = level.getBlockState(pos);
         if (!state.isAir()) {
            VoxelShape shape = state.getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
               return (float)((double)pos.getY() + shape.max(Axis.Y));
            }
         }

         pos.move(0, -1, 0);
      }

      return (float)nearY;
   }
}
