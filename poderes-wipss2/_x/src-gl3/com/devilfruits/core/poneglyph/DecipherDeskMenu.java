package com.devilfruits.core.poneglyph;

import com.devilfruits.core.network.DFNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.NeoForge;

public class DecipherDeskMenu extends AbstractContainerMenu {
   public static final int STATUS_EMPTY = 0;
   public static final int STATUS_ACTIVE = 1;
   public static final int STATUS_OUTPUT_FULL = 2;
   public static final int STATUS_TOO_HARD = 3;
   public static final int STATUS_UNREADABLE = 4;
   public static final int EV_NONE = 0;
   public static final int EV_CORRECT = 1;
   public static final int EV_WRONG = 2;
   public static final int EV_FORGIVEN = 3;
   public static final int EV_RESET = 4;
   public static final int EV_SUCCESS = 5;
   public static final int EV_SMUDGED = 6;
   public static final int EV_HINT = 7;
   public static final int EV_REVEAL = 8;
   public static final int EV_LOCKED = 9;
   public static final int BUTTON_HINT = 100;
   public static final int BUTTON_REVEAL = 101;
   public static final int INPUT_X = 227;
   public static final int INPUT_Y = 15;
   public static final int PRIMER_X = 269;
   public static final int PRIMER_Y = 15;
   public static final int OUTPUT_X = 248;
   public static final int OUTPUT_Y = 43;
   public static final int INV_X = 9;
   public static final int INV_Y = 141;
   private final Container container;
   @Nullable
   private final DecipherDeskBlockEntity desk;
   private final Player player;
   private ItemStack seenInput = ItemStack.EMPTY;
   private boolean seenOutput;
   private boolean dirty = true;
   private int event = 0;
   private int eventIndex = -1;
   private List<PoneglyphPuzzle.Round> rounds = List.of();
   private int roundsKey;

