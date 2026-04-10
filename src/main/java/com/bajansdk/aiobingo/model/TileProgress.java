package com.bajansdk.aiobingo.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TileProgress {
    private String tileId;
    private long currentValue;
    private long requiredValue;
    private boolean completed;
    private String completedAt;
    private String completedBy;

    /** Tier completion flags. */
    private boolean tier1Reached;
    private boolean tier2Reached;
    private boolean tier3Reached;

    /** Computed 0.0–1.0 progress ratio for display. */
    public double getProgressRatio() {
        if (completed) return 1.0;
        if (requiredValue <= 0) return 0.0;
        return Math.min(1.0, (double) currentValue / requiredValue);
    }

    /** Returns the highest tier reached (0 if none). */
    public int getCurrentTier() {
        if (tier3Reached) return 3;
        if (tier2Reached) return 2;
        if (tier1Reached) return 1;
        return 0;
    }
}
