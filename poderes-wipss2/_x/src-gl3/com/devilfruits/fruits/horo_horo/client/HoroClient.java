package com.devilfruits.fruits.horo_horo.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.form.FormMovements;
import com.devilfruits.client.fx.ClientFxRegistry;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.fruits.horo_horo.HoroEntities;
import com.devilfruits.fruits.horo_horo.HoroFx;
import com.devilfruits.fruits.horo_horo.HoroHoro;
import com.devilfruits.fruits.horo_horo.client.dev.HoroCapture;
import com.devilfruits.fruits.horo_horo.client.vfx.HoroRender;
import com.devilfruits.fruits.horo_horo.ghost.GhostForm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

public final class HoroClient {
   public static void init(IEventBus modBus) {
      modBus.addListener(e -> e.registerEntityRenderer(HoroEntities.GHOST_BODY.get(), GhostBodyRenderer::new));
      FormMovements.register(HoroHoro.id("form/ghost"), new FormMovements.FormMovement() {
         @Override
         public void tick(LocalPlayer player, FruitForm form, boolean boosting) {
            if (this.held(player)) {
               player.setDeltaMovement(Vec3.ZERO);
            }
         }

         private boolean held(LocalPlayer player) {
            return FormFlags.has(player, HoroHoro.ID, GhostForm.POSSESSING) || FormFlags.has(player, HoroHoro.ID, GhostForm.RECALLING);
         }

         @Override
         public void input(LocalPlayer player, FruitForm form, Input input) {
            if (FormFlags.has(player, HoroHoro.ID, GhostForm.OUT)) {
               player.noPhysics = true;
               player.setOnGround(false);
               player.getAbilities().flying = true;
            }

            if (this.held(player)) {
               input.forwardImpulse = 0.0F;
               input.leftImpulse = 0.0F;
               input.jumping = false;
               input.shiftKeyDown = false;
            }
         }
      });
      ClientFxRegistry.register(HoroFx.QUIET, (level, entity, p) -> {
      });
      HoroClientFx.register();
      NegativeLook.init(modBus);
      GhostLook.init(modBus);
      SwallowedView.init(modBus);
      NeoForge.EVENT_BUS.addListener(HoroRender::onRenderStage);
      NeoForge.EVENT_BUS.addListener(e -> {
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null && !Minecraft.getInstance().isPaused()) {
            NegativeLook.tick(level);
            GhostLook.tick(level);
         }
      });
      NeoForge.EVENT_BUS.addListener(e -> {
         NegativeLook.clear();
         GhostLook.clear();
         SwallowedView.end();
         HoroRender.clear();
      });
      AutoTest.addScript(DevilFruits.id("vfx_horo_m4"), new HoroCapture("vfx_horo4_"));
      AutoTest.addScript(DevilFruits.id("vfx_horo_m5"), new HoroCapture("vfx_horo5_", HoroCapture.Stage.M5));
      AutoTest.addScript(DevilFruits.id("vfx_horo_m6"), new HoroCapture("vfx_horo6_", HoroCapture.Stage.M6));
   }

   private HoroClient() {
   }
}
