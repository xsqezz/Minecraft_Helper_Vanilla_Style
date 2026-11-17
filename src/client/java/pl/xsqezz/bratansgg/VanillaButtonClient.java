package pl.xsqezz.bratansgg;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class VanillaButtonClient implements ClientModInitializer {
    public static final String MOD_ID = "bratansgg";

    public static final int BUTTON_X = 8;
    public static final int BUTTON_Y = 8;
    public static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_MIN_WIDTH = 78;
    private static final int BUTTON_GAP = 2;
    private static final int ROW_HEIGHT = BUTTON_HEIGHT + BUTTON_GAP;
    private static final double PLAYER_RANGE_SQUARED = 100.0 * 100.0;

    private static boolean enabled = true;
    private static boolean vWasDown;
    private static boolean rightShiftWasDown;
    private static List<String> buttonLabels = List.of("Vanilla");

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
        updateButtonLabels(client);

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

    private static void updateButtonLabels(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            buttonLabels = List.of("Vanilla");
            return;
        }

        List<String> nearbyPlayers = new ArrayList<>();
        Vec3d localPlayerPosition = client.player.getEyePos();

        for (PlayerEntity other : client.world.getPlayers()) {
            if (other == client.player) {
                continue;
            }

            Vec3d difference = other.getEyePos().subtract(localPlayerPosition);
            if (difference.lengthSquared() > PLAYER_RANGE_SQUARED) {
                continue;
            }

            nearbyPlayers.add(other.getName().getString());
        }

        nearbyPlayers.sort(String.CASE_INSENSITIVE_ORDER);
        buttonLabels = nearbyPlayers.isEmpty() ? List.of("Vanilla") : List.copyOf(nearbyPlayers);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.currentScreen != null) {
            return;
        }

        List<String> labels = buttonLabels.isEmpty() ? List.of("Vanilla") : buttonLabels;
        int widestButton = BUTTON_MIN_WIDTH;
        for (String label : labels) {
            widestButton = Math.max(widestButton, getButtonWidth(client, label));
        }

        boolean cursorUnlocked = !client.mouse.isCursorLocked();
        double mouseX = client.mouse.getScaledX(client.getWindow());
        double mouseY = client.mouse.getScaledY(client.getWindow());

        int borderColor = 0xFF171717;
        int backgroundColor;
        int stateColor;

        if (enabled) {
            backgroundColor = 0xFF4E6E35;
            stateColor = 0xFF8FE36F;
        } else {
            backgroundColor = 0xFF4A4A4A;
            stateColor = 0xFFCCCCCC;
        }

        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            int buttonY = BUTTON_Y + i * ROW_HEIGHT;
            int buttonWidth = getButtonWidth(client, label);
            boolean hovered = cursorUnlocked
                    && mouseX >= BUTTON_X
                    && mouseX < BUTTON_X + buttonWidth
                    && mouseY >= buttonY
                    && mouseY < buttonY + BUTTON_HEIGHT;
            int rowBackground = enabled
                    ? (hovered ? 0xFF668747 : backgroundColor)
                    : (hovered ? 0xFF696969 : backgroundColor);

            context.fill(BUTTON_X - 1, buttonY - 1,
                    BUTTON_X + buttonWidth + 1, buttonY + BUTTON_HEIGHT + 1, borderColor);
            context.fill(BUTTON_X, buttonY,
                    BUTTON_X + buttonWidth, buttonY + BUTTON_HEIGHT, rowBackground);
            context.drawCenteredTextWithShadow(
                    client.textRenderer, label,
                    BUTTON_X + buttonWidth / 2, buttonY + 6, 0xFFFFFFFF
            );
        }

        context.drawTextWithShadow(
                client.textRenderer, enabled ? "ON" : "OFF",
                BUTTON_X + widestButton + 6, BUTTON_Y + 6, stateColor
        );
    }

    private static int getButtonWidth(MinecraftClient client, String label) {
        return Math.max(BUTTON_MIN_WIDTH, client.textRenderer.getWidth(label) + 12);
    }

    public static boolean isInsideButton(double mouseX, double mouseY) {
        int row = (int) ((mouseY - BUTTON_Y) / ROW_HEIGHT);
        if (row < 0 || row >= buttonLabels.size()) {
            return false;
        }

        int buttonY = BUTTON_Y + row * ROW_HEIGHT;
        if (mouseY < buttonY || mouseY >= buttonY + BUTTON_HEIGHT) {
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int buttonWidth = getButtonWidth(client, buttonLabels.get(row));
        return mouseX >= BUTTON_X && mouseX < BUTTON_X + buttonWidth;
    }

    public static void toggle() {
        enabled = !enabled;
    }
}
