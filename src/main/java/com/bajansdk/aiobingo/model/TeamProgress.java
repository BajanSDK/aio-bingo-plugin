package com.bajansdk.aiobingo.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class TeamProgress {
    private String teamToken;
    private String teamName;
    private int completedTiles;
    private int totalTiles;
    private int totalPoints;

    /** Per-tile progress details. */
    private List<TileProgress> tileProgress;

    /** Bingo lines completed (row, column, diagonal). */
    private int linesCompleted;

    /** Whether a full blackout (every tile complete) has been achieved. */
    private boolean blackout;

    public double getCompletionRatio() {
        if (totalTiles <= 0) return 0.0;
        return (double) completedTiles / totalTiles;
    }

    /** Returns the TileProgress for a given tile ID, or null. */
    public TileProgress getProgressFor(String tileId) {
        if (tileProgress == null) return null;
        return tileProgress.stream()
            .filter(tp -> tileId.equals(tp.getTileId()))
            .findFirst()
            .orElse(null);
    }
}
