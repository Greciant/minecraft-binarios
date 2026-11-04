package com.devilfruits.fruits.doru_doru.client.vfx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.fruits.doru_doru.DoruItems;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;

public class PourCinematic extends Cinematic {
   private static final int SEGMENTS = 14;
   private final int entityId;
   private final long[] path;
   private final int total;
   private final int rate;
   private final long start;
   private final boolean carve;
   private final int tint;
   private final int tendrils;
   private final Vec3[] aim;
   private final Vec3[] aimPrev;
   private final RandomSource random;
   private int ended = -1;
   private int cursor;
   private double cx;
   private double cy;
   private double cz;
   private final float[] xs = new float[15];
   private final float[] ys = new float[15];
   private final float[] zs = new float[15];
   private final float[] ws = new float[15];
   private final float[] hs = new float[15];
   private final float[] as = new float[15];

   public PourCinematic(Entity user, long[] path, int total, int rate, long start, boolean carve, int color) {
      super(VfxAnchor.of(user, user.position()), VfxParams.NONE);
      this.entityId = user.getId();
      this.path = path;
      this.total = Math.max(1, total);
      this.rate = Math.max(1, rate);
      this.start = start;
      this.carve = carve;
      this.tint = DoruLook.tint(color);
      this.tendrils = Mth.clamp(2 + this.rate / 96, 2, 6);
      this.aim = new Vec3[this.tendrils];
      this.aimPrev = new Vec3[this.tendrils];
      this.random = RandomSource.create(start ^ (long)total);
      Vec3 c = path.length == 0 ? user.position() : this.centre(0);

      for (int i = 0; i < this.tendrils; i++) {
         this.aim[i] = c;
         this.aimPrev[i] = c;
      }

      this.cx = c.x;
      this.cy = c.y;
      this.cz = c.z;
   }

   public int entityId() {
      return this.entityId;
   }

   public void end() {
      if (this.ended < 0) {
         this.ended = this.age;
      }
   }

   private Vec3 centre(int i) {
      double f = this.total <= 1 ? 0.0 : (double)Mth.clamp(i, 0, this.total - 1) / (double)(this.total - 1) * (double)(this.path.length - 1);
      int a = (int)Math.floor(f);
      int b = Math.min(this.path.length - 1, a + 1);
      double t = f - (double)a;
      long pa = this.path[a];
      long pb = this.path[b];
      double x = (double)BlockPos.getX(pa) + (double)(BlockPos.getX(pb) - BlockPos.getX(pa)) * t;
      double y = (double)BlockPos.getY(pa) + (double)(BlockPos.getY(pb) - BlockPos.getY(pa)) * t;
      double z = (double)BlockPos.getZ(pa) + (double)(BlockPos.getZ(pb) - BlockPos.getZ(pa)) * t;
      return new Vec3((x + 0.5) / 4.0, (y + 0.5) / 4.0, (z + 0.5) / 4.0);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = this.anchor.entity(level);
      if (this.path.length != 0 && e != null) {
         this.cursor = (int)Math.min((long)this.total, (level.getGameTime() - this.start + 1L) * (long)this.rate);
         if (this.ended < 0 && this.cursor >= this.total && level.getGameTime() - this.start > (long)(this.total / this.rate + 40)) {
            this.end();
         }

         if (this.ended >= 0 && this.age - this.ended > 8) {
            this.finish();
         } else {
            boolean live = this.ended < 0 && this.cursor < this.total;

            for (int k = 0; k < this.tendrils; k++) {
               this.aimPrev[k] = this.aim[k];
               int idx = live
                  ? this.cursor - this.rate + (int)(((double)k + 0.5) / (double)this.tendrils * (double)this.rate)
                  : this.total - 1 - k * Math.max(1, this.total / (this.tendrils * 3));
               Vec3 want = this.centre(idx);
               this.aim[k] = this.aim[k].add(want.subtract(this.aim[k]).scale(0.4));
            }

            Vec3 front = this.centre(Math.max(0, this.cursor - 1));
            this.cx = front.x;
            this.cy = front.y;
            this.cz = front.z;
            boolean local = VfxWorld.local(this.entityId);
            if (live && local) {
               CameraDirector.dolly(0.965F, 0.04F);
            }

            if (VfxSettings.legacy()) {
               if (live && this.random.nextInt(2) == 0) {
                  level.addParticle(
                     new ItemParticleOption(ParticleTypes.ITEM, new ItemStack((ItemLike)DoruItems.WAX_LUMP.get())), front.x, front.y, front.z, 0.0, 0.05, 0.0
                  );
               }
            } else if (!live) {
               if (this.ended >= 0 && this.age == this.ended + 1 && !this.carve) {
                  for (int i = 0; i < VfxSettings.count(14.0F); i++) {
                     Vec3 p = this.centre(this.random.nextInt(this.total));
                     VfxParticles.emit(DoruLook.GLINT, p.x, p.y + 0.15, p.z, 0.0, 0.01, 0.0).rotation(this.random.nextFloat() * (float) (Math.PI * 2));
                  }

                  for (int i = 0; i < VfxSettings.count(3.0F); i++) {
                     Vec3 p = this.centre(this.random.nextInt(this.total));
                     VfxParticles.emit(DoruLook.STEAM_PUFF, p.x, p.y + 0.3, p.z, 0.0, 0.02, 0.0).tint(15921388).alpha(0.35F);
                  }
               }
            } else {
               float light = VfxWorld.light(level, front.x, front.y, front.z);
               VfxParticles.emit(DoruLook.FRONT_GLOW, front.x, front.y, front.z, 0.0, 0.0, 0.0).tint(16767400).alpha(this.carve ? 0.25F : 0.55F);
               if (!this.carve && this.age % 3 == 0) {
                  Vec3 p = this.centre(this.cursor - 1 - this.random.nextInt(Math.max(1, this.rate)));
                  VfxParticles.emit(DoruLook.SPLASH, p.x, p.y + 0.05, p.z, 0.0, 0.0, 0.0)
                     .tint(VfxWorld.shade(this.tint, light))
                     .size(0.5F + this.random.nextFloat() * 0.4F)
                     .rotation(this.random.nextFloat() * (float) (Math.PI * 2));
               }

               for (int k = 0; k < this.tendrils; k++) {
                  if (this.random.nextFloat() < 0.3F) {
                     Vec3 h = DoruLook.hand((Player)e, 1.0F, k % 2 == 0, local && Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON);
                     Vec3 p = curve(h, this.aim[k], 0.35 + this.random.nextDouble() * 0.45);
                     VfxParticles.emit(DoruLook.BEAD, p.x, p.y, p.z, (this.random.nextDouble() - 0.5) * 0.02, -0.02, (this.random.nextDouble() - 0.5) * 0.02)
                        .tint(VfxWorld.shade(this.tint, light));
                  }
               }

               if (!this.carve) {
                  for (int i = 0; i < VfxSettings.count(2.0F); i++) {
                     int idx = this.cursor - this.rate * (3 + this.random.nextInt(3));
                     if (idx > 0) {
                        Vec3 p = this.centre(idx);
                        VfxParticles.emit(DoruLook.GLINT, p.x, p.y + 0.12, p.z, 0.0, 0.0, 0.0)
                           .rotation(this.random.nextFloat() * (float) (Math.PI * 2))
                           .alpha(0.8F);
                     }
                  }

                  if (this.random.nextFloat() < 0.18F) {
                     Vec3 p = this.centre(Math.max(0, this.cursor - this.rate * 6));
                     VfxParticles.emit(DoruLook.STEAM_PUFF, p.x, p.y + 0.2, p.z, 0.0, 0.015, 0.0).tint(15921388).alpha(0.3F);
                  }
               }
            }
         }
      } else {
         this.finish();
      }
   }

