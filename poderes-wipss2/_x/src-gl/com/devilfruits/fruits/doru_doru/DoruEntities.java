package com.devilfruits.fruits.doru_doru;

import com.devilfruits.fruits.doru_doru.entity.WaxLanceEntity;
import com.devilfruits.registry.DFEntities;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;

public final class DoruEntities {
   public static final Supplier<EntityType<WaxLanceEntity>> LANCE = DFEntities.ENTITIES
      .register(
         "doru_doru/wax_lance",
         () -> Builder.of(WaxLanceEntity::new, MobCategory.MISC)
               .sized(0.35F, 0.35F)
               .clientTrackingRange(10)
               .updateInterval(1)
               .noSummon()
               .build("doru_doru/wax_lance")
      );

   static void init() {
   }

   private DoruEntities() {
   }
}
