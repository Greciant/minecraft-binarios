package com.devilfruits.core.poneglyph;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public final class PoneglyphApiEvents {
   private PoneglyphApiEvents() {
   }

   public static final class BlueprintDecipheredEvent extends Event {
      private final ServerPlayer player;
      private final ResourceLocation poneglyph;
      private final ResourceLocation knowledge;

      public BlueprintDecipheredEvent(ServerPlayer player, ResourceLocation poneglyph, ResourceLocation knowledge) {
         this.player = player;
         this.poneglyph = poneglyph;
         this.knowledge = knowledge;
      }

      public ServerPlayer getPlayer() {
         return this.player;
      }

      public ResourceLocation getPoneglyph() {
         return this.poneglyph;
      }

      public ResourceLocation getKnowledge() {
         return this.knowledge;
      }
   }

   public static final class KnowledgeLearnedEvent extends Event {
      private final ServerPlayer player;
      private final ResourceLocation knowledge;

      public KnowledgeLearnedEvent(ServerPlayer player, ResourceLocation knowledge) {
         this.player = player;
         this.knowledge = knowledge;
      }

      public ServerPlayer getPlayer() {
         return this.player;
      }

      public ResourceLocation getKnowledge() {
         return this.knowledge;
      }
   }

   public static final class PoneglyphRubbedEvent extends Event {
      private final ServerPlayer player;
      private final BlockPos pos;
      private final ResourceLocation poneglyph;
      private final int cell;

      public PoneglyphRubbedEvent(ServerPlayer player, BlockPos pos, ResourceLocation poneglyph, int cell) {
         this.player = player;
         this.pos = pos;
         this.poneglyph = poneglyph;
         this.cell = cell;
      }

      public ServerPlayer getPlayer() {
         return this.player;
      }

      public BlockPos getPos() {
         return this.pos;
      }

      public ResourceLocation getPoneglyph() {
         return this.poneglyph;
      }

      public int getCell() {
         return this.cell;
      }
   }
}
