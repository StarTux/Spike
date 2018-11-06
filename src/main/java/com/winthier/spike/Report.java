package com.winthier.spike;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import lombok.Data;
import org.bukkit.command.CommandSender;

final class Report {
    HashMap<String, Entry> entries = new HashMap<>();

    void onMissedTick(final StackTraceElement stackTraceElement) {
        if (stackTraceElement.isNativeMethod()) {
            return;
        }
        final String key = stackTraceElement.getClassName();
        Entry entry = this.entries.get(key);
        if (entry == null) {
            entry = new Entry(stackTraceElement);
            this.entries.put(key, entry);
        }
        entry.methodNames.add(stackTraceElement.getMethodName());
        if (stackTraceElement.getLineNumber() >= 0) {
            entry.lineNumbers.add(stackTraceElement.getLineNumber());
        }
        entry.count += 1;
    }

    void reset() {
        this.entries.clear();
    }

    private List<Entry> createReport() {
        ArrayList<Entry> result = new ArrayList<>(this.entries.values());
        Collections.sort(result, (a, b) -> Integer.compare(a.count, b.count));
        return result;
    }

    int report(final PrintStream printStream) {
        for (Entry entry: this.createReport()) {
            printStream.println(entry.format());
        }
        return entries.size();
    }

    int report(final Logger logger) {
        for (Entry entry: this.createReport()) {
            logger.info(entry.format());
        }
        return entries.size();
    }

    int report(final CommandSender sender) {
        for (Entry entry: this.createReport()) {
            sender.sendMessage(entry.format());
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
            this.className = stackTraceElement.getClassName();
            this.fileName = stackTraceElement.getFileName();
        }

        String keyOf(final StackTraceElement stackTraceElement) {
            return String.format("%s.%s(%s)", stackTraceElement.getClassName(), stackTraceElement.getMethodName(), stackTraceElement.getFileName());
        }

        String format() {
            StringBuilder sb = new StringBuilder(String.format("%03d %s ", this.count, this.className));
            sb.append("(");
            if (!this.methodNames.isEmpty()) {
                ArrayList<String> names = new ArrayList<>(this.methodNames);
                Collections.sort(names);
                sb.append(names.get(0));
                for (int i = 1; i < names.size(); i += 1) sb.append(", ").append(names.get(i));
            }
            sb.append(") [");
            if (!this.lineNumbers.isEmpty()) {
                ArrayList<Integer> lines = new ArrayList<>(this.lineNumbers);
                Collections.sort(lines);
                sb.append(lines.get(0));
                for (int i = 1; i < lines.size(); i += 1) sb.append(", ").append(lines.get(i));
            }
            sb.append("]");
            return sb.toString();
        }
    }
}
