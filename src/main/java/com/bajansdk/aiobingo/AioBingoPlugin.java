package com.bajansdk.aiobingo;

import com.bajansdk.aiobingo.model.BingoBoard;
import com.bajansdk.aiobingo.model.EventType;
import com.bajansdk.aiobingo.model.GameEvent;
import com.bajansdk.aiobingo.model.LeaderboardEntry;
import com.bajansdk.aiobingo.model.TeamProgress;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.game.ItemStack;
import net.runelite.client.Notifier;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@PluginDescriptor(
    name = "AIO Bingo",
    description = "Clan bingo event tracker — tracks drops, collection log, quests, diaries, combat achievements, and PvP. XP, kill counts, and raids are tracked via highscores. Events are sent to aiobingo.com only when opted in.",
    tags = {"bingo", "clan", "event", "tracker", "team", "aiobingo"}
)
public class AioBingoPlugin extends Plugin {

    // Flush batch after this many events or after FLUSH_INTERVAL_SECONDS
    private static final int MAX_QUEUE_SIZE = 50;
    private static final int FLUSH_INTERVAL_SECONDS = 30;

    @Inject private Client client;
    @Inject private AioBingoConfig config;
    @Inject private BingoApiClient apiClient;
    @Inject private ClientToolbar clientToolbar;
    @Inject private Notifier notifier;
    @Inject private SkillIconManager skillIconManager;

    private final ConcurrentLinkedQueue<GameEvent> eventQueue = new ConcurrentLinkedQueue<>();

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> flushTask;
    private ScheduledFuture<?> refreshTask;

    private AioBingoPluginPanel pluginPanel;
    private NavigationButton navButton;

    private volatile BingoBoard currentBoard;
    private volatile TeamProgress currentProgress;
    private volatile List<LeaderboardEntry> currentLeaderboard = Collections.emptyList();

    private volatile boolean loggedIn = false;
    private volatile int currentRefreshInterval;
    private volatile boolean boardExpired = false;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void startUp() {
        pluginPanel = new AioBingoPluginPanel();
        pluginPanel.setSkillIconManager(skillIconManager);
        pluginPanel.setOnRefreshClicked(() -> scheduler.execute(this::validateTokens));
        pluginPanel.setTokenStatus(TokenStatus.NOT_CONFIGURED, null);

        BufferedImage icon = loadIcon();
        navButton = NavigationButton.builder()
            .tooltip("AIO Bingo")
            .icon(icon)
            .panel(pluginPanel)
            .build();
        clientToolbar.addNavigation(navButton);

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduleFlush();
        scheduleRefresh();

        if (client.getGameState() == GameState.LOGGED_IN) {
            loggedIn = true;
            scheduler.execute(this::refreshBoard);
        }
    }

    @Override
    protected void shutDown() {
        scheduler.execute(this::flushEvents);
        scheduler.shutdown();
        scheduler = null;
        clientToolbar.removeNavigation(navButton);
        navButton = null;
        pluginPanel = null;
        currentBoard = null;
        currentProgress = null;
        eventQueue.clear();
        loggedIn = false;
        boardExpired = false;
    }

