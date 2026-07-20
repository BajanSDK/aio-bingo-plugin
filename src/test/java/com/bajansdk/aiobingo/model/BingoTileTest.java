package com.bajansdk.aiobingo.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BingoTileTest {
    @Test
    public void tierProgressUsesNextTierRange() {
        BingoTile tile = new BingoTile();
        tile.setTier1Threshold(10);
        tile.setTier2Threshold(100);
        tile.setTier3Threshold(250);

        assertEquals(10, tile.getNextTierThreshold(0));
        assertEquals(100, tile.getNextTierThreshold(1));
        assertEquals(250, tile.getNextTierThreshold(2));
        assertEquals(0, tile.getNextTierThreshold(3));

        assertEquals(0.5, tile.getTierProgressRatio(5, 0), 0.0001);
        assertEquals(88.0 / 90.0, tile.getTierProgressRatio(98, 1), 0.0001);
        assertEquals(0.5, tile.getTierProgressRatio(175, 2), 0.0001);
        assertEquals(1.0, tile.getTierProgressRatio(250, 3), 0.0001);
    }

    @Test
    public void tierProgressIsClamped() {
        BingoTile tile = new BingoTile();
        tile.setTier1Threshold(10);
        tile.setTier2Threshold(100);

        assertEquals(0.0, tile.getTierProgressRatio(5, 1), 0.0001);
        assertEquals(1.0, tile.getTierProgressRatio(150, 1), 0.0001);
    }
}
