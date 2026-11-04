package com.awakenedmod.awakened.content.earth;

import com.awakenedmod.awakened.core.Fx;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class EarthFx {
   private EarthFx() {
   }

   static void rockThrow(ServerLevel l, Vec3 hand, EarthMaterial mat) {
      Fx.burst(l, new BlockParticleOption(ParticleTypes.BLOCK, mat.state()), hand, 8, 0.25, 0.04);
      Fx.sound(l, hand, mat.state().getSoundType().getPlaceSound(), SoundSource.PLAYERS, 0.9F, 0.8F);
   }

   static void rockImpact(ServerLevel l, Vec3 p, EarthMaterial mat) {
      Fx.burst(l, new BlockParticleOption(ParticleTypes.BLOCK, mat.state()), p, 22, 0.55, 0.12);
      Fx.burst(l, ParticleTypes.POOF, p, 6, 0.4, 0.02);
      Fx.sound(l, p, mat.state().getSoundType().getBreakSound(), SoundSource.PLAYERS, 1.1F, 0.7F);
   }

   static void rockStamp(ServerLevel l, Vec3 at, EarthMaterial mat) {
      Fx.burst(l, new BlockParticleOption(ParticleTypes.BLOCK, mat.state()), at.add(0.0, 0.1, 0.0), 12, 0.4, 0.1);
      Fx.sound(l, at, mat.state().getSoundType().getPlaceSound(), SoundSource.PLAYERS, 0.9F, 0.7F);
   }

   static void rockFling(ServerLevel l, Vec3 at, EarthMaterial mat) {
      Fx.burst(l, new BlockParticleOption(ParticleTypes.BLOCK, mat.state()), at, 6, 0.2, 0.05);
      Fx.sound(l, at, mat.state().getSoundType().getHitSound(), SoundSource.PLAYERS, 1.0F, 0.9F);
   }

   static void materialImpact(ServerLevel l, Vec3 p, EarthMaterial mat) {
      switch (mat.kind()) {
         case STONE:
            Fx.groundImpact(l, p, 2.5, true);
            break;
         case SAND:
            Fx.burst(l, new BlockParticleOption(ParticleTypes.BLOCK, mat.state()), p, 60, 1.4, 0.02);
            Fx.burst(l, ParticleTypes.POOF, p, 22, 1.2, 0.01);
      }
   }

   static void rockCrumble(ServerLevel l, Vec3 at, EarthMaterial mat) {
      Fx.burst(l, new BlockParticleOption(ParticleTypes.BLOCK, mat.state()), at, 16, 0.45, 0.08);
      Fx.burst(l, ParticleTypes.POOF, at, 5, 0.35, 0.02);
      Fx.sound(l, at, mat.state().getSoundType().getBreakSound(), SoundSource.PLAYERS, 0.9F, 0.6F);
   }

   static void upheave(ServerLevel l, Vec3 base, EarthMaterial mat) {
      BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, mat.state());
      Fx.burst(l, debris, base.add(0.0, 0.2, 0.0), 24, 0.6, 0.18);
      Fx.burst(l, debris, base.add(0.0, 1.2, 0.0), 10, 0.5, 0.12);
      Fx.burst(l, ParticleTypes.POOF, base, 8, 0.7, 0.02);
      Fx.sound(l, base, mat.state().getSoundType().getPlaceSound(), SoundSource.PLAYERS, 1.2F, 0.5F);
      Fx.sound(l, base, mat.state().getSoundType().getBreakSound(), SoundSource.PLAYERS, 1.0F, 0.6F);
      Fx.groundImpact(l, base, 2.0, false);
      Fx.shake(l, base, 14.0, 0.16F, 0, 8);
   }

   static void upheaveCrack(ServerLevel l, Vec3 top, EarthMaterial mat) {
      Fx.burst(l, new BlockParticleOption(ParticleTypes.BLOCK, mat.state()), top, 18, 0.5, 0.22);
      Fx.burst(l, ParticleTypes.POOF, top, 8, 0.6, 0.03);
      Fx.sound(l, top, mat.state().getSoundType().getBreakSound(), SoundSource.PLAYERS, 1.3F, 0.45F);
      Fx.shake(l, top, 16.0, 0.22F, 2, 10);
   }

   static void upheaveColumn(ServerLevel l, Vec3 base, double topY, EarthMaterial mat, int age) {
      double height = Math.max(1.0, topY - base.y + 0.5);
      BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, mat.state());

      for (double y = 0.0; y <= height; y += 0.5) {
         double spread = 0.18 + y * 0.03;
         Fx.burst(l, debris, base.add(0.0, y, 0.0), 2, spread, 0.05);
      }

      Fx.burst(l, ParticleTypes.POOF, base.add(0.0, height, 0.0), 3, 0.3, 0.02);
      Fx.burst(l, ParticleTypes.POOF, base, 4, 0.5, 0.02);
   }

   static void faultStomp(ServerLevel l, Vec3 at, EarthMaterial mat) {
      Fx.burst(l, new BlockParticleOption(ParticleTypes.BLOCK, mat.state()), at.add(0.0, 0.1, 0.0), 14, 0.7, 0.05);
      Fx.sound(l, at, mat.state().getSoundType().getBreakSound(), SoundSource.PLAYERS, 1.3F, 0.5F);
      Fx.groundImpact(l, at, 2.2, false);
      Fx.shake(l, at, 16.0, 0.16F, 0, 9);
   }

   static void faultSpike(ServerLevel l, Vec3 at, EarthMaterial mat) {
      Fx.burst(l, new BlockParticleOption(ParticleTypes.BLOCK, mat.state()), at, 6, 0.3, 0.14);
      Fx.sound(l, at, mat.state().getSoundType().getBreakSound(), SoundSource.PLAYERS, 0.8F, 0.7F);
      Fx.shake(l, at, 12.0, 0.08F, 0, 6);
   }

   static void faultTear(ServerLevel l, Vec3 from, Vec3 to, EarthMaterial mat) {
      BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, mat.state());
      int steps = 4;

      for (int i = 1; i <= steps; i++) {
         double t = (double)i / (double)steps;
         Vec3 p = from.add(to.subtract(from).scale(t)).add(0.0, 0.1, 0.0);
         Fx.burst(l, debris, p, 2, 0.18, 0.02);
         Fx.burst(l, ParticleTypes.POOF, p, 1, 0.2, 0.01);
      }
   }

   static void faultCrown(ServerLevel l, Vec3 c, EarthMaterial mat) {
      Fx.burst(l, new BlockParticleOption(ParticleTypes.BLOCK, mat.state()), c.add(0.0, 0.4, 0.0), 46, 1.4, 0.26);
      Fx.burst(l, ParticleTypes.POOF, c.add(0.0, 0.3, 0.0), 16, 1.3, 0.04);
      Fx.sound(l, c, mat.state().getSoundType().getBreakSound(), SoundSource.PLAYERS, 1.5F, 0.4F);
      Fx.groundImpact(l, c, 3.0, true);
      Fx.sound(l, c, mat.state().getSoundType().getBreakSound(), SoundSource.PLAYERS, 1.2F, 0.6F);
      Fx.shake(l, c, 24.0, 0.35F, 4, 16);
   }

   static void maulForge(ServerLevel l, Vec3 foeFeet, Vec3 headCenter, EarthMaterial mat) {
      BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, mat.state());
      Fx.burst(l, debris, foeFeet.add(0.0, 0.1, 0.0), 24, 1.1, 0.05);
      Fx.burst(l, ParticleTypes.POOF, foeFeet, 10, 0.8, 0.02);
      Fx.burst(l, debris, headCenter, 32, 2.1, 0.04);
      Fx.burst(l, ParticleTypes.POOF, headCenter, 12, 1.7, 0.02);
      Fx.sound(l, headCenter, mat.state().getSoundType().getPlaceSound(), SoundSource.PLAYERS, 1.4F, 0.45F);
      Fx.sound(l, foeFeet, mat.state().getSoundType().getHitSound(), SoundSource.PLAYERS, 1.2F, 0.35F);
   }

   static void maulLoom(ServerLevel l, Vec3 headBottom, EarthMaterial mat) {
      Fx.burst(l, new BlockParticleOption(ParticleTypes.BLOCK, mat.state()), headBottom, 3, 1.5, 0.01);
      Fx.burst(l, ParticleTypes.POOF, headBottom.add(0.0, -0.2, 0.0), 2, 1.1, 0.0);
   }

   static void maulTell(ServerLevel l, Vec3 headBottom, EarthMaterial mat) {
      Fx.burst(l, new BlockParticleOption(ParticleTypes.BLOCK, mat.state()), headBottom, 10, 1.6, 0.02);
      Fx.burst(l, ParticleTypes.POOF, headBottom.add(0.0, -0.2, 0.0), 5, 1.2, 0.0);
      Fx.sound(l, headBottom, mat.state().getSoundType().getHitSound(), SoundSource.PLAYERS, 1.1F, 0.3F);
   }

   static void maulFall(ServerLevel l, Vec3 headBottom, EarthMaterial mat) {
      Fx.burst(l, new BlockParticleOption(ParticleTypes.BLOCK, mat.state()), headBottom, 4, 0.5, 0.06);
      Fx.burst(l, ParticleTypes.POOF, headBottom.add(0.0, 0.1, 0.0), 2, 0.4, 0.02);
   }

   static void maulSettle(ServerLevel l, Vec3 at, EarthMaterial mat) {
      Fx.burst(l, ParticleTypes.POOF, at.add(0.0, 0.1, 0.0), 10, 1.0, 0.01);
      Fx.burst(l, new BlockParticleOption(ParticleTypes.BLOCK, mat.state()), at.add(0.0, 0.1, 0.0), 6, 0.8, 0.03);
   }

   static void maulSlam(ServerLevel l, Vec3 impact, EarthMaterial mat) {
      Fx.burst(l, new BlockParticleOption(ParticleTypes.BLOCK, mat.state()), impact, 64, 1.7, 0.34);
      Fx.burst(l, ParticleTypes.POOF, impact, 20, 1.5, 0.06);
      Fx.burst(l, ParticleTypes.LARGE_SMOKE, impact.add(0.0, 0.4, 0.0), 14, 1.3, 0.03);
      Fx.sound(l, impact, mat.state().getSoundType().getBreakSound(), SoundSource.PLAYERS, 1.6F, 0.3F);
      Fx.groundImpact(l, impact, 4.0, true);
      Fx.sound(l, impact, mat.state().getSoundType().getBreakSound(), SoundSource.PLAYERS, 1.3F, 0.5F);
      Fx.shake(l, impact, 30.0, 0.46F, 6, 18);
   }

   static void maulMaterial(ServerLevel l, Vec3 at, EarthMaterial mat) {
      BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, mat.state());
      switch (mat.kind()) {
         case STONE:
            Fx.groundImpact(l, at, 3.0, false);
            break;
         case SAND:
            Fx.burst(l, debris, at, 70, 1.6, 0.02);
            Fx.burst(l, ParticleTypes.POOF, at, 26, 1.5, 0.01);
            break;
         case GRAVEL:
            Fx.burst(l, debris, at.add(0.0, 0.3, 0.0), 70, 2.4, 0.18);
            break;
         case DIRT:
            Fx.burst(l, debris, at.add(0.0, 0.2, 0.0), 20, 1.0, 0.08);
      }
   }

   static void maulPress(ServerLevel l, Vec3 at, EarthMaterial mat) {
      Fx.burst(l, ParticleTypes.POOF, at.add(0.0, 0.2, 0.0), 8, 0.7, 0.02);
      if (l.random.nextInt(2) == 0) {
         Fx.burst(l, new BlockParticleOption(ParticleTypes.BLOCK, mat.state()), at.add(0.0, 0.2, 0.0), 10, 0.6, 0.05);
      }

      Fx.sound(l, at, mat.state().getSoundType().getHitSound(), SoundSource.PLAYERS, 0.9F, 0.4F);
   }

   static void maulCrumble(ServerLevel l, Vec3 at, EarthMaterial mat) {
      Fx.burst(l, new BlockParticleOption(ParticleTypes.BLOCK, mat.state()), at.add(0.0, 0.6, 0.0), 54, 1.6, 0.16);
      Fx.burst(l, ParticleTypes.POOF, at.add(0.0, 0.6, 0.0), 20, 1.4, 0.04);
      Fx.sound(l, at, mat.state().getSoundType().getBreakSound(), SoundSource.PLAYERS, 1.5F, 0.45F);
   }

   static void mechAssemble(ServerLevel l, Vec3 at) {
      BlockState stone = Blocks.STONE.defaultBlockState();
      Fx.burst(l, new BlockParticleOption(ParticleTypes.BLOCK, stone), at.add(0.0, 1.0, 0.0), 30, 1.0, 0.1);
      Fx.burst(l, ParticleTypes.POOF, at.add(0.0, 1.0, 0.0), 14, 1.0, 0.05);
      Fx.sound(l, at, stone.getSoundType().getPlaceSound(), SoundSource.PLAYERS, 1.5F, 0.4F);
   }

   static void mechHit(ServerLevel l, Vec3 at) {
      BlockState stone = Blocks.STONE.defaultBlockState();
      Vec3 mid = at.add(0.0, 1.6, 0.0);
      Fx.burst(l, new BlockParticleOption(ParticleTypes.BLOCK, stone), mid, 10, 0.6, 0.08);
      Fx.sound(l, at, stone.getSoundType().getHitSound(), SoundSource.PLAYERS, 1.0F, 0.5F);
   }

   static void mechCrumble(ServerLevel l, Vec3 at) {
      BlockState cobble = Blocks.COBBLESTONE.defaultBlockState();
      Fx.burst(l, new BlockParticleOption(ParticleTypes.BLOCK, cobble), at.add(0.0, 1.0, 0.0), 44, 1.3, 0.15);
      Fx.burst(l, ParticleTypes.POOF, at.add(0.0, 1.0, 0.0), 16, 1.2, 0.04);
      Fx.sound(l, at, cobble.getSoundType().getBreakSound(), SoundSource.PLAYERS, 1.5F, 0.5F);
   }
}
