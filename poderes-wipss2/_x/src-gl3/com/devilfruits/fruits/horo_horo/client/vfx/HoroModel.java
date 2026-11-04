package com.devilfruits.fruits.horo_horo.client.vfx;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.model.geo.GeoAnimation;
import com.devilfruits.client.model.geo.GeoModel;
import com.devilfruits.client.model.geo.GeoPose;
import com.devilfruits.client.model.geo.GeoRenderer;
import com.devilfruits.client.model.geo.GeoResources;
import com.devilfruits.fruits.horo_horo.HoroHoro;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
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

public final class HoroModel {
   public static final HoroModel LONG = new HoroModel("hollow_long");
   public static final HoroModel MINI = new HoroModel("hollow_mini");
   public static final HoroModel GIANT = new HoroModel("hollow_giant");
   private final String name;
   private final ResourceLocation texture;
   private int generation = -1;
   @Nullable
   private GeoModel model;
   private GeoRenderer geo;
   private GeoPose pose;
   private final Map<String, GeoAnimation> clips = new HashMap<>();

   private HoroModel(String name) {
      this.name = name;
      this.texture = DevilFruits.id("textures/entity/horo_horo/" + name + ".png");
   }

   public boolean ready() {
      int gen = GeoResources.MODELS.generation();
      if (gen != this.generation) {
         this.generation = gen;
         this.clips.clear();
         this.model = GeoResources.model(HoroHoro.id(this.name));
         if (this.model != null) {
            this.geo = new GeoRenderer(this.model);
            this.pose = new GeoPose(this.model);
         }
      }

      return this.model != null;
   }

   @Nullable
   private GeoAnimation clip(String clip) {
      return this.clips.computeIfAbsent(clip, c -> GeoResources.animation(HoroHoro.id(this.name + "/" + c)));
   }

   public float length(String clip) {
      GeoAnimation a = this.clip(clip);
      return a == null ? 1.0F : Math.max(1.0F, a.length());
   }

   public GeoPose pose() {
      this.pose.reset();
      return this.pose;
   }

   public void play(String clip, float time, boolean loop, float weight) {
      GeoAnimation a = this.clip(clip);
      if (a != null && weight > 0.001F) {
         float len = Math.max(1.0F, a.length());
         a.add(this.model, this.pose, loop ? (time % len + len) % len : Math.min(time, len), weight);
      }
   }

   public void drawGhost(PoseStack ps, MultiBufferSource buffers, float alpha) {
      this.geo.pose(ps.last(), this.pose);
      if (buffers instanceof BufferSource source) {
         RenderType depth = HoroModel.Types.depth(this.texture);
         this.geo.render(source.getBuffer(depth), this.pose, 15728880, OverlayTexture.NO_OVERLAY, -1);
         source.endBatch(depth);
      }

      int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
      RenderType colour = HoroModel.Types.ghost(this.texture);
      this.geo.render(buffers.getBuffer(colour), this.pose, 15728880, OverlayTexture.NO_OVERLAY, a << 24 | 16777215);
      if (buffers instanceof BufferSource source) {
         source.endBatch(colour);
      }
   }

   public static final class Types extends RenderType {
      private static final Map<ResourceLocation, RenderType> DEPTH = new HashMap<>();
      private static final Map<ResourceLocation, RenderType> GHOST = new HashMap<>();

      private Types() {
         super("", DefaultVertexFormat.POSITION, Mode.QUADS, 0, false, false, () -> {
         }, () -> {
         });
      }

      public static RenderType depth(ResourceLocation texture) {
         return DEPTH.computeIfAbsent(
            texture,
            t -> RenderType.create(
                  "devilfruits_horo_ghost_depth",
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

      public static RenderType ghost(ResourceLocation texture) {
         return GHOST.computeIfAbsent(
            texture,
            t -> RenderType.create(
                  "devilfruits_horo_ghost",
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
