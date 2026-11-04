package com.devilfruits.fruits.yomi_yomi.client.vfx;

import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.ParticleStyle;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.post.VfxPost;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class Frost {
   public static void chips(RandomSource r, Vec3 at, Vec3 dir, int count, double speed) {
      for (int i = 0; i < VfxSettings.count((float)count); i++) {
         Vec3 v = dir.lengthSqr() < 1.0E-4
            ? new Vec3(r.nextGaussian(), 0.6 + r.nextDouble(), r.nextGaussian()).normalize()
            : dir.normalize().add(r.nextGaussian() * 0.45, 0.3 + r.nextDouble() * 0.5, r.nextGaussian() * 0.45).normalize();
         ParticleStyle s = YomiLook.CHIPS[r.nextInt(YomiLook.CHIPS.length)];
         VfxParticles.emit(
               s, at.x, at.y, at.z, v.x * speed * (0.5 + r.nextDouble()), v.y * speed * (0.5 + r.nextDouble()), v.z * speed * (0.5 + r.nextDouble())
            )
            .size(0.7F + r.nextFloat() * 0.8F);
      }
   }

   public static void glint(RandomSource r, Vec3 at, float size) {
      VfxParticles.emit(YomiLook.ICE_GLINT, at.x, at.y, at.z, 0.0, 0.0, 0.0).size(size * (0.7F + r.nextFloat() * 0.6F)).rotation(r.nextFloat() * 0.8F);
   }

   public static void vapour(RandomSource r, Vec3 at, double radius) {
      VfxParticles.emit(
            YomiLook.MIST, at.x + r.nextGaussian() * radius, at.y, at.z + r.nextGaussian() * radius, r.nextGaussian() * 0.012, -0.004, r.nextGaussian() * 0.012
         )
         .tint(14874367)
         .size(0.45F + r.nextFloat() * 0.3F)
         .alpha(0.4F);
   }

   public static void filings(RandomSource r, Vec3 at, Vec3 dir, int count) {
      for (int i = 0; i < VfxSettings.count((float)count); i++) {
         VfxParticles.emit(
            YomiLook.FILING, at.x, at.y, at.z, dir.x * 0.25 + r.nextGaussian() * 0.08, 0.05 + r.nextDouble() * 0.1, dir.z * 0.25 + r.nextGaussian() * 0.08
         );
      }
   }

   public static void snow(RandomSource r, Vec3 at, double radius, int count) {
      for (int i = 0; i < VfxSettings.count((float)count); i++) {
         VfxParticles.emit(
               YomiLook.SNOW,
               at.x + r.nextGaussian() * radius,
               at.y + r.nextDouble() * radius,
               at.z + r.nextGaussian() * radius,
               r.nextGaussian() * 0.01,
               0.01 + r.nextDouble() * 0.02,
               r.nextGaussian() * 0.01
            )
            .size(0.8F + r.nextFloat() * 0.8F);
      }
   }

   public static void breath(RandomSource r, Vec3 mouth, Vec3 look) {
      VfxParticles.emit(YomiLook.BREATH, mouth.x, mouth.y, mouth.z, look.x * 0.05 + r.nextGaussian() * 0.01, 0.006, look.z * 0.05 + r.nextGaussian() * 0.01)
         .tint(15136511)
         .size(0.8F + r.nextFloat() * 0.5F);
   }

   public static void mist(RandomSource r, Vec3 at, double radius, int count) {
      for (int i = 0; i < VfxSettings.count((float)count); i++) {
         VfxParticles.emit(
               YomiLook.MIST,
               at.x + r.nextGaussian() * radius,
               at.y + 0.2,
               at.z + r.nextGaussian() * radius,
               r.nextGaussian() * 0.015,
               0.002,
               r.nextGaussian() * 0.015
            )
            .tint(14545919)
            .alpha(0.55F);
      }
   }

   public static void chill(Vec3 at, float strength, double near) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.gameRenderer != null && mc.gameRenderer.getMainCamera() != null) {
         double d = mc.gameRenderer.getMainCamera().getPosition().distanceTo(at);
         float k = (float)Math.max(0.0, 1.0 - Math.max(0.0, d - near) / (near * 2.0)) * strength;
         if (k > 0.01F) {
            VfxPost.grade(10476799, k * 0.5F);
            VfxPost.desaturate(k * 0.25F);
         }
      }
   }

   private Frost() {
   }

   public static final class Creep {
      private final List<Frost.Creep.Patch> patches = new ArrayList<>();
      private int next;
      private int age;

      public static Frost.Creep path(Level level, RandomSource r, Vec3 a, Vec3 b, double radius, float speed) {
         Frost.Creep c = new Frost.Creep();
         double len = a.distanceTo(b);
         Vec3 dir = len < 0.001 ? new Vec3(1.0, 0.0, 0.0) : b.subtract(a).scale(1.0 / len);
         Vec3 side = new Vec3(-dir.z, 0.0, dir.x);

         for (double s = 0.0; s <= len; s += 0.6) {
            for (double o = -radius; o <= radius + 0.001; o += 0.65) {
               Vec3 p = a.add(dir.scale(s + r.nextGaussian() * 0.2)).add(side.scale(o + r.nextGaussian() * 0.2));
               c.add(level, r, p, (float)(s / (double)speed + Math.abs(o) * 2.5), 0.6F + r.nextFloat() * 0.45F - (float)Math.abs(o) * 0.1F);
            }
         }

         c.sort();
         return c;
      }

      public static Frost.Creep disc(Level level, RandomSource r, Vec3 at, double radius, float speed) {
         Frost.Creep c = new Frost.Creep();

         for (double x = -radius; x <= radius; x += 0.8) {
            for (double z = -radius; z <= radius; z += 0.8) {
               double d = Math.hypot(x, z);
               if (d <= radius) {
                  Vec3 p = at.add(x + r.nextGaussian() * 0.2, 0.0, z + r.nextGaussian() * 0.2);
                  c.add(level, r, p, (float)(d / (double)speed), (float)(1.3 - d / radius * 0.5) * (0.85F + r.nextFloat() * 0.3F));
               }
            }
         }

         c.sort();
         return c;
      }

      private void add(Level level, RandomSource r, Vec3 p, float delay, float size) {
         double top = VfxWorld.ground(level, p.x, p.y + 1.2, p.z, 4);
         if (!Double.isNaN(top)) {
            this.patches
               .add(
                  new Frost.Creep.Patch(
                     p.x, top + 0.015 + (double)r.nextFloat() * 0.01, p.z, delay, r.nextFloat() * (float) Math.PI * 2.0F, r.nextBoolean(), size
                  )
               );
         }
      }

      private void sort() {
         this.patches.sort((p, q) -> Float.compare(p.delay, q.delay));
      }

      public void tick() {
         this.age++;

         while (this.next < this.patches.size() && this.patches.get(this.next).delay <= (float)this.age) {
            Frost.Creep.Patch p = this.patches.get(this.next++);
            VfxParticles.emit(p.variant ? YomiLook.RIME_B : YomiLook.RIME, p.x, p.y, p.z, 0.0, 0.0, 0.0).size(p.size).rotation(p.rot).alpha(0.75F);
         }
      }

      public boolean done() {
         return this.next >= this.patches.size();
      }

      private static record Patch(double x, double y, double z, float delay, float rot, boolean variant, float size) {
      }
   }
}
