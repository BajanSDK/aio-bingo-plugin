package com.bajansdk.aiobingo.model;

public enum TileType {
    XP,                 // Gain X XP in a specific skill
    KILL_COUNT,         // Kill a specific NPC X times
    DROP,               // Receive a specific item drop
    RAID,               // Complete a raid X times
    COLLECTION_LOG,     // Unlock a collection log slot
    QUEST,              // Complete a specific quest
    ACHIEVEMENT_DIARY,  // Complete an achievement diary tier
    COMBAT_ACHIEVEMENT, // Complete a combat achievement
    PVP,                // Kill players in PvP
    FREE,               // Free space — auto-complete at start
    CUSTOM,             // Custom objective with manual verification
    CLUE_SCROLL,        // Complete clue scrolls (tracked via highscores)
    MINIGAME            // Minigame completions (tracked via highscores)
}
