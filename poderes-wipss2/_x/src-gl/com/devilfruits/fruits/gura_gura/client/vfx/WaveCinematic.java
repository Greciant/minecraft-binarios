package com.devilfruits.fruits.gura_gura.client.vfx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.fx.GroundWave;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.fruits.gura_gura.entity.QuakeWaveEntity;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;

public class WaveCinematic extends Cinematic {
   private static final ResourceLocation WATER_STILL = ResourceLocation.withDefaultNamespace("block/water_still");
   private static final ResourceLocation WATER_FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");
   private static final int COLUMNS = 22;
   private static final float[] OF = new float[]{-1.3F, -0.9F, -0.55F, -0.28F, -0.08F, 0.12F, 0.34F, 0.56F, 0.74F, 0.86F, 0.9F};
   private static final float[] OH = new float[]{0.0F, 0.25F, 0.55F, 0.82F, 0.97F, 1.04F, 1.03F, 0.94F, 0.78F, 0.6F, 0.44F};
   private static final float[] IF = new float[]{0.42F, 0.3F, 0.22F, 0.22F, 0.3F, 0.46F, 0.66F, 0.88F};
   private static final float[] IH = new float[]{0.0F, 0.15F, 0.33F, 0.5F, 0.62F, 0.67F, 0.61F, 0.46F};
   private static final float[] GF = new float[]{-1.5F, -0.9F, -0.4F, 0.0F, 0.35F, 0.65F, 0.9F};
   private static final float[] GH = new float[]{0.0F, 0.42F, 0.85F, 1.0F, 0.86F, 0.45F, 0.0F};
   private final int waveId;
   private final RandomSource random;
   private final boolean water;
   private double px;
   private double py;
   private double pz;
   private double opx;
   private double opy;
   private double opz;
   private float fx = 0.0F;
   private float fz = 1.0F;
   private float width;
   private float height;
   private float envelope;
   private float oEnvelope;
   private int lifetime;
   private int gone = -1;
   private float light = 1.0F;
   private float surface = 1.0F;
   private final float[] cs = new float[23];
   private final float[] cf = new float[23];

   private WaveCinematic(ClientLevel level, QuakeWaveEntity wave) {
      super(new VfxAnchor(wave.getId(), wave.position()), VfxParams.NONE);
      this.waveId = wave.getId();
      this.random = RandomSource.create((long)wave.getId() * 31L);
      this.water = wave.isWater();
      this.pull(level, wave, 1.0F);
      this.opx = this.px;
      this.opy = this.py;
      this.opz = this.pz;
   }

