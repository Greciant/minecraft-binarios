package com.devilfruits.core.poneglyph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

public class ConfigGatePlacement extends PlacementModifier {
   public static final MapCodec<ConfigGatePlacement> CODEC = Codec.STRING.fieldOf("key").xmap(ConfigGatePlacement::new, c -> c.key);
   private final String key;

   public ConfigGatePlacement(String key) {
      this.key = key;
   }

   private boolean enabled() {
      String var1 = this.key;
      byte var2 = -1;
      switch (var1.hashCode()) {
         case 551995545:
            if (var1.equals("seastone_ore")) {
               var2 = 0;
            }
         default:
            return switch (var2) {
               case 0 -> PoneglyphConfig.SEASTONE_ORE.get();
               default -> true;
            };
      }
   }

   public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
      return this.enabled() ? Stream.of(pos) : Stream.empty();
   }

   public PlacementModifierType<?> type() {
      return Poneglyphs.CONFIG_GATE.get();
   }
}
