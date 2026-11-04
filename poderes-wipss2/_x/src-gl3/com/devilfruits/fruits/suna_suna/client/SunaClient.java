package com.devilfruits.fruits.suna_suna.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.logia.LogiaThemes;
import com.devilfruits.fruits.suna_suna.SunaSuna;
import com.devilfruits.fruits.suna_suna.block.QuicksandBlock;
import com.devilfruits.fruits.suna_suna.client.dev.SandBlowCheck;
import com.devilfruits.fruits.suna_suna.client.dev.SunaAutotest;
import com.devilfruits.fruits.suna_suna.client.dev.SunaCapture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.bus.api.IEventBus;

public final class SunaClient {
   public static void init(IEventBus modBus) {
      SunaClientFx.register();
      SandTravelClient.register();
      LogiaThemes.register(SunaSuna.ELEMENT, SunaTheme.create());
      QuicksandBlock.jumpHeld = entity -> entity == Minecraft.getInstance().player && ((LocalPlayer)entity).input.jumping;
      modBus.addListener(SunaParticleProviders::register);
      AutoTest.addScript(SunaSuna.ID, new SunaAutotest());
      AutoTest.addScript(DevilFruits.id("vfx_sand_blows"), new SandBlowCheck());
      AutoTest.addScript(DevilFruits.id("vfx_suna"), new SunaCapture(false, null));
      AutoTest.addScript(DevilFruits.id("vfx_suna_baseline"), new SunaCapture(true, null));
      AutoTest.addScript(DevilFruits.id("vfx_suna_look"), new SunaCapture(false, true, null));

      for (String code : new String[]{"spb", "spt", "ssp", "scl", "sbj", "sgs", "sgr", "sls", "sld", "sen", "sts", "ssb", "slg"}) {
         AutoTest.addScript(DevilFruits.id("vfx_suna_" + code), new SunaCapture(false, "vfx_" + code + "_"));
         AutoTest.addScript(DevilFruits.id("vfx_suna_look_" + code), new SunaCapture(false, true, "vfx_" + code + "_"));
      }
   }

   private SunaClient() {
   }
}
