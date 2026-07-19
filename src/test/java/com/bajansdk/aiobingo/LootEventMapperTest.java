package com.bajansdk.aiobingo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.bajansdk.aiobingo.model.EventType;
import com.bajansdk.aiobingo.model.GameEvent;
import java.util.Arrays;
import java.util.List;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.http.api.loottracker.LootRecordType;
import org.junit.Test;

public class LootEventMapperTest {

    private static final String PLAYER = "BajanSDK";
    private static final String TIMESTAMP = "2026-07-19T12:00:00Z";

    @Test
    public void mapsNpcLootWithCanonicalIdsAndSourceMetadata() {
        LootReceived loot = loot(
            "Branda the Fire Queen",
            LootRecordType.NPC,
            Arrays.asList(new ItemStack(31042, 1), new ItemStack(995, 1250)),
            1,
            14148
        );

        List<GameEvent> events = LootEventMapper.map(loot, PLAYER, TIMESTAMP, id -> "Item " + id);

        assertEquals(2, events.size());
        GameEvent unique = events.get(0);
        assertEquals(EventType.ITEM_DROP, unique.getEventType());
        assertEquals(31042, unique.getItemId());
        assertEquals(1, unique.getQuantity());
        assertEquals("Item 31042", unique.getItemName());
        assertEquals("NPC", unique.getLootSourceType());
        assertEquals(Integer.valueOf(14148), unique.getNpcId());
        assertEquals("Branda the Fire Queen", unique.getNpcName());
        assertEquals("Branda the Fire Queen", unique.getMetadata().get("loot_source_name"));
        assertEquals("1", unique.getMetadata().get("loot_event_amount"));

        assertEquals(995, events.get(1).getItemId());
        assertEquals(1250, events.get(1).getQuantity());
    }

    @Test
    public void mapsRaidAndChestEventsThroughTheSameGenericPath() {
        for (String source : Arrays.asList(
            "Chambers of Xeric",
            "Theatre of Blood",
            "Tombs of Amascut",
            "Barrows",
            "Lunar Chest",
            "Fortis Colosseum"
        )) {
            LootReceived loot = loot(
                source,
                LootRecordType.EVENT,
                List.of(new ItemStack(20997, 1)),
                1,
                null
            );

            List<GameEvent> events = LootEventMapper.map(loot, PLAYER, TIMESTAMP, id -> "Twisted bow");

            assertEquals(source, 1, events.size());
            GameEvent event = events.get(0);
            assertEquals(source, "EVENT", event.getLootSourceType());
            assertEquals(source, source, event.getMetadata().get("loot_source_name"));
            assertNull(source, event.getNpcId());
            assertNull(source, event.getNpcName());
        }
    }

    @Test
    public void acceptsPickpocketAndUnknownNonPlayerLoot() {
        for (LootRecordType type : Arrays.asList(LootRecordType.PICKPOCKET, LootRecordType.UNKNOWN)) {
            LootReceived loot = loot("Reward", type, List.of(new ItemStack(2577, 1)), 1, null);

            List<GameEvent> events = LootEventMapper.map(loot, PLAYER, TIMESTAMP, id -> "Ranger boots");

            assertEquals(type.name(), 1, events.size());
            assertEquals(type.name(), events.get(0).getLootSourceType());
        }
    }

    @Test
    public void ignoresPlayerLoot() {
        LootReceived loot = loot(
            "Other player",
            LootRecordType.PLAYER,
            List.of(new ItemStack(4151, 1)),
            1,
            null
        );

        assertTrue(LootEventMapper.map(loot, PLAYER, TIMESTAMP, id -> "Abyssal whip").isEmpty());
    }

    @Test
    public void filtersInvalidStacksAndDoesNotMultiplyGroupedQuantities() {
        LootReceived loot = loot(
            "Grouped reward",
            LootRecordType.EVENT,
            Arrays.asList(
                new ItemStack(-1, 1),
                new ItemStack(995, 0),
                new ItemStack(995, -1),
                new ItemStack(995, 250)
            ),
            4,
            null
        );

        List<GameEvent> events = LootEventMapper.map(loot, PLAYER, TIMESTAMP, id -> "Coins");

        assertEquals(1, events.size());
        assertEquals(250, events.get(0).getQuantity());
        assertEquals("4", events.get(0).getMetadata().get("loot_event_amount"));
    }

    @Test
    public void missingItemNameDoesNotLoseCanonicalDrop() {
        LootReceived loot = loot(
            "Barrows",
            LootRecordType.EVENT,
            List.of(new ItemStack(4708, 1)),
            1,
            null
        );

        List<GameEvent> events = LootEventMapper.map(loot, PLAYER, TIMESTAMP, id -> {
            throw new IllegalStateException("composition unavailable");
        });

        assertEquals(1, events.size());
        assertEquals(4708, events.get(0).getItemId());
        assertEquals("", events.get(0).getItemName());
    }

    private static LootReceived loot(
        String name,
        LootRecordType type,
        List<ItemStack> items,
        int amount,
        Object metadata
    ) {
        return new LootReceived(name, -1, type, items, amount, metadata);
    }
}
