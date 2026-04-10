package com.bajansdk.aiobingo.ui;

import com.bajansdk.aiobingo.model.BingoBoard;
import com.bajansdk.aiobingo.model.BingoTile;
import com.bajansdk.aiobingo.model.TileType;
import com.bajansdk.aiobingo.model.TeamProgress;
import com.bajansdk.aiobingo.model.TileProgress;
import net.runelite.api.Skill;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Split layout: the bingo grid is pinned at the top (never scrolls) while the
 * tile list fills the remaining vertical space and scrolls independently when
 * it overflows.
 *
 * <pre>
 * ┌──────────────────────┐
 * │  grid  (NORTH, fixed)│
 * ├──────────────────────┤
 * │  list scroll (CENTER)│  ← scrolls only when list overflows
 * └──────────────────────┘
 * </pre>
 */
public class BingoBoardPanel extends JPanel {

    private static final int MIN_CELL  = 28;
    private static final int PANEL_W   = 215;
    private static final int GRID_GAP  = 2;

    private BingoBoard       board;
    private TeamProgress     teamProgress;
    private SkillIconManager skillIconManager;

    private final JPanel        gridHolder = new JPanel();
    private final TileListPanel listPanel  = new TileListPanel();
    private final JScrollPane   listScroll;

    public BingoBoardPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(BingoColors.SURFACE);

        // Grid — pinned at top, 2px horizontal indent
        gridHolder.setOpaque(false);
        gridHolder.setBorder(new EmptyBorder(2, 2, 0, 2));
        add(gridHolder, BorderLayout.NORTH);

