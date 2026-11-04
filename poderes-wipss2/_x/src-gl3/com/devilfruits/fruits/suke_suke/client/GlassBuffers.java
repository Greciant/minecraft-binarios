package com.devilfruits.fruits.suke_suke.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.resources.ResourceLocation;

public final class GlassBuffers implements MultiBufferSource {
   private static final Pattern TEXTURE = Pattern.compile("texture\\[Optional\\[([a-z0-9_.\\-]+:[a-z0-9_./\\-]+)\\]");
   private static final Map<RenderType, Object> TWINS = new IdentityHashMap<>();
   private final MultiBufferSource inner;
   private final int r;
   private final int g;
   private final int b;
   private final float alpha;

   private GlassBuffers(MultiBufferSource inner, int tint, float alpha) {
      this.inner = inner;
      this.r = tint >> 16 & 0xFF;
      this.g = tint >> 8 & 0xFF;
      this.b = tint & 0xFF;
      this.alpha = alpha;
   }

   public static GlassBuffers of(MultiBufferSource inner, int tint, float alpha) {
      return new GlassBuffers(inner instanceof GlassBuffers g ? g.inner : inner, tint, alpha);
   }

   public static MultiBufferSource unwrap(MultiBufferSource buffers) {
      return buffers instanceof GlassBuffers g ? g.inner : buffers;
   }

   public float alpha() {
      return this.alpha;
   }

   public VertexConsumer getBuffer(RenderType type) {
      Object twin = TWINS.computeIfAbsent(type, GlassBuffers::twin);
      return (VertexConsumer)(twin == GlassBuffers.Marker.DROP
         ? GlassBuffers.Nothing.INSTANCE
         : new GlassBuffers.Fade(this.inner.getBuffer(twin instanceof RenderType t ? t : type), this.r, this.g, this.b, this.alpha));
   }

   private static Object twin(RenderType type) {
      String desc = type.toString();
      if (desc.contains("glint")) {
         return GlassBuffers.Marker.DROP;
      } else {
         RenderType made = null;
         if (type == Sheets.cutoutBlockSheet()
            || type == Sheets.solidBlockSheet()
            || type == RenderType.solid()
            || type == RenderType.cutout()
            || type == RenderType.cutoutMipped()) {
            made = Sheets.translucentItemSheet();
         } else if (!desc.contains("translucent") && (desc.contains("entity") || desc.contains("armor") || desc.contains("item"))) {
            Matcher m = TEXTURE.matcher(desc);
            if (m.find()) {
               ResourceLocation tex = ResourceLocation.tryParse(m.group(1));
               if (tex != null) {
                  made = RenderType.entityTranslucent(tex);
               }
            }
         }

         return made == null ? GlassBuffers.Marker.KEEP : made;
      }
   }

   private static final class Fade implements VertexConsumer {
      private final VertexConsumer vc;
      private final int r;
      private final int g;
      private final int b;
      private final float alpha;

      Fade(VertexConsumer vc, int r, int g, int b, float alpha) {
         this.vc = vc;
         this.r = r;
         this.g = g;
         this.b = b;
         this.alpha = alpha;
      }

      public VertexConsumer addVertex(float x, float y, float z) {
         this.vc.addVertex(x, y, z);
         return this;
      }

      public VertexConsumer setColor(int red, int green, int blue, int a) {
         this.vc.setColor(red * this.r / 255, green * this.g / 255, blue * this.b / 255, Math.round((float)a * this.alpha));
         return this;
      }

      public VertexConsumer setUv(float u, float v) {
         this.vc.setUv(u, v);
         return this;
      }

      public VertexConsumer setUv1(int u, int v) {
         this.vc.setUv1(u, v);
         return this;
      }

      public VertexConsumer setUv2(int u, int v) {
         this.vc.setUv2(u, v);
         return this;
      }

      public VertexConsumer setNormal(float x, float y, float z) {
         this.vc.setNormal(x, y, z);
         return this;
      }
   }

   private static enum Marker {
      KEEP,
      DROP;
   }

   private static enum Nothing implements VertexConsumer {
      INSTANCE;

      public VertexConsumer addVertex(float x, float y, float z) {
         return this;
      }

      public VertexConsumer setColor(int r, int g, int b, int a) {
         return this;
      }

      public VertexConsumer setUv(float u, float v) {
         return this;
      }

      public VertexConsumer setUv1(int u, int v) {
         return this;
      }

      public VertexConsumer setUv2(int u, int v) {
         return this;
      }

      public VertexConsumer setNormal(float x, float y, float z) {
         return this;
      }
   }
}
