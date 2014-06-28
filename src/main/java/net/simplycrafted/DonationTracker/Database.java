package net.simplycrafted.DonationTracker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

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
public class Database {
    // MySQL database handler.
    // No other class should be talking to the database.
    // No other class should be constructing SQL statements.

    static private Connection db_conn;
    static private DonationTracker donationtracker;

    // We call this from the constructor, and whenever we find that the database has gone away
    public void Connect () {
        donationtracker.getLogger().info("Opening MySQL database connection");
        String hostname = donationtracker.getConfig().getString("mysql.hostname");
        int port = donationtracker.getConfig().getInt("mysql.port");
        String database = donationtracker.getConfig().getString("mysql.database");
        String user = donationtracker.getConfig().getString("mysql.user");
        String password = donationtracker.getConfig().getString("mysql.password");
        try {
            db_conn = DriverManager.getConnection(String.format("jdbc:mysql://%s:%s/%s?user=%s&password=%s", hostname, String.valueOf(port), database, user, password));
        } catch (Exception exception) {
            donationtracker.getLogger().info("DonationTracker requires a MySQL database. Couldn't get connected.");
            donationtracker.getLogger().info(exception.toString());
        }
    }

    public boolean connectionIsDead() {
        try {
            db_conn.createStatement().executeQuery("SELECT 1").close();
        } catch (Exception exception) {
            donationtracker.getLogger().info("Database connection went wrong");
            return true;
        }
        return false;
    }

    // Call this once, when the plugin is being disabled. Called statically from onDisable()
    public static void Disconnect () {
        try {
            donationtracker.getLogger().info("Closing MySQL database connection");
            db_conn.close();
        } catch (Exception exception) {
            donationtracker.getLogger().info("Tried to close the connection, but failed (possibly because it's already closed)");
        }
    }

    // Create any tables we need (if they don't exist)
    private void CreateTables() {
        Statement sql;
        String prefix;
        if (donationtracker.getConfig().isSet("mysql.prefix")) {
            prefix = donationtracker.getConfig().getString("mysql.prefix");
        }
        else {
            prefix = "";
        }
        try {
            sql = db_conn.createStatement();
            sql.executeUpdate("CREATE TABLE IF NOT EXISTS `"+prefix+"settings` (" +
                    "setting VARCHAR(10) primary key," +
                    "stringvalue VARCHAR(50)," +
                    "numericvalue INT(10)" +
                    ")");
            sql.executeUpdate("CREATE TABLE IF NOT EXISTS `"+prefix+"donations` (" +
                    "donationtime TIMESTAMP," +
                    "uuid INT(16)," +
                    "amount INT(10)" +
                    ")");
        } catch (SQLException exception) {
            donationtracker.getLogger().info(exception.toString());
        }
    }

    // Constructor
    public Database () {
        donationtracker = DonationTracker.getInstance();
        // Automatically call Connect() when class is instantiated, and
        // create tables. We assume that if the database connection is
        // already present, then the tables are there too.
        if (db_conn == null) {
            Connect();
            CreateTables();
        }
    }
}
