package com.devilfruits.datagen;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityRegistry;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.FruitModule;
import com.devilfruits.core.fruit.FruitModules;
import com.devilfruits.core.fruit.FruitRegistry;
import com.devilfruits.registry.DFItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class DFItemModelProvider extends ItemModelProvider {
   public DFItemModelProvider(PackOutput output, ExistingFileHelper efh) {
      super(output, "devilfruits", efh);
   }

   protected void registerModels() {
      this.basicItem((Item)DFItems.SEASTONE_INGOT.get());
      ((ItemModelBuilder)this.withExistingParent("haki_scroll_armament", "item/generated")).texture("layer0", this.modLoc("item/haki/scroll_armament"));
      ((ItemModelBuilder)this.withExistingParent("haki_scroll_armament_master", "item/generated"))
         .texture("layer0", this.modLoc("item/haki/scroll_armament_master"));
      this.withExistingParent(DFItems.SEASTONE_BLOCK.getId().getPath(), this.modLoc("block/seastone_block"));
      ItemModelBuilder icon = (ItemModelBuilder)((ItemModelBuilder)this.withExistingParent("ability_icon", this.mcLoc("item/generated")))
         .texture("layer0", this.modLoc("item/ability/generic"));

      for (Ability ability : AbilityRegistry.all()) {
         DevilFruit fruit = AbilityRegistry.owner(ability);
         if (fruit != null && !fruit.isDevOnly()) {
            ModelFile model = ((ItemModelBuilder)this.withExistingParent("item/ability_icon/" + ability.id().getPath(), this.mcLoc("item/generated")))
               .texture("layer0", this.modLoc("item/ability/" + ability.id().getPath()));
            icon.override().predicate(DevilFruits.id("ability"), (float)AbilityRegistry.iconIndex(ability.id())).model(model).end();
         }
      }

      this.pirates();

      for (FruitModule module : FruitModules.all()) {
         DevilFruit fruit = FruitRegistry.get(module.id());
         if (fruit != null && !fruit.isDevOnly()) {
            module.data().itemModels(this);
         }
      }
   }

   private void pirates() {
      ((ItemModelBuilder)this.withExistingParent("item/pirate/musket_ball", this.mcLoc("item/generated")))
         .texture("layer0", this.modLoc("item/pirate/musket_ball"));
      ((ItemModelBuilder)this.withExistingParent("item/pirate/jolly_roger_banner_pattern", this.mcLoc("item/generated")))
         .texture("layer0", this.mcLoc("item/skull_banner_pattern"));

      for (String mob : new String[]{"deckhand", "gunner", "captain"}) {
         this.withExistingParent("item/pirate/" + mob + "_spawn_egg", this.mcLoc("item/template_spawn_egg"));
      }
   }

   public ItemModelBuilder fruitItem(DevilFruit fruit, Item item) {
      String path = BuiltInRegistries.ITEM.getKey(item).getPath();
      return (ItemModelBuilder)((ItemModelBuilder)this.withExistingParent(path, this.mcLoc("item/generated")))
         .texture("layer0", this.modLoc("item/fruit/" + fruit.id().getPath() + "/" + path));
   }
}
