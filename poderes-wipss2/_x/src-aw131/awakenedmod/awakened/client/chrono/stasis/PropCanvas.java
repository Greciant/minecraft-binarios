package com.awakenedmod.awakened.client.chrono.stasis;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;

public final class PropCanvas {
   private final PoseStack pose;
   private final PropCanvas.BlockSink sink;
   private final int light;

   public PropCanvas(PoseStack pose, PropCanvas.BlockSink sink, int light) {
      this.pose = pose;
      this.sink = sink;
      this.light = light;
   }

   public PoseStack pose() {
      return this.pose;
   }

   public void bodyFrame(float bodyYawDegrees) {
      this.pose.mulPose(new Quaternionf().rotationY(-bodyYawDegrees * (float) (Math.PI / 180.0)));
   }

   public void box(BlockState state, float x0, float y0, float z0, float x1, float y1, float z1) {
      float sx = x1 - x0;
      float sy = y1 - y0;
      float sz = z1 - z0;
      if (!(sx <= 0.0F) && !(sy <= 0.0F) && !(sz <= 0.0F)) {
         this.pose.pushPose();
         this.pose.translate(x0, y0, z0);
         this.pose.scale(sx, sy, sz);
         this.sink.block(this.pose, state, this.light);
         this.pose.popPose();
      }
   }

   public void column(BlockState state, float cx, float cz, float halfX, float halfZ, float y0, float y1) {
      this.box(state, cx - halfX, y0, cz - halfZ, cx + halfX, y1, cz + halfZ);
   }

   @FunctionalInterface
   public interface BlockSink {
      void block(PoseStack var1, BlockState var2, int var3);
   }
}
