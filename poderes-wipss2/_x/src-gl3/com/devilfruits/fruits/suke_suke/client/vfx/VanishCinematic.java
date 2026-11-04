package com.devilfruits.fruits.suke_suke.client.vfx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.fruits.suke_suke.client.ClearTransitions;
import com.devilfruits.fruits.suke_suke.client.Visibility;
import com.mojang.blaze3d.vertex.BufferBuilder;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class VanishCinematic extends Cinematic {
   private static final int[] PRISM = new int[]{12578784, 8378568, 14546376, 16777215, 16769264, 14736127};
   private static final int GLINT = 16056312;
   private static final float[] WIDTH = new float[]{0.13F, 0.15F, 0.17F, 0.2F, 0.23F, 0.24F, 0.1F, 0.15F, 0.16F, 0.12F};
   private static final int ROWS = WIDTH.length - 1;
   private static final int COLS = 6;
   private final int entityId;
   private final boolean vanish;
   private Vec3 last;
   private float height;
   private boolean ripples;
   private static final float[] XS = new float[3];
   private static final float[] YS = new float[3];
   private static final float[] ZS = new float[3];
   private static final float[] WS = new float[3];
   private static final float[] HS = new float[]{-1.0F, -1.0F, -1.0F};
   private static final float[] AS = new float[3];

   public VanishCinematic(Entity e, boolean vanish) {
      super(VfxAnchor.of(e, e.position()), VfxParams.NONE);
      this.entityId = e.getId();
      this.vanish = vanish;
      this.last = e.position();
      this.height = e.getBbHeight();
   }

   @Nullable
   private Entity entity(ClientLevel level) {
      return level.getEntity(this.entityId);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = this.entity(level);
      if (e != null && this.age <= 64) {
         this.last = e.position();
         this.height = e.getBbHeight();
         float phase = ClearTransitions.phase(e, 0.0F);
         boolean running = ClearTransitions.running(e, 0.0F);
         if (running || this.age <= 4 || (this.vanish ? !(phase >= 1.0F) : !(phase <= 0.0F) && !(phase < 0.0F))) {
            if (!this.ripples && (this.vanish ? phase >= 0.22F : phase <= 0.98F && running)) {
               this.ripples = true;
               double floor = VfxWorld.ground(level, this.last.x, this.last.y + 0.2, this.last.z, 3);
               Vec3 at = new Vec3(this.last.x, floor + 0.04, this.last.z);

               for (int k = 0; k < 3; k++) {
                  float r0 = this.vanish ? 0.25F + (float)k * 0.1F : 2.6F - (float)k * 0.3F;
                  float r1 = this.vanish ? 2.4F + (float)k * 0.5F : 0.3F;
                  ShockRing.at(at, new Vec3(0.0, 1.0, 0.0))
                     .radius(r0, r1)
                     .life(18 + k * 5)
                     .strength(0.55F - (float)k * 0.12F)
                     .band(0.16F)
                     .rim(0.3F - (float)k * 0.07F, 14546376)
                     .spawn();
               }
            }

            float lively = Mth.sin((float) Math.PI * Mth.clamp(phase, 0.0F, 1.0F));
            if (running && lively > 0.15F && !VfxWorld.firstPersonCaster(this.entityId)) {
               RandomSource r = VfxSettings.random();
               int n = r.nextFloat() < lively * 1.6F ? 2 : 1;

               for (int i = 0; i < n; i++) {
                  double y = this.last.y + r.nextDouble() * (double)this.height;
                  double a = r.nextDouble() * (float) (Math.PI * 2);
                  double rad = 0.2 + r.nextDouble() * 0.25;
                  VfxParticles.emit(
                        r.nextFloat() < 0.65F ? VfxPresets.GLASS_GLINT : VfxPresets.GLASS_DUST,
                        this.last.x + Math.cos(a) * rad,
                        y,
                        this.last.z + Math.sin(a) * rad,
                        (r.nextDouble() - 0.5) * 0.01,
                        0.02 + r.nextDouble() * 0.035,
                        (r.nextDouble() - 0.5) * 0.01
                     )
                     .tint(PRISM[r.nextInt(PRISM.length)])
                     .size(0.55F + r.nextFloat() * 0.5F);
               }
            }
         } else {
            this.finish();
         }
      } else {
         this.finish();
      }
   }

   @Override
   public void render(VfxFrame f) {
      ClientLevel level = Minecraft.getInstance().level;
      Entity e = level == null ? null : level.getEntity(this.entityId);
      if (e != null) {
         float phase = ClearTransitions.phase(e, f.entityPartial);
         if (!(phase < 0.0F) && ClearTransitions.running(e, f.entityPartial) && !VfxWorld.firstPersonCaster(this.entityId)) {
            Vec3 p = e.getPosition(f.entityPartial);
            float h = e.getBbHeight();
            float ox = f.rx(p.x);
            float oy = f.ry(p.y);
            float oz = f.rz(p.z);
            float rx = -f.left.x;
            float rz = -f.left.z;
            float rl = Mth.sqrt(rx * rx + rz * rz) + 1.0E-4F;
            rx /= rl;
            rz /= rl;
            boolean hidden = ClearTransitions.relation(e) == Visibility.Mode.HIDDEN;
            float lens = Mth.sin((float) Math.PI * phase) * (hidden ? 1.0F : 0.85F);
            this.lens(f, ox, oy, oz, rx, rz, h, lens * 0.09F);
            this.glintLine(f, ox, oy, oz, rx, rz, h, phase);
         }
      }
   }

   private void lens(VfxFrame f, float ox, float oy, float oz, float rx, float rz, float h, float strength) {
      BufferBuilder d = f.distortion();
      if (d != null && !(strength <= 0.002F)) {
         for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < 6; col++) {
               lensVertex(d, ox, oy, oz, rx, rz, h, row, col, strength);
               lensVertex(d, ox, oy, oz, rx, rz, h, row, col + 1, strength);
               lensVertex(d, ox, oy, oz, rx, rz, h, row + 1, col + 1, strength);
               lensVertex(d, ox, oy, oz, rx, rz, h, row + 1, col, strength);
            }
         }
      }
   }

   private static void lensVertex(BufferBuilder d, float ox, float oy, float oz, float rx, float rz, float h, int row, int col, float strength) {
      float across = (float)col / 6.0F * 2.0F - 1.0F;
      float up = (float)row / (float)ROWS;
      float w = WIDTH[row] * h * 1.15F;
      float x = ox + rx * across * w;
      float y = oy + up * h * 1.04F;
      float z = oz + rz * across * w;
      float edge = 1.0F - across * across;
      float ends = Mth.sin((float) Math.PI * Mth.clamp(up, 0.02F, 0.98F));
      MeshWriter.distortVertex(d, x, y, z, across * 0.9F, (up - 0.5F) * 0.25F, strength * edge * ends, 0.0F, 2);
   }

   private void glintLine(VfxFrame f, float ox, float oy, float oz, float rx, float rz, float h, float phase) {
      if (f.allowMesh()) {
         float s = this.vanish ? (phase - 0.05F) / 0.4F : (0.42F - phase) / 0.4F;
         if (!(s <= 0.0F) && !(s >= 1.0F)) {
            float k = s * s * (3.0F - 2.0F * s);
            float fade = Mth.sin((float) Math.PI * s);
            float y = oy + h * (1.02F - k * 1.02F);
            float rel = y - oy;
            int row = Mth.clamp((int)(rel / h * (float)ROWS), 0, ROWS);
            float w = WIDTH[row] * h * 1.2F;
            BufferBuilder b = f.mesh(MeshTex.FLAME_RIBBON);
            line(b, ox, y, oz, rx, rz, w, 0.032F, MeshWriter.argb(16056312, fade), 3.6F, f.time);
            line(b, ox, y, oz, rx, rz, w * 1.2F, 0.14F, MeshWriter.argb(12578784, 0.45F * fade), 1.6F, f.time);
         }
      }
   }

   private static void line(BufferBuilder b, float cx, float cy, float cz, float rx, float rz, float half, float width, int argb, float emissive, float time) {
      for (int i = 0; i < 3; i++) {
         float t = (float)i - 1.0F;
         XS[i] = cx + rx * t * half;
         YS[i] = cy;
         ZS[i] = cz + rz * t * half;
         WS[i] = i == 1 ? width : width * 0.35F;
         AS[i] = (i == 1 ? 1.0F : 0.15F) * ((float)(argb >>> 24) / 255.0F);
      }

      MeshWriter.strip(b, 3, XS, YS, ZS, WS, HS, AS, time * 0.05F, 0.5F, argb & 16777215, 0, emissive);
   }

   @Override
   public double x() {
      return this.last.x;
   }

   @Override
   public double y() {
      return this.last.y;
   }

   @Override
   public double z() {
      return this.last.z;
   }
}
