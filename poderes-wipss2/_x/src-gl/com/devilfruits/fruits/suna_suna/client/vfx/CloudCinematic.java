package com.devilfruits.fruits.suna_suna.client.vfx;

import com.devilfruits.client.logia.LogiaMovementClient;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.fruits.suna_suna.SandTravel;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class CloudCinematic extends SandCinematic {
   private static final int TAIL = 12;
   private int leftAt = -1;
   private double cx;
   private double cy;
   private double cz;
   private double pcx;
   private double pcy;
   private double pcz;
   private int recorded;
   private final double[] hx = new double[12];
   private final double[] hy = new double[12];
   private final double[] hz = new double[12];
   private final double[] px = new double[13];
   private final double[] py = new double[13];
   private final double[] pz = new double[13];
   private final float[] w = new float[13];
   private final float[] a = new float[13];

   public CloudCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.cx = this.pcx = this.x;
      this.cy = this.pcy = this.y + 0.9;
      this.cz = this.pcz = this.z;
   }

   @Nullable
   public static CloudCinematic of(Entity e) {
      return VfxDirector.find(CloudCinematic.class, c -> c.follows(e) && c.leftAt < 0);
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      this.pcx = this.cx;
      this.pcy = this.cy;
      this.pcz = this.cz;
      Entity e = this.caster(level);
      if (e instanceof Player player && !e.isRemoved() && SandTravel.CLOUD.id().equals(LogiaMovementClient.modeOf(player))) {
         this.cx = e.getX();
         this.cy = e.getY() + 0.9;
         this.cz = e.getZ();
         System.arraycopy(this.hx, 0, this.hx, 1, 11);
         System.arraycopy(this.hy, 0, this.hy, 1, 11);
         System.arraycopy(this.hz, 0, this.hz, 1, 11);
         this.hx[0] = this.cx;
         this.hy[0] = this.cy;
         this.hz[0] = this.cz;
         this.recorded = Math.min(12, this.recorded + 1);
         int tint = lit(level, this.cx, this.cy, this.cz);
         boolean inside = this.firstPersonCaster();
         Vec3 v = e.getDeltaMovement();
         int n = VfxSettings.count(inside ? 1.0F : 7.0F);

         for (int i = 0; i < n; i++) {
            Vec3 d = jitter(this.random, Vec3.ZERO, 1.0F);
            double r = inside ? 1.6 + this.random.nextDouble() * 0.6 : 0.15 + this.random.nextDouble() * 0.75;
            double sx = this.cx + d.x * r;
            double sy = this.cy + d.y * r * 0.7;
            double sz = this.cz + d.z * r;
            double tang = 0.08;
            plume(this.random, sx, sy, sz, v.x * 0.6 - d.z * tang, v.y * 0.4 + 0.005, v.z * 0.6 + d.x * tang, tint)
               .size(inside ? 0.8F : 0.8F + this.random.nextFloat() * 0.5F)
               .life(0.4F)
               .alpha(inside ? 0.35F : 1.0F);
            if (!inside && i % 3 == 0) {
               puff(this.random, sx, sy, sz, v.x * 0.3 - d.z * 0.12, 0.01, v.z * 0.3 + d.x * 0.12, tint).size(0.7F);
            }
         }

         for (int ix = 0; ix < VfxSettings.count(inside ? 1.0F : 3.0F); ix++) {
            grains(
               this.random,
               this.cx + this.spread(0.8),
               this.cy - 0.4 + this.spread(0.3),
               this.cz + this.spread(0.8),
               v.x * 0.5 + this.spread(0.03),
               -0.04,
               v.z * 0.5 + this.spread(0.03),
               tint
            );
         }

         shadow(level, this.cx, this.cy, this.cz, 2.2F, 0.8F);
         return;
      }

      if (this.leftAt < 0) {
         this.leftAt = this.age;
      }

      if (this.age - this.leftAt > 8) {
         this.finish();
      }
   }

   @Override
   public void render(VfxFrame f) {
      float pt = f.partial;
      float t = (float)this.age + pt;
      float fade = this.leftAt < 0 ? Math.min(1.0F, t / 5.0F) : Math.max(0.0F, 1.0F - ((float)(this.age - this.leftAt) + pt) / 8.0F);
      if (!(fade <= 0.01F)) {
         Entity e = this.caster();
         double ex = e != null ? Mth.lerp((double)f.entityPartial, e.xo, e.getX()) : Mth.lerp((double)pt, this.pcx, this.cx);
         double ey = e != null ? Mth.lerp((double)f.entityPartial, e.yo, e.getY()) + 0.9 : Mth.lerp((double)pt, this.pcy, this.cy);
         double ez = e != null ? Mth.lerp((double)f.entityPartial, e.zo, e.getZ()) : Mth.lerp((double)pt, this.pcz, this.cz);
         ClientLevel level = Minecraft.getInstance().level;
         int tint = level != null ? lit(level, ex, ey, ez) : 16777215;
         boolean inside = this.firstPersonCaster();
         if (this.recorded >= 3) {
            int n = 0;
            this.px[n] = ex;
            this.py[n] = ey;
            this.pz[n] = ez;
            this.w[n] = 0.6F;
            this.a[n] = 0.75F * fade;
            n++;

            for (int i = 1; i < this.recorded; i++) {
               float q = (float)i / 11.0F;
               this.px[n] = this.hx[i];
               this.py[n] = this.hy[i];
               this.pz[n] = this.hz[i];
               this.w[n] = 0.6F + 0.5F * q;
               this.a[n] = (1.0F - q) * (1.0F - q) * 0.7F * fade;
               n++;
            }

            SandDraw.stream(f, n, this.px, this.py, this.pz, this.w, this.a, -t * 0.25F, 0.3F, tint);
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
