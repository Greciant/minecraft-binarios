package com.awakenedmod.awakened.client.limitless;

import com.awakenedmod.awakened.client.SoundDuck;
import com.awakenedmod.awakened.client.render.limitless.Ease;
import com.awakenedmod.awakened.content.limitless.LimitlessCast;
import com.awakenedmod.awakened.content.limitless.purple.PurpleCast;
import com.awakenedmod.awakened.core.Lab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

final class PurpleDuck implements SoundDuck.Source {
   private static final Lab.Value OVER = Lab.CLIENT.def("limitless.purple.duck_over", 0.5, 0.0, 1.0, 0.01);
   private static final Lab.Value PULL = Lab.CLIENT.def("limitless.purple.duck_pull", 0.94, 0.0, 1.0, 0.01);
   private static final Lab.Value RETURN = Lab.CLIENT.def("limitless.purple.duck_return", 3.0, 0.0, 20.0, 1.0);
   private static final Lab.Value NEAR = Lab.CLIENT.def("limitless.purple.duck_near", 8.0, 0.0, 64.0, 1.0);
   private static final Lab.Value FAR = Lab.CLIENT.def("limitless.purple.duck_far", 28.0, 1.0, 96.0, 1.0);
   private static final float STEP = 0.004F;
   private float gain = 1.0F;

   private PurpleDuck() {
   }

   static void register() {
      SoundDuck.add(new PurpleDuck());
   }

   @Override
   public boolean tick(Minecraft mc) {
      float next = 1.0F;
      if (mc.level != null && mc.player != null) {
         long now = mc.level.getGameTime();
         Vec3 ear = mc.player.getEyePosition();
         float near = NEAR.getF();
         float far = Math.max(near + 1.0F, FAR.getF());
         float over = OVER.getF();
         float pull = PULL.getF();
         float back = RETURN.getF();

         for (Player p : mc.level.players()) {
            float depth = depth(PurpleCast.of(p), now, over, pull, back);
            if (!(depth <= 0.0F)) {
               double d = ear.distanceTo(p.getEyePosition());
               float k = d <= (double)near ? 1.0F : Math.max(0.0F, 1.0F - (float)(d - (double)near) / (far - near));
               next = Math.min(next, 1.0F - depth * k);
            }
         }
      }

      boolean changed = Math.abs(next - this.gain) > 0.004F || next >= 1.0F && this.gain < 1.0F;
      if (changed) {
         this.gain = next;
      }

      return changed;
   }

   private static float depth(PurpleCast cast, long now, float over, float pull, float back) {
      LimitlessCast c = cast.cast();
      float beats = over * cast.beats((double)now) / 4.0F;
      if (c.live()) {
         return cast.pulling() ? beats + (pull - beats) * Ease.smooth(cast.pullIn((double)now)) : beats;
      } else {
         return c.state() == 2 && cast.pulling() && back > 0.0F ? pull * (1.0F - Ease.clamp01((float)(now - (long)c.end()) / back)) : 0.0F;
      }
   }

   @Override
   public float gain(SoundInstance sound) {
      return this.gain;
   }
}
