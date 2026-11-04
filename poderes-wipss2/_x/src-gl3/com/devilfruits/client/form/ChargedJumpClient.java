package com.devilfruits.client.form;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.ClientFruitState;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.network.JumpInputPayload;
import com.devilfruits.registry.DFAttachments;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(
   modid = "devilfruits",
   value = {Dist.CLIENT}
)
public final class ChargedJumpClient {
   public static final ResourceLocation BAR = DevilFruits.id("charged_jump");
   private static boolean charging;
   private static int ticks;
   private static boolean leaping;
   private static long blockedUntil;

   @Nullable
   private static FruitForm formOf(LocalPlayer player) {
      FruitForm form = player.hasData(DFAttachments.PLAYER_DATA) ? DFAttachments.data(player).form() : null;
      return form != null && form.chargedJumpHeight() > 0.0F ? form : null;
   }

   @SubscribeEvent
   public static void onInput(MovementInputUpdateEvent event) {
      if (event.getEntity() instanceof LocalPlayer player) {
         Minecraft var12 = Minecraft.getInstance();
         long now = player.level().getGameTime();
         if (leaping) {
            if (!player.onGround()) {
               blockedUntil = now + 12L;
            } else if (now >= blockedUntil) {
               leaping = false;
            }
         }

         boolean able = formOf(player) != null && var12.screen == null && !player.isSpectator();
         boolean shift = var12.options.keyShift.isDown();
         boolean jump = var12.options.keyJump.isDown();
         if (!charging) {
            if (!able || !shift || !jump || !player.onGround() || player.isInWater() || player.isPassenger() || player.getAbilities().flying) {
               return;
            }

            if (leaping) {
               event.getInput().jumping = false;
               return;
            }

            charging = true;
            ticks = 0;
            PacketDistributor.sendToServer(new JumpInputPayload(JumpInputPayload.Action.START, 0.0F, 0.0F), new CustomPacketPayload[0]);
            ClientFruitState.startCharge(BAR, now, 30.0F);
         }

         if (able && (player.onGround() || ticks <= 2)) {
            ticks++;
            Input in = event.getInput();
            in.jumping = false;
            in.forwardImpulse = 0.0F;
            in.leftImpulse = 0.0F;
            in.up = in.down = in.left = in.right = false;
            in.shiftKeyDown = true;
            if (!jump) {
               float f = (float)((var12.options.keyUp.isDown() ? 1 : 0) - (var12.options.keyDown.isDown() ? 1 : 0));
               float s = (float)((var12.options.keyLeft.isDown() ? 1 : 0) - (var12.options.keyRight.isDown() ? 1 : 0));
               float yaw = player.getYRot() * (float) (Math.PI / 180.0);
               stop(JumpInputPayload.Action.RELEASE, -Mth.sin(yaw) * f + Mth.cos(yaw) * s, Mth.cos(yaw) * f + Mth.sin(yaw) * s);
               leaping = true;
               blockedUntil = now + 12L;
            }
         } else {
            stop(JumpInputPayload.Action.CANCEL, 0.0F, 0.0F);
         }
      }
   }

   private static void stop(JumpInputPayload.Action action, float dx, float dz) {
      charging = false;
      PacketDistributor.sendToServer(new JumpInputPayload(action, dx, dz), new CustomPacketPayload[0]);
      if (BAR.equals(ClientFruitState.chargingAbility())) {
         ClientFruitState.stopCharge();
      }
   }

   public static boolean charging() {
      return charging;
   }

   @SubscribeEvent
   public static void onLogout(LoggingOut event) {
      charging = false;
      leaping = false;
      blockedUntil = 0L;
   }

   private ChargedJumpClient() {
   }
}
