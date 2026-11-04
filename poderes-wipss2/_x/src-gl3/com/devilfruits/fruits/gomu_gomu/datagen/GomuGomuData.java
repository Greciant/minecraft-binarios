package com.devilfruits.fruits.gomu_gomu.datagen;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.item.AbilityIconItem;
import com.devilfruits.core.seastone.SeastoneGear;
import com.devilfruits.core.trigger.DevilFruitTrigger;
import com.devilfruits.datagen.DFItemModelProvider;
import com.devilfruits.datagen.DFTagProviders;
import com.devilfruits.datagen.FruitDataContributor;
import com.devilfruits.fruits.gomu_gomu.GomuAbilities;
import com.devilfruits.fruits.gomu_gomu.GomuGomu;
import com.devilfruits.fruits.gomu_gomu.GomuItems;
import com.devilfruits.fruits.gomu_gomu.GomuTags;
import com.devilfruits.fruits.gomu_gomu.GomuTriggers;
import com.devilfruits.fruits.kage_kage.KageEntities;
import com.devilfruits.fruits.kage_kage.KageItems;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuItems;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import com.devilfruits.mobs.pirate.Pirates;
import com.devilfruits.registry.DFTriggers;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Advancement.Builder;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.advancements.critereon.PlayerTrigger.TriggerInstance;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class GomuGomuData implements FruitDataContributor {
   @Override
   public void itemModels(DFItemModelProvider models) {
      models.fruitItem(GomuGomu.fruit(), (Item)GomuItems.FRUIT.get());
   }

   @Override
   public void advancements(Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper efh) {
      AdvancementHolder root = Builder.advancement()
         .display(
            new ItemStack((ItemLike)GomuItems.FRUIT.get()),
            title("rubber_man"),
            desc("rubber_man"),
            ResourceLocation.withDefaultNamespace("textures/block/purple_wool.png"),
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion("ate_fruit", DevilFruitTrigger.TriggerInstance.of(DFTriggers.ATE_FRUIT.get(), GomuGomu.ID))
         .save(saver, GomuGomu.id("rubber_man").toString());
      child(saver, root, "no_effect", GomuAbilities.PISTOL, GomuTriggers.BLUNT.get(), AdvancementType.TASK);
      child(saver, root, "return_to_sender", GomuAbilities.PISTOL, GomuTriggers.BULLET_RETURNED.get(), AdvancementType.GOAL);
      child(saver, root, "bounce_back", GomuAbilities.BAZOOKA, GomuTriggers.HIGH_FALL_SURVIVED.get(), AdvancementType.TASK);
      child(saver, root, "slingshot", GomuAbilities.ROCKET, GomuTriggers.ROCKET_GAP_CROSSED.get(), AdvancementType.GOAL);
      child(saver, root, "crater_maker", GomuAbilities.AXE, GomuTriggers.AXE_CRATER.get(), AdvancementType.GOAL);
      child(saver, root, "barrage", GomuAbilities.GATLING, GomuTriggers.GATLING_FINISHED.get(), AdvancementType.CHALLENGE);
   }

   private static void child(Consumer<AdvancementHolder> saver, AdvancementHolder root, String name, Ability icon, PlayerTrigger trigger, AdvancementType type) {
      Builder.advancement()
         .parent(root)
         .display(AbilityIconItem.of(icon), title(name), desc(name), null, type, true, true, false)
         .addCriterion(name, trigger.createCriterion(new TriggerInstance(Optional.empty())))
         .save(saver, GomuGomu.id(name).toString());
   }

   private static Component title(String name) {
      return Component.translatable("advancements.devilfruits.gomu_gomu." + name + ".title");
   }

   private static Component desc(String name) {
      return Component.translatable("advancements.devilfruits.gomu_gomu." + name + ".description");
   }

   @Override
   public void itemTags(DFTagProviders.ItemTagGen tags) {
      tags.item(GomuTags.SHARP_WEAPONS)
         .addTag(ItemTags.SWORDS)
         .addTag(ItemTags.AXES)
         .addTag(ItemTags.PICKAXES)
         .addTag(ItemTags.HOES)
         .add(new Item[]{Items.TRIDENT, (Item)KageItems.MORIA_SCISSORS.get(), (Item)KageItems.MORIA_BLADE.get()})
         .addOptionalTag(common("tools/spear"));
      tags.item(GomuTags.BLUNT_WEAPONS)
         .add(
            new Item[]{
               Items.MACE, Items.STICK, Items.BLAZE_ROD, Items.BREEZE_ROD, Items.BONE, (Item)SeiryuItems.HASSAIKAI.get(), (Item)SeastoneGear.JITTE.get()
            }
         )
         .addTag(ItemTags.SHOVELS)
         .addOptionalTag(common("tools/mace"));
   }

   @Override
   public void entityTypeTags(DFTagProviders.EntityTypeTagGen tags) {
      tags.entityType(GomuTags.BLUNT_ATTACKERS)
         .add(
            new EntityType[]{
               EntityType.IRON_GOLEM,
               EntityType.RAVAGER,
               EntityType.WARDEN,
               EntityType.ENDERMAN,
               EntityType.ZOMBIE,
               EntityType.HUSK,
               EntityType.DROWNED,
               EntityType.ZOMBIE_VILLAGER,
               EntityType.ZOMBIFIED_PIGLIN,
               EntityType.GIANT,
               EntityType.SLIME,
               EntityType.MAGMA_CUBE,
               EntityType.GOAT,
               EntityType.PANDA,
               EntityType.DOLPHIN,
               EntityType.ENDER_DRAGON,
               EntityType.PIGLIN,
               EntityType.PIGLIN_BRUTE,
               EntityType.SKELETON,
               EntityType.STRAY,
               EntityType.BOGGED,
               EntityType.WITHER_SKELETON,
               EntityType.VINDICATOR,
               EntityType.ILLUSIONER,
               EntityType.WITCH,
               EntityType.CAMEL,
               EntityType.LLAMA,
               EntityType.TRADER_LLAMA
            }
         )
         .add(KageEntities.SHADOW_ZOMBIE.get());
      tags.entityType(GomuTags.SHARP_ATTACKERS)
         .add(
            new EntityType[]{
               EntityType.SPIDER,
               EntityType.CAVE_SPIDER,
               EntityType.WOLF,
               EntityType.CAT,
               EntityType.OCELOT,
               EntityType.FOX,
               EntityType.HOGLIN,
               EntityType.ZOGLIN,
               EntityType.VEX,
               EntityType.PHANTOM,
               EntityType.SILVERFISH,
               EntityType.ENDERMITE,
               EntityType.POLAR_BEAR,
               EntityType.AXOLOTL,
               EntityType.PUFFERFISH,
               EntityType.BEE,
               EntityType.GUARDIAN,
               EntityType.ELDER_GUARDIAN
            }
         );
      tags.entityType(GomuTags.BULLETS).add(Pirates.MUSKET_BALL_ENTITY.get());
   }

   @Override
   public void damageTypeTags(DFTagProviders.DamageTypeTagGen tags) {
      tags.damageType(GomuTags.BLUNT_DAMAGE)
         .add(new ResourceKey[]{DamageTypes.FLY_INTO_WALL, DamageTypes.FALLING_BLOCK, DamageTypes.FALLING_ANVIL, DamageTypes.CRAMMING});
      tags.damageType(GomuTags.ELECTRIC).add(new ResourceKey[]{DamageTypes.LIGHTNING_BOLT, UoUoSeiryu.THUNDER_DAMAGE});
   }

   private static TagKey<Item> common(String path) {
      return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
   }
}
