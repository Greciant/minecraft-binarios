package com.devilfruits.fruits.yomi_yomi.client;

import com.devilfruits.client.fx.ClientFxRegistry;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.camera.CameraDirector;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.fruits.yomi_yomi.YomiEntities;
import com.devilfruits.fruits.yomi_yomi.YomiFx;
import com.devilfruits.fruits.yomi_yomi.client.vfx.Frost;
import com.devilfruits.fruits.yomi_yomi.client.vfx.SoulColdCinematic;
import com.devilfruits.fruits.yomi_yomi.client.vfx.SoulFlames;
import com.devilfruits.fruits.yomi_yomi.client.vfx.SoulTetherCinematic;
import com.devilfruits.fruits.yomi_yomi.client.vfx.YomiLook;
import com.devilfruits.fruits.yomi_yomi.entity.SoulShell;
import com.devilfruits.fruits.yomi_yomi.soul.SoulProjection;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

public final class SoulClient {
   static void init(IEventBus modBus) {
      NeoForge.EVENT_BUS.addListener(e -> {
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null && !Minecraft.getInstance().isPaused()) {
            tick(level);
         }
      });
      modBus.addListener(e -> e.registerEntityRenderer(YomiEntities.SOUL_SHELL.get(), SoulShellRenderer::new));
      ClientFxRegistry.register(YomiFx.PROJECT, (level, entity, p) -> {
         if (entity != null) {
            SoulRenderer.play(entity.getId(), "emerge");
            SoulFlames.burst(RandomSource.create(p.seed()), p.pos().add(0.0, 1.0, 0.0), 1.0F);
            if (entity instanceof Player soul && tether(soul) == null) {
               VfxDirector.spawn(new SoulTetherCinematic(soul, p.extra(), p.pos(), p.seed()));
            }
         }
      });
      ClientFxRegistry.register(YomiFx.SOUL_RECOIL, (level, entity, p) -> {
         if (entity != null) {
            SoulRenderer.play(entity.getId(), "recoil");
            SoulRenderer.yank(entity.getId(), p.dir().add(0.0, 0.1, 0.0), p.extra());
         }
      });
      ClientFxRegistry.register(YomiFx.SOUL_SNAP, (level, entity, p) -> {
         RandomSource r = RandomSource.create(p.seed());
         Vec3 chest = p.pos().add(0.0, 1.0, 0.0);
         SoulFlames.burst(r, chest, 1.4F);
         VfxParticles.emit(YomiLook.SOUL_FLASH, chest.x, chest.y, chest.z, 0.0, 0.0, 0.0).size(1.4F);
         SoulFlames.tongues(r, p.pos(), 0.4, 6, 0.6F);
         if (entity == Minecraft.getInstance().player) {
            CameraDirector.shake(0.3F, 0.08F, 1.6F, 12.0F);
            CameraDirector.fovPunch(0.08F);
         }
      });
      ClientFxRegistry.register(YomiFx.SOUL_DASH, (level, entity, p) -> {
         if (entity != null) {
            SoulRenderer.play(entity.getId(), "dash");
         }
      });
      ClientFxRegistry.register(YomiFx.SOUL_DASH_SURGE, (level, entity, p) -> {
         RandomSource r = RandomSource.create(p.seed());
         Vec3 a = p.pos().add(0.0, 1.15, 0.0);
         Vec3 b = p.dir().add(0.0, 1.15, 0.0);
         SoulFlames.wispTrail(r, a, b, 3.0F, 1.1F);
         Vec3 d = b.subtract(a);

         for (int i = 0; i < 14; i++) {
            Vec3 q = a.lerp(b, r.nextDouble());
            VfxParticles.emit(YomiLook.STREAK, q.x, q.y, q.z, d.x * 0.03, d.y * 0.03, d.z * 0.03);
         }

         Frost.filings(r, b, d.normalize(), 6);
         if (entity != null && VfxWorld.local(entity.getId())) {
            CameraDirector.fovPunch(0.12F);
         }
      });
      ClientFxRegistry.register(YomiFx.SOUL_CHILL, (level, entity, p) -> {
         RandomSource r = RandomSource.create(p.seed() ^ (long)(entity == null ? 0 : entity.getId()));
         VfxParticles.emit(YomiLook.COLD_FLASH, p.pos().x, p.pos().y, p.pos().z, 0.0, 0.0, 0.0).size(0.8F);
         Frost.chips(r, p.pos(), Vec3.ZERO, 5, 0.18);
      });
      ClientFxRegistry.register(YomiFx.COLD_SOUL, (level, entity, p) -> {
         if (entity != null) {
            SoulRenderer.play(entity.getId(), "laugh");
            SoulRenderer.cold(entity.getId(), SoulRenderer.MODEL.length("laugh") + 4.0F);
            VfxDirector.spawn(SoulColdCinematic.flare(entity, p.seed()));
         }
      });
      ClientFxRegistry.register(YomiFx.COLD_SOUL_PULSE, (level, entity, p) -> {
         if (entity != null) {
            VfxDirector.spawn(SoulColdCinematic.wave(level, entity, p.pos(), (double)p.power(), p.seed() + (long)p.extra()));
         }
      });
      ClientFxRegistry.register(YomiFx.SECOND_WIND, (level, entity, p) -> {
         if (entity != null) {
            VfxDirector.spawn(SoulColdCinematic.secondWind(level, entity, (double)p.power(), p.seed()));
         }
      });
   }

   @Nullable
   private static SoulTetherCinematic tether(Player soul) {
      return VfxDirector.find(SoulTetherCinematic.class, k -> !k.isDone() && k.soul() == soul);
   }

   static void tick(ClientLevel level) {
      for (Player player : level.players()) {
         if (SoulProjection.active(player) && tether(player) == null) {
            SoulShell shell = null;

            for (Entity e : level.entitiesForRendering()) {
               if (e instanceof SoulShell s && player.getUUID().equals(s.ownerId())) {
                  shell = s;
                  break;
               }
            }

            VfxDirector.spawn(
               new SoulTetherCinematic(player, shell != null ? shell.getId() : -1, shell != null ? shell.position() : player.position(), (long)player.getId())
            );
         }
      }
   }

   static void clear() {
   }

   private SoulClient() {
   }
}
