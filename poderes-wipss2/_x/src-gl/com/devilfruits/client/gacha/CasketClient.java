package com.devilfruits.client.gacha;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.animation.AnimationController;
import com.devilfruits.client.dev.AutoTest;
import com.devilfruits.client.dev.CasketCapture;
import com.devilfruits.client.vfx.VfxSound;
import com.devilfruits.core.gacha.CasketItem;
import com.devilfruits.registry.DFItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.common.NeoForge;

public final class CasketClient {
   private static final Int2IntMap HEARD = new Int2IntOpenHashMap();
   private static final float[] BEATS = new float[]{0.0F, 0.2F, 0.45F, 0.62F, 0.75F, 0.88F};
   private static final IClientItemExtensions CASKET = new IClientItemExtensions() {
      public BlockEntityWithoutLevelRenderer getCustomRenderer() {
         return CasketItemRenderer.get();
      }

      public boolean applyForgeHandTransform(
         PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack, float partialTick, float equipProcess, float swingProcess
      ) {
         if (CasketClient.unlocking(player) && player.getUseItem() == stack) {
            int side = arm == HumanoidArm.RIGHT ? 1 : -1;
            float raise = CasketItemRenderer.smooth(((float)player.getTicksUsingItem() + partialTick) / 6.0F);
            pose.translate((float)side * Mth.lerp(raise, 0.56F, 0.12F), Mth.lerp(raise, -0.52F, -0.34F), Mth.lerp(raise, -0.72F, -0.62F));
            pose.mulPose(Axis.YP.rotationDegrees((float)side * -6.0F * raise));
            return true;
         } else {
            return false;
         }
      }
   };
   private static final IClientItemExtensions KEY = new IClientItemExtensions() {
      public boolean applyForgeHandTransform(
         PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack, float partialTick, float equipProcess, float swingProcess
      ) {
         if (CasketClient.unlocking(player) && player.getOffhandItem() == stack) {
            pose.translate(0.0F, -4.0F, 0.0F);
            return true;
         } else {
            return false;
         }
      }
   };

   public static void init(IEventBus modBus) {
      AnimationController.registerUseAnimation(
         s -> s.is((Item)DFItems.CASKET.get()), DevilFruits.id("gacha/casket_unlock"), DevilFruits.id("gacha/casket_unlock")
      );
      modBus.addListener(e -> {
         e.registerItem(CASKET, new Item[]{(Item)DFItems.CASKET.get()});
         e.registerItem(KEY, new Item[]{(Item)DFItems.GOLD_KEY.get()});
      });
      modBus.addListener(e -> {
         e.register(CasketItemRenderer.BASE);
         e.register(CasketItemRenderer.LID);
         e.register(CasketItemRenderer.KEY);
      });
      modBus.addListener(
         e -> e.enqueueWork(
               () -> ItemProperties.register(
                     (Item)DFItems.CASKET.get(), DevilFruits.id("casket_use"), (stack, level, entity, seed) -> useProgress(stack, entity)
                  )
            )
      );
      NeoForge.EVENT_BUS.addListener(e -> CasketRevealClient.onRenderStage(e));
      NeoForge.EVENT_BUS.addListener(e -> tick());
      NeoForge.EVENT_BUS.addListener(e -> {
         HEARD.clear();
         CasketRevealClient.clear();
      });
      if (AutoTest.ENABLED) {
         for (boolean look : new boolean[]{false, true}) {
            AutoTest.addScript(DevilFruits.id("vfx_casket" + (look ? "_look" : "")), new CasketCapture(look));
         }
      }
   }

   private static float useProgress(ItemStack stack, LivingEntity entity) {
      if (entity != null && entity.isUsingItem() && entity.getUseItem() == stack) {
         float p = Mth.clamp((float)entity.getTicksUsingItem() / (float)CasketItem.useTicks(), 0.0F, 1.0F);
         CasketItemRenderer.progress = p;
         CasketItemRenderer.seed = entity.getId();
         CasketItemRenderer.offhand = entity.getOffhandItem();
         return Math.max(0.001F, p);
      } else {
         return 0.0F;
      }
   }

   private static boolean unlocking(LivingEntity entity) {
      return entity.isUsingItem() && entity.getUsedItemHand() == InteractionHand.MAIN_HAND && entity.getUseItem().is((Item)DFItems.CASKET.get());
   }

   private static void tick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && !mc.isPaused()) {
         for (Player player : mc.level.players()) {
            if (!unlocking(player)) {
               HEARD.remove(player.getId());
            } else {
               int used = player.getTicksUsingItem();
               int total = CasketItem.useTicks();
               int heard = HEARD.getOrDefault(player.getId(), -1);
               if (heard < 0) {
                  hum(player, total);
               }

               for (int i = 0; i < BEATS.length; i++) {
                  int at = Math.round(BEATS[i] * (float)total);
                  if (at > heard && at <= used) {
                     beat(player, i);
                  }
               }

               HEARD.put(player.getId(), used);
            }
         }
      }
   }

   private static void hum(Player player, int total) {
      VfxSound.play(
         SoundEvents.BEACON_AMBIENT,
         player,
         player.position(),
         true,
         age -> 0.15F + 0.75F * Mth.clamp((float)age / (float)total, 0.0F, 1.0F),
         age -> 0.7F + 0.9F * Mth.clamp((float)age / (float)total, 0.0F, 1.0F),
         total + 5,
         () -> unlocking(player)
      );
   }

   private static void beat(Player player, int i) {
      switch (i) {
         case 0:
            play(player, SoundEvents.VAULT_INSERT_ITEM, 0.8F, 1.1F);
            break;
         case 1:
            play(player, SoundEvents.LEVER_CLICK, 0.6F, 0.7F);
            play(player, SoundEvents.CHEST_LOCKED, 0.4F, 1.5F);
            break;
         case 2:
            play(player, SoundEvents.LEVER_CLICK, 0.7F, 0.9F);
            play(player, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5F, 1.0F);
            break;
         case 3:
            play(player, SoundEvents.CHEST_OPEN, 0.25F, 0.5F);
            break;
         case 4:
            play(player, SoundEvents.LEVER_CLICK, 0.8F, 1.1F);
            play(player, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6F, 1.3F);
            break;
         default:
            play(player, SoundEvents.CHEST_OPEN, 0.3F, 0.6F);
            play(player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.6F, 1.6F);
      }
   }

   private static void play(Player player, SoundEvent event, float volume, float pitch) {
      player.level().playLocalSound(player.getX(), player.getEyeY() - 0.4, player.getZ(), event, SoundSource.PLAYERS, volume, pitch, false);
   }

   private CasketClient() {
   }
}
