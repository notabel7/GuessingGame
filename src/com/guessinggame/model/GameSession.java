package com.guessinggame.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Holds the complete state of a single game round:
 * secret number, guess history, narrowing range, timer, and score.
 *
 * Hint messages are generated here — the level decides which
 * layer of information the player receives (proximity, range, or direction only).
 *
 * Power-up interactions are managed here:
 *   - SHIELD  : activateShield() / isShieldActive() — intercepts the next wrong guess
 *   - CUT     : applyCut() — halves the remaining range, always removing the wrong half
 *   - PEEK    : isTargetOdd() — reveals odd/even parity of the secret number
 *   - FREEZE  : handled entirely in GamePanel (pauses the countdown timer)
 */
public class GameSession {

    public enum GuessResult { TOO_LOW, TOO_HIGH, CORRECT, TIMEOUT, GAME_OVER }

    // ── Proximity hint thresholds (fraction of total range) ───────────────
    /** Within 2 % of range → "Extremely close" */
    private static final double HINT_EXTREMELY_CLOSE = 0.02;
    /** Within 8 % of range → "Very close" */
    private static final double HINT_VERY_CLOSE      = 0.08;
    /** Within 20 % of range → "Getting warm" */
    private static final double HINT_WARM            = 0.20;
    /** Within 40 % of range → "Somewhat close" */
    private static final double HINT_SOMEWHAT        = 0.40;

    private final int    targetNumber;
    private final Level  level;
    private       int    guessCount = 0;
    private       int    rangeLow;
    private       int    rangeHigh;

    private final List<String>  history     = new ArrayList<>();
    private final List<Integer> guessValues = new ArrayList<>();   // raw int per guess (timeouts excluded)

    private final long startTime = System.currentTimeMillis();     // for timer + score

    private boolean won         = false;
    private boolean active      = true;
    private boolean shieldActive = false;

    public GameSession(Level level) {
        this.level      = level;
        this.rangeLow   = 1;
        this.rangeHigh  = level.getNumberRange();
        this.targetNumber = new Random().nextInt(level.getNumberRange()) + 1;
    }

    // ── Core game methods ─────────────────────────────────────────────────

    /**
     * Processes a guess, updates internal state, and returns the result.
     *
     * If a SHIELD is active and the guess is wrong, the guess count is NOT
     * incremented (the shield absorbs the penalty). The range still narrows
     * and a hint is still given — the shield is then consumed.
     */
    public GuessResult makeGuess(int guess) {
        if (!active) return GuessResult.GAME_OVER;

        guessValues.add(guess);

        // ── Correct ──────────────────────────────────────────────────────
        if (guess == targetNumber) {
            guessCount++;
            history.add(String.format("  #%02d   %5d   →   CORRECT!", guessCount, guess));
            won    = true;
            active = false;
            return GuessResult.CORRECT;
        }

        // ── Wrong ─────────────────────────────────────────────────────────
        boolean tooLow = (guess < targetNumber);
        if (tooLow) rangeLow  = Math.max(rangeLow,  guess + 1);
        else        rangeHigh = Math.min(rangeHigh, guess - 1);

        GuessResult result = tooLow ? GuessResult.TOO_LOW : GuessResult.TOO_HIGH;

        if (shieldActive) {
            // Shield absorbs this wrong guess — no count increment
            shieldActive = false;
            String dir;
            if (!level.showsProximityHint() && !level.showsRangeDisplay()) {
                dir = tooLow ? "Higher" : "Lower";
            } else {
                dir = tooLow ? "Too Low  ↑" : "Too High ↓";
            }
            history.add(String.format("  [S]   %5d   →   %s  Ὦ1 SHIELDED", guess, dir));
            return result;
        }

        guessCount++;
        history.add(buildHistoryEntry(guess, tooLow ? "Too Low  ↑" : "Too High ↓"));

        if (guessCount >= level.getMaxGuesses()) {
            active = false;
        }

        return result;
    }

    /**
     * Called when the per-guess countdown expires.
     * Counts as a wasted guess (no range narrowing, no information given).
     * Returns TIMEOUT while guesses remain, or GAME_OVER when the limit is hit.
     */
    public GuessResult timeoutGuess() {
        if (!active) return GuessResult.GAME_OVER;
        guessCount++;
        // Timeouts are NOT added to guessValues (no meaningful number-line position).
        history.add(String.format("  #%02d   -----   →   ⏰ TIME OUT", guessCount));
        if (guessCount >= level.getMaxGuesses()) {
            active = false;
            return GuessResult.GAME_OVER;
        }
        return GuessResult.TIMEOUT;
    }

    /** Builds a single history line, stripping information for harder levels. */
    private String buildHistoryEntry(int guess, String outcome) {
        // Expert shows only the guess number and a terse direction word
        if (!level.showsProximityHint() && !level.showsRangeDisplay()) {
            String dir = outcome.contains("Low") ? "Higher" : "Lower";
            return String.format("  #%02d   %5d   →   %s", guessCount, guess, dir);
        }
        return String.format("  #%02d   %5d   →   %s", guessCount, guess, outcome);
    }

    // ── Power-up support ──────────────────────────────────────────────────

    /** Activates the SHIELD for the next wrong guess. */
    public void activateShield()  { shieldActive = true;  }

