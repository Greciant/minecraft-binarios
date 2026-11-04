package com.awakenedmod.awakened.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public final class AwakenedKeybinds {
   private static final String CATEGORY = "key.categories.awakened";
   public static final KeyMapping ACTIVATE = make("activate", 82);
   public static final KeyMapping CYCLE = make("cycle", 86);
   public static final KeyMapping CYCLE_BACK = make("cycle_back", InputConstants.UNKNOWN.getValue());
   public static final KeyMapping COMBAT_MODE = make("combat_mode", InputConstants.UNKNOWN.getValue());
   public static final KeyMapping OPEN_SCREEN = make("open_screen", 75);
   public static final KeyMapping OPEN_ADMIN = make("open_admin", 92);
   public static final KeyMapping SECONDARY_ABILITY = make("secondary_ability", 71);
   public static final KeyMapping EXO_DEPLOY = make("exo_deploy", 72);
   public static final KeyMapping EXO_VISOR = make("exo_visor", 74);
   public static final KeyMapping SLOT_1 = make("slot_1", InputConstants.UNKNOWN.getValue());
   public static final KeyMapping SLOT_2 = make("slot_2", InputConstants.UNKNOWN.getValue());
   public static final KeyMapping SLOT_3 = make("slot_3", InputConstants.UNKNOWN.getValue());
   public static final KeyMapping SLOT_4 = make("slot_4", InputConstants.UNKNOWN.getValue());
   public static final KeyMapping SLOT_5 = make("slot_5", InputConstants.UNKNOWN.getValue());
   public static final KeyMapping[] SLOT_KEYS = new KeyMapping[]{SLOT_1, SLOT_2, SLOT_3, SLOT_4, SLOT_5};

   private AwakenedKeybinds() {
   }

   public static KeyMapping make(String name, int key) {
      return new KeyMapping("key.awakened." + name, key, "key.categories.awakened");
   }

   public static void register(RegisterKeyMappingsEvent event) {
      event.register(ACTIVATE);
      event.register(CYCLE);
      event.register(CYCLE_BACK);
      event.register(COMBAT_MODE);
      event.register(OPEN_SCREEN);
      event.register(OPEN_ADMIN);
      event.register(SECONDARY_ABILITY);
      event.register(EXO_DEPLOY);
      event.register(EXO_VISOR);
      event.register(SLOT_1);
      event.register(SLOT_2);
      event.register(SLOT_3);
      event.register(SLOT_4);
      event.register(SLOT_5);
   }
}
