package com.bajansdk.aiobingo.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class BingoBoard {
    private String boardToken;
    private String name;
    private String description;

    /** Width of the grid (number of columns). */
    private int gridWidth;

    /** Height of the grid (number of rows). */
    private int gridHeight;

    /** All tiles ordered by row then column. */
    private List<BingoTile> tiles;

    private String startDate;
    private String endDate;
    private boolean active;

    /** Returns the tile at a given (row, col), or null if not found. */
    public BingoTile getTileAt(int row, int col) {
        if (tiles == null) return null;
        return tiles.stream()
            .filter(t -> t.getRow() == row && t.getCol() == col)
            .findFirst()
            .orElse(null);
    }

    public int totalTiles() {
        return tiles == null ? 0 : tiles.size();
    }
}
