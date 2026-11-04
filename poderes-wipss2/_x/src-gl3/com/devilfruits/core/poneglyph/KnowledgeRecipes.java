package com.devilfruits.core.poneglyph;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

public final class KnowledgeRecipes {
   private KnowledgeRecipes() {
   }

   public interface Locked {
      ResourceLocation knowledge();
   }

   public static final class Shaped extends ShapedRecipe implements KnowledgeRecipes.Locked {
      private final ResourceLocation knowledge;

      public Shaped(ResourceLocation knowledge, ShapedRecipe base) {
         super(base.getGroup(), base.category(), base.pattern, base.getResultItem(null), base.showNotification());
         this.knowledge = knowledge;
      }

      @Override
      public ResourceLocation knowledge() {
         return this.knowledge;
      }

      public RecipeSerializer<?> getSerializer() {
         return Poneglyphs.KNOWLEDGE_SHAPED.get();
      }

      public static final class Serializer implements RecipeSerializer<KnowledgeRecipes.Shaped> {
         private static final MapCodec<KnowledgeRecipes.Shaped> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                     ResourceLocation.CODEC.fieldOf("knowledge").forGetter(KnowledgeRecipes.Shaped::knowledge),
                     RecipeSerializer.SHAPED_RECIPE.codec().forGetter(r -> r)
                  )
                  .apply(i, KnowledgeRecipes.Shaped::new)
         );
         private static final StreamCodec<RegistryFriendlyByteBuf, KnowledgeRecipes.Shaped> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            KnowledgeRecipes.Shaped::knowledge,
            RecipeSerializer.SHAPED_RECIPE.streamCodec(),
            r -> r,
            KnowledgeRecipes.Shaped::new
         );

         public MapCodec<KnowledgeRecipes.Shaped> codec() {
            return CODEC;
         }

         public StreamCodec<RegistryFriendlyByteBuf, KnowledgeRecipes.Shaped> streamCodec() {
            return STREAM_CODEC;
         }
      }
   }

   public static final class Shapeless extends ShapelessRecipe implements KnowledgeRecipes.Locked {
      private final ResourceLocation knowledge;

      public Shapeless(ResourceLocation knowledge, ShapelessRecipe base) {
         super(base.getGroup(), base.category(), base.getResultItem(null), base.getIngredients());
         this.knowledge = knowledge;
      }

      @Override
      public ResourceLocation knowledge() {
         return this.knowledge;
      }

      public RecipeSerializer<?> getSerializer() {
         return Poneglyphs.KNOWLEDGE_SHAPELESS.get();
      }

      public static final class Serializer implements RecipeSerializer<KnowledgeRecipes.Shapeless> {
         private static final MapCodec<KnowledgeRecipes.Shapeless> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                     ResourceLocation.CODEC.fieldOf("knowledge").forGetter(KnowledgeRecipes.Shapeless::knowledge),
                     RecipeSerializer.SHAPELESS_RECIPE.codec().forGetter(r -> r)
                  )
                  .apply(i, KnowledgeRecipes.Shapeless::new)
         );
         private static final StreamCodec<RegistryFriendlyByteBuf, KnowledgeRecipes.Shapeless> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            KnowledgeRecipes.Shapeless::knowledge,
            RecipeSerializer.SHAPELESS_RECIPE.streamCodec(),
            r -> r,
            KnowledgeRecipes.Shapeless::new
         );

         public MapCodec<KnowledgeRecipes.Shapeless> codec() {
            return CODEC;
         }

         public StreamCodec<RegistryFriendlyByteBuf, KnowledgeRecipes.Shapeless> streamCodec() {
            return STREAM_CODEC;
         }
      }
   }
}
