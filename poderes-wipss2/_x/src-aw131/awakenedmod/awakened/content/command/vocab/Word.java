package com.awakenedmod.awakened.content.command.vocab;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;

public interface Word {
   String id();

   int unlockLevel();

   String category();

   default Component displayName() {
      return Component.translatable(this.langKey());
   }

   default String langKey() {
      return "awakened.command." + this.category() + "." + this.id();
   }

   default String descriptionKey() {
      return this.langKey() + ".desc";
   }

   default Component description() {
      return Component.translatable(this.descriptionKey());
   }

   @Nullable
   default Clash clash(Target target, Effect effect, Trigger trigger) {
      return null;
   }
}
