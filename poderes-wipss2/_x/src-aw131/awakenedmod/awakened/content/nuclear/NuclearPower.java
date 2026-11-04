package com.awakenedmod.awakened.content.nuclear;

import com.awakenedmod.awakened.Config;
import com.awakenedmod.awakened.content.AwakenedParticles;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.PassiveToggle;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class NuclearPower extends Power {
   private static final String HEAT = "heat";
   private static final float HEAT_PER_DAMAGE = 0.045F;
   private static final int COOL_DELAY_TICKS = 60;
   private static final float COOL_PER_TICK = 0.009F;
   private static final double DETONATE_RADIUS = 5.0;
   private static final float DETONATE_DAMAGE = 12.0F;
   private static final int FALLOUT_TICKS = 160;
   private static final float FALLOUT_DAMAGE = 2.5F;
   private static final String DETONATE_KEY = "NuclearDetonate";
   private static final String FALLOUT_KEY = "NuclearFallout";
   private static final PassiveToggle OVERHEAT = new PassiveToggle(
      "overheat", "awakened.screen.setting_nuclear_overheat", "awakened.screen.setting_nuclear_overheat_desc", true
   );
   private final Map<UUID, Long> lastHit = new ConcurrentHashMap<>();
   private final Map<UUID, Float> lastSyncedHeat = new ConcurrentHashMap<>();
   private final Map<UUID, Long> detonateCooldownUntil = new ConcurrentHashMap<>();

   public NuclearPower() {
      super("nuclear", List.of(new ClusterBurstAbility(), new ExplosiveDashAbility(), new HowitzerAbility(), new ExcavateAbility(), new NukeAbility()));
   }

   @Override
   public int themeColor() {
      return -24546;
   }

   @Override
   public int signatureSlot() {
      return 0;
   }

   @Override
   public List<PassiveToggle> passiveToggles() {
      return List.of(OVERHEAT);
   }

   @Override
   public List<String> extraBalanceKeys() {
      return List.of("NuclearDetonate", "NuclearFallout");
   }

   @Override
   public void onAwaken(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 c = player.position().add(0.0, 1.0, 0.0);
      NuclearFx.boom(level, c, 3.0, NuclearFx.Kind.COMBAT);
      NuclearFx.screenShake(level, c, 24.0, 14);
      Fx.sound(level, c, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2F, 0.7F);
   }

   @Override
   public void passiveTick(ServerPlayer player, PowerData data) {
      super.passiveTick(player, data);
      MinecraftServer server = player.serverLevel().getServer();
      if (server != null) {
         NuclearFx.tickAll(server);
         Fallout.tickAll(server);
      }

      float heat = data.var("heat", 0.0F);
      if (!OVERHEAT.enabled(data)) {
         if (heat != 0.0F) {
            data.setVar("heat", 0.0F);
         }

         this.maybeSyncHeat(player, 0.0F);
      } else {
         long now = player.serverLevel().getGameTime();
         long hitAt = this.lastHit.getOrDefault(player.getUUID(), 0L);
         if (heat > 0.0F && now - hitAt >= 60L) {
            heat = Math.max(0.0F, heat - 0.009F);
            data.setVar("heat", heat);
         }

         this.emitHeatTells(player, heat);
         boolean onCooldown = this.onCooldown(player, now);
         if (heat >= 1.0F && !onCooldown) {
            this.detonate(player, data);
            heat = 0.0F;
         }

         this.maybeSyncHeat(player, heat);
         if (onCooldown && player.tickCount % 20 == 0) {
            PowerService.sync(player);
         }
      }
   }

   @Override
   public void onSuppressed(ServerPlayer player, PowerData data) {
      data.setVar("heat", 0.0F);
      this.lastSyncedHeat.put(player.getUUID(), 0.0F);
   }

   private boolean onCooldown(ServerPlayer player, long now) {
      return now < this.detonateCooldownUntil.getOrDefault(player.getUUID(), 0L);
   }

   private void maybeSyncHeat(ServerPlayer player, float heat) {
      float last = this.lastSyncedHeat.getOrDefault(player.getUUID(), -1.0F);
      boolean settled = heat == 0.0F && last == 0.0F;
      if (!settled && (Math.abs(heat - last) >= 0.02F || heat == 0.0F && last > 0.0F) && (player.tickCount % 3 == 0 || heat == 0.0F)) {
         this.lastSyncedHeat.put(player.getUUID(), heat);
         PowerService.sync(player);
      }
   }

   private void emitHeatTells(ServerPlayer player, float heat) {
      if (!(heat < 0.15F)) {
         ServerLevel level = player.serverLevel();
         Vec3 c = player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0);
         double spread = (double)player.getBbWidth() * 0.6 + 0.2;
         if (player.tickCount % 3 == 0) {
            NuclearFx.scatter(level, AwakenedParticles.EMBER, c, 0.35, 1 + (int)(heat * 3.0F), spread);
         }

         if (heat > 0.7F) {
            NuclearFx.scatter(level, AwakenedParticles.SPARK, c, 0.3, 1 + (int)(heat * 2.0F), spread * 1.1);
            if (player.tickCount % 10 == 0) {
               Fx.sound(level, player.position(), SoundEvents.BLAZE_BURN, SoundSource.PLAYERS, 0.4F + heat * 0.4F, 0.6F + heat);
            }
         }
      }
   }

   private void detonate(ServerPlayer player, PowerData data) {
      data.setVar("heat", 0.0F);
      double cooldownSeconds = (Double)Config.NUCLEAR_OVERHEAT_COOLDOWN_SECONDS.get();
      if (cooldownSeconds > 0.0) {
         this.detonateCooldownUntil.put(player.getUUID(), player.serverLevel().getGameTime() + Math.round(cooldownSeconds * 20.0));
      }

      ServerLevel level = player.serverLevel();
      Vec3 c = player.position().add(0.0, 1.0, 0.0);
      NuclearFx.boom(level, c, 5.0, NuclearFx.Kind.COMBAT);
      NuclearFx.screenShake(level, c, 28.0, 16);
      DamageSource src = Dmg.by(player, DamageTypes.MAGIC);
      boolean hit = false;
      LivingEntity threat = null;

      for (LivingEntity victim : AimUtil.livingAround(player, c, 5.0)) {
         Dmg.hurt(victim, level, src, MasteryScaling.scaledDamage(12.0F, player, "NuclearDetonate"));
         Vec3 push = victim.position().subtract(c);
         if (push.lengthSqr() < 1.0E-4) {
            push = new Vec3(0.0, 0.0, 1.0);
         }

         push = push.normalize();
         Harm.push(player, victim, push.x * 1.1, 0.5, push.z * 1.1);
         hit = true;
         if (threat == null && Foes.counts(player, victim)) {
            threat = victim;
         }
      }

      Fallout.add(player, level, player.position(), 5.0, 160, MasteryScaling.scaledDamage(2.5F, player, "NuclearFallout"));
      if (hit) {
         MasterySystem.award(player, MasteryReason.HIT, threat);
      }

      player.displayClientMessage(Component.translatable("awakened.nuclear.overheat"), true);
   }

   @Override
   public float onIncomingDamage(ServerPlayer player, DamageSource source, float amount) {
      PowerData data = PowerService.data(player);
      if (amount > 0.0F && OVERHEAT.enabled(data)) {
         float heat = Math.min(1.0F, data.var("heat", 0.0F) + amount * 0.045F);
         data.setVar("heat", heat);
         this.lastHit.put(player.getUUID(), player.serverLevel().getGameTime());
      }

      return super.onIncomingDamage(player, source, amount);
   }

   @Override
   public boolean hasResourceBar() {
      return true;
   }

   @Override
   public int resourceBarColor() {
      return -42466;
   }

   @Override
   public Component resourceBarLabel() {
      return Component.translatable("awakened.nuclear.heat");
   }

   @Override
   public float resourceBarFraction(ServerPlayer player, PowerData data) {
      return data.var("heat", 0.0F);
   }

   @Override
   public int resourceBarCooldownSeconds(ServerPlayer player, PowerData data) {
      long remainingTicks = this.detonateCooldownUntil.getOrDefault(player.getUUID(), 0L) - player.serverLevel().getGameTime();
      return remainingTicks > 0L ? (int)Math.ceil((double)remainingTicks / 20.0) : 0;
   }
}
