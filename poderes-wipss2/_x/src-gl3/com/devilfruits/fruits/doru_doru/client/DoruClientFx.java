package com.devilfruits.fruits.doru_doru.client;

import com.devilfruits.client.fx.ClientFxRegistry;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.core.network.FxPayload;
import com.devilfruits.fruits.doru_doru.DoruFx;
import com.devilfruits.fruits.doru_doru.DoruItems;
import com.devilfruits.fruits.doru_doru.client.vfx.DoruLook;
import com.devilfruits.fruits.doru_doru.client.vfx.ServiceClient;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;

public final class DoruClientFx {
   static void register() {
      ClientFxRegistry.register(DoruFx.ABSORB, DoruClientFx::absorb);
      ClientFxRegistry.register(DoruFx.HEAT_WARNING, DoruClientFx::heatWarning);
      ClientFxRegistry.register(DoruFx.TOOL_FORM, DoruClientFx::toolForm);
      ClientFxRegistry.register(DoruFx.WALL_RISE, DoruClientFx::wallRise);
      ClientFxRegistry.register(DoruFx.LANCE_FORM, DoruClientFx::lanceForm);
      ClientFxRegistry.register(DoruFx.LANCE_THRUST, DoruClientFx::lanceThrust);
      ClientFxRegistry.register(DoruFx.LANCE_THROW, DoruClientFx::lanceThrow);
      ClientFxRegistry.register(DoruFx.LANCE_HIT, DoruClientFx::splat);
      ClientFxRegistry.register(DoruFx.LANCE_EMBED, DoruClientFx::splat);
      ClientFxRegistry.register(DoruFx.ENCASE, DoruClientFx::encase);
      ClientFxRegistry.register(DoruFx.BREAK_FREE, DoruClientFx::breakFree);
      ClientFxRegistry.register(DoruFx.MELT_OFF, DoruClientFx::meltOff);
      ClientFxRegistry.register(DoruFx.STRUGGLE, DoruClientFx::struggle);
      ClientFxRegistry.register(DoruFx.SERVICE, (level, e, p) -> ServiceClient.pulse(level, p));
      ClientFxRegistry.register(DoruFx.SERVICE_END, (level, e, p) -> ServiceClient.end(level, p));
      ClientFxRegistry.register(DoruFx.CHAMPION_RISE, DoruClientFx::championRise);
      ClientFxRegistry.register(DoruFx.CHAMPION_MELT, DoruClientFx::championMelt);
      ClientFxRegistry.register(DoruFx.CHAMPION_PUNCH, DoruClientFx::championPunch);
      ClientFxRegistry.register(DoruFx.CHAMPION_STOMP, DoruClientFx::championStomp);
   }

   private static double jit(RandomSource r, double a) {
      return (r.nextDouble() * 2.0 - 1.0) * a;
   }

   private static ItemParticleOption lump() {
      return new ItemParticleOption(ParticleTypes.ITEM, new ItemStack((ItemLike)DoruItems.WAX_LUMP.get()));
   }

   private static Vec3 hand(@Nullable Entity e, Vec3 fallback) {
      return e instanceof Player p ? DoruLook.hand(p, 1.0F, true, VfxWorld.firstPersonCaster(p.getId())) : fallback;
   }

   private static void beads(ClientLevel level, Vec3 at, float n, double spread, double up, int tint) {
      RandomSource r = level.random;
      if (VfxSettings.legacy()) {
         ItemParticleOption wax = lump();

         for (int i = 0; i < Math.max(1, (int)(n / 2.0F)); i++) {
            level.addParticle(wax, at.x, at.y, at.z, jit(r, spread), up * r.nextDouble(), jit(r, spread));
         }
      } else {
         float light = VfxWorld.light(level, at.x, at.y, at.z);

         for (int i = 0; i < VfxSettings.count(n); i++) {
            VfxParticles.emit(DoruLook.BEAD, at.x, at.y, at.z, jit(r, spread), up * (0.4 + 0.6 * r.nextDouble()), jit(r, spread))
               .tint(VfxWorld.shade(tint, light))
               .size(0.7F + r.nextFloat() * 0.7F);
         }
      }
   }

