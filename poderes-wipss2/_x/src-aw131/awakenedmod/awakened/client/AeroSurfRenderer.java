package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.data.AeroVisual;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

public final class AeroSurfRenderer {
   private static final double BOARD_LEN = 1.15;
   private static final double BOARD_WID = 0.55;
   private static final double NOSE_STRETCH = 1.25;
   private static final double TAIL_SQUASH = 0.75;
   private static final double BOARD_DROP = 0.16;
   private static final int PAD_MOTES = 7;
   private static final double PAD_CARRY = 0.85;
   private static final double WAKE_MIN_SPEED = 0.12;

   private AeroSurfRenderer() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(AeroSurfRenderer::onClientTick);
   }

   private static boolean surfing(Player p) {
      return ((AeroVisual)p.getData(AwakenedAttachments.AERO_VISUAL)).gliding();
   }

   private static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && !mc.isPaused()) {
         for (Player p : mc.level.players()) {
            if (surfing(p)) {
               wake(mc, p);
            }
         }
      }
   }

   private static void wake(Minecraft mc, Player p) {
      Vec3 feet = p.position();
      Vec3 move = new Vec3(p.getX() - p.xo, p.getY() - p.yo, p.getZ() - p.zo);
      double speed = move.length();
      RandomSource rand = p.getRandom();
      if (speed < 0.12) {
         hoverPad(mc, p, feet, rand);
      } else {
         Vec3 fwd = move.scale(1.0 / speed);
         Vec3 side = fwd.cross(new Vec3(0.0, 1.0, 0.0));
         side = side.lengthSqr() > 1.0E-6 ? side.normalize() : new Vec3(1.0, 0.0, 0.0);
         Vec3 up = side.cross(fwd).normalize();
         double bank = Math.toRadians((double)AwakenedAnimations.surfBankDeg(p.getId()));
         if (Math.abs(bank) > 0.001) {
            double cos = Math.cos(bank);
            double sin = Math.sin(bank);
            Vec3 s2 = side.scale(cos).add(up.scale(sin));
            up = up.scale(cos).subtract(side.scale(sin));
            side = s2;
         }

         Vec3 center = feet.add(up.scale(-0.16));

         for (int i = 0; i < 7; i++) {
            double a = rand.nextDouble() * Math.PI * 2.0;
            double rr = Math.sqrt(rand.nextDouble());
            double along = Math.cos(a) * rr;
            along *= along > 0.0 ? 1.25 : 0.75;
            double lateral = Math.sin(a) * rr;
            Vec3 at = center.add(fwd.scale(1.15 * along)).add(side.scale(0.55 * lateral));
            Vec3 vel = move.scale(0.85).add(side.scale(-lateral * 0.05)).add(up.scale(0.01 + rand.nextDouble() * 0.02));
            mc.level.addParticle(ParticleTypes.CLOUD, at.x, at.y, at.z, vel.x, vel.y, vel.z);
         }

         Vec3 back = fwd.scale(-1.0);
         double kick = Mth.clamp(speed, 0.3, 1.4);
         int spray = (int)Math.round(1.0 + kick * 3.0);

         for (int i = 0; i < spray; i++) {
            double lateral = rand.nextDouble() * 2.0 - 1.0;
            Vec3 at = feet.add(back.scale(0.4 + rand.nextDouble() * 0.8)).add(side.scale(lateral * 0.45)).add(0.0, -0.1 + rand.nextDouble() * 0.25, 0.0);
            Vec3 vel = back.scale(0.1 + 0.1 * kick).add(side.scale(lateral * 0.1 * kick)).add(0.0, -0.02, 0.0);
            mc.level.addParticle(ParticleTypes.CLOUD, at.x, at.y, at.z, vel.x, vel.y, vel.z);
         }

         for (int s = -1; s <= 1; s += 2) {
            Vec3 at = center.add(side.scale((double)s * 0.55)).add(back.scale(rand.nextDouble() * 0.5));
            Vec3 vel = back.scale(0.08).add(0.0, 0.01, 0.0);
            mc.level.addParticle(Fx.dust(16055551, 0.6F), at.x, at.y, at.z, vel.x, vel.y, vel.z);
         }
      }
   }

   private static void hoverPad(Minecraft mc, Player p, Vec3 feet, RandomSource rand) {
      for (int i = 0; i < 2; i++) {
         double a = rand.nextDouble() * Math.PI * 2.0;
         double r = 0.55 * (0.5 + rand.nextDouble() * 0.7);
         Vec3 at = feet.add(Math.cos(a) * r, -0.16 + rand.nextDouble() * 0.1, Math.sin(a) * r);
         Vec3 vel = new Vec3(-Math.sin(a), 0.0, Math.cos(a)).scale(0.05).add(0.0, -0.01, 0.0);
         mc.level.addParticle(ParticleTypes.CLOUD, at.x, at.y, at.z, vel.x, vel.y, vel.z);
      }

      if (p.tickCount % 4 == 0) {
         double a = rand.nextDouble() * Math.PI * 2.0;
         mc.level.addParticle(Fx.dust(16055551, 0.6F), feet.x + Math.cos(a) * 0.55, feet.y - 0.16, feet.z + Math.sin(a) * 0.55, 0.0, 0.01, 0.0);
      }
   }
}
