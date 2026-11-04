package com.devilfruits.fruits.yomi_yomi;

import com.devilfruits.fruits.yomi_yomi.entity.PlayerCorpse;
import com.devilfruits.fruits.yomi_yomi.entity.SoulShell;
import com.devilfruits.registry.DFEntities;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;

public final class YomiEntities {
   public static final Supplier<EntityType<PlayerCorpse>> PLAYER_CORPSE = DFEntities.ENTITIES
      .register(
         "yomi_yomi/player_corpse",
         () -> Builder.of(PlayerCorpse::new, MobCategory.MISC)
               .sized(1.8F, 0.45F)
               .clientTrackingRange(10)
               .noSummon()
               .fireImmune()
               .build("yomi_yomi/player_corpse")
      );
   public static final Supplier<EntityType<SoulShell>> SOUL_SHELL = DFEntities.ENTITIES
      .register(
         "yomi_yomi/soul_shell",
         () -> Builder.of(SoulShell::new, MobCategory.MISC)
               .sized(0.6F, 1.3F)
               .clientTrackingRange(10)
               .noSummon()
               .fireImmune()
               .noSave()
               .build("yomi_yomi/soul_shell")
      );

   static void init() {
   }

   private YomiEntities() {
   }
}
