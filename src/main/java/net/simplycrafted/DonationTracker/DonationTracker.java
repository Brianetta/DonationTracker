package net.simplycrafted.DonationTracker;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

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
    private HashMap <String, Goal> goals;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        donationtracker = this;
        // Bail out now if we have no database
        Database dbtest = new Database();
        if (dbtest.connectionIsDead()) return;
        getCommand("donation").setExecutor(new CommandHandler());
        getCommand("donorgoal").setExecutor(new CommandHandler());
        // Load the goals from the config file
        ConfigurationSection configurationSection;
        for (String key : getConfig().getKeys(false)) {
            configurationSection = getConfig().getConfigurationSection(key);
            goals.put(key, new Goal(configurationSection));
        }
    }

    @Override
    public void onDisable() {
        Database.disconnect();
    }

    // Lets other classes get a reference to our instance
    public static DonationTracker getInstance() {
        return donationtracker;
    }
}