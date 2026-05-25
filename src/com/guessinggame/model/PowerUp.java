package com.guessinggame.model;

/**
 * Power-ups earned by winning games.
 *
 * Each type provides a unique in-game advantage. The player can hold at most
 * {@link #MAX_HELD} of each type at once. Power-ups are awarded randomly after
 * every win and persist to disk via {@link SessionStats}.
 *
 * <pre>
 *   CUT    – Narrows the range by cutting the wrong half (always safe).
 *   SHIELD – Next wrong guess won't cost a turn (range still narrows).
 *   PEEK   – Reveals whether the secret number is odd or even.
 *   FREEZE – Pauses the per-guess countdown timer for 15 seconds.
 * </pre>
 *
 * Each value also carries a short "best used when" {@link #getTip()} so the UI
 * can teach players without duplicating text across multiple classes.
 */
public enum PowerUp {

    CUT(
        "Cut",
        "Eliminates the wrong half of the remaining range",
        "Best used when the range is still large — instantly halves your search space.",
        new java.awt.Color(230, 81,   0)
    ),

    SHIELD(
        "Shield",
        "Your next wrong guess won't cost a turn",
        "Best used when you have only a few guesses left and can't afford to waste one.",
        new java.awt.Color(21,  101, 192)
    ),

    PEEK(
        "Peek",
        "Reveals whether the secret number is odd or even",
        "Best used early — odd/even instantly eliminates half of all remaining numbers.",
        new java.awt.Color(123,  31, 162)
    ),

    FREEZE(
        "Freeze",
        "Pauses the countdown timer for 15 seconds",
        "Best used when the countdown bar is running low and you need time to think.",
        new java.awt.Color(0,   131, 143)
    );

    /** Maximum of each power-up the player may hold simultaneously. */
    public static final int MAX_HELD = 2;

    private final String         displayName;
    private final String         description;
    private final String         tip;          // "best used when…" guidance for new players
    private final java.awt.Color color;

    PowerUp(String displayName, String description, String tip, java.awt.Color color) {
        this.displayName = displayName;
        this.description = description;
        this.tip         = tip;
        this.color       = color;
    }

    public String         getDisplayName() { return displayName; }
    public String         getDescription() { return description; }
    /** Short "best used when…" guidance shown in the educational pick dialog and reference panel. */
    public String         getTip()         { return tip;         }
    public java.awt.Color getColor()       { return color;       }
}
