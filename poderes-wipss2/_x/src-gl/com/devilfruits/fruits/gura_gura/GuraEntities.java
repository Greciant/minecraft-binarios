package com.devilfruits.fruits.gura_gura;

import com.devilfruits.fruits.gura_gura.entity.QuakeSlashEntity;
import com.devilfruits.fruits.gura_gura.entity.QuakeWaveEntity;
import com.devilfruits.registry.DFEntities;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;

public final class GuraEntities {
   public static final Supplier<EntityType<QuakeSlashEntity>> QUAKE_SLASH = DFEntities.ENTITIES
      .register(
         "gura_gura/quake_slash",
         () -> Builder.of(QuakeSlashEntity::new, MobCategory.MISC)
               .sized(1.0F, 1.0F)
               .clientTrackingRange(8)
               .updateInterval(1)
               .noSummon()
               .fireImmune()
               .build("gura_gura/quake_slash")
      );
   public static final Supplier<EntityType<QuakeWaveEntity>> QUAKE_WAVE = DFEntities.ENTITIES
      .register(
         "gura_gura/quake_wave",
         () -> Builder.of(QuakeWaveEntity::new, MobCategory.MISC)
               .sized(1.0F, 1.0F)
               .clientTrackingRange(10)
               .updateInterval(1)
               .noSummon()
               .fireImmune()
               .build("gura_gura/quake_wave")
      );

   static void init() {
   }

   private GuraEntities() {
   }
}
