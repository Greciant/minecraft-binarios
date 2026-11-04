package com.devilfruits.fruits.bomu_bomu.client.vfx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.VfxSound;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.fruits.bomu_bomu.BomuSounds;
import com.mojang.blaze3d.vertex.BufferBuilder;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class SelfDestructChargeCinematic extends Cinematic {
   private final Player player;
   private final float duration;
   private final RandomSource random;
   @Nullable
   private final VfxSound hum;
   private float progress;

   public SelfDestructChargeCinematic(Player player, float duration, long seed) {
      super(VfxAnchor.of(player, player.position()), VfxParams.NONE);
      this.player = player;
      this.duration = Math.max(1.0F, duration);
      this.random = RandomSource.create(seed);
      this.hum = VfxSound.tracking(
         BomuSounds.SELF_DESTRUCT_CHARGE.get(),
         player::position,
         true,
         a -> 0.25F + 0.75F * Math.min(1.0F, (float)a / this.duration),
         a -> 0.6F + 0.8F * Math.min(1.0F, (float)a / this.duration),
         (int)this.duration + 60,
         () -> !this.isDone() && player.isAlive()
      );
   }

   public Player player() {
      return this.player;
   }

   public float progress() {
      return this.progress;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.player.isAlive() && !this.player.isRemoved() && !((float)this.age > this.duration + 60.0F)) {
         this.progress = Math.min(1.0F, (float)this.age / this.duration);
         float p = this.progress;
         boolean fp = VfxWorld.firstPersonCaster(this.player.getId());
         Vec3 c = this.player.position().add(0.0, (double)this.player.getBbHeight() * 0.5, 0.0);
         float pulse = 0.75F + 0.25F * Mth.sin((float)this.age * (0.4F + p * 0.9F));
         if (!fp) {
            VfxParticles.emit(VfxPresets.GLOW, c.x, c.y, c.z, 0.0, 0.0, 0.0).size((1.4F + 3.2F * p) * pulse).tint(16756832).alpha(0.25F + 0.5F * p);
         }

         for (int i = 0; i < VfxSettings.count(1.0F + 5.0F * p); i++) {
            Vec3 at = BomuLook.onBody(this.player, 1.0F, this.random);
            VfxParticles.emit(BomuLook.GLINT, at.x, at.y, at.z, 0.0, 0.0, 0.0).size(0.8F + p).heat(0.9F + 0.2F * p);
         }

         for (int i = 0; i < VfxSettings.count(0.3F + 2.5F * p * p); i++) {
            Vec3 at = BomuLook.onBody(this.player, 1.0F, this.random);
            Vec3 out = at.subtract(c).add(this.spread(0.3), this.spread(0.3), this.spread(0.3)).normalize().scale(0.25 + 0.3 * (double)p);
            VfxParticles.emit(BomuLook.LIGHT_RAY, at.x, at.y, at.z, out.x, out.y, out.z).size((fp ? 0.5F : 1.0F) * (0.8F + p));
         }

         for (int i = 0; i < VfxSettings.count(0.5F + 1.5F * p); i++) {
            Vec3 at = BomuLook.onBody(this.player, 1.0F, this.random);
            VfxParticles.emit(VfxPresets.BLAST_EMBER, at.x, at.y, at.z, this.spread(0.02), 0.03 + 0.04 * (double)p, this.spread(0.02));
         }

         double g = VfxWorld.ground(level, this.player.getX(), this.player.getY() + 0.5, this.player.getZ(), 3);
         if (!Double.isNaN(g)) {
            VfxParticles.emit(VfxPresets.GROUND_GLOW, this.player.getX(), g + 0.05, this.player.getZ(), 0.0, 0.0, 0.0)
               .size(1.5F + 3.5F * p)
               .tint(16747066)
               .alpha(0.3F + 0.5F * p);
         }

         if (fp && VfxWorld.local(this.player.getId())) {
            CameraDirector.shake(1, 0.04F + 0.12F * p * p, 0.2F, 1.2F + p, 6.0F);
         }
      } else {
         this.finish();
      }
   }

   @Override
   public void render(VfxFrame f) {
      BufferBuilder d = f.distortion();
      if (d != null) {
         VfxSprites.Sprite glow = VfxSprites.GLOW;
         float s = 0.9F + this.progress * 1.2F;
         MeshWriter.distortQuad(
            d,
            f.rx(this.player.getX()),
            f.ry(this.player.getY() + (double)this.player.getBbHeight() * 0.7 + (double)this.progress * 0.4),
            f.rz(this.player.getZ()),
            s,
            0.35F + 0.5F * this.progress,
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
            f.time * 0.015F,
            0.5F
         );
      }
   }

   @Override
   public void removed() {
      if (this.hum != null) {
         this.hum.end();
      }
   }

   private double spread(double a) {
      return (this.random.nextDouble() * 2.0 - 1.0) * a;
   }

   @Override
   public double x() {
      return this.player.getX();
   }

   @Override
   public double y() {
      return this.player.getY();
   }

   @Override
   public double z() {
      return this.player.getZ();
   }
}
