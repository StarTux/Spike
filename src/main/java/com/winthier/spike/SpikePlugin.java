package com.winthier.spike;

import org.bukkit.plugin.java.JavaPlugin;

public final class SpikePlugin extends JavaPlugin {
    WatchTask watchTask = new WatchTask(this);
    SpikeCommand spikeCommand = new SpikeCommand(this);

    @Override
    public void onEnable() {
        saveDefaultConfig();
        importConfig();
        watchTask.enable();
        getServer().getScheduler().runTaskTimer(this, () -> watchTask.tick(), 0L, 1L);
        getServer().getScheduler().runTaskAsynchronously(this, watchTask);
        getCommand("spike").setExecutor(spikeCommand);
    }

    @Override
    public void onDisable() {
        watchTask.stop();
    }

    void importConfig() {
        reloadConfig();
        watchTask.reportingThreshold = getConfig().getInt("ReportingThreshold");
        getLogger().info("ReportingThreshold: " + watchTask.reportingThreshold);
    }
}
