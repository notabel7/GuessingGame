package com.guessinggame.model;

import java.awt.Color;

/**
 * Represents a difficulty level.
 *
 * Design principle — Progressive Information Stripping:
 *   Each level removes one layer of cognitive scaffolding. The number is always
 *   equally random; what changes is how much help the player gets locating it.
 *
 *   Beginner : proximity hints  +  range display  +  number line  (full help)  1–50
 *   Medium   : proximity hints  +  range display  (line removed)               1–100
 *   Hard     : range display only  (no proximity language, no number line)     1–500
 *   Expert   : nothing — direction word only, counter hidden until 2 left      1–1000
 *
 * Guess counts are calibrated so every level is mathematically winnable
 * with good strategy (binary search needs ⌈log₂(range)⌉ guesses):
 *   Beginner: 15 guesses, need 6  → very forgiving
 *   Medium  : 10 guesses, need 7  → moderate
 *   Hard    :  9 guesses, need 9  → requires near-perfect binary search
 *   Expert  :  7 guesses, need 10 → cannot be guaranteed; info stripping makes it brutal
 */
public enum Level {

    //               display     max   range  prox   range  line   mult  color                    icon            description
    BEGINNER("Beginner",   15,    50,  true,  true,  true,   1, new Color( 46, 125,  50), "seedling.svg",  "Perfect for first-timers"),
    MEDIUM  ("Medium",     10,   100,  true,  true,  false,  2, new Color( 21, 101, 192), "zap.svg",       "A balanced challenge"),
    HARD    ("Hard",        9,   500,  false, true,  false,  3, new Color(230,  81,   0), "mountain.svg",  "Think carefully — no warm hints"),
    EXPERT  ("Expert",      7,  1000,  false, false, false,  5, new Color(183,  28,  28), "flame.svg",     "Requires perfect strategy to win");

    private final String  displayName;
    private final int     maxGuesses;
    private final int     numberRange;          // secret number is in [1 .. numberRange]
    private final boolean showsProximityHint;   // warm/cold language after each guess
    private final boolean showsRangeDisplay;    // "Number is between X and Y" text
    private final boolean showsNumberLine;      // visual bar narrowing with each guess
    private final int     difficultyMultiplier; // score weight (1, 2, 3, 5)
    private final Color   color;
    private final String  iconName;
    private final String  description;

    Level(String displayName, int maxGuesses, int numberRange,
          boolean showsProximityHint, boolean showsRangeDisplay, boolean showsNumberLine,
          int difficultyMultiplier, Color color, String iconName, String description) {
        this.displayName          = displayName;
        this.maxGuesses           = maxGuesses;
        this.numberRange          = numberRange;
        this.showsProximityHint   = showsProximityHint;
        this.showsRangeDisplay    = showsRangeDisplay;
        this.showsNumberLine      = showsNumberLine;
        this.difficultyMultiplier = difficultyMultiplier;
        this.color                = color;
        this.iconName             = iconName;
        this.description          = description;
    }

    public String  getDisplayName()         { return displayName;          }
    public int     getMaxGuesses()          { return maxGuesses;           }
    public int     getNumberRange()         { return numberRange;          }
    public boolean showsProximityHint()     { return showsProximityHint;   }
    public boolean showsRangeDisplay()      { return showsRangeDisplay;    }
    public boolean showsNumberLine()        { return showsNumberLine;      }
    public int     getDifficultyMultiplier(){ return difficultyMultiplier; }
    public Color   getColor()               { return color;                }
    public String  getIconName()            { return iconName;             }
    public String  getDescription()         { return description;          }

    /** Label shown on the level card: "15 guesses  •  1 – 100" */
    public String getRangeLabel() {
        return maxGuesses + " guesses  •  1 – " + numberRange;
    }
}
