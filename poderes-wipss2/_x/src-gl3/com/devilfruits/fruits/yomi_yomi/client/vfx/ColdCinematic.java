package com.devilfruits.fruits.yomi_yomi.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.fruits.yomi_yomi.YomiSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class ColdCinematic extends YomiCinematic {
   private final boolean freeze;
   private final int ticks;
   private final float height;

   private ColdCinematic(Entity target, Vec3 at, boolean freeze, int ticks, float height, long seed) {
      super(VfxAnchor.of(target, at), at, seed);
      this.freeze = freeze;
      this.ticks = ticks;
      this.height = height;
   }

   public static ColdCinematic burst(ClientLevel level, Entity target, Vec3 center, float height, long seed) {
      ColdCinematic c = new ColdCinematic(target, center, false, 24, height, seed);
      Vec3 feet = target.position();
      float s = Math.max(0.6F, Math.min(1.6F, height * 0.55F));
      String[] clusters = new String[]{"ice_cluster_a", "ice_cluster_c", "ice_cluster_b"};

      for (int i = 0; i < 3; i++) {
         double a = ((double)i + c.random.nextDouble() * 0.4) / 3.0 * Math.PI * 2.0;
         double r = Math.max(0.5, (double)target.getBbWidth() * 0.6 + 0.25);
         Vec3 p = feet.add(Math.cos(a) * r, 0.0, Math.sin(a) * r);
         double top = VfxWorld.ground(level, p.x, p.y + 0.8, p.z, 3);
         if (!Double.isNaN(top)) {
            c.ice
               .add(
                  new IceProp(
                     clusters[i],
                     new Vec3(p.x, top - 0.05, p.z),
                     (float)Math.toDegrees(a) - 90.0F,
                     14.0F,
                     s * (0.8F + c.random.nextFloat() * 0.3F),
                     (float)i,
                     14.0F,
                     c.random.nextLong()
                  )
               );
         }
      }

      for (int ix = 0; ix < 14; ix++) {
         double a = c.random.nextDouble() * Math.PI * 2.0;
         double r = (double)target.getBbWidth() * 0.5 * c.random.nextDouble();
         VfxParticles.emit(
               YomiLook.FLAME_COLD,
               feet.x + Math.cos(a) * r,
               feet.y + c.random.nextDouble() * (double)height * 0.8 + 0.3,
               feet.z + Math.sin(a) * r,
               0.0,
               0.03,
               0.0
            )
            .size(0.6F + c.random.nextFloat() * 0.5F)
            .rotation(0.0F);
      }

      VfxParticles.emit(YomiLook.COLD_FLASH, center.x, center.y, center.z, 0.0, 0.0, 0.0).size(1.6F * s);
      VfxParticles.emit(YomiLook.COLD_GLOW, center.x, center.y, center.z, 0.0, 0.0, 0.0).size(1.3F * s);
      Frost.chips(c.random, center, Vec3.ZERO, 14, 0.3);
      Frost.snow(c.random, center, 0.9, 12);
      c.creeps.add(Frost.Creep.disc(level, c.random, feet, 1.6, 0.4F));
      if (Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(center) < 144.0) {
         CameraDirector.shake(0.2F, 0.07F, 1.2F, 12.0F);
         VfxPost.flash(11070719, 0.2F);
      }

      return c;
   }

   public static ColdCinematic freeze(ClientLevel level, Entity target, float height, int ticks, long seed) {
      Vec3 center = target.position().add(0.0, (double)height * 0.5, 0.0);
      ColdCinematic c = new ColdCinematic(target, center, true, ticks, height, seed);
      VfxParticles.emit(YomiLook.COLD_FLASH, center.x, center.y, center.z, 0.0, 0.0, 0.0).size(1.1F);
      Frost.chips(c.random, center, Vec3.ZERO, 8, 0.2);
      Frost.snow(c.random, center, 0.6, 8);
      c.creeps.add(Frost.Creep.disc(level, c.random, target.position(), Math.max(0.9, (double)target.getBbWidth() + 0.4), 0.5F));
      return c;
   }

   @Override
   protected int length() {
      return this.ticks + 4;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity target = this.anchorEntity();
      if (!this.freeze) {
         if (this.age < 16) {
            Frost.chill(this.at, 0.5F * (1.0F - (float)this.age / 16.0F), 6.0);
         }
      } else if (target != null && target.isAlive()) {
         if (this.age % 12 == 6 && this.age < this.ticks) {
            Vec3 mouth = target.getEyePosition().add(target.getLookAngle().scale((double)target.getBbWidth() * 0.5));
            Frost.breath(this.random, mouth, target.getLookAngle());
         }

         if (this.age == this.ticks) {
            Vec3 c = target.position().add(0.0, (double)this.height * 0.5, 0.0);
            Frost.chips(this.random, c, Vec3.ZERO, 12, 0.22);
            Frost.mist(this.random, target.position(), 0.5, 3);
            level.playLocalSound(c.x, c.y, c.z, YomiSounds.ICE_SHATTER.get(), SoundSource.PLAYERS, 0.8F, 1.1F, false);
         }
      }
   }
}
