package com.devilfruits.fruits.gura_gura.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.fx.GlassCrack;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.fruits.gura_gura.entity.QuakeSlashEntity;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class SlashCinematic extends QuakeCinematic {
   private static final int N = 24;
   private static final float ARC = 1.25F;
   private final int slashId;
   private float width = 3.0F;
   private float yaw;
   private float pitch;
   private float roll;
   private double px;
   private double py;
   private double pz;
   private double opx;
   private double opy;
   private double opz;
   private int gone = -1;
   private final float[] jitter = new float[24];
   private final float[] branchAt = new float[5];
   private final float[] branchLen = new float[5];
   private final float[] branchSide = new float[5];
   private final float[] sx = new float[24];
   private final float[] sy = new float[24];
   private final float[] sz = new float[24];
   private final float[] sw = new float[24];
   private final float[] sh = new float[24];
   private final float[] sa = new float[24];

   public SlashCinematic(ClientLevel level, QuakeSlashEntity slash) {
      super(level, new VfxAnchor(slash.getId(), slash.position()), new VfxParams(slash.position(), Vec3.ZERO, 0.0F, (long)slash.getId() * 7919L, 0));
      this.slashId = slash.getId();
      this.pull(slash);
      this.opx = this.px;
      this.opy = this.py;
      this.opz = this.pz;
      RandomSource r = RandomSource.create(this.seed);

      for (int b = 0; b < this.branchAt.length; b++) {
         this.branchAt[b] = 0.1F + r.nextFloat() * 0.8F;
         this.branchLen[b] = 0.12F + r.nextFloat() * 0.18F;
         this.branchSide[b] = r.nextBoolean() ? 1.0F : -1.0F;
      }
   }

   public static SlashCinematic of(Entity e) {
      return VfxDirector.find(SlashCinematic.class, c -> c.slashId == e.getId());
   }

   private void pull(QuakeSlashEntity s) {
      this.px = s.getX();
      this.py = s.getY();
      this.pz = s.getZ();
      this.width = s.width();
      this.yaw = s.getYRot() * (float) (Math.PI / 180.0);
      this.pitch = s.getXRot() * (float) (Math.PI / 180.0);
      this.roll = s.roll();
   }

   @Override
   public void tick(ClientLevel level) {
      label49: {
         super.tick(level);
         this.opx = this.px;
         this.opy = this.py;
         this.opz = this.pz;
         if (level.getEntity(this.slashId) instanceof QuakeSlashEntity s && !s.isRemoved()) {
            this.pull(s);
            break label49;
         }

         if (this.gone < 0) {
            this.gone = this.age;
         }
      }

      if (this.gone >= 0 && this.age - this.gone > 5) {
         this.finish();
      } else {
         for (int i = 0; i < 24; i++) {
            this.jitter[i] = (this.random.nextFloat() - 0.5F) * 0.07F;
         }

         if (this.gone < 0) {
            float r = this.width * 0.55F;
            Vec3 f = this.forward();
            if (this.age % 2 == 0) {
               float a = (this.random.nextFloat() * 2.0F - 1.0F) * 1.25F;
               Vec3 p = this.arcPoint(a, r);
               GlassCrack.at(p, f.scale(-1.0), 0.35F * r, this.seed + (long)this.age * 13L)
                  .pattern(8, 2)
                  .grow(1)
                  .hold(0)
                  .shatter(f.scale(-0.15))
                  .refraction(0.8F)
                  .lines(0.035F)
                  .tint(15398655)
                  .spawn();
            }

            for (int i = 0; i < VfxSettings.count(4.0F); i++) {
               Vec3 p = this.arcPoint((this.random.nextFloat() * 2.0F - 1.0F) * 1.25F, r);
               VfxParticles.emit(
                     i % 2 == 0 ? VfxPresets.GLASS_GLINT : VfxPresets.GLASS_DUST,
                     p.x,
                     p.y,
                     p.z,
                     -f.x * 0.2 + this.spread(0.08),
                     this.spread(0.08),
                     -f.z * 0.2 + this.spread(0.08)
                  )
                  .tint(14217983);
            }
         }
      }
   }

   private Vec3 forward() {
      return new Vec3((double)(Mth.sin(this.yaw) * Mth.cos(this.pitch)), (double)Mth.sin(this.pitch), (double)(Mth.cos(this.yaw) * Mth.cos(this.pitch)));
   }

   private Vec3 side() {
      Vec3 f = this.forward();
      Vec3 right = new Vec3((double)(-Mth.cos(this.yaw)), 0.0, (double)Mth.sin(this.yaw));
      Vec3 up = right.cross(f).normalize();
      return right.scale((double)Mth.cos(this.roll)).add(up.scale((double)Mth.sin(this.roll)));
   }

   private Vec3 arcPoint(float a, float r) {
      Vec3 f = this.forward();
      Vec3 s = this.side();
      Vec3 c = new Vec3(this.px, this.py, this.pz).subtract(f.scale((double)r * 0.85));
      return c.add(f.scale((double)(Mth.cos(a) * r))).add(s.scale((double)(Mth.sin(a) * r)));
   }

   @Override
   public void render(VfxFrame f) {
      float t = (float)this.age + f.partial;
      float fade = Math.min(1.0F, t / 1.5F) * (this.gone < 0 ? 1.0F : Math.max(0.0F, 1.0F - (t - (float)this.gone) / 5.0F));
      if (!(fade <= 0.01F) && f.allowMesh()) {
         float r = this.width * 0.55F;
         Vec3 fw = this.forward();
         Vec3 sd = this.side();
         float bx = f.rx(Mth.lerp((double)f.partial, this.opx, this.px) - fw.x * (double)r * 0.85);
         float by = f.ry(Mth.lerp((double)f.partial, this.opy, this.py) - fw.y * (double)r * 0.85);
         float bz = f.rz(Mth.lerp((double)f.partial, this.opz, this.pz) - fw.z * (double)r * 0.85);

         for (int i = 0; i < 24; i++) {
            float u = (float)i / 23.0F;
            float a = (u * 2.0F - 1.0F) * 1.25F;
            float rr = r * (1.0F + this.jitter[i]);
            float c = Mth.cos(a);
            float s = Mth.sin(a);
            this.sx[i] = bx + (float)(fw.x * (double)c + sd.x * (double)s) * rr;
            this.sy[i] = by + (float)(fw.y * (double)c + sd.y * (double)s) * rr;
            this.sz[i] = bz + (float)(fw.z * (double)c + sd.z * (double)s) * rr;
            float taper = Mth.sin(u * (float) Math.PI);
            this.sw[i] = 0.07F * r * (0.25F + 0.75F * taper);
            this.sh[i] = -1.0F;
            this.sa[i] = fade * (0.3F + 0.7F * taper) * VfxWorld.nearFade(this.sx[i], this.sy[i], this.sz[i]);
         }

         BufferBuilder lance = f.mesh(MeshTex.LANCE);
         float[] wide = new float[24];
         float[] soft = new float[24];

         for (int i = 0; i < 24; i++) {
            wide[i] = this.sw[i] * 4.0F;
            soft[i] = this.sa[i] * 0.3F;
         }

         MeshWriter.strip(lance, 24, this.sx, this.sy, this.sz, wide, this.sh, soft, 0.0F, 0.4F, 15398655, 0, 1.0F);
         MeshWriter.strip(lance, 24, this.sx, this.sy, this.sz, this.sw, this.sh, this.sa, 0.0F, 0.4F, 15398655, 0, 3.2F);

         for (int b = 0; b < this.branchAt.length; b++) {
            int i = Mth.clamp(Math.round(this.branchAt[b] * 23.0F), 0, 23);
            float a = (this.branchAt[b] * 2.0F - 1.0F) * 1.25F;
            float out = this.branchSide[b] * this.branchLen[b] * r;
            float c = Mth.cos(a);
            float s = Mth.sin(a);
            float ex = this.sx[i] + (float)(fw.x * (double)c + sd.x * (double)s) * out + (float)fw.x * out * 0.4F;
            float ey = this.sy[i] + (float)(fw.y * (double)c + sd.y * (double)s) * out + (float)fw.y * out * 0.4F;
            float ez = this.sz[i] + (float)(fw.z * (double)c + sd.z * (double)s) * out + (float)fw.z * out * 0.4F;
            float[] bxs = new float[]{this.sx[i], (this.sx[i] + ex) * 0.5F + this.jitter[b] * r, ex};
            float[] bys = new float[]{this.sy[i], (this.sy[i] + ey) * 0.5F, ey};
            float[] bzs = new float[]{this.sz[i], (this.sz[i] + ez) * 0.5F - this.jitter[b] * r, ez};
            float[] bw = new float[]{this.sw[i] * 0.6F, this.sw[i] * 0.4F, this.sw[i] * 0.15F};
            float[] bh = new float[]{-1.0F, -1.0F, -1.0F};
            float[] ba = new float[]{this.sa[i] * 0.9F, this.sa[i] * 0.7F, this.sa[i] * 0.3F};
            MeshWriter.strip(lance, 3, bxs, bys, bzs, bw, bh, ba, 0.0F, 0.4F, 15398655, 0, 2.4F);
         }

         BufferBuilder d = f.distortion();
         if (d != null) {
            float lx = -f.left.x;
            float ly = -f.left.y;
            float lz = -f.left.z;
            float w = 0.28F * r;

            for (int i = 0; i < 23; i++) {
               for (int q = 0; q < 4; q++) {
                  int k = i + (q != 1 && q != 2 ? 0 : 1);
                  float u = (float)k / 23.0F;
                  float a = (u * 2.0F - 1.0F) * 1.25F;
                  float across = q >= 2 ? 1.0F : 0.0F;
                  float rr = r + (across * 2.0F - 1.0F) * w;
                  float c = Mth.cos(a);
                  float s = Mth.sin(a);
                  float dx = (float)(fw.x * (double)c + sd.x * (double)s);
                  float dy = (float)(fw.y * (double)c + sd.y * (double)s);
                  float dz = (float)(fw.z * (double)c + sd.z * (double)s);
                  float ox = dx * lx + dy * ly + dz * lz;
                  float oy = dx * f.up.x + dy * f.up.y + dz * f.up.z;
                  float ol = Mth.sqrt(ox * ox + oy * oy) + 1.0E-4F;
                  MeshWriter.distortVertex(
                     d, bx + dx * rr, by + dy * rr, bz + dz * rr, ox / ol, oy / ol, 0.75F * fade * Mth.sin(u * (float) Math.PI), across, 3
                  );
               }
            }
         }
      }
   }

   @Override
   public double x() {
      return this.px;
   }

   @Override
   public double y() {
      return this.py;
   }

   @Override
   public double z() {
      return this.pz;
   }
}