    @Provides
    AioBingoConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AioBingoConfig.class);
    }

    private BufferedImage loadIcon() {
        try {
            BufferedImage img = ImageUtil.loadImageResource(getClass(), "/bingo_icon.png");
            if (img != null) return img;
        } catch (Exception ignored) { }
        return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
    }

    // -------------------------------------------------------------------------
    // Scheduling
    // -------------------------------------------------------------------------

    private void scheduleFlush() {
        if (flushTask != null) flushTask.cancel(false);
        flushTask = scheduler.scheduleAtFixedRate(
            this::flushEvents, FLUSH_INTERVAL_SECONDS, FLUSH_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void scheduleRefresh() {
        scheduleRefresh(Math.max(10, config.refreshIntervalSeconds()));
    }

    private void scheduleRefresh(int intervalSeconds) {
        if (refreshTask != null) refreshTask.cancel(false);
        currentRefreshInterval = intervalSeconds;
        refreshTask = scheduler.scheduleAtFixedRate(
            this::refreshBoard, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private void rescheduleRefreshIfNeeded(int targetIntervalSeconds) {
        if (targetIntervalSeconds != currentRefreshInterval) {
            scheduleRefresh(targetIntervalSeconds);
        }
    }

    // -------------------------------------------------------------------------
    // Board refresh
    // -------------------------------------------------------------------------

    private void refreshBoard() {
        if (!loggedIn) return;

        String boardToken = config.boardToken().trim();
        String teamToken = config.teamToken().trim();
        if (boardToken.isEmpty()) return;

        try {
            currentBoard = apiClient.fetchBoard(boardToken);

            // Inactive board — slow polling, skip progress + leaderboard
            if (!currentBoard.isActive()) {
                rescheduleRefreshIfNeeded(300);
                updatePanelStatus(TokenStatus.INACTIVE, currentBoard);
                return;
            }

            // Active board — restore normal polling interval
            rescheduleRefreshIfNeeded(Math.max(10, config.refreshIntervalSeconds()));

            if (teamToken.isEmpty()) {
                updatePanelStatus(TokenStatus.BOARD_VALID_TEAM_MISSING, currentBoard);
                return;
            }

            // Board is reachable — clear expiry flag if it was set
            if (boardExpired) {
                boardExpired = false;
            }

            TeamProgress oldProgress = currentProgress;
            currentProgress = apiClient.fetchTeamProgress(boardToken, teamToken);
            currentLeaderboard = apiClient.fetchLeaderboard(boardToken);
            checkProgressNotifications(oldProgress, currentProgress);
            updatePanelStatus(TokenStatus.ACTIVE, currentBoard);
            postPanelUpdate();
        } catch (BingoApiException e) {
            if (e.isTokenInvalid()) {
                boolean wasExpired = boardExpired;
                boardExpired = true;
                eventQueue.clear();
                if (refreshTask != null) refreshTask.cancel(false);
                updatePanelStatus(TokenStatus.EXPIRED, currentBoard);
                if (!wasExpired) {
                    notifier.notify("[AIO Bingo] Board has expired or been deactivated. Event tracking paused.");
                }
            } else {
                log.warn("Board refresh failed: {}", e.getMessage());
                updatePanelStatus(TokenStatus.ERROR, currentBoard);
            }
        } catch (IOException e) {
            log.warn("Board refresh failed: {}", e.getMessage());
            updatePanelStatus(TokenStatus.ERROR, currentBoard);
        }
    }

    private void validateTokens() {
        String boardToken = config.boardToken().trim();
        String teamToken = config.teamToken().trim();

        if (boardToken.isEmpty()) {
            updatePanelStatus(TokenStatus.NOT_CONFIGURED, null);
            return;
        }

        updatePanelStatus(TokenStatus.VALIDATING, null);

        try {
            BingoBoard board = apiClient.fetchBoard(boardToken);
            currentBoard = board;
            boardExpired = false;

            if (teamToken.isEmpty()) {
                updatePanelStatus(TokenStatus.BOARD_VALID_TEAM_MISSING, board);
                return;
            }

            TeamProgress progress = apiClient.fetchTeamProgress(boardToken, teamToken);
            currentProgress = progress;

            TokenStatus status = board.isActive() ? TokenStatus.ACTIVE : TokenStatus.INACTIVE;
            int interval = board.isActive() ? Math.max(10, config.refreshIntervalSeconds()) : 300;
            scheduleRefresh(interval);

            SwingUtilities.invokeLater(() -> {
                if (pluginPanel != null) {
                    pluginPanel.setTokenStatus(status, board);
                    pluginPanel.update(board, progress, currentLeaderboard);
                }
            });
        } catch (BingoApiException e) {
            if (e.isTokenInvalid()) {
                updatePanelStatus(TokenStatus.INVALID_TOKEN, null);
            } else {
                log.warn("Token validation failed: {}", e.getMessage());
                updatePanelStatus(TokenStatus.ERROR, null);
            }
        } catch (IOException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            updatePanelStatus(TokenStatus.ERROR, null);
        }
    }

    private void updatePanelStatus(TokenStatus status, BingoBoard board) {
        SwingUtilities.invokeLater(() -> {
            if (pluginPanel != null) {
                pluginPanel.setTokenStatus(status, board);
            }
        });
    }

    private void checkProgressNotifications(TeamProgress oldProgress, TeamProgress newProgress) {
        if (oldProgress == null || newProgress == null) return;
        // Detect new line completions
        int oldLines = oldProgress.getLinesCompleted();
        int newLines = newProgress.getLinesCompleted();
        if (newLines > oldLines) {
            int gained = newLines - oldLines;
            notifier.notify(String.format("[Bingo] Line completed! (%d total)", newLines));
        }
        // Detect new tile completions
        int oldTiles = oldProgress.getCompletedTiles();
        int newTiles = newProgress.getCompletedTiles();
        if (newTiles > oldTiles) {
            notifier.notify(String.format("[Bingo] Tile completed! (%d/%d)", newTiles, newProgress.getTotalTiles()));
        }
    }

    private void postPanelUpdate() {
        BingoBoard board = currentBoard;
        TeamProgress progress = currentProgress;
        List<LeaderboardEntry> leaderboard = currentLeaderboard;
        SwingUtilities.invokeLater(() -> {
            if (pluginPanel != null) pluginPanel.update(board, progress, leaderboard);
        });
    }

    // -------------------------------------------------------------------------
    // Event queue
    // -------------------------------------------------------------------------

    private void enqueue(GameEvent event) {
        if (!config.enableEventTracking()) return;
        if (!config.autoSubmit()) return;
        if (boardExpired) return;
        eventQueue.add(event);
        if (eventQueue.size() >= MAX_QUEUE_SIZE) {
            scheduler.execute(this::flushEvents);
        }
    }

    private void flushEvents() {
        if (boardExpired) {
            eventQueue.clear();
            return;
        }
        if (eventQueue.isEmpty()) return;
        String teamToken = config.teamToken().trim();
        if (teamToken.isEmpty()) {
            eventQueue.clear();
            return;
        }
        List<GameEvent> batch = drainQueue();
        if (batch.isEmpty()) return;
        try {
            apiClient.submitEventBatch(teamToken, batch);
        } catch (BingoApiException e) {
            if (e.isTokenInvalid()) {
                boardExpired = true;
                eventQueue.clear();
            }
            log.warn("Event batch submit failed ({} events): {}", batch.size(), e.getMessage());
        } catch (IOException e) {
            log.warn("Event batch submit failed ({} events): {}", batch.size(), e.getMessage());
        }
    }

    private List<GameEvent> drainQueue() {
        List<GameEvent> batch = new ArrayList<>();
        GameEvent e;
        while ((e = eventQueue.poll()) != null) batch.add(e);
        return batch;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String playerName() {
        Player player = client.getLocalPlayer();
        return player != null && player.getName() != null ? player.getName() : "Unknown";
    }

    private static String now() {
        return Instant.now().toString();
    }

    // -------------------------------------------------------------------------
    // Event subscriptions
    // -------------------------------------------------------------------------

    @Subscribe
    public void onGameStateChanged(GameStateChanged e) {
        if (e.getGameState() == GameState.LOGGED_IN) {
            loggedIn = true;
            scheduler.execute(this::refreshBoard);
        } else if (e.getGameState() == GameState.LOGIN_SCREEN) {
            loggedIn = false;
            scheduler.execute(this::flushEvents);
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged e) {
        if (!"aiobingobajansdk".equals(e.getGroup())) return;
        String key = e.getKey();
        if ("refreshIntervalSeconds".equals(key)) {
            scheduleRefresh();
        }
        if ("boardToken".equals(key) || "teamToken".equals(key)) {
            scheduler.execute(this::validateTokens);
        }
    }

    @Subscribe
    public void onNpcLootReceived(NpcLootReceived e) {
        if (!config.trackDrops()) return;
        for (ItemStack item : e.getItems()) {
            enqueue(GameEvent.builder()
                .eventType(EventType.ITEM_DROP)
                .playerName(playerName())
                .timestamp(now())
                .itemId(item.getId())
                .itemName("")
                .quantity(item.getQuantity())
                .npcId(e.getNpc().getId())
                .npcName(e.getNpc().getName())
                .lootSourceType("NPC")
                .build());
        }
    }



    @Subscribe
    public void onPlayerLootReceived(PlayerLootReceived e) {
        if (!config.trackPvp()) return;
        String opponentName = e.getPlayer().getName();
        enqueue(GameEvent.builder()
            .eventType(EventType.PVP_KILL)
            .playerName(playerName())
            .timestamp(now())
            .opponentName(opponentName)
            .build());
    }

}
