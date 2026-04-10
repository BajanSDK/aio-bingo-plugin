package com.bajansdk.aiobingo.ui;

import com.bajansdk.aiobingo.model.BingoTile;
import com.bajansdk.aiobingo.model.TileProgress;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Styled tile-detail dialog — replaces the plain JOptionPane used previously.
 * Shows tile type, description, progress bar, and tier breakdown.
 */
public class TileDetailDialog extends JDialog {

    private static final int WIDTH  = 280;
    private static final int INSET  = 12;

    private TileDetailDialog(Window owner, BingoTile tile, TileProgress progress) {
        super(owner, tile.getTitle(), ModalityType.APPLICATION_MODAL);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BingoColors.SURFACE);
        setContentPane(root);

        root.add(buildHeader(tile), BorderLayout.NORTH);
        root.add(buildBody(tile, progress), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        pack();
        setSize(WIDTH, getHeight());
        setLocationRelativeTo(owner);
    }

    // -------------------------------------------------------------------------
    // Header — accent-colored bar with icon + title
    // -------------------------------------------------------------------------

    private static JPanel buildHeader(BingoTile tile) {
        String typeName = tile.getTileType() != null ? tile.getTileType().name() : "CUSTOM";
        Color accent = BingoColors.tileAccent(typeName);
        String icon  = BingoColors.tileIcon(typeName);
        String label = BingoColors.tileLabel(typeName);

        JPanel header = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                // Dark warm background
                g2.setColor(BingoColors.SURFACE_2);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Accent left stripe (3px)
                g2.setColor(accent);
                g2.fillRect(0, 0, 3, getHeight());
                // Gold bottom divider
                g2.setColor(BingoColors.BORDER_BRIGHT);
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(10, INSET, 10, INSET));

        // Icon + label on the left
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
        iconLabel.setForeground(accent);
        left.add(iconLabel);

