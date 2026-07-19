package com.bajansdk.aiobingo;

import com.bajansdk.aiobingo.model.BingoBoard;
import com.bajansdk.aiobingo.model.LeaderboardEntry;
import com.bajansdk.aiobingo.model.TeamProgress;
import com.bajansdk.aiobingo.ui.BingoBoardPanel;
import com.bajansdk.aiobingo.ui.BingoColors;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Side panel with a styled header, custom tab bar, bingo board grid, and leaderboard.
 * Uses the OSRS dark-parchment/gold palette from the web frontend.
 */
public class AioBingoPluginPanel extends PluginPanel {

    private static final String WEBSITE_URL = "https://aiobingo.com";
    private static final String DISCORD_URL = "https://discord.gg/Ag376YRv8p";
    private static final String GITHUB_URL = "https://github.com/BajanSDK/aio-bingo-plugin";
    private static final String BUY_ME_A_COFFEE_URL = "https://buymeacoffee.com/aiobingo";

    // ── Header labels ────────────────────────────────────────────────────────
    private final JLabel boardNameLabel  = new JLabel("AIO Bingo", SwingConstants.LEFT);
    private final JLabel statusDot       = new JLabel("\u25cf", SwingConstants.LEFT); // ●
    private final JLabel statusText      = new JLabel("Not configured", SwingConstants.LEFT);
    private final JPanel statusBadge = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0)) {
        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color accent = statusText.getForeground();
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 28));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(graphics);
        }
    };
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
                // Quiet separator; the active tab supplies the gold accent.
                g2.setColor(BingoColors.BORDER);
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(6, 8, 7, 8));
        header.add(buildLinkBar(), BorderLayout.CENTER);
        return header;
    }

    private JPanel buildLinkBar() {
        JPanel linkBar = new JPanel(new BorderLayout(0, 0));
        linkBar.setOpaque(false);
        linkBar.setPreferredSize(new Dimension(0, 34));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        brand.setOpaque(false);
        brand.add(buildLinkButton(
            new AioLogoIcon(32, BingoColors.GOLD),
            new AioLogoIcon(32, BingoColors.GOLD_LIGHT),
            "Open aiobingo.com",
            WEBSITE_URL,
            34));
        brand.add(Box.createRigidArea(new Dimension(3, 0)));

        JLabel wordmark = new JLabel("BINGO");
        wordmark.setFont(new Font("Dialog", Font.BOLD, 10));
        wordmark.setForeground(BingoColors.GOLD);
        brand.add(wordmark);
        linkBar.add(brand, BorderLayout.WEST);

        JPanel supportLinks = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
        supportLinks.setOpaque(false);
        supportLinks.add(buildImageLinkButton(
            "/net/runelite/client/plugins/info/discord_icon.png",
            "Join the AIO Bingo Discord",
            DISCORD_URL));
        supportLinks.add(buildImageLinkButton(
            "/net/runelite/client/plugins/info/github_icon.png",
            "View the AIO Bingo plugin on GitHub",
            GITHUB_URL));
        supportLinks.add(buildLinkButton(
            new CoffeeIcon(24, BingoColors.PARCHMENT_DIM),
            new CoffeeIcon(24, BingoColors.GOLD_LIGHT),
            "Support AIO Bingo — buy me a coffee",
            BUY_ME_A_COFFEE_URL,
            30));
        linkBar.add(supportLinks, BorderLayout.EAST);

        return linkBar;
    }

    private static JButton buildImageLinkButton(String resource, String tooltip, String url) {
        BufferedImage image = ImageUtil.loadImageResource(AioBingoPluginPanel.class, resource);
        BufferedImage resizedImage = ImageUtil.resizeImage(image, 24, 24);
        return buildLinkButton(
            new ImageIcon(tintImage(resizedImage, BingoColors.PARCHMENT_DIM)),
            new ImageIcon(tintImage(resizedImage, BingoColors.GOLD_LIGHT)),
            tooltip,
            url,
            30);
    }

    private static BufferedImage tintImage(BufferedImage image, Color color) {
        BufferedImage tinted = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int rgb = color.getRGB() & 0x00ffffff;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                tinted.setRGB(x, y, (alpha << 24) | rgb);
            }
        }

        return tinted;
    }

    private static JButton buildLinkButton(Icon icon, Icon hoverIcon, String tooltip, String url, int size) {
        JButton button = new JButton(icon) {
            @Override
            protected void paintComponent(Graphics graphics) {
                if (getModel().isRollover() || getModel().isPressed()) {
                    Graphics2D g2 = (Graphics2D) graphics.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int alpha = getModel().isPressed() ? 36 : 22;
                    g2.setColor(new Color(255, 255, 255, alpha));
                    g2.fillOval(1, 1, getWidth() - 2, getHeight() - 2);
                    g2.dispose();
                }
                super.paintComponent(graphics);
            }
        };
        button.setPreferredSize(new Dimension(size, size));
        button.setMinimumSize(new Dimension(size, size));
        button.setBorder(new EmptyBorder(0, 0, 0, 0));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setToolTipText(tooltip);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                button.setIcon(hoverIcon);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                button.setIcon(icon);
            }
        });
        button.addActionListener(event -> LinkBrowser.browse(url));
        return button;
    }

    private static final class AioLogoIcon implements Icon {
        private final int size;
        private final Color accent;

        private AioLogoIcon(int size, Color accent) {
            this.size = size;
            this.accent = accent;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(BingoColors.SURFACE_3);
            g2.fillOval(x + 1, y + 1, size - 2, size - 2);
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(accent);
            g2.drawOval(x + 2, y + 2, size - 5, size - 5);

            Font font = new Font("Dialog", Font.BOLD, 10);
            g2.setFont(font);
            FontMetrics metrics = g2.getFontMetrics(font);
            String label = "AIO";
            int textX = x + (size - metrics.stringWidth(label)) / 2;
            int textY = y + (size - metrics.getHeight()) / 2 + metrics.getAscent();
            g2.drawString(label, textX, textY);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    private static final class CoffeeIcon implements Icon {
        private final int size;
        private final Color accent;

        private CoffeeIcon(int size, Color accent) {
            this.size = size;
            this.accent = accent;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(accent);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval(x + 1, y + 1, size - 3, size - 3);

            g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 6, y + 10, x + 6, y + 14);
            g2.drawArc(x + 6, y + 10, 10, 8, 180, 180);
            g2.drawLine(x + 16, y + 10, x + 16, y + 14);
            g2.drawArc(x + 15, y + 10, 4, 5, -90, 180);
            g2.drawLine(x + 6, y + 18, x + 17, y + 18);
            g2.drawArc(x + 8, y + 5, 3, 5, 90, 100);
            g2.drawArc(x + 12, y + 5, 3, 5, 90, 100);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
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

    private JPanel buildBoardSummary() {
        Font smallFont = FontManager.getRunescapeSmallFont();
        boardNameLabel.setFont(new Font(smallFont.getName(), Font.BOLD, smallFont.getSize()));
        boardNameLabel.setForeground(BingoColors.PARCHMENT);

        statusDot.setFont(new Font("Dialog", Font.PLAIN, 8));
        statusDot.setForeground(BingoColors.PARCHMENT_FAINT);
        statusText.setFont(new Font(smallFont.getName(), Font.BOLD, smallFont.getSize()));
        statusText.setForeground(BingoColors.PARCHMENT_FAINT);

        progressLabel.setFont(smallFont);
        progressLabel.setForeground(BingoColors.PARCHMENT_DIM);
        progressLabel.setVisible(false);

        JPanel titleRow = new JPanel(new BorderLayout(6, 0));
        titleRow.setOpaque(false);
        titleRow.add(boardNameLabel, BorderLayout.CENTER);

        statusBadge.setOpaque(false);
        statusBadge.setBorder(new EmptyBorder(1, 4, 1, 4));
        statusBadge.add(statusDot);
        statusBadge.add(statusText);

        JPanel statusActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        statusActions.setOpaque(false);
        statusActions.add(statusBadge);
        statusActions.add(refreshButton);
        titleRow.add(statusActions, BorderLayout.EAST);

        JPanel summary = new JPanel(new BorderLayout(0, 3));
        summary.setBackground(BingoColors.SURFACE_2);
        summary.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, BingoColors.BORDER),
            new EmptyBorder(6, 8, 5, 8)));
        summary.add(titleRow, BorderLayout.NORTH);
        summary.add(progressLabel, BorderLayout.CENTER);
        return summary;
    }

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
        tabBar.setBorder(null);
        tabBar.add(boardTabBtn);
        tabBar.add(lbTabBtn);

        // boardPanel handles its own layout — grid pinned at top, list scrolls
        // internally via its own JScrollPane. No outer scroll pane needed.
        JPanel boardCard = new JPanel(new BorderLayout(0, 0));
        boardCard.setBackground(BingoColors.SURFACE);
        boardCard.add(buildBoardSummary(), BorderLayout.NORTH);
        boardCard.add(boardPanel, BorderLayout.CENTER);

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
        contentCard.add(boardCard, "board");
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
        String statusTooltip;
        Color dotColor;
        boolean showRefresh = false;

        switch (status) {
            case NOT_CONFIGURED:
                detail   = "Setup required";
                statusTooltip = "Enter tokens in plugin settings";
                dotColor = BingoColors.PARCHMENT_FAINT;
                name     = "AIO Bingo";
                break;
            case VALIDATING:
                detail   = "Validating\u2026";
                statusTooltip = "Validating board and team tokens";
                dotColor = BingoColors.AMBER;
                break;
            case BOARD_VALID_TEAM_MISSING:
                detail   = "Team token needed";
                statusTooltip = "Enter the team token in plugin settings";
                dotColor = BingoColors.AMBER;
                break;
            case ACTIVE:
                detail   = "Active";
                statusTooltip = "Connected and tracking board progress";
                dotColor = BingoColors.GREEN_LIGHT;
                break;
            case INACTIVE:
                detail   = "Not started";
                statusTooltip = "This board has not started yet";
                dotColor = BingoColors.PARCHMENT_DIM;
                break;
            case INVALID_TOKEN:
                detail      = "Invalid token";
                statusTooltip = "Check the tokens in plugin settings";
                dotColor    = BingoColors.RED;
                showRefresh = true;
                break;
            case EXPIRED:
                detail      = "Expired";
                statusTooltip = "Board expired — events are paused";
                dotColor    = BingoColors.RED;
                showRefresh = true;
                break;
            case ERROR:
                detail   = "Connection error";
                statusTooltip = "Connection error — retrying…";
                dotColor = new Color(0xff, 0xa5, 0x00);
                break;
            default:
                detail   = "";
                statusTooltip = null;
                dotColor = BingoColors.PARCHMENT_FAINT;
        }

        boardNameLabel.setText(name);
        boardNameLabel.setToolTipText(name);
        statusDot.setForeground(dotColor);
        statusText.setText(detail);
        statusText.setForeground(dotColor);
        statusBadge.setToolTipText(statusTooltip);
        statusText.setToolTipText(statusTooltip);
        statusBadge.repaint();
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
            progressLabel.setVisible(false);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(progress.getCompletedTiles()).append("/").append(progress.getTotalTiles()).append(" tiles");
        if (progress.getTotalPoints() > 0)
            sb.append("  \u00b7  ").append(progress.getTotalPoints()).append(" pts");
        if (progress.getLinesCompleted() > 0) {
            int lines = progress.getLinesCompleted();
            sb.append("  \u00b7  ").append(lines).append(lines == 1 ? " line" : " lines");
        }
        if (progress.isBlackout())
            sb.append("  \u00b7  \u2605"); // · ★
        progressLabel.setText(sb.toString());
        progressLabel.setVisible(true);
        if (progressLabel.getParent() != null) {
            progressLabel.getParent().revalidate();
        }
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
