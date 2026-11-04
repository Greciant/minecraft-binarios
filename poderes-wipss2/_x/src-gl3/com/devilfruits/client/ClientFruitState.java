package com.devilfruits.client;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormAbility;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.network.AbilityInputPayload;
import com.devilfruits.core.network.AbilityTablePayload;
import com.devilfruits.registry.DFAttachments;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ClientFruitState {
   private static final PlayerFruitData EMPTY = new PlayerFruitData();
   private static final List<Integer> DEFAULT_CURVE = List.of(100, 180, 300, 450, 650, 900, 1200, 1600, 2100);
   @Nullable
   private static AbilityTablePayload table;
   private static final Map<ResourceLocation, AbilityTablePayload.Entry> ENTRIES = new HashMap<>();
   private static final Map<String, AbilityTablePayload.FormEntry> FORMS = new HashMap<>();
   private static int selected;
   private static boolean selectionInitialised;
   private static long chargeStart = -1L;
   @Nullable
   private static ResourceLocation chargeAbility;
   private static float chargeDuration = 40.0F;

   public static PlayerFruitData data() {
      LocalPlayer player = Minecraft.getInstance().player;
      return player == null ? EMPTY : (PlayerFruitData)player.getData(DFAttachments.PLAYER_DATA);
   }

   public static boolean hasFruit() {
      return data().fruit() != null;
   }

   @Nullable
   public static DevilFruit fruit() {
      return data().fruit();
   }

   @Nullable
   public static FruitForm form() {
      return data().form();
   }

   public static List<FormAbility> abilities() {
      FruitForm form = form();
      return form == null ? List.of() : form.abilities();
   }

   @Nullable
   public static FormAbility ability(int slot) {
      List<FormAbility> list = abilities();
      return slot >= 0 && slot < list.size() ? list.get(slot) : null;
   }

   public static void setTable(AbilityTablePayload payload) {
      table = payload;
      ENTRIES.clear();
      FORMS.clear();

      for (AbilityTablePayload.Entry e : payload.entries()) {
         ENTRIES.put(e.ability(), e);
      }

      for (AbilityTablePayload.FormEntry f : payload.forms()) {
         FORMS.put(f.form(), f);
      }
   }

   @Nullable
   private static AbilityTablePayload.Entry entry(Ability ability) {
      return ENTRIES.get(ability.id());
   }

   public static int energyCost(FormAbility entry) {
      if (data().form() != null && data().form().config().freeAbilities()) {
         return 0;
      } else {
         AbilityTablePayload.Entry e = entry(entry.ability());
         int base = e != null ? e.energyCost() : entry.ability().defaults().energyCost();
         return Math.round((float)base * entry.modifiers().energyCost());
      }
   }

   public static int requiredMastery(Ability ability) {
      AbilityTablePayload.Entry e = entry(ability);
      return e != null ? e.requiredMastery() : ability.defaults().requiredMastery();
   }

   public static boolean enabled(Ability ability) {
      AbilityTablePayload.Entry e = entry(ability);
      return e == null || e.enabled();
   }

   public static int cooldownTicks(FormAbility entry) {
      AbilityTablePayload.Entry e = entry(entry.ability());
      int base = e != null ? e.cooldownTicks() : entry.ability().defaults().cooldownTicks();
      return Math.round((float)base * entry.modifiers().cooldown());
   }

   public static boolean isLocked(Ability ability) {
      return data().mastery() < requiredMastery(ability);
   }

   @Nullable
   public static AbilityTablePayload.FormEntry formEntry(FruitForm form) {
      return FORMS.get(form.name());
   }

   public static int maxEnergy() {
      int mastery = data().mastery();
      DevilFruit fruit = fruit();
      if (table != null && fruit != null && table.fruit().<Boolean>map(fruit.id()::equals).orElse(false)) {
         return table.baseEnergy() + table.energyPerLevel() * (mastery - 1);
      } else {
         return fruit != null ? fruit.energyDefaults().baseEnergy() + fruit.energyDefaults().energyPerLevel() * (mastery - 1) : 100 + 10 * (mastery - 1);
      }
   }

   public static int xpForNextLevel() {
      int mastery = data().mastery();
      if (mastery >= 10) {
         return 0;
      } else {
         List<Integer> curve = table == null ? DEFAULT_CURVE : table.xpCurve();
         return curve.isEmpty() ? 100 : curve.get(Math.min(curve.size() - 1, mastery - 1));
      }
   }

   public static int selected() {
      int size = abilities().size();
      if (!selectionInitialised && hasFruit()) {
         selected = data().selected();
         selectionInitialised = true;
      }

      return size == 0 ? 0 : Mth.clamp(selected, 0, size - 1);
   }

   @Nullable
   public static FormAbility selectedAbility() {
      return ability(selected());
   }

   public static void select(int index) {
      int size = abilities().size();
      if (size != 0) {
         selected = Math.floorMod(index, size);
         selectionInitialised = true;
         FormAbility entry = ability(selected);
         if (entry != null) {
            PacketDistributor.sendToServer(AbilityInputPayload.select(entry.ability().id(), selected), new CustomPacketPayload[0]);
         }
      }
   }

   public static void cycle(int delta) {
      select(selected() + delta);
   }

   public static void startCharge(ResourceLocation ability, long gameTime, float duration) {
      chargeStart = gameTime;
      chargeAbility = ability;
      chargeDuration = Math.max(1.0F, duration);
   }

   @Nullable
   public static ResourceLocation chargingAbility() {
      return chargeStart < 0L ? null : chargeAbility;
   }

   public static void stopCharge() {
      chargeStart = -1L;
      chargeAbility = null;
   }

   public static float chargeProgress(float partialTick) {
      if (chargeStart >= 0L && Minecraft.getInstance().level != null) {
         float elapsed = (float)(Minecraft.getInstance().level.getGameTime() - chargeStart) + partialTick;
         return Math.min(1.0F, elapsed / chargeDuration);
      } else {
         return -1.0F;
      }
   }

   public static void reset() {
      table = null;
      ENTRIES.clear();
      FORMS.clear();
      selectionInitialised = false;
      selected = 0;
      stopCharge();
   }

   private ClientFruitState() {
   }
}
