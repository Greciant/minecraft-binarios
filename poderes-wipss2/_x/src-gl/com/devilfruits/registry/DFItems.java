package com.devilfruits.registry;

import com.devilfruits.core.gacha.CasketItem;
import com.devilfruits.core.gacha.GoldKeyItem;
import com.devilfruits.core.haki.HakiScrollItem;
import com.devilfruits.core.item.AbilityIconItem;
import com.devilfruits.core.item.SeastoneItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Items;

public final class DFItems {
   public static final Items ITEMS = DeferredRegister.createItems("devilfruits");
   public static final DeferredItem<AbilityIconItem> ABILITY_ICON = ITEMS.register("ability_icon", () -> new AbilityIconItem(new Properties().stacksTo(1)));
   public static final DeferredItem<SeastoneItem> SEASTONE_INGOT = ITEMS.register("seastone_ingot", () -> new SeastoneItem(new Properties()));
   public static final DeferredItem<BlockItem> SEASTONE_BLOCK = ITEMS.register(
      "seastone_block", () -> new SeastoneItem.Block((Block)DFBlocks.SEASTONE_BLOCK.get(), new Properties())
   );
   public static final DeferredItem<HakiScrollItem> HAKI_SCROLL = ITEMS.register(
      "haki_scroll_armament", () -> new HakiScrollItem(false, new Properties().stacksTo(16).rarity(Rarity.RARE))
   );
   public static final DeferredItem<HakiScrollItem> HAKI_SCROLL_MASTER = ITEMS.register(
      "haki_scroll_armament_master", () -> new HakiScrollItem(true, new Properties().stacksTo(16).rarity(Rarity.EPIC))
   );
   public static final DeferredItem<CasketItem> CASKET = ITEMS.register(
      "devil_fruit_casket", () -> new CasketItem(new Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC))
   );
   public static final DeferredItem<GoldKeyItem> GOLD_KEY = ITEMS.register("gold_key", () -> new GoldKeyItem(new Properties()));

   private DFItems() {
   }
}
