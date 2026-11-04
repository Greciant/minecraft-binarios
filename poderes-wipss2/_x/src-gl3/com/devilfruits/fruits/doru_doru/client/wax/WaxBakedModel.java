package com.devilfruits.fruits.doru_doru.client.wax;

import com.devilfruits.fruits.doru_doru.DoruBlocks;
import com.devilfruits.fruits.doru_doru.wax.WaxBlock;
import com.devilfruits.fruits.doru_doru.wax.WaxBlockEntity;
import com.google.common.collect.UnmodifiableIterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.common.util.TriState;

public final class WaxBakedModel extends BakedModelWrapper<BakedModel> {
   private static final ModelProperty<WaxMesher.Key> KEY = new ModelProperty();
   private static final ChunkRenderTypeSet SOLID = ChunkRenderTypeSet.of(new RenderType[]{RenderType.solid()});

   public WaxBakedModel(BakedModel original) {
      super(original);
   }

   public static void wrap(ModifyBakingResult event) {
      Map<ModelResourceLocation, BakedModel> models = event.getModels();
      UnmodifiableIterator var2 = ((WaxBlock)DoruBlocks.WAX.get()).getStateDefinition().getPossibleStates().iterator();

      while (var2.hasNext()) {
         BlockState state = (BlockState)var2.next();
         ModelResourceLocation id = BlockModelShaper.stateToModelLocation(state);
         BakedModel model = models.get(id);
         if (model != null && !(model instanceof WaxBakedModel)) {
            models.put(id, new WaxBakedModel(model));
         }
      }
   }

   public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
      return level.getBlockEntity(pos) instanceof WaxBlockEntity be ? ModelData.builder().with(KEY, WaxMesher.key(level, pos, be)).build() : modelData;
   }

   public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
      WaxMesher.Key key = (WaxMesher.Key)data.get(KEY);
      return key == null ? this.originalModel.getQuads(state, side, rand, data, renderType) : WaxMesher.quads(key, side);
   }

   public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
      return SOLID;
   }

   public boolean useAmbientOcclusion() {
      return false;
   }

   public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType) {
      return TriState.FALSE;
   }
}