   public static void ensure(QuakeWaveEntity wave) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null && wave.isWater() && VfxDirector.find(WaveCinematic.class, c -> c.waveId == wave.getId()) == null) {
         VfxDirector.spawn(new WaveCinematic(level, wave));
      }
   }

   private void pull(ClientLevel level, QuakeWaveEntity w, float partial) {
      this.px = w.getX();
      this.py = w.getY();
      this.pz = w.getZ();
      Vec3 d = w.direction();
      this.fx = (float)d.x;
      this.fz = (float)d.z;
      this.width = w.width();
      this.height = w.height();
      this.lifetime = w.lifetime();
      this.oEnvelope = this.envelope;
      this.envelope = w.envelope(partial);
      this.light = VfxWorld.light(level, this.px, this.py + 1.0, this.pz);
      this.surface = VfxWorld.surfaceLight(level, this.px, this.py + 1.0, this.pz);
   }

   @Override
   public void tick(ClientLevel level) {
      label56: {
         super.tick(level);
         this.opx = this.px;
         this.opy = this.py;
         this.opz = this.pz;
         if (level.getEntity(this.waveId) instanceof QuakeWaveEntity w && !w.isRemoved()) {
            this.pull(level, w, 1.0F);
            break label56;
         }

         if (this.gone < 0) {
            this.gone = this.age;
            if (this.water) {
               this.crash(level);
            }
         }
      }

      if (this.gone >= 0) {
         this.oEnvelope = this.envelope;
         this.envelope = Math.max(0.0F, this.envelope - 0.2F);
         if (this.age - this.gone > 20) {
            this.finish();
         }
      } else {
         float h = this.height * this.envelope;
         if (!(h < 0.2F)) {
            float lx = -this.fz;
            float lz = this.fx;
            if (this.water) {
               for (int i = 0; i < VfxSettings.count(3.0F); i++) {
                  float s = (this.random.nextFloat() - 0.5F) * this.width * 0.9F;
                  float hm = this.profileAcross(s);
                  double cx = this.px + (double)(lx * s) + (double)this.fx * 0.1 * (double)h;
                  double cy = this.py + (double)(h * hm) * 1.02;
                  double cz = this.pz + (double)(lz * s) + (double)this.fz * 0.1 * (double)h;
                  VfxParticles.emit(
                        VfxPresets.SPEED_LINE,
                        cx,
                        cy,
                        cz,
                        (double)(-this.fx) * 0.35 + (double)(this.random.nextFloat() - 0.5F) * 0.1,
                        0.18 + (double)this.random.nextFloat() * 0.12,
                        (double)(-this.fz) * 0.35 + (double)(this.random.nextFloat() - 0.5F) * 0.1
                     )
                     .tint(VfxWorld.shade(15398138, Math.max(0.4F, this.surface)))
                     .alpha(0.7F)
                     .life(1.6F);
                  if (i == 0) {
                     VfxParticles.emit(VfxPresets.SMOKE, cx, cy, cz, (double)(-this.fx) * 0.1, 0.06, (double)(-this.fz) * 0.1)
                        .tint(VfxWorld.shade(15923452, Math.max(0.4F, this.surface)))
                        .size(0.35F + h * 0.05F)
                        .alpha(0.3F)
                        .life(0.2F);
                  }
               }

               for (int ix = 0; ix < VfxSettings.count(4.0F); ix++) {
                  float s = (this.random.nextFloat() - 0.5F) * this.width;
                  double cx = this.px + (double)(lx * s) + (double)this.fx * 0.45 * (double)h;
                  double cz = this.pz + (double)(lz * s) + (double)this.fz * 0.45 * (double)h;
                  VfxParticles.emit(VfxPresets.SMOKE, cx, this.py + 0.3, cz, (double)this.fx * 0.15, 0.05, (double)this.fz * 0.15)
                     .tint(VfxWorld.shade(15266552, this.light))
                     .size(0.6F + h * 0.06F)
                     .alpha(0.35F)
                     .life(0.25F);
               }

               if (this.age % 8 == 3) {
                  ShockRing.at(
                        new Vec3(this.px + (double)(this.fx * h), this.py + (double)h * 0.5, this.pz + (double)(this.fz * h)),
                        new Vec3((double)this.fx, 0.0, (double)this.fz)
                     )
                     .radius(this.width * 0.25F, this.width * 0.6F)
                     .life(10)
                     .move(new Vec3((double)this.fx * 0.9, 0.0, (double)this.fz * 0.9))
                     .strength(0.8F)
                     .band(0.25F)
                     .rim(0.0F, 15398655)
                     .spawn();
               }
            }
         }
      }
   }

   private void crash(ClientLevel level) {
      float h = Math.max(1.0F, this.height * Math.max(this.envelope, 0.6F));
      float lx = -this.fz;
      float lz = this.fx;

      for (int i = 0; i < VfxSettings.count(40.0F); i++) {
         float s = (this.random.nextFloat() - 0.5F) * this.width;
         double cx = this.px + (double)(lx * s) + (double)this.fx * 0.5 * (double)h;
         double cz = this.pz + (double)(lz * s) + (double)this.fz * 0.5 * (double)h;
         VfxParticles.emit(
               VfxPresets.SMOKE,
               cx,
               this.py + (double)(this.random.nextFloat() * h),
               cz,
               (double)this.fx * 0.25 + (double)(this.random.nextFloat() - 0.5F) * 0.2,
               0.1 + (double)this.random.nextFloat() * 0.2,
               (double)this.fz * 0.25 + (double)(this.random.nextFloat() - 0.5F) * 0.2
            )
            .tint(VfxWorld.shade(15923452, this.light))
            .size(1.2F + h * 0.2F)
            .alpha(0.6F)
            .life(0.6F);
      }

      GroundWave.at(this.px + (double)(this.fx * h) * 0.6, this.py, this.pz + (double)(this.fz * h) * 0.6, this.width * 0.7F, 14)
         .strength(1.0F)
         .ring(15267066, 0.4F)
         .spawn();
   }

   private float profileAcross(float s) {
      float a = Mth.clamp(s / (this.width * 0.5F), -1.0F, 1.0F);
      float a2 = a * a;
      return 1.0F - 0.85F * a2 * a2 * a2;
   }

   @Override
   public void render(VfxFrame f) {
      float env = Mth.lerp(f.partial, this.oEnvelope, this.envelope);
      float h = this.height * env;
      if (!(h < 0.1F) && f.allowMesh()) {
         float t = (float)this.age + f.partial;
         float ox = f.rx(Mth.lerp((double)f.partial, this.opx, this.px));
         float oy = f.ry(Mth.lerp((double)f.partial, this.opy, this.py));
         float oz = f.rz(Mth.lerp((double)f.partial, this.opz, this.pz));
         float lx = -this.fz;
         float lz = this.fx;

         for (int c = 0; c <= 22; c++) {
            float s = ((float)c / 22.0F - 0.5F) * this.width;
            float a = s / (this.width * 0.5F);
            this.cs[c] = s;
            this.cf[c] = -0.35F * h * a * a + 0.06F * h * Mth.sin(t * 0.3F + (float)c * 0.7F);
         }

         this.renderTsunami(f, t, h, ox, oy, oz, lx, lz);
      }
   }

   private void renderTsunami(VfxFrame f, float t, float h, float ox, float oy, float oz, float lx, float lz) {
      TextureAtlasSprite still = (TextureAtlasSprite)Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(WATER_STILL);
      TextureAtlasSprite flow = (TextureAtlasSprite)Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(WATER_FLOW);
      BufferBuilder b = f.mesh(MeshTex.BLOCKS);
      float near = 1.0F;

      for (int c = 0; c < 22; c++) {
         for (int r = 0; r < OF.length - 1; r++) {
            for (int q = 0; q < 4; q++) {
               int cc = c + (q != 1 && q != 2 ? 0 : 1);
               int rr = r + (q >= 2 ? 1 : 0);
               float hm = this.profileAcross(this.cs[cc]);
               float lip = rr >= 6 ? 1.0F + 0.08F * Mth.sin(t * 0.5F + (float)cc * 0.9F) : 1.0F;
               float fwd = OF[rr] * h * lip + this.cf[cc];
               float up = OH[rr] * h * hm;
               float x = ox + lx * this.cs[cc] + this.fx * fwd;
               float y = oy + up;
               float z = oz + lz * this.cs[cc] + this.fz * fwd;
               float k = OH[rr];
               int rgb = rr >= 6 ? lerpRgb(3050147, 7326938, (float)(rr - 6) / 4.0F) : lerpRgb(733768, 2784920, k);
               float alpha = rr >= 7 ? 0.82F : 0.93F;
               TextureAtlasSprite sp = r >= 5 ? flow : still;
               float u = q != 1 && q != 2 ? sp.getU0() : sp.getU1();
               float v = q >= 2 ? sp.getV0() : sp.getV1();
               MeshWriter.vertex(
                  b,
                  x,
                  y,
                  z,
                  MeshWriter.argb(VfxWorld.shade(rgb, Math.max(0.3F, this.surface)), alpha * near),
                  u,
                  v,
                  -1.0F,
                  0,
                  0.04F,
                  0.0F,
                  0.0F,
                  0.0F,
                  1.0F,
                  0.0F
               );
            }
         }
      }

      for (int c = 0; c < 22; c++) {
         for (int r = 0; r < IF.length - 1; r++) {
            for (int q = 0; q < 4; q++) {
               int cc = c + (q != 1 && q != 2 ? 0 : 1);
               int rr = r + (q >= 2 ? 1 : 0);
               float hm = this.profileAcross(this.cs[cc]);
               float fwd = IF[rr] * h + this.cf[cc];
               float up = IH[rr] * h * hm;
               float x = ox + lx * this.cs[cc] + this.fx * fwd;
               float y = oy + up;
               float z = oz + lz * this.cs[cc] + this.fz * fwd;
               int rgb = lerpRgb(534584, 2060168, IH[rr] / 0.66F);
               float u = q != 1 && q != 2 ? flow.getU0() : flow.getU1();
               float v = q >= 2 ? flow.getV0() : flow.getV1();
               MeshWriter.vertex(
                  b, x, y, z, MeshWriter.argb(VfxWorld.shade(rgb, Math.max(0.3F, this.surface)), 0.95F), u, v, -1.0F, 0, 0.03F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F
               );
            }
         }
      }

      BufferBuilder foam = f.mesh(MeshTex.FLAME_RIBBON);
      int white = VfxWorld.shade(16055039, Math.max(0.42F, this.surface));

      for (int c = 0; c < 22; c++) {
         for (int band = 0; band < 2; band++) {
            for (int q = 0; q < 4; q++) {
               int cc = c + (q != 1 && q != 2 ? 0 : 1);
               float hm = this.profileAcross(this.cs[cc]);
               float across = q >= 2 ? 1.0F : 0.0F;
               float fwd;
               float up;
               if (band == 0) {
                  float k = 5.0F + across * 5.0F;
                  int i0 = (int)Math.floor((double)k);
                  int i1 = Math.min(OF.length - 1, i0 + 1);
                  float e = k - (float)i0;
                  fwd = Mth.lerp(e, OF[i0], OF[i1]) * h * 1.01F + this.cf[cc];
                  up = Mth.lerp(e, OH[i0], OH[i1]) * h * hm + 0.04F;
               } else {
                  fwd = Mth.lerp(across, IF[0] + 0.25F, IF[1]) * h + this.cf[cc];
                  up = Mth.lerp(across, 0.02F, IH[1] * 0.8F) * h * hm;
               }

               float x = ox + lx * this.cs[cc] + this.fx * fwd;
               float y = oy + up;
               float z = oz + lz * this.cs[cc] + this.fz * fwd;
               float u = (float)cc * 0.37F - t * 0.06F + (float)band * 0.5F;
               MeshWriter.vertex(foam, x, y, z, MeshWriter.argb(white, band == 0 ? 0.85F : 0.7F), u, across, -1.0F, 0, 0.45F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F);
            }
         }
      }

      BufferBuilder d = f.distortion();
      if (d != null) {
         VfxSprites.Sprite glow = VfxSprites.GLOW;

         for (int c = 1; c < 22; c += 3) {
            float hm = this.profileAcross(this.cs[c]);
            float fwd = 0.45F * h + this.cf[c];
            MeshWriter.distortQuad(
               d,
               ox + lx * this.cs[c] + this.fx * fwd,
               oy + h * 0.55F * hm,
               oz + lz * this.cs[c] + this.fz * fwd,
               0.5F + h * 0.25F,
               0.55F,
               1,
               false,
               f.left.x,
               f.left.y,
               f.left.z,
               f.up.x,
               f.up.y,
               f.up.z,
               glow.u0(),
               glow.v0(),
               glow.u1(),
               glow.v1(),
               (float)c * 0.21F + f.time * 0.012F,
               0.6F
            );
         }
      }
   }

   private static int lerpRgb(int a, int b, float t) {
      t = Mth.clamp(t, 0.0F, 1.0F);
      int r = (int)Mth.lerp(t, (float)(a >> 16 & 0xFF), (float)(b >> 16 & 0xFF));
      int g = (int)Mth.lerp(t, (float)(a >> 8 & 0xFF), (float)(b >> 8 & 0xFF));
      int bl = (int)Mth.lerp(t, (float)(a & 0xFF), (float)(b & 0xFF));
      return r << 16 | g << 8 | bl;
   }

   @Override
   public double x() {
      return this.px;
   }

   @Override
   public double y() {
      return this.py + (double)this.height * 0.5;
   }

   @Override
   public double z() {
      return this.pz;
   }

   @Override
   public double cullDistance() {
      return 160.0;
   }
}
