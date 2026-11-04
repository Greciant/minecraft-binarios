package com.devilfruits.fruits.doru_doru.client.render;

import com.devilfruits.fruits.doru_doru.DoruItems;
import com.devilfruits.fruits.doru_doru.entity.WaxLanceEntity;
import com.devilfruits.fruits.doru_doru.item.WaxTools;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;

public class WaxLanceRenderer extends EntityRenderer<WaxLanceEntity> {
   private static final float SCALE = 1.8F;

   public WaxLanceRenderer(Context context) {
      super(context);
   }

   public void render(WaxLanceEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
      Vec3 v = e.getDeltaMovement();
      if (v.lengthSqr() < 1.0E-6) {
         v = new Vec3(0.0, 0.0, 1.0);
      }

      float ry = (float)(Mth.atan2(v.x, v.z) * 180.0F / (float)Math.PI);
      float rp = (float)(Mth.atan2(v.y, Math.sqrt(v.x * v.x + v.z * v.z)) * 180.0F / (float)Math.PI);
      pose.pushPose();
      pose.mulPose(Axis.YP.rotationDegrees(ry - 90.0F));
      pose.mulPose(Axis.ZP.rotationDegrees(rp));
      pose.scale(1.8F, 1.8F, 1.8F);
      pose.translate(-0.45, 0.0, 0.0);
      ItemStack stack = new ItemStack((ItemLike)DoruItems.WAX_LANCE.get());
      stack.set((DataComponentType)DoruItems.BOUND.get(), new WaxTools.Bound(ItemStack.EMPTY, -1, e.color()));
      Minecraft.getInstance()
         .getItemRenderer()
         .renderStatic(stack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, pose, buffers, e.level(), e.getId());
      pose.popPose();
      super.render(e, yaw, partial, pose, buffers, light);
   }

   public ResourceLocation getTextureLocation(WaxLanceEntity e) {
      return InventoryMenu.BLOCK_ATLAS;
   }
}
