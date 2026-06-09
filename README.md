# Number Guessing Game: Java Swing

A polished number-guessing game built with Java Swing. Pick a difficulty, hunt
the secret number before your guesses (or the clock) run out, and manage real
**stakes** — a limited pool of lives and an escalating win-streak multiplier.

---

## Getting Started

Don't just download the ZIP — use Git so the team stays in sync. Open your
terminal or IDE and run:

```bash
git clone https://github.com/abelinthegit800/GuessingGame.git
```

All images, icons, and audio live in the `resources/` folder and load via
relative paths, so they resolve automatically once the project is opened
correctly. See **Building & Running** below to launch the game.

---

## Gameplay

### Difficulty levels — "progressive information stripping"

The secret number is always equally random. What changes between levels is *how
much help you get* locating it. Each level peels away a layer of assistance.

| Level    | Range   | Guesses | Help you get                                        | Score ×  |
|----------|---------|---------|-----------------------------------------------------|----------|
| Beginner | 1–50    | 15      | Warm/cold hints + range text + visual number line   | ×1       |
| Medium   | 1–100   | 10      | Warm/cold hints + range text                         | ×2       |
| Hard     | 1–500   | 9       | Range text only — no warm/cold language              | ×3       |
| Expert   | 1–1000  | 7       | Nothing but "Higher" / "Lower"; counter hidden       | ×5       |

There is also a **per-guess countdown timer** — let it hit zero and the guess is
wasted.

### Stakes: lives + win streak

This is the heart of the game's risk/reward loop.

- **Lives.** You start every run with **5 lives**. Every loss — guessing wrong
  too many times, letting the timer run out on your last guess, or abandoning a
  game — **costs one life**. Lose your last life and it's **Run Over**: the run
  resets back to 5 fresh lives.
- **Win streak.** Each win in a row builds a streak that **multiplies your
  score**: ×1.0 → ×1.5 → ×2.0 … up to a **×5.0** cap (+0.5 per consecutive win).
  A single loss **resets the streak to zero** — so the longer your streak, the
  more every guess is worth, and the more you have to lose.
- **They feed each other.** Every **3 wins** in a streak earns **+1 life** back
  (up to the cap of 5), so a hot streak buys you survivability.

The current lives (heart icons) and active streak/multiplier are always shown on
the in-game stakes bar, and on the main-menu stats bar.

### Power-ups

Winning a game also grants a random power-up (you can hold up to 2 of each). If
your inventory is empty when you start a game, you're offered one free pick.

| Power-up | Effect                                                        |
|----------|---------------------------------------------------------------|
| Cut      | Eliminates the wrong half of the remaining range (always safe)|
| Shield   | Your next wrong guess won't cost a turn                        |
| Peek     | Reveals whether the secret number is odd or even              |
| Freeze   | Pauses the per-guess countdown for 15 seconds                 |

### Scoring

Final score = guess efficiency (80%) + time bonus (20%), scaled by the level's
difficulty multiplier, then multiplied by your active **streak multiplier**.

Stats (games played/won, best guess count, high score, best score per level,
lives, current & best streak, and your power-up inventory) persist between
sessions in `game_stats.properties` in your home directory.

---

## Building & Running

**Requirements:** a JDK (Java 8+) and `lib\svgSalamander.jar` present in the
`lib` folder (used to render the SVG icons).

### Windows (scripts provided)

```bat
compile.bat   :: compiles to out\ and copies icon/audio assets
run.bat       :: launches the game
```

### Manual (any OS)

```bash
# compile
javac -cp "lib/svgSalamander.jar" -d out -sourcepath src src/Main.java
# copy assets so icons/audio resolve at runtime
#   resources/icons  -> out/icons
#   resources/audio  -> out/audio
# run
java -cp "out:lib/svgSalamander.jar" Main      # use ; instead of : on Windows
```

### In an IDE (VS Code / NetBeans)

Open the project folder, ensure `lib\svgSalamander.jar` is on the classpath, and
run the `Main` class.

---

## Project Structure

```
GuessingGame/
├── src/
│   ├── Main.java                         App entry point
│   └── com/guessinggame/
│       ├── model/
│       │   ├── GameSession.java          One round: secret number, guesses, hints, scoring
│       │   ├── Level.java                Difficulty enum + information-stripping rules
│       │   ├── PowerUp.java              Power-up types, effects, and tips
│       │   └── SessionStats.java         Lives, streak, scores, inventory + persistence
│       ├── gui/
│       │   ├── MainFrame.java            Window + screen switching
│       │   ├── WelcomePanel.java         Menu: level cards + stats/stakes bar
│       │   └── GamePanel.java            The play screen (input, timer, lives, dialogs)
│       └── util/
│           ├── UIConstants.java          Colours, fonts, HiDPI SVG icon loaders
│           ├── SoundPlayer.java          Sound effects
│           └── AudioPlayer.java          Audio playback helper
├── resources/
│   ├── icons/                            SVG icons (incl. heart.svg for lives)
│   └── audio/                            Sound effects (nice.wav, fail.wav, …)
├── lib/svgSalamander.jar                 SVG rendering library
├── compile.bat / run.bat                 Build & run scripts (Windows)
└── README.md
```

Player stats are saved to `game_stats.properties` in your user home directory
(not inside the project folder), so they survive rebuilds.
