package com.devilfruits.registry;

import com.devilfruits.core.gacha.CasketLootModifier;
import com.devilfruits.core.haki.HakiScrollLootModifier;
import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries.Keys;

public final class DFLootModifiers {
   public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS = DeferredRegister.create(
      Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "devilfruits"
   );
   public static final Supplier<MapCodec<CasketLootModifier>> ADD_CASKET = SERIALIZERS.register("add_casket", () -> CasketLootModifier.CODEC);
   public static final Supplier<MapCodec<HakiScrollLootModifier>> ADD_HAKI_SCROLL = SERIALIZERS.register("add_haki_scroll", () -> HakiScrollLootModifier.CODEC);

   private DFLootModifiers() {
   }
}
