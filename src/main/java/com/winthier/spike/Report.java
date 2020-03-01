package com.winthier.spike;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

final class Report {
    HashMap<String, Entry> entries = new HashMap<>();

    void onMissedTick(final StackTraceElement[] trace) {
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

    void reset() {
        entries.clear();
    }

    private List<Entry> createReport() {
        ArrayList<Entry> result = new ArrayList<>(entries.values());
        Collections.sort(result, (a, b) -> Integer.compare(a.count, b.count));
        return result;
    }

    int report(final PrintStream printStream) {
        for (Entry entry: createReport()) {
            printStream.println(entry.format(false));
        }
        return entries.size();
    }

    int report(final Logger logger) {
        for (Entry entry: createReport()) {
            logger.info(entry.format(true));
        }
        return entries.size();
    }

    int report(final CommandSender sender) {
        for (Entry entry: createReport()) {
            sender.sendMessage(entry.format(true));
        }
        return entries.size();
    }

    @Data
    static final class Entry {
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

        String format(boolean color) {
            String display = className
                .replace("java.lang.", "")
                .replace("java.util.", "")
                .replace("net.minecraft.server", "nms")
                .replace("org.bukkit.craftbukkit", "obc")
                .replace("org.bukkit", "bukkit")
                .replace("." + Bukkit.getServer().getClass().getName().split("\\.")[3], "")
                .replace("com.destroystokyo.paper", "paper");
            StringBuilder sb = new StringBuilder("");
            if (color) sb.append(ChatColor.YELLOW);
            sb.append(count);
            if (color) sb.append(ChatColor.GOLD);
            sb.append(" ").append(display);
            if (color) sb.append(ChatColor.DARK_GRAY);
            sb.append(" (");
            if (!methodNames.isEmpty()) {
                ArrayList<String> names = new ArrayList<>(methodNames);
                Collections.sort(names);
                if (color) sb.append(ChatColor.GRAY);
                sb.append(names.get(0));
                for (int i = 1; i < names.size(); i += 1) {
                    if (color) sb.append(ChatColor.DARK_GRAY);
                    sb.append(", ");
                    if (color) sb.append(ChatColor.GRAY);
                    sb.append(names.get(i));
                }
            }
            if (color) sb.append(ChatColor.DARK_GRAY);
            sb.append(") [");
            if (!lineNumbers.isEmpty()) {
                ArrayList<Integer> lines = new ArrayList<>(lineNumbers);
                Collections.sort(lines);
                if (color) sb.append(ChatColor.GRAY);
                sb.append(lines.get(0));
                for (int i = 1; i < lines.size(); i += 1) {
                    if (color) sb.append(ChatColor.DARK_GRAY);
                    sb.append(", ");
                    if (color) sb.append(ChatColor.GRAY);
                    sb.append(lines.get(i));
                }
            }
            if (color) sb.append(ChatColor.DARK_GRAY);
            sb.append("]");
            return sb.toString();
        }
    }
}