        JLabel typeLabel = new JLabel(label);
        typeLabel.setFont(FontManager.getRunescapeSmallFont());
        typeLabel.setForeground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 200));
        left.add(typeLabel);

        // Title (center / main)
        JLabel titleLabel = new JLabel(
            "<html><div style='max-width:200px'>" + escapeHtml(tile.getTitle()) + "</div></html>");
        titleLabel.setFont(FontManager.getRunescapeBoldFont());
        titleLabel.setForeground(BingoColors.PARCHMENT);

        header.add(left, BorderLayout.WEST);
        header.add(titleLabel, BorderLayout.CENTER);
        return header;
    }

    // -------------------------------------------------------------------------
    // Body — description, progress, tier breakdown
    // -------------------------------------------------------------------------

    private static JPanel buildBody(BingoTile tile, TileProgress progress) {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(BingoColors.SURFACE);
        body.setBorder(new EmptyBorder(INSET, INSET, INSET, INSET));

        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);

        // Description
        if (tile.getDescription() != null && !tile.getDescription().isEmpty()) {
            JLabel desc = new JLabel(
                "<html><div style='width:220px'>" + escapeHtml(tile.getDescription()) + "</div></html>");
            desc.setFont(FontManager.getRunescapeSmallFont());
            desc.setForeground(BingoColors.PARCHMENT_DIM);
            desc.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(desc);
            body.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        String typeName = tile.getTileType() != null ? tile.getTileType().name() : "CUSTOM";
        boolean isFree  = "FREE".equals(typeName);
        boolean complete = isFree || tile.isCompleted() || (progress != null && progress.isCompleted());

        // Tier breakdown
        if (tile.hasTiers()) {
            body.add(sectionLabel("Tier Breakdown"));
            body.add(Box.createRigidArea(new Dimension(0, 6)));

            int currentTier = (progress != null) ? progress.getCurrentTier() : 0;

            if (tile.getTier1Threshold() != null)
                body.add(tierRow(1, tile.getTier1Threshold(), tile.getTier1Points(), currentTier >= 1, nf,
                    BingoColors.TIER_BRONZE));
            if (tile.getTier2Threshold() != null)
                body.add(tierRow(2, tile.getTier2Threshold(), tile.getTier2Points(), currentTier >= 2, nf,
                    BingoColors.TIER_SILVER));
            if (tile.getTier3Threshold() != null)
                body.add(tierRow(3, tile.getTier3Threshold(), tile.getTier3Points(), currentTier >= 3, nf,
                    BingoColors.TIER_GOLD));

            body.add(Box.createRigidArea(new Dimension(0, 10)));

            // Progress toward next tier
            if (progress != null && !progress.isCompleted()) {
                body.add(sectionLabel("Progress"));
                body.add(Box.createRigidArea(new Dimension(0, 4)));
                long nextThreshold = nextTierThreshold(tile, currentTier);
                body.add(progressBar(progress.getCurrentValue(), nextThreshold,
                    BingoColors.tileAccent(typeName), nf));
            }

        } else if (!isFree) {
            // Single-tier progress
            body.add(sectionLabel("Progress"));
            body.add(Box.createRigidArea(new Dimension(0, 4)));

            if (tile.getRequirement() > 1 && progress != null) {
                body.add(progressBar(progress.getCurrentValue(), progress.getRequiredValue(),
                    BingoColors.tileAccent(typeName), nf));
            } else if (complete) {
                JLabel done = new JLabel("\u2713 Completed");
                done.setFont(FontManager.getRunescapeSmallFont());
                done.setForeground(BingoColors.GREEN_LIGHT);
                done.setAlignmentX(Component.LEFT_ALIGNMENT);
                body.add(done);
            } else {
                JLabel pending = new JLabel("Not yet completed");
                pending.setFont(FontManager.getRunescapeSmallFont());
                pending.setForeground(BingoColors.PARCHMENT_FAINT);
                pending.setAlignmentX(Component.LEFT_ALIGNMENT);
                body.add(pending);
            }

            // Completed-by attribution
            if (complete && progress != null && progress.getCompletedBy() != null) {
                body.add(Box.createRigidArea(new Dimension(0, 6)));
                JLabel by = new JLabel("Completed by: " + progress.getCompletedBy());
                by.setFont(FontManager.getRunescapeSmallFont());
                by.setForeground(BingoColors.PARCHMENT_DIM);
                by.setAlignmentX(Component.LEFT_ALIGNMENT);
                body.add(by);
            }
        }

        return body;
    }

    // -------------------------------------------------------------------------
    // Footer — close button
    // -------------------------------------------------------------------------

    private static JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, INSET, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(BingoColors.SURFACE_2.darker());
                g.fillRect(0, 0, getWidth(), 1); // top divider line
                g.setColor(BingoColors.SURFACE_2);
                g.fillRect(0, 1, getWidth(), getHeight() - 1);
            }
        };
        footer.setOpaque(false);

        JButton close = new JButton("Close");
        close.setFont(FontManager.getRunescapeSmallFont());
        close.setBackground(BingoColors.SURFACE_3);
        close.setForeground(BingoColors.PARCHMENT);
        close.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BingoColors.BORDER_BRIGHT, 1),
            new EmptyBorder(3, 10, 3, 10)));
        close.setFocusPainted(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.addActionListener(e -> SwingUtilities.getWindowAncestor(close).dispose());

        footer.add(close);
        return footer;
    }

    // -------------------------------------------------------------------------
    // Row helpers
    // -------------------------------------------------------------------------

    private static JPanel tierRow(int num, long threshold, int pts, boolean reached, NumberFormat nf, Color tierColor) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        row.setBorder(new EmptyBorder(1, 0, 1, 0));

        // Star icon
        JLabel star = new JLabel(reached ? "\u2605" : "\u2606");
        star.setFont(new Font("Dialog", Font.PLAIN, 11));
        star.setForeground(reached ? tierColor : BingoColors.PARCHMENT_FAINT);

        // Label: "Tier N: X,XXX"
        JLabel label = new JLabel("Tier " + num + ": " + nf.format(threshold));
        label.setFont(FontManager.getRunescapeSmallFont());
        label.setForeground(reached ? BingoColors.PARCHMENT : BingoColors.PARCHMENT_DIM);

        // Right: "Xpts ✓"
        String ptText = pts + " pts" + (reached ? "  \u2713" : "");
        JLabel ptsLabel = new JLabel(ptText);
        ptsLabel.setFont(FontManager.getRunescapeSmallFont());
        ptsLabel.setForeground(reached ? tierColor : BingoColors.PARCHMENT_FAINT);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        left.setOpaque(false);
        left.add(star);
        left.add(label);

        row.add(left, BorderLayout.WEST);
        row.add(ptsLabel, BorderLayout.EAST);
        return row;
    }

    private static JPanel progressBar(long current, long required, Color accent, NumberFormat nf) {
        JPanel wrap = new JPanel();
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setOpaque(false);
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);

        float ratio = required > 0 ? Math.min(1f, (float) current / required) : 0f;

        // Bar
        JPanel bar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                int w = getWidth();
                int h = getHeight();
                g2.setColor(BingoColors.SURFACE_3);
                g2.fillRoundRect(0, 0, w, h, 4, 4);
                if (ratio > 0f) {
                    g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 200));
                    g2.fillRoundRect(0, 0, Math.max(4, (int)(w * ratio)), h, 4, 4);
                }
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 8));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.add(bar);

        wrap.add(Box.createRigidArea(new Dimension(0, 3)));

        // Numeric label
        JLabel nums = new JLabel(nf.format(current) + " / " + nf.format(required)
            + "  (" + (int)(ratio * 100) + "%)");
        nums.setFont(FontManager.getRunescapeSmallFont());
        nums.setForeground(BingoColors.PARCHMENT_DIM);
        nums.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.add(nums);

        return wrap;
    }

    private static JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text.toUpperCase());
        label.setFont(FontManager.getRunescapeSmallFont());
        label.setForeground(BingoColors.PARCHMENT_FAINT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static long nextTierThreshold(BingoTile tile, int currentTier) {
        if (currentTier < 1 && tile.getTier1Threshold() != null) return tile.getTier1Threshold();
        if (currentTier < 2 && tile.getTier2Threshold() != null) return tile.getTier2Threshold();
        if (currentTier < 3 && tile.getTier3Threshold() != null) return tile.getTier3Threshold();
        return 0;
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------

    public static void show(Window owner, BingoTile tile, TileProgress progress) {
        TileDetailDialog dialog = new TileDetailDialog(owner, tile, progress);
        dialog.setVisible(true);
    }
}
