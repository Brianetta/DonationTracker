**Please note: This repository was private, for use by a specific server. This documentation is brief, and was intended to be read by the staff of that server, rather than the general public. As a result, ionformation may be missing, or may refer to specifics details of a private server.**

The plugin was never installed on the server, and is provided here as-is in the hopes that it proves useful. I am releasing it under the GNU General Public License, version 3.0 or (at your option) any later version.
---

# DonationTracker #

A Bukkit plugin for handling pool-based donations and rewards, in line with recent clarfification of the Mojang EULA with respect to the funding of Minecraft servers by their players.

## Purpose (SimplyCrafted) ##

Since we will no longer be using donations to provide perks to individual owners, but will instead be rewarding the entire player base with respect to the total amount donated over a period of time, we needed a way to track the cumulative donations made by players

### Timescale ###

Justin is unwilling to embrace the New World Order. He is planning to continue with the old funding model for as long as he can. This means that we have a little lee-way regarding the 1st August deadline for getting this plugin tested. If we don't get closed down by lawyers, that is.

## Mechanism ##

Donations are made through the Enjin shop, and the Enjin plugin runs a specified command. That command logs the donation amount into a database, and tells the plugin to recalculate the pool.

Every so often (in the default config, every 12,000 ticks, which is ten minutes if there's no lag) the plugin will go through the goals that have been configured, and using the information about donations in the database, decide which to enable or disable.

Each goal has a target amount, and a number of days. If the target amount has been donated between that many days ago and the present, the goal is considered reached. If that number is zero, the plugin acts as if it was infinitely large - that is to say, all donations ever are counted for that goal.

Each goal also has two lists of commands - those to run when the goal is reached, and those to run when the goal is lost. The commands can be any commands that are provided by a plugin (unfortunately, no built-in server commands can be used).

The plugin will also store in the database information about which goals have been reached, so that goal setup commands aren't run multiple times.

## Commands ##

There are currently three primary commands (although this will change as development progresses).

### /donation ###

This command is intended to be run by the Enjin plugin when somebody donates through the Enjin shop. It can also be run on demand by players, for example of an out-of-band cash payment is made directly to Justin, we can use this command from the console to record it. It takes two arguments.

The first is the name or UUID of a player. If it's a name, then it must be of a player who has previously joined the server, so that the plugin can find the UUID.

The second is the amount donated by that player, in dollars (or dollars and cents) without the dollar sign. It must be a positive number, otherwise it will be rejected.

**Examples:**

    /donation JustInTime3371 50
    
    /donation ffb9928c-d518-4e9b-9618-528cd85df5b7 35

### /donorgoal ###

This command lists goals, and if given a goal name will list the commands that goal will run, and allow them to be edited.

**Examples**

List all goals:

    /donorgoal
    
Show details of a goal called chestshops:

    /donorgoal chestshops

Alter the number of days of the chestshops goal to 30:

    /donorgoal chestshops days 30

Alter the donation amount required for the chestshops goal to $50:

    /donorgoal chestshops amount 50

Add an enable command to chestshops, announcing it:

    /donorgoal chestshops enable bc Chestshops enabled!

Add a disable command to chestshops, announcing its removal:

    /donorgoal chestshops disable bc Chestshops disabled :-(

Totally delete the chestshops goal:

    /donorgoal chestshops clear

**You can create a new goal by specifying either a donation amount or a number of days.**

### /dgadmin ###

This command has several sub-commands, which are used for testing. Some of these commands will remain in the plugin (perhaps after being renamed) while others will be removed.

**Examples:**

    /dgadmin advance 1
    /dgadmin advance 30    

This command simulated the advancing of time through multiples of 24 hours, by subtracting the number of days from the times of all the donations in the database. They all move that many days back in time, allowing testers to test how goals and donations behave.

    /dgadmin assess

This command causes the plugin to re-assess all the goals. It is run every ten minutes or so anyway, but is useful after advancing the donations.

    /dgadmin withdraw

This command causes all the goals to be abandoned, as if there have been no donations at all. Each goal that has been reached will be considered lost, and its disable commands will be run. It can be un-done with `/dgadmin assess`, since it doesn't touch the actual donations. It is run whenever the plugin is disabled, to allow changes to be made to the configuration in safety.

    /dgadmin reload

This command causes the plugin to unload all goals, reload the configuration from disk, and re-initialise all internal storage with new goal information from the newly loaded configuration.

    /dgadmin save

This immediately writes the plugin's config file to disk. It shouldn't be needed, since the plugin does this itself every time it makes changes.

### /donationpool ###

This command shows how much has been donated to the server.

**Examples:**

    /donationpool

Show total donations over the last 30 days (by default).

    /donationpool 5
    
Show total donations over the last 5 days.

    /donationpool 0

Show total donations since records began.

### Future commands ###

We need commands to see who was the biggest donator over the last *n* days.

We also need additional features, such as signwriting or announcements for individual or cumulative donations.

## Configuration ##

The config looks like this by default:

    mysql:
        hostname: localhost
        port: 3306
        database: donationtracker
        user: donationtracker
        password: secret
    period: 12000
    defaultgoaldays: 30
    donationthanks: bc Thanks to PLAYER for donating $AMOUNT to the server!
    goals:

The `mysql` section is configured as required. The period is the number of ticks between goal assessments (12,000 is 10 minutes at full TPS).

The goals need to be added. Here's what the config looks like with two goals added. One provides /fly if there have been $30 of donations in the past day, the other offers chest shops if there have been $50 of donations over the past 30 days:

    mysql:
        hostname: localhost
        port: 3306
        database: donationtracker
        user: donationtracker
        password: secret
    period: 12000
    defaultgoaldays: 30
    donationthanks: bc Thanks to PLAYER for donating $AMOUNT to the server!
    goals:
        flight:
            days: 1
            amount: 30
            enable:
                - bc Fly mode enabled!
                - pex group Player add essentials.fly
                - pex group Player add essentials.fly.safelogin
            disable:    
                - bc Fly mode disabled!
                - pex group Player remove essentials.fly
                - pex group Player remove essentials.fly.safelogin
            ondonate:
                - bc This was due to a donation that just happened.
        chestshop:
            days: 30
            amount: 50
            enable:
                - bc Chest shops enabled!
                - pex group Player add ChestShop.shop.create.*
            disable:    
                - bc Chest shops disabled!
                - pex group Player remove ChestShop.shop.create.*

## Permissions ##

`donationtracker.record` lets a player record donations using `/donation`.

`donationtracker.goals` lets a player view or amend goals.

Console has everything.
