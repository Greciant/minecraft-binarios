package com.devilfruits.client.vfx.particle;

import com.devilfruits.client.vfx.sprite.VfxSprites;

public final class ParticleStyle {
   final VfxSprites.Sprite sprite;
   final int palette;
   final int lifeMin;
   final int lifeMax;
   final float size0;
   final float size1;
   final float sizePow;
   final float fadeIn;
   final float fadeOut;
   final float heat0;
   final float heat1;
   final float additive;
   final float emissive;
   final float soft;
   final float drag;
   final float gravity;
   final ParticleStyle.Mode mode;
   final float stretch;
   final boolean collide;
   final float bounce;
   final float spin;
   final float fps;
   final boolean occluding;
   final boolean stepped;

   private ParticleStyle(ParticleStyle.Builder b) {
      this.sprite = b.sprite;
      this.palette = b.palette;
      this.lifeMin = b.lifeMin;
      this.lifeMax = b.lifeMax;
      this.size0 = b.size0;
      this.size1 = b.size1;
      this.sizePow = b.sizePow;
      this.fadeIn = b.fadeIn;
      this.fadeOut = b.fadeOut;
      this.heat0 = b.heat0;
      this.heat1 = b.heat1;
      this.additive = b.additive;
      this.emissive = b.emissive;
      this.soft = b.soft;
      this.drag = b.drag;
      this.gravity = b.gravity;
      this.mode = b.mode;
      this.stretch = b.stretch;
      this.collide = b.collide;
      this.bounce = b.bounce;
      this.spin = b.spin;
      this.fps = b.fps;
      this.occluding = b.occluding;
      this.stepped = b.stepped;
   }

   public static ParticleStyle.Builder of(VfxSprites.Sprite sprite, int palette) {
      return new ParticleStyle.Builder(sprite, palette);
   }

   public static ParticleStyle.Builder colour(VfxSprites.Sprite sprite) {
      return new ParticleStyle.Builder(sprite, -1);
   }

   public boolean heat() {
      return this.palette >= 0;
   }

   public static final class Builder {
      private final VfxSprites.Sprite sprite;
      private final int palette;
      private int lifeMin = 20;
      private int lifeMax = 30;
      private float size0 = 0.5F;
      private float size1 = 1.0F;
      private float sizePow = 1.0F;
      private float fadeIn = 0.1F;
      private float fadeOut = 0.3F;
      private float heat0 = 1.0F;
      private float heat1 = 1.0F;
      private float additive;
      private float emissive = 1.0F;
      private float soft = 0.5F;
      private float drag = 0.04F;
      private float gravity;
      private ParticleStyle.Mode mode = ParticleStyle.Mode.BILLBOARD;
      private float stretch;
      private boolean collide;
      private float bounce = 0.5F;
      private float spin = 0.05F;
      private float fps;
      private boolean occluding;
      private boolean stepped;

      private Builder(VfxSprites.Sprite sprite, int palette) {
         this.sprite = sprite;
         this.palette = palette;
      }

      public ParticleStyle.Builder life(int min, int max) {
         this.lifeMin = min;
         this.lifeMax = Math.max(min, max);
         return this;
      }

      public ParticleStyle.Builder size(float start, float end, float pow) {
         this.size0 = start;
         this.size1 = end;
         this.sizePow = pow;
         return this;
      }

      public ParticleStyle.Builder fade(float in, float out) {
         this.fadeIn = in;
         this.fadeOut = out;
         return this;
      }

      public ParticleStyle.Builder heat(float start, float end) {
         this.heat0 = start;
         this.heat1 = end;
         return this;
      }

      public ParticleStyle.Builder additive(float a) {
         this.additive = a;
         return this;
      }

      public ParticleStyle.Builder emissive(float e) {
         this.emissive = e;
         return this;
      }

      public ParticleStyle.Builder soft(float blocks) {
         this.soft = blocks;
         return this;
      }

      public ParticleStyle.Builder drag(float d) {
         this.drag = d;
         return this;
      }

      public ParticleStyle.Builder gravity(float g) {
         this.gravity = g;
         return this;
      }

      public ParticleStyle.Builder flat() {
         this.mode = ParticleStyle.Mode.FLAT;
         return this;
      }

      public ParticleStyle.Builder stretch(float factor) {
         this.mode = ParticleStyle.Mode.STRETCH;
         this.stretch = factor;
         return this;
      }

      public ParticleStyle.Builder collide(float restitution) {
         this.collide = true;
         this.bounce = restitution;
         return this;
      }

      public ParticleStyle.Builder spin(float maxRadPerTick) {
         this.spin = maxRadPerTick;
         return this;
      }

      public ParticleStyle.Builder loop(float fps) {
         this.fps = fps;
         return this;
      }

      public ParticleStyle.Builder occluding() {
         this.occluding = true;
         return this;
      }

      public ParticleStyle.Builder stepped() {
         this.stepped = true;
         return this;
      }

      public ParticleStyle build() {
         return new ParticleStyle(this);
      }
   }

   public static enum Mode {
      BILLBOARD,
      STRETCH,
      FLAT;
   }
}
