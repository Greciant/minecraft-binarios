package com.devilfruits.fruits.bomu_bomu.client.vfx;

import com.devilfruits.client.vfx.Cinematic;
import com.devilfruits.client.vfx.VfxAnchor;
import com.devilfruits.client.vfx.VfxParams;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.fruits.bomu_bomu.BomuBomu;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class VanishCinematic extends Cinematic {
   private final Player player;
   private final Vec3 at;
   private final int ticks;
   private final RandomSource random;

   public VanishCinematic(Player player, Vec3 at, int ticks, long seed) {
      super(VfxAnchor.of(player, at), VfxParams.NONE);
      this.player = player;
      this.at = at;
      this.ticks = Math.max(4, ticks);
      this.random = RandomSource.create(seed);
      if (VfxWorld.local(player.getId())) {
         VfxPost.flash(16777215, 1.0F);
      }
   }

   @Override
   public void tick(ClientLevel level) {
      super.tick(level);
      if (this.age <= this.ticks + 10 && (this.age <= 4 || FormFlags.has(this.player, BomuBomu.ID, BomuBomu.VANISHED))) {
         float k = 1.0F - (float)this.age / (float)this.ticks;
         float light = VfxWorld.light(level, this.at.x, this.at.y, this.at.z);

         for (int i = 0; i < VfxSettings.count(1.2F); i++) {
            double h = this.random.nextDouble();
            VfxParticles.emit(
                  VfxPresets.BLAST_SMOKE,
                  this.at.x + this.spread(0.3),
                  this.at.y - 0.6 + h * 1.4,
                  this.at.z + this.spread(0.3),
                  this.spread(0.015),
                  0.006,
                  this.spread(0.015)
               )
               .size(0.9F + this.random.nextFloat() * 0.6F)
               .tint(VfxWorld.shade(3946034, light))
               .alpha(0.75F)
               .life(0.35F)
               .unflipped();
         }

         if (this.random.nextFloat() < 0.8F) {
            VfxParticles.emit(
               VfxPresets.BLAST_EMBER,
               this.at.x + this.spread(0.35),
               this.at.y + this.spread(0.8),
               this.at.z + this.spread(0.35),
               this.spread(0.03),
               0.03,
               this.spread(0.03)
            );
         }

         VfxParticles.emit(VfxPresets.GLOW, this.at.x, this.at.y, this.at.z, 0.0, 0.0, 0.0).size(2.2F * Math.max(0.2F, k)).tint(16756832).alpha(0.4F * k);
         if (VfxWorld.local(this.player.getId())) {
            float w = Mth.clamp(1.0F - (float)this.age / 12.0F, 0.0F, 1.0F);
            VfxPost.grade(16774374, 0.6F * w);
            VfxPost.vignette(0.35F * w);
         }
      } else {
         this.finish();
      }
   }

   private double spread(double a) {
      return (this.random.nextDouble() * 2.0 - 1.0) * a;
   }

   @Override
   public double x() {
      return this.at.x;
   }

   @Override
   public double y() {
      return this.at.y;
   }

   @Override
   public double z() {
      return this.at.z;
   }
}
