package com.devilfruits.fruits.gura_gura.client.vfx;

import com.devilfruits.client.particle.Fx;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.GlassCrack;
import com.devilfruits.client.vfx.fx.GroundWave;
import com.devilfruits.client.vfx.fx.ShockRing;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.particle.VfxPresets;
import com.devilfruits.client.vfx.post.VfxPost;
import com.devilfruits.registry.DFParticles;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class QuakeStrikes {
   private static final int CRACK = 15398655;
   private static final int GLINT = 14217983;

   private static float exposure(@Nullable Entity caster, Vec3 at) {
      return VfxWorld.exposure(caster, at, 8.0, 40.0);
   }

   private static void glints(RandomSource r, Vec3 at, Vec3 push, int n, float speed) {
      for (int i = 0; i < VfxSettings.count((float)n); i++) {
         VfxParticles.emit(
               i % 3 == 0 ? VfxPresets.GLASS_DUST : VfxPresets.GLASS_GLINT,
               at.x,
               at.y,
               at.z,
               push.x + (double)((r.nextFloat() - 0.5F) * speed),
               push.y + (double)(r.nextFloat() * speed * 0.6F),
               push.z + (double)((r.nextFloat() - 0.5F) * speed)
            )
            .tint(14217983);
      }
   }

   public static void gekishin(ClientLevel level, @Nullable Entity caster, Vec3 hit, Vec3 dir, boolean target, long seed) {
      FistCrackCinematic bubble = caster != null ? FistCrackCinematic.of(caster) : null;
      if (bubble != null) {
         bubble.release(dir);
      }

      RandomSource r = RandomSource.create(seed);
      GlassCrack.at(hit, dir.scale(-1.0), target ? 2.0F : 1.1F, seed)
         .pattern(target ? 14 : 10, 3)
         .grow(1)
         .hold(target ? 3 : 2)
         .shatter(dir.scale(0.55))
         .refraction(1.0F)
         .lines(0.022F)
         .tint(15398655)
         .spawn();
      ShockRing.at(hit, dir).radius(0.3F, target ? 4.5F : 2.6F).life(9).move(dir.scale(0.25)).strength(1.4F).band(0.3F).rim(0.18F, 15398655).spawn();
      ShockRing.at(hit, dir).radius(0.2F, target ? 2.8F : 1.6F).life(6).strength(1.0F).band(0.4F).rim(0.0F, 15398655).spawn();
      VfxParticles.emit(VfxPresets.GLOW, hit.x, hit.y, hit.z, 0.0, 0.0, 0.0).size(target ? 2.6F : 1.6F).tint(15398655).alpha(0.4F).life(1.2F);
      glints(r, hit, dir.scale(0.3), target ? 30 : 14, 0.5F);
      float ex = exposure(caster, hit);
      if (ex > 0.0F) {
         boolean fp = caster != null && VfxWorld.firstPersonCaster(caster.getId());
         float soft = fp ? 0.5F : 1.0F;
         CameraDirector.shake((target ? 0.5F : 0.3F) * ex, 0.09F, 2.5F, 3.2F);
         CameraDirector.fovPunch((target ? 0.07F : 0.04F) * ex);
         VfxPost.chromatic((target ? 0.4F : 0.2F) * ex * soft);
         VfxPost.radialBlur(hit, (target ? 0.16F : 0.08F) * ex * soft);
         if (target && ex > 0.5F) {
            VfxClock.hitStop(3);
            VfxPost.flash(15398655, 0.07F * ex * soft);
         }
      }
   }

   public static void landing(ClientLevel level, Vec3 at, float radius, long seed) {
      RandomSource r = RandomSource.create(seed);
      double gy = VfxWorld.ground(level, at.x, at.y + 0.5, at.z, 4);
      double fy = Double.isNaN(gy) ? at.y : gy;
      Vec3 g = new Vec3(at.x, fy + 0.04, at.z);
      GlassCrack.at(g, new Vec3(0.0, 1.0, 0.0), radius, seed)
         .pattern(11, 3)
         .grow(2)
         .hold(6)
         .shatter(new Vec3(0.0, 0.16, 0.0))
         .refraction(0.8F)
         .lines(0.024F)
         .tint(15398655)
         .spawn();
      int dust = QuakeCinematic.dust(level, at.x, fy - 0.5, at.z);
      GroundWave.at(at.x, fy, at.z, radius * 1.8F, 12)
         .strength(1.3F)
         .ring(15398655, 0.35F)
         .dust(18, dust, 0.6F, 0.35F, 0.8F, seed)
         .air(fy + 1.0, radius)
         .spawn();
      MutableBlockPos pos = new MutableBlockPos();
      BlockState ground = Fx.groundAt(level, at.x, at.y, at.z, pos);
      if (ground != null) {
         for (int i = 0; i < Fx.count(14); i++) {
            Fx.spawn(
               new BlockParticleOption(DFParticles.BLOCK_DEBRIS.get(), ground),
               at.x + (double)((r.nextFloat() - 0.5F) * radius * 2.0F),
               (double)pos.getY() + 1.05,
               at.z + (double)((r.nextFloat() - 0.5F) * radius * 2.0F),
               (double)(r.nextFloat() - 0.5F) * 0.4,
               0.3 + (double)r.nextFloat() * 0.4,
               (double)(r.nextFloat() - 0.5F) * 0.4
            );
         }
      }

      glints(r, g.add(0.0, 0.3, 0.0), new Vec3(0.0, 0.1, 0.0), 16, 0.5F);
      float ex = exposure(null, at);
      if (ex > 0.0F) {
         CameraDirector.shake(0.35F * ex, 0.1F, 2.0F, 3.0F);
      }
   }

   public static void kaisho(ClientLevel level, @Nullable Entity caster, Vec3 at, Vec3 dir, boolean water, long seed) {
      boolean fp = caster != null && VfxWorld.firstPersonCaster(caster.getId());
      Vec3 pane = at.add(dir.scale(fp ? 2.8 : 1.2));
      RandomSource r = RandomSource.create(seed);
      GlassCrack.at(pane, dir.scale(-1.0), fp ? 1.1F : 1.8F, seed)
         .pattern(13, 3)
         .grow(2)
         .hold(3)
         .shatter(dir.scale(0.6))
         .refraction(0.9F)
         .lines(0.022F)
         .tint(15398655)
         .spawn();

      for (int k = 0; k < 3; k++) {
         ShockRing.at(at.add(dir.scale(0.6)), dir)
            .radius(0.4F + 0.3F * (float)k, 3.0F + 2.2F * (float)k)
            .life(9 + 3 * k)
            .move(dir.scale(0.7 + 0.25 * (double)k))
            .strength(1.2F - 0.15F * (float)k)
            .band(0.3F)
            .rim(0.15F, 15398655)
            .spawn();
      }

      VfxParticles.emit(VfxPresets.GLOW, pane.x, pane.y, pane.z, 0.0, 0.0, 0.0).size(2.6F).tint(15398655).alpha(0.35F).life(1.2F);
      glints(r, pane, dir.scale(0.35), 24, 0.5F);
      float ex = exposure(caster, at);
      if (ex > 0.0F) {
         float soft = fp ? 0.5F : 1.0F;
         CameraDirector.shake(0.45F * ex, 0.08F, 2.5F, 3.0F);
         CameraDirector.fovPunch(0.06F * ex);
         VfxPost.chromatic(0.3F * ex * soft);
         VfxPost.radialBlur(pane, 0.12F * ex * soft);
      }
   }

   public static void leap(ClientLevel level, @Nullable Entity caster, Vec3 at, long seed) {
      double gy = VfxWorld.ground(level, at.x, at.y + 0.5, at.z, 4);
      double fy = Double.isNaN(gy) ? at.y : gy;
      int dust = QuakeCinematic.dust(level, at.x, fy - 0.5, at.z);
      GroundWave.at(at.x, fy, at.z, 3.0F, 9).strength(1.0F).dust(12, dust, 0.5F, 0.3F, 0.7F, seed).spawn();
      ShockRing.at(new Vec3(at.x, fy + 0.3, at.z), new Vec3(0.0, 1.0, 0.0))
         .radius(0.4F, 2.6F)
         .life(8)
         .move(new Vec3(0.0, 0.12, 0.0))
         .strength(0.9F)
         .band(0.35F)
         .rim(0.12F, 15398655)
         .spawn();
   }

   public static void deflect(ClientLevel level, Vec3 at, Vec3 velocity, long seed) {
      Vec3 dir = velocity.lengthSqr() > 1.0E-6 ? velocity.normalize() : new Vec3(0.0, 0.0, 1.0);
      ShockRing.at(at, dir).radius(0.15F, 1.8F).life(8).strength(1.5F).band(0.5F).rim(0.0F, 15398655).spawn();
      ShockRing.at(at, dir).radius(0.1F, 1.0F).life(6).strength(1.2F).band(0.6F).twist(1.2F).rim(0.0F, 15398655).spawn();
   }

   public static void auraHit(ClientLevel level, Vec3 at, Vec3 dir, long seed) {
      ShockRing.at(at, dir).radius(0.2F, 2.2F).life(8).move(dir.scale(0.18)).strength(1.5F).band(0.45F).rim(0.0F, 15398655).spawn();
      ShockRing.at(at, dir).radius(0.1F, 1.2F).life(6).strength(1.1F).band(0.6F).twist(-1.0F).rim(0.0F, 15398655).spawn();
   }

   public static void slashCast(ClientLevel level, @Nullable Entity caster, Vec3 at, Vec3 dir, long seed) {
      ShockRing.at(at, dir).radius(0.4F, 2.4F).life(7).move(dir.scale(0.3)).strength(1.0F).band(0.35F).rim(0.12F, 15398655).spawn();
      glints(RandomSource.create(seed), at, dir.scale(0.3), 10, 0.3F);
      float ex = exposure(caster, at);
      if (ex > 0.0F) {
         CameraDirector.shake(0.15F * ex, 0.12F, 1.0F, 3.0F);
         CameraDirector.fovPunch(0.03F * ex);
      }
   }

   public static void slashImpact(ClientLevel level, Vec3 at, Vec3 dir, float width, long seed) {
      float r = Math.max(0.9F, width * 0.55F);
      GlassCrack.at(at.subtract(dir.scale(0.05)), dir.scale(-1.0), r, seed)
         .pattern(12, 3)
         .grow(1)
         .hold(4)
         .shatter(dir.scale(-0.2))
         .refraction(0.9F)
         .lines(0.024F)
         .tint(15398655)
         .spawn();
      ShockRing.at(at, dir).radius(0.3F, r * 2.0F).life(8).strength(1.1F).band(0.3F).rim(0.15F, 15398655).spawn();
      RandomSource rs = RandomSource.create(seed);
      glints(rs, at, dir.scale(-0.2), 18, 0.5F);
      new MutableBlockPos();
      BlockState wall = level.getBlockState(BlockPos.containing(at.add(dir.scale(0.2))));
      if (!wall.isAir()) {
         for (int i = 0; i < Fx.count(10); i++) {
            Fx.spawn(
               new BlockParticleOption(DFParticles.BLOCK_DEBRIS.get(), wall),
               at.x,
               at.y,
               at.z,
               -dir.x * 0.2 + (double)(rs.nextFloat() - 0.5F) * 0.3,
               0.1 + (double)rs.nextFloat() * 0.2,
               -dir.z * 0.2 + (double)(rs.nextFloat() - 0.5F) * 0.3
            );
         }
      }

      float ex = exposure(null, at);
      if (ex > 0.0F) {
         CameraDirector.shake(0.2F * ex, 0.12F, 1.2F, 3.0F);
      }
   }

   private QuakeStrikes() {
   }
}
