package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.client.render.vfx.VfxContext;
import com.devilfruits.client.render.vfx.VfxEffect;
import com.devilfruits.fruits.suna_suna.SunaBlocks;
import com.devilfruits.fruits.suna_suna.SunaSuna;
import com.devilfruits.fruits.suna_suna.block.QuicksandBlock;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public abstract class SandPaintEffect extends VfxEffect {
   protected static final int[] SAND = abgr(9402960, 11047010, 12297076, 13219205, 14932912, 15197371, 15592395);
   protected static final int[] RED_SAND = abgr(6171660, 8010256, 9783830, 10834971, 13332004, 13792555, 14252848);
   protected static final float[] BAYER = new float[]{0.0F, 8.0F, 2.0F, 10.0F, 12.0F, 4.0F, 14.0F, 6.0F, 3.0F, 11.0F, 1.0F, 9.0F, 15.0F, 7.0F, 13.0F, 5.0F};
   public final long seed;
   protected final Vec3 centre;
   protected final float radius;
   protected final int[] palette;
   protected final int size;
   protected final int ppb;
   protected final float[] dist;
   protected final float[] angle;
   protected final float[] noise;
   private final int cols;
   private final int minX;
   private final int minZ;
   private final float[] surface;
   private final int[] light;
   private final ResourceLocation location;
   private final int closeTicks;
   @Nullable
   private NativeImage image;
   @Nullable
   private DynamicTexture texture;
   protected int closing = -1;

   protected SandPaintEffect(ClientLevel level, Vec3 centre, float radius, long seed, String name, int closeTicks) {
      this.seed = seed;
      this.centre = centre;
      this.radius = Math.max(1.0F, radius);
      this.closeTicks = closeTicks;
      BlockState middle = level.getBlockState(BlockPos.containing(centre.x, centre.y - 0.5, centre.z));
      boolean red = middle.is(Blocks.RED_SAND) || middle.is((Block)SunaBlocks.QUICKSAND.get()) && (Boolean)middle.getValue(QuicksandBlock.RED);
      this.palette = red ? RED_SAND : SAND;
      int half = Mth.ceil(this.radius) + 1;
      this.cols = half * 2 + 1;
      this.ppb = this.radius <= 14.0F ? 16 : 8;
      this.size = this.cols * this.ppb;
      this.minX = Mth.floor(centre.x) - half;
      this.minZ = Mth.floor(centre.z) - half;
      this.surface = new float[this.cols * this.cols];
      this.light = new int[this.cols * this.cols];
      this.dist = new float[this.size * this.size];
      this.angle = new float[this.size * this.size];
      this.noise = new float[this.size * this.size];
      RandomSource random = RandomSource.create(seed);

      for (int iz = 0; iz < this.size; iz++) {
         for (int ix = 0; ix < this.size; ix++) {
            double wx = (double)this.minX + ((double)ix + 0.5) / (double)this.ppb - centre.x;
            double wz = (double)this.minZ + ((double)iz + 0.5) / (double)this.ppb - centre.z;
            int i = iz * this.size + ix;
            this.dist[i] = (float)Math.sqrt(wx * wx + wz * wz);
            float a = (float)(Math.atan2(wz, wx) / (Math.PI * 2));
            this.angle[i] = a < 0.0F ? a + 1.0F : a;
            this.noise[i] = random.nextFloat() - 0.5F;
         }
      }

      this.location = SunaSuna.id("dynamic/" + name + "_" + Long.toHexString(seed));
      this.image = new NativeImage(this.size, this.size, true);
      this.texture = new DynamicTexture(this.image);
      Minecraft.getInstance().getTextureManager().register(this.location, this.texture);
      this.scanSurface(level);
   }

   protected static int[] abgr(int... rgb) {
      int[] out = new int[rgb.length];

      for (int i = 0; i < rgb.length; i++) {
         int c = rgb[i];
         out[i] = 0xFF000000 | (c & 0xFF) << 16 | (c >> 8 & 0xFF) << 8 | c >> 16 & 0xFF;
      }

      return out;
   }

   protected abstract void paint(NativeImage var1);

   protected abstract float shownRadius(float var1);

   public void close() {
      if (this.closing < 0) {
         this.closing = 0;
      }
   }

   public boolean closing() {
      return this.closing >= 0;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.closing >= 0 && ++this.closing > this.closeTicks) {
         this.finish();
      } else {
         if (this.age % 10 == 0) {
            this.scanSurface(level);
         }

         if (this.image != null && this.texture != null) {
            this.paint(this.image);
            this.texture.upload();
         }
      }
   }

   private void scanSurface(ClientLevel level) {
      MutableBlockPos pos = new MutableBlockPos();
      int top = Mth.floor(this.centre.y) + 3;

      for (int cz = 0; cz < this.cols; cz++) {
         for (int cx = 0; cx < this.cols; cx++) {
            int i = cz * this.cols + cx;
            this.surface[i] = Float.NaN;
            pos.set(this.minX + cx, top, this.minZ + cz);
            int k = 0;

            while (k < 9) {
               BlockState state = level.getBlockState(pos);
               if (!state.isAir()) {
                  if (!state.getFluidState().isEmpty()) {
                     break;
                  }

                  if (state.is((Block)SunaBlocks.QUICKSAND.get())) {
                     this.surface[i] = (float)pos.getY() + 1.0F;
                     break;
                  }

                  if (!state.getCollisionShape(level, pos).isEmpty()) {
                     if (state.is(BlockTags.SAND) && !state.hasBlockEntity()) {
                        this.surface[i] = (float)pos.getY() + 1.0F;
                     }
                     break;
                  }
               }

               k++;
               pos.move(Direction.DOWN);
            }

            if (!Float.isNaN(this.surface[i])) {
               this.light[i] = LevelRenderer.getLightColor(
                  level, BlockPos.containing((double)(this.minX + cx) + 0.5, (double)this.surface[i] + 0.1, (double)(this.minZ + cz) + 0.5)
               );
            }
         }
      }
   }

   public float surfaceAt(double x, double z) {
      int cx = Mth.floor(x) - this.minX;
      int cz = Mth.floor(z) - this.minZ;
      return cx >= 0 && cz >= 0 && cx < this.cols && cz < this.cols ? this.surface[cz * this.cols + cx] : Float.NaN;
   }

   protected float alpha(float partialTick) {
      return this.closing >= 0
         ? Math.max(0.0F, 1.0F - ((float)this.closing + partialTick) / (float)this.closeTicks)
         : Math.min(1.0F, ((float)this.age + partialTick) / 4.0F);
   }

   @Override
   public void renderTextured(VfxContext ctx) {
      if (this.texture != null) {
         int a = (int)(this.alpha(ctx.partialTick) * 255.0F);
         if (a > 2) {
            float r = this.shownRadius((float)this.age + ctx.partialTick) + 0.5F;
            VertexConsumer vc = ctx.buffers.getBuffer(RenderType.entityTranslucent(this.location));
            Matrix4f m = ctx.matrix;
            float step = 1.0F / (float)this.cols;

            for (int cz = 0; cz < this.cols; cz++) {
               for (int cx = 0; cx < this.cols; cx++) {
                  int i = cz * this.cols + cx;
                  float top = this.surface[i];
                  if (!Float.isNaN(top)) {
                     double x0 = (double)(this.minX + cx);
                     double z0 = (double)(this.minZ + cz);
                     double nx = Mth.clamp(this.centre.x, x0, x0 + 1.0) - this.centre.x;
                     double nz = Mth.clamp(this.centre.z, z0, z0 + 1.0) - this.centre.z;
                     if (!(nx * nx + nz * nz > (double)(r * r))) {
                        float y = ctx.ry((double)top + 0.004);
                        float ax = ctx.rx(x0);
                        float az = ctx.rz(z0);
                        float bx = ctx.rx(x0 + 1.0);
                        float bz = ctx.rz(z0 + 1.0);
                        float u0 = (float)cx * step;
                        float u1 = (float)(cx + 1) * step;
                        float v0 = (float)cz * step;
                        float v1 = (float)(cz + 1) * step;
                        int l = this.light[i];
                        vc.addVertex(m, ax, y, az)
                           .setColor(255, 255, 255, a)
                           .setUv(u0, v0)
                           .setOverlay(OverlayTexture.NO_OVERLAY)
                           .setLight(l)
                           .setNormal(0.0F, 1.0F, 0.0F);
                        vc.addVertex(m, ax, y, bz)
                           .setColor(255, 255, 255, a)
                           .setUv(u0, v1)
                           .setOverlay(OverlayTexture.NO_OVERLAY)
                           .setLight(l)
                           .setNormal(0.0F, 1.0F, 0.0F);
                        vc.addVertex(m, bx, y, bz)
                           .setColor(255, 255, 255, a)
                           .setUv(u1, v1)
                           .setOverlay(OverlayTexture.NO_OVERLAY)
                           .setLight(l)
                           .setNormal(0.0F, 1.0F, 0.0F);
                        vc.addVertex(m, bx, y, az)
                           .setColor(255, 255, 255, a)
                           .setUv(u1, v0)
                           .setOverlay(OverlayTexture.NO_OVERLAY)
                           .setLight(l)
                           .setNormal(0.0F, 1.0F, 0.0F);
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public void removed() {
      if (this.texture != null) {
         Minecraft.getInstance().getTextureManager().release(this.location);
         this.texture = null;
         this.image = null;
      }
   }

   @Override
   public double x() {
      return this.centre.x;
   }

   @Override
   public double y() {
      return this.centre.y;
   }

   @Override
   public double z() {
      return this.centre.z;
   }

   @Override
   public double cullDistance() {
      return 160.0;
   }
}
