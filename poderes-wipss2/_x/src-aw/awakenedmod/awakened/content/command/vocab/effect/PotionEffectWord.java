package com.awakenedmod.awakened.content.command.vocab.effect;

import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.core.Harm;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public abstract class PotionEffectWord implements Effect {
   protected static final int REFRESH_TICKS = 8;
   private final String id;
   private final int unlockLevel;
   private final double base;
   private final boolean levelled;

   protected PotionEffectWord(String id, int unlockLevel, double base, boolean levelled) {
      this.id = id;
      this.unlockLevel = unlockLevel;
      this.base = base;
      this.levelled = levelled;
   }

   protected abstract Holder<MobEffect> mobEffect(int var1);

   protected int instanceTicks() {
      return 8;
   }

   protected int amplifier(int level) {
      return Math.max(0, level - 1);
   }

   @Override
   public final String id() {
      return this.id;
   }

   @Override
   public final int unlockLevel() {
      return this.unlockLevel;
   }

   @Override
   public final double defaultWeight() {
      return this.base;
   }

   @Override
   public boolean hasLevel() {
      return this.levelled;
   }

   @Override
   public String numberUnit() {
      return "awakened.command.unit.tier";
   }

   @Override
   public boolean apply(ServerPlayer caster, LivingEntity target, int level, int age) {
      Holder<MobEffect> holder = this.mobEffect(level);
      boolean fresh = !target.hasEffect(holder);
      MobEffectInstance instance = new MobEffectInstance(holder, this.instanceTicks(), this.amplifier(level), false, false, false);
      if (!this.hostileAt(level)) {
         target.addEffect(instance);
         return fresh;
      } else {
         return Harm.effect(caster, target, instance) && fresh;
      }
   }

   protected boolean hostileAt(int level) {
      return this.hostile();
   }

   @Override
   public boolean holds() {
      return this.hostile();
   }

   @Override
   public void release(LivingEntity target) {
      if (this.instanceTicks() > 8) {
         MobEffectInstance current = target.getEffect(this.mobEffect(1));
         if (current != null && !current.isVisible() && !current.showIcon()) {
            target.removeEffect(this.mobEffect(1));
         }
      }
   }
}
