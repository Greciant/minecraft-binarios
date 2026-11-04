package com.devilfruits.fruits.bomu_bomu.client;

import com.devilfruits.client.fx.ChargeVisuals;
import com.devilfruits.client.vfx.VfxClock;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.fx.VfxWorld;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.core.network.ChargePayload;
import com.devilfruits.fruits.bomu_bomu.BomuBomu;
import com.devilfruits.fruits.bomu_bomu.client.vfx.AirShotCinematic;
import com.devilfruits.fruits.bomu_bomu.client.vfx.ArmedFistCinematic;
import com.devilfruits.fruits.bomu_bomu.client.vfx.BomuLook;
import com.devilfruits.fruits.bomu_bomu.client.vfx.SelfDestructChargeCinematic;
import com.devilfruits.fruits.bomu_bomu.entity.BombBullet;
import com.devilfruits.fruits.bomu_bomu.entity.BoogerBomb;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeFov;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

final class BomuClientEvents {
   private static final Set<Entity> PROJECTILES = Collections.newSetFromMap(new WeakHashMap<>());
   @Nullable
   private static SelfDestructChargeCinematic localCharge;
   private static boolean packLoaded;
   static final ChargeVisuals.ChargeVisual SELF_DESTRUCT = new ChargeVisuals.ChargeVisual() {
      @Override
      public void start(ClientLevel level, Entity entity, ChargePayload payload) {
         if (entity instanceof Player player) {
            if (VfxSettings.legacy()) {
               for (int i = 0; i < 6; i++) {
                  level.addParticle(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 0.0, 0.05, 0.0);
               }
            } else {
               SelfDestructChargeCinematic c = VfxDirector.spawn(new SelfDestructChargeCinematic(player, payload.duration(), payload.seed()));
               if (VfxWorld.local(player.getId())) {
                  BomuClientEvents.localCharge = c;
               }
            }

            if (VfxWorld.local(player.getId())) {
               BomuClientEvents.chargeStart = level.getGameTime();
               BomuClientEvents.chargeDuration = Math.max(1.0F, payload.duration());
            }
         }
      }

      @Override
      public void stop(ClientLevel level, Entity entity, boolean released) {
         SelfDestructChargeCinematic c = VfxDirector.find(SelfDestructChargeCinematic.class, x -> x.player() == entity);
         if (c != null) {
            c.finish();
         }

         if (VfxWorld.local(entity.getId())) {
            BomuClientEvents.localCharge = null;
            BomuClientEvents.chargeStart = -1L;
         }
      }
   };
   private static long chargeStart = -1L;
   private static float chargeDuration = 1.0F;

   static float selfDestructProgress(float partial) {
      Minecraft mc = Minecraft.getInstance();
      return chargeStart >= 0L && mc.level != null ? Mth.clamp(((float)(mc.level.getGameTime() - chargeStart) + partial) / chargeDuration, 0.0F, 1.0F) : -1.0F;
   }

   static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      ClientLevel level = mc.level;
      if (level != null && !mc.isPaused()) {
         if (!packLoaded && mc.player != null && BomuBomu.isUser(mc.player) && !VfxSettings.legacy()) {
            packLoaded = true;
            RenderSystem.recordRenderCall(() -> VfxSprites.id(VfxSprites.packTexture()));
         }

         for (Player p : level.players()) {
            if (FormFlags.has(p, BomuBomu.ID, BomuBomu.ARMED)) {
               if (VfxSettings.legacy()) {
                  if (p.tickCount % 3 == 0) {
                     Vec3 f = BomuLook.fist(p, 1.0F, true, VfxWorld.firstPersonCaster(p.getId()));
                     level.addParticle(ParticleTypes.SMALL_FLAME, f.x, f.y, f.z, 0.0, 0.01, 0.0);
                  }
               } else if (VfxDirector.find(ArmedFistCinematic.class, c -> c.anchor().entityId() == p.getId()) == null) {
                  VfxDirector.spawn(new ArmedFistCinematic(p));
               }
            }
         }

         if (!VfxClock.frozen()) {
            Iterator<Entity> it = PROJECTILES.iterator();

            while (it.hasNext()) {
               Entity e = it.next();
               if (e.isRemoved() || e.level() != level) {
                  it.remove();
               } else if (e instanceof BoogerBomb) {
                  BoogerBomb b = (BoogerBomb)e;
                  if (!b.stuck() && level.random.nextFloat() < 0.6F) {
                     if (VfxSettings.legacy()) {
                        level.addParticle(ParticleTypes.ITEM_SLIME, e.getX(), e.getY(), e.getZ(), 0.0, 0.0, 0.0);
                     } else {
                        VfxParticles.emit(BomuLook.DROP, e.getX(), e.getY() - 0.05, e.getZ(), e.getDeltaMovement().x * 0.2, -0.02, e.getDeltaMovement().z * 0.2)
                           .tint(12111936)
                           .alpha(0.85F)
                           .size(0.8F);
                     }
                  }
               } else if (e instanceof BombBullet && VfxSettings.legacy() && level.random.nextFloat() < 0.5F) {
                  level.addParticle(ParticleTypes.WHITE_ASH, e.getX(), e.getY(), e.getZ(), 0.0, 0.0, 0.0);
               }
            }
         }
      }
   }

   static void onJoin(EntityJoinLevelEvent event) {
      if (event.getLevel().isClientSide() && (event.getEntity() instanceof BoogerBomb || event.getEntity() instanceof BombBullet)) {
         PROJECTILES.add(event.getEntity());
         if (event.getEntity() instanceof BombBullet shot && !VfxSettings.legacy()) {
            VfxDirector.spawn(new AirShotCinematic(shot));
         }
      }
   }

   static void onRenderPlayer(Pre event) {
      if (FormFlags.has(event.getEntity(), BomuBomu.ID, BomuBomu.VANISHED)) {
         event.setCanceled(true);
      }
   }

   static void onRenderHand(RenderHandEvent event) {
      Player player = Minecraft.getInstance().player;
      if (player != null && FormFlags.has(player, BomuBomu.ID, BomuBomu.VANISHED)) {
         event.setCanceled(true);
      }
   }

   static void onFov(ComputeFov event) {
      float p = selfDestructProgress((float)event.getPartialTick());
      if (p > 0.0F) {
         event.setFOV(event.getFOV() * (1.0 - 0.14 * (double)p * (double)p));
      }
   }

   static void onLogout() {
      packLoaded = false;
      chargeStart = -1L;
      localCharge = null;
      PROJECTILES.clear();
   }

   private BomuClientEvents() {
   }
}
