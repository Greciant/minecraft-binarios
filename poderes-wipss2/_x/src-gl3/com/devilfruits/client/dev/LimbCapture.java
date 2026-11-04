package com.devilfruits.client.dev;

import com.devilfruits.DevilFruits;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class LimbCapture extends CaptureStage {
   private static final int RATE = 20;

   public LimbCapture(String prefix) {
      super(prefix);
   }

   @Override
   protected ResourceLocation fruit() {
      return DevilFruits.id("gomu_gomu");
   }

   @Override
   protected void setup(ServerPlayer p) {
      command(p, "time set 6000");
   }

   @Override
   protected void onReset(ServerPlayer p) {
      command(p, "devilfruit gomu limbtest off");
   }

   @Override
   protected int takes(AutoTest s, int t) {
      for (String skin : List.of("classic", "slim", "hd")) {
         t = this.still(s, t, skin, "12px", rig(3.4, 0.0F, 4.0F, 0.1), skin + "_rest");
         t = this.still(s, t, skin, "3", rig(6.5, 90.0F, 8.0F, 0.2), skin + "_b3");
         t = this.still(s, t, skin, "3", rig(2.3, 50.0F, 18.0F, 0.35), skin + "_close");
         t = this.still(s, t, skin, "10", rig(12.0, 90.0F, 6.0F, 0.0), skin + "_b10");
         t = this.still(s, t, skin, "20", rig(20.0, 90.0F, 5.0F, 0.0), skin + "_b20");
         t = this.still(s, t, skin, "bent", rig(10.0, 135.0F, 50.0F, 0.0), skin + "_bent");
         t = this.still(s, t, skin, "leg", rig(8.0, 60.0F, 8.0F, 1.6), skin + "_leg");
         t = this.still(s, t, skin, "10", firstPerson(), skin + "_fp");
      }

      t = this.still(s, t, "classic", "thin", rig(6.0, 90.0F, 10.0F, 0.2), "classic_thin");
      t = this.still(s, t, "classic", "twist", rig(8.0, 70.0F, 14.0F, 0.0), "classic_twist");
      return this.still(s, t, "classic", "bent", firstPerson(), "classic_fpbent");
   }

   private int still(AutoTest s, int t, String skin, String mode, Consumer<Minecraft> camera, String name) {
      return this.take(
         s,
         t,
         20,
         p -> command(p, "tp @s 0.5 " + s.origin().getY() + " -2.5 180 -1"),
         4,
         p -> command(p, "devilfruit gomu limbtest " + skin + " " + mode),
         camera,
         12,
         c -> this.shot(s, c + 6, name)
      );
   }
}
