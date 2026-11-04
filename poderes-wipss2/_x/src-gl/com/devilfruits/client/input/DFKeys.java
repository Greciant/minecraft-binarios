package com.devilfruits.client.input;

import com.devilfruits.core.logia.LogiaManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

public final class DFKeys {
   public static final String CATEGORY = "key.categories.devilfruits";
   public static final KeyMapping USE = key("use", 82);
   public static final KeyMapping PREVIOUS = key("previous", 90);
   public static final KeyMapping NEXT = key("next", 88);
   public static final KeyMapping TOGGLE_BAR = key("toggle_bar", 86);
   public static final KeyMapping ULTIMATE = key("ultimate", 71);
   public static final KeyMapping WHEEL = key("wheel", 66);
   public static final KeyMapping INFO = key("info", 75);
   public static final KeyMapping TRANSFORM = key("transform", 72);
   public static final KeyMapping REVERT_FORM = key("revert_form", 74);
   public static final KeyMapping SELECT_MODIFIER = key("select_modifier", 342);
   public static final KeyMapping BOOST = new KeyMapping(
      "key.devilfruits.boost", DFKeys.FormKeyContext.INSTANCE, Type.KEYSYM, 341, "key.categories.devilfruits"
   );
   public static final KeyMapping ELEMENTAL_BODY = new KeyMapping(
      "key.devilfruits.elemental_body", DFKeys.LogiaKeyContext.INSTANCE, Type.KEYSYM, 89, "key.categories.devilfruits"
   );
   public static final KeyMapping INTANGIBILITY = new KeyMapping(
      "key.devilfruits.intangibility", DFKeys.LogiaKeyContext.INSTANCE, Type.KEYSYM, 73, "key.categories.devilfruits"
   );
   public static final KeyMapping ELEMENTAL_DASH = new KeyMapping(
      "key.devilfruits.elemental_dash", DFKeys.LogiaKeyContext.INSTANCE, Type.KEYSYM, 85, "key.categories.devilfruits"
   );
   public static final KeyMapping HIDE_HUD = key("hide_hud", 296);
   public static final KeyMapping LOCK_FRUIT = key("lock_fruit", 79);
   public static final KeyMapping HAKI_COAT = key("haki_coat", 78);
   public static final KeyMapping HAKI_RYUO = key("haki_ryuo", 77);
   public static final KeyMapping SWAP_SHOULDER = key("swap_shoulder", InputConstants.UNKNOWN.getValue());

   private static KeyMapping key(String name, int code) {
      return new KeyMapping("key.devilfruits." + name, KeyConflictContext.IN_GAME, Type.KEYSYM, code, "key.categories.devilfruits");
   }

   public static void register(RegisterKeyMappingsEvent event) {
      event.register(USE);
      event.register(PREVIOUS);
      event.register(NEXT);
      event.register(TOGGLE_BAR);
      event.register(HIDE_HUD);
      event.register(LOCK_FRUIT);
      event.register(ULTIMATE);
      event.register(WHEEL);
      event.register(INFO);
      event.register(TRANSFORM);
      event.register(REVERT_FORM);
      event.register(SELECT_MODIFIER);
      event.register(BOOST);
      event.register(ELEMENTAL_BODY);
      event.register(ELEMENTAL_DASH);
      event.register(INTANGIBILITY);
      event.register(HAKI_COAT);
      event.register(HAKI_RYUO);
      event.register(SWAP_SHOULDER);
      DirectKeys.register(event);
   }

   private DFKeys() {
   }

   private static enum FormKeyContext implements IKeyConflictContext {
      INSTANCE;

      public boolean isActive() {
         return KeyConflictContext.IN_GAME.isActive();
      }

      public boolean conflicts(IKeyConflictContext other) {
         return false;
      }
   }

   private static enum LogiaKeyContext implements IKeyConflictContext {
      INSTANCE;

      public boolean isActive() {
         Minecraft mc = Minecraft.getInstance();
         return KeyConflictContext.IN_GAME.isActive() && mc.player != null && LogiaManager.isLogia(mc.player);
      }

      public boolean conflicts(IKeyConflictContext other) {
         return other == this || other == KeyConflictContext.IN_GAME;
      }
   }
}
