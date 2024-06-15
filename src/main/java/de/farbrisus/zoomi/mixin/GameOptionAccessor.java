package de.farbrisus.zoomi.mixin;

import de.farbrisus.zoomi.client.ZoomiClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public class GameOptionAccessor {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void getZoomLevel(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> callbackInfo) {
        if (ZoomiClient.isZooming()) {
            double fov = callbackInfo.getReturnValue();
            callbackInfo.setReturnValue(fov * ZoomiClient.getZoomLevel());
        }

        ZoomiClient.manageSmoothCamera();
    }
}
