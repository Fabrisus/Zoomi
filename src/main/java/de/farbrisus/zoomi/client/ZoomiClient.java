package de.farbrisus.zoomi.client;

import de.farbrisus.zoomi.owo.ZoomiConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

public class ZoomiClient implements ClientModInitializer {
    public static final ZoomiConfig CONFIG = ZoomiConfig.createAndLoad();

    private static boolean CHECKED_KEYBINDING = false;
    private static boolean currentlyZoomed;
    private static KeyBinding zoomKeyBinding;
    private static boolean originalSmoothCameraEnabled;
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private Screen lastScreen = null;
    private static final double ZOOM_INCREMENT = 0.05f;
    private static final double MAX_ZOOM = 1.00f;
    private static final double MIN_ZOOM = 0.01f;
    private static double ZOOM_LEVEL = CONFIG.DefaultZoomLevel();
    private static double TARGET_ZOOM_LEVEL = ZOOM_LEVEL;
    private static final double SMOOTH_SPEED = 5f;
    private static double DefaultMouseSensitivity = 0;


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
        HudRenderCallback.EVENT.register((drawContext, tickDeltaManager) -> {
            if (isZooming()) {
                renderText(drawContext);
                if (!CONFIG.SmoothCamera()) {
                    mc.options.getMouseSensitivity().setValue(DefaultMouseSensitivity * (float) ZoomiClient.getZoomLevel());
                }
                ZOOM_LEVEL = approachValue(ZOOM_LEVEL, TARGET_ZOOM_LEVEL, SMOOTH_SPEED, 0.005);
            }
            else {
                ZOOM_LEVEL = approachValue(ZOOM_LEVEL, 1, SMOOTH_SPEED, 0.01);
                updateZoomLevelToDefault();
            }


        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Screen currentScreen = MinecraftClient.getInstance().currentScreen;
            if (currentScreen != lastScreen) {
                if (!(currentScreen == null)) {
                    DefaultMouseSensitivity = mc.options.getMouseSensitivity().getValue();
                }
            }
            lastScreen = currentScreen;

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
        SystemToast toast = new SystemToast(
                SystemToast.Type.LOW_DISK_SPACE,
                Text.of(title),
                Text.of(description)
        );

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
        //TARGET_ZOOM_LEVEL += ZOOM_INCREMENT;
        TARGET_ZOOM_LEVEL = approachValue(ZOOM_LEVEL, ZOOM_LEVEL+ZOOM_INCREMENT, SMOOTH_SPEED, 1);
        if (TARGET_ZOOM_LEVEL > MAX_ZOOM) {
            TARGET_ZOOM_LEVEL = MAX_ZOOM;
        }
    }

    public static void zoomOut() {
        //TARGET_ZOOM_LEVEL -= ZOOM_INCREMENT;
        TARGET_ZOOM_LEVEL = approachValue(ZOOM_LEVEL, ZOOM_LEVEL-ZOOM_INCREMENT, SMOOTH_SPEED, 1);
        if (TARGET_ZOOM_LEVEL < MIN_ZOOM) {
            TARGET_ZOOM_LEVEL = MIN_ZOOM;
        }
    }

    private static void updateZoomLevelToDefault() {
        if (!isZooming()) {
            TARGET_ZOOM_LEVEL = CONFIG.DefaultZoomLevel();
        }
    }

    public static double approachValue(double current, double target, double speed, double dt) {
        double difference = target - current;
        double step = speed * dt;

        if (Math.abs(difference) <= step) {
            return target;
        }

        return current + (difference > 0 ? step : -step);
    }

    public static void manageSmoothCamera() {
        if (zoomStarting()) {
            zoomStarted();
            if (CONFIG.SmoothCamera()) {
                enableSmoothCamera();
            }
        }

        if (zoomStopping()) {
            zoomStopped();
            if (CONFIG.SmoothCamera()) {
                resetSmoothCamera();
            }
            mc.options.getMouseSensitivity().setValue(DefaultMouseSensitivity);
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

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    public static void renderText(@NotNull DrawContext context) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        MutableText zoomlvl = Text.translatable("text.desc.zoomlvl").append(calculatePercentage(round(ZOOM_LEVEL, 2)) + " %");
        Text text = Text.of(zoomlvl);
        renderScaledText(context, textRenderer, text, 10, 10, 0xFFFFFF, 0.5f);
    }

    public static void renderScaledText(DrawContext context, TextRenderer textRenderer, Text text, int x, int y, int color, float scale) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.scale(scale, scale, 1.0f);
        float adjustedX = x / scale;
        float adjustedY = y / scale;
        context.drawText(textRenderer, text, (int) adjustedX, (int) adjustedY, color, false);
        matrices.pop();
    }

    public static int calculatePercentage(double val) {
        if (val <= 0.01) {
            val = 0.00;
        }
        return (int) ((1.00 - val) * 100);
    }
}
