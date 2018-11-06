package com.winthier.spike;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * A single instance of this class will run in an async task and take
 * note of missed ticks.
 */
@RequiredArgsConstructor
final class WatchTask implements Runnable {
    // Required
    private final SpikePlugin plugin;
    // Config
    @Getter @Setter private int reportingThreshold = 4;
    // Async
    private volatile AtomicBoolean ticked = new AtomicBoolean(true);
    private volatile boolean cancelled = false;
    Thread mainThread;
    // Stats
    private int missedTicks = 0;
    @Getter private final Report fullReport = new Report();
    private final Report shortReport = new Report();
    // IO
    private PrintStream out;
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // --- Visible methods for main thread

    /**
     * Set up logging and initialize some state information.  This is
     * called from the main thread.
     */
    void enable() {
        this.mainThread = Thread.currentThread();
        this.missedTicks = 0;
        try {
            final File dataFolder = this.plugin.getDataFolder();
            dataFolder.mkdirs();
            String filename = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()) + ".log";
            final File file = new File(dataFolder, filename);
            this.plugin.getLogger().info("Using log file " + file.getPath());
            // Create PrintStream
            this.out = new PrintStream(file);
            this.out.println(new Date().toString());
            this.out.println();
            this.out.flush();
            // Create symlink
            final File link = new File(dataFolder, "latest.log");
            if (link.exists()) link.delete();
            Files.createSymbolicLink(link.toPath(), Paths.get(filename));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    void stop() {
        this.cancelled = true;
        this.ticked.set(true);
    }

    /**
     * Called by a plugin task once every (main thread) tick.
     */
    void tick() {
        this.ticked.set(true);
    }

    // --- Task

    @Override
    public void run() {
        this.plugin.getLogger().info("Watch task started");
        while (!this.cancelled) {
            this.loop();
        }
        this.out.println("Final report:");
        this.fullReport.report(this.out);
        this.fullReport.reset();
        this.out.close();
        this.plugin.getLogger().info("Watch task terminated");
    }

    private void loop() {
        try {
            Thread.sleep(50L);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        if (this.ticked.getAndSet(false)) {
            // If we were ticked and the previous no-tick span was
            // larger than the threshold, log the short report to
            // console and the log file and reset it.
            final int missed = this.missedTicks + 1;
            if (missed >= this.reportingThreshold) {
                this.plugin.getLogger().info(String.format("Reporting %d missed ticks", missed));
                this.out.format("%s SPIKE missed %d ticks.\n", this.dateFormat.format(new Date()), missed);
                this.shortReport.report(this.out);
                this.out.println();
            }
            this.missedTicks = 0;
            this.shortReport.reset();
        } else {
            // If a tick was missed, add this info to the short and
            // full report.
            this.missedTicks += 1;
            final StackTraceElement[] trace = this.mainThread.getStackTrace();
            this.fullReport.onMissedTick(trace);
            this.shortReport.onMissedTick(trace);
        }
    }
}
