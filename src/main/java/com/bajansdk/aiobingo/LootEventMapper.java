package com.bajansdk.aiobingo;

import com.bajansdk.aiobingo.model.EventType;
import com.bajansdk.aiobingo.model.GameEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.http.api.loottracker.LootRecordType;

/** Maps RuneLite's normalized loot events to AIO Bingo item-drop events. */
final class LootEventMapper {

    private LootEventMapper() {
    }

    static List<GameEvent> map(
        LootReceived loot,
        String playerName,
        String timestamp,
        IntFunction<String> itemNameLookup
    ) {
        if (loot == null || loot.getType() == null || loot.getType() == LootRecordType.PLAYER) {
            return List.of();
        }

        Collection<ItemStack> items = loot.getItems();
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        String sourceType = loot.getType().name();
        String sourceName = loot.getName();
        Integer npcId = loot.getType() == LootRecordType.NPC && loot.getMetadata() instanceof Integer
            ? (Integer) loot.getMetadata()
            : null;
        String npcName = loot.getType() == LootRecordType.NPC ? sourceName : null;

        Map<String, String> metadata = new HashMap<>();
        if (sourceName != null && !sourceName.isBlank()) {
            metadata.put("loot_source_name", sourceName);
        }
        metadata.put("loot_event_amount", Integer.toString(loot.getAmount()));

        List<GameEvent> events = new ArrayList<>(items.size());
        for (ItemStack item : items) {
            if (item == null || item.getId() < 0 || item.getQuantity() <= 0) {
                continue;
            }

            events.add(GameEvent.builder()
                .eventType(EventType.ITEM_DROP)
                .playerName(playerName)
                .timestamp(timestamp)
                .itemId(item.getId())
                .itemName(lookupItemName(itemNameLookup, item.getId()))
                .quantity(item.getQuantity())
                .npcId(npcId)
                .npcName(npcName)
                .lootSourceType(sourceType)
                .metadata(Map.copyOf(metadata))
                .build());
        }
        return events;
    }

    private static String lookupItemName(IntFunction<String> itemNameLookup, int itemId) {
        if (itemNameLookup == null) {
            return "";
        }

        try {
            String itemName = itemNameLookup.apply(itemId);
            return itemName == null ? "" : itemName;
        } catch (RuntimeException ignored) {
            // Item ID matching is canonical; a missing display name must not lose the drop.
            return "";
        }
    }
}
