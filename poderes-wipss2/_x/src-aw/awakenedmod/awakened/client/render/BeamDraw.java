package com.awakenedmod.awakened.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class BeamDraw {
   private BeamDraw() {
   }

   public static Vector3f vec(Vec3 v) {
      return new Vector3f((float)v.x, (float)v.y, (float)v.z);
   }

   public static void beamSegment(Pose pose, VertexConsumer vc, Vector3f a, Vector3f b, float w, int color) {
      Vector3f axis = new Vector3f(b).sub(a);
      if (!(axis.lengthSquared() < 1.0E-8F)) {
         axis.normalize();
         Vector3f u = Math.abs(axis.y) > 0.95F ? new Vector3f(1.0F, 0.0F, 0.0F) : new Vector3f(0.0F, 1.0F, 0.0F);
         Vector3f perp1 = new Vector3f(axis).cross(u).normalize().mul(w);
         Vector3f perp2 = new Vector3f(axis).cross(perp1).normalize().mul(w);
         crossQuad(pose, vc, a, b, perp1, color);
         crossQuad(pose, vc, a, b, perp2, color);
      }
   }

   public static void crossQuad(Pose pose, VertexConsumer vc, Vector3f a, Vector3f b, Vector3f perp, int color) {
      Vector3f a0 = new Vector3f(a).add(perp);
      Vector3f a1 = new Vector3f(a).sub(perp);
      Vector3f b0 = new Vector3f(b).add(perp);
      Vector3f b1 = new Vector3f(b).sub(perp);
      tri(pose, vc, a0, b0, b1, color);
      tri(pose, vc, a0, b1, a1, color);
      tri(pose, vc, a0, b1, b0, color);
      tri(pose, vc, a0, a1, b1, color);
   }

   public static void tri(Pose pose, VertexConsumer vc, Vector3f a, Vector3f b, Vector3f c, int color) {
      vc.addVertex(pose, a.x, a.y, a.z).setColor(color);
      vc.addVertex(pose, b.x, b.y, b.z).setColor(color);
      vc.addVertex(pose, c.x, c.y, c.z).setColor(color);
   }
}
