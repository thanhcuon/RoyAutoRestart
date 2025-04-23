package com.github.athanh.royAutoRestart.commands;

import com.github.athanh.royAutoRestart.RoyAutoRestart;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class RoyAutoRestartCommand implements CommandExecutor, TabCompleter {
    private final RoyAutoRestart plugin;

    public RoyAutoRestartCommand(RoyAutoRestart plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("royautorestart.admin")) {
            sender.sendMessage(plugin.getConfig().getString("messages.errors.no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.getConfig().getString("messages.errors.invalid-command")
                    .replace("%label%", label));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "infotime":
                sender.sendMessage(plugin.getRestartManager().getNextRestartInfo());
                break;

            case "reload":
                plugin.reloadConfiguration();
                sender.sendMessage(plugin.getConfig().getString("messages.success.reload"));
                break;

            case "start":
                if (args.length < 2) {
                    sender.sendMessage(plugin.getConfig().getString("messages.errors.invalid-command")
                            .replace("%label%", label));
                    return true;
                }

                try {
                    int time = Integer.parseInt(args[1]);
                    String message = args.length > 2 ? args[2] : null;
                    int delay = args.length > 3 ? Integer.parseInt(args[3]) : 0;

                    plugin.getRestartManager().startManualRestart(time, message, delay);
                    sender.sendMessage(plugin.getConfig().getString("messages.success.start"));
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.getConfig().getString("messages.errors.invalid-time"));
                }
                break;

            default:
                sender.sendMessage(plugin.getConfig().getString("messages.errors.invalid-command")
                        .replace("%label%", label));
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("reload");
            completions.add("start");
            completions.add("infotime");
        }

        return completions;
    }
}
