package com.awakenedmod.awakened.client.limitless;

import com.awakenedmod.awakened.client.limitless.domain.VoidClient;
import com.awakenedmod.awakened.client.render.limitless.LimitlessFrame;
import com.awakenedmod.awakened.client.render.limitless.blue.BlueCue;
import com.awakenedmod.awakened.client.render.limitless.blue.BlueEnding;
import com.awakenedmod.awakened.client.render.limitless.blue.BlueLapse;
import com.awakenedmod.awakened.client.render.limitless.infinity.InfinityCue;
import com.awakenedmod.awakened.client.render.limitless.infinity.InfinityZeno;
import com.awakenedmod.awakened.client.render.limitless.sixeyes.SixEyesAura;
import com.awakenedmod.awakened.client.render.limitless.sixeyes.SixEyesCue;
import com.awakenedmod.awakened.client.render.limitless.voidlook.VoidGalaxy;
import com.awakenedmod.awakened.content.limitless.blue.BlueEnd;
import com.awakenedmod.awakened.content.limitless.blue.BlueOrbEntity;
import com.awakenedmod.awakened.content.limitless.domain.VoidAnchorEntity;
import com.awakenedmod.awakened.content.limitless.purple.PurpleSphereEntity;
import com.awakenedmod.awakened.content.limitless.red.RedBlastEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

public final class LimitlessFxClient {
   private static final BlueLapse BLUE = new BlueLapse();
   private static final InfinityZeno ZENO = new InfinityZeno();
   private static final SixEyesAura AURA = new SixEyesAura();
   private static final VoidGalaxy VOID = new VoidGalaxy();

   private LimitlessFxClient() {
   }

   public static void register(IEventBus gameBus) {
      LimitlessFrame.register(gameBus, LimitlessFxClient::draw);
      gameBus.addListener(LimitlessFxClient::onTick);
      gameBus.addListener(LimitlessFxClient::onLoggingOut);
      gameBus.addListener(PurpleCrumbs::onLeave);
      PurpleFov.register(gameBus);
      PurpleDuck.register();
      VoidClient.register(gameBus);
   }

   private static void draw(LimitlessFrame f) {
      RedFxClient.begin();
      PurpleFxClient.begin();

      for (Entity e : f.level.entitiesForRendering()) {
         if (e instanceof BlueOrbEntity orb) {
            drawOrb(f, orb);
         } else if (e instanceof VoidAnchorEntity a) {
            VoidClient.draw(f, a, VOID);
         } else if (e instanceof RedBlastEntity shot) {
            RedFxClient.drawShot(f, shot);
         } else if (e instanceof PurpleSphereEntity sphere) {
            PurpleFxClient.drawSphere(f, sphere);
         }
      }

      if (!InfinityStops.isEmpty()) {
         ZENO.draw(f, (InfinityCue)InfinityStops.INSTANCE, 0.0F);
      }

      if (SixEyesSight.INSTANCE.prepare(f)) {
         AURA.draw(f, (SixEyesCue)SixEyesSight.INSTANCE, SixEyesSight.INSTANCE.age());
      }

      RedFxClient.drawCasts(f);
      PurpleFxClient.drawCasts(f);
      RctFxClient.draw(f);
   }

   private static void drawOrb(LimitlessFrame f, BlueOrbEntity orb) {
      float age = Math.max(0.0F, orb.age(f.partialTick));
      BlueCue cue = blueCue(orb);
      if (age <= cue.release() + 12.0F) {
         BLUE.draw(f, cue, age);
         BlueEnding.draw(f, cue, age);
      }
   }

   private static BlueCue blueCue(BlueOrbEntity orb) {
      int releaseAt = orb.releaseAt();
      final float release = releaseAt == Integer.MAX_VALUE ? Float.MAX_VALUE : (float)releaseAt;
      final Vec3 at = orb.position();
      final long seed = orb.seed();
      final BlueEnd ending = orb.ending();
      return new BlueCue() {
         @Override
         public Vec3 orb() {
            return at;
         }

         @Override
         public long seed() {
            return seed;
         }

         @Override
         public float release() {
            return release;
         }

         @Override
         public BlueEnd ending() {
            return ending;
         }
      };
   }

   private static void onTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && !mc.isPaused()) {
         InfinityStops.tick(mc.level);
         SixEyesSight.INSTANCE.tick(mc);

         for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof RedBlastEntity shot) {
               RedFxClient.tickShot(mc.level, shot);
            } else if (e instanceof BlueOrbEntity orb) {
               BLUE.tick(mc.level, blueCue(orb), (int)orb.age(0.0F));
            } else if (e instanceof PurpleSphereEntity sphere) {
               PurpleFxClient.tickSphere(mc.level, sphere);
            }
         }

         PurpleFxClient.tickCasts(mc.level);
         PurpleFxClient.endTick();
      }
   }

   private static void onLoggingOut(LoggingOut event) {
      InfinityStops.clear();
      SixEyesSight.INSTANCE.reset();
      PurpleFxClient.clear();
   }
}
