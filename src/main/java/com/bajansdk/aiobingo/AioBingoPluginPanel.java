package com.bajansdk.aiobingo;

import com.bajansdk.aiobingo.model.BingoBoard;
import com.bajansdk.aiobingo.model.LeaderboardEntry;
import com.bajansdk.aiobingo.model.TeamProgress;
import com.bajansdk.aiobingo.ui.BingoBoardPanel;
import com.bajansdk.aiobingo.ui.BingoColors;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Side panel with a styled header, custom tab bar, bingo board grid, and leaderboard.
 * Uses the OSRS dark-parchment/gold palette from the web frontend.
 */
public class AioBingoPluginPanel extends PluginPanel {

    // ── Header labels ────────────────────────────────────────────────────────
    private final JLabel boardNameLabel  = new JLabel("AIO Bingo", SwingConstants.LEFT);
    private final JLabel statusDot       = new JLabel("\u25cf", SwingConstants.LEFT); // ●
    private final JLabel statusText      = new JLabel("Not configured", SwingConstants.LEFT);
    private final JLabel progressLabel   = new JLabel("", SwingConstants.LEFT);
    private final JButton refreshButton  = buildRefreshButton();

    // ── Board + leaderboard ──────────────────────────────────────────────────
    private final BingoBoardPanel boardPanel      = new BingoBoardPanel();
    private final JPanel          leaderboardPanel = new JPanel();

    // ── Tab state ────────────────────────────────────────────────────────────
    private final CardLayout cardLayout  = new CardLayout();
    private final JPanel     contentCard = new JPanel(cardLayout);
    private JLabel boardTabBtn;
    private JLabel lbTabBtn;
    private boolean boardTabActive = true;

    // ── Data state ───────────────────────────────────────────────────────────
    private Runnable onRefreshClicked;
    private String currentTeamName;

