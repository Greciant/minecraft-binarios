package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.content.hydro.HydroJetFx;
import com.awakenedmod.awakened.content.vfx.VfxEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class WaterJetComposites {
   static final float FP_HAND_SCALE = 0.45F;
   private static final float SPLAT_RADIUS = 0.5F;
   private static final float SPLAT_CUBE = 0.16F;
   private static final float SPLAT_DEPTH = 0.1F;
   private static final int SPLAT_CUBES = 8;

   private WaterJetComposites() {
   }

   public static void register() {
      VfxComposites.register(VfxEntity.Kind.WATER_GEYSER, GeyserJet::draw);
      VfxComposites.register(VfxEntity.Kind.WATER_TENDRIL, WaterJetComposites::tendril);
   }

   private static void tendril(PoseStack ps, VfxDraw.Sink sink, VfxRenderState st, float life) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.level.getEntity(VfxEntity.idFromSecondary(st.secondary.x)) instanceof Player caster) {
         float age = st.ageInTicks;
         float pt = age - (float)Mth.floor(age);
         Vec3 hand = HydroHands.hand(caster, pt, castLift(age), HydroHands.mainArmLeft(caster));
         Vec3 rel = hand.subtract(st.worldPos);
         float fade = life > 0.7F ? Math.max(0.0F, (1.0F - life) / 0.3F) : 1.0F;
         WaterMesh.Paint water = WaterMesh.Paint.of(st.color, 0.8F * st.intensity * fade, st.lightCoords);
         if (!(water.opacity() < 0.12F)) {
            float reach = Math.min(1.0F, (age + 0.3F) / 2.2F);
            Vector3f from = new Vector3f((float)rel.x, (float)rel.y, (float)rel.z);
            WaterTendril.Strain strain = WaterTendril.Strain.taut(new Vector3f(), HydroHands.ownFirstPerson(caster) ? 0.45F : 1.0F);
            Vec3 normal = HydroJetFx.unpackNormal(st.secondary.y);
            if (reach >= 1.0F) {
               WaterCubeSpray.burstOnce(
                  mc.level,
                  "tendril" + st.seed,
                  st.worldPos,
                  normal.scale(0.12),
                  WaterCubeSpray.Burst.of(8, 0.07F, 0.192F).lifted(0.1F).lasting(16),
                  new WaterCubeSpray.Look(st.color, water.opacity(), water.opacity() * 0.3F)
               );
            }

            float spread = Mth.clamp((age - 1.5F) / 3.0F, 0.0F, 1.0F);
            WaterFramePass.queue(ps, (pose, vc) -> {
               WaterTendril.draw(pose, vc, water, from, new Vector3f(), st.scale, reach, age, st.seed, strain);
               if (spread > 0.0F) {
                  splat(pose, vc, water, normal, spread, st.seed);
               }
            });
         }
      }
   }

   private static void splat(Pose pose, VertexConsumer vc, WaterMesh.Paint water, Vec3 normal, float spread, int seed) {
      Pose p = pose.copy();
      Quaternionf q = new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, (float)normal.x, (float)normal.y, (float)normal.z);
      p.pose().rotate(q);
      p.normal().rotate(q);
      p.pose().translate(0.0F, -0.09F, 0.0F);
      WaterCubeGrid g = WaterBlocks.puddle(0.5F * (0.4F + 0.6F * spread), 0.16F, 0.1F, 0.45F, seed);
      g.drawWater(p, vc, water, true);
      WaterMesh.Paint sheen = water.sheen(water.opacity() * 0.3F);
      WaterFramePass.foam(p, vc, (fp, fvc) -> g.drawFoam(fp, fvc, sheen, (k, r, h) -> 0.5F));
   }

   private static float castLift(float age) {
      if (age < 1.6F) {
         return Mth.lerp(age / 1.6F, -90.0F, 10.0F);
      } else if (age < 6.0F) {
         return Mth.lerp((age - 1.6F) / 4.4F, 10.0F, 4.0F);
      } else {
         return age < 14.0F ? Mth.lerp((age - 6.0F) / 8.0F, 4.0F, 2.0F) : Mth.lerp(AnimEnvelopes.smooth((age - 14.0F) / 5.0F), 2.0F, -90.0F);
      }
   }
}
