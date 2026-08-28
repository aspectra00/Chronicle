package com.aspectra00.chronicle.client;

import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

public final class ChroniclePlaceholders {
    private ChroniclePlaceholders() {}

    public static String resolve(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        Minecraft client = Minecraft.getInstance();
        String expanded = resolveBuiltIns(input, client);
        PlaceholderContext context = placeholderContext(client);
        if (context == null) {
            return expanded;
        }
        try {
            return Placeholders.parseText(Component.literal(expanded), context).getString();
        } catch (RuntimeException ignored) {
            return expanded;
        }
    }

    private static String resolveBuiltIns(String input, Minecraft client) {
        String world = currentWorldName();
        String player = "";
        String coords = "";
        String dimension = "";
        String biome = "";
        String x = "";
        String y = "";
        String z = "";
        if (client != null && client.player != null && client.level != null) {
            player = client.player.getName().getString();
            x = Integer.toString(client.player.getBlockX());
            y = Integer.toString(client.player.getBlockY());
            z = Integer.toString(client.player.getBlockZ());
            coords = x + " " + y + " " + z;
            dimension = client.level.dimension().identifier().toString();
            biome = client.level.getBiome(client.player.blockPosition()).unwrapKey()
                    .map(key -> key.identifier().toString())
                    .orElse("");
        }
        return input
                .replace("{world}", world == null ? "" : world)
                .replace("{coords}", coords)
                .replace("{player}", player)
                .replace("{dimension}", dimension)
                .replace("{biome}", biome)
                .replace("{x}", x)
                .replace("{y}", y)
                .replace("{z}", z);
    }

    private static PlaceholderContext placeholderContext(Minecraft client) {
        if (client == null || !client.hasSingleplayerServer() || client.getSingleplayerServer() == null) {
            return null;
        }
        if (client.player != null) {
            var player = client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID());
            if (player != null) {
                return PlaceholderContext.of(player);
            }
        }
        return PlaceholderContext.of(client.getSingleplayerServer());
    }

    private static String currentWorldName() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) return null;
        if (client.hasSingleplayerServer() && client.getSingleplayerServer() != null) {
            String name = client.getSingleplayerServer().getWorldData().getLevelName();
            if (name != null && !name.isBlank()) return name;
        }
        ServerData server = client.getCurrentServer();
        return server == null || server.name == null || server.name.isBlank() ? null : server.name;
    }
}