   public DecipherDeskMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
      this(id, inventory, new SimpleContainer(3), null);
      buf.readBlockPos();
   }

   public DecipherDeskMenu(int id, Inventory inventory, DecipherDeskBlockEntity desk) {
      this(id, inventory, desk.items(), desk);
      desk.setReader(inventory.player.getUUID());
   }

   private DecipherDeskMenu(int id, Inventory inventory, Container container, @Nullable DecipherDeskBlockEntity desk) {
      super(Poneglyphs.DECIPHER_DESK_MENU.get(), id);
      this.container = container;
      this.desk = desk;
      this.player = inventory.player;
      this.addSlot(new Slot(container, 0, 227, 15) {
         public boolean mayPlace(ItemStack stack) {
            return stack.is((Item)Poneglyphs.ENCRYPTED_BLUEPRINT.get());
         }
      });
      this.addSlot(new Slot(container, 1, 269, 15) {
         public boolean mayPlace(ItemStack stack) {
            return stack.is((Item)Poneglyphs.PRIMER.get());
         }
      });
      this.addSlot(new Slot(container, 2, 248, 43) {
         public boolean mayPlace(ItemStack stack) {
            return false;
         }
      });

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col + row * 9 + 9, 9 + col * 18, 141 + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.addSlot(new Slot(inventory, col, 9 + col * 18, 199));
      }
   }

   @Nullable
   public DecipherDeskBlockEntity desk() {
      return this.desk;
   }

   public boolean hasPrimer() {
      return this.container.getItem(1).is((Item)Poneglyphs.PRIMER.get());
   }

   public boolean stillValid(Player player) {
      return this.desk == null || Container.stillValidBlockEntity(this.desk, player);
   }

   public void removed(Player player) {
      super.removed(player);
      if (this.desk != null && !player.level().isClientSide) {
         this.desk.setReader(null);
      }
   }

   public ItemStack quickMoveStack(Player player, int index) {
      Slot slot = (Slot)this.slots.get(index);
      if (!slot.hasItem()) {
         return ItemStack.EMPTY;
      } else {
         ItemStack stack = slot.getItem();
         ItemStack copy = stack.copy();
         if (index < 3) {
            if (!this.moveItemStackTo(stack, 3, this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (stack.is((Item)Poneglyphs.ENCRYPTED_BLUEPRINT.get())) {
            if (!this.moveItemStackTo(stack, 0, 1, false)) {
               return ItemStack.EMPTY;
            }
         } else if (stack.is((Item)Poneglyphs.PRIMER.get())) {
            if (!this.moveItemStackTo(stack, 1, 2, false)) {
               return ItemStack.EMPTY;
            }
         } else if (index < 30 ? !this.moveItemStackTo(stack, 30, this.slots.size(), false) : !this.moveItemStackTo(stack, 3, 30, false)) {
            return ItemStack.EMPTY;
         }

         if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }

         return copy;
      }
   }

   public boolean clickMenuButton(Player clicker, int id) {
      if (clicker instanceof ServerPlayer sp && clicker == this.player) {
         BlueprintData.Encrypted enc = this.activeBlueprint();
         PoneglyphData.Poneglyph def = enc == null ? null : PoneglyphData.poneglyph(enc.poneglyph());
         if (enc != null && def != null && this.status(enc, def) == 1) {
            BlueprintData.Progress p = this.progress(enc);
            PoneglyphPuzzle.Round round = this.round(enc, p, def);
            int literacy = Literacy.level(sp);
            long now = sp.level().getGameTime();
            this.dirty = true;
            if (id == 100) {
               if (Literacy.hint(literacy) && !p.has(1)) {
                  this.write(enc.with(withRoundMark(p.with(1), 8, p.round())));
                  this.event(7, -1);
               }

               return true;
            }

            if (id == 101) {
               if (Literacy.reveal(literacy) && !p.has(2)) {
                  this.write(enc.with(withRoundMark(p.with(2), 16, p.round())));
                  this.event(8, round.correct());
               }

               return true;
            }

            if (id >= 0 && id < round.options().size()) {
               int lock = (int)Math.round((double)((Integer)PoneglyphConfig.LOCKOUT_TICKS.get()).intValue() * Literacy.lockoutFactor(literacy));
               PoneglyphPuzzle.Step step = PoneglyphPuzzle.pick(
                  p,
                  id == round.correct(),
                  this.rounds.size(),
                  now,
                  lock,
                  (Integer)PoneglyphConfig.MAX_MISTAKES.get(),
                  Literacy.forgive(literacy),
                  (Boolean)PoneglyphConfig.HARD_MODE.get()
               );
               switch (step.outcome()) {
                  case LOCKED:
                     this.event(9, id);
                     break;
                  case CORRECT:
                     this.write(enc.with(step.progress()));
                     Literacy.addXp(sp, (Integer)PoneglyphConfig.XP_ROUND.get());
                     this.sound(Poneglyphs.SOUND_CORRECT.get(), 1.0F);
                     this.sound(Poneglyphs.SOUND_PAGE.get(), 1.0F);
                     this.event(1, id);
                     break;
                  case SOLVED:
                     Literacy.addXp(sp, (Integer)PoneglyphConfig.XP_ROUND.get());
                     this.finish(sp, enc, def);
                     this.event(5, id);
                     break;
                  case WRONG:
                     this.write(enc.with(step.progress()));
                     this.sound(Poneglyphs.SOUND_WRONG.get(), 1.0F);
                     this.event(2, id);
                     break;
                  case FORGIVEN:
                     this.write(enc.with(step.progress()));
                     this.sound(Poneglyphs.SOUND_WRONG.get(), 1.4F);
                     sp.displayClientMessage(Component.translatable("poneglyph.devilfruits.message.forgiven").withStyle(ChatFormatting.AQUA), true);
                     this.event(3, id);
                     break;
                  case RESET:
                     this.write(enc.with(step.progress()));
                     this.sound(Poneglyphs.SOUND_WRONG.get(), 0.7F);
                     this.sound(Poneglyphs.SOUND_PAGE.get(), 0.8F);
                     sp.displayClientMessage(Component.translatable("poneglyph.devilfruits.message.reset").withStyle(ChatFormatting.RED), true);
                     this.event(4, id);
                     break;
                  case DESTROYED:
                     this.container.setItem(0, ItemStack.EMPTY);
                     this.sound(Poneglyphs.SOUND_SMUDGE.get(), 1.0F);
                     sp.displayClientMessage(Component.translatable("poneglyph.devilfruits.message.smudged").withStyle(ChatFormatting.DARK_RED), true);
                     this.event(6, id);
               }

               return true;
            }

            return false;
         }

         return false;
      }

      return true;
   }

   private void finish(ServerPlayer sp, BlueprintData.Encrypted enc, PoneglyphData.Poneglyph def) {
      ItemStack out = new ItemStack((ItemLike)Poneglyphs.DECIPHERED_BLUEPRINT.get());
      out.set((DataComponentType)Poneglyphs.DECIPHERED.get(), new BlueprintData.Deciphered(def.knowledge(), enc.poneglyph()));
      this.container.setItem(0, ItemStack.EMPTY);
      this.container.setItem(2, out);
      Literacy.addXp(sp, (Integer)PoneglyphConfig.XP_DECIPHER.get());
      NeoForge.EVENT_BUS.post(new PoneglyphApiEvents.BlueprintDecipheredEvent(sp, enc.poneglyph(), def.knowledge()));
      Poneglyphs.DECIPHERED_TRIGGER.get().trigger(sp);
      this.sound(Poneglyphs.SOUND_SUCCESS.get(), 1.0F);
      this.sound(Poneglyphs.SOUND_SEAL.get(), 1.0F);
      if (this.desk != null && sp.level() instanceof ServerLevel level) {
         level.sendParticles(
            Poneglyphs.GLYPH_PARTICLE.get(),
            (double)this.desk.getBlockPos().getX() + 0.5,
            (double)this.desk.getBlockPos().getY() + 1.1,
            (double)this.desk.getBlockPos().getZ() + 0.5,
            28,
            0.35,
            0.25,
            0.35,
            0.06
         );
      }

      if (sp.getRandom().nextDouble() < (Double)PoneglyphConfig.LEAD_MAP_CHANCE.get()) {
         ItemStack map = LeadMaps.create(sp);
         if (!map.isEmpty()) {
            if (!sp.getInventory().add(map)) {
               sp.drop(map, false);
            }

            sp.displayClientMessage(Component.translatable("poneglyph.devilfruits.message.lead_map").withStyle(ChatFormatting.GOLD), false);
         }
      }
   }

   public void broadcastChanges() {
      super.broadcastChanges();
      if (this.player instanceof ServerPlayer sp) {
         ItemStack var4 = this.container.getItem(0);
         boolean output = !this.container.getItem(2).isEmpty();
         if (!ItemStack.matches(var4, this.seenInput) || output != this.seenOutput) {
            this.dirty = true;
         }

         if (this.dirty) {
            this.dirty = false;
            DFNetwork.sendTo(sp, this.buildState(sp));
            this.seenInput = this.container.getItem(0).copy();
            this.seenOutput = !this.container.getItem(2).isEmpty();
            this.event = 0;
            this.eventIndex = -1;
         }
      }
   }

   private PoneglyphNetwork.DeskStatePayload buildState(ServerPlayer sp) {
      int literacy = Literacy.level(sp);
      int xp = Literacy.get(sp).xp();
      int xpFrom = Literacy.xpFor(literacy);
      int xpTo = literacy >= 5 ? xpFrom : Literacy.xpFor(literacy + 1);
      int perks = (Literacy.hint(literacy) ? 4 : 0) | (Literacy.reveal(literacy) ? 8 : 0);
      BlueprintData.Encrypted enc = this.activeBlueprint();
      PoneglyphData.Poneglyph def = enc == null ? null : PoneglyphData.poneglyph(enc.poneglyph());
      int status = enc == null ? 0 : (def == null ? 4 : this.status(enc, def));
      if (status != 1) {
         return new PoneglyphNetwork.DeskStatePayload(
            this.containerId, status, 0, 0, "", List.of(), 0, 0, 0, "", -1, perks, literacy, xp, xpFrom, xpTo, this.event, this.eventIndex
         );
      } else {
         BlueprintData.Progress p = this.progress(enc);
         PoneglyphPuzzle.Round round = this.round(enc, p, def);
         int[] perm = Cipher.active(sp.server);
         List<String> options = new ArrayList<>(round.options().size());
         round.options().forEach(o -> options.add(Cipher.encode(o, perm)));
         long lock = Math.max(0L, p.lockUntil() - sp.level().getGameTime());
         String hint = p.has(1) && roundMark(p, 8) == p.round() ? Cipher.encode(round.clue().substring(0, 1), perm) : "";
         int reveal = p.has(2) && roundMark(p, 16) == p.round() ? round.correct() : -1;
         if (Literacy.hint(literacy) && !p.has(1)) {
            perks |= 1;
         }

         if (Literacy.reveal(literacy) && !p.has(2)) {
            perks |= 2;
         }

         return new PoneglyphNetwork.DeskStatePayload(
            this.containerId,
            1,
            p.round(),
            this.rounds.size(),
            round.clue(),
            options,
            p.mistakes(),
            (Integer)PoneglyphConfig.MAX_MISTAKES.get(),
            (int)Math.min(lock, 72000L),
            hint,
            reveal,
            perks,
            literacy,
            xp,
            xpFrom,
            xpTo,
            this.event,
            this.eventIndex
         );
      }
   }

   private int status(BlueprintData.Encrypted enc, PoneglyphData.Poneglyph def) {
      if (!this.container.getItem(2).isEmpty()) {
         return 2;
      } else {
         return Literacy.level(this.player) < def.minLiteracy() ? 3 : 1;
      }
   }

   @Nullable
   private BlueprintData.Encrypted activeBlueprint() {
      ItemStack input = this.container.getItem(0);
      return input.is((Item)Poneglyphs.ENCRYPTED_BLUEPRINT.get()) ? (BlueprintData.Encrypted)input.get((DataComponentType)Poneglyphs.ENCRYPTED.get()) : null;
   }

   private BlueprintData.Progress progress(BlueprintData.Encrypted enc) {
      int owner = ownerHash(this.player);
      if (enc.progress().owner() == owner) {
         return enc.progress();
      } else {
         BlueprintData.Progress fresh = new BlueprintData.Progress(owner, 0, 0, 0, 0L, 0, 0);
         this.write(enc.with(fresh));
         return fresh;
      }
   }

   private PoneglyphPuzzle.Round round(BlueprintData.Encrypted enc, BlueprintData.Progress p, PoneglyphData.Poneglyph def) {
      int options = Math.max(3, Math.min(12, (Integer)PoneglyphConfig.OPTIONS.get() - Literacy.decoyReduction(Literacy.level(this.player))));
      int key = Objects.hash(enc.seed(), enc.poneglyph(), p.owner(), p.draw(), options, PoneglyphConfig.ROUNDS.get());
      if (key != this.roundsKey || this.rounds.isEmpty()) {
         List<String> others = new ArrayList<>();

         for (Entry<ResourceLocation, PoneglyphData.Poneglyph> e : PoneglyphData.poneglyphs().entrySet()) {
            if (!e.getKey().equals(enc.poneglyph())) {
               others.addAll(e.getValue().words());
            }
         }

         this.rounds = PoneglyphPuzzle.generate(
            enc.seed(), playerKey(this.player), p.draw(), def.words(), others, (Integer)PoneglyphConfig.ROUNDS.get(), options
         );
         this.roundsKey = key;
      }

      return PoneglyphPuzzle.current(this.rounds, p, enc.seed());
   }

   private void write(BlueprintData.Encrypted enc) {
      ItemStack stack = this.container.getItem(0).copy();
      stack.set((DataComponentType)Poneglyphs.ENCRYPTED.get(), enc);
      this.container.setItem(0, stack);
   }

   private void event(int type, int index) {
      this.event = type;
      this.eventIndex = index;
      this.dirty = true;
   }

   private void sound(SoundEvent sound, float pitch) {
      if (this.desk != null) {
         this.player.level().playSound(null, this.desk.getBlockPos(), sound, SoundSource.BLOCKS, 0.8F, pitch);
      }
   }

   public static long playerKey(Player player) {
      return player.getUUID().getMostSignificantBits() ^ player.getUUID().getLeastSignificantBits();
   }

   public static int ownerHash(Player player) {
      long key = playerKey(player);
      int h = (int)(key ^ key >>> 32);
      return h == 0 ? 1 : h;
   }

   private static BlueprintData.Progress withRoundMark(BlueprintData.Progress p, int shift, int round) {
      int flags = p.flags() & ~(255 << shift) | round + 1 << shift;
      return new BlueprintData.Progress(p.owner(), p.draw(), p.round(), p.mistakes(), p.lockUntil(), flags, p.shuffle());
   }

   private static int roundMark(BlueprintData.Progress p, int shift) {
      return (p.flags() >>> shift & 0xFF) - 1;
   }
}
