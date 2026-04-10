package com.bajansdk.aiobingo.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameEvent {
    private EventType eventType;
    private String playerName;

    /** ISO-8601 timestamp, e.g. "2025-01-01T12:00:00Z". */
    private String timestamp;

    // --- Drop / loot fields ---
    private int itemId;
    private String itemName;
    private int quantity;
    private int npcId;
    private String npcName;

    /** Type string from LootReceivedType, e.g. "NPC", "PICKPOCKET", "EVENT". */
    private String lootSourceType;

    // --- PvP ---
    private String opponentName;
}
