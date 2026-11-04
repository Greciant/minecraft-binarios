package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.spider.SpiderTrick;
import com.awakenedmod.awakened.core.net.SpiderTrickPayload;
import java.util.EnumSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

public final class SpiderTrickController {
   private static final EnumSet<SpiderTrick> landed = EnumSet.noneOf(SpiderTrick.class);
   private static boolean running;
   private static SpiderTrick current;
   private static int currentTicks;
   private static int lastHurtTime;
   private static boolean sneakHeld;

   private SpiderTrickController() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(SpiderTrickController::onClientTick);
   }

   static void beginAirRun() {
      LocalPlayer player = Minecraft.getInstance().player;
      reset();
      running = player != null;
      lastHurtTime = player != null ? player.hurtTime : 0;
   }

   static double cashIn() {
      int points = running ? landed.size() : 0;
      reset();
      return SpiderTrick.boost(points);
   }

   private static void reset() {
      running = false;
      landed.clear();
      current = null;
      currentTicks = 0;
   }

   private static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      boolean sneak = player != null && mc.options.keyShift.isDown() && mc.screen == null;
      boolean sneakPressed = mc.options.toggleCrouch().get() ? sneak != sneakHeld : sneak && !sneakHeld;
      sneakHeld = sneak;
      if (running) {
         if (player != null && !endsRun(player)) {
            lastHurtTime = player.hurtTime;
            if (current != null && --currentTicks <= 0) {
               if (landed.add(current)) {
                  player.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 0.9F + 0.25F * (float)landed.size());
               }

               current = null;
            }

            if (sneakPressed && current == null && !SpiderSwingController.zipping() && !SpiderSwingController.striking()) {
               startTrick(mc);
            }
         } else {
            reset();
         }
      }
   }

   private static boolean endsRun(LocalPlayer player) {
      return player.onGround()
         || player.isInWater()
         || player.hurtTime > lastHurtTime
         || !"spider".equals(ClientPowerState.get().livePowerId())
         || SpiderSwingController.gliding();
   }

   static void cancelCurrent() {
      current = null;
      currentTicks = 0;
   }

   private static void startTrick(Minecraft mc) {
      int slot = SpiderSwingController.swingSlot();
      if (slot >= 0) {
         SpiderTrick trick = SpiderTrick.fromInput(
            mc.options.keyUp.isDown(), mc.options.keyDown.isDown(), mc.options.keyLeft.isDown(), mc.options.keyRight.isDown()
         );
         current = trick;
         currentTicks = 9;
         SpiderSwingController.send(new SpiderTrickPayload(slot, trick.ordinal()));
      }
   }
}
