package com.devilfruits.fruits.horo_horo;

import com.devilfruits.fruits.horo_horo.entity.GhostBody;
import com.devilfruits.registry.DFEntities;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;

public final class HoroEntities {
   public static final Supplier<EntityType<GhostBody>> GHOST_BODY = DFEntities.ENTITIES
      .register(
         "horo_horo/ghost_body",
         () -> Builder.of(GhostBody::new, MobCategory.MISC)
               .sized(0.6F, 1.2F)
               .clientTrackingRange(10)
               .noSummon()
               .fireImmune()
               .noSave()
               .build("horo_horo/ghost_body")
      );

   static void init() {
   }

   private HoroEntities() {
   }
}
