package com.devilfruits.fruits.doru_doru.client.atelier;

import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;

public final class SculptKeys {
   public static final String CATEGORY = "key.categories.devilfruits.sculpt";
   static final IKeyConflictContext CONTEXT = new IKeyConflictContext() {
      public boolean isActive() {
         return Minecraft.getInstance().screen == null && SculptClient.active();
      }

      public boolean conflicts(IKeyConflictContext other) {
         return other == this;
      }
   };
   public static final KeyMapping WHEEL = key("wheel", 67);
   public static final KeyMapping PARAM = key("param", 96);
   public static final KeyMapping MIRROR = key("mirror", 89);
   public static final KeyMapping HOLLOW = key("hollow", 85);
   public static final KeyMapping VISCOSITY = key("viscosity", 73);
   public static final KeyMapping UNDO = new KeyMapping(
      "key.devilfruits.sculpt.undo", CONTEXT, KeyModifier.CONTROL, Type.KEYSYM, 90, "key.categories.devilfruits.sculpt"
   );
   public static final KeyMapping REDO = new KeyMapping(
      "key.devilfruits.sculpt.redo", CONTEXT, KeyModifier.CONTROL, Type.KEYSYM, 89, "key.categories.devilfruits.sculpt"
   );

   private static KeyMapping key(String name, int code) {
      return new KeyMapping("key.devilfruits.sculpt." + name, CONTEXT, Type.KEYSYM, code, "key.categories.devilfruits.sculpt");
   }

   public static void register(RegisterKeyMappingsEvent event) {
      event.register(WHEEL);
      event.register(PARAM);
      event.register(MIRROR);
      event.register(HOLLOW);
      event.register(VISCOSITY);
      event.register(UNDO);
      event.register(REDO);
   }

   private SculptKeys() {
   }
}
