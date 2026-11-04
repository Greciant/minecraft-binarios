package com.devilfruits.client.poneglyph;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.poneglyph.PoneglyphItems;
import com.devilfruits.core.poneglyph.Poneglyphs;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

public final class PoneglyphClient {
   public static void init(IEventBus modBus) {
      modBus.addListener(e -> e.register(Poneglyphs.DECIPHER_DESK_MENU.get(), DecipherDeskScreen::new));
      modBus.addListener(
         e -> e.registerSpriteSet(
               (ParticleType)Poneglyphs.GLYPH_PARTICLE.get(),
               sprites -> (type, level, x, y, z, vx, vy, vz) -> new GlyphParticle(level, x, y, z, vx, vy, vz, sprites)
            )
      );
      modBus.addListener(e -> e.registerAbove(VanillaGuiLayers.CROSSHAIR, DevilFruits.id("poneglyph_rub"), RubbingClient::renderHud));
      NeoForge.EVENT_BUS.addListener(e -> RubbingClient.tick());
      NeoForge.EVENT_BUS.addListener(e -> {
         if (e.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS) {
            RubbingClient.renderGlow(e);
         }
      });
      NeoForge.EVENT_BUS.addListener(e -> PoneglyphTooltips.onTooltip(e));
      PoneglyphItems.openPrimerScreen = () -> Minecraft.getInstance().setScreen(new PrimerScreen());
      PoneglyphAutotest.register();
   }

   private PoneglyphClient() {
   }
}