   private static void splash(ClientLevel level, Vec3 at, float size, int tint) {
      RandomSource r = level.random;
      if (VfxSettings.legacy()) {
         level.addParticle(ParticleTypes.WHITE_SMOKE, at.x, at.y, at.z, 0.0, 0.02, 0.0);
      } else {
         float light = VfxWorld.light(level, at.x, at.y, at.z);
         VfxParticles.emit(DoruLook.SPLASH, at.x, at.y, at.z, 0.0, 0.0, 0.0)
            .tint(VfxWorld.shade(tint, light))
            .size(size)
            .rotation(r.nextFloat() * (float) (Math.PI * 2));
         VfxParticles.emit(DoruLook.GLINT, at.x, at.y + 0.1, at.z, 0.0, 0.01, 0.0).size(size).rotation(r.nextFloat() * (float) (Math.PI * 2));
         double ground = VfxWorld.ground(level, at.x, at.y, at.z, 3);
         if (!Double.isNaN(ground) && at.y - ground < 2.5) {
            VfxParticles.emit(DoruLook.PUDDLE, at.x + jit(r, 0.2), ground + 0.02, at.z + jit(r, 0.2), 0.0, 0.0, 0.0)
               .tint(VfxWorld.shade(tint, VfxWorld.surfaceLight(level, at.x, ground, at.z)))
               .size(size * 0.9F)
               .rotation(r.nextFloat() * (float) (Math.PI * 2));
         }
      }
   }

   private static void steam(ClientLevel level, Vec3 at, float n, double spread) {
      RandomSource r = level.random;

      for (int i = 0; i < (VfxSettings.legacy() ? 2 : VfxSettings.count(n)); i++) {
         double x = at.x + jit(r, spread);
         double y = at.y + r.nextDouble() * spread;
         double z = at.z + jit(r, spread);
         if (VfxSettings.legacy()) {
            level.addParticle(ParticleTypes.WHITE_SMOKE, x, y, z, 0.0, 0.03, 0.0);
         } else {
            VfxParticles.emit(DoruLook.STEAM_PUFF, x, y, z, 0.0, 0.02, 0.0).tint(15921388).alpha(0.35F);
         }
      }
   }

   private static void absorb(ClientLevel level, @Nullable Entity e, FxPayload p) {
      Vec3 at = p.pos();
      ItemParticleOption wax = lump();

      for (int i = 0; i < 8; i++) {
         level.addParticle(wax, at.x + jit(level.random, 0.25), at.y + jit(level.random, 0.2), at.z + jit(level.random, 0.25), 0.0, 0.03, 0.0);
      }
   }

   private static void heatWarning(ClientLevel level, @Nullable Entity e, FxPayload p) {
      if (e != null) {
         ItemParticleOption wax = lump();

         for (int i = 0; i < 10; i++) {
            level.addParticle(
               wax, e.getX() + jit(level.random, 0.35), e.getY() + 0.4 + level.random.nextDouble() * 1.3, e.getZ() + jit(level.random, 0.35), 0.0, -0.04, 0.0
            );
            level.addParticle(
               ParticleTypes.WHITE_SMOKE,
               e.getX() + jit(level.random, 0.4),
               e.getY() + level.random.nextDouble() * 1.8,
               e.getZ() + jit(level.random, 0.4),
               0.0,
               0.03,
               0.0
            );
         }
      }
   }

   private static void toolForm(ClientLevel level, @Nullable Entity e, FxPayload p) {
      Vec3 at = hand(e, p.pos().add(p.dir().scale(0.6)));
      boolean fp = e != null && VfxWorld.firstPersonCaster(e.getId());
      RandomSource r = level.random;
      if (!VfxSettings.legacy()) {
         for (int i = 0; i < VfxSettings.count(fp ? 5.0F : 9.0F); i++) {
            Vec3 from = at.add(jit(r, 0.35), jit(r, 0.3), jit(r, 0.35));
            Vec3 v = at.subtract(from).scale(0.18);
            VfxParticles.emit(DoruLook.BEAD, from.x, from.y, from.z, v.x, v.y + 0.04, v.z).tint(16776696).size(0.6F).life(0.6F);
         }

         VfxParticles.emit(DoruLook.GLINT, at.x, at.y, at.z, 0.0, 0.0, 0.0).size(fp ? 0.6F : 1.0F);
      } else {
         beads(level, at, 6.0F, 0.05, 0.05, 16776696);
      }

      steam(level, at, fp ? 1.0F : 2.0F, 0.15);
   }

