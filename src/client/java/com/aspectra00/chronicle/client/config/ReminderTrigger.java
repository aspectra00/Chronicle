package com.aspectra00.chronicle.client.config;

import java.util.Locale;

public final class ReminderTrigger {
    public enum Type {
        HEALTH_BELOW,
        HUNGER_BELOW,
        AIR_BELOW,
        INVENTORY_FULL,
        DURABILITY_BELOW,
        ENTER_DIMENSION,
        ENTER_AREA
    }

    public Type type = Type.HEALTH_BELOW;
    public int threshold = 25;
    public String target = "minecraft:overworld";
    public int x;
    public int z;
    public int radius = 16;

    public ReminderTrigger copy() {
        ReminderTrigger copy = new ReminderTrigger();
        copy.type = type;
        copy.threshold = threshold;
        copy.target = target;
        copy.x = x;
        copy.z = z;
        copy.radius = radius;
        return copy;
    }

    public boolean sameDefinition(ReminderTrigger other) {
        return other != null
                && type == other.type
                && threshold == other.threshold
                && normalizedTarget().equals(other.normalizedTarget())
                && x == other.x
                && z == other.z
                && radius == other.radius;
    }

    public String normalizedTarget() {
        return target == null ? "" : target.trim().toLowerCase(Locale.ROOT);
    }
}
