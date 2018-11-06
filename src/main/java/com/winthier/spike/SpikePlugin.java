package com.winthier.spike;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class SpikePlugin extends JavaPlugin {
    final TickTask tickTask;
    final WatchTask watchTask;

    public SpikePlugin() {
        this.tickTask = new TickTask(this);
        this.watchTask = new WatchTask(this);
    }

    @Override
    public void onEnable() {
        this.tickTask.enable();
        this.watchTask.enable();
        new BukkitRunnable() {
            public void run() {
                SpikePlugin.this.watchTask.mainThread = Thread.currentThread();
            }
        }.runTaskLater((Plugin)this, 20L);
    }

    @Override
    public void onDisable() {
        this.watchTask.disable();
        this.tickTask.disable();
    }

    @Override
    public boolean onCommand(final CommandSender commandSender, final Command command, final String s, final String[] array) {
        return false;
    }
}
