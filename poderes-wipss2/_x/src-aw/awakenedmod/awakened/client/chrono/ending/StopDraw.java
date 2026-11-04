package com.awakenedmod.awakened.client.chrono.ending;

import com.awakenedmod.awakened.client.render.ChronoGlass;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.world.phys.Vec3;

public record StopDraw(Pose pose, VertexConsumer vc, ChronoGlass.Layer layer, Vec3 cam, float partialTick) {
   public Vec3 rel(Vec3 world) {
      return world.subtract(this.cam);
   }
}
