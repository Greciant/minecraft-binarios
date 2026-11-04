package com.devilfruits.core.poneglyph;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DecipherDeskBlockEntity extends BlockEntity {
   public static final int INPUT = 0;
   public static final int PRIMER = 1;
   public static final int OUTPUT = 2;
   private final SimpleContainer items = new SimpleContainer(3) {
      public void setChanged() {
         super.setChanged();
         DecipherDeskBlockEntity.this.changed();
      }

      public int getMaxStackSize() {
         return 1;
      }
   };
   @Nullable
   private UUID reader;

   public DecipherDeskBlockEntity(BlockPos pos, BlockState state) {
      super(Poneglyphs.DECIPHER_DESK_BE.get(), pos, state);
   }

   public SimpleContainer items() {
      return this.items;
   }

   public boolean busyFor(ServerPlayer player) {
      if (this.reader != null && !this.reader.equals(player.getUUID()) && this.level != null && this.level.getServer() != null) {
         ServerPlayer other = this.level.getServer().getPlayerList().getPlayer(this.reader);
         if (other != null && other.containerMenu instanceof DecipherDeskMenu menu && menu.desk() == this) {
            return true;
         } else {
            this.reader = null;
            return false;
         }
      } else {
         return false;
      }
   }

   public void setReader(@Nullable UUID uuid) {
      this.reader = uuid;
      this.updateState();
   }

   private void changed() {
      this.setChanged();
      this.updateState();
   }

   private void updateState() {
      if (this.level != null && !this.level.isClientSide) {
         BlockState state = this.getBlockState();
         if (state.hasProperty(DecipherDeskBlock.SHEET)) {
            DecipherDeskBlock.Sheet sheet = !this.items.getItem(0).isEmpty()
               ? DecipherDeskBlock.Sheet.ENCRYPTED
               : (!this.items.getItem(2).isEmpty() ? DecipherDeskBlock.Sheet.DECIPHERED : DecipherDeskBlock.Sheet.NONE);
            BlockState next = (BlockState)((BlockState)((BlockState)state.setValue(DecipherDeskBlock.SHEET, sheet))
                  .setValue(DecipherDeskBlock.BOOK, !this.items.getItem(1).isEmpty()))
               .setValue(DecipherDeskBlock.ACTIVE, this.reader != null);
            if (next != state) {
               this.level.setBlock(this.worldPosition, next, 2);
            }
         }
      }
   }

   protected void saveAdditional(CompoundTag tag, Provider registries) {
      super.saveAdditional(tag, registries);
      ContainerHelper.saveAllItems(tag, this.items.getItems(), true, registries);
   }

   protected void loadAdditional(CompoundTag tag, Provider registries) {
      super.loadAdditional(tag, registries);
      this.items.getItems().replaceAll(s -> ItemStack.EMPTY);
      ContainerHelper.loadAllItems(tag, this.items.getItems(), registries);
   }

   public CompoundTag getUpdateTag(Provider registries) {
      CompoundTag tag = new CompoundTag();
      ContainerHelper.saveAllItems(tag, this.items.getItems(), true, registries);
      return tag;
   }

   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }
}
