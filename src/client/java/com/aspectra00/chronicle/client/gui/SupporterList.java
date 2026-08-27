package com.aspectra00.chronicle.client.gui;

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

final class SupporterList {
    private static final Gson GSON = new Gson();
    private static final String RESOURCE = "/assets/chronicle/supporters.json";
    private static final int MAX_SUPPORTERS = 500;
    private static final int MAX_NAME_LENGTH = 40;

    private SupporterList() {}

    static List<String> load() {
        try (InputStream stream = SupporterList.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) return List.of();
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                Data data = GSON.fromJson(reader, Data.class);
                if (data == null || data.supporters == null) return List.of();
                TreeSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                for (String value : data.supporters) {
                    if (value == null) continue;
                    String name = value.strip();
                    if (name.isEmpty()
                            || name.codePointCount(0, name.length()) > MAX_NAME_LENGTH
                            || name.codePoints().anyMatch(Character::isISOControl)) {
                        continue;
                    }
                    names.add(name);
                    if (names.size() >= MAX_SUPPORTERS) break;
                }
                return List.copyOf(names);
            }
        } catch (RuntimeException | java.io.IOException ignored) {
            return List.of();
        }
    }

    private static final class Data {
        private List<String> supporters = new ArrayList<>();
    }
}
