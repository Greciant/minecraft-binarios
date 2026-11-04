package com.awakenedmod.awakened.content.scaler;

import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class JarControl {
   private static final int ESCAPE_PRESSES = 12;
   private static final double CARRY_HEIGHT_FRAC = 0.45;
   private static final int INVIS_REFRESH_TICKS = 12;
   private static final float BURST_SCALE_MARGIN = 0.01F;
   private static final Map<UUID, JarControl.Jarred> JARRED = new ConcurrentHashMap<>();

   private JarControl() {
   }

   public static boolean isJarred(LivingEntity entity) {
      return entity instanceof Player && JARRED.containsKey(entity.getUUID());
   }

   public static void capture(ServerPlayer carrier, ServerPlayer victim) {
      HitchControl.detach(victim, false);
      SeizeControl.clearPlayer(victim);
      RayEffects.pause(victim.getUUID());
      JARRED.put(victim.getUUID(), new JarControl.Jarred(carrier.getUUID()));
      RayEffects.syncRestraint(victim);
      victim.displayClientMessage(Component.translatable("awakened.scaler.jarred_hint"), true);
   }

   public static void free(ServerPlayer victim, ServerPlayer carrier) {
      if (JARRED.remove(victim.getUUID()) != null) {
         victim.removeEffect(MobEffects.INVISIBILITY);
         victim.setDeltaMovement(Vec3.ZERO);
         victim.fallDistance = 0.0F;
         RayEffects.resume(victim, 100);
         RayEffects.syncRestraint(victim);
         if (carrier != null) {
            clearJarFor(carrier, victim.getUUID());
            carrier.displayClientMessage(Component.translatable("awakened.scaler.jar_escaped"), true);
         }
      }
   }

   public static void tick(ServerPlayer ticking) {
      if (!JARRED.isEmpty()) {
         for (Entry<UUID, JarControl.Jarred> entry : JARRED.entrySet()) {
            if (entry.getValue().carrier.equals(ticking.getUUID())) {
               tickCarried(ticking, entry.getKey());
            }
         }

         JarControl.Jarred mine = JARRED.get(ticking.getUUID());
         if (mine != null) {
            ServerPlayer carrier = player(ticking, mine.carrier);
            if (carrier == null || !carrier.isAlive()) {
               free(ticking, carrier);
               ticking.displayClientMessage(Component.translatable("awakened.scaler.broke_free"), true);
            }
         }
      }
   }

   private static void tickCarried(ServerPlayer carrier, UUID victimId) {
      ServerPlayer victim = player(carrier, victimId);
      if (victim == null) {
         JARRED.remove(victimId);
         clearJarFor(carrier, victimId);
      } else {
         boolean valid = victim.isAlive()
            && carrier.isAlive()
            && victim.level() == carrier.level()
            && victim.getScale() <= 0.28F
            && findJar(carrier, victimId) != null;
         if (!valid) {
            free(victim, carrier);
            victim.displayClientMessage(Component.translatable("awakened.scaler.broke_free"), true);
         } else {
            Vec3 spot = carrier.position().add(0.0, (double)carrier.getBbHeight() * 0.45, 0.0);
            SeizeControl.pinTo(victim, spot);
            victim.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 12, 0, false, false));
         }
      }
   }

   public static void struggle(ServerPlayer victim) {
      JarControl.Jarred jarred = JARRED.get(victim.getUUID());
      if (jarred != null) {
         if (++jarred.struggles < 12) {
            Fx.burst(Dmg.serverLevel(victim), ParticleTypes.CRIT, victim.position().add(0.0, (double)victim.getBbHeight() * 0.5, 0.0), 2, 0.2, 0.05);
         } else {
            ServerPlayer carrier = player(victim, jarred.carrier);
            free(victim, carrier);
            victim.displayClientMessage(Component.translatable("awakened.scaler.broke_free"), true);
            Fx.sound(Dmg.serverLevel(victim), victim.position(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.7F, 1.4F);
            if (carrier != null) {
               Vec3 out = victim.position().subtract(carrier.position());
               out = out.lengthSqr() < 1.0E-4 ? carrier.getLookAngle() : out.normalize();
               victim.setDeltaMovement(out.x * 0.4, 0.4, out.z * 0.4);
               victim.hurtMarked = true;
            }
         }
      }
   }

   private static ItemStack findJar(ServerPlayer carrier, UUID victimId) {
      Inventory inv = carrier.getInventory();

      for (int i = 0; i < inv.getContainerSize(); i++) {
         ItemStack stack = inv.getItem(i);
         if (stack.getItem() instanceof ContainmentJarItem && victimId.equals(ContainmentJarItem.jarredPlayerId(stack))) {
            return stack;
         }
      }

      return null;
   }

   private static void clearJarFor(ServerPlayer carrier, UUID victimId) {
      ItemStack jar = findJar(carrier, victimId);
      if (jar != null) {
         ContainmentJarItem.empty(jar);
      }
   }

   private static ServerPlayer player(ServerPlayer context, UUID id) {
      return context.level().getServer() == null ? null : context.level().getServer().getPlayerList().getPlayer(id);
   }

   private static final class Jarred {
      private final UUID carrier;
      private int struggles;

      private Jarred(UUID carrier) {
         this.carrier = carrier;
      }
   }
}
