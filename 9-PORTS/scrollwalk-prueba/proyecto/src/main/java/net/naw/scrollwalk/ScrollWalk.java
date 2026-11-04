package net.naw.scrollwalk;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Main initializer class for the mod (port Forge: constructor en vez de ModInitializer#onInitialize)
@Mod(ScrollWalk.MOD_ID)
public class ScrollWalk {
    public static final String MOD_ID = "scrollwalk";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Stores the current calculated walking speed
    public static double currentSpeed = 0.1;
    public static double momentumSpeed = 0.1;
    // Holds the configuration instance for accessing settings
    public static ModConfig config;

    public ScrollWalk() {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }
        // Registers the configuration class with AutoConfig to handle saving/loading
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
        // Loads the config instance into the static variable
        config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        // Sets the initial speed based on the config file
        currentSpeed = config.scrollSpeed;

        // Port Forge: no hay ModMenu, se usa la pantalla de ajustes de Forge (boton "Config" en la lista de mods)
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (mc, parent) -> AutoConfig.getConfigScreen(ModConfig.class, parent).get()));
    }
}
