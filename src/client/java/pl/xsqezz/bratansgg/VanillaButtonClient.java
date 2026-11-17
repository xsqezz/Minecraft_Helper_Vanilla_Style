package pl.xsqezz.bratansgg;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class VanillaButtonClient implements ClientModInitializer {
    public static final String MOD_ID = "bratansgg";

    public static final int BUTTON_X = 8;
    public static final int BUTTON_Y = 8;
    public static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_MIN_WIDTH = 78;
    private static final int BUTTON_GAP = 2;
    private static final int ROW_HEIGHT = BUTTON_HEIGHT + BUTTON_GAP;
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };
    private static final PlayerButton VANILLA_BUTTON = new PlayerButton("Vanilla", ItemStack.EMPTY);

    private static boolean enabled = true;
    private static boolean vWasDown;
    private static boolean rightShiftWasDown;
    private static List<PlayerButton> buttons = List.of(VANILLA_BUTTON);

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
        updateButtons(client);

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

    private static void updateButtons(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            buttons = List.of(VANILLA_BUTTON);
            return;
        }

        List<PlayerButton> playerButtons = new ArrayList<>();
        for (PlayerEntity other : client.world.getPlayers()) {
            if (other != client.player) {
                playerButtons.add(new PlayerButton(
                        other.getName().getString(),
                        getMajorityArmorIcon(other)
                ));
            }
        }

        playerButtons.sort(Comparator.comparing(
                PlayerButton::name,
                String.CASE_INSENSITIVE_ORDER
        ));
        buttons = playerButtons.isEmpty() ? List.of(VANILLA_BUTTON) : List.copyOf(playerButtons);
    }

    private static ItemStack getMajorityArmorIcon(PlayerEntity player) {
        Map<Identifier, Integer> materialCounts = new HashMap<>();
        Map<Identifier, ItemStack> materialExamples = new HashMap<>();

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack armorPiece = player.getEquippedStack(slot);
            Identifier material = getArmorMaterial(armorPiece);
            if (material != null) {
                materialCounts.merge(material, 1, Integer::sum);
                materialExamples.putIfAbsent(material, armorPiece.copy());
            }
        }

        if (materialCounts.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int largestCount = materialCounts.values().stream()
                .max(Integer::compareTo)
                .orElse(0);

        Identifier chestplateMaterial = getArmorMaterial(
                player.getEquippedStack(EquipmentSlot.CHEST)
        );
        Identifier majorityMaterial = chestplateMaterial != null
                && materialCounts.getOrDefault(chestplateMaterial, 0) == largestCount
                ? chestplateMaterial
                : materialCounts.keySet().stream()
                        .filter(material -> materialCounts.get(material) == largestCount)
                        .min(Comparator.comparing(
                                Identifier::toString,
                                String.CASE_INSENSITIVE_ORDER
                        ))
                        .orElse(null);

        if (majorityMaterial == null) {
            return ItemStack.EMPTY;
        }

        ItemStack wornChestplate = player.getEquippedStack(EquipmentSlot.CHEST);
        if (majorityMaterial.equals(getArmorMaterial(wornChestplate))) {
            return wornChestplate.copy();
        }

        Identifier chestplateId = Identifier.of(
                majorityMaterial.getNamespace(),
                majorityMaterial.getPath() + "_chestplate"
        );
        Item chestplateItem = Registries.ITEM.get(chestplateId);
        if (chestplateItem != Items.AIR) {
            return new ItemStack(chestplateItem);
        }

        return materialExamples.get(majorityMaterial).copy();
    }

    private static Identifier getArmorMaterial(ItemStack armorPiece) {
        if (armorPiece.isEmpty()) {
            return null;
        }

        Identifier itemId = Registries.ITEM.getId(armorPiece.getItem());
        String path = itemId.getPath();
        for (String suffix : List.of("_helmet", "_chestplate", "_leggings", "_boots")) {
            if (path.endsWith(suffix) && path.length() > suffix.length()) {
                return Identifier.of(
                        itemId.getNamespace(),
                        path.substring(0, path.length() - suffix.length())
                );
            }
        }

        return null;
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.currentScreen != null) {
            return;
        }

        List<PlayerButton> visibleButtons = buttons.isEmpty() ? List.of(VANILLA_BUTTON) : buttons;
        int widestButton = BUTTON_MIN_WIDTH;
        for (PlayerButton button : visibleButtons) {
            widestButton = Math.max(widestButton, getButtonWidth(client, button));
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

        for (int i = 0; i < visibleButtons.size(); i++) {
            PlayerButton button = visibleButtons.get(i);
            int buttonY = BUTTON_Y + i * ROW_HEIGHT;
            int buttonWidth = getButtonWidth(client, button);
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

            int nameWidth = client.textRenderer.getWidth(button.name());
            int contentWidth = nameWidth + (button.chestplateIcon().isEmpty() ? 0 : 20);
            int contentX = BUTTON_X + (buttonWidth - contentWidth) / 2;
            context.drawTextWithShadow(
                    client.textRenderer, button.name(),
                    contentX, buttonY + 6, 0xFFFFFFFF
            );
            if (!button.chestplateIcon().isEmpty()) {
                context.drawItem(button.chestplateIcon(), contentX + nameWidth + 4, buttonY + 2);
            }
        }

        context.drawTextWithShadow(
                client.textRenderer, enabled ? "ON" : "OFF",
                BUTTON_X + widestButton + 6, BUTTON_Y + 6, stateColor
        );
    }

    private static int getButtonWidth(MinecraftClient client, PlayerButton button) {
        int contentWidth = client.textRenderer.getWidth(button.name())
                + (button.chestplateIcon().isEmpty() ? 0 : 20);
        return Math.max(BUTTON_MIN_WIDTH, contentWidth + 12);
    }

    public static boolean isInsideButton(double mouseX, double mouseY) {
        int row = (int) ((mouseY - BUTTON_Y) / ROW_HEIGHT);
        if (row < 0 || row >= buttons.size()) {
            return false;
        }

        int buttonY = BUTTON_Y + row * ROW_HEIGHT;
        if (mouseY < buttonY || mouseY >= buttonY + BUTTON_HEIGHT) {
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int buttonWidth = getButtonWidth(client, buttons.get(row));
        return mouseX >= BUTTON_X && mouseX < BUTTON_X + buttonWidth;
    }

    public static void toggle() {
        enabled = !enabled;
    }

    private record PlayerButton(String name, ItemStack chestplateIcon) {
    }
}
