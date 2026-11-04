package com.devilfruits.client.render;

import com.devilfruits.core.form.FruitForm;
import com.devilfruits.mixin.client.EntityRendererInvoker;
import com.devilfruits.registry.DFAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(
   modid = "devilfruits",
   value = {Dist.CLIENT}
)
public final class FormRenderers {
   private static final int DEATH_VISUAL_TICKS = 40;
   private static final Map<ResourceLocation, FormRenderers.FormRenderer> RENDERERS = new HashMap<>();
   private static final Map<Integer, FormRenderers.DeathVisual> DEATH_VISUALS = new HashMap<>();

   public static void register(ResourceLocation id, FormRenderers.FormRenderer renderer) {
      if (RENDERERS.putIfAbsent(id, renderer) != null) {
         throw new IllegalStateException("Duplicate form renderer " + id);
      }
   }

   @Nullable
   public static FormRenderers.FormRenderer get(@Nullable ResourceLocation id) {
      return id == null ? null : RENDERERS.get(id);
   }

   public static void keepDuringDeath(Player player, FruitForm form) {
      if (form.renderer() != null && player.level() != null) {
         DEATH_VISUALS.put(player.getId(), new FormRenderers.DeathVisual(form, player.level().getGameTime()));
      }
   }

   @Nullable
   public static FruitForm visualForm(Player player) {
      FormRenderers.DeathVisual death = DEATH_VISUALS.get(player.getId());
      if (death != null) {
         return death.form;
      } else {
         return player.hasData(DFAttachments.PLAYER_DATA) ? DFAttachments.data(player).form() : null;
      }
   }

   public static boolean isDeathVisual(Player player) {
      return DEATH_VISUALS.containsKey(player.getId());
   }

   @SubscribeEvent
   public static void onClientTick(Post event) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null && !DEATH_VISUALS.isEmpty()) {
         Iterator<Entry<Integer, FormRenderers.DeathVisual>> it = DEATH_VISUALS.entrySet().iterator();

         while (it.hasNext()) {
            Entry<Integer, FormRenderers.DeathVisual> entry = it.next();
            Entity entity = level.getEntity(entry.getKey());
            long age = level.getGameTime() - entry.getValue().since;
            if (entity == null || entity.isRemoved() || age > 40L || age > 5L && entity.isAlive()) {
               it.remove();
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLogout(LoggingOut event) {
      DEATH_VISUALS.clear();
   }

   @SubscribeEvent(
      priority = EventPriority.HIGH
   )
   public static void onRenderPlayer(Pre event) {
      if (event.getEntity() instanceof AbstractClientPlayer player) {
         FruitForm form = visualForm(player);
         FormRenderers.FormRenderer renderer = form == null ? null : get(form.renderer());
         if (renderer != null
            && renderer.render(player, form, event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight())) {
            event.setCanceled(true);
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGH
   )
   public static void onRenderHand(RenderHandEvent event) {
      AbstractClientPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         FruitForm form = visualForm(player);
         FormRenderers.FormRenderer renderer = form == null ? null : get(form.renderer());
         if (renderer != null
            && renderer.renderFirstPerson(
               player,
               form,
               event.getHand(),
               event.getItemStack(),
               event.getPartialTick(),
               event.getSwingProgress(),
               event.getEquipProgress(),
               event.getPoseStack(),
               event.getMultiBufferSource(),
               event.getPackedLight()
            )) {
            event.setCanceled(true);
         }
      }
   }

   public static void renderNameTag(AbstractClientPlayer player, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      EntityRenderer<? super AbstractClientPlayer> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
      RenderNameTagEvent event = new RenderNameTagEvent(player, player.getDisplayName(), renderer, poseStack, buffers, packedLight, partialTick);
      NeoForge.EVENT_BUS.post(event);
      EntityRendererInvoker invoker = (EntityRendererInvoker)renderer;
      if (event.canRender().isTrue() || event.canRender().isDefault() && invoker.devilfruits$shouldShowName(player)) {
         invoker.devilfruits$renderNameTag(player, event.getContent(), poseStack, buffers, packedLight, partialTick);
      }
   }

   public static boolean renderArm(AbstractClientPlayer player, HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      FruitForm form = visualForm(player);
      FormRenderers.FormRenderer renderer = form == null ? null : get(form.renderer());
      return renderer != null && renderer.renderArm(player, form, arm, poseStack, buffers, packedLight);
   }

   private FormRenderers() {
   }

   private static record DeathVisual(FruitForm form, long since) {
   }

   public interface FormRenderer {
      boolean render(AbstractClientPlayer var1, FruitForm var2, float var3, PoseStack var4, MultiBufferSource var5, int var6);

      default boolean renderFirstPerson(
         AbstractClientPlayer player,
         FruitForm form,
         InteractionHand hand,
         ItemStack stack,
         float partialTick,
         float swingProgress,
         float equipProgress,
         PoseStack poseStack,
         MultiBufferSource buffers,
         int packedLight
      ) {
         return false;
      }

      default boolean renderArm(AbstractClientPlayer player, FruitForm form, HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
         return false;
      }
   }
}
