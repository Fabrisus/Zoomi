package de.farbrisus.zoomi.client;

import de.farbrisus.zoomi.owo.ZoomiConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
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
    private static final int ZOOM_INCREMENT = 5;
    private static final int MAX_ZOOM = 100;
    private static final int MIN_ZOOM = 1;
    private static final int SMOOTH_SPEED = 5;
    private static int ZOOM_LEVEL = (int)(CONFIG.DefaultZoomLevel() * 100);
    private static int TARGET_ZOOM_LEVEL = ZOOM_LEVEL;
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
                String mcVersion = FabricLoader.getInstance().getModContainer("minecraft")
                        .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                        .orElse("unknown");

                float newSensitivity = (float) (DefaultMouseSensitivity * ((float)ZOOM_LEVEL / 100f));
                if (mcVersion.startsWith("1.21.1") ||
                        mcVersion.startsWith("1.21.2") ||
                        mcVersion.startsWith("1.21.3") ||
                        mcVersion.startsWith("1.21.4")) {
                    mc.options.getMouseSensitivity().setValue((double) newSensitivity);
                }
                ZOOM_LEVEL = approachValue(ZOOM_LEVEL, TARGET_ZOOM_LEVEL, SMOOTH_SPEED);
            } else {
                ZOOM_LEVEL = approachValue(ZOOM_LEVEL, MAX_ZOOM, SMOOTH_SPEED);
                updateZoomLevelToDefault();
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Screen currentScreen = MinecraftClient.getInstance().currentScreen;
            if (currentScreen != lastScreen) {
                if (currentScreen != null) {
                    DefaultMouseSensitivity = mc.options.getMouseSensitivity().getValue();
                }
            }
            lastScreen = currentScreen;
        });
    }

    private void checkKeyBinding(KeyBinding myKeyBinding) {
        int conflictCount = 0;
        if (CONFIG.ShowKeyConflict()) {
            for (KeyBinding otherKeyBinding : mc.options.allKeys) {
                if (otherKeyBinding == myKeyBinding) continue;
                if (Objects.equals(otherKeyBinding.getBoundKeyTranslationKey(), myKeyBinding.getBoundKeyTranslationKey())) {
                    conflictCount++;
                    break;
                }
            }
            if (conflictCount > 0) {
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

    public static int getZoomLevel() {
        return ZOOM_LEVEL;
    }

    public static void zoomOut() {
        TARGET_ZOOM_LEVEL += ZOOM_INCREMENT;
        if (TARGET_ZOOM_LEVEL == 6) {
            TARGET_ZOOM_LEVEL = 5;
        }
        if (TARGET_ZOOM_LEVEL > MAX_ZOOM) {
            TARGET_ZOOM_LEVEL = MAX_ZOOM;
        }
        ZOOM_LEVEL = approachValue(ZOOM_LEVEL, TARGET_ZOOM_LEVEL, SMOOTH_SPEED);
    }


    public static void zoomIn() {
        TARGET_ZOOM_LEVEL -= ZOOM_INCREMENT;
        if (TARGET_ZOOM_LEVEL < MIN_ZOOM) {
            TARGET_ZOOM_LEVEL = MIN_ZOOM;
        }
        ZOOM_LEVEL = approachValue(ZOOM_LEVEL, TARGET_ZOOM_LEVEL, SMOOTH_SPEED);
    }

    private static void updateZoomLevelToDefault() {
        if (!isZooming()) {
            TARGET_ZOOM_LEVEL = (int)(CONFIG.DefaultZoomLevel() * 100);
        }
    }

    public static int approachValue(int current, int target, int step) {
        int difference = target - current;
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
            DefaultMouseSensitivity = mc.options.getMouseSensitivity().getValue();
        }
        if (isZooming()) {
            float newSensitivity = (float) (DefaultMouseSensitivity * ((float)ZOOM_LEVEL / 100f));
            mc.options.getMouseSensitivity().setValue((double) newSensitivity);
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

    public static void renderText(@NotNull DrawContext context) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        MutableText zoomlvl = Text.translatable("text.desc.zoomlvl")
                .append(calculatePercentage(ZOOM_LEVEL) + " %");
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

    public static int calculatePercentage(int zoomVal) {
        if (MAX_ZOOM - zoomVal == 99)
            return 100;
        return MAX_ZOOM - zoomVal;
    }
}