    /** Returns true if a SHIELD is currently protecting the next wrong guess. */
    public boolean isShieldActive() { return shieldActive; }

    /**
     * CUT: Halves the remaining [rangeLow, rangeHigh] interval, always
     * removing the half that does NOT contain the secret number.
     *
     * @return A human-readable description of what was eliminated,
     *         e.g. "Eliminated 501 – 1000 from the range"
     */
    public String applyCut() {
        int mid = (rangeLow + rangeHigh) / 2;
        if (targetNumber <= mid) {
            int old = rangeHigh;
            rangeHigh = mid;
            return "Eliminated " + (mid + 1) + " – " + old + " from the range";
        } else {
            int old = rangeLow;
            rangeLow = mid + 1;
            return "Eliminated " + old + " – " + mid + " from the range";
        }
    }

    /** PEEK: Returns true if the secret number is odd, false if even. */
    public boolean isTargetOdd() { return targetNumber % 2 != 0; }

    // ── Hint messages ─────────────────────────────────────────────────────

    /**
     * Returns the opening prompt shown before the first guess.
     * Harder levels reveal less about the target range.
     */
    public String getInitialMessage() {
        if (level.showsRangeDisplay()) {
            return "Guess a number between  1  and  " + level.getNumberRange();
        }
        return "Make your first guess!";
    }

    /**
     * Returns the hint to display after a guess, tuned to the level:
     *   Beginner / Medium  →  proximity language  +  direction
     *   Hard               →  terse direction with a full stop
     *   Expert             →  single cold direction word only
     */
    public String getHintMessage(int guess, GuessResult result) {
        if (result == GuessResult.CORRECT) return "You got it!";
        if (result == GuessResult.TIMEOUT) return "Time ran out — guess wasted!";

        boolean tooHigh = (result == GuessResult.TOO_HIGH);

        // Expert: coldest, shortest
        if (!level.showsProximityHint() && !level.showsRangeDisplay()) {
            return tooHigh ? "Lower." : "Higher.";
        }

        // Hard: terse direction, no warmth
        if (!level.showsProximityHint()) {
            return tooHigh ? "Too High." : "Too Low.";
        }

        // Beginner / Medium: proximity-aware language
        int diff = Math.abs(guess - targetNumber);
        double pct = (double) diff / level.getNumberRange();
        String dir = tooHigh ? "Too High" : "Too Low";

        if (pct <= HINT_EXTREMELY_CLOSE) return "Extremely close!  —  " + dir + "!";
        if (pct <= HINT_VERY_CLOSE)      return "Very close  —  " + dir;
        if (pct <= HINT_WARM)            return "Getting warm  —  " + dir;
        if (pct <= HINT_SOMEWHAT)        return "Somewhat close  —  " + dir;
        return dir + "  —  far away";
    }

    // ── Scoring ───────────────────────────────────────────────────────────

    /**
     * Calculates the final score for a won game.
     * Formula: guess efficiency (80%) + time bonus (20%), scaled by difficulty.
     *
     * Score = (800 * diffMult * guessesSavedRatio) + (timeBonus * diffMult)
     *   where timeBonus shrinks linearly to 0 at 120 seconds.
     */
    public int calculateScore() {
        if (!won) return 0;
        int    max      = level.getMaxGuesses();
        int    mult     = level.getDifficultyMultiplier();
        double ratio    = (max > 1) ? (double)(max - guessCount) / (max - 1) : 1.0;
        int    guessScore = (int)(800 * mult * ratio);
        int    elapsed    = getElapsedSeconds();
        int    timeBonus  = (int)(Math.max(0.0, 1.0 - elapsed / 120.0) * 100 * mult);
        return Math.max(mult * 50, guessScore + timeBonus);
    }

    /**
     * Award label based on guesses used relative to the level's max.
     * Thresholds are proportional so they feel equivalent across difficulty levels.
     */
    public String getAward() {
        if (!won) return null;
        int max = level.getMaxGuesses();
        if (guessCount == 1)              return "PERFECT!!";
        if (guessCount <= max * 0.25)     return "EXCELLENT!";
        if (guessCount <= max * 0.45)     return "VERY GOOD!";
        if (guessCount <= max * 0.65)     return "GOOD!";
        if (guessCount <= max * 0.85)     return "SATISFACTORY";
        return "LUCKY";
    }

    // ── Timer ─────────────────────────────────────────────────────────────

    public int getElapsedSeconds() {
        return (int)((System.currentTimeMillis() - startTime) / 1000L);
    }

    /** Formats elapsed seconds as M:SS for display. */
    public String getElapsedFormatted() {
        int s = getElapsedSeconds();
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    public int          getGuessCount()   { return guessCount;                         }
    public int          getGuessesLeft()  { return level.getMaxGuesses() - guessCount; }
    public int          getRangeLow()     { return rangeLow;                           }
    public int          getRangeHigh()    { return rangeHigh;                          }
    public int          getTargetNumber() { return targetNumber;                       }
    public Level        getLevel()        { return level;                              }
    public boolean      isWon()           { return won;                                }
    public boolean      isActive()        { return active;                             }
    public List<String> getHistory()      { return history;                            }
    public List<Integer>getGuessValues()  { return guessValues;                        }
}
