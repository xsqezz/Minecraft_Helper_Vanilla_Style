package pl.xsqezz.bratansgg.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.xsqezz.bratansgg.VanillaButtonClient;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void bratansgg$handleVanillaButton(
            long window,
            MouseInput input,
            int action,
            CallbackInfo callback
    ) {
        if (action != GLFW.GLFW_PRESS || input.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.currentScreen != null || client.mouse.isCursorLocked()) {
            return;
        }

        double mouseX = client.mouse.getScaledX(client.getWindow());
        double mouseY = client.mouse.getScaledY(client.getWindow());
        if (VanillaButtonClient.isInsideButton(mouseX, mouseY)) {
            VanillaButtonClient.toggle();
            client.mouse.lockCursor();
            callback.cancel();
        }
    }
}
