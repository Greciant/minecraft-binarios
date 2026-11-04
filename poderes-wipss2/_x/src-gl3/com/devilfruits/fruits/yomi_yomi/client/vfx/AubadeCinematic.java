package com.devilfruits.fruits.yomi_yomi.client.vfx;

import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.post.VfxPost;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class AubadeCinematic extends YomiCinematic {
   private static final int LIFE = 12;
   private final Vec3 a;
   private final Vec3 b;
   private final Vec3 dir;
   private final float width;

   public AubadeCinematic(ClientLevel level, Entity user, Vec3 a, Vec3 b, float width, long seed, int hits) {
      super(VfxAnchor.of(user, a), a.lerp(b, 0.5), seed);
      this.a = a;
      this.b = b;
      this.dir = b.subtract(a).normalize();
      this.width = width;
      Vec3 fa = new Vec3(a.x, a.y - 1.0, a.z);
      Vec3 fb = new Vec3(b.x, b.y - 1.0, b.z);
      this.creeps.add(Frost.Creep.path(level, this.random, fa, fb, 0.4, 2.5F));
      SoulFlames.wispTrail(this.random, a, b, 2.2F, 1.0F);
      SoulFlames.burst(this.random, b, hits > 0 ? 1.1F : 0.7F);
      Frost.filings(this.random, b, this.dir, 10);
      Frost.chips(this.random, b, this.dir, hits > 0 ? 6 : 3, 0.18);
      Frost.snow(this.random, a.lerp(b, 0.5), 1.0, 8);
      if (VfxWorld.local(user.getId())) {
         CameraDirector.shake(0.18F, 0.08F, 1.2F, 14.0F);
         VfxPost.radialBlur(b, 0.12F);
      }
   }

   @Override
   protected int length() {
      return 12;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age <= 6) {
         Vec3 p = this.a.lerp(this.b, this.random.nextDouble());
         VfxParticles.emit(YomiLook.STREAK, p.x, p.y, p.z, this.dir.x * 0.2, this.dir.y * 0.2 + 0.02, this.dir.z * 0.2);
      }

      if (this.age <= 4) {
         Frost.chill(this.at, 0.35F * (1.0F - (float)this.age / 5.0F), 6.0);
      }
   }

   @Override
   public void render(VfxFrame f) {
      float t = (float)this.age + f.partial;
      float fade = Mth.clamp(1.0F - t / 12.0F, 0.0F, 1.0F);
      float grow = Math.min(1.0F, t / 1.2F);
      Vec3 tip = this.a.lerp(this.b, (double)grow);
      SoulCuts.thrust(f, this.a, tip, this.width * 0.55F, 0.75F + 0.45F * fade, fade, -t * 0.8F);

      for (int k = 1; k <= 2; k++) {
         float lag = t - (float)k * 1.5F;
         if (!(lag <= 0.0F)) {
            float fk = Mth.clamp(1.0F - lag / (12.0F - (float)k * 3.0F), 0.0F, 1.0F) * (0.55F - (float)k * 0.15F);
            Vec3 off = this.dir.scale(-0.35 * (double)k);
            SoulCuts.thrust(f, this.a.add(off), this.b.add(off), this.width * (0.45F - (float)k * 0.08F), 0.6F, fk, -t * 0.6F);
         }
      }
   }
}
