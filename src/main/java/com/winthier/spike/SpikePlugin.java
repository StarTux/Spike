package com.winthier.spike;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpikePlugin extends JavaPlugin {
    private WatchTask watchTask = new WatchTask(this);

    // --- JavaPlugin

    @Override
    public void onEnable() {
        saveDefaultConfig();
        importConfig();
        this.watchTask.enable();
        getServer().getScheduler().runTaskTimer(this, () -> this.watchTask.tick(), 0L, 1L);
        getServer().getScheduler().runTaskAsynchronously(this, this.watchTask);
    }

    @Override
    public void onDisable() {
        this.watchTask.stop();
    }

    // --- Util

    private void importConfig() {
        reloadConfig();
        this.watchTask.setReportingThreshold(getConfig().getInt("ReportingThreshold"));
    }

    // --- Command

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 0) return false;
        try {
            return onCommand(sender, args);
        } catch (Exception e) {
            if (sender instanceof HumanEntity) {
                sender.sendMessage(ChatColor.RED + e.getMessage());
            } else {
                e.printStackTrace();
            }
            return true;
        }
    }

    private boolean onCommand(final CommandSender sender, final String[] args) throws Exception {
        switch (args[0]) {
        case "reload": {
            if (args.length != 1) return false;
            importConfig();
            sender.sendMessage("[Spike] Configuration reloaded.");
            return true;
        }
        case "generate": {
            if (args.length != 2) return false;
            int ticks = Integer.parseInt(args[1]);
            sender.sendMessage("[Spike] Generating " + ticks + " missed ticks...");
            Thread.sleep((long)ticks * 50);
            sender.sendMessage("[Spike] Generated " + ticks + " missed ticks.");
            return true;
        }
        default: return false;
        }
    }
}
