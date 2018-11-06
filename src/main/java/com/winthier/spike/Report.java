package com.winthier.spike;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

final class Report {
    Map<String, Integer> counts = new HashMap<>();
    private static final Comparator<Entry> COMPARATOR = (entry, entry2) -> {
            if (entry.count < entry2.count) {
                return -1;
            }
            if (entry.count > entry2.count) {
                return 1;
            }
            return 0;
        };

    static String format(final StackTraceElement stackTraceElement) {
        return String.format("%s.%s(%s)", stackTraceElement.getClassName(), stackTraceElement.getMethodName(), stackTraceElement.getFileName());
    }

    void report(final StackTraceElement stackTraceElement) {
        if (stackTraceElement.isNativeMethod()) {
            return;
        }
        final String format = format(stackTraceElement);
        Integer value = this.counts.get(format);
        if (value == null) {
            value = 0;
        }
        this.counts.put(format, value + 1);
    }

    void reset() {
        this.counts.clear();
    }

    List<Entry> report() {
        final Entry[] array = new Entry[this.counts.size()];
        int n = 0;
        for (final Map.Entry<String, Integer> entry : this.counts.entrySet()) {
            array[n++] = new Entry(entry.getKey(), entry.getValue());
        }
        Arrays.sort(array, Report.COMPARATOR);
        return Arrays.asList(array);
    }

    void report(final PrintStream printStream) {
        final Iterator<Entry> iterator = this.report().iterator();
        while (iterator.hasNext()) {
            printStream.println(iterator.next().toString());
        }
    }

    static final class Entry {
        final String key;
        final int count;

        Entry(final String key, final int count) {
            this.key = key;
            this.count = count;
        }

        @Override
        public String toString() {
            return String.format("%03d %s", this.count, this.key);
        }
    }
}
