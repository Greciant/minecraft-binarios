package com.devilfruits.fruits.uo_uo_seiryu.client.dev;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.CaptureRig;
import com.devilfruits.client.dev.CaptureStage;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormManager;
import com.devilfruits.core.rules.Mastery;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import com.devilfruits.registry.DFAttachments;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class SoundCheck extends CaptureStage {
   private static boolean logging;

   public SoundCheck() {
      super("vfx_snd_");
      NeoForge.EVENT_BUS.addListener(SoundCheck::onSound);
   }

   private static void onSound(PlaySoundEvent event) {
      SoundInstance s = event.getSound();
      if (logging && s != null) {
         Minecraft mc = Minecraft.getInstance();
         if (s.getSound() == null) {
            s.resolve(mc.getSoundManager());
         }

         Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
         double d = s.isRelative() ? 0.0 : cam.distanceTo(new Vec3(s.getX(), s.getY(), s.getZ()));
         float range = s.getSound() != null ? Math.max(s.getVolume(), 1.0F) * (float)s.getSound().getAttenuationDistance() : -1.0F;
         DevilFruits.LOGGER
            .info(
               "[sound] {} vol {} pitch {} range {} dist {} {}",
               new Object[]{
                  s.getLocation(),
                  String.format("%.2f", s.getVolume()),
                  String.format("%.2f", s.getPitch()),
                  String.format("%.0f", range),
                  String.format("%.1f", d),
                  d > (double)range && range > 0.0F ? "OUT OF RANGE" : ""
               }
            );
      }
   }

   @Override
   protected ResourceLocation fruit() {
      return UoUoSeiryu.ID;
   }

   @Override
   protected void setup(ServerPlayer p) {
      DFAttachments.data(p).setMastery(10);
      command(p, "time set 6000");
   }

   private static Consumer<ServerPlayer> into(String form, double lift) {
      return p -> {
         FormManager.request(p, form, FormManager.Reason.COMMAND);
         PlayerFruitData data = DFAttachments.data(p);
         data.clearCooldowns();
         data.setEnergy((float)Mastery.maxEnergy(data));
         p.teleportTo(p.serverLevel(), p.getX(), p.getY() + lift, p.getZ(), 180.0F, 20.0F);
      };
   }

   private static Consumer<Minecraft> own() {
      return mc -> {
         CaptureRig.off();
         mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
      };
   }

   private static void mark(AutoTest s, int tick, String what, boolean on) {
      s.add(tick, mc -> {
         logging = on;
         DevilFruits.LOGGER.info("[sound] ---- {} {}", what, on ? "start" : "end");
      });
   }

   @Override
   protected int takes(AutoTest s, int t) {
      t += 40;
      int[] at = new int[]{t};
      at[0] = this.take(s, at[0], 20, into("hybrid", 0.0), 20, p -> castAbility(p, "devilfruits:uo_uo_seiryu/blast_breath", 30), own(), 140, c -> {
         mark(s, c - 2, "hybrid bolo breath", true);
         mark(s, c + 138, "hybrid bolo breath", false);
      });
      at[0] = this.take(s, at[0], 20, into("dragon", 16.0), 40, p -> castAbility(p, "devilfruits:uo_uo_seiryu/blast_breath", 30), own(), 200, c -> {
         mark(s, c - 2, "dragon bolo breath", true);
         mark(s, c + 198, "dragon bolo breath", false);
      });
      at[0] = this.take(s, at[0], 20, into("dragon", 14.0), 40, p -> castAbility(p, "devilfruits:uo_uo_seiryu/bolo_breath", 30), own(), 160, c -> {
         mark(s, c - 2, "dragon meteors", true);
         mark(s, c + 158, "dragon meteors", false);
      });
      return at[0];
   }
}
