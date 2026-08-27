package com.aspectra00.chronicle.client;

import com.aspectra00.chronicle.client.config.ReminderConfig;
import com.aspectra00.chronicle.client.config.WatchTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class WatchManager {
    private enum Evaluation {
        WAITING,
        READY,
        UNAVAILABLE,
        INVALID
    }

    private static final long SCAN_PERIOD_MS = 250L;
    private static long nextScanAt = Long.MIN_VALUE;

    private WatchManager() {}

    public static void reset() {
        nextScanAt = Long.MIN_VALUE;
    }

    public static void toggleHovered(Minecraft client) {
        if (!ready(client)) return;
        String scope = currentScope(client);
        String dimension = currentDimension(client);
        if (scope.isBlank() || dimension.isBlank()) {
            feedback(client, "watch.unavailable");
            return;
        }
        if (client.hitResult instanceof BlockHitResult blockHit) {
            toggleBlock(client, scope, dimension, blockHit.getBlockPos());
            return;
        }
        Entity entity = client.hitResult instanceof EntityHitResult entityHit
                ? entityHit.getEntity() : client.crosshairPickEntity;
        if (entity != null) {
            toggleEntity(client, scope, dimension, entity);
            return;
        }
        feedback(client, "watch.unsupported");
    }

    public static void tick(Minecraft client, long monotonicNow) {
        if (!ready(client) || ChronicleClient.CONFIG.watches.isEmpty()
                || monotonicNow < nextScanAt) {
            return;
        }
        nextScanAt = monotonicNow + SCAN_PERIOD_MS;
        String scope = currentScope(client);
        String dimension = currentDimension(client);
        if (scope.isBlank() || dimension.isBlank()) return;
        List<WatchTarget> completed = new ArrayList<>();
        List<WatchTarget> invalid = new ArrayList<>();
        for (WatchTarget watch : List.copyOf(ChronicleClient.CONFIG.watches)) {
            if (!watch.matchesScope(scope) || !watch.matchesDimension(dimension)) continue;
            Evaluation evaluation = evaluate(client, watch);
            if (evaluation == Evaluation.READY) {
                completed.add(watch);
            } else if (evaluation == Evaluation.INVALID) {
                invalid.add(watch);
            }
        }
        if (completed.isEmpty() && invalid.isEmpty()) return;
        ChronicleClient.CONFIG.watches.removeAll(completed);
        ChronicleClient.CONFIG.watches.removeAll(invalid);
        ChronicleClient.saveWatchState();
        for (WatchTarget watch : completed) {
            ChronicleClient.enqueueWatchNotification(
                    ChronicleI18n.tr("watch.ready." + watch.kind.name().toLowerCase(Locale.ROOT),
                            watch.label));
        }
        if (invalid.size() == 1) {
            feedback(client, "watch.cancelled.one");
        } else if (invalid.size() > 1) {
            feedback(client, "watch.cancelled.many", invalid.size());
        }
    }

    public static List<WatchTarget> watchesForCurrentWorld(Minecraft client) {
        if (ChronicleClient.CONFIG == null || ChronicleClient.CONFIG.watches == null) {
            return List.of();
        }
        String scope = currentScope(client);
        return ChronicleClient.CONFIG.watches.stream()
                .filter(watch -> watch != null && watch.matchesScope(scope))
                .sorted(Comparator.comparingLong((WatchTarget watch) -> watch.createdAtEpochMillis).reversed())
                .toList();
    }

    public static boolean removeWatch(WatchTarget watch) {
        if (ChronicleClient.CONFIG == null || watch == null) return false;
        int index = ChronicleClient.CONFIG.watches.indexOf(watch);
        if (index < 0) return true;
        ChronicleClient.CONFIG.watches.remove(index);
        if (ChronicleClient.saveWatchEdit()) return true;
        ChronicleClient.CONFIG.watches.add(Math.min(index, ChronicleClient.CONFIG.watches.size()), watch);
        return false;
    }

    public static boolean clearCurrentWatches(Minecraft client) {
        if (ChronicleClient.CONFIG == null) return false;
        String scope = currentScope(client);
        List<WatchTarget> previous = new ArrayList<>(ChronicleClient.CONFIG.watches);
        ChronicleClient.CONFIG.watches.removeIf(watch -> watch.matchesScope(scope));
        if (ChronicleClient.saveWatchEdit()) return true;
        ChronicleClient.CONFIG.watches = previous;
        return false;
    }

    public static String condition(WatchTarget watch) {
        if (watch == null || watch.kind == null) return "";
        return ChronicleI18n.tr("watch.condition." + watch.kind.name().toLowerCase(Locale.ROOT));
    }

    public static String detail(WatchTarget watch) {
        if (watch == null) return "";
        String dimension = shortDimension(watch.dimension);
        return ChronicleI18n.tr("watch.detail", dimension, watch.x, watch.y, watch.z);
    }

    public static String currentScope(Minecraft client) {
        if (client == null) return "";
        String identity;
        if (client.hasSingleplayerServer() && client.getSingleplayerServer() != null) {
            identity = client.getSingleplayerServer().getWorldPath(LevelResource.ROOT)
                    .toAbsolutePath().normalize().toString();
            return "singleplayer:" + stableId(identity);
        }
        ServerData server = client.getCurrentServer();
        if (server != null && server.ip != null && !server.ip.isBlank()) {
            identity = server.ip.trim().toLowerCase(Locale.ROOT);
            return "server:" + stableId(identity);
        }
        if (client.getConnection() != null && client.getConnection().getConnection() != null
                && client.getConnection().getConnection().getRemoteAddress() != null) {
            identity = client.getConnection().getConnection().getRemoteAddress().toString();
            return "server:" + stableId(identity);
        }
        return "";
    }

    private static void toggleBlock(Minecraft client, String scope, String dimension, BlockPos pos) {
        WatchTarget existing = ChronicleClient.CONFIG.watches.stream()
                .filter(watch -> watch.sameBlock(scope, dimension, pos))
                .findFirst().orElse(null);
        if (existing != null) {
            if (removeWatch(existing)) feedback(client, "watch.removed", existing.label);
            else feedback(client, "watch.save_failed");
            return;
        }
        if (!client.level.isLoaded(pos)) {
            feedback(client, "watch.unavailable");
            return;
        }
        BlockState state = client.level.getBlockState(pos);
        WatchTarget watch = createBlockWatch(scope, dimension, pos, state);
        if (watch == null) {
            feedback(client, "watch.unsupported");
            return;
        }
        if (evaluate(client, watch) == Evaluation.READY) {
            feedback(client, "watch.already_ready", watch.label);
            return;
        }
        addWatch(client, watch);
    }

    private static void toggleEntity(Minecraft client, String scope, String dimension, Entity entity) {
        WatchTarget existing = ChronicleClient.CONFIG.watches.stream()
                .filter(watch -> watch.sameEntity(scope, dimension, entity.getUUID()))
                .findFirst().orElse(null);
        if (existing != null) {
            if (removeWatch(existing)) feedback(client, "watch.removed", existing.label);
            else feedback(client, "watch.save_failed");
            return;
        }
        if (!(entity instanceof AgeableMob ageable)) {
            feedback(client, "watch.unsupported");
            return;
        }
        if (!ageable.isBaby()) {
            feedback(client, "watch.already_ready", entity.getName().getString());
            return;
        }
        WatchTarget watch = new WatchTarget();
        watch.kind = WatchTarget.Kind.ENTITY_GROWTH;
        watch.scope = scope;
        watch.dimension = dimension;
        watch.x = entity.blockPosition().getX();
        watch.y = entity.blockPosition().getY();
        watch.z = entity.blockPosition().getZ();
        watch.entityUuid = entity.getUUID().toString();
        watch.entityType = EntityType.getKey(entity.getType()).toString();
        watch.label = entity.getName().getString();
        addWatch(client, watch);
    }

    private static void addWatch(Minecraft client, WatchTarget watch) {
        if (ChronicleClient.CONFIG.watches.size() >= ReminderConfig.MAX_WATCH_TARGETS) {
            feedback(client, "watch.limit", ReminderConfig.MAX_WATCH_TARGETS);
            return;
        }
        ChronicleClient.CONFIG.watches.add(watch);
        if (!ChronicleClient.saveWatchEdit()) {
            ChronicleClient.CONFIG.watches.remove(watch);
            feedback(client, "watch.save_failed");
            return;
        }
        feedback(client, "watch.added", watch.label);
    }

    private static WatchTarget createBlockWatch(String scope, String dimension,
                                                BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
        String path = blockId.getPath();
        WatchTarget watch = baseBlockWatch(scope, dimension, pos, state);
        if (block instanceof AbstractFurnaceBlock) {
            Property<?> lit = property(state, "lit");
            if (lit == null || !"true".equals(value(state, lit))) return null;
            watch.kind = WatchTarget.Kind.FURNACE;
            watch.property = "lit";
            watch.targetValue = "false";
            return watch;
        }
        if (block instanceof WeatheringCopper) {
            Block finalBlock = block;
            for (int step = 0; step < 8; step++) {
                Block next = WeatheringCopper.getNext(finalBlock).orElse(null);
                if (next == null) break;
                finalBlock = next;
            }
            watch.kind = WatchTarget.Kind.COPPER;
            watch.property = "block";
            watch.targetValue = BuiltInRegistries.BLOCK.getKey(finalBlock).toString();
            return watch;
        }
        Property<?> honey = property(state, "honey_level");
        if (honey != null) {
            watch.kind = WatchTarget.Kind.HONEY;
            watch.property = honey.getName();
            watch.targetValue = maximum(honey);
            return watch;
        }
        Property<?> level = property(state, "level");
        if (level != null && path.contains("cauldron")) {
            watch.kind = WatchTarget.Kind.CAULDRON;
            watch.property = level.getName();
            watch.targetValue = maximum(level);
            return watch;
        }
        if (level != null && path.equals("composter")) {
            watch.kind = WatchTarget.Kind.COMPOSTER;
            watch.property = level.getName();
            watch.targetValue = maximum(level);
            return watch;
        }
        Property<?> berries = property(state, "berries");
        if (berries != null && berries.getValue("true").isPresent()) {
            watch.kind = WatchTarget.Kind.BERRIES;
            watch.property = berries.getName();
            watch.targetValue = "true";
            return watch;
        }
        Property<?> age = property(state, "age");
        if (age != null && isCropLike(path)) {
            watch.kind = WatchTarget.Kind.CROP;
            watch.property = age.getName();
            watch.targetValue = maximum(age);
            return watch;
        }
        return null;
    }

    private static WatchTarget baseBlockWatch(String scope, String dimension,
                                              BlockPos pos, BlockState state) {
        WatchTarget watch = new WatchTarget();
        watch.scope = scope;
        watch.dimension = dimension;
        watch.x = pos.getX();
        watch.y = pos.getY();
        watch.z = pos.getZ();
        watch.blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        watch.label = state.getBlock().getName().getString();
        return watch;
    }

    private static Evaluation evaluate(Minecraft client, WatchTarget watch) {
        if (!watch.matchesDimension(currentDimension(client))) return Evaluation.UNAVAILABLE;
        if (watch.isEntity()) return evaluateEntity(client, watch);
        BlockPos pos = watch.blockPos();
        if (!client.level.isLoaded(pos)) return Evaluation.UNAVAILABLE;
        BlockState state = client.level.getBlockState(pos);
        String currentId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        if (watch.kind == WatchTarget.Kind.COPPER) {
            if (currentId.equals(watch.targetValue)) return Evaluation.READY;
            Block cursor = state.getBlock();
            for (int step = 0; step < 8; step++) {
                Block next = WeatheringCopper.getNext(cursor).orElse(null);
                if (next == null) break;
                if (BuiltInRegistries.BLOCK.getKey(next).toString().equals(watch.targetValue)) {
                    return Evaluation.WAITING;
                }
                cursor = next;
            }
            return Evaluation.INVALID;
        }
        if (!currentId.equals(watch.blockId)) return Evaluation.INVALID;
        Property<?> property = property(state, watch.property);
        if (property == null) return Evaluation.INVALID;
        return watch.targetValue.equals(value(state, property))
                ? Evaluation.READY : Evaluation.WAITING;
    }

    private static Evaluation evaluateEntity(Minecraft client, WatchTarget watch) {
        UUID uuid = watch.parsedEntityUuid();
        if (uuid == null) return Evaluation.INVALID;
        Entity entity = client.level.getEntityInAnyDimension(uuid);
        if (entity == null) return Evaluation.UNAVAILABLE;
        if (entity.isRemoved() || !entity.isAlive()
                || !EntityType.getKey(entity.getType()).toString().equals(watch.entityType)) {
            return Evaluation.INVALID;
        }
        if (!(entity instanceof AgeableMob ageable)) return Evaluation.INVALID;
        return ageable.isBaby() ? Evaluation.WAITING : Evaluation.READY;
    }

    private static Property<?> property(BlockState state, String name) {
        if (state == null || name == null) return null;
        for (Property<?> property : state.getProperties()) {
            if (name.equals(property.getName())) return property;
        }
        return null;
    }

    private static <T extends Comparable<T>> String valueTyped(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    private static String value(BlockState state, Property<?> property) {
        return valueTyped(state, property);
    }

    private static <T extends Comparable<T>> String maximumTyped(Property<T> property) {
        List<T> values = property.getPossibleValues();
        T maximum = values.stream().max(Comparator.naturalOrder()).orElse(null);
        return maximum == null ? "" : property.getName(maximum);
    }

    private static String maximum(Property<?> property) {
        return maximumTyped(property);
    }

    private static boolean isCropLike(String path) {
        return path.endsWith("_crop") || path.equals("wheat") || path.equals("carrots")
                || path.equals("potatoes") || path.equals("beetroots")
                || path.equals("nether_wart") || path.equals("cocoa")
                || path.equals("sweet_berry_bush");
    }

    private static String currentDimension(Minecraft client) {
        return client != null && client.level != null
                ? client.level.dimension().identifier().toString() : "";
    }

    private static String shortDimension(String value) {
        Identifier id = Identifier.tryParse(value == null ? "" : value);
        return id == null ? value : id.getPath().replace('_', ' ');
    }

    private static String stableId(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static void feedback(Minecraft client, String key, Object... args) {
        if (client != null && client.player != null) {
            client.player.sendOverlayMessage(ChronicleI18n.component(key, args));
        }
    }

    private static boolean ready(Minecraft client) {
        return client != null && client.player != null && client.level != null
                && ChronicleClient.CONFIG != null && ChronicleClient.CONFIG.watches != null;
    }
}
