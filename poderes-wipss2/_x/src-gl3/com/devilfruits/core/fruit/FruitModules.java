package com.devilfruits.core.fruit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FruitModules {
   private static final List<FruitModule> MODULES = new ArrayList<>();

   public static synchronized void add(FruitModule module, FruitModuleContext ctx) {
      if (FruitRegistry.isFrozen()) {
         throw new IllegalStateException("Fruit modules must be added during mod construction: " + module.id());
      } else {
         MODULES.add(module);
         module.register(ctx);
         if (FruitRegistry.get(module.id()) == null) {
            throw new IllegalStateException("Fruit module " + module.id() + " did not register its fruit");
         }
      }
   }

   public static List<FruitModule> all() {
      return Collections.unmodifiableList(MODULES);
   }

   private FruitModules() {
   }
}
