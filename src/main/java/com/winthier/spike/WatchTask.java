package com.winthier.spike;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

final class WatchTask extends BukkitRunnable {
    private static final int MISSED_TICKS_THRESHOLD = 9;
    private static final int TICKS_PER_LOOP = 3;
    // Const
    private final SpikePlugin plugin;
    // Async
    private volatile int missedTicks;
    private volatile boolean cancelled;
    Thread mainThread;
    // Stats
    private int previouslyMissedTicks;
    private final Report fullReport;
    private final Report shortReport;
    // IO
    private PrintStream out;
    private final DateFormat dateFormat;

    WatchTask(final SpikePlugin plugin) {
        this.cancelled = false;
        this.previouslyMissedTicks = 0;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.fullReport = new Report();
        this.shortReport = new Report();
        this.plugin = plugin;
    }

    void enable() {
        this.mainThread = Thread.currentThread();
        this.missedTicks = 0;
        try {
            final File dataFolder = this.plugin.getDataFolder();
            dataFolder.mkdirs();
            final File file = new File(dataFolder, new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()) + ".txt");
            this.plugin.getLogger().info("Using log file " + file.getPath());
            this.out = new PrintStream(file);
            this.out.println(new Date().toString());
            this.out.println();
            this.out.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        this.runTaskAsynchronously((Plugin)this.plugin);
    }

    void disable() {
        this.cancelled = true;
        try {
            this.cancel();
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
        }
    }

    void tick() {
        this.missedTicks = 0;
    }

    private void loop() {
        try {
            Thread.sleep(150L);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            return;
        }
        if (this.out == null) {
            return;
        }
        final int preMissedTicks = this.missedTicks;
        this.missedTicks += 3;
        if (preMissedTicks == 0) {
            if (this.previouslyMissedTicks > 0) {
                if (this.previouslyMissedTicks > 9) {
                    System.out.println(String.format("[Spike] Reporting %d missed ticks", this.previouslyMissedTicks));
                    this.out.format("%s SPIKE missed %d ticks.\n", this.dateFormat.format(new Date()), this.previouslyMissedTicks);
                    this.shortReport.report(this.out);
                    this.out.println();
                }
                this.shortReport.reset();
                this.previouslyMissedTicks = 0;
            }
        } else {
            for (final StackTraceElement stackTraceElement : this.mainThread.getStackTrace()) {
                this.fullReport.report(stackTraceElement);
                this.shortReport.report(stackTraceElement);
            }
            this.previouslyMissedTicks = preMissedTicks + 3;
        }
    }

    @Override
    public void run() {
        this.plugin.getLogger().info("Watch task started");
        while (!this.cancelled) {
            this.loop();
        }
        this.out.format("Final report:\n", new Object[0]);
        this.fullReport.report(this.out);
        this.fullReport.reset();
        this.out.close();
        this.plugin.getLogger().info("Watch task terminated");
    }
}
