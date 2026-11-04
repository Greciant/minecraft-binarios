package com.devilfruits.fruits.kage_kage;

import com.devilfruits.fruits.kage_kage.entity.Doppelman;
import com.devilfruits.fruits.kage_kage.entity.HollowCorpse;
import com.devilfruits.fruits.kage_kage.entity.ShadowZombie;
import com.devilfruits.registry.DFEntities;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.neoforged.bus.api.IEventBus;

public final class KageEntities {
   public static final Supplier<EntityType<Doppelman>> DOPPELMAN = DFEntities.ENTITIES
      .register(
         "kage_kage/doppelman",
         () -> Builder.of(Doppelman::new, MobCategory.MISC).sized(0.6F, 1.95F).clientTrackingRange(10).noSummon().build("kage_kage/doppelman")
      );
   public static final Supplier<EntityType<ShadowZombie>> SHADOW_ZOMBIE = DFEntities.ENTITIES
      .register(
         "kage_kage/shadow_zombie",
         () -> Builder.of(ShadowZombie::new, MobCategory.MISC).sized(0.6F, 1.95F).clientTrackingRange(10).noSummon().build("kage_kage/shadow_zombie")
      );
   public static final Supplier<EntityType<HollowCorpse>> HOLLOW_CORPSE = DFEntities.ENTITIES
      .register(
         "kage_kage/hollow_corpse",
         () -> Builder.of(HollowCorpse::new, MobCategory.MISC).sized(1.0F, 0.45F).clientTrackingRange(8).noSummon().build("kage_kage/hollow_corpse")
      );

   static void init(IEventBus modBus) {
      modBus.addListener(e -> {
         e.put(DOPPELMAN.get(), Doppelman.createAttributes().build());
         e.put(SHADOW_ZOMBIE.get(), ShadowZombie.createAttributes().build());
      });
   }

   private KageEntities() {
   }
}
