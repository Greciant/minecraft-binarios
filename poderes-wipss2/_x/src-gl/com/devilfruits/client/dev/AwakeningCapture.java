package com.devilfruits.client.dev;

import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.core.fx.ServerFx;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class AwakeningCapture extends CaptureStage {
   private static final int STILL_RATE = 8;
   private static final int CLIP_RATE = 5;
   private static final long SEED = 24301L;
   private final ResourceLocation fruit;
   private final List<AwakeningCapture.Take> takes;
   private final boolean look;

   public AwakeningCapture(ResourceLocation fruit, boolean look, AwakeningCapture.Take... takes) {
      super("vfx_a_");
      this.fruit = fruit;
      this.takes = List.of(takes);
      this.look = look;
   }

   @Override
   protected ResourceLocation fruit() {
      return this.fruit;
   }

   @Override
   protected void setup(ServerPlayer p) {
      command(p, "time set 6000");
   }

   private static Consumer<ServerPlayer> eat(ResourceLocation fx, int extra) {
      return p -> ServerFx.send(p, fx, p.position(), Vec3.ZERO, extra == 1 ? 2.0F : 1.0F, 24301L, extra);
   }

   private void still(AutoTest s, int tick, String name) {
      s.add(tick, mc -> s.shot(mc, name));
   }

   private int take(AutoTest s, int t, AwakeningCapture.Take x) {
      String pre = x.pre();
      Consumer<ServerPlayer> away = p -> dummyTo(s, p, -16.0);
      Consumer<Minecraft> rig = rig(x.dist(), x.yaw(), x.pitch(), 0.5);
      t = this.take(s, t, 8, away, 0, eat(x.fx(), 0), rig, x.length(), c -> {
         for (int k = 0; k <= x.length(); k += 2) {
            this.still(s, c + k, pre + String.format("seq_%03d", k));
         }
      });
      t = this.take(s, t, 8, away, 0, eat(x.fx(), 0), firstPerson(), x.length(), c -> {
         for (int k : x.firstPerson()) {
            this.still(s, c + k, pre + String.format("fp_%03d", k));
         }
      });
      if (this.look) {
         return t;
      } else {
         t = this.take(
            s,
            t,
            5,
            away,
            0,
            eat(x.fx(), 0),
            rig,
            x.length() + 30,
            c -> s.add(c - 2, mc -> VfxCapture.clip(pre + "clip", Math.min(180, (x.length() + 10) * 3 / 2), 0.6666666666666666))
         );
         t = this.take(s, t, 5, away, 0, eat(x.fx(), 0), rig, x.length() + 30, c -> s.add(c - 2, mc -> VfxCapture.clip(pre + "slow", 150, 0.3333333333333333)));
         t = this.take(s, t, 8, away, 0, eat(x.fx(), 1), rig, x.length() + 10, c -> {
            for (int k = 0; k <= x.length(); k += 4) {
               this.still(s, c + k, pre + String.format("death_%03d", k));
            }
         });
         s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.LOW));
         t = this.take(s, t + 2, 8, away, 0, eat(x.fx(), 0), rig, x.length(), c -> {
            for (int k = 0; k <= x.length(); k += 6) {
               this.still(s, c + k, pre + String.format("cmp_low_%03d", k));
            }
         });
         s.add(t, mc -> quality(CoreClientConfig.CinematicQuality.ULTRA));
         return this.take(s, t + 2, 20, away, 0, eat(x.fx(), 0), rig, x.length() + 20, c -> {
            s.add(c, mc -> VfxCapture.fps("ULTRA " + x.label()));
            s.add(c + x.length(), mc -> VfxCapture.fpsEnd());
         });
      }
   }

   @Override
   protected int takes(AutoTest s, int t) {
      t += 40;

      for (AwakeningCapture.Take x : this.takes) {
         t = this.take(s, t, x);
      }

      return t;
   }

   public static record Take(String pre, String label, ResourceLocation fx, double dist, float yaw, float pitch, int length, int[] firstPerson) {
   }
}
