package com.awakenedmod.awakened.content.command;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public record CommandData(List<RuleSlot> slots, List<String> names) {
   public static final int SLOTS = 5;
   public static final int MAX_ON = 3;
   public static final CommandData EMPTY = new CommandData(List.of(), List.of());
   public static final int NAME_MAX = 16;
   public static final MapCodec<CommandData> MAP_CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
               RuleSlot.MAP_CODEC.codec().listOf().optionalFieldOf("slots", List.of()).forGetter(CommandData::slots),
               Codec.STRING.listOf().optionalFieldOf("names", List.of()).forGetter(CommandData::names)
            )
            .apply(instance, CommandData::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, CommandData> STREAM_CODEC = StreamCodec.of(CommandData::encode, CommandData::decode);

   public CommandData(List<RuleSlot> slots, List<String> names) {
      List<RuleSlot> padded = new ArrayList<>(5);

      for (int i = 0; i < 5; i++) {
         padded.add(i < slots.size() && slots.get(i) != null ? slots.get(i) : RuleSlot.EMPTY);
      }

      slots = List.copyOf(padded);
      List<String> named = new ArrayList<>(5);

      for (int i = 0; i < 5; i++) {
         named.add(i < names.size() ? cleanName(names.get(i)) : "");
      }

      names = List.copyOf(named);
      this.slots = slots;
      this.names = names;
   }

   public static String cleanName(String raw) {
      if (raw == null) {
         return "";
      } else {
         StringBuilder out = new StringBuilder(16);

         for (int i = 0; i < raw.length() && out.length() < 16; i++) {
            char c = raw.charAt(i);
            if (c == 167) {
               i++;
            } else if (!Character.isISOControl(c)) {
               out.append(c);
            }
         }

         return out.toString().strip();
      }
   }

   private static void encode(RegistryFriendlyByteBuf buf, CommandData data) {
      for (int i = 0; i < 5; i++) {
         RuleSlot.STREAM_CODEC.encode(buf, data.slot(i));
      }

      for (int i = 0; i < 5; i++) {
         buf.writeUtf(data.name(i), 64);
      }
   }

   private static CommandData decode(RegistryFriendlyByteBuf buf) {
      List<RuleSlot> read = new ArrayList<>(5);

      for (int i = 0; i < 5; i++) {
         read.add((RuleSlot)RuleSlot.STREAM_CODEC.decode(buf));
      }

      List<String> names = new ArrayList<>(5);

      for (int i = 0; i < 5; i++) {
         names.add(buf.readUtf(64));
      }

      return new CommandData(read, names);
   }

   public RuleSlot slot(int index) {
      return index >= 0 && index < 5 ? this.slots.get(index) : RuleSlot.EMPTY;
   }

   public CommandData withSlot(int index, RuleSlot slot) {
      if (index >= 0 && index < 5) {
         List<RuleSlot> next = new ArrayList<>(this.slots);
         next.set(index, slot);
         return new CommandData(next, this.names);
      } else {
         return this;
      }
   }

   public String name(int index) {
      return index >= 0 && index < 5 ? this.names.get(index) : "";
   }

   public CommandData withName(int index, String name) {
      if (index >= 0 && index < 5) {
         List<String> next = new ArrayList<>(this.names);
         next.set(index, cleanName(name));
         return new CommandData(this.slots, next);
      } else {
         return this;
      }
   }

   public boolean anyOn() {
      for (RuleSlot slot : this.slots) {
         if (slot.on()) {
            return true;
         }
      }

      return false;
   }

   public int onCount() {
      int on = 0;

      for (RuleSlot slot : this.slots) {
         if (slot.on()) {
            on++;
         }
      }

      return on;
   }

   public boolean atOnLimit(int maxOn) {
      return this.onCount() >= maxOn;
   }

   public float lockedTotal() {
      float total = 0.0F;

      for (RuleSlot slot : this.slots) {
         if (slot.on()) {
            total += slot.locked();
         }
      }

      return total;
   }

   public static CommandData of(Player player) {
      if (player != null && player.hasData(CommandAttachments.RULES)) {
         CommandData stored = (CommandData)player.getData(CommandAttachments.RULES);
         return stored == null ? EMPTY : stored;
      } else {
         return EMPTY;
      }
   }

   public static void write(ServerPlayer player, CommandData next) {
      if (!of(player).equals(next)) {
         player.setData(CommandAttachments.RULES, next);
      }
   }

   public static void writeSlot(ServerPlayer player, int index, RuleSlot slot) {
      write(player, of(player).withSlot(index, slot));
   }
}
