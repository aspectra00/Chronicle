package com.aspectra00.chronicle.client.config;

import net.minecraft.core.BlockPos;

import java.util.Locale;
import java.util.UUID;

public final class WatchTarget {
    public enum Kind {
        CROP,
        HONEY,
        CAULDRON,
        COMPOSTER,
        BERRIES,
        FURNACE,
        COPPER,
        ENTITY_GROWTH
    }

    public Kind kind = Kind.CROP;
    public String scope = "";
    public String dimension = "minecraft:overworld";
    public int x;
    public int y;
    public int z;
    public String blockId = "minecraft:air";
    public String property = "";
    public String targetValue = "";
    public String entityUuid = "";
    public String entityType = "";
    public String label = "";
    public long createdAtEpochMillis = System.currentTimeMillis();

    public BlockPos blockPos() {
        return new BlockPos(x, y, z);
    }

    public boolean isEntity() {
        return kind == Kind.ENTITY_GROWTH;
    }

    public boolean matchesScope(String currentScope) {
        return currentScope != null && currentScope.equals(scope);
    }

    public boolean matchesDimension(String currentDimension) {
        return currentDimension != null && currentDimension.equals(dimension);
    }

    public boolean sameBlock(String currentScope, String currentDimension, BlockPos pos) {
        return !isEntity() && matchesScope(currentScope) && matchesDimension(currentDimension)
                && pos != null && x == pos.getX() && y == pos.getY() && z == pos.getZ();
    }

    public boolean sameEntity(String currentScope, String currentDimension, UUID uuid) {
        return isEntity() && matchesScope(currentScope) && matchesDimension(currentDimension)
                && uuid != null && uuid.toString().equalsIgnoreCase(entityUuid);
    }

    public String identityKey() {
        String prefix = safe(scope) + '|' + safe(dimension) + '|';
        if (isEntity()) {
            return prefix + "entity|" + safe(entityUuid).toLowerCase(Locale.ROOT);
        }
        return prefix + "block|" + x + '|' + y + '|' + z;
    }

    public UUID parsedEntityUuid() {
        try {
            return UUID.fromString(entityUuid);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
