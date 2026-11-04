package com.devilfruits.fruits.horo_horo.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.animation.AnimationController;
import com.devilfruits.client.render.BodyModels;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.fruits.horo_horo.HoroHoro;
import com.devilfruits.fruits.horo_horo.client.vfx.GhostCinematic;
import com.devilfruits.fruits.horo_horo.client.vfx.HoroModel;
import com.devilfruits.fruits.horo_horo.entity.GhostBody;
import com.devilfruits.fruits.horo_horo.ghost.GhostForm;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderPlayerEvent.Post;
import net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

public final class GhostLook {
   private static final ResourceLocation FLOAT = HoroHoro.id("ghost_float");
   private static final ResourceLocation FLY = HoroHoro.id("ghost_fly");
   private static final ResourceLocation DIVE = HoroHoro.id("ghost_dive");
   private static final int GHOST_ARGB = -2064191745;
   private static final int VEIL_ARGB = 1156902655;
   private static final ResourceLocation VEIL = DevilFruits.id("textures/entity/horo_horo/negative_wash.png");
   private static final IntSet DIVING = new IntOpenHashSet();
   private static final Int2ObjectMap<ItemStack[]> STRIPPED = new Int2ObjectOpenHashMap();
   private static final IntSet SUNK = new IntOpenHashSet();
   private static final String[] ARROWS = new String[]{"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

   static void init(IEventBus modBus) {
      BodyModels.register(GhostLook::body);
      AnimationController.addPoseLayer(new AnimationController.PoseLayer() {
         @Override
         public void poses(Player player, float partial, AnimationController.PoseSink out) {
            if (GhostLook.out(player) && !FormFlags.has(player, HoroHoro.ID, GhostForm.POSSESSING)) {
               float fly = GhostLook.flight(player);
               float t = (float)player.tickCount + partial;
               out.add(GhostLook.FLOAT, t, 1.0F - fly);
               out.add(GhostLook.FLY, t, fly);
            }
         }
      });
      NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, GhostLook::onRenderPre);
      NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, GhostLook::onRenderPost);
      NeoForge.EVENT_BUS.addListener(e -> {
         if (out(Minecraft.getInstance().player)) {
            e.setCanceled(true);
         }
      });
      modBus.addListener(e -> e.registerAbove(VanillaGuiLayers.CROSSHAIR, HoroHoro.id("ghost_hud"), GhostLook::hud));
   }

   static boolean out(@Nullable Player player) {
      return player != null && FormFlags.has(player, HoroHoro.ID, GhostForm.OUT);
   }

   private static float flight(Player player) {
      double v = player.position().distanceTo(new Vec3(player.xo, player.yo, player.zo));
      return Mth.clamp((float)(v - 0.06) / 0.3F, 0.0F, 1.0F);
   }

   private static boolean body(AbstractClientPlayer player, PlayerModel<?> posed, PoseStack pose, MultiBufferSource buffers, int light, int overlay, int color) {
      if (!out(player)) {
         return false;
      } else {
         ResourceLocation skin = player.getSkin().texture();
         float dive = dive(player, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
         float fade = dive < 0.0F ? 1.0F : 1.0F - Mth.clamp((dive - 0.6F) / 0.4F, 0.0F, 1.0F);
         if (!AnimationController.isAnimating(player)) {
            stillLimbs(posed, flight(player));
         }

         if (VfxSettings.legacy()) {
            posed.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(skin)), light, overlay, Math.round(128.0F * fade) << 24 | 16777215);
            return true;
         } else {
            if (buffers instanceof BufferSource source) {
               RenderType depth = HoroModel.Types.depth(skin);
               posed.renderToBuffer(pose, source.getBuffer(depth), light, overlay);
               source.endBatch(depth);
            }

            int a = Math.round(132.0F * fade);
            posed.renderToBuffer(pose, buffers.getBuffer(HoroModel.Types.ghost(skin)), 15728880, overlay, a << 24 | 16183039);
            int v = Math.round(68.0F * fade);
            posed.renderToBuffer(pose, buffers.getBuffer(HoroModel.Types.ghost(VEIL)), 15728880, overlay, v << 24 | 16051967);
            return true;
         }
      }
   }

   private static void stillLimbs(PlayerModel<?> model, float fly) {
      float back = 0.08F + 0.55F * fly;
      model.rightArm.setRotation(back, 0.0F, 0.1F);
      model.leftArm.setRotation(back, 0.0F, -0.1F);
      model.rightSleeve.copyFrom(model.rightArm);
      model.leftSleeve.copyFrom(model.leftArm);
      float drift = 0.12F + 0.35F * fly;
      model.rightLeg.setRotation(drift, 0.0F, 0.04F);
      model.leftLeg.setRotation(drift + 0.06F, 0.0F, -0.04F);
      model.rightPants.copyFrom(model.rightLeg);
      model.leftPants.copyFrom(model.leftLeg);
   }

   private static float dive(Player player, float partial) {
      GhostCinematic c = cinematic(player);
      return c == null ? -1.0F : c.dive(partial);
   }

   @Nullable
   private static GhostCinematic cinematic(Player player) {
      return VfxDirector.find(GhostCinematic.class, k -> !k.isDone() && k.ghost() == player);
   }

   private static void onRenderPre(Pre event) {
      Player player = event.getEntity();
      if (out(player)) {
         Inventory inv = player.getInventory();
         ItemStack[] kept = new ItemStack[6];

         for (int i = 0; i < 4; i++) {
            kept[i] = (ItemStack)inv.armor.get(i);
            inv.armor.set(i, ItemStack.EMPTY);
         }

         kept[4] = (ItemStack)inv.items.get(inv.selected);
         inv.items.set(inv.selected, ItemStack.EMPTY);
         kept[5] = (ItemStack)inv.offhand.get(0);
         inv.offhand.set(0, ItemStack.EMPTY);
         STRIPPED.put(player.getId(), kept);
         GhostCinematic c = cinematic(player);
         float dive = c == null ? -1.0F : c.dive(event.getPartialTick());
         GhostBody body = body(player);
         if (dive > 0.0F && body != null) {
            float k = dive * dive;
            Vec3 at = player.getPosition(event.getPartialTick());
            Vec3 to = body.position().subtract(at).scale((double)k);
            event.getPoseStack().pushPose();
            event.getPoseStack().translate(to.x, to.y, to.z);
            SUNK.add(player.getId());
         }
      }
   }

   private static void onRenderPost(Post event) {
      Player player = event.getEntity();
      if (SUNK.remove(player.getId())) {
         event.getPoseStack().popPose();
      }

      ItemStack[] kept = (ItemStack[])STRIPPED.remove(player.getId());
      if (kept != null) {
         Inventory inv = player.getInventory();

         for (int i = 0; i < 4; i++) {
            inv.armor.set(i, kept[i]);
         }

         inv.items.set(inv.selected, kept[4]);
         inv.offhand.set(0, kept[5]);
      }
   }

   @Nullable
   private static GhostBody body(Player player) {
      if (player.level() instanceof ClientLevel level) {
         for (Entity e : level.entitiesForRendering()) {
            if (e instanceof GhostBody b && player.getUUID().equals(b.ownerId())) {
               return b;
            }
         }
      }

      return null;
   }

   static void tick(ClientLevel level) {
      for (Player player : level.players()) {
         boolean out = out(player);
         if (out && cinematic(player) == null) {
            GhostBody b = body(player);
            VfxDirector.spawn(new GhostCinematic(player, b != null ? b.getId() : -1, b != null ? b.position() : player.position(), (long)player.getId()));
         }

         boolean possessing = out && FormFlags.has(player, HoroHoro.ID, GhostForm.POSSESSING);
         if (possessing && DIVING.add(player.getId())) {
            AnimationController.play(player, DIVE);
         } else if (!possessing) {
            DIVING.remove(player.getId());
         }
      }
   }

   static void onExit(ClientLevel level, @Nullable Entity user, Vec3 bodyPos, int bodyId, long seed) {
      if (user instanceof Player player) {
         GhostCinematic c = cinematic(player);
         if (c == null) {
            c = VfxDirector.spawn(new GhostCinematic(player, bodyId, bodyPos, seed));
         }

         c.onExit(level);
      }
   }

   static void onPossess(@Nullable Entity user, Vec3 bodyPos, int ticks) {
      if (user instanceof Player player) {
         GhostCinematic c = cinematic(player);
         if (c != null) {
            c.onPossess(bodyPos, ticks);
         }
      }
   }

   static void clear() {
      DIVING.clear();
      STRIPPED.clear();
      SUNK.clear();
   }

   private static void hud(GuiGraphics g, DeltaTracker delta) {
      Minecraft mc = Minecraft.getInstance();
      Player player = mc.player;
      if (player != null && !mc.options.hideGui && out(player)) {
         GhostForm.Anchor anchor = (GhostForm.Anchor)player.getData(GhostForm.ANCHOR);
         int cx = g.guiWidth() / 2;
         if (anchor.active() && player.level().dimension() != anchor.dimension()) {
            g.drawCenteredString(mc.font, Component.translatable("hud.devilfruits.horo_horo.body_elsewhere"), cx, 22, 16758741);
         } else {
            GhostBody body = body(player);
            Vec3 home = body != null ? body.position() : (anchor.active() ? anchor.body() : null);
            if (home != null) {
               Vec3 to = home.subtract(player.position());
               double d = to.length();
               double bearing = Math.toDegrees(Math.atan2(-to.x, to.z)) - (double)player.getYRot();
               String arrow = ARROWS[Math.floorMod(Math.round((float)Mth.wrapDegrees(bearing) / 45.0F), 8)];
               double leash = leash();
               int color = d > leash * 0.85 ? 16740264 : 16758741;
               g.drawCenteredString(
                  mc.font,
                  Component.translatable(
                     "hud.devilfruits.horo_horo.body",
                     new Object[]{arrow, String.format(Locale.ROOT, "%d", (int)d), (int)leash, to.y < -2.0 ? "↓" : (to.y > 2.0 ? "↑" : "")}
                  ),
                  cx,
                  22,
                  color
               );
               if (body != null && d <= possessReach() + 1.0 && !FormFlags.has(player, HoroHoro.ID, GhostForm.POSSESSING)) {
                  g.drawCenteredString(mc.font, Component.translatable("hud.devilfruits.horo_horo.possess_hint"), cx, 34, 16183039);
               }
            }
         }
      }
   }

   private static double leash() {
      try {
         return HoroHoro.setting("ghostLeash");
      } catch (IllegalStateException var1) {
         return 64.0;
      }
   }

   private static double possessReach() {
      try {
         return HoroHoro.setting("possessReach");
      } catch (IllegalStateException var1) {
         return 4.0;
      }
   }

   private GhostLook() {
   }
}
