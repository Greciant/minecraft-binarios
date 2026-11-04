package com.devilfruits.client.input;

import com.devilfruits.client.ClientFruitState;
import com.devilfruits.client.hud.AbilityWheelScreen;
import com.devilfruits.client.hud.FruitInfoScreen;
import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.core.form.FormAbility;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.logia.LogiaManager;
import com.devilfruits.core.network.AbilityInputPayload;
import com.devilfruits.core.network.FormInputPayload;
import com.devilfruits.core.network.LogiaInputPayload;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.ClientTickEvent.Pre;
import net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(
   modid = "devilfruits",
   value = {Dist.CLIENT}
)
public final class InputHandler {
   @Nullable
   private static ResourceLocation heldUse;
   @Nullable
   private static ResourceLocation heldUltimate;
   private static boolean boosting;
   private static boolean holdingElement;

   public static boolean isBoosting() {
      return boosting;
   }

   @SubscribeEvent
   public static void onClientTick(Pre event) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null) {
         while (DFKeys.INFO.consumeClick()) {
            mc.setScreen(new FruitInfoScreen());
         }

         while (DFKeys.HIDE_HUD.consumeClick()) {
            CoreClientConfig.HIDE_HUD.set(!(Boolean)CoreClientConfig.HIDE_HUD.get());
            CoreClientConfig.SPEC.save();
         }

         while (DFKeys.LOCK_FRUIT.consumeClick()) {
            boolean locked = !(Boolean)CoreClientConfig.FRUIT_LOCKED.get();
            CoreClientConfig.FRUIT_LOCKED.set(locked);
            CoreClientConfig.SPEC.save();
            player.displayClientMessage(
               Component.translatable(locked ? "message.devilfruits.fruit_locked" : "message.devilfruits.fruit_unlocked")
                  .withStyle(locked ? ChatFormatting.GRAY : ChatFormatting.YELLOW),
               true
            );
         }

         if (!ClientFruitState.hasFruit()) {
            releaseAll();
            drain();
         } else {
            if ((Boolean)CoreClientConfig.FRUIT_LOCKED.get()) {
               releaseAll();
               drain();
               drainLogia();
            }

            if (!(Boolean)CoreClientConfig.FRUIT_LOCKED.get()) {
               DirectKeys.tick(mc);
            }

            FruitForm current = ClientFruitState.form();
            setBoosting(DFKeys.BOOST.isDown() && mc.screen == null && current != null && current.boostable());
            if (DFKeys.SELECT_MODIFIER.isDown()) {
               int count = Math.min(ClientFruitState.abilities().size(), mc.options.keyHotbarSlots.length);

               for (int i = 0; i < count; i++) {
                  while (mc.options.keyHotbarSlots[i].consumeClick()) {
                     ClientFruitState.select(i);
                  }
               }
            }

            while (DFKeys.PREVIOUS.consumeClick()) {
               ClientFruitState.cycle(-1);
            }

            while (DFKeys.NEXT.consumeClick()) {
               ClientFruitState.cycle(1);
            }

            while (DFKeys.TOGGLE_BAR.consumeClick()) {
               CoreClientConfig.SHOW_ABILITY_BAR.set(!(Boolean)CoreClientConfig.SHOW_ABILITY_BAR.get());
               CoreClientConfig.SPEC.save();
            }

            while (DFKeys.TRANSFORM.consumeClick()) {
               DevilFruit fruit = ClientFruitState.fruit();
               if (!(Boolean)CoreClientConfig.FRUIT_LOCKED.get() && fruit != null && fruit.hasForms()) {
                  PacketDistributor.sendToServer(new FormInputPayload(FormInputPayload.Action.CYCLE, ""), new CustomPacketPayload[0]);
               }
            }

            while (DFKeys.REVERT_FORM.consumeClick()) {
               FruitForm form = ClientFruitState.form();
               if (form != null && !form.isBase()) {
                  PacketDistributor.sendToServer(new FormInputPayload(FormInputPayload.Action.REVERT, ""), new CustomPacketPayload[0]);
               }
            }

            tickLogia(mc, player);
            if (DFKeys.WHEEL.consumeClick() && mc.screen == null) {
               releaseAll();
               mc.setScreen(new AbilityWheelScreen());
            } else if (mc.screen == null && !(Boolean)CoreClientConfig.FRUIT_LOCKED.get()) {
               FormAbility selected = ClientFruitState.selectedAbility();
               heldUse = track(DFKeys.USE.isDown() && !DirectKeys.shadows(DFKeys.USE), heldUse, selected == null ? null : selected.ability().id());
               heldUltimate = track(DFKeys.ULTIMATE.isDown() && !DirectKeys.shadows(DFKeys.ULTIMATE), heldUltimate, ultimate());
               drain();
            } else {
               releaseAll();
               drain();
            }
         }
      }
   }

   private static void tickLogia(Minecraft mc, LocalPlayer player) {
      if (LogiaManager.isLogia(player) && !(Boolean)CoreClientConfig.FRUIT_LOCKED.get()) {
         if (CoreClientConfig.LOGIA_ELEMENTAL_KEY.get() == CoreClientConfig.KeyMode.HOLD) {
            while (DFKeys.ELEMENTAL_BODY.consumeClick()) {
            }

            setHoldingElement(DFKeys.ELEMENTAL_BODY.isDown() && mc.screen == null);
         } else {
            setHoldingElement(false);

            while (DFKeys.ELEMENTAL_BODY.consumeClick()) {
               PacketDistributor.sendToServer(new LogiaInputPayload(LogiaInputPayload.Action.ELEMENTAL_TOGGLE), new CustomPacketPayload[0]);
            }
         }

         while (DFKeys.ELEMENTAL_DASH.consumeClick()) {
            PacketDistributor.sendToServer(new LogiaInputPayload(LogiaInputPayload.Action.DASH), new CustomPacketPayload[0]);
         }

         while (DFKeys.INTANGIBILITY.consumeClick()) {
            PacketDistributor.sendToServer(new LogiaInputPayload(LogiaInputPayload.Action.INTANGIBILITY_TOGGLE), new CustomPacketPayload[0]);
         }
      } else {
         setHoldingElement(false);

         while (DFKeys.ELEMENTAL_BODY.consumeClick()) {
         }

         while (DFKeys.ELEMENTAL_DASH.consumeClick()) {
         }

         while (DFKeys.INTANGIBILITY.consumeClick()) {
         }
      }
   }

   private static void setHoldingElement(boolean down) {
      if (down != holdingElement) {
         holdingElement = down;
         PacketDistributor.sendToServer(
            new LogiaInputPayload(down ? LogiaInputPayload.Action.ELEMENTAL_ON : LogiaInputPayload.Action.ELEMENTAL_OFF), new CustomPacketPayload[0]
         );
      }
   }

   @Nullable
   private static ResourceLocation ultimate() {
      FruitForm form = ClientFruitState.form();
      Ability ultimate = form == null ? null : form.ultimate();
      return ultimate == null ? null : ultimate.id();
   }

   private static void setBoosting(boolean down) {
      if (down != boosting) {
         boosting = down;
         PacketDistributor.sendToServer(
            new FormInputPayload(down ? FormInputPayload.Action.BOOST_START : FormInputPayload.Action.BOOST_STOP, ""), new CustomPacketPayload[0]
         );
      }
   }

   @Nullable
   private static ResourceLocation track(boolean down, @Nullable ResourceLocation held, @Nullable ResourceLocation ability) {
      if (down && held == null && ability != null) {
         PacketDistributor.sendToServer(AbilityInputPayload.press(ability), new CustomPacketPayload[0]);
         return ability;
      } else if (!down && held != null) {
         PacketDistributor.sendToServer(AbilityInputPayload.release(held), new CustomPacketPayload[0]);
         return null;
      } else {
         return held;
      }
   }

   private static void releaseAll() {
      DirectKeys.releaseAll();
      if (boosting) {
         setBoosting(false);
      }

      if (holdingElement) {
         setHoldingElement(false);
      }

      if (heldUse != null) {
         PacketDistributor.sendToServer(AbilityInputPayload.release(heldUse), new CustomPacketPayload[0]);
         heldUse = null;
      }

      if (heldUltimate != null) {
         PacketDistributor.sendToServer(AbilityInputPayload.release(heldUltimate), new CustomPacketPayload[0]);
         heldUltimate = null;
      }
   }

   private static void drainLogia() {
      while (DFKeys.ELEMENTAL_BODY.consumeClick()) {
      }

      while (DFKeys.ELEMENTAL_DASH.consumeClick()) {
      }

      while (DFKeys.INTANGIBILITY.consumeClick()) {
      }
   }

   private static void drain() {
      while (DFKeys.USE.consumeClick()) {
      }

      while (DFKeys.ULTIMATE.consumeClick()) {
      }

      while (DFKeys.TRANSFORM.consumeClick()) {
      }

      while (DFKeys.REVERT_FORM.consumeClick()) {
      }
   }

   @SubscribeEvent
   public static void onScroll(MouseScrollingEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.screen == null && mc.player != null && ClientFruitState.hasFruit() && DFKeys.SELECT_MODIFIER.isDown() && event.getScrollDeltaY() != 0.0) {
         ClientFruitState.cycle(event.getScrollDeltaY() > 0.0 ? -1 : 1);
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onLogout(LoggingOut event) {
      heldUse = null;
      heldUltimate = null;
      DirectKeys.forget();
      boosting = false;
      holdingElement = false;
      ClientFruitState.reset();
   }

   private InputHandler() {
   }
}
