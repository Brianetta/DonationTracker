package net.simplycrafted.DonationTracker;

import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.Set;

/**
 * Copyright © Brian Ronald
 * 13/07/14
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

// Goal class - one is instantiated for each donation goal. A donation
// goal has a number of days and an amount; if the amount is reached
// within the number of days, the effects of the goal are enabled. If
// the number falls short, the effects are abandoned.
public class Goal {
    static private DonationTracker donationtracker;
    private int days;
    private int money;
    private String name;

    public String getName() {
        return name;
    }

    private class Command {
        // Simple container for a command name and its arguments
        public String arg0;
        public String[] args;
    }

    private Set<Command> commandsOnEnabled = new HashSet<>();
    private Set<Command> commandsOnDisabled = new HashSet<>();

    // Constructor
    public Goal(ConfigurationSection goalConfig) {
        // Set the DonationTracker instance variable
        donationtracker = DonationTracker.getInstance();

        // Re-usable Command variable
        Command command;

        days = goalConfig.getInt("days");
        money = goalConfig.getInt("amount");
        name = goalConfig.getName();

        donationtracker.getLogger().info("Instantiating goal: " + name);

        // Build lists of Commands to be run when enabled
        for (String commandString : goalConfig.getStringList("enable")) {
            command = new Command();
            command.arg0 = commandString.substring(0, commandString.indexOf(' '));
            command.args = commandString.substring(commandString.indexOf(' ') + 1).split(" ");
            commandsOnEnabled.add(command);
        }
        // Build lists of Commands to be run when disabled
        for (String commandString : goalConfig.getStringList("disable")) {
            command = new Command();
            command.arg0 = commandString.substring(0, commandString.indexOf(' '));
            command.args = commandString.substring(commandString.indexOf(' ') + 1).split(" ");
            commandsOnDisabled.add(command);
        }
    }

    public void enable() {
        Database database = new Database();
        // Check whether rewards have been enabled
        if (database.rewardsAreEnabled(this.name)) {
            return;
        }
        // Enable rewards
        donationtracker.getLogger().info("Enabling rewards: " + name);
        for (Command command : commandsOnEnabled) {
                PluginCommand pluginCommand = donationtracker.getServer().getPluginCommand(command.arg0);
                if (pluginCommand != null) {
                    pluginCommand.execute(donationtracker.getServer().getConsoleSender(), command.arg0, command.args);
                } else {
                    donationtracker.getLogger().info("Invalid command: " + command.arg0);
                }
            }
        // Record that rewards have been enabled
        database.recordReward(this.name,true);
    }

    public void abandon() {
        Database database = new Database();
        // Check whether rewards have been disabled
        if (!database.rewardsAreEnabled(this.name)) {
            return;
        }
        // Disable rewards
        donationtracker.getLogger().info("Disabling rewards: " + name);
        for (Command command : commandsOnDisabled) {
            PluginCommand pluginCommand = donationtracker.getServer().getPluginCommand(command.arg0);
            if (pluginCommand != null) {
                pluginCommand.execute(donationtracker.getServer().getConsoleSender(),command.arg0,command.args);
            } else {
                donationtracker.getLogger().info("Invalid command: " + command.arg0);
            }
        }
        // Record that rewards have been disabled
        database.recordReward(this.name,false);
    }

    public boolean reached() {
        // Ask Database whether this goal has been reached
        Database database = new Database();
        return database.isGoalReached(days,money);
    }
}