package com.awakenedmod.awakened.core.trade;

import com.awakenedmod.awakened.core.CatalystConfig;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.net.TradeScreenPayload;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import com.awakenedmod.awakened.core.rules.ServerRules;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PowerTrades {
   static final int MAX_DISTANCE = 10;
   private static final int INVITE_TICKS = 1200;
   private static final int OPEN_TICKS = 3600;
   private static final int ARM_TICKS = 60;
   private static final int PROPOSE_ARM_TICKS = 20;
   private static final int SERVER_ARM_TICKS = 40;
   private static final int CHECK_EVERY = 5;
   private static final int INVITE_COOLDOWN_TICKS = 200;
   private static final Map<UUID, PowerTrade> BY_PLAYER = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> LAST_INVITE = new ConcurrentHashMap<>();

   private PowerTrades() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(PowerTrades::onInteract);
      gameBus.addListener(PowerTrades::onServerTick);
      gameBus.addListener(PowerTrades::onLogout);
      gameBus.addListener(PowerTrades::onDeath);
      gameBus.addListener(PowerTrades::onServerStopping);
   }

   private static void onInteract(EntityInteract event) {
      Player self = event.getEntity();
      boolean allowed = self.level().isClientSide() ? TradeRule.clientAllowed() : TradeRule.allowed(self.level().getServer());
      if (event.getHand() == InteractionHand.MAIN_HAND
         && event.getTarget() instanceof Player
         && self.isShiftKeyDown()
         && self.getMainHandItem().isEmpty()
         && allowed) {
         event.setCanceled(true);
         event.setCancellationResult(InteractionResult.SUCCESS);
         if (self instanceof ServerPlayer sender && event.getTarget() instanceof ServerPlayer other) {
            sneakUse(sender, other);
         }
      }
   }

   private static void sneakUse(ServerPlayer self, ServerPlayer other) {
      PowerTrade open = BY_PLAYER.get(self.getUUID());
      if (open != null) {
         if (!open.other(self.getUUID()).equals(other.getUUID())) {
            TradeChat.refuse(self, Component.translatable("awakened.trade.reason.you_busy"));
         } else if (open.stage != PowerTrade.Stage.INVITED) {
            push(self.level().getServer(), open);
         } else if (open.target.equals(self.getUUID())) {
            review(self);
         } else {
            TradeChat.refuse(self, Component.translatable("awakened.trade.waiting", new Object[]{nameOf(other)}));
         }
      } else {
         Component problem = problem(self, other, null);
         if (problem == null && BY_PLAYER.containsKey(other.getUUID())) {
            problem = Component.translatable("awakened.trade.reason.they_busy", new Object[]{nameOf(other)});
         }

         if (problem != null) {
            TradeChat.refuse(self, problem);
         } else {
            PacketDistributor.sendToPlayer(self, screen(self, other, 0, null, 0), new CustomPacketPayload[0]);
         }
      }
   }

   public static void handle(ServerPlayer player, int action, UUID partner) {
      if (player != null) {
         switch (action) {
            case 0:
               send(player, partner);
               break;
            case 1:
               ready(player, PowerTrade.Stage.REVIEW);
               break;
            case 2:
               ready(player, PowerTrade.Stage.FINAL);
               break;
            case 3:
               cancel(player);
         }
      }
   }

   private static void send(ServerPlayer self, UUID partner) {
      MinecraftServer server = self.level().getServer();
      ServerPlayer other = partner != null ? server.getPlayerList().getPlayer(partner) : null;
      if (other != null) {
         Component problem = problem(self, other, null);
         if (problem == null && BY_PLAYER.containsKey(self.getUUID())) {
            problem = Component.translatable("awakened.trade.reason.you_busy");
         }

         if (problem == null && BY_PLAYER.containsKey(other.getUUID())) {
            problem = Component.translatable("awakened.trade.reason.they_busy", new Object[]{nameOf(other)});
         }

         long now = (long)server.getTickCount();
         Long last = LAST_INVITE.get(self.getUUID());
         if (problem == null && last != null && now >= last && now - last < 200L) {
            problem = Component.translatable("awakened.trade.reason.invite_cooldown", new Object[]{(200L - (now - last) + 19L) / 20L});
         }

         if (problem != null) {
            TradeChat.refuse(self, problem);
         } else {
            LAST_INVITE.put(self.getUUID(), now);
            PowerTrade trade = new PowerTrade(
               self.getUUID(), other.getUUID(), nameOf(self), nameOf(other), PowerService.data(self).powerId(), PowerService.data(other).powerId()
            );
            trade.advance(PowerTrade.Stage.INVITED, (long)server.getTickCount(), 1200L);
            BY_PLAYER.put(self.getUUID(), trade);
            BY_PLAYER.put(other.getUUID(), trade);
            TradeChat.sendInvite(self, other, power(trade.initiatorPower), power(trade.targetPower), 60);
         }
      }
   }

   public static void review(ServerPlayer self) {
      PowerTrade trade = BY_PLAYER.get(self.getUUID());
      if (trade != null && trade.stage == PowerTrade.Stage.INVITED && trade.target.equals(self.getUUID())) {
         MinecraftServer server = self.level().getServer();
         ServerPlayer other = server.getPlayerList().getPlayer(trade.initiator);
         if (other != null) {
            Component problem = problem(self, other, trade);
            if (problem != null) {
               TradeChat.refuse(self, problem);
            } else {
               trade.advance(PowerTrade.Stage.REVIEW, (long)server.getTickCount(), 3600L);
               push(server, trade);
            }
         }
      } else {
         TradeChat.tell(self, "awakened.trade.none");
      }
   }

   public static void decline(ServerPlayer self) {
      PowerTrade trade = BY_PLAYER.get(self.getUUID());
      if (trade != null && trade.stage == PowerTrade.Stage.INVITED && trade.target.equals(self.getUUID())) {
         end(
            self.level().getServer(),
            trade,
            p -> p == self
                  ? Component.translatable("awakened.trade.reason.you_declined")
                  : Component.translatable("awakened.trade.reason.they_declined", new Object[]{nameOf(self)})
         );
      } else {
         TradeChat.tell(self, "awakened.trade.none");
      }
   }

   public static void cancelByCommand(ServerPlayer self) {
      if (!BY_PLAYER.containsKey(self.getUUID())) {
         TradeChat.tell(self, "awakened.trade.none_open");
      } else {
         cancel(self);
      }
   }

   private static void cancel(ServerPlayer self) {
      PowerTrade trade = BY_PLAYER.get(self.getUUID());
      if (trade != null) {
         end(
            self.level().getServer(),
            trade,
            p -> p == self
                  ? Component.translatable("awakened.trade.reason.you_cancelled")
                  : Component.translatable("awakened.trade.reason.they_cancelled", new Object[]{nameOf(self)})
         );
      }
   }

   private static void ready(ServerPlayer self, PowerTrade.Stage stage) {
      PowerTrade trade = BY_PLAYER.get(self.getUUID());
      if (trade != null && trade.stage == stage) {
         MinecraftServer server = self.level().getServer();
         long now = (long)server.getTickCount();
         if (now - trade.stageStart < 40L) {
            push(server, trade);
         } else {
            ServerPlayer other = server.getPlayerList().getPlayer(trade.other(self.getUUID()));
            if (other != null) {
               if (problem(self, other, trade) != null) {
                  endOnProblem(server, trade, self, other);
               } else {
                  trade.setReady(self.getUUID());
                  if (!trade.bothReady()) {
                     push(server, trade);
                  } else if (stage == PowerTrade.Stage.REVIEW) {
                     trade.advance(PowerTrade.Stage.FINAL, now, 3600L);
                     push(server, trade);
                  } else {
                     complete(trade, self, other);
                  }
               }
            }
         }
      }
   }

   private static void complete(PowerTrade trade, ServerPlayer self, ServerPlayer other) {
      forget(trade);
      PowerService.tradePowers(self, other);

      for (ServerPlayer party : new ServerPlayer[]{self, other}) {
         PacketDistributor.sendToPlayer(party, TradeScreenPayload.closed(), new CustomPacketPayload[0]);
         Power now = PowerRegistry.get(PowerService.data(party).powerId());
         TradeChat.tell(party, "awakened.trade.done", nameOf(party == self ? other : self), now != null ? now.displayName() : Component.empty());
      }
   }

   private static Component problem(ServerPlayer self, ServerPlayer other, PowerTrade trade) {
      if (!TradeRule.allowed(self.level().getServer())) {
         return Component.translatable("awakened.trade.reason.disabled");
      } else {
         String otherName = nameOf(other);
         if (self.getUUID().equals(other.getUUID())) {
            return Component.translatable("awakened.trade.reason.self");
         } else if (self.isSpectator() || !self.isAlive()) {
            return Component.translatable("awakened.trade.reason.you_unavailable");
         } else if (!other.isSpectator() && other.isAlive()) {
            PowerData mine = PowerService.data(self);
            PowerData theirs = PowerService.data(other);
            if (!mine.hasPower()) {
               return Component.translatable("awakened.trade.reason.you_no_power");
            } else if (!theirs.hasPower()) {
               return Component.translatable("awakened.trade.reason.they_no_power", new Object[]{otherName});
            } else if (!ServerRules.powerDisabled(mine.powerId()) && !ServerRules.powerDisabled(theirs.powerId())) {
               if (trade == null || mine.powerId().equals(trade.powerOf(self.getUUID())) && theirs.powerId().equals(trade.powerOf(other.getUUID()))) {
                  if (mine.powerId().equals(theirs.powerId())) {
                     return Component.translatable("awakened.trade.reason.same_power");
                  } else if (PowerService.powerChangePending(self)) {
                     return Component.translatable("awakened.trade.reason.you_changing");
                  } else if (PowerService.powerChangePending(other)) {
                     return Component.translatable("awakened.trade.reason.they_changing", new Object[]{otherName});
                  } else {
                     return self.level() == other.level() && !(self.distanceToSqr(other) > 100.0)
                        ? null
                        : Component.translatable("awakened.trade.reason.too_far", new Object[]{10});
                  }
               } else {
                  return Component.translatable("awakened.trade.reason.power_changed");
               }
            } else {
               return Component.translatable("awakened.trade.reason.power_disabled");
            }
         } else {
            return Component.translatable("awakened.trade.reason.they_unavailable", new Object[]{otherName});
         }
      }
   }

   private static void endOnProblem(MinecraftServer server, PowerTrade trade, ServerPlayer first, ServerPlayer second) {
      end(server, trade, p -> {
         Component why = problem(p, p == first ? second : first, trade);
         return (Component)(why != null ? why : Component.translatable("awakened.trade.reason.power_changed"));
      });
   }

   private static void onServerTick(Post event) {
      MinecraftServer server = event.getServer();
      if (!BY_PLAYER.isEmpty() && server.getTickCount() % 5 == 0) {
         long now = (long)server.getTickCount();

         for (PowerTrade trade : new HashSet<>(BY_PLAYER.values())) {
            if (!TradeRule.allowed(server)) {
               end(server, trade, p -> Component.translatable("awakened.trade.reason.disabled"));
            } else {
               ServerPlayer initiator = server.getPlayerList().getPlayer(trade.initiator);
               ServerPlayer target = server.getPlayerList().getPlayer(trade.target);
               if (initiator == null || target == null) {
                  forget(trade);
               } else if (now >= trade.expires) {
                  end(
                     server,
                     trade,
                     p -> Component.translatable(
                           trade.stage == PowerTrade.Stage.INVITED ? "awakened.trade.reason.invite_expired" : "awakened.trade.reason.expired"
                        )
                  );
               } else if (trade.stage != PowerTrade.Stage.INVITED && problem(initiator, target, trade) != null) {
                  endOnProblem(server, trade, initiator, target);
               } else if (trade.stage == PowerTrade.Stage.INVITED && !holdsTradedPowers(initiator, target, trade)) {
                  end(server, trade, p -> Component.translatable("awakened.trade.reason.power_changed"));
               }
            }
         }
      }
   }

   private static boolean holdsTradedPowers(ServerPlayer initiator, ServerPlayer target, PowerTrade trade) {
      return PowerService.data(initiator).powerId().equals(trade.initiatorPower) && PowerService.data(target).powerId().equals(trade.targetPower);
   }

   private static void onDeath(LivingDeathEvent event) {
      if (event.getEntity() instanceof ServerPlayer dead) {
         PowerTrade trade = BY_PLAYER.get(dead.getUUID());
         if (trade != null) {
            end(
               dead.level().getServer(),
               trade,
               p -> p == dead
                     ? Component.translatable("awakened.trade.reason.you_unavailable")
                     : Component.translatable("awakened.trade.reason.they_unavailable", new Object[]{nameOf(dead)})
            );
         }
      }
   }

   private static void onLogout(PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer leaving) {
         LAST_INVITE.remove(leaving.getUUID());
         PowerTrade trade = BY_PLAYER.get(leaving.getUUID());
         if (trade != null) {
            end(
               leaving.level().getServer(),
               trade,
               p -> p == leaving ? null : Component.translatable("awakened.trade.reason.they_left", new Object[]{nameOf(leaving)})
            );
         }
      }
   }

   private static void onServerStopping(ServerStoppingEvent event) {
      BY_PLAYER.clear();
      LAST_INVITE.clear();
   }

   private static void end(MinecraftServer server, PowerTrade trade, Function<ServerPlayer, Component> reason) {
      forget(trade);

      for (UUID id : new UUID[]{trade.initiator, trade.target}) {
         ServerPlayer party = server.getPlayerList().getPlayer(id);
         if (party != null) {
            PacketDistributor.sendToPlayer(party, TradeScreenPayload.closed(), new CustomPacketPayload[0]);
            Component why = reason.apply(party);
            if (why != null) {
               TradeChat.tell(party, "awakened.trade.cancelled", trade.nameOf(trade.other(id)), why);
            }
         }
      }
   }

   private static void forget(PowerTrade trade) {
      BY_PLAYER.remove(trade.initiator, trade);
      BY_PLAYER.remove(trade.target, trade);
   }

   private static void push(MinecraftServer server, PowerTrade trade) {
      int stage = trade.stage == PowerTrade.Stage.FINAL ? 2 : 1;
      int arm = (int)Math.max(0L, 60L - ((long)server.getTickCount() - trade.stageStart));
      ServerPlayer initiator = server.getPlayerList().getPlayer(trade.initiator);
      ServerPlayer target = server.getPlayerList().getPlayer(trade.target);
      if (initiator != null && target != null) {
         for (ServerPlayer party : new ServerPlayer[]{initiator, target}) {
            if (party.containerMenu != party.inventoryMenu) {
               party.closeContainer();
            }
         }

         PacketDistributor.sendToPlayer(initiator, screen(initiator, target, stage, trade, arm), new CustomPacketPayload[0]);
         PacketDistributor.sendToPlayer(target, screen(target, initiator, stage, trade, arm), new CustomPacketPayload[0]);
      }
   }

   private static TradeScreenPayload screen(ServerPlayer self, ServerPlayer other, int stage, PowerTrade trade, int armTicks) {
      PowerData mine = PowerService.data(self);
      String give = mine.powerId();
      String get = PowerService.data(other).powerId();
      int getMastery = CatalystConfig.bankProgress() ? mine.masteryLevelOf(get) : 0;
      boolean youReady = trade != null && trade.ready(self.getUUID());
      boolean partnerReady = trade != null && trade.ready(other.getUUID());
      return new TradeScreenPayload(
         stage, other.getUUID(), nameOf(other), give, get, mine.masteryLevel(), getMastery, youReady, partnerReady, stage == 0 ? 20 : armTicks
      );
   }

   private static Component power(String id) {
      Power power = PowerRegistry.get(id);
      return (Component)(power != null ? power.displayName() : Component.literal(id));
   }

   private static String nameOf(ServerPlayer player) {
      return player.getName().getString();
   }
}
