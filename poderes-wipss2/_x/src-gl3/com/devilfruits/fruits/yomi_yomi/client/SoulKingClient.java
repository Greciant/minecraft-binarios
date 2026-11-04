package com.devilfruits.fruits.yomi_yomi.client;

import com.devilfruits.client.animation.AnimationController;
import com.devilfruits.client.fx.ChargeVisuals;
import com.devilfruits.client.fx.ClientFxRegistry;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.core.network.ChargePayload;
import com.devilfruits.fruits.yomi_yomi.YomiFx;
import com.devilfruits.fruits.yomi_yomi.YomiItems;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import com.devilfruits.fruits.yomi_yomi.client.vfx.SoulKingCinematic;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.common.NeoForge;

public final class SoulKingClient {
   private static long stunUntil = -1L;
   private static int stunTicks;
   private static float muffledFrom = Float.NaN;

   static void init(IEventBus modBus) {
      modBus.addListener(e -> e.registerItem(new IClientItemExtensions() {
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
               return SoulGuitarRenderer.get();
            }
         }, new Item[]{(Item)YomiItems.SOUL_GUITAR.get()}));
      modBus.addListener(SoulKingClient::addLayers);
      AnimationController.addPoseLayer(SoulGuitarRenderer.POSE);
      ChargeVisuals.register(YomiYomi.id("form/soul_king"), new ChargeVisuals.ChargeVisual() {
         @Override
         public void start(ClientLevel level, Entity entity, ChargePayload payload) {
            VfxDirector.spawn(SoulKingCinematic.riff(entity, payload.seed()));
         }

         @Override
         public void stop(ClientLevel level, Entity entity, boolean released) {
            if (!released) {
               SoulKingCinematic c = VfxDirector.find(SoulKingCinematic.class, k -> k.entityId() == entity.getId());
               if (c != null) {
                  c.cut();
               }
            }
         }
      });
      ClientFxRegistry.register(YomiFx.KING_SUMMON, (level, entity, p) -> {
         if (entity != null) {
            SoulKingCinematic.summon(entity, p.seed());
         }
      });
      ClientFxRegistry.register(YomiFx.KING_VANISH, (level, entity, p) -> {
         if (entity != null) {
            VfxDirector.spawn(SoulKingCinematic.vanish(entity, p.seed()));
         }
      });
      ClientFxRegistry.register(YomiFx.KING_STRIKE, (level, entity, p) -> {
         if (entity != null) {
            VfxDirector.spawn(SoulKingCinematic.strike(entity, p.dir(), (double)p.power(), Mth.clamp(p.extra(), 0, 2), p.seed()));
         }
      });
      ClientFxRegistry.register(YomiFx.KING_HIT, (level, entity, p) -> {
         if (entity != null) {
            SoulKingCinematic.hit(entity, p.pos(), p.dir(), Mth.clamp(p.extra(), 0, 2), p.seed());
         }
      });
      ClientFxRegistry.register(YomiFx.KING_CRUSH, (level, entity, p) -> {
         if (entity != null) {
            VfxDirector.spawn(SoulKingCinematic.crush(level, entity, p.pos(), p.dir(), (double)p.power(), p.seed()));
         }
      });
      ClientFxRegistry.register(YomiFx.KING_ROAR, (level, entity, p) -> {
         if (entity != null) {
            VfxDirector.spawn(SoulKingCinematic.roar(entity, (double)p.power(), p.seed()));
         }
      });
      ClientFxRegistry.register(YomiFx.KING_ROAR_STUN, (level, entity, p) -> {
         if (entity == Minecraft.getInstance().player) {
            stunTicks = Math.max(1, Math.round(p.power()));
            stunUntil = level.getGameTime() + (long)stunTicks;
            level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.MASTER, 0.6F, 1.9F, false);
         }
      });
      NeoForge.EVENT_BUS.addListener(e -> tick());
      NeoForge.EVENT_BUS.addListener(e -> clear());
   }

   private static void addLayers(AddLayers event) {
      for (Model skin : event.getSkins()) {
         if (event.getSkin(skin) instanceof PlayerRenderer r) {
            r.addLayer(new SoulGuitarRenderer.Layer(r));
         }
      }
   }

   private static void tick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         SoulKingGhost.tick(mc.level);
         SoulRenderer.tickFire(mc.level);
         muffle(mc, stunned(mc));
      }
   }

   private static boolean stunned(Minecraft mc) {
      return mc.level != null && stunUntil > 0L && mc.level.getGameTime() < stunUntil;
   }

   private static void muffle(Minecraft mc, boolean on) {
      if (on && Float.isNaN(muffledFrom)) {
         muffledFrom = mc.options.getSoundSourceVolume(SoundSource.MASTER);
         mc.getSoundManager().updateSourceVolume(SoundSource.MASTER, muffledFrom * 0.25F);
      } else if (!on && !Float.isNaN(muffledFrom)) {
         mc.getSoundManager().updateSourceVolume(SoundSource.MASTER, mc.options.getSoundSourceVolume(SoundSource.MASTER));
         muffledFrom = Float.NaN;
         stunUntil = -1L;
      }
   }

   public static void previewStun(int ticks) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         stunTicks = ticks;
         stunUntil = mc.level.getGameTime() + (long)ticks;
      }
   }

   public static void clear() {
      Minecraft mc = Minecraft.getInstance();
      SoulKingGhost.clear();
      stunUntil = -1L;
      muffle(mc, false);
   }

   static void renderStun(GuiGraphics g, DeltaTracker delta) {
      Minecraft mc = Minecraft.getInstance();
      if (stunned(mc)) {
         float left = (float)(stunUntil - mc.level.getGameTime()) - delta.getGameTimeDeltaPartialTick(false);
         float k = Mth.clamp(left / 20.0F, 0.0F, 1.0F) * Mth.clamp(((float)stunTicks - left) / 3.0F, 0.0F, 1.0F);
         int w = g.guiWidth();
         int h = g.guiHeight();
         int tint = Math.round(k * 70.0F);
         g.fill(0, 0, w, h, tint << 24 | 1997330);
         int line = Math.round(k * 40.0F) << 24 | 666118;

         for (int y = 0; y < h; y += 3) {
            g.fill(0, y, w, y + 1, line);
         }

         long beat = VfxSettings.reduceFlashing() ? 0L : mc.level.getGameTime() / 5L;
         RandomSource r = RandomSource.create(beat * 31L + 7L);

         for (int i = 0; i < 6; i++) {
            int y = r.nextInt(h);
            int bh = 2 + r.nextInt(10);
            int shift = (r.nextInt(2) == 0 ? -1 : 1) * (6 + r.nextInt(30));
            int a = Math.round(k * (float)(60 + r.nextInt(70)));
            g.fill(Math.max(0, shift), y, Math.min(w, w + shift), y + bh, a << 24 | (r.nextInt(3) == 0 ? 13172586 : 8191802));
         }

         int edge = Math.round(k * 120.0F);

         for (int i = 0; i < 12; i++) {
            int a = edge * (12 - i) / 12 / 3;
            g.fill(0, i * 2, w, i * 2 + 2, a << 24 | 12580095);
            g.fill(0, h - i * 2 - 2, w, h - i * 2, a << 24 | 12580095);
         }
      }
   }

   private SoulKingClient() {
   }
}
