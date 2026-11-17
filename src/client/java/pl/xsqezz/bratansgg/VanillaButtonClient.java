package pl.xsqezz.bratansgg;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class VanillaButtonClient implements ClientModInitializer {
    public static final String MOD_ID = "bratansgg";

    public static final int BUTTON_X = 8;
    public static final int BUTTON_Y = 8;
    public static final int BUTTON_WIDTH = 78;
    public static final int BUTTON_HEIGHT = 20;

    private static boolean enabled = true;
    private static boolean vWasDown;
    private static boolean rightShiftWasDown;

    @Override
    public void onInitializeClient() {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.of(MOD_ID, "vanilla_button"),
                VanillaButtonClient::render
        );

        ClientTickEvents.END_CLIENT_TICK.register(VanillaButtonClient::onClientTick);
    }

    private static void onClientTick(MinecraftClient client) {
        long window = client.getWindow().getHandle();
        boolean vDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_V) == GLFW.GLFW_PRESS;
        boolean rightShiftDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

        if (client.player != null && client.currentScreen == null) {
            if (vDown && !vWasDown) {
                toggle();
            }

            if (rightShiftDown && !rightShiftWasDown) {
                if (client.mouse.isCursorLocked()) {
                    client.mouse.unlockCursor();
                } else {
                    client.mouse.lockCursor();
                }
            }
        }

        vWasDown = vDown;
        rightShiftWasDown = rightShiftDown;
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.currentScreen != null) {
            return;
        }

        boolean cursorUnlocked = !client.mouse.isCursorLocked();
        double mouseX = client.mouse.getScaledX(client.getWindow());
        double mouseY = client.mouse.getScaledY(client.getWindow());
        boolean hovered = cursorUnlocked && isInsideButton(mouseX, mouseY);

        int borderColor = 0xFF171717;
        int backgroundColor;
        int stateColor;

        if (enabled) {
            backgroundColor = hovered ? 0xFF668747 : 0xFF4E6E35;
            stateColor = 0xFF8FE36F;
        } else {
            backgroundColor = hovered ? 0xFF696969 : 0xFF4A4A4A;
            stateColor = 0xFFCCCCCC;
        }

        context.fill(BUTTON_X - 1, BUTTON_Y - 1,
                BUTTON_X + BUTTON_WIDTH + 1, BUTTON_Y + BUTTON_HEIGHT + 1, borderColor);
        context.fill(BUTTON_X, BUTTON_Y,
                BUTTON_X + BUTTON_WIDTH, BUTTON_Y + BUTTON_HEIGHT, backgroundColor);
        context.drawCenteredTextWithShadow(
                client.textRenderer, "Vanilla",
                BUTTON_X + BUTTON_WIDTH / 2, BUTTON_Y + 6, 0xFFFFFFFF
        );
        context.drawTextWithShadow(
                client.textRenderer, enabled ? "ON" : "OFF",
                BUTTON_X + BUTTON_WIDTH + 6, BUTTON_Y + 6, stateColor
        );
    }

    public static boolean isInsideButton(double mouseX, double mouseY) {
        return mouseX >= BUTTON_X
                && mouseX < BUTTON_X + BUTTON_WIDTH
                && mouseY >= BUTTON_Y
                && mouseY < BUTTON_Y + BUTTON_HEIGHT;
    }

    public static void toggle() {
        enabled = !enabled;
    }
}
