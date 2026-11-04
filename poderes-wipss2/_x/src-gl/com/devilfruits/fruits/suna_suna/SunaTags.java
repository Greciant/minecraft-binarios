package com.devilfruits.fruits.suna_suna;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class SunaTags {
   public static final TagKey<Block> SAND_SOURCE = block("sand_source");
   public static final TagKey<Block> DRIES_TO_SAND = block("dries_to_sand");
   public static final TagKey<Block> WITHERS = block("withers");
   public static final TagKey<Block> DRIES_AWAY = block("dries_away");
   public static final TagKey<Block> SPADA_RESISTS = block("spada_resists");

   private static TagKey<Block> block(String path) {
      return TagKey.create(Registries.BLOCK, SunaSuna.id(path));
   }

   private SunaTags() {
   }
}
