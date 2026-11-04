package com.devilfruits.fruits.horo_horo.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.animation.AnimationController;
import com.devilfruits.client.model.geo.GeoOverlays;
import com.devilfruits.client.model.geo.GeoPose;
import com.devilfruits.client.model.geo.GeoRenderer;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.fruits.horo_horo.HoroHoro;
import com.devilfruits.fruits.horo_horo.client.vfx.HoroLook;
import com.devilfruits.fruits.horo_horo.client.vfx.HoroRender;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers;
import net.neoforged.neoforge.client.event.RenderLivingEvent.Post;
import net.neoforged.neoforge.client.event.RenderLivingEvent.Pre;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Quaternionf;

public final class NegativeLook {
   private static final ResourceLocation WASH = DevilFruits.id("textures/entity/horo_horo/negative_wash.png");
   private static final ResourceLocation POSE = HoroHoro.id("negative_pose");
   private static final IntSet KNEELING = new IntOpenHashSet();
   private static final IntSet SLUMPED = new IntOpenHashSet();

   static void init(IEventBus modBus) {
      modBus.addListener(NegativeLook::addLayers);
      modBus.addListener(e -> e.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, HoroHoro.id("negative_vignette"), NegativeLook::vignette));
      GeoOverlays.register(NegativeLook::geo);
      NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, NegativeLook::onRenderPre);
      NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, NegativeLook::onRenderPost);
   }

   private static int wash(LivingEntity e, float partial) {
      float k = HoroRender.gloom(e, partial);
      return k <= 0.01F ? 0 : Math.round(k * 170.0F) << 24 | 10263220;
   }

   private static void geo(LivingEntity e, GeoRenderer geo, GeoPose pose, MultiBufferSource buffers, int light, int overlay) {
      int c = wash(e, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
      if (c != 0) {
         geo.render(buffers.getBuffer(RenderType.entityTranslucent(WASH)), pose, light, overlay, c);
      }
   }

   private static void addLayers(AddLayers event) {
      for (Model skin : event.getSkins()) {
         if (event.getSkin(skin) instanceof LivingEntityRenderer r) {
            r.addLayer(new NegativeLook.Layer(r));
         }
      }

      for (EntityType<?> type : event.getEntityTypes()) {
         if (event.getRenderer(type) instanceof LivingEntityRenderer r) {
            r.addLayer(new NegativeLook.Layer(r));
         }
      }
   }

   private static void onRenderPre(Pre<?, ?> event) {
      LivingEntity e = event.getEntity();
      if (!(e instanceof Player)) {
         float partial = event.getPartialTick();
         float k = HoroRender.gloom(e, partial);
         if (!(k <= 0.01F)) {
            float t = (float)e.tickCount + partial;
            float bow = (26.0F + 3.0F * Mth.sin(t * 0.21F)) * k * (float) (Math.PI / 180.0);
            float yaw = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot) * (float) (Math.PI / 180.0);
            float fx = -Mth.sin(yaw);
            float fz = Mth.cos(yaw);
            PoseStack ps = event.getPoseStack();
            ps.pushPose();
            ps.translate(0.0F, -0.16F * e.getBbHeight() * k, 0.0F);
            ps.mulPose(new Quaternionf().rotateAxis(-bow, -fz, 0.0F, fx));
            SLUMPED.add(e.getId());
         }
      }
   }

   private static void onRenderPost(Post<?, ?> event) {
      if (SLUMPED.remove(event.getEntity().getId())) {
         event.getPoseStack().popPose();
      }
   }

   static void tick(ClientLevel level) {
      long time = level.getGameTime();

      for (Entity e : level.entitiesForRendering()) {
         if (e instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)e;
            float k = HoroRender.gloom(living, 0.0F);
            if (e instanceof AbstractClientPlayer) {
               AbstractClientPlayer p = (AbstractClientPlayer)e;
               boolean kneeling = KNEELING.contains(p.getId());
               if (k > 0.0F && !kneeling) {
                  AnimationController.play(p, POSE);
                  KNEELING.add(p.getId());
               } else if (k <= 0.0F && kneeling) {
                  AnimationController.stop(p);
                  KNEELING.remove(p.getId());
               }
            }

            if (k > 0.3F && !e.isInvisible() && (time + (long)e.getId()) % 3L == 0L) {
               float w = Math.max(0.95F, e.getBbWidth() * 1.7F) * 0.38F;
               double x = e.getX() + (level.random.nextDouble() * 2.0 - 1.0) * (double)w;
               double z = e.getZ() + (level.random.nextDouble() * 2.0 - 1.0) * (double)w * 0.5;
               double y = e.getY() + (double)e.getBbHeight() * 0.88 + 0.3;
               if (VfxSettings.legacy()) {
                  level.addParticle(ParticleTypes.FALLING_WATER, x, y, z, 0.0, 0.0, 0.0);
               } else {
                  VfxParticles.emit(HoroLook.DROP, x, y, z, 0.0, -0.02, 0.0).unflipped();
               }
            }
         }
      }

      KNEELING.removeIf(id -> level.getEntity(id) == null);
   }

   static void clear() {
      KNEELING.clear();
      SLUMPED.clear();
   }

   private static void vignette(GuiGraphics g, DeltaTracker delta) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.options.getCameraType().isFirstPerson()) {
         float k = HoroRender.gloom(mc.player, delta.getGameTimeDeltaPartialTick(false));
         if (!(k <= 0.01F)) {
            int w = g.guiWidth();
            int h = g.guiHeight();
            g.fill(0, 0, w, h, Math.round(k * 50.0F) << 24 | 2893662);
            g.setColor(1.0F, 1.0F, 1.0F, k);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            g.blit(VfxSprites.HORO_VIGNETTE, 0, 0, 0, 0.0F, 0.0F, w, h, w, h);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
         }
      }
   }

   private NegativeLook() {
   }

   static final class Layer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
      Layer(RenderLayerParent<T, M> parent) {
         super(parent);
      }

      public void render(
         PoseStack pose,
         MultiBufferSource buffers,
         int light,
         T e,
         float limbSwing,
         float limbSwingAmount,
         float partial,
         float age,
         float netHeadYaw,
         float headPitch
      ) {
         int c = e.isInvisible() ? 0 : NegativeLook.wash(e, partial);
         if (c != 0) {
            this.getParentModel()
               .renderToBuffer(
                  pose, buffers.getBuffer(RenderType.entityTranslucent(NegativeLook.WASH)), light, LivingEntityRenderer.getOverlayCoords(e, 0.0F), c
               );
         }
      }
   }
}
