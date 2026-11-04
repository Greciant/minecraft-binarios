package com.devilfruits.fruits.kage_kage.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.model.geo.GeoModel;
import com.devilfruits.client.model.geo.GeoPose;
import com.devilfruits.client.model.geo.GeoRenderer;
import com.devilfruits.client.model.geo.GeoResources;
import com.devilfruits.fruits.kage_kage.KageKage;
import com.devilfruits.fruits.kage_kage.item.MoriaBladeItem;
import com.devilfruits.fruits.kage_kage.item.MoriaScissorsItem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class ScissorsItemRenderer extends BlockEntityWithoutLevelRenderer {
   private static final ResourceLocation MODEL = KageKage.id("moria_scissors");
   private static final ResourceLocation TEXTURE = DevilFruits.id("textures/entity/kage_kage/moria_scissors.png");
   private static final Quaternionf TURN = new Quaternionf().rotateX((float) (Math.PI / 18)).rotateY((float) (-Math.PI / 6)).rotateZ((float) (-Math.PI / 4));
   private static final float FILL = 0.94F;
   private static ScissorsItemRenderer instance;
   private int generation = -1;
   @Nullable
   private GeoModel model;
   private GeoRenderer geo;
   private GeoPose pose;
   private final float[][] fits = new float[ScissorsItemRenderer.Variant.values().length][];

   static ScissorsItemRenderer get() {
      if (instance == null) {
         Minecraft mc = Minecraft.getInstance();
         instance = new ScissorsItemRenderer(mc);
      }

      return instance;
   }

   private ScissorsItemRenderer(Minecraft mc) {
      super(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
   }

   public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
      if (this.ready()) {
         ScissorsItemRenderer.Variant variant = stack.getItem() instanceof MoriaBladeItem
            ? ScissorsItemRenderer.Variant.BLADE_B
            : (
               stack.getItem() instanceof MoriaScissorsItem && MoriaScissorsItem.isSplit(stack)
                  ? ScissorsItemRenderer.Variant.BLADE_A
                  : ScissorsItemRenderer.Variant.JOINED
            );
         this.hide(variant);
         float[] fit = this.fit(variant);
         ps.pushPose();
         ps.translate(0.5F, 0.5F, 0.5F);
         ps.scale(fit[3], fit[3], fit[3]);
         ps.translate(-fit[0], -fit[1], -fit[2]);
         ps.mulPose(TURN);
         this.geo.pose(ps.last(), this.pose);
         this.geo.render(buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), this.pose, light, overlay, -1);
         ps.popPose();
      }
   }

   private boolean ready() {
      int gen = GeoResources.MODELS.generation();
      if (gen != this.generation) {
         this.generation = gen;
         this.model = GeoResources.model(MODEL);
         Arrays.fill(this.fits, null);
         if (this.model != null) {
            this.geo = new GeoRenderer(this.model);
            this.pose = new GeoPose(this.model);
         }
      }

      return this.model != null;
   }

   private void hide(ScissorsItemRenderer.Variant variant) {
      this.pose.reset();
      int a = this.model.bone("blade_a");
      int b = this.model.bone("blade_b");
      if (variant == ScissorsItemRenderer.Variant.BLADE_A && b >= 0) {
         this.pose.hidden[b] = true;
      } else if (variant == ScissorsItemRenderer.Variant.BLADE_B && a >= 0) {
         this.pose.hidden[a] = true;
      }
   }

   private float[] fit(ScissorsItemRenderer.Variant variant) {
      float[] fit = this.fits[variant.ordinal()];
      if (fit != null) {
         return fit;
      } else {
         float[] min = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
         float[] max = new float[]{-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
         Vector3f p = new Vector3f();

         for (GeoModel.Bone bone : this.model.bones) {
            if (!this.pose.hidden[bone.index]) {
               for (int v = 0; v < bone.mesh.length; v += 8) {
                  TURN.transform(p.set(bone.mesh[v], bone.mesh[v + 1], bone.mesh[v + 2]));
                  min[0] = Math.min(min[0], p.x);
                  min[1] = Math.min(min[1], p.y);
                  min[2] = Math.min(min[2], p.z);
                  max[0] = Math.max(max[0], p.x);
                  max[1] = Math.max(max[1], p.y);
                  max[2] = Math.max(max[2], p.z);
               }
            }
         }

         float size = Math.max(1.0F, Math.max(max[0] - min[0], max[1] - min[1]));
         fit = new float[]{(min[0] + max[0]) / 2.0F, (min[1] + max[1]) / 2.0F, (min[2] + max[2]) / 2.0F, 0.94F / size};
         this.fits[variant.ordinal()] = fit;
         return fit;
      }
   }

   private static enum Variant {
      JOINED,
      BLADE_A,
      BLADE_B;
   }
}
