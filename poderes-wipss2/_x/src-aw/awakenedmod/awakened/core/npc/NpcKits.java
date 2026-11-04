package com.awakenedmod.awakened.core.npc;

import com.awakenedmod.awakened.core.AwakenedConfigs;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NpcKits {
   private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, "awakened");
   private static final Map<String, NpcKit> KITS = new LinkedHashMap<>();
   private static final float WIDTH = 0.6F;
   private static final float HEIGHT = 1.8F;
   private static final float EYE = 1.62F;

   private NpcKits() {
   }

   public static <K extends NpcKit> K register(K kit) {
      if (KITS.containsKey(kit.id())) {
         throw new IllegalStateException("Duplicate powered NPC kit: " + kit.id());
      } else {
         String id = kit.id();
         Supplier<EntityType<PoweredNpc>> type = TYPES.register(
            id,
            () -> Builder.of((t, level) -> new PoweredNpc(t, level, kit), MobCategory.MONSTER)
                  .sized(0.6F, 1.8F)
                  .eyeHeight(1.62F)
                  .clientTrackingRange(10)
                  .updateInterval(2)
                  .build(id)
         );
         kit.bindType(type);
         KITS.put(id, kit);
         return kit;
      }
   }

   public static void register(IEventBus modBus) {
      TYPES.register(modBus);
      modBus.addListener(NpcKits::registerAttributes);
      NpcSpawner.register(NeoForge.EVENT_BUS);
      AwakenedConfigs.keyChoices("npcs.kitWeights", (Function<CommandSourceStack, Stream<String>>)(source -> KITS.keySet().stream()));
   }

   private static void registerAttributes(EntityAttributeCreationEvent event) {
      for (NpcKit kit : KITS.values()) {
         event.put(kit.type().get(), kit.attributes().build());
      }
   }

   public static Collection<NpcKit> all() {
      return Collections.unmodifiableCollection(KITS.values());
   }

   @Nullable
   public static NpcKit byId(String id) {
      return KITS.get(id);
   }
}
