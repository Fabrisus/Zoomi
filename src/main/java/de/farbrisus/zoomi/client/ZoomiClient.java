package de.farbrisus.zoomi.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

public class ZoomiClient implements ClientModInitializer {

    private static boolean CHECKED_KEYBINDING = false;
    private static boolean currentlyZoomed;
    private static KeyBinding zoomKeyBinding;
    private static boolean originalSmoothCameraEnabled;
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final double ZOOM_INCREMENT = 0.05;
    private static final double MAX_ZOOM = 0.83;
    private static final double MIN_ZOOM = 0.03;

    public static double ZOOM_LEVEL = 0.23;

    @Override
    public void onInitializeClient() {
        zoomKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zoomi",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                "category.zoomi"
        ));
        currentlyZoomed = false;
        originalSmoothCameraEnabled = false;
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!CHECKED_KEYBINDING && screen instanceof TitleScreen) {
                checkKeyBinding(zoomKeyBinding);
                CHECKED_KEYBINDING = true;
            }
        });

    }

    private void checkKeyBinding(KeyBinding keyBinding) {
        int count = 0;
        for (KeyBinding otherKeyBinding : mc.options.allKeys) {
            if (Objects.equals(otherKeyBinding.getBoundKeyTranslationKey(), keyBinding.getBoundKeyTranslationKey())) {
                count++;
            }
            if (count > 1) {
                MutableText title = Text.translatable("keybind.title.zoomi");
                MutableText desc = Text.translatable("keybind.desc.zoomi");
                displayToast(title, desc);
            }
        }
    }

    public void displayToast(MutableText title, MutableText description) {
        // Create the toast message
        SystemToast toast = new SystemToast(
                SystemToast.Type.LOW_DISK_SPACE,
                Text.of(title),
                Text.of(description)
        );

        // Get the Minecraft client and display the toast
        MinecraftClient client = MinecraftClient.getInstance();
        ToastManager toastManager = client.getToastManager();
        toastManager.add(toast);

    }

    public static boolean isZooming() {
        return zoomKeyBinding.isPressed();
    }

    public static double getZoomLevel() {
        return ZOOM_LEVEL;
    }

    public static void zoomIn() {
        ZOOM_LEVEL += ZOOM_INCREMENT;
        ZOOM_LEVEL = MathHelper.clamp(ZOOM_LEVEL, MIN_ZOOM, MAX_ZOOM);
    }

    public static void zoomOut() {
        ZOOM_LEVEL += -ZOOM_INCREMENT;
        ZOOM_LEVEL = MathHelper.clamp(ZOOM_LEVEL, MIN_ZOOM, MAX_ZOOM);
    }

    public static void manageSmoothCamera() {
        if (zoomStarting()) {
            zoomStarted();
            enableSmoothCamera();
        }

        if (zoomStopping()) {
            zoomStopped();
            resetSmoothCamera();
        }
    }

    private static boolean isSmoothCamera() {
        return mc.options.smoothCameraEnabled;
    }

    private static void enableSmoothCamera() {
        mc.options.smoothCameraEnabled = true;
    }

    private static void disableSmoothCamera() {
        mc.options.smoothCameraEnabled = false;
    }

    private static boolean zoomStarting() {
        return isZooming() && !currentlyZoomed;
    }

    private static boolean zoomStopping() {
        return !isZooming() && currentlyZoomed;
    }

    private static void zoomStarted() {
        originalSmoothCameraEnabled = isSmoothCamera();
        currentlyZoomed = true;
    }

    private static void zoomStopped() {

        currentlyZoomed = false;
    }

    private static void resetSmoothCamera() {
        if (originalSmoothCameraEnabled) {
            enableSmoothCamera();
        } else {
            disableSmoothCamera();
        }
    }
}
