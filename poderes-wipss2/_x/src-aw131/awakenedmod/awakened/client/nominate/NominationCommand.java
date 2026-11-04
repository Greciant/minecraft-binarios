package com.awakenedmod.awakened.client.nominate;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;

public final class NominationCommand {
   static final String OPEN = "awakened nominate open";
   static final String STOP = "awakened nominate stop";

   private NominationCommand() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(
         e -> e.getDispatcher()
               .register(
                  (LiteralArgumentBuilder)Commands.literal("awakened")
                     .then(((LiteralArgumentBuilder)Commands.literal("nominate").then(Commands.literal("open").executes(c -> {
                        open();
                        return 1;
                     }))).then(Commands.literal("stop").executes(c -> {
                        NominationLedger.get().stop();
                        say(Component.translatable("awakened.nominate.stopped"));
                        return 1;
                     })))
               )
      );
   }

   public static void open() {
      if (!NominationCampaign.live(NominationCampaign.today.get())) {
         say(Component.translatable("awakened.nominate.closed"));
      } else {
         NominationLedger.get().click();
         Minecraft.getInstance().keyboardHandler.setClipboard("https://www.curseforge.com/minecraft/mc-mods/the-awakening");
         NominationCampaign.opener.accept("https://overwolfdevs.typeform.com/to/S3mAnc9s?utm_source=cf_site&utm_medium=cta&utm_campaign=moty2026");
         say(Component.translatable("awakened.nominate.copied"));
      }
   }

   private static void say(Component text) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         player.displayClientMessage(text.copy().withStyle(ChatFormatting.GRAY), false);
      }
   }
}
