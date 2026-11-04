package com.awakenedmod.awakened.content.pocket;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.ParsedCommandNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.border.WorldBorder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;

public final class RiftBorder {
   private static final double MAX = 5.999997E7F;
   private static final int ABSOLUTE_MAX = 29999984;
   private static final Map<UUID, ServerPlayer> informed = new HashMap<>();

   private RiftBorder() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(RiftBorder::onServerTick);
      gameBus.addListener(RiftBorder::onServerStopped);
      gameBus.addListener(RiftBorder::onCommand);
   }

   private static void onCommand(CommandEvent event) {
      ParseResults<CommandSourceStack> parse = event.getParseResults();
      List<ParsedCommandNode<CommandSourceStack>> nodes = parse.getContext().getNodes();
      if (nodes.size() >= 2 && "worldborder".equals(nodes.get(0).getNode().getName()) && "add".equals(nodes.get(1).getNode().getName())) {
         CommandSourceStack source = (CommandSourceStack)parse.getContext().getSource();
         if (RiftSpace.DIMENSION.equals(source.getLevel().dimension())) {
            source.sendFailure(Component.translatable("awakened.pocket.worldborder_add"));
            event.setCanceled(true);
         }
      }
   }

   private static void onServerStopped(ServerStoppedEvent event) {
      informed.clear();
   }

   private static void onServerTick(Post event) {
      ServerLevel rift = RiftSpace.riftLevel(event.getServer());
      if (rift == null) {
         informed.clear();
      } else {
         WorldBorder border = rift.getWorldBorder();
         inform(rift, border, enforce(border));
      }
   }

   private static boolean enforce(WorldBorder border) {
      if (border.getSize() == 5.999997E7F
         && border.getLerpTarget() == 5.999997E7F
         && border.getCenterX() == 0.0
         && border.getCenterZ() == 0.0
         && border.getAbsoluteMaxSize() == 29999984) {
         return false;
      } else {
         border.setAbsoluteMaxSize(29999984);
         border.setCenter(0.0, 0.0);
         border.setSize(5.999997E7F);
         return true;
      }
   }

   private static void inform(ServerLevel rift, WorldBorder border, boolean resend) {
      List<ServerPlayer> occupants = rift.players();
      if (occupants.isEmpty()) {
         informed.clear();
      } else {
         informed.keySet().removeIf(id -> rift.getPlayerByUUID(id) == null);

         for (ServerPlayer player : occupants) {
            if (informed.put(player.getUUID(), player) != player || resend) {
               player.connection.send(new ClientboundInitializeBorderPacket(border));
            }
         }
      }
   }
}
