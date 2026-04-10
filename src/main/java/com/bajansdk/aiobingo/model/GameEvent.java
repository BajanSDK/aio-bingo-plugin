package com.bajansdk.aiobingo.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class GameEvent {
    private EventType eventType;
    private String playerName;

    /** ISO-8601 timestamp, e.g. "2025-01-01T12:00:00Z". */
    private String timestamp;

    // --- XP gain fields ---
    private String skillName;
    private int xpGained;
    private int totalXp;

    // --- Drop / loot fields ---
    private int itemId;
    private String itemName;
    private int quantity;

    // --- Source NPC / activity ---
    private int npcId;
    private String npcName;

    /** Type string from LootReceivedType, e.g. "NPC", "PICKPOCKET", "EVENT". */
    private String lootSourceType;

    // --- Kill count fields ---
    private int killCount;

    // --- Raid fields ---
    /** One of: CHAMBERS_OF_XERIC, THEATRE_OF_BLOOD, TOMBS_OF_AMASCUT */
    private String raidType;

    // --- Collection log ---
    private String collectionLogItem;
    private int collectionLogItemId;

    // --- Quest / diary / CA ---
    private String objectiveName;

    // --- PvP ---
    private String opponentName;

    /** Arbitrary extra metadata for CUSTOM tile types. */
    private Map<String, String> metadata;
}
