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
public class MouseWheelAccessor {

    @Inject(method = "onMouseScroll", at = @At("RETURN"),  cancellable = true)
    public void onScrollEvent(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (ZoomiClient.isZooming()) {
            if(vertical < 0) {
                ZoomiClient.zoomIn();
            }
            if(vertical > 0) {
                ZoomiClient.zoomOut();
            }
        }
    }
}
