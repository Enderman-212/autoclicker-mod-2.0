package com.autoclicker.mod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoClickMod implements ClientModInitializer {

    public static final String MOD_ID = "autoclicker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Click rates: 20 CPS = one click every 3 ticks (60 ticks/sec / 20 CPS = 3)
    private static final int TICKS_PER_CLICK = 3; // 20 CPS at 20 TPS = every 3 ticks
    private static final double ATTACK_REACH = 3.0; // survival attack reach in blocks

    // Left-click autoclicker state
    private boolean leftClickActive = false;
    private int leftClickTicker = 0;

    // Right-click autoclicker state
    private boolean rightClickActive = false;
    private int rightClickTicker = 0;

    // Double-click detection for CapsLock
    private long lastCapsLockPress = 0L;
    private static final long DOUBLE_CLICK_THRESHOLD_MS = 1000L; // within 1 second

    // CapsLock key tracking (raw key, not a KeyBinding)
    private boolean capsLockWasDown = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[AutoClicker] Mod initialized. CapsLock = Left AutoClick. Double-tap CapsLock = Right AutoClick.");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            handleCapsLockInput(client);

            // Left auto-click: only when an entity is within attack reach
            if (leftClickActive) {
                leftClickTicker++;
                if (leftClickTicker >= TICKS_PER_CLICK) {
                    leftClickTicker = 0;
                    if (isEntityInReach(client)) {
                        performLeftClick(client);
                    }
                }
            }

            // Right auto-click: always fires (placing blocks)
            if (rightClickActive) {
                rightClickTicker++;
                if (rightClickTicker >= TICKS_PER_CLICK) {
                    rightClickTicker = 0;
                    performRightClick(client);
                }
            }
        });
    }

    private void handleCapsLockInput(MinecraftClient client) {
        // Check CapsLock key state via GLFW directly
        long windowHandle = client.getWindow().getHandle();
        boolean capsLockDown = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_CAPS_LOCK) == GLFW.GLFW_PRESS;

        // Detect rising edge (key just pressed this tick)
        if (capsLockDown && !capsLockWasDown) {
            long now = System.currentTimeMillis();
            long timeSinceLast = now - lastCapsLockPress;

            if (timeSinceLast <= DOUBLE_CLICK_THRESHOLD_MS && lastCapsLockPress != 0) {
                // Double-tap detected → toggle right-click autoclicker
                // Also disable left-click if it was on
                leftClickActive = false;
                leftClickTicker = 0;
                rightClickActive = !rightClickActive;
                rightClickTicker = 0;
                lastCapsLockPress = 0; // reset so triple-tap doesn't re-trigger

                if (rightClickActive) {
                    LOGGER.info("[AutoClicker] Right-click autoclicker ENABLED (20 CPS)");
                    sendStatusMessage(client, "§a[AutoClicker] Right-click ON §7(20 CPS)");
                } else {
                    LOGGER.info("[AutoClicker] Right-click autoclicker DISABLED");
                    sendStatusMessage(client, "§c[AutoClicker] Right-click OFF");
                }
            } else {
                // Single tap → schedule toggle of left-click
                // We store the time; if a second press doesn't come within threshold, it's a single tap
                // But since we're polling every tick we handle it immediately as single and let double override
                lastCapsLockPress = now;

                // Toggle left-click (if right is on, turn it off first)
                if (rightClickActive) {
                    rightClickActive = false;
                    rightClickTicker = 0;
                }
                leftClickActive = !leftClickActive;
                leftClickTicker = 0;

                if (leftClickActive) {
                    LOGGER.info("[AutoClicker] Left-click autoclicker ENABLED (20 CPS, entity-only)");
                    sendStatusMessage(client, "§a[AutoClicker] Left-click ON §7(20 CPS, entity-only)");
                } else {
                    LOGGER.info("[AutoClicker] Left-click autoclicker DISABLED");
                    sendStatusMessage(client, "§c[AutoClicker] Left-click OFF");
                }
            }
        }

        capsLockWasDown = capsLockDown;
    }

    /**
     * Returns true if the player's crosshair is targeting a living entity within attack reach.
     */
    private boolean isEntityInReach(MinecraftClient client) {
        if (client.crosshairTarget == null) return false;
        if (client.crosshairTarget.getType() != HitResult.Type.ENTITY) return false;

        EntityHitResult entityHit = (EntityHitResult) client.crosshairTarget;
        Entity target = entityHit.getEntity();

        if (!(target instanceof LivingEntity)) return false;
        if (!target.isAlive()) return false;

        // Distance check
        if (client.player == null) return false;
        double dist = client.player.squaredDistanceTo(target);
        return dist <= (ATTACK_REACH * ATTACK_REACH);
    }

    /**
     * Simulates a left mouse button click (attack).
     */
    private void performLeftClick(MinecraftClient client) {
        // Use the doAttack method via the interactionManager
        if (client.interactionManager == null || client.player == null) return;
        // Trigger attack on the targeted entity
        if (client.crosshairTarget instanceof EntityHitResult entityHit) {
            client.interactionManager.attackEntity(client.player, entityHit.getEntity());
            client.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        }
    }

    /**
     * Simulates a right mouse button click (use/place).
     */
    private void performRightClick(MinecraftClient client) {
        if (client.interactionManager == null || client.player == null) return;
        // Simulate right-click key press via the attack/use key binding
        // We directly call the use item logic
        client.options.useKey.setPressed(true);
        client.doItemUse();
        client.options.useKey.setPressed(false);
    }

    private void sendStatusMessage(MinecraftClient client, String message) {
        if (client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.literal(message), true); // true = action bar
        }
    }
}
