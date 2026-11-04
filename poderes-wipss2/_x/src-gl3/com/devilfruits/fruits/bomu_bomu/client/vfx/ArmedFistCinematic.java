package com.devilfruits.fruits.bomu_bomu.client.vfx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxFrame;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.render.MeshWriter;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.fruits.bomu_bomu.BomuBomu;
import com.devilfruits.fruits.bomu_bomu.BomuSounds;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class ArmedFistCinematic extends Cinematic {
   private final Player player;
   private final RandomSource random = RandomSource.create();
   private Vec3 fist = Vec3.ZERO;

   public ArmedFistCinematic(Player player) {
      super(VfxAnchor.of(player, player.position()), VfxParams.NONE);
      this.player = player;
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.player.isAlive() && !this.player.isRemoved() && FormFlags.has(this.player, BomuBomu.ID, BomuBomu.ARMED)) {
         boolean fp = VfxWorld.firstPersonCaster(this.player.getId());
         this.fist = BomuLook.fist(this.player, 1.0F, true, fp);
         float k = fp ? 0.45F : 1.0F;
         float pulse = 0.6F + 0.4F * Mth.sin((float)this.age * 0.63F);
         VfxParticles.emit(VfxPresets.GLOW, this.fist.x, this.fist.y, this.fist.z, 0.0, 0.0, 0.0)
            .size((0.7F + 0.25F * pulse) * k)
            .tint(16756832)
            .alpha(0.55F * pulse);
         if (this.age % 2 == 0) {
            VfxParticles.emit(VfxPresets.FIRE_TONGUE, this.fist.x, this.fist.y, this.fist.z, this.spread(0.01), 0.03, this.spread(0.01))
               .size(0.28F * k)
               .life(0.6F);
         }

         for (int i = 0; i < VfxSettings.count(fp ? 0.5F : 1.2F); i++) {
            VfxParticles.emit(
                  VfxPresets.BLAST_EMBER,
                  this.fist.x + this.spread(0.08),
                  this.fist.y + this.spread(0.08),
                  this.fist.z + this.spread(0.08),
                  this.spread(0.02),
                  0.025 + this.random.nextDouble() * 0.03,
                  this.spread(0.02)
               )
               .life(0.5F);
         }

         if (this.random.nextFloat() < (fp ? 0.15F : 0.35F)) {
            VfxParticles.emit(
                  VfxPresets.BLAST_SPARK, this.fist.x, this.fist.y, this.fist.z, this.spread(0.12), 0.05 + this.random.nextDouble() * 0.1, this.spread(0.12)
               )
               .size(0.5F * k)
               .life(0.5F);
         }

         if (this.age % 10 == 0) {
            VfxParticles.emit(VfxPresets.GLOW, this.fist.x, this.fist.y, this.fist.z, 0.0, 0.0, 0.0).size(1.4F * k).tint(16773850).alpha(0.5F).life(0.7F);
            level.playLocalSound(
               this.fist.x, this.fist.y, this.fist.z, BomuSounds.PUNCH_TICK.get(), SoundSource.PLAYERS, 0.35F, 1.0F + (this.age % 20 == 0 ? 0.1F : 0.0F), false
            );
         }

         if (this.age % 3 == 0) {
            VfxParticles.emit(VfxPresets.SOOT, this.fist.x, this.fist.y + 0.1, this.fist.z, this.spread(0.01), 0.02, this.spread(0.01))
               .size(0.35F * k)
               .tint(VfxWorld.shade(3814448, 0.8F))
               .alpha(0.35F)
               .life(0.4F);
         }
      } else {
         this.finish();
      }
   }

   @Override
   public void render(VfxFrame f) {
      BufferBuilder d = f.distortion();
      if (d != null && (!VfxWorld.firstPersonCaster(this.player.getId()) || !VfxClock.frozen())) {
         VfxSprites.Sprite glow = VfxSprites.GLOW;
         float size = VfxWorld.firstPersonCaster(this.player.getId()) ? 0.18F : 0.4F;
         MeshWriter.distortQuad(
            d,
            f.rx(this.fist.x),
            f.ry(this.fist.y + 0.15),
            f.rz(this.fist.z),
            size,
            0.45F,
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
            f.time * 0.02F,
            0.4F
         );
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