   private static void wallRise(ClientLevel level, @Nullable Entity e, FxPayload p) {
      RandomSource r = RandomSource.create(p.seed());
      Vec3 base = p.pos().add(0.0, 0.1, 0.0);
      boolean dome = p.extra() == 1;
      int n = dome ? 14 : Math.max(3, Mth.ceil(p.power()));
      Vec3 f = new Vec3(p.dir().x, 0.0, p.dir().z);
      f = f.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : f.normalize();
      Vec3 side = new Vec3(-f.z, 0.0, f.x);

      for (int i = 0; i < n; i++) {
         Vec3 at;
         if (dome) {
            double a = (double)((float)i * (float) (Math.PI * 2) / (float)n);
            at = base.add(Math.cos(a) * (double)p.power(), 0.0, Math.sin(a) * (double)p.power());
         } else {
            at = base.add(side.scale((double)i + 0.5 - (double)n / 2.0));
         }

         splash(level, at.add(jit(r, 0.2), 0.0, jit(r, 0.2)), 0.9F + r.nextFloat() * 0.4F, 16776696);
         beads(level, at.add(0.0, 0.3, 0.0), 2.5F, 0.06, 0.28, 16776696);
         if (i % 2 == 0) {
            steam(level, at.add(0.0, 0.6, 0.0), 1.5F, 0.5);
         }
      }
   }

   private static void lanceForm(ClientLevel level, @Nullable Entity e, FxPayload p) {
      Vec3 at = hand(e, p.pos());
      Vec3 look = p.dir().normalize();
      RandomSource r = level.random;
      if (VfxSettings.legacy()) {
         beads(level, at, 4.0F, 0.03, 0.03, 16776696);
      } else {
         int tint = DoruLook.tint(p.extra());

         for (int i = 0; i < VfxSettings.count(10.0F); i++) {
            double t = r.nextDouble() * 1.5;
            Vec3 q = at.add(look.scale(t)).add(jit(r, 0.05), jit(r, 0.05), jit(r, 0.05));
            VfxParticles.emit(DoruLook.BEAD, q.x, q.y, q.z, look.x * 0.05, look.y * 0.05 + 0.02, look.z * 0.05).tint(tint).size(0.5F).life(0.5F);
         }

         VfxParticles.emit(DoruLook.GLINT, at.x, at.y, at.z, 0.0, 0.0, 0.0).size(0.8F);
      }
   }

   private static void lanceThrust(ClientLevel level, @Nullable Entity e, FxPayload p) {
      int tint = DoruLook.tint(p.extra());
      Vec3 from = hand(e, p.pos());
      Vec3 d = p.dir();
      double len = d.length();
      Vec3 dir = len < 1.0E-4 ? Vec3.ZERO : d.scale(1.0 / len);
      RandomSource r = level.random;
      if (!VfxSettings.legacy()) {
         for (int i = 0; i < VfxSettings.count(12.0F); i++) {
            double t = 0.3 + r.nextDouble() * (len - 0.3);
            Vec3 q = from.add(dir.scale(t));
            VfxParticles.emit(DoruLook.BEAD, q.x, q.y, q.z, dir.x * 0.12 + jit(r, 0.04), dir.y * 0.12 + 0.05, dir.z * 0.12 + jit(r, 0.04))
               .tint(tint)
               .size(0.6F + r.nextFloat() * 0.5F);
         }

         Vec3 tip = from.add(d);
         VfxParticles.emit(DoruLook.FRONT_GLOW, tip.x, tip.y, tip.z, 0.0, 0.0, 0.0).tint(16767400).alpha(0.4F);
      }

      beads(level, from.add(d), 4.0F, 0.08, 0.15, tint);
   }

   private static void lanceThrow(ClientLevel level, @Nullable Entity e, FxPayload p) {
      int tint = DoruLook.tint(p.extra());
      Vec3 at = hand(e, p.pos());
      Vec3 v = p.dir().scale(0.15);
      RandomSource r = level.random;
      if (VfxSettings.legacy()) {
         beads(level, at, 4.0F, 0.05, 0.05, tint);
      } else {
         for (int i = 0; i < VfxSettings.count(8.0F); i++) {
            VfxParticles.emit(DoruLook.BEAD, at.x, at.y, at.z, v.x + jit(r, 0.05), v.y + 0.03, v.z + jit(r, 0.05)).tint(tint).size(0.6F);
         }
      }
   }

