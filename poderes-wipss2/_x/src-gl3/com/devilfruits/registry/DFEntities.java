package com.devilfruits.registry;

import com.devilfruits.core.world.DebrisEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DFEntities {
   public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, "devilfruits");
   public static final Supplier<EntityType<DebrisEntity>> DEBRIS = ENTITIES.register(
      "debris", () -> Builder.of(DebrisEntity::new, MobCategory.MISC).sized(0.98F, 0.98F).clientTrackingRange(10).updateInterval(2).noSummon().build("debris")
   );

   private DFEntities() {
   }
}
