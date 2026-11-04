package com.devilfruits.fruits.yomi_yomi.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.animation.AnimationController;
import com.devilfruits.client.model.geo.GeoModel;
import com.devilfruits.client.model.geo.GeoPose;
import com.devilfruits.client.model.geo.GeoRenderer;
import com.devilfruits.client.model.geo.GeoResources;
import com.devilfruits.fruits.yomi_yomi.YomiItems;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class SoulGuitarRenderer extends BlockEntityWithoutLevelRenderer {
   private static final ResourceLocation MODEL = YomiYomi.id("soul_guitar");
   private static final ResourceLocation TEXTURE = DevilFruits.id("textures/entity/yomi_yomi/soul_guitar.png");
   private static final ResourceLocation HOLD = YomiYomi.id("guitar_hold");
   private static final float WORN = 0.024468085F;
   private static final float NECK_RISE = 28.0F;
   private static final float NECK_OUT = 25.0F;
   private static final float HIP_X = -2.0F;
   private static final float HIP_Y = 9.0F;
   private static final float HIP_Z = -3.6F;
   private static final Quaternionf ICON = new Quaternionf().rotateZ(0.61086524F).rotateY((float) Math.PI).rotateX((float) (-Math.PI / 18));
   private static final float FILL = 0.98F;
   private static SoulGuitarRenderer instance;
   private int generation = -1;
   @Nullable
   private GeoModel model;
   private GeoRenderer geo;
   private GeoPose pose;
   @Nullable
   private float[] fit;
   public static final AnimationController.PoseLayer POSE = (player, partialTick, out) -> {
      if (playing(player)) {
         out.add(HOLD, (float)player.tickCount + partialTick, 1.0F);
      }
   };

   public static SoulGuitarRenderer get() {
      if (instance == null) {
         Minecraft mc = Minecraft.getInstance();
         instance = new SoulGuitarRenderer(mc);
      }

      return instance;
   }

   private SoulGuitarRenderer(Minecraft mc) {
      super(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
   }

   public static boolean playing(Player player) {
      return player.getMainHandItem().is((Item)YomiItems.SOUL_GUITAR.get())
         && !YomiYomi.isSoul(player)
         && !player.isSleeping()
         && !player.isSwimming()
         && !player.isFallFlying();
   }

   public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
      if (this.ready()) {
         if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND && context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            ps.pushPose();
            if (context == ItemDisplayContext.GUI) {
               float[] f = this.fit();
               ps.translate(0.5F, 0.5F, 0.5F);
               ps.scale(f[3], f[3], f[3]);
               ps.translate(-f[0], -f[1], -f[2]);
               ps.mulPose(ICON);
            } else if (context.firstPerson()) {
               boolean right = context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
               ps.translate(right ? 0.3F : 0.7F, 0.38F, 0.5F);
               ps.mulPose(Axis.ZP.rotationDegrees(right ? -20.0F : 20.0F));
               ps.mulPose(Axis.XP.rotationDegrees(25.0F));
               ps.scale(0.012765958F, 0.012765958F, 0.012765958F);
               ps.translate(right ? -10.0F : 10.0F, 0.0F, 0.0F);
            } else {
               ps.translate(0.5F, 0.5F, 0.5F);
               ps.mulPose(Axis.YP.rotationDegrees(180.0F));
               ps.scale(0.019148936F, 0.019148936F, 0.019148936F);
            }

            this.draw(ps, buffers, light, overlay);
            ps.popPose();
         } else {
            if (SoulCaneRenderer.drawingEntity() instanceof Player p && playing(p) && p.getMainHandItem() == stack) {
               return;
            }

            ps.pushPose();
            ps.translate(0.5F, 0.35F, 0.5F);
            ps.mulPose(Axis.ZP.rotationDegrees(-60.0F));
            ps.scale(0.012765958F, 0.012765958F, 0.012765958F);
            this.draw(ps, buffers, light, overlay);
            ps.popPose();
         }
      }
   }

   private void draw(PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
      this.pose.reset();
      this.geo.pose(ps.last(), this.pose);
      this.geo.render(buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), this.pose, light, overlay, -1);
   }

   private boolean ready() {
      int gen = GeoResources.MODELS.generation();
      if (gen != this.generation) {
         this.generation = gen;
         this.model = GeoResources.model(MODEL);
         this.fit = null;
         if (this.model != null) {
            this.geo = new GeoRenderer(this.model);
            this.pose = new GeoPose(this.model);
         }
      }

      return this.model != null;
   }

   private float[] fit() {
      if (this.fit != null) {
         return this.fit;
      } else {
         float[] min = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
         float[] max = new float[]{-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
         Vector3f p = new Vector3f();

         for (GeoModel.Bone bone : this.model.bones) {
            for (int v = 0; v < bone.mesh.length; v += 8) {
               ICON.transform(p.set(bone.mesh[v], bone.mesh[v + 1], bone.mesh[v + 2]));
               min[0] = Math.min(min[0], p.x);
               min[1] = Math.min(min[1], p.y);
               min[2] = Math.min(min[2], p.z);
               max[0] = Math.max(max[0], p.x);
               max[1] = Math.max(max[1], p.y);
               max[2] = Math.max(max[2], p.z);
            }
         }

         float size = Math.max(1.0F, Math.max(max[0] - min[0], max[1] - min[1]));
         this.fit = new float[]{(min[0] + max[0]) / 2.0F, (min[1] + max[1]) / 2.0F, (min[2] + max[2]) / 2.0F, 0.98F / size};
         return this.fit;
      }
   }

   public static final class Layer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
      public Layer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
         super(parent);
      }

      public void render(
         PoseStack ps,
         MultiBufferSource buffers,
         int light,
         AbstractClientPlayer player,
         float limbSwing,
         float limbSwingAmount,
         float partialTick,
         float ageInTicks,
         float netHeadYaw,
         float headPitch
      ) {
         SoulGuitarRenderer r = SoulGuitarRenderer.get();
         if (SoulGuitarRenderer.playing(player) && !player.isInvisible() && r.ready()) {
            ps.pushPose();
            ((PlayerModel)this.getParentModel()).body.translateAndRotate(ps);
            ps.translate(-0.125F, 0.5625F, -0.225F);
            ps.mulPose(Axis.YP.rotationDegrees(25.0F));
            ps.mulPose(Axis.ZP.rotationDegrees(152.0F));
            ps.scale(0.024468085F, 0.024468085F, 0.024468085F);
            ps.translate(-10.0F, 0.0F, 0.0F);
            r.draw(ps, buffers, light, OverlayTexture.NO_OVERLAY);
            ps.popPose();
         }
      }
   }
}
