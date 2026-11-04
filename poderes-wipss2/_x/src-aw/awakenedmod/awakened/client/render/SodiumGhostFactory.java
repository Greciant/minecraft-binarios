package com.awakenedmod.awakened.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

final class SodiumGhostFactory {
   private SodiumGhostFactory() {
   }

   static GhostVertexConsumer create(VertexConsumer inner, float alpha, float red, float green, float blue) {
      return new SodiumGhostVertexConsumer(inner, alpha, red, green, blue);
   }
}
