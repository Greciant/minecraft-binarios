package com.devilfruits.fruits.yomi_yomi.client.vfx;

import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.particle.ParticleStyle;
import com.devilfruits.client.vfx.particle.VfxParticles;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class SoulFire {
   public static final int GREEN_LIGHT = 8191802;
   public static final int COLD_LIGHT = 7334143;

   public static VfxParticles.Handle lick(double x, double y, double z, double vx, double vy, double vz, float size, boolean tongue, boolean cold) {
      ParticleStyle s = cold ? (tongue ? YomiLook.COLD_FIRE_TONGUE : YomiLook.COLD_FIRE) : (tongue ? YomiLook.SOUL_FIRE_TONGUE : YomiLook.SOUL_FIRE);
      return VfxParticles.emit(s, x, y, z, vx, vy, vz).size(size);
   }

   public static void pool(RandomSource r, double cx, double y, double cz, double radius, float count, float size) {
      int n = VfxSettings.count(count);

      for (int i = 0; i < n; i++) {
         double a = r.nextDouble() * (float) (Math.PI * 2);
         double d = Math.sqrt(r.nextDouble()) * radius;
         boolean tongue = i % 3 == 2;
         float s = size * (tongue ? 0.75F : 1.0F) * (0.75F + r.nextFloat() * 0.5F);
         lick(
            cx + Math.cos(a) * d,
            y + 0.15 + (double)r.nextFloat() * 0.25,
            cz + Math.sin(a) * d,
            (double)(r.nextFloat() - 0.5F) * 0.02,
            0.03 + (double)r.nextFloat() * 0.05,
            (double)(r.nextFloat() - 0.5F) * 0.02,
            s,
            tongue,
            false
         );
      }
   }

   public static void burst(RandomSource r, Vec3 at, float scale) {
      VfxParticles.emit(YomiLook.SOUL_FIRE_FLARE, at.x, at.y, at.z, 0.0, 0.0, 0.0).size(scale * 0.45F).rotation(r.nextFloat() * (float) (Math.PI * 2));

      for (int i = 0; i < VfxSettings.count(10.0F * scale); i++) {
         double a = r.nextDouble() * (float) (Math.PI * 2);
         double up = (r.nextDouble() - 0.3) * 0.12;
         double sp = (0.08 + r.nextDouble() * 0.1) * Math.sqrt((double)scale);
         lick(at.x, at.y, at.z, Math.cos(a) * sp, up + 0.02, Math.sin(a) * sp, scale * (0.7F + r.nextFloat() * 0.5F), i % 3 == 0, false);
      }

      embers(r, at, (double)scale * 0.5, VfxSettings.count(8.0F * scale));
   }

   public static void embers(RandomSource r, Vec3 at, double radius, int count) {
      for (int i = 0; i < count; i++) {
         VfxParticles.emit(
            YomiLook.SOUL_FIRE_EMBER,
            at.x + (r.nextDouble() - 0.5) * radius * 2.0,
            at.y + (r.nextDouble() - 0.5) * radius,
            at.z + (r.nextDouble() - 0.5) * radius * 2.0,
            (double)(r.nextFloat() - 0.5F) * 0.08,
            0.04 + (double)r.nextFloat() * 0.08,
            (double)(r.nextFloat() - 0.5F) * 0.08
         );
      }
   }

   public static void halo(double x, double y, double z, float size, int rgb, float alpha) {
      VfxParticles.emit(YomiLook.HALO, x, y, z, 0.0, 0.0, 0.0).size(size).tint(rgb).alpha(alpha);
   }

   public static void floorGlow(double x, double y, double z, float size, int rgb, float alpha) {
      VfxParticles.emit(YomiLook.FLOOR_GLOW, x, y + 0.06, z, 0.0, 0.0, 0.0).size(size).tint(rgb).alpha(alpha);
   }

   public static void skull(RandomSource r, Vec3 head, float cold) {
      for (int i = 0; i < VfxSettings.count(4.0F); i++) {
         double a = r.nextDouble() * (float) (Math.PI * 2);
         double up = r.nextDouble() * 2.0 - 1.0;
         double d = 0.2 + r.nextDouble() * 0.08;
         double flat = Math.sqrt(1.0 - up * up);
         lick(
               head.x + Math.cos(a) * flat * d,
               head.y + up * d * 0.9,
               head.z + Math.sin(a) * flat * d,
               Math.cos(a) * flat * 0.008,
               0.015 + (double)r.nextFloat() * 0.02,
               Math.sin(a) * flat * 0.008,
               0.42F + r.nextFloat() * 0.18F,
               false,
               r.nextFloat() < cold
            )
            .life(0.6F);
      }

      if (VfxSettings.count(1.0F) > 0) {
         lick(
            head.x + (r.nextDouble() - 0.5) * 0.2,
            head.y + 0.18,
            head.z + (r.nextDouble() - 0.5) * 0.2,
            0.0,
            0.05 + (double)r.nextFloat() * 0.03,
            0.0,
            0.45F + r.nextFloat() * 0.15F,
            true,
            r.nextFloat() < cold
         );
      }

      halo(head.x, head.y, head.z, 1.7F, cold > 0.5F ? 7334143 : 8191802, 0.5F);
      if (cold < 0.5F && r.nextFloat() < 0.25F) {
         embers(r, head.add(0.0, 0.2, 0.0), 0.2, 1);
      }
   }

   private SoulFire() {
   }
}
