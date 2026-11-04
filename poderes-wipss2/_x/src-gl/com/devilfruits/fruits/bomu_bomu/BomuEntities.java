package com.devilfruits.fruits.bomu_bomu;

import com.devilfruits.fruits.bomu_bomu.entity.BombBullet;
import com.devilfruits.fruits.bomu_bomu.entity.BoogerBomb;
import com.devilfruits.registry.DFEntities;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;

public final class BomuEntities {
   public static final Supplier<EntityType<BoogerBomb>> BOOGER = DFEntities.ENTITIES
      .register(
         "bomu_bomu/booger_bomb",
         () -> Builder.of(BoogerBomb::new, MobCategory.MISC)
               .sized(0.25F, 0.25F)
               .clientTrackingRange(8)
               .updateInterval(1)
               .noSummon()
               .fireImmune()
               .build("bomu_bomu/booger_bomb")
      );
   public static final Supplier<EntityType<BombBullet>> BULLET = DFEntities.ENTITIES
      .register(
         "bomu_bomu/bomb_bullet",
         () -> Builder.of(BombBullet::new, MobCategory.MISC)
               .sized(0.15F, 0.15F)
               .clientTrackingRange(10)
               .updateInterval(1)
               .noSummon()
               .fireImmune()
               .build("bomu_bomu/bomb_bullet")
      );

   static void init() {
   }

   private BomuEntities() {
   }
}
