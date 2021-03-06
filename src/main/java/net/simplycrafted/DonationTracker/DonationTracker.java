package net.simplycrafted.DonationTracker;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.*;

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
public class DonationTracker extends JavaPlugin {

    // Our only instance
    private static DonationTracker donationtracker;

    // List of instantiated goals
    Map<String,Goal> goals;
    Map<String,Goal> goalsBackwards;

    // Determine which goals need to be rewarded or otherwise
    public void assess(boolean atDonationTime) {
        for(String key: goals.keySet())
        {
            Goal goal = goals.get(key);
            if (goal.reached()) {
                if (atDonationTime) {
                    goal.ondonate();
                }
                goal.enable();
            }
        }
        for(String key: goalsBackwards.keySet())
        {
            Goal goal = goalsBackwards.get(key);
            if (!goal.reached()) {
                goal.abandon();
            }
        }
    }

    // Withdraw all rewards (used when closing down the plugin).
    public void withdraw() {
        for(String key: goalsBackwards.keySet())
        {
            Goal goal = goalsBackwards.get(key);
            if (goal.reached()) {
                getLogger().info("Abandoning " + goal.getName());
                goal.abandon();
            }
        }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        donationtracker = this;
        goals = new TreeMap<>();
        goalsBackwards = new TreeMap<>(Collections.reverseOrder());
        // Bail out now if we have no database
        Database dbtest = new Database();
        if (dbtest.connectionIsDead()) return;
        CommandHandler commandHandler = new CommandHandler();
        getCommand("donation").setExecutor(commandHandler);
        getCommand("donorgoal").setExecutor(commandHandler);
        getCommand("donationpool").setExecutor(commandHandler);
        getCommand("dgadmin").setExecutor(commandHandler);

        // Load the goals from the config file
        ConfigurationSection goalConfig;
        ConfigurationSection goalsConfig = getConfig().getConfigurationSection("goals");
        for (String key : goalsConfig.getKeys(false)) {
            goalConfig = goalsConfig.getConfigurationSection(key);
            Goal goal = new Goal (goalConfig);
            if (goal.reached()) {
                getLogger().info("...reached");
            }
            goals.put(key, goal);
            goalsBackwards.put(key, goal);
        }

        // Schedule a checker to examine these goals periodically
        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(this,new Runnable() {
            @Override
            public void run() {
                assess(false);
            }
        },100,getConfig().getInt("period"));
    }

    @Override
    public void onDisable() {
        withdraw();
        Database.disconnect();
    }

    // Lets other classes get a reference to our instance
    public static DonationTracker getInstance() {
        return donationtracker;
    }

    public void reload() {
        // Withdraw all goal rewards
        withdraw();
        // Re-initialise the goal hashes
        goals = new TreeMap<>();
        goalsBackwards = new TreeMap<>(Collections.reverseOrder());
        // Grab a new config
        reloadConfig();
        // Re-populate the goal hashes
        ConfigurationSection goalConfig;
        ConfigurationSection goalsConfig = getConfig().getConfigurationSection("goals");
        for (String key : goalsConfig.getKeys(false)) {
            goalConfig = goalsConfig.getConfigurationSection(key);
            Goal goal = new Goal (goalConfig);
            if (goal.reached()) {
                getLogger().info("...reached");
            }
            goals.put(key, goal);
            goalsBackwards.put(key, goal);
        }
        // re-assess all goals, and reward
        assess(false);
    }
}