package com.awakenedmod.awakened.content.quest;

import com.awakenedmod.awakened.content.AwakenedItems;
import com.awakenedmod.awakened.content.relic.RelicItem;
import com.awakenedmod.awakened.core.ItemGrant;
import com.awakenedmod.awakened.core.quest.QuestReward;
import com.awakenedmod.awakened.core.relic.Relic;
import com.awakenedmod.awakened.core.relic.RelicDrops;
import com.awakenedmod.awakened.core.relic.RelicRegistry;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ItemLike;

public final class QuestRewards {
   private QuestRewards() {
   }

   public static QuestReward item(Supplier<? extends ItemLike> item, int count) {
      return stack(() -> new ItemStack(item.get(), count));
   }

   public static QuestReward stack(final Supplier<ItemStack> stack) {
      return new QuestReward() {
         @Override
         public ItemStack preview() {
            return stack.get();
         }

         @Override
         public Component label() {
            ItemStack s = stack.get();
            return (Component)(s.getCount() > 1
               ? Component.translatable("awakened.quest.reward_item_n", new Object[]{s.getCount(), s.getHoverName()})
               : s.getHoverName());
         }

         @Override
         public boolean grant(ServerPlayer player) {
            ItemStack s = stack.get();
            if (s.isEmpty()) {
               return false;
            } else {
               ItemGrant.to(player, s);
               return true;
            }
         }
      };
   }

   public static QuestReward relic(final String relicId) {
      return new QuestReward() {
         @Override
         public ItemStack preview() {
            return RelicItem.of(relicId);
         }

         @Override
         public Component label() {
            Relic relic = RelicRegistry.get(relicId);
            return (Component)(relic != null ? relic.displayName() : Component.translatable("awakened.quest.reward_relic"));
         }

         @Override
         public boolean grant(ServerPlayer player) {
            RelicItem.give(player, relicId);
            return true;
         }
      };
   }

   public static QuestReward relicBand(final Rarity floor, final Rarity ceil, final boolean favorHigh) {
      return new QuestReward() {
         @Override
         public ItemStack preview() {
            return new ItemStack((ItemLike)AwakenedItems.RELIC.get());
         }

         @Override
         public Component label() {
            return Component.translatable("awakened.quest.reward_relic");
         }

         @Override
         public boolean grant(ServerPlayer player) {
            String id = RelicDrops.roll(player.getRandom(), floor, ceil, favorHigh);
            if (id == null) {
               return false;
            } else {
               RelicItem.give(player, id);
               return true;
            }
         }
      };
   }

   public static QuestReward catalyst() {
      return item(AwakenedItems.CATALYST, 1);
   }
}
