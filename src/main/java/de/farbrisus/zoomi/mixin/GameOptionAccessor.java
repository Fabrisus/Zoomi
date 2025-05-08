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
    public void getZoomLevel(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> callbackInfo) {
        Float fov = callbackInfo.getReturnValue();
        float zoomMultiplier = ((float) ZoomiClient.getZoomLevel()) / 100.0f;
        callbackInfo.setReturnValue(fov * zoomMultiplier);
        ZoomiClient.manageSmoothCamera();
    }

    // For 1.21.1
    // @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    // public void getZoomLevel(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> callbackInfo) {
    //     Double fov = callbackInfo.getReturnValue();
    //     float zoomMultiplier = ((float) ZoomiClient.getZoomLevel()) / 100.0f;
    //     callbackInfo.setReturnValue(fov * zoomMultiplier);
    //     ZoomiClient.manageSmoothCamera();
    // }
}
