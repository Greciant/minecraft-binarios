package com.devilfruits.fruits.kage_kage.client;

import com.devilfruits.client.fx.ClientFxRegistry;
import com.devilfruits.client.vfx.VfxDirector;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.client.vfx.particle.VfxParticles;
import com.devilfruits.core.network.FxPayload;
import com.devilfruits.fruits.kage_kage.KageFx;
import com.devilfruits.fruits.kage_kage.client.vfx.AegisBlockCinematic;
import com.devilfruits.fruits.kage_kage.client.vfx.AegisCinematic;
import com.devilfruits.fruits.kage_kage.client.vfx.CollectCinematic;
import com.devilfruits.fruits.kage_kage.client.vfx.DoppelPoolCinematic;
import com.devilfruits.fruits.kage_kage.client.vfx.HandCinematic;
import com.devilfruits.fruits.kage_kage.client.vfx.InjectCinematic;
import com.devilfruits.fruits.kage_kage.client.vfx.KageLook;
import com.devilfruits.fruits.kage_kage.client.vfx.SpearCinematic;
import com.devilfruits.fruits.kage_kage.client.vfx.SwallowedView;
import com.devilfruits.fruits.kage_kage.client.vfx.SwarmCinematic;
import com.devilfruits.fruits.kage_kage.client.vfx.TrapCinematic;
import com.devilfruits.fruits.kage_kage.entity.Doppelman;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

final class KageClientFx {
   static void register() {
      ClientFxRegistry.register(KageFx.AEGIS_ON, KageClientFx::aegisOn);
      ClientFxRegistry.register(KageFx.AEGIS_OFF, KageClientFx::aegisOff);
      ClientFxRegistry.register(KageFx.AEGIS_BLOCK, KageClientFx::aegisBlock);
      ClientFxRegistry.register(KageFx.SWARM, KageClientFx::swarm);
      ClientFxRegistry.register(KageFx.SPEAR, KageClientFx::spear);
      ClientFxRegistry.register(KageFx.TRAP, KageClientFx::trap);
      ClientFxRegistry.register(KageFx.TRAP_SWALLOW, KageClientFx::trapSwallow);
      ClientFxRegistry.register(KageFx.HAND, KageClientFx::hand);
      ClientFxRegistry.register(KageFx.DOPPEL_POOL, KageClientFx::doppelPool);
      ClientFxRegistry.register(KageFx.DOPPEL_ORDER, KageClientFx::doppelOrder);
      ClientFxRegistry.register(KageFx.COLLECT, KageClientFx::collect);
      ClientFxRegistry.register(KageFx.ZOMBIE_INJECT, KageClientFx::zombieInject);
      ClientFxRegistry.register(KageFx.COLLECT_SNIP, KageClientFx::collectSnip);
      ClientFxRegistry.register(KageFx.COLLECT_END, KageClientFx::collectEnd);
   }

   private static void aegisOn(ClientLevel level, @Nullable Entity e, FxPayload p) {
      if (e instanceof Player user && AegisCinematic.of(user) == null) {
         VfxDirector.spawn(new AegisCinematic(user, p.seed()));
      }
   }

   private static void aegisOff(ClientLevel level, @Nullable Entity e, FxPayload p) {
      if (e instanceof Player user && p.extra() == 0) {
         AegisCinematic aegis = AegisCinematic.of(user);
         if (aegis != null) {
            aegis.sink();
         }
      }
   }

   private static void aegisBlock(ClientLevel level, @Nullable Entity e, FxPayload p) {
      if (e instanceof Player user) {
         VfxDirector.spawn(new AegisBlockCinematic(user, p.pos(), p.dir(), p.seed()));
      }
   }

   private static void swarm(ClientLevel level, @Nullable Entity e, FxPayload p) {
      if (e instanceof Player user) {
         VfxDirector.spawn(new SwarmCinematic(user, p.extra(), p.pos(), Math.round(p.power()), p.seed()));
      }
   }

   private static void spear(ClientLevel level, @Nullable Entity e, FxPayload p) {
      if (e instanceof Player user) {
         VfxDirector.spawn(new SpearCinematic(user, p.extra(), p.pos(), (int)p.dir().x, (int)p.dir().y, Math.round(p.power()), p.seed()));
      }
   }

   private static void trap(ClientLevel level, @Nullable Entity e, FxPayload p) {
      if (e instanceof Player user) {
         VfxDirector.spawn(new TrapCinematic(user, p.pos(), (int)p.dir().x, (int)p.dir().y, (float)p.dir().z, p.extra(), p.seed()));
      }
   }

   private static void trapSwallow(ClientLevel level, @Nullable Entity e, FxPayload p) {
      if (e != null) {
         TrapCinematic trap = TrapCinematic.at(p.pos());
         if (trap != null) {
            trap.swallow(e);
         }

         if (e == Minecraft.getInstance().player) {
            VfxDirector.spawn(new SwallowedView((Player)e, p.extra(), p.seed()));
         }
      }
   }

   private static void hand(ClientLevel level, @Nullable Entity e, FxPayload p) {
      if (e instanceof Player user) {
         VfxDirector.spawn(new HandCinematic(user, p.pos(), p.dir(), Math.round(p.power()), p.extra(), p.seed()));
      }
   }

   private static void doppelPool(ClientLevel level, @Nullable Entity e, FxPayload p) {
      VfxDirector.spawn(new DoppelPoolCinematic(level, p.pos(), p.power(), p.extra(), p.seed()));
   }

   private static void doppelOrder(ClientLevel level, @Nullable Entity e, FxPayload p) {
      level.addParticle(ParticleTypes.WITCH, p.pos().x, p.pos().y + 0.6, p.pos().z, 0.0, 0.05, 0.0);
      if (!VfxSettings.legacy()) {
         VfxParticles.emit(KageLook.FLASH, p.pos().x, p.pos().y, p.pos().z, 0.0, 0.0, 0.0).size(0.9F).tint(14708991).alpha(0.7F);
      }

      KageLook.sound(level, p.pos(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 0.6F);
      if (level.getEntity(p.extra()) instanceof Doppelman d) {
         KageLook.sound(level, d.position(), SoundEvents.ENDERMAN_AMBIENT, 0.6F, 0.5F);
      }
   }

   private static void zombieInject(ClientLevel level, @Nullable Entity e, FxPayload p) {
      if (e instanceof Player user) {
         VfxDirector.spawn(new InjectCinematic(user, p.pos(), p.seed()));
      }
   }

   private static void collect(ClientLevel level, @Nullable Entity e, FxPayload p) {
      if (e instanceof Player user && CollectCinematic.of(user, p.extra()) == null) {
         VfxDirector.spawn(new CollectCinematic(user, p.extra(), p.pos(), p.seed()));
      }
   }

   private static void collectSnip(ClientLevel level, @Nullable Entity e, FxPayload p) {
      if (e instanceof Player user) {
         CollectCinematic cut = CollectCinematic.of(user, p.extra());
         if (cut == null) {
            cut = VfxDirector.spawn(new CollectCinematic(user, p.extra(), p.pos(), p.seed()));
         }

         cut.snip();
      }
   }

   private static void collectEnd(ClientLevel level, @Nullable Entity e, FxPayload p) {
      if (e instanceof Player user) {
         CollectCinematic cut = CollectCinematic.of(user, p.extra());
         if (cut != null) {
            cut.cancel();
         }
      }
   }

   private KageClientFx() {
   }
}
