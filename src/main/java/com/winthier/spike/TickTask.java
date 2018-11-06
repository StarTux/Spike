package com.winthier.spike;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

final class TickTask extends BukkitRunnable {
    private final SpikePlugin plugin;

    TickTask(final SpikePlugin plugin) {
        this.plugin = plugin;
    }

    void enable() {
        this.runTaskTimer((Plugin)this.plugin, 1L, 1L);
    }

    void disable() {
        try {
            this.cancel();
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
        }
    }

    public void run() {
        this.plugin.watchTask.tick();
    }
}
