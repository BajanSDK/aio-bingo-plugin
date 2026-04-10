package com.bajansdk.aiobingo;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("aiobingobajansdk")
public interface AioBingoConfig extends Config {

    @ConfigSection(
        name = "Connection",
        description = "API connection settings",
        position = 0
    )
    String connectionSection = "connection";

    @ConfigItem(
        keyName = "boardToken",
        name = "Bingo Board Token",
        description = "Token identifying the bingo board for this event",
        section = connectionSection,
        position = 1
    )
    default String boardToken() {
        return "";
    }

    @ConfigItem(
        keyName = "teamToken",
        name = "Team Token",
        description = "Token identifying your team on this bingo board",
        section = connectionSection,
        position = 2
    )
    default String teamToken() {
        return "";
    }

    @ConfigItem(
        keyName = "apiBaseUrl",
        name = "API Base URL",
        description = "Base URL of the AIO Bingo API (must use HTTPS in production)",
        section = connectionSection,
        position = 3
    )
    default String apiBaseUrl() {
        return "https://aiobingo.com";
    }

    @ConfigSection(
        name = "Tracking",
        description = "What game events to track and submit",
        position = 10
    )
    String trackingSection = "tracking";

    @ConfigItem(
        keyName = "enableEventTracking",
        name = "Enable Event Tracking",
        description = "Send game events to the AIO Bingo API (aiobingo.com). No data is sent unless this is enabled.",
        section = trackingSection,
        position = 10
    )
    default boolean enableEventTracking() {
        return false;
    }

    @ConfigItem(
        keyName = "autoSubmit",
        name = "Auto-Submit Events",
        description = "Automatically send game events to the API as they happen",
        section = trackingSection,
        position = 11
    )
    default boolean autoSubmit() {
        return true;
    }

    @ConfigItem(
        keyName = "trackDrops",
        name = "Track Item Drops",
        description = "Submit item drop / loot received events",
        section = trackingSection,
        position = 12
    )
    default boolean trackDrops() {
        return true;
    }

    @ConfigItem(
        keyName = "trackCollectionLog",
        name = "Track Collection Log",
        description = "Submit new collection log slot events",
        section = trackingSection,
        position = 13
    )
    default boolean trackCollectionLog() {
        return true;
    }

    @ConfigItem(
        keyName = "trackCombatAchievements",
        name = "Track Combat Achievements",
        description = "Submit combat achievement completion events",
        section = trackingSection,
        position = 14
    )
    default boolean trackCombatAchievements() {
        return true;
    }

    @ConfigItem(
        keyName = "trackPvp",
        name = "Track PvP Kills",
        description = "Submit player kill events from PvP combat",
        section = trackingSection,
        position = 15
    )
    default boolean trackPvp() {
        return true;
    }

    @ConfigSection(
        name = "Display",
        description = "Panel display settings",
        position = 20
    )
    String displaySection = "display";

    @ConfigItem(
        keyName = "refreshIntervalSeconds",
        name = "Refresh Interval (seconds)",
        description = "How often to pull updated board/leaderboard data from the API (minimum 10)",
        section = displaySection,
        position = 21
    )
    default int refreshIntervalSeconds() {
        return 30;
    }
}
