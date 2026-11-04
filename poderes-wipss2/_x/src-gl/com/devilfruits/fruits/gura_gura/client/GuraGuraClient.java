package com.devilfruits.fruits.gura_gura.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.AwakeningCapture;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.fruits.gura_gura.GuraEntities;
import com.devilfruits.fruits.gura_gura.GuraFx;
import com.devilfruits.fruits.gura_gura.GuraGura;
import com.devilfruits.fruits.gura_gura.client.dev.GuraAutotest;
import com.devilfruits.fruits.gura_gura.client.dev.GuraCapture;
import com.devilfruits.fruits.gura_gura.client.dev.QuakeDeflectCheck;
import com.devilfruits.fruits.gura_gura.client.dev.ReplayCheck;
import com.devilfruits.fruits.gura_gura.client.particle.GuraParticleProviders;
import com.devilfruits.fruits.gura_gura.client.render.QuakeSlashRenderer;
import com.devilfruits.fruits.gura_gura.client.render.QuakeWaveRenderer;
import com.devilfruits.fruits.gura_gura.entity.QuakeWaveEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;

public final class GuraGuraClient {
   public static void init(IEventBus modBus) {
      GuraClientFx.register();
      QuakeWaveEntity.vanillaParticles = VfxSettings::legacy;
      modBus.addListener(GuraParticleProviders::register);
      modBus.addListener(GuraGuraClient::registerRenderers);
      AutoTest.addScript(GuraGura.ID, new GuraAutotest());
      AutoTest.addScript(DevilFruits.id("vfx_quake_deflect"), new QuakeDeflectCheck());
      AutoTest.addScript(DevilFruits.id("vfx_replay"), new ReplayCheck());
      AutoTest.addScript(DevilFruits.id("vfx_gura"), new GuraCapture(false, null));
      AutoTest.addScript(DevilFruits.id("vfx_gura_baseline"), new GuraCapture(true, null));
      AutoTest.addScript(DevilFruits.id("vfx_gura_waves"), new GuraCapture(false, "vfx_gkg_,vfx_gkw_"));
      AutoTest.addScript(DevilFruits.id("vfx_gura_fix"), new GuraCapture(false, "vfx_gkg_,vfx_gau_,vfx_gsy_,vfx_gkw_"));

      for (boolean look : new boolean[]{false, true}) {
         AutoTest.addScript(
            DevilFruits.id("vfx_awaken_gura" + (look ? "_look" : "")),
            new AwakeningCapture(
               GuraGura.ID,
               look,
               new AwakeningCapture.Take("vfx_awg_", "gura awakening", GuraFx.AWAKEN, 5.0, 140.0F, 6.0F, 64, new int[]{4, 12, 20, 24, 27, 34, 48})
            )
         );
      }

      for (String code : new String[]{"gks", "ggk", "gkb", "gkg", "gau", "gsl", "gsy", "gkw"}) {
         AutoTest.addScript(DevilFruits.id("vfx_gura_" + code), new GuraCapture(false, "vfx_" + code + "_"));
      }
   }

   private static void registerRenderers(RegisterRenderers event) {
      event.registerEntityRenderer(GuraEntities.QUAKE_SLASH.get(), QuakeSlashRenderer::new);
      event.registerEntityRenderer(GuraEntities.QUAKE_WAVE.get(), QuakeWaveRenderer::new);
   }

   private GuraGuraClient() {
   }
}
