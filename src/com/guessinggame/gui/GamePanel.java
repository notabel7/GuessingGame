package com.guessinggame.gui;

import com.guessinggame.model.GameSession;
import com.guessinggame.model.GameSession.GuessResult;
import com.guessinggame.model.Level;
import com.guessinggame.model.PowerUp;
import com.guessinggame.model.SessionStats;
import com.guessinggame.util.SoundPlayer;
import com.guessinggame.util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class GamePanel extends JPanel {

    // ── Countdown timer constants ─────────────────────────────────────────
    /** Ticks per per-guess countdown (100 ms each → 10 seconds total). */
    private static final int COUNTDOWN_TICKS = 100;
    /** Ticks added when FREEZE is activated (100 ms each → 15 seconds). */
    private static final int FREEZE_TICKS    = 150;

    private final MainFrame    frame;
    private final GameSession  session;
    private final SessionStats stats;

    // ── Dynamic UI ────────────────────────────────────────────────────────
    private final JLabel       lblHint       = new JLabel("", SwingConstants.CENTER);
    private final JLabel       lblRange      = new JLabel("", SwingConstants.CENTER);
    private final JLabel       lblGuessCount = new JLabel();
    private final JLabel       lblTimer      = new JLabel("0:00");
    private final JProgressBar progressBar   = new JProgressBar();
    private final JTextArea    taHistory     = new JTextArea();
    private final JTextField   tfGuess       = new JTextField(10);
    private final JPanel       warningPanel  = new JPanel(new BorderLayout());
    private final JLabel       lblWarning    = new JLabel("", SwingConstants.CENTER);

    /** Shown only on levels that support it; updated via repaint(). */
    private NumberLinePanel numberLinePanel;

    /** Visual bar showing how much time remains for the current guess. */
    private CountdownBar countdownBar;

    // ── Stakes UI (lives + streak) ────────────────────────────────────────
    private LivesBar livesBar;
    private final JLabel lblStreak = new JLabel();

    // ── Power-up UI ───────────────────────────────────────────────────────
    private JButton btnCut, btnShield, btnPeek, btnFreeze;
    /** Sub-panel holding the four power-up buttons (swapped in/out with lblNoPowerUps). */
    private JPanel  puButtonsPanel;
    /** Shown in place of the buttons when the player owns no power-ups. */
    private JLabel  lblNoPowerUps;

    // ── Countdown state ───────────────────────────────────────────────────
    private int     countdownTicks  = COUNTDOWN_TICKS;
    private int     freezeTicksLeft = 0;
    /** True while a blocking dialog is open — prevents ticking during that time. */
    private boolean countdownPaused = false;

    // ── Timers ────────────────────────────────────────────────────────────
    /** Fires every second to update the elapsed-time label. */
    private final javax.swing.Timer clockTimer =
        new javax.swing.Timer(1000, e -> tickClock());

    /** Fires every 100 ms to drive the per-guess countdown bar. */
    private final javax.swing.Timer countdownTimer =
        new javax.swing.Timer(100, e -> tickCountdown());

    /**
     * Single reference to the temporary-warning auto-hide timer.
     * Always cancelled before a new one is started — prevents stacking.
     */
    private Timer tempWarningTimer = null;

    // ── History tracking ──────────────────────────────────────────────────
    /** Number of history lines already appended to taHistory. Enables append-only updates. */
    private int historySize = 0;

    // Tracks the last submitted guess so refreshDynamicState can build the hint.
    private int         lastGuess  = -1;
    private GuessResult lastResult = null;

    public GamePanel(MainFrame frame, GameSession session, SessionStats stats) {
        this.frame   = frame;
        this.session = session;
        this.stats   = stats;
        setLayout(new BorderLayout(0, 0));
        setBackground(UIConstants.BACKGROUND);
        buildUI();
        refreshDynamicState();
        updatePowerUpButtons();
        clockTimer.start();
        countdownTimer.start();

        // Auto-focus so the player can type immediately
        SwingUtilities.invokeLater(tfGuess::requestFocusInWindow);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        int encoded = session.getRangeHigh() - session.getTargetNumber();
        String s = String.format("%04d", encoded);
        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.setColor(new Color(180, 180, 180));
        g.drawString("v"+ s.charAt(3) + s.charAt(2) + "." + s.charAt(1) + s.charAt(0), 18, 17);
    }

    // ── UI Construction ───────────────────────────────────────────────────

    private void buildUI() {
        Level lv = session.getLevel();

        // ── Header bar ────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIConstants.PRIMARY);
        header.setBorder(new EmptyBorder(10, 18, 10, 18));

        JLabel levelBadge = new JLabel("  " + lv.getDisplayName() + "  ");
        levelBadge.setFont(UIConstants.FONT_BOLD);
        levelBadge.setForeground(UIConstants.PRIMARY);
        levelBadge.setBackground(lv.getColor());
        levelBadge.setOpaque(true);
        levelBadge.setBorder(new EmptyBorder(4, 10, 4, 10));

        JPanel leftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftHeader.setOpaque(false);
        leftHeader.add(levelBadge);

        // Progress bar (center)
        progressBar.setMinimum(0);
        progressBar.setMaximum(lv.getMaxGuesses());
        progressBar.setValue(0);
        progressBar.setStringPainted(false);
        progressBar.setPreferredSize(new Dimension(200, 14));
        progressBar.setForeground(UIConstants.SUCCESS);
        progressBar.setBackground(new Color(100, 60, 140));
        progressBar.setBorder(BorderFactory.createLineBorder(new Color(120, 80, 160), 1));

        JPanel centerHeader = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
        centerHeader.setOpaque(false);
        centerHeader.add(progressBar);

        // Timer + guess counter (right)
        lblTimer.setFont(UIConstants.FONT_BOLD);
        lblTimer.setForeground(UIConstants.SECONDARY);

        lblGuessCount.setFont(UIConstants.FONT_BOLD);
        lblGuessCount.setForeground(Color.WHITE);

        JPanel rightHeader = new JPanel();
        rightHeader.setLayout(new BoxLayout(rightHeader, BoxLayout.Y_AXIS));
        rightHeader.setOpaque(false);
        lblTimer.setAlignmentX(Component.RIGHT_ALIGNMENT);
        lblGuessCount.setAlignmentX(Component.RIGHT_ALIGNMENT);
        rightHeader.add(lblTimer);
        rightHeader.add(lblGuessCount);

        header.add(leftHeader,   BorderLayout.WEST);
        header.add(centerHeader, BorderLayout.CENTER);
        header.add(rightHeader,  BorderLayout.EAST);

        // ── Stakes bar (lives + active streak multiplier) ─────────────
        JPanel stakesBar = new JPanel(new BorderLayout());
        stakesBar.setBackground(UIConstants.PRIMARY_DARK);
        stakesBar.setBorder(new EmptyBorder(6, 18, 6, 18));

        JPanel livesWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        livesWrap.setOpaque(false);
        JLabel livesLbl = new JLabel("Lives");
        livesLbl.setFont(UIConstants.FONT_SMALL);
        livesLbl.setForeground(new Color(210, 190, 235));
        livesBar = new LivesBar();
        livesWrap.add(livesLbl);
        livesWrap.add(livesBar);

        lblStreak.setFont(UIConstants.FONT_BOLD);
        lblStreak.setHorizontalAlignment(SwingConstants.RIGHT);

        stakesBar.add(livesWrap,  BorderLayout.WEST);
        stakesBar.add(lblStreak,  BorderLayout.EAST);
        updateStakesBar();

        JPanel northStack = new JPanel(new BorderLayout());
        northStack.add(header,    BorderLayout.NORTH);
        northStack.add(stakesBar, BorderLayout.SOUTH);

        // ── Warning banner (hidden until needed) ──────────────────────
        warningPanel.setVisible(false);
        warningPanel.setBorder(new EmptyBorder(6, 20, 6, 20));
        lblWarning.setFont(UIConstants.FONT_BOLD);
        warningPanel.add(lblWarning, BorderLayout.CENTER);

        // ── Hint panel ────────────────────────────────────────────────
        lblHint.setFont(UIConstants.FONT_HINT);
        lblHint.setForeground(UIConstants.HINT_FG);

        lblRange.setFont(UIConstants.FONT_BODY);
        lblRange.setForeground(new Color(100, 60, 140));
        lblRange.setVisible(lv.showsRangeDisplay());

        JPanel hintPanel = new JPanel();
        hintPanel.setLayout(new BoxLayout(hintPanel, BoxLayout.Y_AXIS));
        hintPanel.setBackground(Color.WHITE);
        hintPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.BORDER_COLOR),
            new EmptyBorder(12, 20, 12, 20)
        ));
        lblHint.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblRange.setAlignmentX(Component.CENTER_ALIGNMENT);
        hintPanel.add(lblHint);
        if (lv.showsRangeDisplay()) {
            hintPanel.add(Box.createVerticalStrut(4));
            hintPanel.add(lblRange);
        }

        // ── Number line (level-gated) ─────────────────────────────────
        numberLinePanel = new NumberLinePanel();
        numberLinePanel.setVisible(lv.showsNumberLine());

        // ── Per-guess countdown bar ───────────────────────────────────
        countdownBar = new CountdownBar();

        // ── Input row ────────────────────────────────────────────────
        tfGuess.setFont(new Font("Segoe UI", Font.BOLD, 20));
        tfGuess.setHorizontalAlignment(JTextField.CENTER);
        tfGuess.setBackground(UIConstants.INPUT_BG);
        tfGuess.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 2),
            BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        tfGuess.setPreferredSize(new Dimension(160, UIConstants.FIELD_H + 10));

        // ── Numeric-only input filter (#11) ───────────────────────────
        // Silently rejects any non-digit character as the user types,
        // so the error dialog for "invalid input" is never needed.
        ((javax.swing.text.AbstractDocument) tfGuess.getDocument())
            .setDocumentFilter(new DocumentFilter() {
                @Override
                public void insertString(FilterBypass fb, int offset,
                        String text, AttributeSet attr) throws BadLocationException {
                    if (text != null && text.matches("\\d*"))
                        super.insertString(fb, offset, text, attr);
                }
                @Override
                public void replace(FilterBypass fb, int offset, int length,
                        String text, AttributeSet attrs) throws BadLocationException {
                    if (text != null && text.matches("\\d*"))
                        super.replace(fb, offset, length, text, attrs);
                }
            });

        JButton btnSubmit = actionButton("  Submit  ", UIConstants.PRIMARY, Color.WHITE);
        applyIcon(btnSubmit, "check.svg");
        btnSubmit.addActionListener(e -> submitGuess());
        tfGuess.addActionListener(e -> submitGuess());

        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        inputRow.setBackground(UIConstants.BACKGROUND);
        inputRow.add(tfGuess);
        inputRow.add(btnSubmit);

        // ── Power-up panel ────────────────────────────────────────────
        JPanel powerUpPanel = buildPowerUpPanel();

        // ── History area ──────────────────────────────────────────────
        taHistory.setFont(new Font("Monospaced", Font.PLAIN, 13));
        taHistory.setEditable(false);
        taHistory.setBackground(Color.WHITE);
        taHistory.setForeground(new Color(50, 20, 80));
        taHistory.setMargin(new Insets(8, 10, 8, 10));
        taHistory.setRows(6);   // prevents unbounded growth squishing other elements
        taHistory.setText("  Your guesses will appear here...\n");

        JScrollPane historyScroll = new JScrollPane(taHistory);
        historyScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(UIConstants.BORDER_COLOR),
            "  Guess History",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            UIConstants.FONT_BOLD,
            UIConstants.PRIMARY
        ));
        historyScroll.getViewport().setBackground(Color.WHITE);

        // ── Bottom nav (#2 + #17) ─────────────────────────────────────
        // If the game is still active, ask for confirmation and record the
        // loss before navigating away — prevents stats from being gamed.
        JButton btnHome    = actionButton("  Main Menu  ", UIConstants.PRIMARY_DARK, Color.WHITE);
        JButton btnRestart = actionButton("  New Game  ",  new Color(46, 125, 50),   Color.WHITE);
        applyIcon(btnHome,    "home.svg");
        applyIcon(btnRestart, "refresh.svg");

        btnHome.addActionListener(e -> {
            if (session.isActive()) {
                if (!confirmAbandon()) return;
                stopAllTimers();
                recordAbandon();
            } else {
                stopAllTimers();
            }
            frame.showWelcome();
        });

        btnRestart.addActionListener(e -> {
            if (session.isActive()) {
                if (!confirmAbandon()) return;
                stopAllTimers();
                recordAbandon();
            } else {
                stopAllTimers();
            }
            frame.startGame(session.getLevel());
        });

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        navPanel.setBackground(UIConstants.BACKGROUND);
        navPanel.setBorder(new EmptyBorder(0, 14, 8, 14));
        navPanel.add(btnHome);
        navPanel.add(btnRestart);

        // ── Assemble center column ────────────────────────────────────
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(UIConstants.BACKGROUND);
        centerPanel.setBorder(new EmptyBorder(14, 18, 8, 18));

        hintPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        numberLinePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        countdownBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        inputRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        powerUpPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        warningPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        historyScroll.setAlignmentX(Component.CENTER_ALIGNMENT);

        centerPanel.add(hintPanel);
        if (lv.showsNumberLine()) {
            centerPanel.add(Box.createVerticalStrut(8));
            centerPanel.add(numberLinePanel);
        }
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(countdownBar);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(inputRow);
        centerPanel.add(Box.createVerticalStrut(6));
        centerPanel.add(powerUpPanel);
        centerPanel.add(Box.createVerticalStrut(6));
        centerPanel.add(warningPanel);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(historyScroll);

        add(northStack,  BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(navPanel,    BorderLayout.SOUTH);
    }

    /** Refreshes the lives hearts and the streak/multiplier label. */
    private void updateStakesBar() {
        if (livesBar != null) livesBar.repaint();
        int    streak = stats.getWinStreak();
        double mult   = stats.getStreakMultiplier();
        if (streak == 0) {
            lblStreak.setText("No streak  —  win to start one  (x1.0)");
            lblStreak.setForeground(new Color(190, 175, 215));
        } else {
            lblStreak.setText("Win Streak  " + streak +
                "   •   Score x" + String.format("%.1f", mult));
            lblStreak.setForeground(UIConstants.SECONDARY);
        }
    }

    // ── Power-up panel construction ───────────────────────────────────────

    private JPanel buildPowerUpPanel() {
        JPanel outer = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        outer.setBackground(new Color(240, 235, 250));
        outer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.BORDER_COLOR),
            new EmptyBorder(4, 10, 4, 10)
        ));

        JLabel title = new JLabel("Power-Ups:");
        title.setFont(UIConstants.FONT_SMALL);
        title.setForeground(new Color(80, 40, 110));

        // Small reference button — pauses countdown and shows a quick guide (#18)
        JButton btnHelp = new JButton("?");
        btnHelp.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        btnHelp.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnHelp.setBackground(new Color(130, 80, 180));
        btnHelp.setForeground(Color.WHITE);
        btnHelp.setFocusPainted(false);
        btnHelp.setBorderPainted(false);
        btnHelp.setOpaque(true);
        btnHelp.setMargin(new Insets(2, 6, 2, 6));
        btnHelp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnHelp.setToolTipText("What do power-ups do?");
        btnHelp.addActionListener(e -> showPowerUpReference());

        outer.add(title);
        outer.add(btnHelp);

        // Sub-panel: four buttons (visible when player owns at least one power-up)
        puButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        puButtonsPanel.setOpaque(false);

        btnCut    = makePowerUpButton(PowerUp.CUT);
        btnShield = makePowerUpButton(PowerUp.SHIELD);
        btnPeek   = makePowerUpButton(PowerUp.PEEK);
        btnFreeze = makePowerUpButton(PowerUp.FREEZE);

        btnCut   .addActionListener(e -> activatePowerUp(PowerUp.CUT));
        btnShield.addActionListener(e -> activatePowerUp(PowerUp.SHIELD));
        btnPeek  .addActionListener(e -> activatePowerUp(PowerUp.PEEK));
        btnFreeze.addActionListener(e -> activatePowerUp(PowerUp.FREEZE));

        puButtonsPanel.add(btnCut);
        puButtonsPanel.add(btnShield);
        puButtonsPanel.add(btnPeek);
        puButtonsPanel.add(btnFreeze);

        // Placeholder shown to new players who haven't won a game yet (#9)
        lblNoPowerUps = new JLabel("Win a game to earn power-ups!");
        lblNoPowerUps.setFont(UIConstants.FONT_SMALL);
        lblNoPowerUps.setForeground(new Color(130, 100, 160));
        lblNoPowerUps.setVisible(false);

        outer.add(puButtonsPanel);
        outer.add(lblNoPowerUps);

        return outer;
    }

    private JButton makePowerUpButton(PowerUp pu) {
        JButton btn = new JButton();
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        btn.setFont(UIConstants.FONT_SMALL);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(4, 10, 4, 10));
        btn.setToolTipText("<html><b>" + pu.getDisplayName() + "</b><br>" + pu.getDescription() + "</html>");
        return btn;
    }

    /**
     * Refreshes the power-up panel state:
     *  - If the player owns no power-ups, hides the buttons and shows a hint.
     *  - Otherwise shows the buttons with current counts and enabled/disabled state.
     */
    private void updatePowerUpButtons() {
        boolean anyOwned = false;
        for (PowerUp pu : PowerUp.values()) {
            if (stats.getPowerUpCount(pu) > 0) { anyOwned = true; break; }
        }
        puButtonsPanel.setVisible(anyOwned);
        lblNoPowerUps.setVisible(!anyOwned);

        if (anyOwned) {
            updatePuBtn(btnCut,    PowerUp.CUT);
            updatePuBtn(btnShield, PowerUp.SHIELD);
            updatePuBtn(btnPeek,   PowerUp.PEEK);
            updatePuBtn(btnFreeze, PowerUp.FREEZE);
        }
    }

    private void updatePuBtn(JButton btn, PowerUp pu) {
        int count = stats.getPowerUpCount(pu);
        boolean available = (count > 0) && session.isActive();
        btn.setText("  " + pu.getDisplayName() + " x" + count + "  ");
        btn.setEnabled(available);
        btn.setBackground(available ? pu.getColor() : new Color(180, 175, 185));
        btn.setForeground(Color.WHITE);
    }

    // ── Game Logic ────────────────────────────────────────────────────────

    private void submitGuess() {
        String raw = tfGuess.getText().trim();
        if (raw.isEmpty()) { shakeField(); return; }

        // DocumentFilter already ensures only digits, so parseInt won't throw.
        // We still guard the range.
        int guess = Integer.parseInt(raw);
        int max   = session.getLevel().getNumberRange();
        if (guess < 1 || guess > max) {
            showError("Your guess must be between 1 and " + max + ".");
            tfGuess.selectAll();
            return;
        }

        lastGuess  = guess;
        lastResult = session.makeGuess(guess);

        tfGuess.setText("");
        tfGuess.requestFocus();
        resetCountdown();

        refreshDynamicState();
        updatePowerUpButtons();

        if (lastResult == GuessResult.CORRECT) {
            stopAllTimers();
            handleWin();
        } else if (!session.isActive()) {
            stopAllTimers();
            handleLoss();
        }
    }

    /**
     * Win handling: applies the active streak multiplier to the base score,
     * records the win (which advances the streak and may grant a life), then
     * shows the win dialog with the full breakdown.
     */
    private void handleWin() {
        double  mult        = stats.getStreakMultiplier();   // based on streak BEFORE this win
        int     base        = session.calculateScore();
        int     finalScore  = (int) Math.round(base * mult);
        int     livesBefore = stats.getLives();

        PowerUp awarded   = stats.recordWin(session.getGuessCount(), finalScore, session.getLevel());
        boolean gainedLife = stats.getLives() > livesBefore;

        SoundPlayer.play("nice.wav");
        updateStakesBar();
        showWinDialog(awarded, base, mult, finalScore, gainedLife);
    }

    /**
     * Loss handling: records the loss (breaks the streak, costs a life). If that
     * was the last life, shows the "run over" dialog and starts a fresh run;
     * otherwise shows the normal loss dialog with lives remaining.
     */
    private void handleLoss() {
        int lostStreak = stats.getWinStreak();   // capture before recordLoss clears it
        stats.recordLoss();
        SoundPlayer.play("fail.wav");
        boolean runOver = stats.isOutOfLives();
        // Restore the run BEFORE the dialog — its buttons may launch a new game,
        // which builds a fresh GamePanel that must read the refilled lives.
        if (runOver) stats.startNewRun();
        updateStakesBar();
        if (runOver) showRunOverDialog(lostStreak);
        else         showLossDialog(lostStreak);
    }

    // ── Confirmation dialog (#17) ─────────────────────────────────────────

    /**
     * Asks the player to confirm abandoning an active game.
     * Returns true if they confirmed (game should be abandoned).
     */
    private boolean confirmAbandon() {
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Abandon this game? It counts as a loss — you'll lose a life and your win streak.",
            "Abandon Game",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        return choice == JOptionPane.YES_OPTION;
    }

    /** Records an abandoned game as a loss, and resets the run if it was the last life. */
    private void recordAbandon() {
        stats.recordLoss();
        if (stats.isOutOfLives()) stats.startNewRun();
    }

    // ── Countdown & timers ────────────────────────────────────────────────

    /** Fires every second from clockTimer to update the timer label. */
    private void tickClock() {
        lblTimer.setText(session.getElapsedFormatted());
    }

    /** Fires every 100 ms from countdownTimer to animate the per-guess bar. */
    private void tickCountdown() {
        if (!session.isActive()) { countdownTimer.stop(); return; }
        if (countdownPaused) return;

        if (freezeTicksLeft > 0) {
            freezeTicksLeft--;
            countdownBar.repaint();
            return;
        }

        if (countdownTicks > 0) {
            countdownTicks--;
            countdownBar.repaint();
        }

        if (countdownTicks == 0) {
            handleTimeout();
        }
    }

    /** Called when the per-guess countdown reaches zero. */
    private void handleTimeout() {
        resetCountdown();
        GuessResult result = session.timeoutGuess();
        lastResult = GuessResult.TIMEOUT;

        tfGuess.setText("");
        tfGuess.requestFocus();

        refreshDynamicState();
        updatePowerUpButtons();

        if (result == GuessResult.GAME_OVER || !session.isActive()) {
            stopAllTimers();
            handleLoss();
        } else {
            showTempWarning("Time's up! That guess was wasted.", UIConstants.DANGER);
        }
    }

    /** Resets the per-guess countdown to full (called after every submission). */
    private void resetCountdown() {
        countdownTicks  = COUNTDOWN_TICKS;
        freezeTicksLeft = 0;
        countdownPaused = false;
        countdownBar.repaint();
    }

    private void stopAllTimers() {
        clockTimer.stop();
        countdownTimer.stop();
    }

    // ── Power-up activation ───────────────────────────────────────────────

    private void activatePowerUp(PowerUp pu) {
        if (!stats.hasPowerUp(pu) || !session.isActive()) return;

        switch (pu) {
            case CUT: {
                if (session.getRangeHigh() - session.getRangeLow() < 2) {
                    showTempWarning("Range is already too narrow to cut!", pu.getColor());
                    return;
                }
                String msg = session.applyCut();
                stats.usePowerUp(PowerUp.CUT);
                refreshDynamicState();
                updatePowerUpButtons();
                showTempWarning("Cut!  " + msg, pu.getColor());
                break;
            }
            case SHIELD: {
                if (session.isShieldActive()) {
                    showTempWarning("Shield is already active!", pu.getColor());
                    return;
                }
                session.activateShield();
                stats.usePowerUp(PowerUp.SHIELD);
                updatePowerUpButtons();
                showTempWarning("Shield active — next wrong guess won't cost a turn!", pu.getColor());
                break;
            }
            case PEEK: {
                countdownPaused = true;
                stats.usePowerUp(PowerUp.PEEK);
                String oddEven = session.isTargetOdd() ? "ODD" : "EVEN";
                JOptionPane.showMessageDialog(
                    this,
                    "The secret number is  " + oddEven + "!",
                    "Peek",
                    JOptionPane.INFORMATION_MESSAGE
                );
                countdownPaused = false;
                updatePowerUpButtons();
                break;
            }
            case FREEZE: {
                freezeTicksLeft += FREEZE_TICKS;
                stats.usePowerUp(PowerUp.FREEZE);
                updatePowerUpButtons();
                showTempWarning("Timer frozen for 15 seconds!", pu.getColor());
                break;
            }
        }
    }

    // ── State refresh ─────────────────────────────────────────────────────

    /**
     * Rebuilds hint, range label, progress bar, counter, warnings, and history
     * according to the current session state and the level's information tier.
     */
    private void refreshDynamicState() {
        Level   lv       = session.getLevel();
        int     used     = session.getGuessCount();
        int     max      = lv.getMaxGuesses();
        int     left     = session.getGuessesLeft();
        boolean isExpert = (lv == Level.EXPERT);

        // ── Hint label ────────────────────────────────────────────────
        if (used == 0) {
            lblHint.setText(session.getInitialMessage());
        } else if (lastResult == GuessResult.TIMEOUT) {
            lblHint.setText("Time ran out — guess wasted!");
        } else if (lastResult != null) {
            lblHint.setText(session.getHintMessage(lastGuess, lastResult));
        }

        // ── Range label (shown only if level supports it) ─────────────
        if (lv.showsRangeDisplay() && used > 0 && lastResult != GuessResult.CORRECT) {
            lblRange.setText("Number is between  " + session.getRangeLow()
                + "  and  " + session.getRangeHigh());
        } else if (used == 0 && lv.showsRangeDisplay()) {
            lblRange.setText("");
        }

        // ── Number line repaint ───────────────────────────────────────
        if (lv.showsNumberLine()) numberLinePanel.repaint();

        // ── Progress bar ─────────────────────────────────────────────
        progressBar.setValue(used);
        if (left <= 1)      progressBar.setForeground(UIConstants.DANGER);
        else if (left <= 3) progressBar.setForeground(UIConstants.WARNING);
        else                progressBar.setForeground(UIConstants.SUCCESS);

        // ── Guess counter: Expert hides it until 2 left ───────────────
        if (isExpert && left > 2 && session.isActive()) {
            lblGuessCount.setText("");
            progressBar.setVisible(false);
        } else {
            progressBar.setVisible(true);
            lblGuessCount.setText(used + " / " + max + " guesses used");
        }

        // ── Warning banner ────────────────────────────────────────────
        if (!session.isActive()) {
            warningPanel.setVisible(false);
        } else if (left == 1) {
            showWarning("Last chance — make it count!", UIConstants.DANGER);
        } else if (left == 2 && isExpert) {
            showWarning("2 guesses left!", UIConstants.DANGER);
        } else if (left <= 3) {
            showWarning("Only " + left + " guesses remaining!", UIConstants.WARNING);
        } else {
            warningPanel.setVisible(false);
        }

        // ── History: append-only — never rebuild from scratch (#16) ──
        List<String> hist = session.getHistory();
        if (hist.size() > historySize) {
            if (historySize == 0) {
                taHistory.setText(""); // clear the "guesses will appear here" placeholder
            }
            while (historySize < hist.size()) {
                taHistory.append(hist.get(historySize) + "\n");
                historySize++;
            }
            taHistory.setCaretPosition(taHistory.getDocument().getLength());
        }
    }

    private void showWarning(String message, Color bg) {
        lblWarning.setText(message);
        lblWarning.setForeground(Color.WHITE);
        warningPanel.setBackground(bg);
        warningPanel.setVisible(true);
    }

    /**
     * Shows a temporary coloured message in the warning panel and restores
     * normal state after 2 seconds. Cancels any previously running timer
     * before starting a new one (#3 — prevents stacking).
     */
    private void showTempWarning(String message, Color bg) {
        if (tempWarningTimer != null && tempWarningTimer.isRunning()) {
            tempWarningTimer.stop();
        }
        showWarning(message, bg);
        tempWarningTimer = new Timer(2000, e -> refreshDynamicState());
        tempWarningTimer.setRepeats(false);
        tempWarningTimer.start();
    }

    // ── Result Dialogs ────────────────────────────────────────────────────

    private void showWinDialog(PowerUp awarded, int baseScore, double mult,
                               int finalScore, boolean gainedLife) {
        countdownPaused = true;

        int h = 360;
        if (mult > 1.0)        h += 24;   // multiplier breakdown line
        h += 22;                          // win-streak line (always shown)
        if (gainedLife)        h += 24;   // life-earned line
        if (awarded != null)   h += 95;   // power-up section

        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
            "You Won!", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(440, h);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Color.WHITE);

        // Header
        JPanel dlgHeader = new JPanel(new BorderLayout());
        dlgHeader.setBackground(UIConstants.SUCCESS);
        dlgHeader.setBorder(new EmptyBorder(14, 20, 14, 20));
        JLabel dlgTitle = new JLabel("  You got it!", SwingConstants.CENTER);
        dlgTitle.setFont(UIConstants.FONT_H2);
        dlgTitle.setForeground(Color.WHITE);
        applyIconLabel(dlgTitle, "trophy.svg");
        dlgHeader.add(dlgTitle, BorderLayout.CENTER);
        content.add(dlgHeader, BorderLayout.NORTH);

        // Body
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(18, 30, 10, 30));

        JLabel awardLbl = new JLabel(session.getAward(), SwingConstants.CENTER);
        awardLbl.setFont(UIConstants.FONT_AWARD);
        awardLbl.setForeground(UIConstants.SECONDARY);
        awardLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel starsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        starsPanel.setBackground(Color.WHITE);
        starsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        javax.swing.ImageIcon starIcon = UIConstants.loadIconTinted("star.svg", UIConstants.SECONDARY);
        for (int i = 0; i < getAwardStarCount(); i++) {
            starsPanel.add(new JLabel(starIcon));
        }

        JLabel msgLbl = new JLabel(
            "The number was " + session.getTargetNumber() +
            ".  Found in " + session.getGuessCount() +
            " guess" + (session.getGuessCount() == 1 ? "" : "es") + ".",
            SwingConstants.CENTER
        );
        msgLbl.setFont(UIConstants.FONT_BODY);
        msgLbl.setForeground(new Color(50, 50, 50));
        msgLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel scoreLbl = new JLabel(
            "Score:  " + String.format("%,d", finalScore) +
            "   •   Time:  " + session.getElapsedFormatted(),
            SwingConstants.CENTER
        );
        scoreLbl.setFont(UIConstants.FONT_BOLD);
        scoreLbl.setForeground(UIConstants.PRIMARY);
        scoreLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        body.add(awardLbl);
        body.add(Box.createVerticalStrut(6));
        body.add(starsPanel);
        body.add(Box.createVerticalStrut(12));
        body.add(msgLbl);
        body.add(Box.createVerticalStrut(6));
        body.add(scoreLbl);

        // Multiplier breakdown — only shown when a streak was actually boosting score
        if (mult > 1.0) {
            JLabel breakdown = new JLabel(
                String.format("%,d  ×  x%.1f streak  =  %,d", baseScore, mult, finalScore),
                SwingConstants.CENTER);
            breakdown.setFont(UIConstants.FONT_SMALL);
            breakdown.setForeground(UIConstants.SECONDARY);
            breakdown.setAlignmentX(Component.CENTER_ALIGNMENT);
            body.add(Box.createVerticalStrut(4));
            body.add(breakdown);
        }

        // Win-streak line — the stake the player is now carrying
        JLabel streakLbl = new JLabel(
            "Win streak:  " + stats.getWinStreak() +
            "   (next win:  x" + String.format("%.1f", stats.getStreakMultiplier()) + ")",
            SwingConstants.CENTER);
        streakLbl.setFont(UIConstants.FONT_BOLD);
        streakLbl.setForeground(new Color(120, 70, 160));
        streakLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(Box.createVerticalStrut(6));
        body.add(streakLbl);

        // Life-earned line — milestone reward for sustaining a streak
        if (gainedLife) {
            JLabel lifeLbl = new JLabel(
                "+1 Life earned!   (" + stats.getLives() + " / " + stats.getMaxLives() + ")",
                SwingConstants.CENTER);
            lifeLbl.setFont(UIConstants.FONT_BOLD);
            lifeLbl.setForeground(new Color(229, 57, 53));
            lifeLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            body.add(Box.createVerticalStrut(4));
            body.add(lifeLbl);
        }

        if (awarded != null) {
            body.add(Box.createVerticalStrut(12));
            JSeparator sep = new JSeparator();
            sep.setAlignmentX(Component.CENTER_ALIGNMENT);
            sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            body.add(sep);
            body.add(Box.createVerticalStrut(10));

            JLabel puLbl = new JLabel(
                "Power-up earned:  " + awarded.getDisplayName() + "!",
                SwingConstants.CENTER
            );
            puLbl.setFont(UIConstants.FONT_BOLD);
            puLbl.setForeground(awarded.getColor());
            puLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel puDescLbl = new JLabel(awarded.getDescription(), SwingConstants.CENTER);
            puDescLbl.setFont(UIConstants.FONT_SMALL);
            puDescLbl.setForeground(Color.GRAY);
            puDescLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            body.add(puLbl);
            body.add(Box.createVerticalStrut(3));
            body.add(puDescLbl);
        }

        content.add(body, BorderLayout.CENTER);

        JButton btnAgain = actionButton("  Play Again  ", UIConstants.SUCCESS,      Color.WHITE);
        JButton btnMenu  = actionButton("  Main Menu  ",  UIConstants.PRIMARY_DARK, Color.WHITE);
        applyIcon(btnAgain, "refresh.svg");
        applyIcon(btnMenu,  "home.svg");
        btnAgain.addActionListener(e -> { dlg.dispose(); frame.startGame(session.getLevel()); });
        btnMenu .addActionListener(e -> { dlg.dispose(); frame.showWelcome();                 });

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        btns.setBackground(Color.WHITE);
        btns.add(btnMenu);
        btns.add(btnAgain);
        content.add(btns, BorderLayout.SOUTH);

        dlg.setContentPane(content);
        dlg.setVisible(true);
        countdownPaused = false;
    }

    private void showLossDialog(int lostStreak) {
        countdownPaused = true;

        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
            "Game Over", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(420, 360);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Color.WHITE);

        JPanel dlgHeader = new JPanel(new BorderLayout());
        dlgHeader.setBackground(UIConstants.DANGER);
        dlgHeader.setBorder(new EmptyBorder(14, 20, 14, 20));
        JLabel dlgTitle = new JLabel("Game Over!", SwingConstants.CENTER);
        dlgTitle.setFont(UIConstants.FONT_H2);
        dlgTitle.setForeground(Color.WHITE);
        dlgHeader.add(dlgTitle, BorderLayout.CENTER);
        content.add(dlgHeader, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(24, 30, 10, 30));

        JLabel revealLbl = new JLabel(
            "The secret number was:  " + session.getTargetNumber(),
            SwingConstants.CENTER
        );
        revealLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        revealLbl.setForeground(UIConstants.DANGER);
        revealLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel timeLbl = new JLabel(
            "Time spent:  " + session.getElapsedFormatted(),
            SwingConstants.CENTER
        );
        timeLbl.setFont(UIConstants.FONT_BODY);
        timeLbl.setForeground(new Color(100, 60, 140));
        timeLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel consoleLbl = new JLabel("Better luck next time!", SwingConstants.CENTER);
        consoleLbl.setFont(UIConstants.FONT_BODY);
        consoleLbl.setForeground(new Color(80, 80, 80));
        consoleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Lives remaining — the stake the player just paid
        JLabel livesLbl = new JLabel(
            "Lives remaining:  " + stats.getLives() + " / " + stats.getMaxLives(),
            SwingConstants.CENTER);
        livesLbl.setFont(UIConstants.FONT_BOLD);
        livesLbl.setForeground(new Color(229, 57, 53));
        livesLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        body.add(revealLbl);
        body.add(Box.createVerticalStrut(8));
        body.add(timeLbl);
        body.add(Box.createVerticalStrut(6));

        // Show the streak that was broken (only if there was one worth noting)
        if (lostStreak > 1) {
            JLabel streakLostLbl = new JLabel(
                "Your " + lostStreak + "-win streak has ended.", SwingConstants.CENTER);
            streakLostLbl.setFont(UIConstants.FONT_BODY);
            streakLostLbl.setForeground(new Color(120, 70, 160));
            streakLostLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            body.add(streakLostLbl);
            body.add(Box.createVerticalStrut(6));
        }

        body.add(livesLbl);
        body.add(Box.createVerticalStrut(6));
        body.add(consoleLbl);
        content.add(body, BorderLayout.CENTER);

        JButton btnAgain = actionButton("  Try Again  ", UIConstants.DANGER,       Color.WHITE);
        JButton btnMenu  = actionButton("  Main Menu  ", UIConstants.PRIMARY_DARK,  Color.WHITE);
        applyIcon(btnAgain, "refresh.svg");
        applyIcon(btnMenu,  "home.svg");
        btnAgain.addActionListener(e -> { dlg.dispose(); frame.startGame(session.getLevel()); });
        btnMenu .addActionListener(e -> { dlg.dispose(); frame.showWelcome();                 });

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        btns.setBackground(Color.WHITE);
        btns.add(btnMenu);
        btns.add(btnAgain);
        content.add(btns, BorderLayout.SOUTH);

        dlg.setContentPane(content);
        dlg.setVisible(true);
        countdownPaused = false;
    }

    /**
     * Shown when the player loses their LAST life. This is the run-over moment —
     * the real stake of the lives system. The run has already been refilled to
     * START_LIVES by the time this shows, so it frames a fresh start.
     */
    private void showRunOverDialog(int lostStreak) {
        countdownPaused = true;

        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
            "Run Over", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(440, 380);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Color.WHITE);

        JPanel dlgHeader = new JPanel(new BorderLayout());
        dlgHeader.setBackground(UIConstants.DANGER);
        dlgHeader.setBorder(new EmptyBorder(14, 20, 14, 20));
        JLabel dlgTitle = new JLabel("Out of Lives — Run Over", SwingConstants.CENTER);
        dlgTitle.setFont(UIConstants.FONT_H2);
        dlgTitle.setForeground(Color.WHITE);
        dlgHeader.add(dlgTitle, BorderLayout.CENTER);
        content.add(dlgHeader, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(22, 30, 12, 30));

        JLabel revealLbl = new JLabel(
            "The secret number was:  " + session.getTargetNumber(), SwingConstants.CENTER);
        revealLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        revealLbl.setForeground(UIConstants.DANGER);
        revealLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel outLbl = new JLabel(
            "You've used your last life. This run is over.", SwingConstants.CENTER);
        outLbl.setFont(UIConstants.FONT_BODY);
        outLbl.setForeground(new Color(80, 80, 80));
        outLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel bestLbl = new JLabel(
            "Longest streak reached:  " + stats.getBestStreak() + " wins", SwingConstants.CENTER);
        bestLbl.setFont(UIConstants.FONT_BOLD);
        bestLbl.setForeground(UIConstants.SECONDARY);
        bestLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel freshLbl = new JLabel(
            "A new run begins with " + stats.getLives() + " fresh lives.", SwingConstants.CENTER);
        freshLbl.setFont(UIConstants.FONT_BOLD);
        freshLbl.setForeground(new Color(0, 105, 92));
        freshLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        body.add(revealLbl);
        body.add(Box.createVerticalStrut(10));
        body.add(outLbl);
        if (lostStreak > 1) {
            JLabel endedLbl = new JLabel(
                "Your " + lostStreak + "-win streak has ended.", SwingConstants.CENTER);
            endedLbl.setFont(UIConstants.FONT_BODY);
            endedLbl.setForeground(new Color(120, 70, 160));
            endedLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            body.add(Box.createVerticalStrut(6));
            body.add(endedLbl);
        }
        body.add(Box.createVerticalStrut(10));
        body.add(bestLbl);
        body.add(Box.createVerticalStrut(6));
        body.add(freshLbl);
        content.add(body, BorderLayout.CENTER);

        JButton btnAgain = actionButton("  New Run  ",  new Color(0, 105, 92),   Color.WHITE);
        JButton btnMenu  = actionButton("  Main Menu  ", UIConstants.PRIMARY_DARK, Color.WHITE);
        applyIcon(btnAgain, "refresh.svg");
        applyIcon(btnMenu,  "home.svg");
        btnAgain.addActionListener(e -> { dlg.dispose(); frame.startGame(session.getLevel()); });
        btnMenu .addActionListener(e -> { dlg.dispose(); frame.showWelcome();                 });

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        btns.setBackground(Color.WHITE);
        btns.add(btnMenu);
        btns.add(btnAgain);
        content.add(btns, BorderLayout.SOUTH);

        dlg.setContentPane(content);
        dlg.setVisible(true);
        countdownPaused = false;
    }

    // ── Countdown Bar ─────────────────────────────────────────────────────

    /**
     * A thin bar that shows how much time remains for the current guess.
     * Green (> 6 s) → Orange (3–6 s) → Red (< 3 s).
     * Turns solid teal while a FREEZE is active.
     */
    private class CountdownBar extends JPanel {

        CountdownBar() {
            setPreferredSize(new Dimension(0, 22));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight(), arc = h;

            // Background track
            g2.setColor(new Color(215, 208, 228));
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            Color  barColor;
            double ratio;
            String label;

            if (freezeTicksLeft > 0) {
                ratio    = 1.0;
                barColor = new Color(0, 150, 160);
                int secs = (int) Math.ceil(freezeTicksLeft / 10.0);
                label    = "Frozen  " + secs + "s";
            } else {
                ratio = (double) countdownTicks / COUNTDOWN_TICKS;
                int secs = (int) Math.ceil(countdownTicks / 10.0);
                if      (countdownTicks > 60) barColor = new Color(67,  160, 71);
                else if (countdownTicks > 30) barColor = new Color(251, 140, 0);
                else                          barColor = UIConstants.DANGER;
                label = secs + "s";
            }

            int barW = (int)(w * ratio);
            if (barW > 0) {
                g2.setColor(barColor);
                g2.fillRoundRect(0, 0, barW, h, arc, arc);
            }

            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2.setColor(Color.WHITE);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, (w - fm.stringWidth(label)) / 2,
                (h + fm.getAscent() - fm.getDescent()) / 2);

            g2.dispose();
        }
    }

    // ── Lives Bar ─────────────────────────────────────────────────────────

    /**
     * Paints a row of MAX_LIVES hearts: filled red for remaining lives,
     * faded for lost ones. Uses the tinted heart.svg so there are no Unicode
     * glyphs (which render as empty boxes on some Windows fonts).
     */
    private class LivesBar extends JPanel {
        private static final int ICON = 18, GAP = 3;
        private final javax.swing.ImageIcon full =
            UIConstants.loadIconTinted("heart.svg", new Color(229, 57, 53));
        private final javax.swing.ImageIcon empty =
            UIConstants.loadIconTinted("heart.svg", new Color(96, 70, 130));

        LivesBar() {
            int max = stats.getMaxLives();
            setOpaque(false);
            setPreferredSize(new Dimension(max * (ICON + GAP), ICON + 2));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int max   = stats.getMaxLives();
            int lives = stats.getLives();
            for (int i = 0; i < max; i++) {
                javax.swing.ImageIcon ic = (i < lives) ? full : empty;
                int x = i * (ICON + GAP);
                if (ic != null) ic.paintIcon(this, g, x, 1);
            }
        }
    }

    // ── Number Line Panel ─────────────────────────────────────────────────

    private class NumberLinePanel extends JPanel {

        NumberLinePanel() {
            setPreferredSize(new Dimension(0, 48));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR),
                new EmptyBorder(4, 6, 4, 6)
            ));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int padL = 26, padR = 26, lineW = w - padL - padR;
            int lineY = h / 2 - 4;
            int max   = session.getLevel().getNumberRange();
            Color lc  = session.getLevel().getColor();

            g2.setColor(new Color(215, 208, 228));
            g2.fillRoundRect(padL, lineY - 3, lineW, 6, 6, 6);

            int xLo = padL + (int)((double)(session.getRangeLow() - 1) / max * lineW);
            int xHi = padL + (int)((double) session.getRangeHigh()     / max * lineW);
            g2.setColor(new Color(lc.getRed(), lc.getGreen(), lc.getBlue(), 80));
            g2.fillRoundRect(xLo, lineY - 3, Math.max(4, xHi - xLo), 6, 6, 6);

            List<Integer> vals = session.getGuessValues();
            for (int i = 0; i < vals.size(); i++) {
                int gv = vals.get(i);
                if (gv < 1) continue;
                int gx   = padL + (int)((double)(gv - 1) / max * lineW);
                boolean low  = gv < session.getTargetNumber();
                boolean last = (i == vals.size() - 1);
                int r = last ? 6 : 4;
                g2.setColor(low ? new Color(220, 110, 0) : UIConstants.DANGER);
                g2.fillOval(gx - r, lineY - r, r * 2, r * 2);
                if (last) {
                    g2.setColor(Color.WHITE);
                    g2.fillOval(gx - 2, lineY - 2, 4, 4);
                }
            }

            g2.setFont(UIConstants.FONT_SMALL);
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(new Color(110, 70, 150));
            g2.drawString("1", padL - fm.stringWidth("1") - 3, lineY + 4);
            String maxStr = String.valueOf(max);
            g2.drawString(maxStr, w - padR + 3, lineY + 4);

            if (session.getGuessCount() > 0 && session.isActive()) {
                String rangeText = session.getRangeLow() + " — " + session.getRangeHigh();
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                fm = g2.getFontMetrics();
                g2.setColor(new Color(100, 60, 140));
                g2.drawString(rangeText, (w - fm.stringWidth(rangeText)) / 2, lineY + 18);
            }

            g2.dispose();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private int getAwardStarCount() {
        String award = session.getAward();
        if (award == null)                return 0;
        if (award.equals("PERFECT!!"))    return 5;
        if (award.equals("EXCELLENT!"))   return 4;
        if (award.equals("VERY GOOD!"))   return 3;
        if (award.equals("GOOD!"))        return 2;
        return 1;
    }

    /**
     * Opens a quick-reference dialog listing every power-up with its description
     * and "best used when" tip. The countdown is paused while the dialog is open.
     */
    private void showPowerUpReference() {
        countdownPaused = true;

        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
            "Power-Up Reference", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(400, 420);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Color.WHITE);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(130, 80, 180));
        header.setBorder(new EmptyBorder(12, 18, 12, 18));
        JLabel titleLbl = new JLabel("Power-Up Reference", SwingConstants.CENTER);
        titleLbl.setFont(UIConstants.FONT_H2);
        titleLbl.setForeground(Color.WHITE);
        header.add(titleLbl, BorderLayout.CENTER);
        content.add(header, BorderLayout.NORTH);

        // One row per power-up
        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setBackground(Color.WHITE);
        rows.setBorder(new EmptyBorder(12, 16, 12, 16));

        for (PowerUp pu : PowerUp.values()) {
            JPanel row = new JPanel(new BorderLayout(0, 2));
            row.setBackground(Color.WHITE);
            row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, pu.getColor()),
                new EmptyBorder(6, 10, 6, 6)
            ));

            JLabel nameLbl = new JLabel(pu.getDisplayName());
            nameLbl.setFont(UIConstants.FONT_BOLD);
            nameLbl.setForeground(pu.getColor());

            JLabel descLbl = new JLabel(
                "<html><body style='width:310px'>" + pu.getDescription() + "</body></html>");
            descLbl.setFont(UIConstants.FONT_SMALL);
            descLbl.setForeground(new Color(50, 50, 50));

            JLabel tipLbl = new JLabel(
                "<html><body style='width:310px'><i>" + pu.getTip() + "</i></body></html>");
            tipLbl.setFont(UIConstants.FONT_SMALL);
            tipLbl.setForeground(new Color(110, 80, 140));

            JPanel text = new JPanel();
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            text.setOpaque(false);
            text.add(nameLbl);
            text.add(Box.createVerticalStrut(2));
            text.add(descLbl);
            text.add(Box.createVerticalStrut(2));
            text.add(tipLbl);

            row.add(text, BorderLayout.CENTER);
            rows.add(row);
            rows.add(Box.createVerticalStrut(8));
        }

        JScrollPane scroll = new JScrollPane(rows);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        content.add(scroll, BorderLayout.CENTER);

        JButton btnClose = new JButton("  Close  ");
        btnClose.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        btnClose.setFont(UIConstants.FONT_BUTTON);
        btnClose.setBackground(new Color(130, 80, 180));
        btnClose.setForeground(Color.WHITE);
        btnClose.setFocusPainted(false);
        btnClose.setBorderPainted(false);
        btnClose.setOpaque(true);
        btnClose.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> dlg.dispose());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        footer.setBackground(Color.WHITE);
        footer.add(btnClose);
        content.add(footer, BorderLayout.SOUTH);

        dlg.setContentPane(content);
        dlg.setVisible(true);   // blocks until closed
        countdownPaused = false;
    }

    private void shakeField() {
        tfGuess.setBackground(new Color(255, 220, 220));
        Timer t = new Timer(300, e -> tfGuess.setBackground(UIConstants.INPUT_BG));
        t.setRepeats(false);
        t.start();
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Invalid Input", JOptionPane.WARNING_MESSAGE);
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

    private void applyIconLabel(JLabel lbl, String iconFile) {
        javax.swing.ImageIcon ic = UIConstants.loadIconWhite(iconFile);
        if (ic != null) { lbl.setIcon(ic); lbl.setIconTextGap(10); }
    }
}
