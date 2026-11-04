package com.devilfruits.fruits.yomi_yomi.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.post.VfxPost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class FubukiCinematic extends YomiCinematic {
   private static final int SEAM_TICKS = 22;
   private static final int HOLD = 48;
   private final boolean burst;
   private final Vec3 a;
   private final Vec3 b;
   private final int dashTicks;

   private FubukiCinematic(Entity user, Vec3 a, Vec3 b, boolean burst, int dashTicks, long seed) {
      super(VfxAnchor.of(user, a), a.lerp(b, 0.5), seed);
      this.a = a;
      this.b = b;
      this.burst = burst;
      this.dashTicks = dashTicks;
   }

   public static FubukiCinematic dash(Entity user, Vec3 a, Vec3 b, int ticks, long seed) {
      FubukiCinematic c = new FubukiCinematic(user, a, b, false, ticks, seed);
      SoulFlames.burst(c.random, a.add(0.0, 0.9, 0.0), 0.7F);
      return c;
   }

   public static FubukiCinematic burst(ClientLevel level, Entity user, Vec3 a, Vec3 b, float radius, long seed) {
      FubukiCinematic c = new FubukiCinematic(user, a, b, true, 0, seed);
      float body = (user instanceof LivingEntity l ? l.yBodyRot : user.getYRot()) * (float) (Math.PI / 180.0);
      Vec3 hip = user.position()
         .add((double)Mth.cos(body) * 0.06 - (double)Mth.sin(body) * 0.24, 0.95, (double)Mth.sin(body) * 0.06 + (double)Mth.cos(body) * 0.24);
      VfxParticles.emit(YomiLook.SOUL_FLASH, hip.x, hip.y, hip.z, 0.0, 0.0, 0.0).size(0.7F);
      VfxParticles.emit(YomiLook.COLD_FLASH, hip.x, hip.y, hip.z, 0.0, 0.0, 0.0).size(0.4F);
      if (Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(user.position()) < 576.0) {
         VfxClock.hitStop(2);
      }

      Vec3 d = b.subtract(a);
      double len = d.length();
      Vec3 dir = len < 0.001 ? new Vec3(0.0, 0.0, 1.0) : d.scale(1.0 / len);
      Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
      float yaw = (float)Math.toDegrees(Math.atan2(-dir.x, dir.z));
      int n = Math.max(6, (int)Math.round(len / 0.36));

      for (int i = 0; i <= n; i++) {
         double s = (double)i / (double)n * len;
         boolean left = i % 2 == 0;
         double off = (double)(left ? 1 : -1) * (0.08 + c.random.nextDouble() * 0.12);
         Vec3 p = a.add(dir.scale(s)).add(side.scale(off));
         double top = VfxWorld.ground(level, p.x, p.y + 1.0, p.z, 4);
         if (!Double.isNaN(top)) {
            Vec3 foot = new Vec3(p.x, top - 0.05, p.z);
            float lean = 6.0F + c.random.nextFloat() * 4.0F;
            float turn = yaw + 90.0F + (left ? 0.0F : 180.0F) + (c.random.nextFloat() - 0.5F) * 14.0F;
            float born = (float)(s * 0.4);
            c.ice.add(new IceProp("ice_blade", foot, turn, lean, 0.95F + c.random.nextFloat() * 0.15F, born, 48.0F, c.random.nextLong()));
         }
      }

      double top = VfxWorld.ground(level, b.x, b.y + 1.0, b.z, 4);
      if (!Double.isNaN(top)) {
         Vec3 end = new Vec3(b.x, top - 0.05, b.z).add(dir.scale(0.5));
         c.ice.add(new IceProp("ice_cluster_b", end, yaw, 4.0F, 0.75F, (float)(len * 0.4) + 1.0F, 46.0F, c.random.nextLong()));
      }

      Frost.Creep creep = Frost.Creep.path(level, c.random, a, b, Math.min((double)radius, 0.55), 3.0F);
      c.creeps.add(creep);
      Frost.mist(c.random, a.lerp(b, 0.5), len * 0.3, 8);
      if (VfxWorld.local(user.getId()) || Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(c.at) < 196.0) {
         CameraDirector.shake(0.3F, 0.05F, 1.8F, 9.0F);
         VfxPost.flash(12580095, 0.18F);
      }

      return c;
   }

   @Override
   protected int length() {
      return this.burst ? 40 : 22;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.burst) {
         double len = this.a.distanceTo(this.b);

         for (int i = 0; i < 2; i++) {
            double s = Math.min(len, (double)this.age / 0.4 - this.random.nextDouble() * 2.0);
            if (s >= 0.0 && (double)this.age / 0.4 <= len + 2.0) {
               Vec3 p = this.a.lerp(this.b, len < 0.001 ? 0.0 : s / len).add(0.0, 0.2, 0.0);
               Frost.chips(this.random, p, Vec3.ZERO, 3, 0.2);
               VfxParticles.emit(YomiLook.COLD_FLASH, p.x, p.y + 0.4, p.z, 0.0, 0.0, 0.0).size(0.6F);
               SoulFlames.wispTrail(this.random, p, p.add(0.0, 1.2, 0.0), 1.5F, 0.8F);
            }
         }

         if (this.age < 30) {
            Frost.chill(this.at, 0.55F * (1.0F - (float)this.age / 30.0F), 8.0);
         }

         if (this.age % 6 == 0 && this.age < 50) {
            Frost.snow(this.random, this.at.add(0.0, 1.0, 0.0), Math.max(1.0, this.a.distanceTo(this.b) * 0.35), 4);
         }
      } else {
         Entity user = this.anchorEntity();
         if (user != null && this.age <= this.dashTicks + 1) {
            Afterimages.add(user, 10, 8191802);
            Vec3 body = user.position().add(0.0, 0.9, 0.0);
            Vec3 back = body.subtract(this.b.subtract(this.a).normalize().scale(1.5));
            SoulFlames.wispTrail(this.random, back, body, 3.0F, 0.9F);

            for (int ix = 0; ix < 3; ix++) {
               VfxParticles.emit(
                  YomiLook.STREAK,
                  body.x + this.random.nextGaussian() * 0.25,
                  body.y + this.random.nextGaussian() * 0.4,
                  body.z + this.random.nextGaussian() * 0.25,
                  (this.b.x - this.a.x) * 0.04,
                  0.0,
                  (this.b.z - this.a.z) * 0.04
               );
            }
         }
      }
   }

   @Override
   public void render(VfxFrame f) {
      if (!this.burst) {
         float t = (float)this.age + f.partial;
         float drawn = Mth.clamp(t / (float)Math.max(1, this.dashTicks), 0.0F, 1.0F);
         float fade = Mth.clamp((22.0F - t) / 4.0F, 0.0F, 1.0F);
         float flicker = 0.75F + 0.25F * Mth.sin(t * 2.3F) * Mth.sin(t * 0.9F + 1.0F);
         Vec3 sa = this.a.add(0.0, 1.0, 0.0);
         Vec3 sb = this.a.lerp(this.b, (double)drawn).add(0.0, 1.0, 0.0);
         SoulCuts.seam(f, sa, sb, 0.05F, 1.0F, fade * flicker * (t < (float)(this.dashTicks + 3) ? 1.0F : 0.7F), -t * 0.3F);
      }
   }
}
