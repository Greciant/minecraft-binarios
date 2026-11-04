package com.devilfruits.fruits.uo_uo_seiryu;

import com.devilfruits.fruits.uo_uo_seiryu.entity.SeiryuDragonEntity;
import com.devilfruits.registry.DFEntities;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;

public final class SeiryuEntities {
   public static final Supplier<EntityType<SeiryuDragonEntity>> DRAGON_BODY = DFEntities.ENTITIES
      .register(
         "uo_uo_seiryu/dragon_body",
         () -> Builder.of(SeiryuDragonEntity::new, MobCategory.MISC)
               .sized(0.2F, 0.2F)
               .clientTrackingRange(16)
               .updateInterval(4)
               .noSave()
               .noSummon()
               .fireImmune()
               .build("uo_uo_seiryu/dragon_body")
      );

   static void init() {
   }

   private SeiryuEntities() {
   }
}
