package vectorwing.blockbox.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Port Forge 1.20.1: el original usa un Access Transformer para llegar a este campo privado
// de CrossCollisionBlock. En vez de ajustar el AT a mano, se usa un Mixin @Accessor, que ya
// funciono bien en otros ports de este hilo.
@Mixin(CrossCollisionBlock.class)
public interface CrossCollisionBlockAccessor {
    @Accessor("stateToIndex")
    Object2IntMap<BlockState> blockbox$getStateToIndex();
}
