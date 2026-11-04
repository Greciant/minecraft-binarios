package com.devilfruits.fruits.doru_doru.client.vfx;

import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.core.network.FxPayload;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

public final class ServiceClient {
   private static final Long2ObjectOpenHashMap<ServiceClient.State> STATES = new Long2ObjectOpenHashMap();

   private static long key(Vec3 plate) {
      return BlockPos.containing(plate).asLong();
   }

   public static void pulse(ClientLevel level, FxPayload p) {
      STATES.put(key(p.pos()), new ServiceClient.State(p.pos(), p.dir().x, (float)p.dir().y, p.dir().z, p.extra(), level.getGameTime()));
   }

   public static void end(ClientLevel level, FxPayload p) {
      ServiceClient.State s = (ServiceClient.State)STATES.remove(key(p.pos()));
      if (s == null) {
         s = new ServiceClient.State(p.pos(), p.dir().x, (float)p.dir().y, p.dir().z, p.extra(), level.getGameTime());
      }

      for (int i = 0; i <= s.candles(); i++) {
         Vec3 w = wick(s, i);
         if (w != null) {
            for (int k = 0; k < 3; k++) {
               level.addParticle(ParticleTypes.SMOKE, w.x, w.y + 0.1, w.z, 0.0, 0.04, 0.0);
            }
         }
      }
   }

   private static Vec3 wick(ServiceClient.State s, int i) {
      boolean middle = i == s.candles();
      double full = middle ? 3.0 : 2.25;
      double h = Math.ceil(full * 4.0 * (double)s.left()) / 4.0;
      if (h <= 0.0) {
         return null;
      } else if (middle) {
         return s.plate().add(0.0, h, 0.0);
      } else {
         double a = (double)((float)i * (float) (Math.PI * 2) / (float)s.candles());
         return s.plate().add(Math.cos(a) * s.ring(), h, Math.sin(a) * s.ring());
      }
   }

   public static void tick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      ClientLevel level = mc.level;
      if (level == null) {
         STATES.clear();
      } else if (!mc.isPaused() && !STATES.isEmpty()) {
         long now = level.getGameTime();
         RandomSource r = level.random;
         boolean low = VfxSettings.legacy();
         Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
         STATES.values().removeIf(sx -> now - sx.seen() > 40L);
         ObjectIterator var8 = STATES.values().iterator();

         while (var8.hasNext()) {
            ServiceClient.State s = (ServiceClient.State)var8.next();
            if (!(s.plate().distanceToSqr(cam) > 25600.0)) {
               for (int i = 0; i <= s.candles(); i++) {
                  Vec3 w = wick(s, i);
                  if (w != null) {
                     if (low) {
                        if ((now + (long)i) % 4L == 0L) {
                           level.addParticle(ParticleTypes.SMALL_FLAME, w.x, w.y + 0.1, w.z, 0.0, 0.005, 0.0);
                        }
                     } else {
                        boolean middle = i == s.candles();
                        VfxParticles.emit(DoruLook.FLAME, w.x, w.y + (middle ? 0.42 : 0.32), w.z, 0.0, 0.004, 0.0)
                           .size(middle ? 1.5F : 1.15F)
                           .rotation(Mth.sin((float)(now + (long)(i * 7)) * 0.31F) * 0.08F)
                           .unflipped();
                        if (r.nextInt(14) == 0) {
                           VfxParticles.emit(DoruLook.EMBER, w.x, w.y + 0.4, w.z, (r.nextDouble() - 0.5) * 0.02, 0.03, (r.nextDouble() - 0.5) * 0.02);
                        }

                        if (r.nextInt(3) == 0) {
                           double a = r.nextDouble() * (float) (Math.PI * 2);
                           double v = 0.04 + r.nextDouble() * 0.08;
                           VfxParticles.emit(DoruLook.FLAKES[r.nextInt(DoruLook.FLAKES.length)], w.x, w.y + 0.5, w.z, Math.cos(a) * v, 0.02, Math.sin(a) * v)
                              .tint(16776696);
                        }
                     }
                  }
               }

               if (!(s.left() <= 0.0F)) {
                  int n = low ? 1 : VfxSettings.count(6.0F);

                  for (int k = 0; k < n; k++) {
                     double a = r.nextDouble() * (float) (Math.PI * 2);
                     double d = s.radius() * Math.sqrt(r.nextDouble());
                     double x = s.plate().x + Math.cos(a) * d;
                     double z = s.plate().z + Math.sin(a) * d;
                     double y = s.plate().y + 2.5 + r.nextDouble() * 4.0;
                     if (level.canSeeSky(BlockPos.containing(x, y, z))) {
                        if (low) {
                           level.addParticle(ParticleTypes.WHITE_ASH, x, y, z, 0.0, -0.02, 0.0);
                        } else {
                           VfxParticles.emit(
                                 DoruLook.FLAKES[r.nextInt(DoruLook.FLAKES.length)],
                                 x,
                                 y,
                                 z,
                                 (r.nextDouble() - 0.5) * 0.02,
                                 -0.01,
                                 (r.nextDouble() - 0.5) * 0.02
                              )
                              .tint(16776696);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static void clear() {
      STATES.clear();
   }

   private ServiceClient() {
   }

   private static record State(Vec3 plate, double ring, float left, double radius, int candles, long seen) {
   }
}
