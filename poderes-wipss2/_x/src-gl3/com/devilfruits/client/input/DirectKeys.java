package com.devilfruits.client.input;

import com.devilfruits.client.ClientFruitState;
import com.devilfruits.core.form.FormAbility;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.core.network.AbilityInputPayload;
import com.mojang.blaze3d.platform.InputConstants.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;

public final class DirectKeys {
   public static final int SLOTS = 9;
   private static final int[] DEFAULTS = new int[]{86, 67, 88, 90, 66, 84, 82, 69, 70};
   public static final KeyMapping[] KEYS = new KeyMapping[9];
   private static final Map<ResourceLocation, BooleanSupplier> ENABLED = new HashMap<>();
   private static final ResourceLocation[] HELD = new ResourceLocation[9];

   public static void enable(ResourceLocation formId, BooleanSupplier on) {
      ENABLED.put(formId, on);
   }

   public static boolean active() {
      FruitForm form = ClientFruitState.form();
      BooleanSupplier on = form == null ? null : ENABLED.get(form.id());
      return on != null && on.getAsBoolean();
   }

   public static boolean shadows(KeyMapping other) {
      if (!active()) {
         return false;
      } else {
         for (KeyMapping key : KEYS) {
            if (key != other && !key.isUnbound() && key.getKey().equals(other.getKey())) {
               return true;
            }
         }

         return false;
      }
   }

   static void register(RegisterKeyMappingsEvent event) {
      for (KeyMapping key : KEYS) {
         event.register(key);
      }
   }

   static void tick(Minecraft mc) {
      if (active() && mc.screen == null) {
         for (KeyMapping other : mc.options.keyMappings) {
            if (shadows(other)) {
               while (other.consumeClick()) {
               }
            }
         }

         List<FormAbility> abilities = ClientFruitState.abilities();

         for (int i = 0; i < 9; i++) {
            while (KEYS[i].consumeClick()) {
            }

            ResourceLocation slot = i < abilities.size() ? abilities.get(i).ability().id() : null;
            boolean down = KEYS[i].isDown();
            if (down && HELD[i] == null && slot != null) {
               PacketDistributor.sendToServer(AbilityInputPayload.press(slot), new CustomPacketPayload[0]);
               HELD[i] = slot;
            } else if (!down && HELD[i] != null) {
               PacketDistributor.sendToServer(AbilityInputPayload.release(HELD[i]), new CustomPacketPayload[0]);
               HELD[i] = null;
            }
         }
      } else {
         releaseAll();

         for (KeyMapping key : KEYS) {
            while (key.consumeClick()) {
            }
         }
      }
   }

   static void releaseAll() {
      for (int i = 0; i < 9; i++) {
         release(i);
      }
   }

   private static void release(int i) {
      ResourceLocation held = HELD[i];
      if (held != null) {
         if (Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(AbilityInputPayload.release(held), new CustomPacketPayload[0]);
         }

         HELD[i] = null;
      }
   }

   static void forget() {
      Arrays.fill(HELD, null);
   }

   private DirectKeys() {
   }

   static {
      for (int i = 0; i < 9; i++) {
         KEYS[i] = new KeyMapping("key.devilfruits.direct_" + (i + 1), DirectKeys.Context.INSTANCE, Type.KEYSYM, DEFAULTS[i], "key.categories.devilfruits");
      }
   }

   private static enum Context implements IKeyConflictContext {
      INSTANCE;

      public boolean isActive() {
         return KeyConflictContext.IN_GAME.isActive() && DirectKeys.active();
      }

      public boolean conflicts(IKeyConflictContext other) {
         return false;
      }
   }
}
