package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.client.screen.AdminScreen;
import com.awakenedmod.awakened.client.screen.CharacterScreen;
import com.awakenedmod.awakened.client.screen.CommandRulesScreen;
import com.awakenedmod.awakened.client.screen.RadialWheelScreen;
import com.awakenedmod.awakened.client.screen.ReaperAuraSliderScreen;
import com.awakenedmod.awakened.client.screen.SoulMenuScreen;
import com.awakenedmod.awakened.content.kineticcharger.MagnetPull;
import com.awakenedmod.awakened.content.shadow.ShadowSight;
import com.awakenedmod.awakened.content.velocity.VelocityVisualState;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.ExoVisual;
import com.awakenedmod.awakened.core.data.VelocityVisual;
import com.awakenedmod.awakened.core.net.AbilityActionPayload;
import com.awakenedmod.awakened.core.net.AbilityFeedbackPayload;
import com.awakenedmod.awakened.core.net.ActivateAbilityPayload;
import com.awakenedmod.awakened.core.net.AeroFlightArmPayload;
import com.awakenedmod.awakened.core.net.AeroFlightTogglePayload;
import com.awakenedmod.awakened.core.net.ExoBoostPayload;
import com.awakenedmod.awakened.core.net.ExoDeployPayload;
import com.awakenedmod.awakened.core.net.ExoFlightPayload;
import com.awakenedmod.awakened.core.net.ExoVisorPayload;
import com.awakenedmod.awakened.core.net.GeoSeismicTogglePayload;
import com.awakenedmod.awakened.core.net.ReaperToggleAuraPayload;
import com.awakenedmod.awakened.core.net.ReleaseAbilityPayload;
import com.awakenedmod.awakened.core.net.SecondaryKeyPayload;
import com.awakenedmod.awakened.core.net.SetActiveSlotPayload;
import com.awakenedmod.awakened.core.net.VelocityGearPayload;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ClientInputHandler {
   private static boolean activateHeld;
   private static final boolean[] slotHeld = new boolean[AwakenedKeybinds.SLOT_KEYS.length];
   private static final boolean[] slotFiring = new boolean[AwakenedKeybinds.SLOT_KEYS.length];
   private static boolean attackHeld;
   private static boolean reaperKeyHeld;
   private static int reaperHeldTicks;
   private static final int REAPER_HOLD_THRESHOLD = 6;
   private static final int SUMMON_HOLD_THRESHOLD = 6;
   private static final KeyTapHold SUMMON_SOUL_KEY = new KeyTapHold(AwakenedKeybinds.SECONDARY_ABILITY, 6);
   private static final int SECONDARY_HOLD_THRESHOLD = 12;
   private static final KeyTapHold SECONDARY_KEY = new KeyTapHold(AwakenedKeybinds.SECONDARY_ABILITY, 12);
   private static boolean velocityKeyHeld;
   private static int velocityHeldTicks;
   private static boolean velocityHoldFired;
   private static final int VELOCITY_HOLD_THRESHOLD = 10;
   private static final int VELOCITY_REPEAT_TICKS = 6;
   private static int velocityPendingUp;
   private static int velocityPendingTicks;
   private static int velocityChosenSeen = -1;
   private static final int VELOCITY_ACK_TICKS = 10;
   private static final DoubleTap EXO_JUMP = new DoubleTap();
   private static final DoubleTap EXO_SPRINT = new DoubleTap();
   private static final DoubleTap AERO_SPACE = new DoubleTap();

   private ClientInputHandler() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(ClientInputHandler::onClientTick);
   }

   private static void onClientTick(Post event) {
      ClientPowerState state = ClientPowerState.get();
      state.clientTick();
      AwakenedAnimations.logLoadedOnce();
      Minecraft mc = Minecraft.getInstance();
      AbilityKeyInput.tick(mc);
      if (mc.player == null) {
         activateHeld = false;
         attackHeld = false;
         velocityKeyHeld = false;
         velocityHeldTicks = 0;
         velocityHoldFired = false;
         SUMMON_SOUL_KEY.reset();
         SECONDARY_KEY.reset();
         velocityPendingUp = 0;
         velocityPendingTicks = 0;
         velocityChosenSeen = -1;
         AbilityWheelInput.reset();
         AbilityKeyInput.reset();
         Arrays.fill(slotHeld, false);
         Arrays.fill(slotFiring, false);
      } else {
         PossessCamera.clientTick();
         if (PossessCamera.handleInput()) {
            activateHeld = false;
            attackHeld = false;
            drainClicks();
         } else {
            StoneMechPilot.clientTick();
            if (StoneMechPilot.handleInput()) {
               activateHeld = false;
               attackHeld = false;
               drainClicks();
            } else if (ClientFreeze.isFrozen()) {
               drainClicks();
            } else {
               if (AwakenedKeybinds.OPEN_SCREEN.consumeClick() && mc.screen == null) {
                  mc.setScreen(new CharacterScreen());
               }

               if (AwakenedKeybinds.OPEN_ADMIN.consumeClick() && mc.screen == null && isOperator(mc)) {
                  mc.setScreen(new AdminScreen());
               }

               if (state.suppressed()) {
                  sealedPowerKeys(mc, state);
               } else {
                  powerKeys(mc, state);
               }

               if (!state.awakened()) {
                  drainClicks();
                  activateHeld = false;
                  attackHeld = false;
               } else {
                  Power power = PowerRegistry.get(state.powerId());
                  int slotCount = power != null ? power.abilityCount() : 0;
                  if (power != null && power.usesRadialActivate() && (Boolean)ClientConfig.ABILITY_WHEEL.get()) {
                     AbilityWheelInput.handle(mc, power);
                     activateHeld = false;
                  } else {
                     AbilityWheelInput.reset();
                     activateKey(mc, state, power);
                  }

                  boolean attackDown = mc.options.keyAttack.isDown() && mc.screen == null;
                  if (attackDown && !attackHeld && state.thunderfallAiming()) {
                     send(new ActivateAbilityPayload(state.activeSlot()));
                  }

                  attackHeld = attackDown;
                  if (AwakenedKeybinds.COMBAT_MODE.consumeClick()) {
                     boolean on = !(Boolean)ClientConfig.QUICK_KEY_MODE.get();
                     ClientConfig.QUICK_KEY_MODE.set(on);
                     mc.player.displayClientMessage(Component.translatable(on ? "awakened.combat_mode.on" : "awakened.combat_mode.off"), true);
                  }

                  int cycleDir = 0;

                  while (AwakenedKeybinds.CYCLE.consumeClick()) {
                     cycleDir = 1;
                  }

                  while (AwakenedKeybinds.CYCLE_BACK.consumeClick()) {
                     cycleDir = -1;
                  }

                  if (cycleDir != 0 && slotCount > 0) {
                     int next = Math.floorMod(state.activeSlot() + cycleDir, slotCount);
                     send(new SetActiveSlotPayload(next));
                  }

                  slotKeys(mc, power, slotCount);
               }
            }
         }
      }
   }

   private static void powerKeys(Minecraft mc, ClientPowerState state) {
      if ("reaper".equals(state.powerId())) {
         handleReaperAuraKey(mc);
      } else if ("velocity".equals(state.powerId())) {
         handleVelocityGearKey(mc);
      } else if ("summoning".equals(state.powerId())) {
         handleSummonSoulKey(mc);
      } else if (handlesSecondaryKey(state.powerId())) {
         SECONDARY_KEY.tick(mc, () -> sendSecondaryKey(mc, false), () -> secondaryKeyHeld(mc, state.powerId()));
      } else if (AwakenedKeybinds.SECONDARY_ABILITY.consumeClick() && mc.screen == null) {
         if ("command".equals(state.powerId())) {
            mc.setScreen(new CommandRulesScreen());
         } else if ("spider".equals(state.powerId())) {
            mc.setScreen(SpiderWheel.create());
         } else if ("geokinetic".equals(state.powerId())) {
            send(GeoSeismicTogglePayload.INSTANCE);
         } else if ("exosavant".equals(state.powerId())) {
            mc.setScreen(ExoWheel.create());
         } else if ("aerokinetic".equals(state.powerId())) {
            send(AeroFlightArmPayload.INSTANCE);
         } else if ("shadow".equals(state.powerId())) {
            PassiveToggleClient.toggle(ShadowSight.TOGGLE);
         } else if ("kinetic_charger".equals(state.powerId())) {
            PassiveToggleClient.toggle(MagnetPull.TOGGLE);
         }
      }

      if (AwakenedKeybinds.EXO_DEPLOY.consumeClick() && mc.screen == null && "exosavant".equals(state.powerId())) {
         send(ExoDeployPayload.INSTANCE);
      }

      if (AwakenedKeybinds.EXO_VISOR.consumeClick() && mc.screen == null && "exosavant".equals(state.powerId())) {
         send(ExoVisorPayload.INSTANCE);
      }

      if ("exosavant".equals(state.powerId()) && mc.screen == null) {
         if (EXO_JUMP.update(mc.options.keyJump.isDown())) {
            send(ExoFlightPayload.INSTANCE);
         }

         ExoVisual vis = (ExoVisual)mc.player.getData(AwakenedAttachments.EXO_VISUAL);
         boolean sprintDown = mc.options.keySprint.isDown();
         if (vis.flying() && vis.jet()) {
            if (EXO_SPRINT.update(sprintDown, (Boolean)mc.options.toggleSprint().get())) {
               send(ExoBoostPayload.INSTANCE);
            }
         } else {
            EXO_SPRINT.reset(sprintDown);
         }
      } else {
         EXO_JUMP.reset(false);
         EXO_SPRINT.reset(false);
      }

      boolean externalFlightOwnsInput = mc.player.getAbilities().mayfly && !AeroClientState.flying();
      if (!"aerokinetic".equals(state.powerId())
         || mc.screen != null
         || !AeroClientState.armed()
         || state.masteryLevel() < 12
         || externalFlightOwnsInput
         || mc.player.isFallFlying()) {
         AERO_SPACE.reset(false);
      } else if (AERO_SPACE.update(mc.options.keyJump.isDown())) {
         send(AeroFlightTogglePayload.INSTANCE);
      }
   }

   private static void sealedPowerKeys(Minecraft mc, ClientPowerState state) {
      boolean pressed = false;

      while (AwakenedKeybinds.SECONDARY_ABILITY.consumeClick()) {
         pressed = true;
      }

      boolean exo = "exosavant".equals(state.powerId());

      while (AwakenedKeybinds.EXO_DEPLOY.consumeClick()) {
         pressed |= exo;
      }

      while (AwakenedKeybinds.EXO_VISOR.consumeClick()) {
         pressed |= exo;
      }

      if (pressed && mc.screen == null && state.awakened()) {
         ClientPowerState.onFeedback(new AbilityFeedbackPayload(3));
      }

      reaperKeyHeld = false;
      reaperHeldTicks = 0;
      velocityKeyHeld = false;
      velocityHeldTicks = 0;
      velocityHoldFired = false;
      SUMMON_SOUL_KEY.reset();
      SECONDARY_KEY.reset();
      EXO_JUMP.reset(false);
      EXO_SPRINT.reset(false);
      AERO_SPACE.reset(false);
   }

   private static void activateKey(Minecraft mc, ClientPowerState state, Power power) {
      boolean down = AwakenedKeybinds.ACTIVATE.isDown() && mc.screen == null;
      Ability active = power != null ? power.ability(state.activeSlot()) : null;
      boolean clientDriven = active != null && active.clientDriven();
      if (!clientDriven) {
         if (down && !activateHeld) {
            sendPress(mc, active, state.activeSlot());
         } else if (!down && activateHeld) {
            send(new ReleaseAbilityPayload(state.activeSlot()));
         }
      }

      activateHeld = down;

      while (AwakenedKeybinds.ACTIVATE.consumeClick()) {
      }
   }

   private static void slotKeys(Minecraft mc, Power power, int slotCount) {
      boolean quickFire = (Boolean)ClientConfig.QUICK_KEY_MODE.get();

      for (int slot = 0; slot < AwakenedKeybinds.SLOT_KEYS.length; slot++) {
         boolean clicked = false;

         while (AwakenedKeybinds.SLOT_KEYS[slot].consumeClick()) {
            clicked = true;
         }

         boolean slotDown = AwakenedKeybinds.SLOT_KEYS[slot].isDown() && mc.screen == null;
         boolean usable = slot < slotCount;
         Ability slotAbility = usable && power != null ? power.ability(slot) : null;
         boolean slotClientDriven = slotAbility != null && slotAbility.clientDriven();
         if ((slotDown || clicked && mc.screen == null) && !slotHeld[slot] && usable) {
            send(new SetActiveSlotPayload(slot));
            if (quickFire && !slotClientDriven) {
               sendPress(mc, slotAbility, slot);
               slotFiring[slot] = true;
            } else if (quickFire && clicked && !slotDown) {
               AbilityKeyInput.markTap(slot);
            }
         }

         if (!slotDown) {
            releaseSlotIfOwed(slot);
         }

         slotHeld[slot] = slotDown;
      }
   }

   private static void releaseSlotIfOwed(int slot) {
      if (slotFiring[slot]) {
         slotFiring[slot] = false;
         send(new ReleaseAbilityPayload(slot));
      }
   }

   static void sendPress(Minecraft mc, Ability ability, int slot) {
      if (ability != null && ability.usesSneakActivate() && mc.options.keyShift.isDown()) {
         send(new AbilityActionPayload(slot, 3));
      } else {
         send(new ActivateAbilityPayload(slot));
      }
   }

   private static void handleSummonSoulKey(Minecraft mc) {
      SUMMON_SOUL_KEY.tick(mc, () -> openSoulMenu(mc), () -> {
         RadialWheelScreen wheel = SummonWheel.create();
         if (wheel == null) {
            openSoulMenu(mc);
         } else {
            mc.setScreen(wheel);
         }
      });
   }

   private static boolean handlesSecondaryKey(String powerId) {
      Power power = PowerRegistry.get(powerId);
      return power != null && power.handlesSecondaryKey();
   }

   private static void sendSecondaryKey(Minecraft mc, boolean held) {
      send(new SecondaryKeyPayload(SECONDARY_KEY.pressedWithShift(), held));
   }

   private static void secondaryKeyHeld(Minecraft mc, String powerId) {
      Power power = PowerRegistry.get(powerId);
      if (power != null && !power.secondaryKeyWheel().isEmpty()) {
         mc.setScreen(PassiveToggleWheel.create(power));
      } else {
         sendSecondaryKey(mc, true);
      }
   }

   public static float secondaryHoldProgress() {
      return SECONDARY_KEY.heldProgress();
   }

   private static void openSoulMenu(Minecraft mc) {
      mc.setScreen(new SoulMenuScreen());
   }

   private static void handleReaperAuraKey(Minecraft mc) {
      boolean down = AwakenedKeybinds.SECONDARY_ABILITY.isDown() && mc.screen == null;
      if (down) {
         reaperHeldTicks++;
         if (reaperHeldTicks == 6) {
            mc.setScreen(new ReaperAuraSliderScreen());
         }
      } else {
         if (reaperKeyHeld && reaperHeldTicks < 6 && mc.screen == null) {
            send(ReaperToggleAuraPayload.INSTANCE);
         }

         reaperHeldTicks = 0;
      }

      reaperKeyHeld = down;

      while (AwakenedKeybinds.SECONDARY_ABILITY.consumeClick()) {
      }
   }

   private static void handleVelocityGearKey(Minecraft mc) {
      velocityAckTick(mc);
      boolean down = AwakenedKeybinds.SECONDARY_ABILITY.isDown() && mc.screen == null;
      if (down) {
         velocityHeldTicks++;
         int held = velocityHeldTicks - 10;
         if (held == 0 && !velocityHoldFired) {
            velocityHoldFired = true;
            shiftGear(VelocityGearPayload.UP);
         } else if (velocityHoldFired && held > 0 && held % 6 == 0 && velocityGearBelowEarned(mc) && !velocityShiftInFlight()) {
            shiftGear(VelocityGearPayload.UP);
         }
      } else {
         if (velocityKeyHeld && !velocityHoldFired && mc.screen == null) {
            shiftGear(VelocityGearPayload.DOWN);
         }

         velocityHeldTicks = 0;
         velocityHoldFired = false;
      }

      velocityKeyHeld = down;

      while (AwakenedKeybinds.SECONDARY_ABILITY.consumeClick()) {
      }
   }

   private static void shiftGear(VelocityGearPayload intent) {
      send(intent);
      if (intent == VelocityGearPayload.UP) {
         velocityPendingUp++;
         velocityPendingTicks = 0;
      }

      VelocityGearHint.learned();
   }

   private static void velocityAckTick(Minecraft mc) {
      int chosen = mc.player == null ? -1 : VelocityVisualState.of(mc.player).chosen();
      if (chosen != velocityChosenSeen) {
         velocityChosenSeen = chosen;
         velocityPendingUp = 0;
         velocityPendingTicks = 0;
      } else if (velocityPendingUp > 0) {
         velocityPendingTicks++;
      }
   }

   private static boolean velocityShiftInFlight() {
      return velocityPendingUp > 0 && velocityPendingTicks < 10;
   }

   private static boolean velocityGearBelowEarned(Minecraft mc) {
      if (mc.player == null) {
         return false;
      } else {
         VelocityVisual visual = VelocityVisualState.of(mc.player);
         return visual.chosen() < visual.earnedGear();
      }
   }

   static void send(CustomPacketPayload payload) {
      PacketDistributor.sendToServer(payload, new CustomPacketPayload[0]);
   }

   public static boolean isOperator(Minecraft mc) {
      return mc.player != null && mc.player.hasPermissions(2);
   }

   private static void drainClicks() {
      AbilityWheelInput.reset();
      AbilityKeyInput.reset();
      SUMMON_SOUL_KEY.reset();
      SECONDARY_KEY.reset();

      while (AwakenedKeybinds.ACTIVATE.consumeClick()) {
      }

      while (AwakenedKeybinds.CYCLE.consumeClick()) {
      }

      while (AwakenedKeybinds.CYCLE_BACK.consumeClick()) {
      }

      while (AwakenedKeybinds.COMBAT_MODE.consumeClick()) {
      }

      while (AwakenedKeybinds.EXO_DEPLOY.consumeClick()) {
      }

      while (AwakenedKeybinds.EXO_VISOR.consumeClick()) {
      }

      while (AwakenedKeybinds.SECONDARY_ABILITY.consumeClick()) {
      }

      for (int slot = 0; slot < AwakenedKeybinds.SLOT_KEYS.length; slot++) {
         while (AwakenedKeybinds.SLOT_KEYS[slot].consumeClick()) {
         }

         boolean down = AwakenedKeybinds.SLOT_KEYS[slot].isDown();
         if (!down) {
            releaseSlotIfOwed(slot);
         }

         slotHeld[slot] = down;
      }
   }
}
