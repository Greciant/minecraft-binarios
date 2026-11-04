package com.awakenedmod.awakened.content.spider;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.net.SpiderForceStopPayload;
import com.awakenedmod.awakened.core.net.SpiderSwingPayload;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent.Size;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(
   modid = "awakened"
)
public final class WebWingsAbility extends Ability {
   public static final String ID = "web_wings";
   private static final int UNLOCK = 10;
   private static final float COST = 3.0F;
   private static final float GLIDE_DRAIN = 0.25F;
   private final Map<UUID, Boolean> gliders = this.activeState();
   private static final Set<UUID> GLIDING = ConcurrentHashMap.newKeySet();
   private static final EntityDimensions GLIDE_DIMENSIONS = EntityDimensions.scalable(0.6F, 0.6F).withEyeHeight(0.4F);

   public static boolean isUnlocked(int masteryLevel) {
      return masteryLevel >= 10;
   }

   public static boolean isGliding(Player player) {
      return GLIDING.contains(player.getUUID());
   }

   public static EntityDimensions glideDimensions(Player player) {
      return GLIDE_DIMENSIONS.scale(player.getScale());
   }

   @SubscribeEvent
   static void onEntitySize(Size event) {
      if (event.getEntity() instanceof Player p && !p.level().isClientSide() && isGliding(p)) {
         event.setNewSize(glideDimensions(p));
      }
   }

   private static void setGlideState(ServerPlayer player, boolean gliding) {
      if (gliding) {
         GLIDING.add(player.getUUID());
      } else {
         GLIDING.remove(player.getUUID());
      }

      SpiderVisualState.setGliding(player, gliding);
      player.refreshDimensions();
   }

   public WebWingsAbility() {
      super("web_wings", 3.0F, 10);
   }

   public static float cost() {
      return StaminaSystem.effectiveCost(3.0F, WebWingsAbility.class.getSimpleName());
   }

   @Override
   public boolean clientDriven() {
      return true;
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public boolean occupiesHands(ServerPlayer player) {
      return false;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      return false;
   }

   public static void handleClientWings(ServerPlayer player, SpiderSwingPayload payload) {
      WebWingsAbility wings = SpiderClientFire.resolve(player, payload.slot(), WebWingsAbility.class);
      if (wings != null) {
         wings.handle(player, payload);
      }
   }

   private void handle(ServerPlayer player, SpiderSwingPayload payload) {
      UUID id = player.getUUID();
      if (payload.action() == 6) {
         if (this.gliders.containsKey(id)) {
            return;
         }

         PowerData data = PowerService.data(player);
         float cost = this.billed(3.0F);
         if (!StaminaSystem.canAfford(data, cost)) {
            PacketDistributor.sendToPlayer(player, SpiderForceStopPayload.INSTANCE, new CustomPacketPayload[0]);
            this.fail(player, "awakened.feedback.no_stamina", new Object[0]);
            return;
         }

         if (!data.infiniteStamina()) {
            data.setStamina(data.stamina() - cost);
         }

         data.setLastSpendTime(player.serverLevel().getGameTime());
         this.gliders.put(id, Boolean.TRUE);
         MasterySystem.awardUse(player);
         PowerService.playAnimation(player, "web_glide");
         setGlideState(player, true);
         PowerService.sync(player);
      } else if (payload.action() == 7) {
         setGlideState(player, false);
         if (this.gliders.remove(id) != null) {
            PowerService.stopAnimation(player);
            PowerService.sync(player);
         }
      }
   }

   @SubscribeEvent
   static void onFireworkBoost(RightClickItem event) {
      if (!event.getLevel().isClientSide()) {
         if (!(event.getEntity() instanceof ServerPlayer player) || !isGliding(player)) {
            return;
         }

         if (event.getItemStack().is(Items.FIREWORK_ROCKET)) {
            event.setCanceled(true);
            if (!player.getAbilities().instabuild) {
               event.getItemStack().shrink(1);
            }

            player.serverLevel()
               .playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0F, 1.0F);
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      if (this.gliders.containsKey(player.getUUID())) {
         player.resetFallDistance();
         PowerData data = PowerService.data(player);
         if (!data.infiniteStamina()) {
            float drain = this.billed(0.25F);
            if (data.stamina() < drain) {
               this.endServerSide(player);
               return;
            }

            StaminaSystem.drainSustained(data, player.serverLevel().getGameTime(), drain);
         }

         if (player.tickCount % 3 == 0) {
            PowerService.sync(player);
         }
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      if (this.gliders.containsKey(player.getUUID())) {
         PowerService.stopAnimation(player);
         PacketDistributor.sendToPlayer(player, SpiderForceStopPayload.INSTANCE, new CustomPacketPayload[0]);
      }

      setGlideState(player, false);
      super.clearPlayer(player);
   }

   private void endServerSide(ServerPlayer player) {
      setGlideState(player, false);
      if (this.gliders.remove(player.getUUID()) != null) {
         PowerService.stopAnimation(player);
         PacketDistributor.sendToPlayer(player, SpiderForceStopPayload.INSTANCE, new CustomPacketPayload[0]);
         SpiderFx.twang(player.serverLevel(), player.getEyePosition());
         PowerService.sync(player);
      }
   }
}
