package com.bajansdk.aiobingo.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LeaderboardEntry {
    private int rank;
    private String teamId;
    private String teamName;
    private int completedTiles;
    private int totalTiles;
    private int totalPoints;
    private int linesCompleted;
    private boolean blackout;
    private String lastActivity;

    public double getCompletionRatio() {
        if (totalTiles <= 0) return 0.0;
        return (double) completedTiles / totalTiles;
    }
}
