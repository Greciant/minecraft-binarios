package com.devilfruits.fruits.yomi_yomi.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.post.VfxPost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class SoulColdCinematic extends YomiCinematic {
   private final SoulColdCinematic.Mode mode;
   private final double radius;
   private final int length;

   private SoulColdCinematic(Entity entity, Vec3 at, SoulColdCinematic.Mode mode, double radius, int length, long seed) {
      super(VfxAnchor.of(entity, at), at, seed);
      this.mode = mode;
      this.radius = radius;
      this.length = length;
   }

   public static SoulColdCinematic flare(Entity soul, long seed) {
      SoulColdCinematic c = new SoulColdCinematic(soul, soul.position(), SoulColdCinematic.Mode.FLARE, 0.0, 30, seed);
      Vec3 head = soul.position().add(0.0, 1.15, 0.0);
      VfxParticles.emit(YomiLook.COLD_FLASH, head.x, head.y, head.z, 0.0, 0.0, 0.0).size(1.2F);
      return c;
   }

   public static SoulColdCinematic wave(ClientLevel level, Entity soul, Vec3 center, double radius, long seed) {
      SoulColdCinematic c = new SoulColdCinematic(soul, center, SoulColdCinematic.Mode.WAVE, radius, 30, seed);
      double floor = VfxWorld.ground(level, center.x, center.y, center.z, 12);
      Vec3 ground = new Vec3(center.x, Double.isNaN(floor) ? center.y - 1.0 : floor, center.z);
      int n = VfxSettings.count((float)(radius * 5.0));

      for (int i = 0; i < n; i++) {
         double a = ((double)i + c.random.nextDouble() * 0.5) / (double)n * Math.PI * 2.0;
         double r = radius * (0.9 + c.random.nextDouble() * 0.15);
         double x = ground.x + Math.cos(a) * r;
         double z = ground.z + Math.sin(a) * r;
         double top = VfxWorld.ground(level, x, ground.y + 1.5, z, 4);
         float size = 0.5F + c.random.nextFloat() * 0.4F;
         VfxParticles.emit(YomiLook.FLAME_COLD, x, (Double.isNaN(top) ? ground.y : top) + (double)size * 0.42, z, Math.cos(a) * 0.03, 0.02, Math.sin(a) * 0.03)
            .size(size)
            .rotation(0.0F);
      }

      VfxParticles.emit(YomiLook.COLD_FLASH, center.x, center.y, center.z, 0.0, 0.0, 0.0).size((float)(0.6 + radius * 0.15));
      c.creeps.add(Frost.Creep.disc(level, c.random, ground, radius * 0.9, (float)(radius / 8.0)));
      Frost.snow(c.random, center, radius * 0.4, (int)(radius * 2.0));
      return c;
   }

   public static SoulColdCinematic secondWind(ClientLevel level, Entity user, double radius, long seed) {
      SoulColdCinematic c = new SoulColdCinematic(user, user.position(), SoulColdCinematic.Mode.SECOND_WIND, radius, 26, seed);
      Vec3 chest = user.position().add(0.0, 1.0, 0.0);
      VfxParticles.emit(YomiLook.SOUL_FLASH, chest.x, chest.y, chest.z, 0.0, 0.0, 0.0).size(2.2F);
      VfxParticles.emit(YomiLook.COLD_FLASH, chest.x, chest.y, chest.z, 0.0, 0.0, 0.0).size(1.8F);
      SoulFlames.burst(c.random, chest, 1.6F);
      Frost.chips(c.random, chest, Vec3.ZERO, 16, 0.3);
      c.creeps.add(Frost.Creep.disc(level, c.random, user.position(), 3.0, 0.6F));
      if (VfxWorld.local(user.getId()) || Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(chest) < 144.0) {
         CameraDirector.shake(0.35F, 0.06F, 2.0F, 10.0F);
         VfxPost.flash(13172704, 0.3F);
      }

      return c;
   }

   @Override
   protected int length() {
      return this.length;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      Entity e = this.anchorEntity();
      switch (this.mode) {
         case FLARE:
         default:
            break;
         case WAVE:
            if (this.age < 10) {
               Frost.chill(this.at, 0.35F * (1.0F - (float)this.age / 10.0F), this.radius);
            }
            break;
         case SECOND_WIND:
            if (this.age <= 6 && this.age % 2 == 0 && e != null) {
               double r = this.radius * (double)(this.age + 2) / 8.0;
               int n = VfxSettings.count((float)(r * 6.0));

               for (int i = 0; i < n; i++) {
                  double a = ((double)i + this.random.nextDouble() * 0.5) / (double)n * Math.PI * 2.0;
                  Vec3 p = e.position().add(Math.cos(a) * r, 0.25, Math.sin(a) * r);
                  VfxParticles.emit(
                        this.age == 0 ? YomiLook.SOUL_FIRE_TONGUE : YomiLook.FLAME_COLD, p.x, p.y, p.z, Math.cos(a) * 0.15, 0.03, Math.sin(a) * 0.15
                     )
                     .size(0.6F + this.random.nextFloat() * 0.4F)
                     .rotation(0.0F);
               }
            }

            if (this.age < 12) {
               Frost.chill(this.at, 0.4F * (1.0F - (float)this.age / 12.0F), this.radius);
            }
      }
   }

   private static enum Mode {
      FLARE,
      WAVE,
      SECOND_WIND;
   }
}
