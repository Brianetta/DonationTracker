package net.simplycrafted.DonationTracker;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;

import java.util.Iterator;
import java.util.Map;
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

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Database database = new Database();
        UUID uuid = null;
        Double amount;

        if (command.getName().equalsIgnoreCase("donation")) {
            if (args.length == 2) {
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
                }
            } else return false;
            return true;
        }

        if (command.getName().equalsIgnoreCase("donorgoal")) {
            Configuration config = DonationTracker.getInstance().getConfig();
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
                if (config.getConfigurationSection("goals."+args[0]) != null) {
                    sender.sendMessage(chatPrefix + String.format("Goal %s: $%.2f in %d days",
                                    args[0],
                                    config.getDouble("goals."+args[0]+".amount"),
                                    config.getInt("goals."+args[0]+".days"))
                    );
                    // Add effects output here
                } else {
                    sender.sendMessage(chatPrefix + "Goal " + args[0] + " not found");
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
                    DonationTracker.getInstance().assess();
                    return true;
                } else {
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
