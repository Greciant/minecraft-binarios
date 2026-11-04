package com.devilfruits.fruits.gura_gura.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.fx.GroundWave;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.client.vfx.render.MeshTex;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.mojang.blaze3d.vertex.BufferBuilder;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class AuraCinematic extends QuakeCinematic {
   private int stopped = -1;
   private final ShockRing.Band band = new ShockRing.Band();

   public AuraCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      if (!Double.isNaN(this.groundY)) {
         GroundWave.at(this.x, this.groundY, this.z, 3.2F, 9).strength(1.0F).ring(15398655, 0.3F).spawn();
      }

      ShockRing.at(new Vec3(this.x, this.y + (double)this.height * 0.55, this.z), new Vec3(0.0, 1.0, 0.0))
         .radius(0.4F, 2.6F)
         .life(9)
         .strength(1.0F)
         .band(0.35F)
         .rim(0.15F, 15398655)
         .spawn();
   }

   @Nullable
   public static AuraCinematic of(Entity e) {
      return VfxDirector.find(AuraCinematic.class, c -> c.follows(e) && c.stopped < 0);
   }

   public void stop() {
      if (this.stopped < 0) {
         this.stopped = this.age;
      }
   }

   private float strength(float t) {
      float in = Math.min(1.0F, t / 6.0F);
      float out = this.stopped < 0 ? 1.0F : Math.max(0.0F, 1.0F - (t - (float)this.stopped) / 6.0F);
      return in * out;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = this.caster(level);
      if (e != null && (this.stopped < 0 || this.age - this.stopped <= 6)) {
         if (this.stopped < 0) {
            double cy = this.y + (double)this.height * 0.55;
            float sc = this.height / 1.8F;
            if (this.age % 7 == 2 && !this.firstPersonCaster()) {
               double a = this.random.nextDouble() * (float) (Math.PI * 2);
               double h = this.spread(0.7) * (double)sc;
               Vec3 p = new Vec3(this.x + Math.cos(a) * 0.95 * (double)sc, cy + h, this.z + Math.sin(a) * 0.95 * (double)sc);
               ShockRing.at(p, new Vec3(Math.cos(a), 0.0, Math.sin(a)))
                  .radius(0.15F, 0.9F * sc)
                  .life(9)
                  .strength(1.1F)
                  .band(0.5F)
                  .twist((this.random.nextFloat() - 0.5F) * 2.0F)
                  .rim(0.0F, 15398655)
                  .spawn();
            }

            if (this.age % 20 == 10 && !Double.isNaN(this.groundY) && this.y - this.groundY < 2.0) {
               GroundWave.at(this.x, this.groundY, this.z, 3.0F * sc, 12).strength(0.6F).spawn();
            }
         }
      } else {
         this.finish();
      }
   }

   @Override
   public void render(VfxFrame f) {
      float t = (float)this.age + f.partial;
      float s = this.strength(t);
      if (!(s <= 0.01F)) {
         Entity e = this.caster();
         float p = f.entityPartial;
         double ex = e != null ? Mth.lerp((double)p, e.xo, e.getX()) : this.x;
         double ey = e != null ? Mth.lerp((double)p, e.yo, e.getY()) : this.y;
         double ez = e != null ? Mth.lerp((double)p, e.zo, e.getZ()) : this.z;
         float cx = f.rx(ex);
         float cy = f.ry(ey + (double)this.height * 0.52);
         float cz = f.rz(ez);
         float sc = this.height / 1.8F;
         float pulse = 0.8F + 0.2F * Mth.sin(t * 0.9F) + 0.1F * Mth.sin(t * 2.3F);
         float r = 1.05F * sc * (this.firstPersonCaster() ? 1.6F : 1.0F);
         this.band.draw(f, cx, cy, cz, -f.left.x, -f.left.y, -f.left.z, f.up.x, f.up.y, f.up.z, r * 1.25F, r * 0.32F, 0.55F * s * pulse, 0.0F);
         if (f.allowMesh() && !this.firstPersonCaster()) {
            BufferBuilder b = f.mesh(MeshTex.LANCE);
            MeshWriter.ring(
               b,
               cx,
               cy,
               cz,
               -f.left.x,
               -f.left.y,
               -f.left.z,
               f.up.x,
               f.up.y,
               f.up.z,
               r * 1.25F,
               0.025F,
               40,
               t * 0.05F,
               1.0F,
               MeshWriter.argb(15398655, 0.18F * s * pulse),
               -1.0F,
               0,
               1.2F,
               0.0F
            );
         }
      }
   }

   @Override
   public double y() {
      return this.y + (double)this.height * 0.5;
   }
}
