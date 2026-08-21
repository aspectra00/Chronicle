package com.aspectra00.chronicle.client;

import com.aspectra00.chronicle.client.config.ReminderTrigger;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class ReminderTriggerEvaluator {
    public enum Result {
        UNAVAILABLE,
        NO_MATCH,
        MATCH
    }

    private ReminderTriggerEvaluator() {
    }

    public static Result evaluate(Minecraft client, ReminderTrigger trigger) {
        if (client == null || client.player == null || client.level == null || trigger == null
                || trigger.type == null) {
            return Result.UNAVAILABLE;
        }

        boolean matched = switch (trigger.type) {
            case HEALTH_BELOW -> percentAtOrBelow(
                    client.player.getHealth(), client.player.getMaxHealth(), trigger.threshold);
            case HUNGER_BELOW -> client.player.getFoodData().getFoodLevel() <= trigger.threshold;
            case AIR_BELOW -> percentAtOrBelow(
                    client.player.getAirSupply(), client.player.getMaxAirSupply(), trigger.threshold);
            case INVENTORY_FULL -> inventoryHasNoEmptySlot(client.player.getInventory());
            case DURABILITY_BELOW -> durabilityAtOrBelow(
                    client.player.getMainHandItem(), trigger.threshold);
            case ENTER_DIMENSION -> client.level.dimension().identifier().toString()
                    .equals(trigger.normalizedTarget());
            case ENTER_AREA -> insideArea(client.player.getX(), client.player.getZ(), trigger);
        };
        return matched ? Result.MATCH : Result.NO_MATCH;
    }

    private static boolean percentAtOrBelow(double value, double maximum, int threshold) {
        return maximum > 0.0 && value * 100.0 <= maximum * threshold;
    }

    private static boolean inventoryHasNoEmptySlot(Inventory inventory) {
        return inventory != null && inventory.getFreeSlot() < 0;
    }

    private static boolean durabilityAtOrBelow(ItemStack stack, int threshold) {
        if (stack == null || stack.isEmpty() || !stack.isDamageableItem()) return false;
        int maximum = stack.getMaxDamage();
        if (maximum <= 0) return false;
        int remaining = Math.max(0, maximum - stack.getDamageValue());
        return (long) remaining * 100L <= (long) maximum * threshold;
    }

    private static boolean insideArea(double playerX, double playerZ, ReminderTrigger trigger) {
        double dx = playerX - trigger.x;
        double dz = playerZ - trigger.z;
        double radius = trigger.radius;
        return dx * dx + dz * dz <= radius * radius;
    }
}
