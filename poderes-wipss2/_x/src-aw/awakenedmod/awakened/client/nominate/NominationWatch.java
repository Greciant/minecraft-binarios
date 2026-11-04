package com.awakenedmod.awakened.client.nominate;

import com.awakenedmod.awakened.client.ClientPowerState;
import com.awakenedmod.awakened.core.ChatFx;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.IEventBus;

public final class NominationWatch {
   private static final int TICKS_PER_MINUTE = 1200;
   private static final int BANNER_TICKS = 100;
   private static final int QUIET_TICKS = 200;
   private static final int CREATIVE_QUIET_TICKS = 400;
   private static final int AFTER_DEATH_TICKS = 1200;
   private static final long LONG_AGO = -1000000L;
   private static final int SETTLE_TICKS = 100;
   private static long clock;
   private static long sessionTicks;
   private static long survivalTicks;
   private static long highPointAt = -1L;
   private static long lastHurt = -1000000L;
   private static long lastAttack = -1000000L;
   private static long lastAbility = -1000000L;
   private static long lastDeath = -1000000L;
   private static boolean inSession;
   private static String masteryPower;
   private static int masteryLevel;
   private static String cooldownPower;
   private static int[] cooldowns = new int[0];
   private static float stamina;

   private NominationWatch() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(e -> tick());
      gameBus.addListener(e -> beginSession());
      gameBus.addListener(e -> endSession());
      gameBus.addListener(e -> {
         if (e.getEntity() == Minecraft.getInstance().player) {
            lastAttack = clock;
         }
      });
   }

   private static void beginSession() {
      NominationLedger.get().beginSession(System.currentTimeMillis());
      inSession = true;
      sessionTicks = 0L;
      survivalTicks = 0L;
      highPointAt = -1L;
      lastDeath = -1000000L;
      lastAbility = -1000000L;
      lastAttack = -1000000L;
      lastHurt = -1000000L;
      masteryPower = null;
      cooldownPower = null;
   }

   private static void endSession() {
      if (inSession) {
         inSession = false;
         NominationLedger ledger = NominationLedger.get();
         ledger.playing(System.currentTimeMillis());
         ledger.save();
      }
   }

   private static void tick() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null && !mc.isPaused()) {
         clock++;
         NominationLedger ledger = NominationLedger.get();
         long now = System.currentTimeMillis();
         if (clock % 1200L == 0L) {
            ledger.playing(now);
            ledger.save();
         }

         NominationCampaign.Mode mode = mode(mc);
         sessionTicks++;
         if (mode == NominationCampaign.Mode.SURVIVAL) {
            survivalTicks++;
         }

         if (player.hurtTime > 0) {
            lastHurt = clock;
         }

         if (player.isDeadOrDying()) {
            lastDeath = clock;
         }

         ClientPowerState state = ClientPowerState.get();
         watchAbilities(state, ledger, now);
         boolean levelUp = watchMastery(state, mode);
         if (NominationCampaign.allowed() && !ledger.stopped()) {
            boolean qualified = NominationCampaign.qualifies(Math.max(state.masteryLevel(), state.highestMastery()), ledger);
            if (levelUp && NominationCampaign.due(ledger, moment(mode, qualified, true)) != null) {
               highPointAt = clock;
            }

            NominationCampaign.Kind kind = NominationCampaign.due(ledger, moment(mode, qualified, highPointAt >= 0L));
            if (kind != null && calm(mc, player, mode)) {
               show(player, kind);
               ledger.shown(kind, mode == NominationCampaign.Mode.CREATIVE, NominationCampaign.today.get());
               highPointAt = -1L;
            }
         }
      }
   }

   private static NominationCampaign.Moment moment(NominationCampaign.Mode mode, boolean qualified, boolean highPoint) {
      return new NominationCampaign.Moment(
         NominationCampaign.today.get(), mode, qualified, highPoint, (int)(survivalTicks / 1200L), (int)(sessionTicks / 1200L)
      );
   }

   private static NominationCampaign.Mode mode(Minecraft mc) {
      GameType type = mc.gameMode != null ? mc.gameMode.getPlayerMode() : GameType.SURVIVAL;

      return switch (type) {
         case CREATIVE -> NominationCampaign.Mode.CREATIVE;
         case SPECTATOR -> NominationCampaign.Mode.SPECTATOR;
         default -> NominationCampaign.Mode.SURVIVAL;
      };
   }

   private static boolean watchMastery(ClientPowerState state, NominationCampaign.Mode mode) {
      String power = state.awakened() ? state.powerId() : null;
      int level = state.masteryLevel();
      boolean up = power != null && power.equals(masteryPower) && level == masteryLevel + 1 && mode == NominationCampaign.Mode.SURVIVAL && sessionTicks > 100L;
      masteryPower = power;
      masteryLevel = level;
      return up;
   }

   private static void watchAbilities(ClientPowerState state, NominationLedger ledger, long now) {
      Power power = state.awakened() ? PowerRegistry.get(state.powerId()) : null;
      if (power == null) {
         cooldownPower = null;
      } else {
         int slots = power.abilities().size();
         if (state.powerId().equals(cooldownPower) && cooldowns.length == slots && sessionTicks > 100L) {
            for (int i = 0; i < slots; i++) {
               int left = state.cooldownLeft(i);
               if (cooldowns[i] == 0 && left > 0) {
                  abilityUsed(ledger, state.powerId(), i, now);
               }

               cooldowns[i] = left;
            }

            float st = state.stamina();
            if (st < stamina - 0.5F && !state.infiniteStamina()) {
               abilityUsed(ledger, state.powerId(), state.activeSlot(), now);
            }

            stamina = st;
         } else {
            cooldownPower = state.powerId();
            cooldowns = new int[slots];

            for (int i = 0; i < slots; i++) {
               cooldowns[i] = state.cooldownLeft(i);
            }

            stamina = state.stamina();
         }
      }
   }

   private static void abilityUsed(NominationLedger ledger, String powerId, int slot, long now) {
      lastAbility = clock;
      if (ledger.abilityUsed(powerId + "/" + slot, now)) {
         ledger.save();
      }
   }

   private static boolean calm(Minecraft mc, LocalPlayer player, NominationCampaign.Mode mode) {
      if (mc.screen == null && !player.isDeadOrDying()) {
         int abilityQuiet = mode == NominationCampaign.Mode.CREATIVE ? 400 : 200;
         return clock - lastDeath >= 1200L
            && clock - lastHurt >= 200L
            && clock - lastAttack >= 200L
            && clock - lastAbility >= (long)abilityQuiet
            && (highPointAt < 0L || clock - highPointAt >= 100L);
      } else {
         return false;
      }
   }

   public static void show(LocalPlayer player, NominationCampaign.Kind kind) {
      MutableComponent line = ChatFx.prefix()
         .append(Component.translatable(kind.key).withStyle(ChatFormatting.GRAY))
         .append(" ")
         .append(ChatFx.button("awakened.nominate.button", ChatFormatting.AQUA, "awakened nominate open", "awakened.nominate.button_tip"))
         .append(" ")
         .append(ChatFx.button("awakened.nominate.stop", ChatFormatting.GRAY, "awakened nominate stop", "awakened.nominate.stop_tip"));
      player.displayClientMessage(line, false);
   }
}
