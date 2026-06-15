package com.guessinggame.gui;

import com.guessinggame.model.GameSession;
import com.guessinggame.model.Level;
import com.guessinggame.model.PowerUp;
import com.guessinggame.model.SessionStats;
import com.guessinggame.util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MainFrame extends JFrame {

    private static final String CARD_WELCOME = "WELCOME";
    private static final String CARD_GAME    = "GAME";

    private final CardLayout   cards = new CardLayout();
    private final JPanel       root  = new JPanel(cards);

    // Stats are loaded from file at startup and auto-saved after every game.
    private final SessionStats stats = SessionStats.loadFromFile();

    private WelcomePanel welcome;
    private GamePanel    game;

    public MainFrame() {
        super("Number Guessing Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(760, 620);
        setMinimumSize(new Dimension(660, 540));
        setLocationRelativeTo(null);

        javax.swing.ImageIcon appIcon = UIConstants.loadIcon("app_logo.svg", 32, 32);
        if (appIcon != null) setIconImage(appIcon.getImage());

        // Save stats cleanly whenever the JVM exits (window close, System.exit, etc.)
        Runtime.getRuntime().addShutdownHook(new Thread(stats::saveToFile));

        welcome = new WelcomePanel(this, stats);
        root.add(welcome, CARD_WELCOME);
        setContentPane(root);
        
        cards.show(root, CARD_WELCOME);
    }

    /**
     * Starts a new game at the chosen difficulty.
     *
     * If the player's power-up inventory is completely empty, they are shown
     * an educational selection dialog first (Option B safety net). The dialog
     * explains each power-up and lets them pick one for free before the game
     * begins. They may also skip and play without one.
     */
    public void startGame(Level level) {
        if (stats.getTotalPowerUpCount() == 0) {
            showFreePowerUpDialog();
        }

        if (game != null) root.remove(game);
        GameSession session = new GameSession(level);
        game = new GamePanel(this, session, stats);
        root.add(game, CARD_GAME);
        root.revalidate();
        root.repaint();
        cards.show(root, CARD_GAME);
    }

    /** Returns to the welcome screen, rebuilding it so stats refresh instantly. */
    public void showWelcome() {
        if (game != null) { root.remove(game); game = null; }
        root.remove(welcome);
        welcome = new WelcomePanel(this, stats);
        root.add(welcome, CARD_WELCOME);
        root.revalidate();
        root.repaint();
        cards.show(root, CARD_WELCOME);
    }

    // ── Free power-up pick dialog ─────────────────────────────────────────

    /**
     * Shows an educational modal dialog that explains each power-up and allows
     * the player to pick one for free. Triggered whenever the inventory is empty.
     *
     * The dialog cannot be closed without making a choice or clicking Skip,
     * so the player is guaranteed to at least read the options.
     */
    private void showFreePowerUpDialog() {
        JDialog dlg = new JDialog(this, "Your First Power-Up", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(560, 460);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        // Prevent closing via the X button — player must pick or skip
        dlg.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Color.WHITE);

        // ── Header ────────────────────────────────────────────────────
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(UIConstants.SECONDARY);
        header.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel titleLbl = new JLabel("Your First Power-Up!");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titleLbl.setForeground(Color.WHITE);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLbl = new JLabel(
            "You have no power-ups. Pick one to take into your game.");
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLbl.setForeground(new Color(255, 235, 190));
        subLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(titleLbl);
        header.add(Box.createVerticalStrut(4));
        header.add(subLbl);
        content.add(header, BorderLayout.NORTH);

        // ── 2 × 2 card grid ───────────────────────────────────────────
        JPanel cardsGrid = new JPanel(new GridLayout(2, 2, 10, 10));
        cardsGrid.setBackground(new Color(245, 240, 255));
        cardsGrid.setBorder(new EmptyBorder(14, 14, 8, 14));

        for (PowerUp pu : PowerUp.values()) {
            cardsGrid.add(buildPickCard(pu, dlg));
        }
        content.add(cardsGrid, BorderLayout.CENTER);

        // ── Skip link ─────────────────────────────────────────────────
        JButton btnSkip = new JButton("Skip — play without a power-up");
        btnSkip.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnSkip.setForeground(new Color(120, 80, 160));
        btnSkip.setBackground(Color.WHITE);
        btnSkip.setBorderPainted(false);
        btnSkip.setContentAreaFilled(false);
        btnSkip.setFocusPainted(false);
        btnSkip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSkip.addActionListener(e -> dlg.dispose());

        JPanel skipRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
        skipRow.setBackground(Color.WHITE);
        skipRow.add(btnSkip);
        content.add(skipRow, BorderLayout.SOUTH);

        dlg.setContentPane(content);
        dlg.setVisible(true);   // blocks until disposed
    }

    /**
     * Builds a single power-up card for the free-pick dialog.
     * Shows the name, what it does, a "best used when" tip, and a Choose button.
     */
    private JPanel buildPickCard(PowerUp pu, JDialog owner) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 5, 0, 0, pu.getColor()),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 200, 225)),
                new EmptyBorder(10, 12, 10, 12)
            )
        ));

        // Name
        JLabel nameLbl = new JLabel(pu.getDisplayName());
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLbl.setForeground(pu.getColor());

        // Description
        JLabel descLbl = new JLabel(
            "<html><body style='width:170px'>" + pu.getDescription() + "</body></html>");
        descLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        descLbl.setForeground(new Color(60, 60, 60));

        // Tip
        JLabel tipLbl = new JLabel(
            "<html><body style='width:170px'><i>" + pu.getTip() + "</i></body></html>");
        tipLbl.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        tipLbl.setForeground(new Color(110, 80, 140));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        info.add(nameLbl);
        info.add(Box.createVerticalStrut(5));
        info.add(descLbl);
        info.add(Box.createVerticalStrut(4));
        info.add(tipLbl);

        // Choose button
        JButton btnChoose = new JButton("Choose");
        btnChoose.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        btnChoose.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnChoose.setBackground(pu.getColor());
        btnChoose.setForeground(Color.WHITE);
        btnChoose.setFocusPainted(false);
        btnChoose.setBorderPainted(false);
        btnChoose.setOpaque(true);
        btnChoose.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnChoose.addActionListener(e -> {
            stats.awardSpecificPowerUp(pu);
            owner.dispose();
        });

        card.add(info,      BorderLayout.CENTER);
        card.add(btnChoose, BorderLayout.SOUTH);
        return card;
    }
}
