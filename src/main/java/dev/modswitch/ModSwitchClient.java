package dev.modswitch;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class ModSwitchClient implements ClientModInitializer {
    private static KeyBinding openMenuKey;

    @Override
    public void onInitializeClient() {
        ModSwitchStore.load();

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.modswitch.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new ModSwitchScreen());
                }
            }
        });

        // Apply queued file moves only after Minecraft exits, not while loaded mods are still running.
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ModSwitchStore.applyQueuedChanges());
    }
}