   private static Vec3 curve(Vec3 h, Vec3 a, double t) {
      double d = h.distanceTo(a);
      Vec3 dir = a.subtract(h);
      Vec3 p1 = h.add(dir.scale(0.3)).add(0.0, 0.25 + 0.08 * d, 0.0);
      Vec3 p2 = a.add(0.0, 0.6 + 0.12 * d, 0.0);
      double u = 1.0 - t;
      return h.scale(u * u * u).add(p1.scale(3.0 * u * u * t)).add(p2.scale(3.0 * u * t * t)).add(a.scale(t * t * t));
   }

   @Override
   public void render(VfxFrame f) {
      if (!VfxSettings.legacy() && f.allowMesh()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null && mc.level.getEntity(this.entityId) instanceof Player player) {
            float fade = this.ended < 0
               ? Math.min(1.0F, ((float)this.age + f.partial) / 4.0F)
               : Math.max(0.0F, 1.0F - ((float)(this.age - this.ended) + f.partial) / 8.0F);
            if (!(fade <= 0.01F)) {
               boolean fp = VfxWorld.local(this.entityId) && mc.options.getCameraType() == CameraType.FIRST_PERSON;
               float light = VfxWorld.light(mc.level, this.cx, this.cy, this.cz);
               int rgb = VfxWorld.shade(this.tint, Math.max(0.35F, light));
               BufferBuilder b = f.mesh(MeshTex.WAX_STREAM);

               for (int k = 0; k < this.tendrils; k++) {
                  Vec3 h = DoruLook.hand(player, f.entityPartial, k % 2 == 0, fp);
                  Vec3 a = this.aimPrev[k].lerp(this.aim[k], (double)f.partial);
                  double reach = this.ended < 0 ? 1.0 : Math.max(0.05, 1.0 - (double)((float)(this.age - this.ended) + f.partial) / 8.0);

                  for (int i = 0; i <= 14; i++) {
                     double t = (double)i / 14.0 * reach;
                     Vec3 p = curve(h, a, t);
                     this.xs[i] = f.rx(p.x);
                     this.ys[i] = f.ry(p.y);
                     this.zs[i] = f.rz(p.z);
                     double s = (double)i / 14.0;
                     float bulge = (float)(0.025 * Math.sin(s * 6.0 - (double)((float)this.age + f.partial) * 0.35 + (double)k));
                     this.ws[i] = (float)(0.08 + 0.1 * Math.sin(Math.PI * Math.min(1.0, s * 1.1)) + 1.4 * (double)bulge) * (fp && s < 0.2 ? 0.6F : 1.0F);
                     this.hs[i] = -1.0F;
                     this.as[i] = fade * (fp && s < 0.08 ? (float)(s / 0.08) : 1.0F);
                  }

                  float scroll = ((float)this.age + f.partial) * (this.carve ? 0.12F : -0.12F);
                  MeshWriter.strip(b, 15, this.xs, this.ys, this.zs, this.ws, this.hs, this.as, scroll, 0.22F, rgb, 0, 0.05F);
               }
            }
         }
      }
   }

   @Override
   public double x() {
      return this.cx;
   }

   @Override
   public double y() {
      return this.cy;
   }

   @Override
   public double z() {
      return this.cz;
   }
}
