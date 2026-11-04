package com.devilfruits.compat.sable.mixin;

import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class SableMixinPlugin implements IMixinConfigPlugin {
   private static boolean loaded(String mod) {
      return LoadingModList.get() != null && LoadingModList.get().getModFileById(mod) != null;
   }

   public void onLoad(String mixinPackage) {
   }

   public String getRefMapperConfig() {
      return null;
   }

   public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
      if (!loaded("sable")) {
         return false;
      } else {
         return mixinClassName.endsWith("CreateContraptionVisualMixin")
            ? loaded("create") && loaded("flywheel")
            : !mixinClassName.endsWith("FlywheelEmbeddingMixin") || loaded("flywheel");
      }
   }

   public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
   }

   public List<String> getMixins() {
      return null;
   }

   public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }

   public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }
}
