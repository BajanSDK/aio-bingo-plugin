package com.bajansdk.aiobingo.ui;

import net.runelite.client.ui.ColorScheme;

import java.awt.Color;
import java.util.Map;

/**
 * Shared design tokens for the AIO Bingo plugin UI.
 * Uses RuneLite's native ColorScheme as the base so the panel feels at home
 * alongside other plugins. Per-tile-type accent colours are retained for the
 * 2px top stripe — the only place colour is applied per type.
 */
public final class BingoColors {

    private BingoColors() {}

    // ── Core backgrounds — RuneLite native ──────────────────────────────────
    public static final Color SURFACE   = ColorScheme.DARK_GRAY_COLOR;    // #3c3f41
    public static final Color SURFACE_2 = ColorScheme.DARKER_GRAY_COLOR;  // #2c2f31
    public static final Color SURFACE_3 = new Color(0x1e, 0x20, 0x22);    // deepest

    // ── Borders ─────────────────────────────────────────────────────────────
    public static final Color BORDER        = new Color(0x55, 0x55, 0x55);
    public static final Color BORDER_BRIGHT = new Color(0x78, 0x78, 0x78);

    // ── Gold — kept for header title only ────────────────────────────────────
    public static final Color GOLD       = new Color(0xc8, 0xa8, 0x4b);
    public static final Color GOLD_LIGHT = new Color(0xe8, 0xc8, 0x70);
    public static final Color GOLD_DIM   = new Color(0x60, 0x50, 0x20);

    // ── Text ────────────────────────────────────────────────────────────────
    public static final Color PARCHMENT       = Color.WHITE;
    public static final Color PARCHMENT_DIM   = ColorScheme.LIGHT_GRAY_COLOR; // #a0a0a0
    public static final Color PARCHMENT_FAINT = new Color(0x66, 0x66, 0x66);

    // ── State ────────────────────────────────────────────────────────────────
    public static final Color GREEN       = new Color(0x1d, 0x7a, 0x40);
    public static final Color GREEN_LIGHT = new Color(0x28, 0xb0, 0x55);
    public static final Color RED         = new Color(0xe0, 0x55, 0x55);
    public static final Color AMBER       = GOLD;

    // ── Tier ────────────────────────────────────────────────────────────────
    public static final Color TIER_BRONZE = new Color(0x8b, 0x69, 0x2e);
    public static final Color TIER_SILVER = new Color(0x90, 0x90, 0x90);
    public static final Color TIER_GOLD   = GOLD;

    // ── Rank (leaderboard) ──────────────────────────────────────────────────
    public static final Color RANK_1 = GOLD;
    public static final Color RANK_2 = new Color(0x90, 0x90, 0x90);
    public static final Color RANK_3 = new Color(0x8b, 0x69, 0x2e);
    public static final Color RANK_N = PARCHMENT_DIM;

    // ── Tile-type accent colours (matches web BingoGrid.tsx) ────────────────
    private static final Map<String, Color> TILE_ACCENT = Map.ofEntries(
        Map.entry("XP",                 new Color(0x5a, 0x9e, 0xd4)),
        Map.entry("KILL_COUNT",         new Color(0xe0, 0x55, 0x55)),
        Map.entry("DROP",               new Color(0xb8, 0x55, 0xdd)),
        Map.entry("RAID",               new Color(0xe0, 0x95, 0x35)),
        Map.entry("COLLECTION_LOG",     new Color(0x35, 0xaa, 0xaa)),
        Map.entry("QUEST",              new Color(0xcc, 0xcc, 0x44)),
        Map.entry("ACHIEVEMENT_DIARY",  new Color(0x44, 0xcc, 0x66)),
        Map.entry("COMBAT_ACHIEVEMENT", new Color(0xdd, 0x44, 0x77)),
        Map.entry("PVP",                new Color(0xe0, 0x44, 0x88)),
        Map.entry("FREE",               new Color(0x33, 0xcc, 0x88)),
        Map.entry("CLUE_SCROLL",        new Color(0xdd, 0xbb, 0x44)),
        Map.entry("MINIGAME",           new Color(0x55, 0xcc, 0xcc)),
        Map.entry("CUSTOM",             new Color(0xaa, 0xaa, 0xaa))
    );

    // ── Tile-type icons — Unicode glyphs rendered with Dialog font ──────────
    private static final Map<String, String> TILE_ICONS = Map.ofEntries(
        Map.entry("XP",                 "\u2726"), // ✦
        Map.entry("KILL_COUNT",         "\u2620"), // ☠
        Map.entry("DROP",               "\u25c8"), // ◈
        Map.entry("RAID",               "\u2694"), // ⚔
        Map.entry("COLLECTION_LOG",     "\u25a3"), // ▣
        Map.entry("QUEST",              "\u25c9"), // ◉
        Map.entry("ACHIEVEMENT_DIARY",  "\u25b2"), // ▲
        Map.entry("COMBAT_ACHIEVEMENT", "\u2605"), // ★
        Map.entry("PVP",                "\u2720"), // ✠
        Map.entry("FREE",               "\u25c6"), // ◆
        Map.entry("CLUE_SCROLL",        "\u2709"), // ✉
        Map.entry("MINIGAME",           "\u26a1"), // ⚡
        Map.entry("CUSTOM",             "\u2699")  // ⚙
    );

    // ── Tile-type short labels ───────────────────────────────────────────────
    private static final Map<String, String> TILE_LABELS = Map.ofEntries(
        Map.entry("XP",                 "XP"),
        Map.entry("KILL_COUNT",         "KC"),
        Map.entry("DROP",               "DROP"),
        Map.entry("RAID",               "RAID"),
        Map.entry("COLLECTION_LOG",     "LOG"),
        Map.entry("QUEST",              "QUEST"),
        Map.entry("ACHIEVEMENT_DIARY",  "DIARY"),
        Map.entry("COMBAT_ACHIEVEMENT", "CA"),
        Map.entry("PVP",                "PVP"),
        Map.entry("FREE",               "FREE"),
        Map.entry("CLUE_SCROLL",        "CLUE"),
        Map.entry("MINIGAME",           "MINI"),
        Map.entry("CUSTOM",             "CUSTOM")
    );

    public static Color tileAccent(String tileType) {
        return TILE_ACCENT.getOrDefault(tileType, PARCHMENT_DIM);
    }

    public static String tileIcon(String tileType) {
        return TILE_ICONS.getOrDefault(tileType, "\u25c6");
    }

    public static String tileLabel(String tileType) {
        return TILE_LABELS.getOrDefault(tileType, tileType);
    }

    public static Color rankColor(int rank) {
        switch (rank) {
            case 1:  return RANK_1;
            case 2:  return RANK_2;
            case 3:  return RANK_3;
            default: return RANK_N;
        }
    }

    public static Color tierColor(int tier) {
        switch (tier) {
            case 3:  return TIER_GOLD;
            case 2:  return TIER_SILVER;
            case 1:  return TIER_BRONZE;
            default: return SURFACE;
        }
    }
}
