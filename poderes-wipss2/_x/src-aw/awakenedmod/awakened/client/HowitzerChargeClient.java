package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.AwakenedParticles;
import com.awakenedmod.awakened.content.nuclear.HowitzerAbility;
import com.awakenedmod.awakened.content.particle.NukeFxOptions;
import com.awakenedmod.awakened.core.AwId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.common.NeoForge;

public final class HowitzerChargeClient {
   private static final ClipClock CHARGE = new ClipClock(AwId.of("howitzer"));
   private static final double SIZE = 1.45;
   private static final Map<Integer, HowitzerChargeClient.Charge> CHARGES = new HashMap<>();

   private HowitzerChargeClient() {
   }

   public static void register() {
      NeoForge.EVENT_BUS.addListener(HowitzerChargeClient::onClientTick);
   }

   private static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      Level level = mc.level;
      if (level != null && !mc.isPaused()) {
         CHARGES.values().forEach(c -> c.seen = false);
         ClipBearers.forEachCaster(level, body -> tick(mc, body));
         CHARGES.values().removeIf(c -> !c.seen || c.particles.isEmpty());
      } else {
         CHARGES.clear();
      }
   }

   private static void tick(Minecraft mc, LivingEntity body) {
      long age = CHARGE.age(body);
      HowitzerChargeClient.Charge charge = CHARGES.get(body.getId());
      Vec3 muzzle = HowitzerAbility.muzzle(body);
      if (charge != null) {
         charge.seen = true;
         carry(charge, muzzle);
      }

      if (age >= 0L) {
         if (charge == null) {
            charge = new HowitzerChargeClient.Charge();
            charge.muzzle = muzzle;
            charge.seen = true;
            CHARGES.put(body.getId(), charge);
         }

         double f = Math.min(1.0, (double)(age + 1L) / 32.0);
         spawn(mc, charge, AwakenedParticles.BURST, muzzle, (0.12 + f * 0.3) * 1.45);
         if (f > 0.6) {
            spawn(mc, charge, AwakenedParticles.SPARK, muzzle, 0.23199999999999998);
         }
      }
   }

   private static void carry(HowitzerChargeClient.Charge charge, Vec3 muzzle) {
      Vec3 d = muzzle.subtract(charge.muzzle);
      charge.muzzle = muzzle;
      Iterator<Particle> it = charge.particles.iterator();

      while (it.hasNext()) {
         Particle p = it.next();
         if (!p.isAlive()) {
            it.remove();
         } else if (d.lengthSqr() > 0.0) {
            p.move(d.x, d.y, d.z);
         }
      }
   }

   private static void spawn(Minecraft mc, HowitzerChargeClient.Charge charge, Supplier<ParticleType<NukeFxOptions>> type, Vec3 at, double scale) {
      Particle p = mc.particleEngine.createParticle(new NukeFxOptions(type.get(), (float)scale), at.x, at.y, at.z, 0.0, 0.0, 0.0);
      if (p != null) {
         charge.particles.add(p);
      }
   }

   private static final class Charge {
      private final List<Particle> particles = new ArrayList<>();
      private Vec3 muzzle;
      private boolean seen;
   }
}
