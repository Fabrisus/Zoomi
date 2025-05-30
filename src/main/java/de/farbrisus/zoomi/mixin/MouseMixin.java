package de.farbrisus.zoomi.mixin;

import de.farbrisus.zoomi.client.ZoomiClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Mouse.class)
public class MouseMixin {

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    public void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo info) {
        if (ZoomiClient.isZooming()) {
            if (vertical > 0) {
                ZoomiClient.zoomIn();
            } else if (vertical < 0) {
                ZoomiClient.zoomOut();
            }
            info.cancel();
        }
    }
}
