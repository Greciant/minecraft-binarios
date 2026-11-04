package com.devilfruits.fruits.kage_kage.client.vfx;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.model.geo.GeoAnimation;
import com.devilfruits.client.model.geo.GeoModel;
import com.devilfruits.client.model.geo.GeoPose;
import com.devilfruits.client.model.geo.GeoRenderer;
import com.devilfruits.client.model.geo.GeoResources;
import com.devilfruits.fruits.kage_kage.KageKage;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class KageModel {
   private final String name;
   private final ResourceLocation texture;
   private final ResourceLocation emissive;
   private final String[] rimBoneNames;
   private boolean silhouette;
   private int generation = -1;
   @Nullable
   private GeoModel model;
   private GeoRenderer geo;
   private GeoPose pose;
   private int[] rimBones = new int[0];
   private final Map<String, GeoAnimation> clips = new HashMap<>();

   public KageModel(String name, String... rimBoneNames) {
      this.name = name;
      this.texture = DevilFruits.id("textures/entity/kage_kage/" + name + ".png");
      this.emissive = DevilFruits.id("textures/entity/kage_kage/" + name + "_emissive.png");
      this.rimBoneNames = rimBoneNames;
   }

   public KageModel silhouetteRim() {
      this.silhouette = true;
      return this;
   }

   public boolean ready() {
      int gen = GeoResources.MODELS.generation();
      if (gen != this.generation) {
         this.generation = gen;
         this.clips.clear();
         this.model = GeoResources.model(KageKage.id(this.name));
         if (this.model != null) {
            this.geo = new GeoRenderer(this.model);
            this.pose = new GeoPose(this.model);
            List<Integer> bones = new ArrayList<>();
            if (this.rimBoneNames.length == 0) {
               for (GeoModel.Bone b : this.model.bones) {
                  bones.add(b.index);
               }
            } else {
               for (String bone : this.rimBoneNames) {
                  int b = this.model.bone(bone);
                  if (b >= 0) {
                     bones.add(b);
                  }
               }
            }

            this.rimBones = bones.stream().mapToInt(Integer::intValue).toArray();
         }
      }

      return this.model != null;
   }

   @Nullable
   public GeoAnimation clip(String clip) {
      return this.clips.computeIfAbsent(clip, c -> GeoResources.animation(KageKage.id(this.name + "/" + c)));
   }

   public GeoPose pose() {
      this.pose.reset();
      return this.pose;
   }

   public int bone(String bone) {
      return this.model == null ? -1 : this.model.bone(bone);
   }

   public void play(String clip, float time, boolean loop, float weight) {
      GeoAnimation a = this.clip(clip);
      if (a != null && weight > 0.001F) {
         float len = Math.max(1.0F, a.length());
         a.add(this.model, this.pose, loop ? (time % len + len) % len : Math.min(time, len), weight);
      }
   }

   public void play(String clip, float time, boolean loop) {
      GeoAnimation a = this.clip(clip);
      if (a != null) {
         float len = Math.max(1.0F, a.length());
         a.add(this.model, this.pose, loop ? (time % len + len) % len : Math.min(time, len), 1.0F);
      }
   }

   public void draw(boolean glow, PoseStack ps, MultiBufferSource buffers, int light, float alpha, float heat, float rimThickness, RimConsumer rim) {
      this.draw(glow, ps, buffers, light, OverlayTexture.NO_OVERLAY, alpha, heat, rimThickness, rim);
   }

   public void drawSkin(PoseStack ps, MultiBufferSource buffers, ResourceLocation skin, int light, int overlay) {
      this.geo.pose(ps.last(), this.pose);
      this.geo.render(buffers.getBuffer(RenderType.entityCutoutNoCull(skin)), this.pose, light, overlay, -1);
   }

   public void draw(boolean glow, PoseStack ps, MultiBufferSource buffers, int light, int overlay, float alpha, float heat, float rimThickness, RimConsumer rim) {
      this.geo.pose(ps.last(), this.pose);
      if (!glow) {
         this.geo.render(buffers.getBuffer(RenderType.entityTranslucent(this.texture)), this.pose, light, overlay, (int)(alpha * 255.0F) << 24 | 16777215);
      } else {
         int em = 0xFF000000 | KageLook.scale(16777215, alpha * Math.max(0.0F, Math.min(1.6F, heat)));
         this.geo.render(buffers.getBuffer(RenderType.eyes(this.emissive)), this.pose, 15728880, OverlayTexture.NO_OVERLAY, em);
         if (this.silhouette) {
            if (rimThickness > 0.0F && buffers instanceof BufferSource source && SilhouetteRim.ready()) {
               RenderType mask = SilhouetteRim.mask(this.texture);
               this.geo.render(source.getBuffer(mask), this.pose, light, overlay, -1);
               source.endBatch(mask);
               RenderType outline = SilhouetteRim.outline();
               float h = Math.max(1.0F, heat);
               rim.to(source.getBuffer(outline), rimThickness * h, KageLook.scale(KageLook.rim(h), alpha), 1.0F);

               for (int b : this.rimBones) {
                  this.geo.renderBone(rim, this.model.bones[b], 15728880, OverlayTexture.NO_OVERLAY, -1);
               }

               source.endBatch(outline);
            }
         } else {
            if (rimThickness > 0.0F) {
               rim.to(
                  buffers.getBuffer(RenderType.eyes(KageLook.white())), rimThickness * Math.max(1.0F, heat), KageLook.scale(KageLook.rim(heat), alpha), 1.0F
               );

               for (int b : this.rimBones) {
                  this.geo.renderBone(rim, this.model.bones[b], 15728880, OverlayTexture.NO_OVERLAY, -1);
               }
            }
         }
      }
   }
}