   private static void splat(ClientLevel level, @Nullable Entity e, FxPayload p) {
      int tint = DoruLook.tint(p.extra());
      Vec3 at = p.pos();
      Vec3 back = p.dir().lengthSqr() < 1.0E-6 ? Vec3.ZERO : p.dir().normalize().scale(-0.12);
      splash(level, at, 1.2F, tint);
      beads(level, at.add(back.scale(2.0)), 7.0F, 0.12, 0.22, tint);
      steam(level, at, 2.0F, 0.3);
   }

   private static void encase(ClientLevel level, @Nullable Entity e, FxPayload p) {
      if (e != null) {
         RandomSource r = level.random;
         double w = (double)e.getBbWidth() * 0.6;
         double h = (double)e.getBbHeight();
         int rings = Math.max(2, Mth.ceil(h / 0.5));

         for (int ring = 0; ring < rings; ring++) {
            double y = e.getY() + ((double)ring + 0.5) * h / (double)rings;

            for (int k = 0; k < 4; k++) {
               double a = (double)((float)k * (float) (Math.PI / 2)) + (double)ring * 0.7 + r.nextDouble() * 0.4;
               Vec3 at = new Vec3(e.getX() + Math.cos(a) * w, y, e.getZ() + Math.sin(a) * w);
               if (VfxSettings.legacy()) {
                  level.addParticle(lump(), at.x, at.y, at.z, 0.0, 0.0, 0.0);
               } else {
                  VfxParticles.emit(DoruLook.SPLASH, at.x, at.y, at.z, 0.0, 0.0, 0.0)
                     .tint(VfxWorld.shade(16776696, VfxWorld.light(level, at.x, at.y, at.z)))
                     .size(0.8F)
                     .life(1.4F)
                     .rotation(r.nextFloat() * (float) (Math.PI * 2));
               }
            }
         }

         if (!VfxSettings.legacy()) {
            VfxParticles.emit(DoruLook.GLINT, e.getX(), e.getY() + h * 0.7, e.getZ(), 0.0, 0.0, 0.0).size(1.6F);
         }

         steam(level, e.position().add(0.0, h * 0.5, 0.0), 4.0F, w + 0.2);
      }
   }

   private static void breakFree(ClientLevel level, @Nullable Entity e, FxPayload p) {
      if (e != null) {
         RandomSource r = level.random;
         double h = (double)e.getBbHeight();
         Vec3 mid = e.position().add(0.0, h * 0.5, 0.0);
         if (!VfxSettings.legacy()) {
            for (int i = 0; i < VfxSettings.count(5.0F); i++) {
               VfxParticles.emit(DoruLook.CRACK, mid.x + jit(r, 0.3), mid.y + jit(r, h * 0.4), mid.z + jit(r, 0.3), 0.0, 0.0, 0.0)
                  .tint(16776696)
                  .rotation(r.nextFloat() * (float) (Math.PI * 2));
            }

            for (int i = 0; i < VfxSettings.count(14.0F); i++) {
               VfxParticles.emit(
                     DoruLook.FLAKES[r.nextInt(DoruLook.FLAKES.length)],
                     mid.x,
                     mid.y + jit(r, h * 0.4),
                     mid.z,
                     jit(r, 0.2),
                     0.05 + r.nextDouble() * 0.15,
                     jit(r, 0.2)
                  )
                  .tint(16776696)
                  .life(0.4F)
                  .size(1.6F);
            }
         }

         beads(level, mid, 10.0F, 0.18, 0.25, 16776696);
      }
   }

   private static void meltOff(ClientLevel level, @Nullable Entity e, FxPayload p) {
      if (e != null) {
         RandomSource r = level.random;
         double h = (double)e.getBbHeight();

         for (int i = 0; i < 6; i++) {
            Vec3 at = e.position().add(jit(r, (double)e.getBbWidth() * 0.5), r.nextDouble() * h, jit(r, (double)e.getBbWidth() * 0.5));
            beads(level, at, 1.5F, 0.02, -0.02, 16776696);
         }

         steam(level, e.position().add(0.0, h * 0.4, 0.0), 6.0F, 0.5);
      }
   }

