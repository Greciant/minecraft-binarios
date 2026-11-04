package de.miraculixx.showmyworld.mixin;

import de.miraculixx.showmyworld.WorldPanoramaManager;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SelectWorldScreen.class)
public abstract class SelectWorldScreenMixin {

    @Inject(method = "removed", at = @At("TAIL"))
    private void showmyworld$onRemoved(CallbackInfo ci) {
        WorldPanoramaManager.releaseAll();
    }
}