        // Tile list — fills remaining vertical space, scrolls on overflow
        listScroll = new JScrollPane(listPanel);
        listScroll.setBackground(BingoColors.SURFACE);
        listScroll.setBorder(null);
        listScroll.getViewport().setBackground(BingoColors.SURFACE);
        listScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        listScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(listScroll, BorderLayout.CENTER);
    }

    public void setSkillIconManager(SkillIconManager mgr) {
        this.skillIconManager = mgr;
    }

    public void update(BingoBoard board, TeamProgress teamProgress) {
        this.board        = board;
        this.teamProgress = teamProgress;
        rebuild();
    }

    // =========================================================================
    // Rebuild
    // =========================================================================

    private void rebuild() {
        gridHolder.removeAll();

        if (board == null || board.getTiles() == null || board.getTiles().isEmpty()) {
            gridHolder.setLayout(new BorderLayout());
            gridHolder.add(buildEmptyState(), BorderLayout.CENTER);
            gridHolder.setPreferredSize(new Dimension(PANEL_W, 120));
            listScroll.setVisible(false);
            revalidate();
            repaint();
            return;
        }

        int cols     = board.getGridWidth();
        int rows     = board.getGridHeight();
        Insets gi    = gridHolder.getInsets();
        int innerW   = panelWidth() - gi.left - gi.right;
        int cellSize = Math.max(MIN_CELL, (innerW - GRID_GAP * (cols - 1)) / cols);
        int gridH    = rows * cellSize + GRID_GAP * (rows - 1);

        gridHolder.setLayout(new GridLayout(rows, cols, GRID_GAP, GRID_GAP));
        gridHolder.setPreferredSize(new Dimension(innerW, gridH + gi.top + gi.bottom));

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                BingoTile    tile = board.getTileAt(r, c);
                TileProgress tp   = (teamProgress != null && tile != null)
                    ? teamProgress.getProgressFor(tile.getId()) : null;
                gridHolder.add(new BingoCellPanel(tile, tp, resolveSkillIcon(tile, 14)));
            }
        }

        listScroll.setVisible(true);
        listPanel.rebuild(board, teamProgress, t -> resolveSkillIcon(t, 12));

        revalidate();
        repaint();
    }

    /** Current panel width, or the RuneLite default before first layout. */
    private int panelWidth() {
        int w = getWidth();
        return w > 0 ? w : PANEL_W;
    }

    /** Returns a scaled skill icon for XP tiles; null for all other types. */
    private Image resolveSkillIcon(BingoTile tile, int size) {
        if (skillIconManager == null || tile == null) return null;
        if (tile.getTileType() != TileType.XP) return null;
        Map<String, String> params = tile.getParameters();
        if (params == null) return null;
        String skillName = params.get("skill");
        if (skillName == null || skillName.isEmpty()) return null;
        try {
            Skill skill = Skill.valueOf(skillName.toUpperCase().replace(" ", "_"));
            BufferedImage img = skillIconManager.getSkillImage(skill, true);
            if (img != null) return img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
        } catch (IllegalArgumentException ignored) {}
        return null;
    }

    private static JPanel buildEmptyState() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BingoColors.SURFACE);
        p.add(Box.createVerticalGlue());

        JLabel icon = new JLabel("\u25c6", SwingConstants.CENTER);
        icon.setFont(new Font("Dialog", Font.PLAIN, 22));
        icon.setForeground(BingoColors.GOLD_DIM);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(icon);
        p.add(Box.createRigidArea(new Dimension(0, 6)));

        JLabel msg = new JLabel("No board loaded", SwingConstants.CENTER);
        msg.setFont(FontManager.getRunescapeSmallFont());
        msg.setForeground(BingoColors.PARCHMENT_DIM);
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(msg);

        JLabel sub = new JLabel("Enter tokens in plugin settings", SwingConstants.CENTER);
        sub.setFont(FontManager.getRunescapeSmallFont());
        sub.setForeground(BingoColors.PARCHMENT_FAINT);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(sub);

        p.add(Box.createVerticalGlue());
        return p;
    }

    // =========================================================================
    // Tile list — implements Scrollable so the JScrollPane stretches it
    // horizontally to fill all available width.
    // =========================================================================

    /**
     * Three collapsible sections:
     *   In Progress — sorted by progress ratio descending
     *   Completed   — collapsible, starts expanded
     *   Not Started — collapsible, starts expanded
     */
    private class TileListPanel extends JPanel implements Scrollable {

        private boolean completedOpen  = true;
        private boolean notStartedOpen = true;

        private BingoBoard                 currentBoard;
        private TeamProgress               currentProgress;
        private Function<BingoTile, Image> currentIcons;

        TileListPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(BingoColors.SURFACE);
            setOpaque(true);
        }

        void rebuild(BingoBoard board, TeamProgress tp, Function<BingoTile, Image> icons) {
            this.currentBoard    = board;
            this.currentProgress = tp;
            this.currentIcons    = icons;
            rebuildInternal();
        }

        private void rebuildInternal() {
            removeAll();

            List<BingoTile> inProg     = new ArrayList<>();
            List<BingoTile> completed  = new ArrayList<>();
            List<BingoTile> notStarted = new ArrayList<>();

            for (BingoTile tile : currentBoard.getTiles()) {
                if (tile == null) continue;
                TileProgress tp  = currentProgress != null
                    ? currentProgress.getProgressFor(tile.getId()) : null;
                boolean free     = TileType.FREE == tile.getTileType();
                boolean done     = free || tile.isCompleted() || (tp != null && tp.isCompleted());
                boolean active   = !done && tp != null && tp.getCurrentValue() > 0;

                if (done)       completed.add(tile);
                else if (active) inProg.add(tile);
                else            notStarted.add(tile);
            }

            inProg.sort((a, b) -> {
                TileProgress pa = currentProgress != null ? currentProgress.getProgressFor(a.getId()) : null;
                TileProgress pb = currentProgress != null ? currentProgress.getProgressFor(b.getId()) : null;
                double ra = pa != null ? pa.getProgressRatio() : 0;
                double rb = pb != null ? pb.getProgressRatio() : 0;
                return Double.compare(rb, ra);
            });

            if (!inProg.isEmpty()) {
                addSectionHeader("In Progress", inProg.size(), false, false);
                for (BingoTile tile : inProg) {
                    TileProgress tp = currentProgress != null
                        ? currentProgress.getProgressFor(tile.getId()) : null;
                    add(buildRow(tile, tp, currentIcons.apply(tile)));
                }
            }

            if (!completed.isEmpty()) {
                addSectionHeader("Completed", completed.size(), true, completedOpen);
                if (completedOpen) {
                    for (BingoTile tile : completed) {
                        TileProgress tp = currentProgress != null
                            ? currentProgress.getProgressFor(tile.getId()) : null;
                        add(buildRow(tile, tp, currentIcons.apply(tile)));
                    }
                }
            }

            if (!notStarted.isEmpty()) {
                addSectionHeader("Not Started", notStarted.size(), true, notStartedOpen);
                if (notStartedOpen) {
                    for (BingoTile tile : notStarted) {
                        add(buildRow(tile, null, currentIcons.apply(tile)));
                    }
                }
            }

            revalidate();
            repaint();
        }

        private void addSectionHeader(String title, int count,
                                      boolean collapsible, boolean open) {
            String arrow = collapsible ? (open ? "  \u25be" : "  \u25b8") : "";
            JLabel header = new JLabel(title + " (" + count + ")" + arrow) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(BingoColors.SURFACE_2);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    FontMetrics fm = g2.getFontMetrics(getFont());
                    int textEnd = 6 + fm.stringWidth(getText());
                    int midY    = getHeight() / 2;
                    g2.setColor(BingoColors.BORDER);
                    g2.fillRect(textEnd + 4, midY, getWidth() - textEnd - 8, 1);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            header.setFont(new Font(
                FontManager.getRunescapeSmallFont().getName(), Font.BOLD,
                FontManager.getRunescapeSmallFont().getSize()));
            header.setForeground(BingoColors.PARCHMENT_FAINT);
            header.setOpaque(true);
            header.setBackground(BingoColors.SURFACE_2);
            header.setBorder(new EmptyBorder(4, 4, 4, 4));
            header.setAlignmentX(Component.LEFT_ALIGNMENT);
            header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

            if (collapsible) {
                header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                header.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (title.equals("Completed")) completedOpen  = !completedOpen;
                        else                           notStartedOpen = !notStartedOpen;
                        rebuildInternal();
                    }
                    @Override public void mouseEntered(MouseEvent e) { header.setForeground(BingoColors.PARCHMENT_DIM); }
                    @Override public void mouseExited(MouseEvent e)  { header.setForeground(BingoColors.PARCHMENT_FAINT); }
                });
            }

            add(Box.createRigidArea(new Dimension(0, 2)));
            add(header);
        }

        private JPanel buildRow(BingoTile tile, TileProgress tp, Image skillIcon) {
            String  typeName = tile.getTileType() != null ? tile.getTileType().name() : "CUSTOM";
            Color   accent   = BingoColors.tileAccent(typeName);
            boolean free     = TileType.FREE == tile.getTileType();
            boolean done     = free || tile.isCompleted() || (tp != null && tp.isCompleted());
            boolean hasBar   = tp != null && !done && tp.getRequiredValue() > 1;

            int rowH = hasBar ? 50 : 26;

            JPanel row = new JPanel(new BorderLayout(6, 0)) {
                @Override
                protected void paintComponent(Graphics g) {
                    g.setColor(BingoColors.SURFACE_2);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            };
            row.setOpaque(false);
            row.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, BingoColors.SURFACE_3),
                new EmptyBorder(hasBar ? 3 : 5, 4, hasBar ? 3 : 5, 4)));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, rowH));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Icon
            JLabel iconLabel;
            if (skillIcon != null) {
                iconLabel = new JLabel(new ImageIcon(skillIcon));
            } else {
                iconLabel = new JLabel(BingoColors.tileIcon(typeName));
                iconLabel.setFont(new Font("Dialog", Font.PLAIN, 10));
                iconLabel.setForeground(
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 210));
            }
            iconLabel.setPreferredSize(new Dimension(14, 14));
            iconLabel.setVerticalAlignment(SwingConstants.TOP);

            // Center: title + optional progress bar
            JPanel center = new JPanel();
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.setOpaque(false);

            JLabel titleLabel = new JLabel(tile.getTitle() != null ? tile.getTitle() : "?");
            titleLabel.setFont(FontManager.getRunescapeSmallFont());
            titleLabel.setForeground(done
                ? new Color(0xb0, 0xff, 0xc0)
                : BingoColors.PARCHMENT);
            center.add(titleLabel);

            if (hasBar) {
                center.add(Box.createRigidArea(new Dimension(0, 3)));
                center.add(buildProgressBar(tp, accent));
            }

            JLabel badge = buildBadge(tile, tp, done);

            row.add(iconLabel, BorderLayout.WEST);
            row.add(center, BorderLayout.CENTER);
            if (badge != null) row.add(badge, BorderLayout.EAST);

            row.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Window owner = SwingUtilities.getWindowAncestor(row);
                    TileDetailDialog.show(owner, tile, tp);
                }
                @Override public void mouseEntered(MouseEvent e) { row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
                @Override public void mouseExited(MouseEvent e)  { row.setCursor(Cursor.getDefaultCursor()); }
            });

            return row;
        }

        private JPanel buildProgressBar(TileProgress tp, Color accent) {
            NumberFormat nf    = NumberFormat.getNumberInstance(Locale.US);
            float        ratio = (float) Math.min(1.0, tp.getProgressRatio());
            String       label = nf.format(tp.getCurrentValue()) + " / " + nf.format(tp.getRequiredValue());

            JPanel bar = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth(), h = getHeight();
                    g2.setColor(BingoColors.SURFACE_3);
                    g2.fillRoundRect(0, 0, w, h, 3, 3);
                    if (ratio > 0) {
                        g2.setColor(new Color(
                            accent.getRed(), accent.getGreen(), accent.getBlue(), 200));
                        g2.fillRoundRect(0, 0, Math.max(3, (int) (w * ratio)), h, 3, 3);
                    }
                    g2.dispose();
                }
            };
            bar.setOpaque(false);
            bar.setPreferredSize(new Dimension(0, 5));
            bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
            bar.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel numLabel = new JLabel(label, SwingConstants.LEFT);
            numLabel.setFont(FontManager.getRunescapeSmallFont());
            numLabel.setForeground(BingoColors.PARCHMENT_FAINT);
            numLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel container = new JPanel();
            container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
            container.setOpaque(false);
            container.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
            container.setAlignmentX(Component.LEFT_ALIGNMENT);
            container.add(bar);
            container.add(Box.createRigidArea(new Dimension(0, 2)));
            container.add(numLabel);
            return container;
        }

        private JLabel buildBadge(BingoTile tile, TileProgress tp, boolean done) {
            if (done) {
                JLabel l = new JLabel("\u2713");
                l.setFont(new Font(FontManager.getRunescapeSmallFont().getName(), Font.BOLD,
                    FontManager.getRunescapeSmallFont().getSize()));
                l.setForeground(BingoColors.GREEN_LIGHT);
                return l;
            }
            if (tile.hasTiers() && tp != null && tp.getCurrentTier() > 0) {
                int tier = tp.getCurrentTier();
                JLabel l = new JLabel("T" + tier);
                l.setFont(new Font(FontManager.getRunescapeSmallFont().getName(), Font.BOLD,
                    FontManager.getRunescapeSmallFont().getSize()));
                l.setForeground(BingoColors.tierColor(tier));
                return l;
            }
            return null;
        }

        // ── Scrollable — fill viewport width, grow vertically ────────────
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d)  { return 26; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 60; }
        @Override public boolean getScrollableTracksViewportWidth()  { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    // =========================================================================
    // Grid cell — square, icon-only, paints all state itself
    // =========================================================================

    private static class BingoCellPanel extends JPanel {

        private static final Font ICON_FONT  = new Font("Dialog", Font.PLAIN, 12);
        private static final Font CHECK_FONT = new Font("Dialog", Font.BOLD, 14);

        private final BingoTile    tile;
        private final TileProgress progress;
        private final String       typeName;
        private final Color        accent;
        private final boolean      isFree;
        private final boolean      complete;
        private final boolean      inProgress;
        private final Image        skillIcon;

        BingoCellPanel(BingoTile tile, TileProgress progress, Image skillIcon) {
            this.tile       = tile;
            this.progress   = progress;
            this.skillIcon  = skillIcon;
            this.typeName   = tile != null && tile.getTileType() != null
                ? tile.getTileType().name() : "CUSTOM";
            this.accent     = BingoColors.tileAccent(typeName);
            this.isFree     = "FREE".equals(typeName);
            this.complete   = isFree
                || (tile != null && tile.isCompleted())
                || (progress != null && progress.isCompleted());
            this.inProgress = !complete && progress != null && progress.getCurrentValue() > 0;

            setOpaque(false);
            setLayout(null);
            setToolTipText(buildTooltip());

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (tile != null)
                        TileDetailDialog.show(SwingUtilities.getWindowAncestor(BingoCellPanel.this), tile, progress);
                }
                @Override public void mouseEntered(MouseEvent e) { setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
                @Override public void mouseExited(MouseEvent e)  { setCursor(Cursor.getDefaultCursor()); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            boolean hasTiers = tile != null && tile.hasTiers();
            int barH  = (!complete && progress != null && progress.getRequiredValue() > 1) ? 3 : 0;
            int pipH  = hasTiers ? 8 : 0;
            int iconH = h - barH - pipH;

            g2.setColor(cellBackground());
            g2.fillRect(0, 0, w, h);

            if (complete && !isFree) {
                g2.setColor(new Color(0x1d, 0x7a, 0x40, 40));
                g2.fillRect(0, 0, w, h);
            }

            if (tile != null) {
                if (skillIcon != null) {
                    int iw = 14, ih = 14;
                    g2.drawImage(skillIcon, (w - iw) / 2, (iconH - ih) / 2, iw, ih, null);
                } else {
                    g2.setFont(ICON_FONT);
                    Color ic = complete
                        ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 160)
                        : new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 220);
                    g2.setColor(ic);
                    FontMetrics fm = g2.getFontMetrics();
                    String glyph = BingoColors.tileIcon(typeName);
                    g2.drawString(glyph,
                        (w - fm.stringWidth(glyph)) / 2,
                        (iconH - fm.getHeight()) / 2 + fm.getAscent());
                }
            }

            if (complete && !isFree) {
                g2.setFont(CHECK_FONT);
                g2.setColor(new Color(0x40, 0xdd, 0x80, 55));
                FontMetrics fm = g2.getFontMetrics();
                String ck = "\u2713";
                g2.drawString(ck,
                    (w - fm.stringWidth(ck)) / 2,
                    (iconH + fm.getAscent() - fm.getDescent()) / 2);
            }

            if (hasTiers && progress != null) {
                int tier   = progress.getCurrentTier();
                int dot    = 4, gap = 3;
                int totalW = 3 * dot + 2 * gap;
                int px     = (w - totalW) / 2;
                int py     = h - barH - pipH + (pipH - dot) / 2;
                for (int i = 0; i < 3; i++) {
                    boolean filled = i < tier;
                    g2.setColor(filled ? BingoColors.tierColor(i + 1) : BingoColors.BORDER);
                    int x = px + i * (dot + gap);
                    if (filled) g2.fillOval(x, py, dot, dot);
                    else        g2.drawOval(x, py, dot - 1, dot - 1);
                }
            }

            if (barH > 0) {
                g2.setColor(BingoColors.SURFACE_3);
                g2.fillRect(0, h - barH, w, barH);
                float ratio = (float) progress.getProgressRatio();
                if (ratio > 0f) {
                    g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 200));
                    g2.fillRect(0, h - barH, Math.max(1, (int) (w * ratio)), barH);
                }
            }

            g2.setColor(cellBorder());
            g2.drawRect(0, 0, w - 1, h - 1);

            g2.dispose();
        }

        private Color cellBackground() {
            if (isFree)   return new Color(0x1e, 0x2e, 0x22);
            if (complete) return new Color(0x18, 0x38, 0x22);
            if (tile != null && tile.hasTiers() && progress != null) {
                switch (progress.getCurrentTier()) {
                    case 3: return new Color(0x30, 0x28, 0x08);
                    case 2: return new Color(0x2a, 0x2a, 0x2e);
                    case 1: return new Color(0x2c, 0x20, 0x0c);
                }
            }
            return inProgress ? BingoColors.SURFACE : BingoColors.SURFACE_2;
        }

        private Color cellBorder() {
            if (complete)   return BingoColors.GREEN_LIGHT;
            if (inProgress) return new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 130);
            return BingoColors.BORDER;
        }

        private String buildTooltip() {
            if (tile == null) return null;
            NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
            StringBuilder sb = new StringBuilder("<html>");
            sb.append("<b>").append(escape(tile.getTitle())).append("</b>");
            if (tile.getDescription() != null)
                sb.append("<br><i>").append(escape(tile.getDescription())).append("</i>");
            sb.append("<br><font color='#a89868'>").append(BingoColors.tileLabel(typeName)).append("</font>");

            if (tile.hasTiers() && progress != null) {
                int cur = progress.getCurrentTier();
                sb.append("<br>");
                appendTierLine(sb, tile.getTier1Threshold(), tile.getTier1Points(), cur >= 1, nf);
                appendTierLine(sb, tile.getTier2Threshold(), tile.getTier2Points(), cur >= 2, nf);
                appendTierLine(sb, tile.getTier3Threshold(), tile.getTier3Points(), cur >= 3, nf);
                sb.append("<br>Current: ").append(nf.format(progress.getCurrentValue()));
            } else if (progress != null) {
                sb.append("<br>").append(nf.format(progress.getCurrentValue()))
                  .append(" / ").append(nf.format(progress.getRequiredValue()));
                if (progress.isCompleted()) {
                    sb.append("<br><b>COMPLETED</b>");
                    if (progress.getCompletedBy() != null)
                        sb.append(" by ").append(escape(progress.getCompletedBy()));
                }
            }
            sb.append("</html>");
            return sb.toString();
        }

        private static void appendTierLine(StringBuilder sb, Integer t, int pts, boolean reached, NumberFormat nf) {
            if (t == null) return;
            sb.append("<br>").append(reached ? "\u2605" : "\u2606").append(" ")
              .append(nf.format(t)).append(" \u2014 ").append(pts).append("pts");
            if (reached) sb.append(" \u2713");
        }

        private static String escape(String s) {
            if (s == null) return "";
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}
