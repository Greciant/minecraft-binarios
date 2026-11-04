package de.miraculixx.showmyworld;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ShowMyWorld.MOD_ID)
public class ShowMyWorld {
    public static final String MOD_ID = "showmyworld";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static PreviewConfig config;

    public ShowMyWorld() {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }
        AutoConfig.register(PreviewConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(PreviewConfig.class).getConfig();

        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (mc, parent) -> AutoConfig.getConfigScreen(PreviewConfig.class, parent).get()));
    }
}
