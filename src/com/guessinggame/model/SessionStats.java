package com.guessinggame.model;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Tracks win/loss statistics, high scores, and the power-up inventory
 * across sessions.
 *
 * Persistence: saves/loads from game_stats.properties in the working directory.
 * Uses Java's built-in Properties — no external library needed.
 *
 * Power-up inventory rules:
 *   - One random power-up is awarded after every win.
 *   - Maximum {@link PowerUp#MAX_HELD} of each type may be held at once.
 *   - If all slots are full the award is skipped (returns null).
 */
public class SessionStats {

    // Stored in the user's home directory so the path is reliable regardless
    // of which folder the game is launched from.
    private static final String STATS_FILE =
        System.getProperty("user.home") + java.io.File.separator + "game_stats.properties";

    // ── Stakes: lives + win-streak multiplier ─────────────────────────────
    /** Lives a fresh run starts with. */
    public static final int    START_LIVES         = 5;
    /** Maximum lives that can be banked. */
    public static final int    MAX_LIVES           = 5;
    /** Every Nth consecutive win grants +1 life (up to MAX_LIVES). */
    public static final int    STREAK_LIFE_EVERY   = 3;
    /** Score multiplier grows by this much per win in the current streak. */
    private static final double MULT_STEP          = 0.5;
    /** Multiplier is capped here so it can't run away. */
    private static final double MULT_CAP           = 5.0;

    private int gamesPlayed = 0;
    private int gamesWon    = 0;
    private int bestGuesses = Integer.MAX_VALUE;
    private int highScore   = 0;

    private int lives       = START_LIVES;  // current run's remaining lives
    private int winStreak   = 0;            // consecutive wins (resets on any loss)
    private int bestStreak  = 0;            // longest streak ever reached (persistent trophy)

    /** Best score achieved per level (absent key = no wins yet on that level). */
    private final Map<Level, Integer> bestScoreByLevel = new EnumMap<>(Level.class);

    /** How many of each power-up the player currently holds. */
    private final Map<PowerUp, Integer> powerUpInventory = new EnumMap<>(PowerUp.class);

    // ── Recording ─────────────────────────────────────────────────────────

    /**
     * Records a win, updates all statistics, awards a random power-up, and
     * persists to disk.
     *
     * @return The power-up that was awarded, or {@code null} if the inventory
     *         for every type is already at maximum capacity.
     */
    public PowerUp recordWin(int guessCount, int score, Level level) {
        gamesPlayed++;
        gamesWon++;
        winStreak++;
        if (winStreak > bestStreak) bestStreak = winStreak;
        // Every Nth win in a streak earns a life back (capped at MAX_LIVES)
        if (winStreak % STREAK_LIFE_EVERY == 0 && lives < MAX_LIVES) lives++;
        if (guessCount < bestGuesses) bestGuesses = guessCount;
        if (score > highScore)        highScore   = score;
        bestScoreByLevel.merge(level, score, Math::max);
        PowerUp awarded = awardInternal();  // pick power-up before saving
        saveToFile();
        return awarded;
    }

    public void recordLoss() {
        gamesPlayed++;
        winStreak = 0;            // a loss breaks the streak
        if (lives > 0) lives--;   // and costs a life
        saveToFile();
    }

    /**
     * Wipes all statistics, streak, lives, and power-up inventory back to the
     * starting state and immediately persists it to disk.
     */
    public void reset() {
        gamesPlayed  = 0;
        gamesWon     = 0;
        bestGuesses  = Integer.MAX_VALUE;
        highScore    = 0;
        lives        = START_LIVES;
        winStreak    = 0;
        bestStreak   = 0;
        bestScoreByLevel.clear();
        powerUpInventory.clear();
        saveToFile();
    }

    // ── Lives + streak ────────────────────────────────────────────────────

    /** Current remaining lives in this run. */
    public int getLives()      { return lives;      }
    public int getMaxLives()   { return MAX_LIVES;  }
    /** Consecutive wins so far (the active streak). */
    public int getWinStreak()  { return winStreak;  }
    /** Longest streak ever reached. */
    public int getBestStreak() { return bestStreak; }

    /** True once lives hit zero — the current run is over. */
    public boolean isOutOfLives() { return lives <= 0; }

    /**
     * Score multiplier currently in effect, based on the active streak.
     * Streak 0 → x1.0, streak 1 → x1.5, streak 2 → x2.0 … capped at MULT_CAP.
     */
    public double getStreakMultiplier() {
        return Math.min(MULT_CAP, 1.0 + MULT_STEP * winStreak);
    }

    /**
     * Starts a fresh run after a game-over: lives back to START_LIVES and the
     * streak cleared. Best-streak and lifetime stats are preserved.
     */
    public void startNewRun() {
        lives     = START_LIVES;
        winStreak = 0;
        saveToFile();
    }

    // ── Power-up inventory ────────────────────────────────────────────────

    /**
     * Awards a random power-up from eligible types (those below the cap).
     * Saves to disk after updating the inventory.
     *
     * @return The awarded {@link PowerUp}, or {@code null} if all slots are full.
     */
    public PowerUp awardRandomPowerUp() {
        PowerUp awarded = awardInternal();
        if (awarded != null) saveToFile();
        return awarded;
    }

    /** Internal award without saving — caller is responsible for saveToFile(). */
    private PowerUp awardInternal() {
        List<PowerUp> eligible = new ArrayList<>();
        for (PowerUp pu : PowerUp.values()) {
            if (powerUpInventory.getOrDefault(pu, 0) < PowerUp.MAX_HELD) {
                eligible.add(pu);
            }
        }
        if (eligible.isEmpty()) return null;
        PowerUp awarded = eligible.get(new Random().nextInt(eligible.size()));
        powerUpInventory.merge(awarded, 1, Integer::sum);
        return awarded;
    }

    /** Returns true if the player holds at least one of the given power-up. */
    public boolean hasPowerUp(PowerUp pu) {
        return powerUpInventory.getOrDefault(pu, 0) > 0;
    }

    /**
     * Consumes one of the given power-up from the inventory and saves to disk.
     * No-op if the count is already zero.
     */
    public void usePowerUp(PowerUp pu) {
        int count = powerUpInventory.getOrDefault(pu, 0);
        if (count > 0) {
            powerUpInventory.put(pu, count - 1);
            saveToFile();
        }
    }

    /** Returns how many of the given power-up the player currently holds (0–2). */
    public int getPowerUpCount(PowerUp pu) {
        return powerUpInventory.getOrDefault(pu, 0);
    }

    /** Returns the total number of power-ups held across all types. */
    public int getTotalPowerUpCount() {
        int total = 0;
        for (PowerUp pu : PowerUp.values()) {
            total += powerUpInventory.getOrDefault(pu, 0);
        }
        return total;
    }

    /**
     * Awards exactly one of the specified power-up (up to the cap).
     * Used when the player makes a deliberate free pick from the selection dialog.
     */
    public void awardSpecificPowerUp(PowerUp pu) {
        int current = powerUpInventory.getOrDefault(pu, 0);
        if (current < PowerUp.MAX_HELD) {
            powerUpInventory.put(pu, current + 1);
            saveToFile();
        }
    }

    // ── Queries ───────────────────────────────────────────────────────────

    public int    getGamesPlayed() { return gamesPlayed;            }
    public int    getGamesWon()    { return gamesWon;               }
    public int    getGamesLost()   { return gamesPlayed - gamesWon; }
    public int    getHighScore()   { return highScore;              }

    /** Best guess count across all levels, or "—" if no wins yet. */
    public String getBestGuesses() {
        if (bestGuesses == Integer.MAX_VALUE) return "—";
        return bestGuesses + (bestGuesses == 1 ? " guess" : " guesses");
    }

    /** High score formatted with comma separators, or "—" if none yet. */
    public String getHighScoreDisplay() {
        return highScore > 0 ? String.format("%,d", highScore) : "—";
    }

    /** Best score for a specific level, or 0 if never won on that level. */
    public int getBestScore(Level level) {
        return bestScoreByLevel.getOrDefault(level, 0);
    }

    // ── Persistence ───────────────────────────────────────────────────────

    /**
     * Saves current stats and power-up inventory to game_stats.properties.
     * Silent on failure — stats loss on crash is acceptable.
     */
    public void saveToFile() {
        try {
            java.util.Properties props = new java.util.Properties();
            props.setProperty("gamesPlayed", String.valueOf(gamesPlayed));
            props.setProperty("gamesWon",    String.valueOf(gamesWon));
            props.setProperty("bestGuesses", String.valueOf(bestGuesses));
            props.setProperty("highScore",   String.valueOf(highScore));
            props.setProperty("lives",       String.valueOf(lives));
            props.setProperty("winStreak",   String.valueOf(winStreak));
            props.setProperty("bestStreak",  String.valueOf(bestStreak));
            for (Level lv : Level.values()) {
                props.setProperty("best." + lv.name(),
                    String.valueOf(bestScoreByLevel.getOrDefault(lv, 0)));
            }
            for (PowerUp pu : PowerUp.values()) {
                props.setProperty("pu." + pu.name(),
                    String.valueOf(powerUpInventory.getOrDefault(pu, 0)));
            }
            try (FileWriter fw = new FileWriter(STATS_FILE)) {
                props.store(fw, "Number Guessing Game - Player Stats");
            }
        } catch (Exception ignored) {}
    }

    /**
     * Loads stats from game_stats.properties.
     * Returns a fresh SessionStats if the file does not exist yet.
     */
    public static SessionStats loadFromFile() {
        SessionStats s = new SessionStats();
        try {
            java.util.Properties props = new java.util.Properties();
            try (FileReader fr = new FileReader(STATS_FILE)) {
                props.load(fr);
            }
            s.gamesPlayed = Integer.parseInt(props.getProperty("gamesPlayed", "0"));
            s.gamesWon    = Integer.parseInt(props.getProperty("gamesWon",    "0"));
            s.bestGuesses = Integer.parseInt(props.getProperty("bestGuesses",
                String.valueOf(Integer.MAX_VALUE)));
            s.highScore   = Integer.parseInt(props.getProperty("highScore", "0"));
            s.lives       = Integer.parseInt(props.getProperty("lives",
                String.valueOf(START_LIVES)));
            s.winStreak   = Integer.parseInt(props.getProperty("winStreak",  "0"));
            s.bestStreak  = Integer.parseInt(props.getProperty("bestStreak", "0"));
            // Guard against a corrupt/edited file leaving an unplayable run
            if (s.lives < 0)          s.lives = 0;
            if (s.lives > MAX_LIVES)  s.lives = MAX_LIVES;
            for (Level lv : Level.values()) {
                int score = Integer.parseInt(props.getProperty("best." + lv.name(), "0"));
                if (score > 0) s.bestScoreByLevel.put(lv, score);
            }
            for (PowerUp pu : PowerUp.values()) {
                int count = Integer.parseInt(props.getProperty("pu." + pu.name(), "0"));
                if (count > 0) s.powerUpInventory.put(pu, Math.min(count, PowerUp.MAX_HELD));
            }
        } catch (Exception ignored) {}   // file missing on first run — use defaults
        return s;
    }
}
