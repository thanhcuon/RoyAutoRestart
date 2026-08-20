package com.github.athanh.royAutoRestart.commands;

import com.github.athanh.royAutoRestart.RoyAutoRestart;
import com.github.athanh.royAutoRestart.config.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Handles /royautorestart (/rar) commands.
 */
public class RoyAutoRestartCommand implements CommandExecutor, TabCompleter {

    private final RoyAutoRestart plugin;

    public RoyAutoRestartCommand(RoyAutoRestart plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LanguageManager lang = plugin.getLanguageManager();

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload": {
                if (!sender.hasPermission("royautorestart.reload") && !sender.hasPermission("royautorestart.admin")) {
                    sender.sendMessage(lang.getMessage("messages.commands.no-permission"));
                    return true;
                }
                plugin.reloadConfiguration();
                sender.sendMessage(plugin.getLanguageManager().getMessage("messages.commands.reload-success"));
                break;
            }

            case "start": {
                if (!sender.hasPermission("royautorestart.start") && !sender.hasPermission("royautorestart.admin")) {
                    sender.sendMessage(lang.getMessage("messages.commands.no-permission"));
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage(lang.getMessage("messages.commands.invalid-command", "%label%", label));
                    return true;
                }

                int time;
                try {
                    time = Integer.parseInt(args[1]);
                    if (time <= 0) {
                        sender.sendMessage(lang.getMessage("messages.commands.invalid-time"));
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(lang.getMessage("messages.commands.invalid-time"));
                    return true;
                }

                String customMsg = null;
                int delay = 0;

                if (args.length >= 3) {
                    // Check if last arg is delay number or message
                    if (args.length >= 4) {
                        try {
                            delay = Integer.parseInt(args[args.length - 1]);
                            // Message is everything between arg 2 and arg length - 2
                            StringBuilder msgBuilder = new StringBuilder();
                            for (int i = 2; i < args.length - 1; i++) {
                                msgBuilder.append(args[i]).append(" ");
                            }
                            customMsg = msgBuilder.toString().trim();
                        } catch (NumberFormatException e) {
                            // If last arg is not a number, all remaining args are message
                            StringBuilder msgBuilder = new StringBuilder();
                            for (int i = 2; i < args.length; i++) {
                                msgBuilder.append(args[i]).append(" ");
                            }
                            customMsg = msgBuilder.toString().trim();
                        }
                    } else {
                        // args.length == 3: could be delay or single word message
                        try {
                            delay = Integer.parseInt(args[2]);
                        } catch (NumberFormatException e) {
                            customMsg = args[2];
                        }
                    }
                }

                if (plugin.getRestartManager().isRestarting()) {
                    sender.sendMessage(lang.getMessage("messages.commands.already-restarting"));
                    return true;
                }

                boolean started = plugin.getRestartManager().startManualRestart(time, customMsg, delay);
                if (started) {
                    sender.sendMessage(lang.getMessage("messages.commands.start-success", "%time%", String.valueOf(time)));
                }
                break;
            }

            case "cancel":
            case "stop": {
                if (!sender.hasPermission("royautorestart.cancel") && !sender.hasPermission("royautorestart.admin")) {
                    sender.sendMessage(lang.getMessage("messages.commands.no-permission"));
                    return true;
                }

                if (!plugin.getRestartManager().isRestarting()) {
                    sender.sendMessage(lang.getMessage("messages.commands.not-restarting"));
                    return true;
                }

                plugin.getRestartManager().cancelRestart();
                break;
            }

            case "infotime":
            case "info":
            case "next": {
                if (!sender.hasPermission("royautorestart.infotime") && !sender.hasPermission("royautorestart.admin")) {
                    sender.sendMessage(lang.getMessage("messages.commands.no-permission"));
                    return true;
                }

                sender.sendMessage(plugin.getRestartManager().getNextRestartInfo());
                break;
            }

            case "help":
            default: {
                sendHelp(sender, label);
                break;
            }
        }

        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        LanguageManager lang = plugin.getLanguageManager();
        List<String> helpLines = lang.getMessageList("messages.commands.help", "%label%", label);
        if (helpLines.isEmpty()) {
            sender.sendMessage(lang.getMessage("messages.commands.invalid-command", "%label%", label));
        } else {
            for (String line : helpLines) {
                sender.sendMessage(line);
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("reload", "start", "cancel", "infotime", "help");
            List<String> results = new ArrayList<>();
            for (String sub : subcommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    results.add(sub);
                }
            }
            return results;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return Arrays.asList("30", "60", "120", "300");
        }

        return Collections.emptyList();
    }
}
