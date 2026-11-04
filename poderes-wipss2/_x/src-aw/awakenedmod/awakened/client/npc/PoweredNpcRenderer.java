package com.awakenedmod.awakened.client.npc;

import com.awakenedmod.awakened.core.npc.NpcKit;
import com.awakenedmod.awakened.core.npc.PoweredNpc;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class PoweredNpcRenderer extends HumanoidMobRenderer<PoweredNpc, PlayerModel<PoweredNpc>> {
   private static final double NAME_RANGE = 24.0;
   private static final double LOOK_DOT = 0.995;
   private final NpcKit kit;
   private final NpcKitVisuals visuals;

   public PoweredNpcRenderer(Context ctx, NpcKit kit, NpcKitVisuals visuals) {
      super(ctx, new NpcPlayerModel(ctx.bakeLayer(ModelLayers.PLAYER), visuals), 0.5F);
      this.kit = kit;
      this.visuals = visuals;
      this.addLayer(
         new HumanoidArmorLayer(
            this,
            new HumanoidArmorModel(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
            new HumanoidArmorModel(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
            ctx.getModelManager()
         )
      );
      visuals.addLayers(this);
   }

   private boolean nameInSight(PoweredNpc npc, double distanceSq) {
      LocalPlayer viewer = Minecraft.getInstance().player;
      if (viewer != null && npc != this.entityRenderDispatcher.crosshairPickEntity) {
         boolean near = distanceSq < 576.0;
         if (!near) {
            Vec3 to = npc.getBoundingBox().getCenter().subtract(viewer.getEyePosition());
            double len = to.length();
            if (len < 0.001 || viewer.getViewVector(1.0F).dot(to) / len < 0.995) {
               return false;
            }
         }

         return viewer.hasLineOfSight(npc);
      } else {
         return viewer != null;
      }
   }

   protected boolean shouldShowName(PoweredNpc npc) {
      return super.shouldShowName(npc) && this.nameInSight(npc, this.entityRenderDispatcher.distanceToSqr(npc));
   }

   public ResourceLocation getTextureLocation(PoweredNpc npc) {
      return this.kit.skin();
   }

   protected void setupRotations(PoweredNpc npc, PoseStack poseStack, float bob, float bodyRot, float partialTick, float scale) {
      super.setupRotations(npc, poseStack, bob, bodyRot, partialTick, scale);
      NpcPose.applyRoot(poseStack, NpcPose.frame(npc, this.visuals, partialTick));
   }
}
