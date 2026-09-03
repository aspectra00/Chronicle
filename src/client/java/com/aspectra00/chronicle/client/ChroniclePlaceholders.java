package com.aspectra00.chronicle.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

public final class ChroniclePlaceholders {
    private ChroniclePlaceholders() {
    }

    public static String resolve(String input) {
        if (input == null || input.isEmpty()) return "";
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null || client.player == null) return input;
        String world = currentWorldName(client);
        String dimension = client.level.dimension().identifier().toString();
        String biome = client.level.getBiome(client.player.blockPosition()).unwrapKey()
                .map(key -> key.identifier().toString()).orElse("");
        int x = client.player.blockPosition().getX();
        int y = client.player.blockPosition().getY();
        int z = client.player.blockPosition().getZ();
        return input
                .replace("{world}", world)
                .replace("{coords}", x + " " + y + " " + z)
                .replace("{player}", client.player.getName().getString())
                .replace("{dimension}", dimension)
                .replace("{biome}", biome)
                .replace("{x}", Integer.toString(x))
                .replace("{y}", Integer.toString(y))
                .replace("{z}", Integer.toString(z));
    }

    private static String currentWorldName(Minecraft client) {
        if (client.hasSingleplayerServer() && client.getSingleplayerServer() != null) {
            String name = client.getSingleplayerServer().getWorldData().getLevelName();
            if (name != null && !name.isBlank()) return name;
        }
        ServerData server = client.getCurrentServer();
        return server == null || server.name == null || server.name.isBlank()
                ? client.level.dimension().identifier().toString() : server.name;
    }
}
