package com.devilfruits.registry;

import com.devilfruits.core.trigger.DevilFruitTrigger;
import java.util.function.Supplier;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DFTriggers {
   public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, "devilfruits");
   public static final Supplier<DevilFruitTrigger> ATE_FRUIT = TRIGGERS.register("ate_fruit", DevilFruitTrigger::new);
   public static final Supplier<DevilFruitTrigger> MAX_MASTERY = TRIGGERS.register("max_mastery", DevilFruitTrigger::new);
   public static final Supplier<DevilFruitTrigger> CHANGED_FORM = TRIGGERS.register("changed_form", DevilFruitTrigger::new);
   public static final Supplier<DevilFruitTrigger> LOGIA_PASSED = TRIGGERS.register("logia_passed", DevilFruitTrigger::new);
   public static final Supplier<PlayerTrigger> HAKI_UNLOCKED = TRIGGERS.register("haki_unlocked", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> HAKI_LOGIA_HIT = TRIGGERS.register("haki_logia_hit", PlayerTrigger::new);
   public static final Supplier<PlayerTrigger> HAKI_MASTER = TRIGGERS.register("haki_master", PlayerTrigger::new);

   private DFTriggers() {
   }
}
