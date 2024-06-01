package com.winthier.spike;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;

/**
 * A single instance of this class will run in an async task and take
 * note of missed ticks.
 */
@RequiredArgsConstructor
final class WatchTask implements Runnable {
    // Required
    final SpikePlugin plugin;
    // Config
    int reportingThreshold = 4;
    // Async
    private volatile AtomicBoolean ticked = new AtomicBoolean(true);
    private volatile boolean cancelled = false;
    private Thread mainThread;
    // Stats
    int missedTicks = 0;
    final Report fullReport = new Report();
    Report lastReport = new Report();
    Report currentReport = new Report();
    // IO
    private PrintStream out;
    final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // --- Visible methods for main thread

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

    /**
     * Set up logging and initialize some state information.  This is
     * called from the main thread.
     */
    void enable() {
        mainThread = Thread.currentThread();
        missedTicks = 0;
        long tooOld = Duration.ofDays(7).toMillis();
        try {
            final File dataFolder = plugin.getDataFolder();
            dataFolder.mkdirs();
            for (File file : dataFolder.listFiles()) {
                if (!file.isFile()) continue;
                String name = file.getName();
                if (!name.endsWith(".log")) continue;
                name = name.substring(0, name.length() - 4);
                if (name.equals("latest")) continue;
                final Date date;
                try {
                    date = DATE_FORMAT.parse(name);
                } catch (ParseException pe) {
                    pe.printStackTrace();
                    continue;
                }
                if (System.currentTimeMillis() - date.getTime() > tooOld) {
                    plugin.getLogger().info("Deleting old file: " + file);
                    file.delete();
                }
            }
            String filename = DATE_FORMAT.format(new Date()) + ".log";
            final File file = new File(dataFolder, filename);
            plugin.getLogger().info("Using log file " + file.getPath());
            // Create PrintStream
            out = new PrintStream(file);
            out.println(new Date().toString());
            out.println();
            out.flush();
            // Create symlink
            final File link = new File(dataFolder, "latest.log");
            if (link.exists()) link.delete();
            // Apparently File::exists yields false for broken symlink
            if (Files.isSymbolicLink(link.toPath())) link.delete();
            Files.createSymbolicLink(link.toPath(), Paths.get(filename));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    void stop() {
        cancelled = true;
        ticked.set(true);
    }

    /**
     * Called by a plugin task once every (main thread) tick.
     */
    void tick() {
        ticked.set(true);
    }

    // --- Task

    @Override
    public void run() {
        plugin.getLogger().info("Watch task started");
        while (!cancelled) {
            loop();
        }
        out.println("Final report:");
        fullReport.report(out);
        fullReport.reset();
        out.close();
        plugin.getLogger().info("Watch task terminated");
    }

    private void loop() {
        try {
            Thread.sleep(50L);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        if (ticked.getAndSet(false)) {
            // If we were ticked and the previous no-tick span was
            // larger than the threshold, log the short report to
            // console and the log file and reset it.
            final int missed = missedTicks + 1;
            if (missed >= reportingThreshold) {
                String msg = String.format("Reporting %d missed ticks", missed);
                plugin.getLogger().info(msg);
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (!player.hasPermission("spike.notify")) continue;
                    player.sendMessage(text("[Spike] " + msg, YELLOW));
                }
                out.format("%s SPIKE missed %d ticks.\n", dateFormat.format(new Date()), missed);
                currentReport.report(out);
                out.println();
                lastReport = currentReport;
                currentReport = new Report();
            } else {
                currentReport.reset();
            }
            missedTicks = 0;
        } else {
            // If a tick was missed, add this info to the short and
            // full report.
            missedTicks += 1;
            final StackTraceElement[] trace = mainThread.getStackTrace();
            fullReport.onMissedTick(trace);
            currentReport.onMissedTick(trace);
        }
    }
}
