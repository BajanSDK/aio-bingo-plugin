package com.bajansdk.aiobingo.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class BingoTile {
    /** Unique identifier for this tile on the board. */
    private String id;

    /** Display name shown in the grid cell. */
    private String title;

    /** Full description of the requirement. */
    private String description;

    /** Category of tile — determines which game events contribute to it. */
    private TileType tileType;

    /**
     * Target value for completion. Semantics depend on tileType:
     *   XP          → total XP to gain in the specified skill
     *   KILL_COUNT  → number of kills required
     *   DROP        → quantity of the item required
     *   RAID        → number of completions required
     *   others      → 1 (boolean completion)
     */
    private long requirement;

    /**
     * Type-specific parameters, e.g.:
     *   XP          → {"skill": "Agility"}
     *   KILL_COUNT  → {"npc": "Zulrah", "npc_id": "2042"}
     *   DROP        → {"item": "Twisted bow", "item_id": "20997", "npc": "Chambers of Xeric"}
     *   RAID        → {"raid": "CHAMBERS_OF_XERIC"}
     *   COLLECTION  → {"item": "Sanguinesti staff", "item_id": "22481"}
     */
    private Map<String, String> parameters;

    /** Position in the bingo grid (0-indexed). */
    private int row;
    private int col;

    /** Scoring — base points for non-tiered tiles. */
    private int basePoints;

    /** Tier thresholds (null if non-tiered). */
    private Integer tier1Threshold;
    private Integer tier2Threshold;
    private Integer tier3Threshold;

    /** Points awarded per tier. */
    private int tier1Points;
    private int tier2Points;
    private int tier3Points;

    /** Set by the API when this tile has been completed by any team member. */
    private boolean completed;
    private String completedAt;
    private String completedBy;

    /** Whether this tile uses tier-based scoring. */
    public boolean hasTiers() {
        return tier1Threshold != null;
    }

    /** Returns the highest configured tier number (0 if no tiers). */
    public int getMaxTier() {
        if (tier3Threshold != null) return 3;
        if (tier2Threshold != null) return 2;
        if (tier1Threshold != null) return 1;
        return 0;
    }
}
