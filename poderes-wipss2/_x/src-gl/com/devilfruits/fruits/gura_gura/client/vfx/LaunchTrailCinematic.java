package com.devilfruits.fruits.gura_gura.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.fx.GlassCrack;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class LaunchTrailCinematic extends QuakeCinematic {
   private static final int TRAIL = 8;
   private final int duration;
   private final double[] px = new double[8];
   private final double[] py = new double[8];
   private final double[] pz = new double[8];
   private int count;

   public LaunchTrailCinematic(ClientLevel level, VfxAnchor anchor, VfxParams params) {
      super(level, anchor, params);
      this.duration = Math.max(5, params.extra());
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = this.caster(level);
      if (e != null && this.age <= this.duration) {
         Vec3 c = e.getBoundingBox().getCenter();
         System.arraycopy(this.px, 0, this.px, 1, 7);
         System.arraycopy(this.py, 0, this.py, 1, 7);
         System.arraycopy(this.pz, 0, this.pz, 1, 7);
         this.px[0] = c.x;
         this.py[0] = c.y;
         this.pz[0] = c.z;
         this.count = Math.min(8, this.count + 1);
         Vec3 v = this.count > 1 ? new Vec3(this.px[0] - this.px[1], this.py[0] - this.py[1], this.pz[0] - this.pz[1]) : Vec3.ZERO;
         double speed = v.length();
         float k = (float)Math.min(1.0, speed / 1.2);
         if (!(k < 0.08F)) {
            if (this.age % 3 == 1) {
               GlassCrack.at(c.subtract(v.scale(0.6)), v.scale(-1.0), 0.5F + 0.5F * k, this.seed + (long)this.age * 31L)
                  .pattern(8, 2)
                  .grow(1)
                  .hold(1)
                  .shatter(v.scale(-0.08))
                  .refraction(0.8F)
                  .lines(0.03F)
                  .tint(15398655)
                  .spawn();
            }

            for (int i = 0; i < VfxSettings.count(3.0F * k); i++) {
               VfxParticles.emit(
                     i % 2 == 0 ? VfxPresets.GLASS_GLINT : VfxPresets.GLASS_DUST,
                     c.x + this.spread(0.4),
                     c.y + this.spread(0.5),
                     c.z + this.spread(0.4),
                     -v.x * 0.15 + this.spread(0.08),
                     -v.y * 0.15 + this.spread(0.08),
                     -v.z * 0.15 + this.spread(0.08)
                  )
                  .tint(14217983);
            }
         }
      } else {
         this.finish();
      }
   }

   @Override
   public void render(VfxFrame f) {
      BufferBuilder d = f.distortion();
      if (d != null && this.count >= 2) {
         float fade = Math.max(0.0F, 1.0F - (float)this.age / (float)this.duration);
         VfxSprites.Sprite glow = VfxSprites.GLOW;

         for (int i = 0; i < this.count; i++) {
            float k = 1.0F - (float)i / 8.0F;
            MeshWriter.distortQuad(
               d,
               f.rx(this.px[i]),
               f.ry(this.py[i]),
               f.rz(this.pz[i]),
               0.9F * k + 0.3F,
               0.8F * k * fade,
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
               (float)i * 0.17F + f.time * 0.01F,
               0.2F
            );
         }
      }
   }
}
