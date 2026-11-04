package com.awakenedmod.awakened.content.item;

import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Fx;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public final class ArbiterSummonsItem extends Item {
   private static final TagKey<Structure> DESTINATION = TagKey.create(Registries.STRUCTURE, AwId.of("on_arbiter_maps"));
   private static final int SEARCH_RADIUS = 10;
   private static final boolean SKIP_EXISTING_CHUNKS = false;
   private static final byte MAP_ZOOM = 1;
   private static final int COOLDOWN_TICKS = 100;

   public ArbiterSummonsItem(Properties properties) {
      super(properties);
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack result = reveal(level, player, hand);
      return InteractionResultHolder.sidedSuccess(result == null ? player.getItemInHand(hand) : result, level.isClientSide());
   }

   private static ItemStack reveal(Level level, Player player, InteractionHand hand) {
      if (player instanceof ServerPlayer sp && level instanceof ServerLevel server) {
         ItemStack held = sp.getItemInHand(hand);
         sp.getCooldowns().addCooldown(held.getItem(), 100);
         BlockPos target = server.findNearestMapStructure(DESTINATION, sp.blockPosition(), 10, false);
         if (target == null) {
            sp.displayClientMessage(Component.translatable("awakened.arbiter_summons.none").withStyle(ChatFormatting.GRAY), true);
            Fx.sound(server, sp.position(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.7F, 0.7F);
            return null;
         }

         sp.awardStat(Stats.ITEM_USED.get(held.getItem()));
         Fx.sound(server, sp.position(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 1.0F, 1.0F);
         return ItemUtils.createFilledResult(held, sp, map(server, target), false);
      }

      return null;
   }

   private static ItemStack map(ServerLevel level, BlockPos target) {
      ItemStack map = MapItem.create(level, target.getX(), target.getZ(), (byte)1, true, true);
      MapItem.renderBiomePreviewMap(level, map);
      MapItemSavedData.addTargetDecoration(map, target, "+", MapDecorationTypes.RED_X);
      map.set(
         DataComponents.ITEM_NAME,
         Component.translatable("item.awakened.arbiter_summons").withStyle(s -> s.withColor(ChatFormatting.DARK_PURPLE).withItalic(false))
      );
      map.set(DataComponents.LORE, new ItemLore(List.of(lore("item.awakened.arbiter_summons.map_lore_1"), lore("item.awakened.arbiter_summons.map_lore_2"))));
      return map;
   }

   private static Component lore(String key) {
      return Component.translatable(key).withStyle(s -> s.withColor(ChatFormatting.GRAY).withItalic(true));
   }
}
