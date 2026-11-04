package com.devilfruits.core.network;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.config.AbilityConfig;
import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.config.FormConfig;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.fruit.DevilFruit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record AbilityTablePayload(
   Optional<ResourceLocation> fruit,
   List<AbilityTablePayload.Entry> entries,
   List<AbilityTablePayload.FormEntry> forms,
   int baseEnergy,
   int energyPerLevel,
   List<Integer> xpCurve
) implements CustomPacketPayload {
   public static final Type<AbilityTablePayload> TYPE = new Type(DevilFruits.id("ability_table"));
   public static final StreamCodec<FriendlyByteBuf, AbilityTablePayload> STREAM_CODEC = CustomPacketPayload.codec((p, buf) -> {
      buf.writeOptional(p.fruit, FriendlyByteBuf::writeResourceLocation);
      buf.writeVarInt(p.entries.size());

      for (AbilityTablePayload.Entry e : p.entries) {
         buf.writeResourceLocation(e.ability);
         buf.writeBoolean(e.enabled);
         buf.writeVarInt(e.energyCost);
         buf.writeVarInt(e.cooldownTicks);
         buf.writeVarInt(e.requiredMastery);
      }

      buf.writeVarInt(p.forms.size());

      for (AbilityTablePayload.FormEntry f : p.forms) {
         buf.writeUtf(f.form, 64);
         buf.writeVarInt(f.requiredMastery);
         buf.writeVarInt(f.enterEnergyCost);
         buf.writeFloat(f.drainPerSecond);
      }

      buf.writeVarInt(p.baseEnergy);
      buf.writeVarInt(p.energyPerLevel);
      buf.writeVarInt(p.xpCurve.size());
      p.xpCurve.forEach(buf::writeVarInt);
   }, buf -> {
      Optional<ResourceLocation> fruit = buf.readOptional(FriendlyByteBuf::readResourceLocation);
      int n = buf.readVarInt();
      List<AbilityTablePayload.Entry> entries = new ArrayList<>(n);

      for (int i = 0; i < n; i++) {
         entries.add(new AbilityTablePayload.Entry(buf.readResourceLocation(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
      }

      int f = buf.readVarInt();
      List<AbilityTablePayload.FormEntry> forms = new ArrayList<>(f);

      for (int i = 0; i < f; i++) {
         forms.add(new AbilityTablePayload.FormEntry(buf.readUtf(64), buf.readVarInt(), buf.readVarInt(), buf.readFloat()));
      }

      int base = buf.readVarInt();
      int perLevel = buf.readVarInt();
      int curveSize = buf.readVarInt();
      List<Integer> curve = new ArrayList<>(curveSize);

      for (int i = 0; i < curveSize; i++) {
         curve.add(buf.readVarInt());
      }

      return new AbilityTablePayload(fruit, entries, forms, base, perLevel, curve);
   });

   public static AbilityTablePayload of(DevilFruit fruit) {
      List<Integer> curve = new ArrayList<>();

      for (int level = 1; level < 10; level++) {
         curve.add(CoreConfig.xpForLevel(level));
      }

      if (fruit == null) {
         return new AbilityTablePayload(Optional.empty(), List.of(), List.of(), 100, 10, curve);
      } else {
         List<AbilityTablePayload.Entry> entries = new ArrayList<>();

         for (Ability ability : fruit.abilities()) {
            AbilityConfig c = ability.config();
            entries.add(new AbilityTablePayload.Entry(ability.id(), c.enabled(), c.energyCost(), c.cooldownTicks(), c.requiredMastery()));
         }

         List<AbilityTablePayload.FormEntry> forms = new ArrayList<>();

         for (FruitForm form : fruit.forms()) {
            FormConfig c = form.config();
            forms.add(new AbilityTablePayload.FormEntry(form.name(), c.requiredMastery(), c.enterEnergyCost(), (float)c.energyDrainPerSecond()));
         }

         return new AbilityTablePayload(Optional.of(fruit.id()), entries, forms, fruit.config().baseEnergy(), fruit.config().energyPerLevel(), curve);
      }
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static record Entry(ResourceLocation ability, boolean enabled, int energyCost, int cooldownTicks, int requiredMastery) {
   }

   public static record FormEntry(String form, int requiredMastery, int enterEnergyCost, float drainPerSecond) {
   }
}
