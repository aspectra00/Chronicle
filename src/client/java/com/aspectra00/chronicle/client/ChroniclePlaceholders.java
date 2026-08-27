package com.aspectra00.chronicle.client;

import eu.pb4.placeholders.api.client.ClientPlaceholderContext;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.ParserBuilder;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

public final class ChroniclePlaceholders {
    private static final NodeParser PARSER = ParserBuilder.of()
            .commonPlaceholders()
            .clientPlaceholders()
            .build();
    private static final Map<String, String> ALIASES = aliases();

    private ChroniclePlaceholders() {}

    public static String resolve(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String expanded = input;
        String worldName = currentWorldName();
        expanded = expanded.replace("{world}", worldName == null ? "%world:name%" : worldName);
        for (Map.Entry<String, String> alias : ALIASES.entrySet()) {
            expanded = expanded.replace(alias.getKey(), alias.getValue());
        }
        try {
            return PARSER.parseComponent(expanded, ClientPlaceholderContext.get().asParserContext()).getString();
        } catch (RuntimeException ignored) {
            return expanded;
        }
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

    private static Map<String, String> aliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("{coords}", "%player:pos_x% %player:pos_y% %player:pos_z%");
        aliases.put("{player}", "%player:name%");
        aliases.put("{dimension}", "%world:id%");
        aliases.put("{biome}", "%player:biome%");
        aliases.put("{x}", "%player:pos_x%");
        aliases.put("{y}", "%player:pos_y%");
        aliases.put("{z}", "%player:pos_z%");
        return aliases;
    }
}
