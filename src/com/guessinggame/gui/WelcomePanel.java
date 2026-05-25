package com.guessinggame.gui;

import com.guessinggame.model.Level;
import com.guessinggame.model.SessionStats;
import com.guessinggame.util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

public class WelcomePanel extends JPanel {

    private final MainFrame    frame;
    private final SessionStats stats;

    public WelcomePanel(MainFrame frame, SessionStats stats) {
        this.frame = frame;
        this.stats = stats;
        setLayout(new BorderLayout());
        setBackground(UIConstants.BACKGROUND);
        buildUI();
    }

    private void buildUI() {

        // ── Title bar ─────────────────────────────────────────────────
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(UIConstants.PRIMARY);
        titleBar.setBorder(new EmptyBorder(18, 24, 14, 24));

        JLabel titleLbl = new JLabel("Number Guessing Game");
        titleLbl.setFont(UIConstants.FONT_TITLE);
        titleLbl.setForeground(Color.WHITE);

        JLabel subLbl = new JLabel(
            "Four difficulty levels. Ranges from 1–50 up to 1–1000. How sharp are you?");
        subLbl.setFont(UIConstants.FONT_BODY);
        subLbl.setForeground(new Color(220, 200, 255));

        JPanel titleStack = new JPanel();
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));
        titleStack.setOpaque(false);
        titleStack.add(titleLbl);
        titleStack.add(Box.createVerticalStrut(4));
        titleStack.add(subLbl);
        titleBar.add(titleStack, BorderLayout.CENTER);

        // ── Session stats bar ─────────────────────────────────────────
        JPanel statsBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 28, 8));
        statsBar.setBackground(UIConstants.PRIMARY_DARK);
        statsBar.add(statWidget("Played",     String.valueOf(stats.getGamesPlayed())));
        statsBar.add(statSep());
        statsBar.add(statWidget("Won",        String.valueOf(stats.getGamesWon())));
        statsBar.add(statSep());
        statsBar.add(statWidget("Lost",       String.valueOf(stats.getGamesLost())));
        statsBar.add(statSep());
        statsBar.add(statWidget("Best",       stats.getBestGuesses()));
        statsBar.add(statSep());
        statsBar.add(statWidget("High Score", stats.getHighScoreDisplay()));

        JPanel north = new JPanel(new BorderLayout());
        north.add(titleBar, BorderLayout.NORTH);
        north.add(statsBar, BorderLayout.SOUTH);

        // ── Level selection cards ─────────────────────────────────────
        JPanel cardsGrid = new JPanel(new GridLayout(2, 2, 14, 14));
        cardsGrid.setBackground(UIConstants.BACKGROUND);
        cardsGrid.setBorder(new EmptyBorder(20, 24, 8, 24));

        for (Level lv : Level.values()) {
            cardsGrid.add(buildLevelCard(lv));
        }

        // ── How to play + Exit ────────────────────────────────────────
        JLabel howLbl = new JLabel(
            "<html><center>Pick a level — each one strips away a layer of help." +
            " Beginner gives warm/cold hints and a visual range bar." +
            " Expert gives you nothing but <i>Higher</i> or <i>Lower</i>." +
            " Win games to earn <b>Power-Ups</b> (Cut, Shield, Peek, Freeze)" +
            " you can use in your next game.</center></html>"
        );
        howLbl.setFont(UIConstants.FONT_SMALL);
        howLbl.setForeground(new Color(90, 50, 120));
        howLbl.setHorizontalAlignment(SwingConstants.CENTER);
        howLbl.setBorder(new EmptyBorder(6, 30, 6, 30));

        JButton btnExit = actionButton("  Exit  ", UIConstants.DANGER, Color.WHITE);
        applyIcon(btnExit, "exit.svg");
        btnExit.addActionListener(e -> System.exit(0));

        JPanel exitRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 6));
        exitRow.setBackground(UIConstants.BACKGROUND);
        exitRow.add(btnExit);

        JPanel south = new JPanel(new BorderLayout());
        south.setBackground(UIConstants.BACKGROUND);
        south.add(howLbl,  BorderLayout.CENTER);
        south.add(exitRow, BorderLayout.SOUTH);

        add(north,     BorderLayout.NORTH);
        add(cardsGrid, BorderLayout.CENTER);
        add(south,     BorderLayout.SOUTH);
    }

    // ── Level card ────────────────────────────────────────────────────────

    private JPanel buildLevelCard(Level lv) {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 6, 0, 0, lv.getColor()),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR),
                new EmptyBorder(12, 14, 12, 14)
            )
        ));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Info column
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel nameLbl = new JLabel(lv.getDisplayName());
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 17));
        nameLbl.setForeground(lv.getColor());
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameLbl.setIconTextGap(8);
        javax.swing.ImageIcon lvIcon = UIConstants.loadIconTinted(lv.getIconName(), lv.getColor());
        if (lvIcon != null) nameLbl.setIcon(lvIcon);

        JLabel descLbl = new JLabel(lv.getDescription());
        descLbl.setFont(UIConstants.FONT_SMALL);
        descLbl.setForeground(Color.GRAY);
        descLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Combined range + guess count line
        JLabel rangeLbl = new JLabel(lv.getRangeLabel());
        rangeLbl.setFont(UIConstants.FONT_BOLD);
        rangeLbl.setForeground(new Color(80, 40, 110));
        rangeLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Per-level best score (only shown if the player has won at this level)
        int bestScore = stats.getBestScore(lv);

        info.add(nameLbl);
        info.add(Box.createVerticalStrut(6));
        info.add(descLbl);
        info.add(Box.createVerticalStrut(5));
        info.add(rangeLbl);

        if (bestScore > 0) {
            JLabel bestLbl = new JLabel("Best: " + String.format("%,d", bestScore) + " pts");
            bestLbl.setFont(UIConstants.FONT_SMALL);
            bestLbl.setForeground(UIConstants.SECONDARY);
            bestLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            info.add(Box.createVerticalStrut(3));
            info.add(bestLbl);
        }

        // Play button
        JButton btnPlay = new JButton("Play");
        btnPlay.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        btnPlay.setFont(UIConstants.FONT_BUTTON);
        btnPlay.setBackground(lv.getColor());
        btnPlay.setForeground(Color.WHITE);
        btnPlay.setFocusPainted(false);
        btnPlay.setBorderPainted(false);
        btnPlay.setOpaque(true);
        btnPlay.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPlay.setMargin(new Insets(8, 16, 8, 16));
        btnPlay.setIconTextGap(6);
        applyIcon(btnPlay, "play.svg");
        btnPlay.addActionListener(e -> frame.startGame(lv));

        JPanel btnWrapper = new JPanel(new GridBagLayout());
        btnWrapper.setOpaque(false);
        btnWrapper.add(btnPlay);

        card.add(info,       BorderLayout.CENTER);
        card.add(btnWrapper, BorderLayout.EAST);

        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { card.setBackground(UIConstants.CARD_HOVER); }
            @Override public void mouseExited (MouseEvent e) { card.setBackground(Color.WHITE);            }
            @Override public void mouseClicked(MouseEvent e) { frame.startGame(lv);                        }
        });

        return card;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private JPanel statWidget(String label, String value) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 2));
        p.setOpaque(false);
        JLabel vLbl = new JLabel(value, SwingConstants.CENTER);
        vLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        vLbl.setForeground(UIConstants.SECONDARY);
        JLabel kLbl = new JLabel(label, SwingConstants.CENTER);
        kLbl.setFont(UIConstants.FONT_SMALL);
        kLbl.setForeground(new Color(200, 180, 220));
        p.add(vLbl);
        p.add(kLbl);
        return p;
    }

    private JLabel statSep() {
        JLabel l = new JLabel("|");
        l.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        l.setForeground(new Color(120, 80, 160));
        return l;
    }

    private JButton actionButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        b.setFont(UIConstants.FONT_BUTTON);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMargin(new Insets(6, 14, 6, 14));
        b.setIconTextGap(6);
        return b;
    }

    private void applyIcon(JButton btn, String iconFile) {
        javax.swing.ImageIcon ic = UIConstants.loadIcon(iconFile);
        if (ic != null) btn.setIcon(ic);
    }
}
