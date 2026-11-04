package com.devilfruits.fruits.yomi_yomi.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.model.geo.GeoAnimation;
import com.devilfruits.client.model.geo.GeoModel;
import com.devilfruits.client.model.geo.GeoPose;
import com.devilfruits.client.model.geo.GeoRenderer;
import com.devilfruits.client.model.geo.GeoResources;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

public final class YomiModel {
   private final String name;
   private final ResourceLocation texture;
   private int generation = -1;
   @Nullable
   private GeoModel model;
   private GeoRenderer geo;
   private GeoPose pose;
   private final Map<String, GeoAnimation> clips = new HashMap<>();

   public YomiModel(String name) {
      this.name = name;
      this.texture = DevilFruits.id("textures/entity/yomi_yomi/" + name + ".png");
   }

   public ResourceLocation texture() {
      return this.texture;
   }

   public boolean ready() {
      int gen = GeoResources.MODELS.generation();
      if (gen != this.generation) {
         this.generation = gen;
         this.clips.clear();
         this.model = GeoResources.model(YomiYomi.id(this.name));
         if (this.model != null) {
            this.geo = new GeoRenderer(this.model);
            this.pose = new GeoPose(this.model);
         }
      }

      return this.model != null;
   }

   @Nullable
   public GeoAnimation clip(String clip) {
      return this.clips.computeIfAbsent(clip, c -> GeoResources.animation(YomiYomi.id(this.name + "/" + c)));
   }

   public float length(String clip) {
      GeoAnimation a = this.clip(clip);
      return a == null ? 1.0F : Math.max(1.0F, a.length());
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

   public void blend(String clip, float time, float weight) {
      GeoAnimation a = this.clip(clip);
      if (a != null && weight > 0.001F) {
         a.blend(this.model, this.pose, Math.min(time, Math.max(1.0F, a.length())), weight);
      }
   }

   public void pivots(Pose base, int[] bones, Vector3f[] out) {
      this.geo.pose(base, this.pose);

      for (int i = 0; i < bones.length; i++) {
         if (bones[i] >= 0) {
            GeoModel.Bone b = this.model.bones[bones[i]];
            this.geo.boneMatrix(bones[i]).transformPosition(b.pivotX, b.pivotY, b.pivotZ, out[i]);
         }
      }
   }

   public void draw(PoseStack ps, MultiBufferSource buffers, int light) {
      this.geo.pose(ps.last(), this.pose);
      this.geo.render(buffers.getBuffer(RenderType.entityCutoutNoCull(this.texture)), this.pose, light, OverlayTexture.NO_OVERLAY, -1);
   }

   public void drawGhost(PoseStack ps, MultiBufferSource buffers, float alpha) {
      this.geo.pose(ps.last(), this.pose);
      if (buffers instanceof BufferSource source) {
         RenderType depth = YomiModel.Types.depth(this.texture);
         this.geo.render(source.getBuffer(depth), this.pose, 15728880, OverlayTexture.NO_OVERLAY, -1);
         source.endBatch(depth);
      }

      int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
      this.geo.render(buffers.getBuffer(YomiModel.Types.ghost(this.texture)), this.pose, 15728880, OverlayTexture.NO_OVERLAY, a << 24 | 16777215);
   }

   public void drawWith(PoseStack ps, BufferSource buffers, RenderType type) {
      this.geo.pose(ps.last(), this.pose);
      this.geo.render(buffers.getBuffer(type), this.pose, 15728880, OverlayTexture.NO_OVERLAY, -1);
      buffers.endBatch(type);
   }

   static final class Types extends RenderType {
      private static final Map<ResourceLocation, RenderType> DEPTH = new HashMap<>();
      private static final Map<ResourceLocation, RenderType> GHOST = new HashMap<>();

      private Types() {
         super("", DefaultVertexFormat.POSITION, Mode.QUADS, 0, false, false, () -> {
         }, () -> {
         });
      }

      static RenderType depth(ResourceLocation texture) {
         return DEPTH.computeIfAbsent(
            texture,
            t -> RenderType.create(
                  "devilfruits_yomi_ghost_depth",
                  DefaultVertexFormat.NEW_ENTITY,
                  Mode.QUADS,
                  16384,
                  false,
                  false,
                  CompositeState.builder()
                     .setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
                     .setTextureState(new TextureStateShard(t, false, false))
                     .setTransparencyState(NO_TRANSPARENCY)
                     .setCullState(CULL)
                     .setLightmapState(LIGHTMAP)
                     .setOverlayState(OVERLAY)
                     .setWriteMaskState(DEPTH_WRITE)
                     .createCompositeState(false)
               )
         );
      }

      static RenderType ghost(ResourceLocation texture) {
         return GHOST.computeIfAbsent(
            texture,
            t -> RenderType.create(
                  "devilfruits_yomi_ghost",
                  DefaultVertexFormat.NEW_ENTITY,
                  Mode.QUADS,
                  16384,
                  false,
                  true,
                  CompositeState.builder()
                     .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                     .setTextureState(new TextureStateShard(t, false, false))
                     .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                     .setCullState(CULL)
                     .setOverlayState(OVERLAY)
                     .setDepthTestState(LEQUAL_DEPTH_TEST)
                     .setWriteMaskState(COLOR_WRITE)
                     .createCompositeState(false)
               )
         );
      }
   }
}
