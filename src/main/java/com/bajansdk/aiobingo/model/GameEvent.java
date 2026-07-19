package com.bajansdk.aiobingo.model;

import java.util.Map;
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
    private Integer npcId;
    private String npcName;

    /** Type string from LootReceivedType, e.g. "NPC", "PICKPOCKET", "EVENT". */
    private String lootSourceType;

    /** Additional source details accepted by the API's metadata field. */
    private Map<String, String> metadata;

    // --- PvP ---
    private String opponentName;
}