   private static void struggle(ClientLevel level, @Nullable Entity e, FxPayload p) {
      if (e != null) {
         Vec3 at = e.position().add(0.0, (double)e.getBbHeight() * (0.4 + level.random.nextDouble() * 0.4), 0.0);
         beads(level, at, 3.0F, 0.12, 0.15, 16776696);
         if (!VfxSettings.legacy() && !VfxWorld.firstPersonCaster(e.getId())) {
            VfxParticles.emit(DoruLook.CRACK, at.x, at.y, at.z, 0.0, 0.0, 0.0)
               .tint(16776696)
               .size(0.5F)
               .rotation(level.random.nextFloat() * (float) (Math.PI * 2));
         }
      }
   }

   private static void ring(ClientLevel level, Vec3 at, double radius, int n, float size, double up) {
      RandomSource r = level.random;

      for (int i = 0; i < n; i++) {
         double a = ((double)i + r.nextDouble() * 0.5) * (float) (Math.PI * 2) / (double)n;
         Vec3 q = at.add(Math.cos(a) * radius, 0.05, Math.sin(a) * radius);
         splash(level, q, size * (0.8F + r.nextFloat() * 0.4F), 16776696);
         beads(level, q.add(0.0, 0.2, 0.0), 2.0F, 0.05, up, 16776696);
      }
   }

   private static void championRise(ClientLevel level, @Nullable Entity e, FxPayload p) {
      if (e != null) {
         Vec3 at = e.position();
         ring(level, at, 1.2, 8, 1.3F, 0.25);
         ring(level, at, 2.2, 12, 1.0F, 0.18);
         beads(level, at.add(0.0, 0.5, 0.0), 16.0F, 0.12, 0.45, 16776696);
         steam(level, at.add(0.0, 1.2, 0.0), 10.0F, 1.2);
         if (!VfxSettings.legacy()) {
            VfxParticles.emit(DoruLook.GLINT, at.x, at.y + 2.2, at.z, 0.0, 0.02, 0.0).size(2.5F);
         }
      }
   }

   private static void championMelt(ClientLevel level, @Nullable Entity e, FxPayload p) {
      if (e != null) {
         Vec3 at = e.position();
         ring(level, at, 1.0, 8, 1.4F, 0.08);
         ring(level, at, 2.0, 12, 1.2F, 0.05);
         steam(level, at.add(0.0, 0.8, 0.0), 12.0F, 1.4);
         meltOff(level, e, p);
      }
   }

   private static void championPunch(ClientLevel level, @Nullable Entity e, FxPayload p) {
      Vec3 at = p.pos();
      splash(level, at, 1.8F, 16776696);
      Vec3 d = p.dir().lengthSqr() < 1.0E-6 ? Vec3.ZERO : p.dir().normalize();
      beads(level, at.add(d.scale(0.4)), 9.0F, 0.16, 0.25, 16776696);
      if (!VfxSettings.legacy()) {
         VfxParticles.emit(DoruLook.FRONT_GLOW, at.x, at.y, at.z, 0.0, 0.0, 0.0).tint(16767400).alpha(0.35F).size(1.6F);
      }
   }

   private static void championStomp(ClientLevel level, @Nullable Entity e, FxPayload p) {
      Vec3 at = p.pos();
      double r = Math.max(2.0, (double)p.power());
      ring(level, at, r * 0.4, 8, 1.6F, 0.3);
      ring(level, at, r * 0.8, 14, 1.2F, 0.22);

      for (int i = 0; i < 16; i++) {
         double a = (double)((float)i * (float) (Math.PI * 2) / 16.0F);
         level.addParticle(
            ParticleTypes.POOF, at.x + Math.cos(a) * r * 0.6, at.y + 0.1, at.z + Math.sin(a) * r * 0.6, Math.cos(a) * 0.12, 0.02, Math.sin(a) * 0.12
         );
      }

      steam(level, at.add(0.0, 0.4, 0.0), 6.0F, r * 0.5);
   }

   private DoruClientFx() {
   }
}
