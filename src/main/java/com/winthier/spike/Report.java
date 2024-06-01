package com.winthier.spike;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import lombok.Data;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText;

final class Report {
    private final HashMap<String, Entry> entries = new HashMap<>();

    public void onMissedTick(final StackTraceElement[] trace) {
        HashSet<String> doneKeys = new HashSet<>();
        for (StackTraceElement stackTraceElement: trace) {
            if (stackTraceElement.isNativeMethod()) continue;
            final String key = stackTraceElement.getClassName();
            Entry entry = entries.get(key);
            if (entry == null) {
                entry = new Entry(stackTraceElement);
                entries.put(key, entry);
            }
            if (!doneKeys.contains(key)) {
                entry.count += 1;
                doneKeys.add(key);
            }
        }
    }

    public void reset() {
        entries.clear();
    }

    private List<Entry> createReport() {
        ArrayList<Entry> result = new ArrayList<>(entries.values());
        Collections.sort(result, (a, b) -> Integer.compare(a.count, b.count));
        return result;
    }

    public int report(final PrintStream printStream) {
        for (Entry entry: createReport()) {
            printStream.println(plainText().serialize(entry.format()));
        }
        return entries.size();
    }

    public int report(final Logger logger) {
        for (Entry entry: createReport()) {
            logger.info(plainText().serialize(entry.format()));
        }
        return entries.size();
    }

    public int report(final CommandSender sender) {
        for (Entry entry: createReport()) {
            sender.sendMessage(entry.format());
        }
        return entries.size();
    }

    @Data
    public static final class Entry {
        private final String className;
        private final String fileName;
        private int count = 0;
        private final HashSet<String> methodNames = new HashSet<>();
        private final HashSet<Integer> lineNumbers = new HashSet<>();

        Entry(final StackTraceElement stackTraceElement) {
            className = stackTraceElement.getClassName();
            fileName = stackTraceElement.getFileName();
            methodNames.add(stackTraceElement.getMethodName());
            if (stackTraceElement.getLineNumber() >= 0) {
                lineNumbers.add(stackTraceElement.getLineNumber());
            }
        }

        public Component format() {
            final List<Component> line = new ArrayList<>();
            final String display = className
                .replace("java.lang.", "")
                .replace("java.util.", "")
                .replace("net.minecraft.server", "nms")
                .replace("org.bukkit.craftbukkit", "obc")
                .replace("org.bukkit", "bukkit")
                .replace("." + Bukkit.getServer().getClass().getName().split("\\.")[3], "")
                .replace("com.destroystokyo.paper", "paper");
            line.add(text(count, YELLOW));
            line.add(text(" " + display, GOLD));
            line.add(text(" (", DARK_GRAY));
            if (!methodNames.isEmpty()) {
                ArrayList<String> names = new ArrayList<>(methodNames);
                Collections.sort(names);
                line.add(text(names.get(0), GRAY));
                for (int i = 1; i < names.size(); i += 1) {
                    line.add(text(", ", DARK_GRAY));
                    line.add(text(names.get(i), GRAY));
                }
            }
            line.add(text(") [", DARK_GRAY));
            if (!lineNumbers.isEmpty()) {
                ArrayList<Integer> lines = new ArrayList<>(lineNumbers);
                Collections.sort(lines);
                line.add(text(lines.get(0), GRAY));
                for (int i = 1; i < lines.size(); i += 1) {
                    line.add(text(", ", DARK_GRAY));
                    line.add(text(lines.get(i), GRAY));
                }
            }
            line.add(text("]", DARK_GRAY));
            return join(noSeparators(), line);
        }
    }
}
