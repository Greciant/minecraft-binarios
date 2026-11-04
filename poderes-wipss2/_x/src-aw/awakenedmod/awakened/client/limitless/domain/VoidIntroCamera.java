package com.awakenedmod.awakened.client.limitless.domain;

import com.awakenedmod.awakened.client.voidview.VoidIntroMode;
import com.awakenedmod.awakened.content.limitless.domain.VoidAnchorEntity;
import com.awakenedmod.awakened.core.Lab;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class VoidIntroCamera {
   private static final String K = "limitless.void.shot.";
   private static final Lab.Value CLOSE_FROM = Lab.CLIENT.def("limitless.void.shot.close_from", 1.7, 0.6, 4.0, 0.05);
   private static final Lab.Value CLOSE_TO = Lab.CLIENT.def("limitless.void.shot.close_to", 1.35, 0.6, 4.0, 0.05);
   private static final Lab.Value WIDE = Lab.CLIENT.def("limitless.void.shot.wide", 0.62, 0.2, 0.95, 0.01);
   private static final Lab.Value WIDE_RISE = Lab.CLIENT.def("limitless.void.shot.wide_rise", 2.2, 0.0, 6.0, 0.1);
   private static final Lab.Value WIDE_SWING = Lab.CLIENT.def("limitless.void.shot.wide_swing", 28.0, -90.0, 90.0, 1.0);

   private VoidIntroCamera() {
   }

   @Nullable
   public static VoidIntroCamera.Shot shot() {
      if (!VoidIntro.shotActive()) {
         return null;
      } else {
         Minecraft mc = Minecraft.getInstance();
         float pt = mc.getTimer().getGameTimeDeltaPartialTick(false);
         VoidIntro.State s = VoidIntro.current(pt);
         if (s != null && s.camera()) {
            VoidAnchorEntity a = s.anchor();
            Entity subject = mc.level == null ? null : mc.level.getEntity(a.casterId());
            Vec3 feet = subject != null ? subject.getPosition(pt) : a.position().subtract(0.0, 1.2, 0.0);
            double eyeH = subject != null ? (double)subject.getEyeHeight() : 1.62;
            Vec3 fwd = a.forward();
            Vec3 right = new Vec3(-fwd.z, 0.0, fwd.x);
            Vec3 sign = feet.add(0.0, eyeH - 0.3, 0.0).add(fwd.scale(0.35));
            Vec3 chest = feet.add(0.0, eyeH - 0.5, 0.0);
            float t = s.t();
            Vec3 close;
            if (s.mode() == VoidIntroMode.SHORT) {
               close = sign.add(fwd.scale(CLOSE_FROM.get() * 2.0)).add(right.scale(0.4));
            } else {
               float creep = smooth(Mth.clamp(t / 30.0F, 0.0F, 1.0F));
               double d = Mth.lerp((double)creep, CLOSE_FROM.get(), CLOSE_TO.get());
               close = sign.add(fwd.scale(d)).add(right.scale(0.22)).add(0.0, -0.08, 0.0);
            }

            float start = s.mode() == VoidIntroMode.SHORT ? 0.0F : 30.0F;
            float pull = smooth(Mth.clamp((t - start) / ((float)s.length() - start), 0.0F, 1.0F));
            double swing = Math.toRadians(WIDE_SWING.get());
            Vec3 wideDir = fwd.scale(Math.cos(swing)).add(right.scale(Math.sin(swing)));
            Vec3 wide = chest.add(wideDir.scale((double)a.radius() * WIDE.get())).add(0.0, WIDE_RISE.get(), 0.0);
            Vec3 pos = close.lerp(wide, (double)pull);
            Vec3 look = sign.lerp(chest, (double)pull);
            pos = insideSphere(pos, a);
            Vec3 dir = look.subtract(pos);
            double len = Math.max(1.0E-4, dir.length());
            float yaw = (float)Math.toDegrees(Math.atan2(-dir.x, dir.z));
            float pitch = (float)Math.toDegrees(-Math.asin(Mth.clamp(dir.y / len, -1.0, 1.0)));
            return new VoidIntroCamera.Shot(pos, yaw, pitch);
         } else {
            return null;
         }
      }
   }

   private static Vec3 insideSphere(Vec3 pos, VoidAnchorEntity a) {
      Vec3 c = a.position();
      Vec3 off = pos.subtract(c);
      double max = Math.max(1.0, (double)a.radius() - 1.0);
      return off.length() > max ? c.add(off.normalize().scale(max)) : pos;
   }

   private static float smooth(float x) {
      return x * x * (3.0F - 2.0F * x);
   }

   public static record Shot(Vec3 pos, float yaw, float pitch) {
   }
}
