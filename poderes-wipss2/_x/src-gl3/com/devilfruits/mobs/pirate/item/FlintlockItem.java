package com.devilfruits.mobs.pirate.item;

import com.devilfruits.mobs.pirate.PirateConfig;
import com.devilfruits.mobs.pirate.Pirates;
import com.devilfruits.mobs.pirate.entity.MusketBall;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FlintlockItem extends Item {
   private final boolean musket;

   public FlintlockItem(boolean musket, Properties properties) {
      super(properties);
      this.musket = musket;
   }

   public boolean isMusket() {
      return this.musket;
   }

   public boolean fire(Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (isLoaded(stack) && !player.level().isClientSide()) {
         Vec3 look = player.getLookAngle();
         Vec3 side = new Vec3(-look.z, 0.0, look.x).normalize().scale(hand == InteractionHand.MAIN_HAND ? 0.22 : -0.22);
         Vec3 muzzle = player.getEyePosition().add(look.scale(this.musket ? 1.3 : 1.0)).add(side).add(0.0, -0.12, 0.0);
         float damage = ((Double)(this.musket ? PirateConfig.MUSKET_DAMAGE : PirateConfig.PISTOL_DAMAGE).get()).floatValue();
         float spread = ((Double)(this.musket ? PirateConfig.MUSKET_SPREAD : PirateConfig.PISTOL_SPREAD).get()).floatValue();
         MusketBall.fire(
            player,
            muzzle,
            look,
            ((Double)PirateConfig.BALL_SPEED.get()).floatValue(),
            spread,
            damage,
            ((Double)PirateConfig.ARMOR_PIERCING.get()).floatValue(),
            this.musket
         );
         setLoaded(stack, false);
         stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
         player.getCooldowns().addCooldown(this, (Integer)(this.musket ? PirateConfig.MUSKET_COOLDOWN : PirateConfig.PISTOL_COOLDOWN).get());
         return true;
      } else {
         return false;
      }
   }

   public static boolean isLoaded(ItemStack stack) {
      return Boolean.TRUE.equals(stack.get((DataComponentType)Pirates.GUN_LOADED.get()));
   }

   public static void setLoaded(ItemStack stack, boolean loaded) {
      stack.set((DataComponentType)Pirates.GUN_LOADED.get(), loaded);
   }

   private int reloadTicks() {
      return this.musket ? (Integer)PirateConfig.MUSKET_RELOAD_TICKS.get() : (Integer)PirateConfig.PISTOL_RELOAD_TICKS.get();
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (isLoaded(stack)) {
         return InteractionResultHolder.pass(stack);
      } else if (!player.getAbilities().instabuild && !hasAmmo(player)) {
         level.playSound(null, player.getX(), player.getY(), player.getZ(), Pirates.GUN_CLICK.get(), SoundSource.PLAYERS, 0.5F, 0.6F);
         return InteractionResultHolder.fail(stack);
      } else {
         player.startUsingItem(hand);
         level.playSound(null, player.getX(), player.getY(), player.getZ(), Pirates.GUN_CLICK.get(), SoundSource.PLAYERS, 0.7F, 1.2F);
         return InteractionResultHolder.consume(stack);
      }
   }

   private static boolean hasAmmo(Player player) {
      return player.getInventory().hasAnyMatching(s -> s.is((Item)Pirates.MUSKET_BALL.get()))
         && (!(Boolean)PirateConfig.GUNPOWDER_PER_SHOT.get() || player.getInventory().hasAnyMatching(s -> s.is(Items.GUNPOWDER)));
   }

   private static void consume(Player player, Item item) {
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         ItemStack s = player.getInventory().getItem(i);
         if (s.is(item)) {
            s.shrink(1);
            return;
         }
      }
   }

   public int getUseDuration(ItemStack stack, LivingEntity entity) {
      return this.reloadTicks();
   }

   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.CROSSBOW;
   }

   public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
      if (entity.isDeadOrDying()) {
         entity.stopUsingItem();
      } else if (!level.isClientSide()) {
         int total = this.reloadTicks();
         int done = total - remaining;
         int from = (int)ReloadCues.drillTick(this.musket, (float)(done - 1), total);
         int to = (int)ReloadCues.drillTick(this.musket, (float)done, total);

         for (int tick = from + 1; tick <= to; tick++) {
            ReloadCues.sound(entity, ReloadCues.action(this.musket), tick, SoundSource.PLAYERS, false);
         }
      }
   }

   public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
      if (!level.isClientSide() && entity instanceof Player player && !player.isDeadOrDying()) {
         if (!player.getAbilities().instabuild) {
            if (!hasAmmo(player)) {
               return stack;
            }

            consume(player, (Item)Pirates.MUSKET_BALL.get());
            if ((Boolean)PirateConfig.GUNPOWDER_PER_SHOT.get()) {
               consume(player, Items.GUNPOWDER);
            }
         }

         setLoaded(stack, true);
         level.playSound(null, player.getX(), player.getY(), player.getZ(), Pirates.GUN_CLICK.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
      }

      return stack;
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(
         Component.translatable(isLoaded(stack) ? "item.devilfruits.pirate.gun.loaded" : "item.devilfruits.pirate.gun.empty")
            .withStyle(isLoaded(stack) ? ChatFormatting.GOLD : ChatFormatting.GRAY)
      );
   }
}