    public AioBingoPluginPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(BingoColors.SURFACE);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
    }

    // =========================================================================
    // Header
    // =========================================================================

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                // Warm dark background
                g2.setColor(BingoColors.SURFACE_2);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Gold bottom rule
                g2.setColor(BingoColors.BORDER_BRIGHT);
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(6, 8, 7, 8));

        // ── Title row: "◆ AIO BINGO" + refresh button ────────────────────
        JPanel titleRow = new JPanel(new BorderLayout(0, 0));
        titleRow.setOpaque(false);

        JLabel title = new JLabel("\u25c6 AIO BINGO"); // ◆
        title.setFont(new Font("Dialog", Font.BOLD, 10));
        title.setForeground(BingoColors.GOLD);
        titleRow.add(title, BorderLayout.WEST);

        refreshButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        titleRow.add(refreshButton, BorderLayout.EAST);

        // ── Board name row ────────────────────────────────────────────────
        boardNameLabel.setFont(FontManager.getRunescapeSmallFont());
        boardNameLabel.setForeground(BingoColors.PARCHMENT);

        // ── Status row: dot + text ────────────────────────────────────────
        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        statusRow.setOpaque(false);

        statusDot.setFont(new Font("Dialog", Font.PLAIN, 8));
        statusDot.setForeground(BingoColors.PARCHMENT_FAINT);
        statusText.setFont(FontManager.getRunescapeSmallFont());
        statusText.setForeground(BingoColors.PARCHMENT_FAINT);

        statusRow.add(statusDot);
        statusRow.add(statusText);

        // ── Progress row ──────────────────────────────────────────────────
        progressLabel.setFont(FontManager.getRunescapeSmallFont());
        progressLabel.setForeground(BingoColors.PARCHMENT_DIM);

        // ── Stack all rows ────────────────────────────────────────────────
        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setOpaque(false);
        rows.add(boardNameLabel);
        rows.add(Box.createRigidArea(new Dimension(0, 2)));
        rows.add(statusRow);
        rows.add(progressLabel);

        header.add(titleRow, BorderLayout.NORTH);
        header.add(rows, BorderLayout.CENTER);
        return header;
    }

    private static JButton buildRefreshButton() {
        JButton btn = new JButton("\u21bb"); // ↻
        btn.setFont(new Font("Dialog", Font.PLAIN, 11));
        btn.setForeground(BingoColors.PARCHMENT_DIM);
        btn.setBackground(BingoColors.SURFACE_3);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BingoColors.BORDER, 1),
            new EmptyBorder(1, 5, 1, 5)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Refresh board");
        btn.setVisible(false);
        return btn;
    }

    // =========================================================================
    // Tab bar + content card
    // =========================================================================

    private JPanel buildContent() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(BingoColors.SURFACE);

        // ── Tab bar ───────────────────────────────────────────────────────
        boardTabBtn = makeTabLabel("Board");
        lbTabBtn    = makeTabLabel("Leaderboard");
        selectTab(boardTabBtn, lbTabBtn, true);

        boardTabBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { activateBoardTab(); }
            @Override public void mouseEntered(MouseEvent e) { if (!boardTabActive) boardTabBtn.setForeground(BingoColors.PARCHMENT); }
            @Override public void mouseExited(MouseEvent e)  { if (!boardTabActive) boardTabBtn.setForeground(BingoColors.PARCHMENT_FAINT); }
        });
        lbTabBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { activateLeaderboardTab(); }
            @Override public void mouseEntered(MouseEvent e) { if (boardTabActive) lbTabBtn.setForeground(BingoColors.PARCHMENT); }
            @Override public void mouseExited(MouseEvent e)  { if (boardTabActive) lbTabBtn.setForeground(BingoColors.PARCHMENT_FAINT); }
        });

        JPanel tabBar = new JPanel(new GridLayout(1, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(BingoColors.SURFACE_2);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        tabBar.setOpaque(false);
        tabBar.setPreferredSize(new Dimension(0, 26));
        tabBar.setBorder(new MatteBorder(0, 0, 1, 0, BingoColors.BORDER));
        tabBar.add(boardTabBtn);
        tabBar.add(lbTabBtn);

        // boardPanel handles its own layout — grid pinned at top, list scrolls
        // internally via its own JScrollPane. No outer scroll pane needed.

        // ── Leaderboard scroll ────────────────────────────────────────────
        leaderboardPanel.setLayout(new BoxLayout(leaderboardPanel, BoxLayout.Y_AXIS));
        leaderboardPanel.setBackground(BingoColors.SURFACE);
        leaderboardPanel.setBorder(new EmptyBorder(4, 4, 4, 4));

        JScrollPane lbScroll = new JScrollPane(leaderboardPanel);
        lbScroll.setBackground(BingoColors.SURFACE);
        lbScroll.setBorder(null);
        lbScroll.getViewport().setBackground(BingoColors.SURFACE);

        // ── Card ──────────────────────────────────────────────────────────
        contentCard.setBackground(BingoColors.SURFACE);
        contentCard.add(boardPanel, "board");
        contentCard.add(lbScroll, "leaderboard");
        cardLayout.show(contentCard, "board");

        wrapper.add(tabBar, BorderLayout.NORTH);
        wrapper.add(contentCard, BorderLayout.CENTER);
        return wrapper;
    }

    private static JLabel makeTabLabel(String text) {
        JLabel label = new JLabel(text.toUpperCase(), SwingConstants.CENTER);
        label.setFont(new Font(FontManager.getRunescapeSmallFont().getName(), Font.BOLD,
            FontManager.getRunescapeSmallFont().getSize()));
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setOpaque(true);
        return label;
    }

    private void selectTab(JLabel selected, JLabel other, boolean isBoard) {
        selected.setBackground(BingoColors.SURFACE_3);
        selected.setForeground(BingoColors.GOLD);
        selected.setBorder(new MatteBorder(0, 0, 2, 0, BingoColors.GOLD));
        other.setBackground(BingoColors.SURFACE_2);
        other.setForeground(BingoColors.PARCHMENT_FAINT);
        other.setBorder(new EmptyBorder(0, 0, 2, 0));
        boardTabActive = isBoard;
    }

    private void activateBoardTab() {
        selectTab(boardTabBtn, lbTabBtn, true);
        cardLayout.show(contentCard, "board");
    }

    private void activateLeaderboardTab() {
        selectTab(lbTabBtn, boardTabBtn, false);
        cardLayout.show(contentCard, "leaderboard");
    }

    // =========================================================================
    // Public update API
    // =========================================================================

    /** Called on EDT after each API refresh. */
    public void update(BingoBoard board, TeamProgress progress, List<LeaderboardEntry> leaderboard) {
        currentTeamName = (progress != null) ? progress.getTeamName() : null;
        boardPanel.update(board, progress);
        updateLeaderboard(leaderboard);
        updateProgressLabel(progress);
    }

    public void setTokenStatus(TokenStatus status, BingoBoard board) {
        String name   = (board != null && board.getName() != null) ? board.getName() : "AIO Bingo";
        String detail;
        Color dotColor;
        boolean showRefresh = false;

        switch (status) {
            case NOT_CONFIGURED:
                detail   = "Enter tokens in plugin settings";
                dotColor = BingoColors.PARCHMENT_FAINT;
                name     = "AIO Bingo";
                break;
            case VALIDATING:
                detail   = "Validating\u2026";
                dotColor = BingoColors.AMBER;
                break;
            case BOARD_VALID_TEAM_MISSING:
                detail   = "Board found \u2014 enter team token";
                dotColor = BingoColors.AMBER;
                break;
            case ACTIVE:
                detail   = "Active";
                dotColor = BingoColors.GREEN_LIGHT;
                break;
            case INACTIVE:
                detail   = "Not started yet";
                dotColor = BingoColors.PARCHMENT_DIM;
                break;
            case INVALID_TOKEN:
                detail      = "Invalid token \u2014 check settings";
                dotColor    = BingoColors.RED;
                showRefresh = true;
                break;
            case EXPIRED:
                detail      = "Board expired \u2014 events paused";
                dotColor    = BingoColors.RED;
                showRefresh = true;
                break;
            case ERROR:
                detail   = "Connection error \u2014 retrying\u2026";
                dotColor = new Color(0xff, 0xa5, 0x00);
                break;
            default:
                detail   = "";
                dotColor = BingoColors.PARCHMENT_FAINT;
        }

        boardNameLabel.setText(name);
        statusDot.setForeground(dotColor);
        statusText.setText(detail);
        statusText.setForeground(dotColor);
        refreshButton.setVisible(showRefresh);
        refreshButton.addActionListener(e -> {
            if (onRefreshClicked != null) onRefreshClicked.run();
        });
    }

    public void setSkillIconManager(net.runelite.client.game.SkillIconManager skillIconManager) {
        boardPanel.setSkillIconManager(skillIconManager);
    }

    public void setOnRefreshClicked(Runnable callback) {
        this.onRefreshClicked = callback;
        // Re-wire — remove old listeners first to avoid stacking
        for (var l : refreshButton.getActionListeners()) refreshButton.removeActionListener(l);
        refreshButton.addActionListener(e -> callback.run());
    }

    // =========================================================================
    // Progress label
    // =========================================================================

    private void updateProgressLabel(TeamProgress progress) {
        if (progress == null) {
            progressLabel.setText("");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(progress.getCompletedTiles()).append("/").append(progress.getTotalTiles()).append(" tiles");
        if (progress.getTotalPoints() > 0)
            sb.append("  \u2022  ").append(progress.getTotalPoints()).append("pts");
        if (progress.getLinesCompleted() > 0)
            sb.append("  \u2022  ").append(progress.getLinesCompleted()).append("L");
        if (progress.isBlackout())
            sb.append("  \u2605"); // ★
        progressLabel.setText(sb.toString());
    }

    // =========================================================================
    // Leaderboard
    // =========================================================================

    private void updateLeaderboard(List<LeaderboardEntry> entries) {
        leaderboardPanel.removeAll();

        if (entries == null || entries.isEmpty()) {
            JPanel empty = new JPanel();
            empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
            empty.setOpaque(false);
            empty.add(Box.createVerticalGlue());

            JLabel icon = new JLabel("\u25c6", SwingConstants.CENTER);
            icon.setFont(new Font("Dialog", Font.PLAIN, 18));
            icon.setForeground(BingoColors.GOLD_DIM);
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);
            empty.add(icon);

            JLabel msg = new JLabel("No teams yet", SwingConstants.CENTER);
            msg.setFont(FontManager.getRunescapeSmallFont());
            msg.setForeground(BingoColors.PARCHMENT_FAINT);
            msg.setAlignmentX(Component.CENTER_ALIGNMENT);
            empty.add(msg);
            empty.add(Box.createVerticalGlue());

            leaderboardPanel.add(empty);
        } else {
            for (LeaderboardEntry entry : entries) {
                leaderboardPanel.add(buildLeaderboardRow(entry));
                leaderboardPanel.add(Box.createRigidArea(new Dimension(0, 3)));
            }
        }

        leaderboardPanel.revalidate();
        leaderboardPanel.repaint();
    }

    private JPanel buildLeaderboardRow(LeaderboardEntry entry) {
        boolean isMyTeam = currentTeamName != null
            && currentTeamName.equalsIgnoreCase(entry.getTeamName());
        Color rankColor = BingoColors.rankColor(entry.getRank());

        // Outer panel — custom paint for gold left stripe on "your team"
        JPanel row = new JPanel(new BorderLayout(6, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(BingoColors.SURFACE_2);
                g2.fillRect(0, 0, getWidth(), getHeight());
                if (isMyTeam) {
                    // Gold left accent stripe
                    g2.setColor(BingoColors.GOLD);
                    g2.fillRect(0, 0, 3, getHeight());
                }
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BingoColors.BORDER, 1),
            new EmptyBorder(4, isMyTeam ? 9 : 6, 4, 6)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        // ── Left: rank badge + team name stack ───────────────────────────
        JPanel left = new JPanel(new BorderLayout(4, 0));
        left.setOpaque(false);

        JLabel rankLabel = new JLabel("#" + entry.getRank());
        rankLabel.setFont(new Font(FontManager.getRunescapeSmallFont().getName(), Font.BOLD,
            FontManager.getRunescapeSmallFont().getSize()));
        rankLabel.setForeground(rankColor);

        JPanel nameStack = new JPanel();
        nameStack.setLayout(new BoxLayout(nameStack, BoxLayout.Y_AXIS));
        nameStack.setOpaque(false);

        JLabel nameLabel = new JLabel(entry.getTeamName()
            + (entry.isBlackout() ? "  \u2605" : ""));
        nameLabel.setFont(FontManager.getRunescapeSmallFont());
        nameLabel.setForeground(isMyTeam ? BingoColors.GOLD_LIGHT : BingoColors.PARCHMENT);

        String sub = entry.getCompletedTiles() + "/" + entry.getTotalTiles() + " tiles";
        if (entry.getLinesCompleted() > 0)
            sub += "  \u2022  " + entry.getLinesCompleted() + "L";
        JLabel subLabel = new JLabel(sub);
        subLabel.setFont(FontManager.getRunescapeSmallFont());
        subLabel.setForeground(BingoColors.PARCHMENT_FAINT);

        nameStack.add(nameLabel);
        nameStack.add(subLabel);

        left.add(rankLabel, BorderLayout.WEST);
        left.add(nameStack, BorderLayout.CENTER);

        // ── Right: score ─────────────────────────────────────────────────
        String scoreText = entry.getTotalPoints() > 0
            ? entry.getTotalPoints() + " pts"
            : entry.getCompletedTiles() + "/" + entry.getTotalTiles();
        JLabel scoreLabel = new JLabel(scoreText, SwingConstants.RIGHT);
        scoreLabel.setFont(new Font(FontManager.getRunescapeSmallFont().getName(), Font.BOLD,
            FontManager.getRunescapeSmallFont().getSize()));
        scoreLabel.setForeground(rankColor);

        row.add(left, BorderLayout.CENTER);
        row.add(scoreLabel, BorderLayout.EAST);
        return row;
    }
}
