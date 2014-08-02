package net.simplycrafted.DonationTracker;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.UUID;

/**
 * Copyright © Brian Ronald
 * 28/06/14
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
public class CommandHandler implements CommandExecutor {

    String chatPrefix = "" + ChatColor.BOLD + ChatColor.GOLD + "[DC] " + ChatColor.RESET;
    DonationTracker donationtracker = DonationTracker.getInstance();

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Database database = new Database();

        if (command.getName().equalsIgnoreCase("donation")) {
            if (args.length == 2) {
                UUID uuid = null;
                Double amount;
                try {
                    // See if the argument is a UUID
                    uuid = UUID.fromString(args[0]);
                } catch (IllegalArgumentException e) {
                    // If that threw an error, assume it's a name
                    for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                        // Search all the known players
                        if (player.getName().equalsIgnoreCase(args[0])) {
                            uuid = player.getUniqueId();
                        }
                    }
                }
                try {
                    amount = Double.parseDouble(args[1]);
                } catch (IllegalArgumentException e) {
                    amount = 0.0;
                }
                if (uuid == null || amount <= 0.0) {
                    if (uuid == null) {
                        sender.sendMessage(chatPrefix + "Could not get UUID for " + args[0]);
                    }
                    if (amount <= 0.0) {
                        sender.sendMessage(chatPrefix + args[1] + " is not a valid positive number");
                    }
                } else {
                    database.recordDonation(uuid, amount);
                    sender.sendMessage(String.format(chatPrefix + "Logged $%.2f against UUID %s",amount,uuid.toString()));

                    // Get the donationthanks command from the config and, um, run it...
                    String thankCommand = donationtracker.getConfig().getString("donationthanks");
                    thankCommand = thankCommand.replaceFirst("PLAYER", donationtracker.getServer().getOfflinePlayer(uuid).getName()).replaceFirst("AMOUNT", amount.toString());
                    String tcarg0 = thankCommand.substring(0, thankCommand.indexOf(' '));
                    String[] tcargs = thankCommand.substring(thankCommand.indexOf(' ') + 1).split(" ");
                    // Have the plugin re-test all goals now
                    PluginCommand pluginCommand = donationtracker.getServer().getPluginCommand(tcarg0);
                    if (pluginCommand != null) {
                        pluginCommand.execute(donationtracker.getServer().getConsoleSender(), tcarg0, tcargs);
                    } else {
                        donationtracker.getLogger().info("Invalid command: " + tcarg0);
                    }
                    donationtracker.assess();
                }
            } else return false;
            return true;
        }

        if (command.getName().equalsIgnoreCase("donorgoal")) {
            Configuration config = donationtracker.getConfig();
            if (args.length == 0) {
                // List the goals
                sender.sendMessage(chatPrefix + "Donation goals:");
                for (String goal : config.getConfigurationSection("goals").getKeys(false)) {
                    sender.sendMessage(chatPrefix + String.format("Goal %s: $%.2f in %d days",
                            goal,
                            config.getDouble("goals."+goal+".amount"),
                            config.getInt("goals."+goal+".days"))
                    );
                }
            } else if (args.length == 1) {
                String goalName = args[0];
                ConfigurationSection goalConfig = config.getConfigurationSection("goals." + goalName);
                if (goalConfig != null) {
                    sender.sendMessage(chatPrefix + String.format("Goal %s: $%.2f in %d days",
                                    goalName,
                                    goalConfig.getDouble("amount"),
                                    goalConfig.getInt("days"))
                    );
                    for (String commandToEnable : goalConfig.getStringList("enable")) {
                        sender.sendMessage(chatPrefix + "Reward: " + ChatColor.WHITE + commandToEnable);
                    }
                    for (String commandToDisable : goalConfig.getStringList("disable")) {
                        sender.sendMessage(chatPrefix + "Withdraw: " + ChatColor.WHITE + commandToDisable);
                    }
                } else {
                    sender.sendMessage(chatPrefix + "Goal " + goalName + " not found");
                }
            } else if ((args.length > 1) && (args[1].toLowerCase().matches("^(amount|days|enable|disable|clear)$"))) {
                String goalName = args[0];
                ConfigurationSection goalConfig = config.getConfigurationSection("goals." + goalName);
                if (goalConfig == null) {
                }
                if (args.length > 2) {
                    if (args[1].equalsIgnoreCase("amount")) {
                        try {
                            int amount;
                            amount = Integer.parseInt(args[2]);
                            if (amount <= 0.0) {
                                sender.sendMessage(chatPrefix + "Amount must be a positive number");
                            } else {
                                goalConfig.set("amount", amount);
                                donationtracker.getConfig().set("goals." + goalName + ".amount", amount);
                                Goal goal = donationtracker.goals.get(goalName);
                                if (goal == null) {
                                    sender.sendMessage(chatPrefix + "Creating new goal: " + goalName);
                                    goal = new Goal(goalConfig);
                                    donationtracker.goals.put(goalName,goal);
                                    donationtracker.goalsBackwards.put(goalName,goal);
                                } else {
                                    goal.money = amount;
                                }
                            }
                        } catch (IllegalArgumentException e) {
                            sender.sendMessage(chatPrefix + "You must specify a valid number");
                        }
                    } else if (args[1].equalsIgnoreCase("days")) {
                        try {
                            Integer days = Integer.parseInt(args[2]);
                            if (days <= 0) {
                                sender.sendMessage(chatPrefix + "Amount must be a positive number");
                            } else {
                                goalConfig.set("days", days);
                                donationtracker.getConfig().set("goals." + goalName + ".days", days);
                                Goal goal = donationtracker.goals.get(goalName);
                                if (goal == null) {
                                    sender.sendMessage(chatPrefix + "Creating new goal: " + goalName);
                                    goal = new Goal(goalConfig);
                                    donationtracker.goals.put(goalName,goal);
                                    donationtracker.goalsBackwards.put(goalName,goal);
                                } else {
                                    goal.days = days;
                                }
                            }
                        } catch (IllegalArgumentException e) {
                            sender.sendMessage(chatPrefix + "You must specify a valid number");
                        }
                    } else if (args[1].equalsIgnoreCase("enable")) {

                    } else if (args[1].equalsIgnoreCase("disable")) {

                    } else if (args[1].equalsIgnoreCase("clear")) {

                    }
                } else {
                    if (args[1].equalsIgnoreCase("amount")) {
                        sender.sendMessage(chatPrefix + "You must specify an amount in dollars");
                        return true;
                    } else if (args[1].equalsIgnoreCase("days")) {
                        sender.sendMessage(chatPrefix + "You must specify a period in whole days");
                        return true;
                    } else if (args[1].equalsIgnoreCase("enable")) {
                        sender.sendMessage(chatPrefix + "You must specify a command to run when enabled");
                        return true;
                    } else if (args[1].equalsIgnoreCase("disable")) {
                        sender.sendMessage(chatPrefix + "You must specify a command to run when disabled");
                        return true;
                    } else if (args[1].equalsIgnoreCase("clear")) {
                        // Clear the whole goal
                        return true;
                    }
                }
            } else return false;
            return true;
        }

        if (command.getName().equalsIgnoreCase("ddbg")) {
            // Debugging and testing only - to be removed before release
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("advance")) {
                    if (args.length > 1) {
                        try {
                            database.dbg_advance(Integer.valueOf(args[1]));
                        } catch (Exception e) {
                            sender.sendMessage("Bad number.");
                        }
                        return true;
                    } else {
                        return true;
                    }
                } else if (args[0].equalsIgnoreCase("assess")) {
                    // Iterate over all the goals
                    donationtracker.assess();
                    return true;
                } else if (args[0].equalsIgnoreCase("withdraw")) {
                    // Iterate over all the goals
                    donationtracker.withdraw();
                    return true;
                } else if (args[0].equalsIgnoreCase("reload")) {
                    // Reload the plugin
                    donationtracker.reload();
                    return true;
                } else if (args[0].equalsIgnoreCase("save")) {
                    // Reload the plugin
                    donationtracker.saveConfig();
                    return true;                } else {
                    sender.sendMessage("Unknown debug command.");
                }
            } else {
                return false;
            }
        }

        // This won't be reached
        return false;
    }


}
