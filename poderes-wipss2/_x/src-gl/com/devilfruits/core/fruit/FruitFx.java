package com.devilfruits.core.fruit;

import com.devilfruits.core.fx.CoreFx;
import net.minecraft.resources.ResourceLocation;

public record FruitFx(ResourceLocation awaken, ResourceLocation levelUp, ResourceLocation reincarnate, ResourceLocation formChange) {
   public static final FruitFx DEFAULT = new FruitFx(CoreFx.AWAKEN, CoreFx.LEVEL_UP, CoreFx.REINCARNATE, CoreFx.FORM_CHANGE);

   public FruitFx withAwaken(ResourceLocation id) {
      return new FruitFx(id, this.levelUp, this.reincarnate, this.formChange);
   }

   public FruitFx withLevelUp(ResourceLocation id) {
      return new FruitFx(this.awaken, id, this.reincarnate, this.formChange);
   }

   public FruitFx withReincarnate(ResourceLocation id) {
      return new FruitFx(this.awaken, this.levelUp, id, this.formChange);
   }

   public FruitFx withFormChange(ResourceLocation id) {
      return new FruitFx(this.awaken, this.levelUp, this.reincarnate, id);
   }
}
