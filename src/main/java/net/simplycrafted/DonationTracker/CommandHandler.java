package net.simplycrafted.DonationTracker;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

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

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Database database = new Database();
        UUID uuid = null;
        Double amount;
        if (command.getName().equalsIgnoreCase("donation")) {
            if (args.length == 2) {
                try {
                    // See if the argument is a UUID
                    uuid = UUID.fromString(args[0]);
                } catch (IllegalArgumentException exception) {
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
                } catch (IllegalArgumentException exception) {
                    amount = 0.0;
                }
                if (uuid == null || amount <= 0.0) {
                    if (uuid == null) {
                        sender.sendMessage("Could not get UUID for " + args[0]);
                    }
                    if (amount <= 0.-0) {
                        sender.sendMessage(args[1] + " is not a valid positive number");
                    }
                } else {
                    database.Record(uuid, amount);
                    sender.sendMessage(String.format("Logged $%10.2f against UUID %s",amount,uuid.toString()));
                }
            } else return false;
        }
        return true;
    }
}
