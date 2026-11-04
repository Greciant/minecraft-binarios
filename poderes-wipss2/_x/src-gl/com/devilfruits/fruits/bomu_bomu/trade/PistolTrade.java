package com.devilfruits.fruits.bomu_bomu.trade;

import com.devilfruits.fruits.bomu_bomu.BomuBomu;
import com.devilfruits.fruits.bomu_bomu.BomuItems;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;

public final class PistolTrade implements ItemListing {
   private static final long SALT = 1601669284304320L;

   public static boolean chosen(UUID id, double chance) {
      long h = id.getMostSignificantBits() * -7046029254386353131L ^ id.getLeastSignificantBits() ^ 1601669284304320L;
      h = (h ^ h >>> 30) * -4658895280553007687L;
      h = (h ^ h >>> 27) * -7723592293110705685L;
      h ^= h >>> 31;
      double unit = (double)(h >>> 11) * 1.110223E-16F;
      return unit < chance;
   }

   @Nullable
   public MerchantOffer getOffer(Entity trader, RandomSource random) {
      if (!chosen(trader.getUUID(), BomuBomu.setting("pistolTradeChance"))) {
         return null;
      } else {
         int iron = (int)BomuBomu.setting("pistolPriceIron");
         return new MerchantOffer(
            new ItemCost(Items.EMERALD, (int)BomuBomu.setting("pistolPriceEmeralds")),
            iron > 0 ? Optional.of(new ItemCost(Items.IRON_INGOT, iron)) : Optional.empty(),
            new ItemStack((ItemLike)BomuItems.BOMB_PISTOL.get()),
            1,
            15,
            0.05F
         );
      }
   }
}
