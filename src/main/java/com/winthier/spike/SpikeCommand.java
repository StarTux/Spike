package com.winthier.spike;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

@RequiredArgsConstructor
final class SpikeCommand implements TabExecutor {
    private final SpikePlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String alias, String[] args) {
        if (args.length == 0) return false;
        try {
            return onCommand(sender, args);
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (args.length == 0) return null;
        String arg = args[args.length - 1];
        if (args.length == 1) {
            return Stream.of("reload", "generate", "threshold", "report", "last")
                .filter(s -> s.startsWith(arg))
                .collect(Collectors.toList());
        }
        // args.length > 1
        switch (args[0]) {
        case "generate":
        case "threshold":
            if (args.length == 2) {
                try {
                    int tmp = Integer.parseInt(args[1]);
                    if (tmp > 0) {
                        return Stream.of(tmp, tmp * 10, tmp * 100)
                            .map(Object::toString)
                            .collect(Collectors.toList());
                    }
                } catch (NumberFormatException nfe) {
                    // Fallthrough
                }
            }
            return Collections.emptyList();
        case "reload":
        case "report":
            return Collections.emptyList();
        default:
            return null;
        }
    }

    private boolean onCommand(CommandSender sender, String[] args) throws Exception {
        switch (args[0]) {
        case "reload": {
            if (args.length != 1) return false;
            plugin.importConfig();
            sender.sendMessage("[Spike] Configuration reloaded.");
            return true;
        }
        case "generate": {
            if (args.length != 2) return false;
            int ticks = Integer.parseInt(args[1]);
            if (ticks <= 0) throw new RuntimeException("Invalid ticks: " + ticks);
            sender.sendMessage("[Spike] Waiting " + ticks + " ticks...");
            Thread.sleep((long) ticks * 50);
            sender.sendMessage("[Spike] Done");
            return true;
        }
        case "threshold": {
            if (args.length != 2) return false;
            int ticks = Integer.parseInt(args[1]);
            if (ticks <= 0) throw new RuntimeException("Invalid ticks: " + ticks);
            plugin.watchTask.reportingThreshold = ticks;
            sender.sendMessage("[Spike] Reporting threshold is now " + ticks + " ticks.");
            return true;
        }
        case "report": {
            if (args.length != 1) return false;
            sender.sendMessage("Full lag spike report:");
            int lines = plugin.watchTask.fullReport.report(sender);
            sender.sendMessage("total " + lines);
            return true;
        }
        case "last": {
            if (args.length != 1) return false;
            sender.sendMessage("Last lag spike report:");
            int lines = plugin.watchTask.shortReport.report(sender);
            sender.sendMessage("total " + lines);
            return true;
        }
        default: return false;
        }
    }
}
