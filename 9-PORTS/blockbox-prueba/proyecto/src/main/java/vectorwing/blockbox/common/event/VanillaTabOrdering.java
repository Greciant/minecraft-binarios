package vectorwing.blockbox.common.event;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import vectorwing.blockbox.common.registry.ModItems;

import java.util.LinkedHashMap;
import java.util.function.Supplier;

// Port Forge 1.20.1: se llena igual que en el original (ver ModItems), pero CommonEvents
// no la usa (el port no inserta items en las pestanas vanilla, ver LEEME-port.txt).
// Se deja para no tener que tocar cada linea de ModItems que la referencia.
public class VanillaTabOrdering
{
	public static final LinkedHashMap<Supplier<? extends Item>, ItemLike> BUILDING_BLOCKS = new LinkedHashMap<>();
	public static final LinkedHashMap<Supplier<? extends Item>, ItemLike> FUNCTIONAL_BLOCKS = new LinkedHashMap<>();
}
