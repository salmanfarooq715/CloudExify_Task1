# CloudExify_Task1

# CloudExify Java Internship — Month 1, Project 1
## Number Guessing Game (JavaFX GUI Edition)

A modern, GUI-based Number Guessing Game built with **JavaFX**, developed as part of the CloudExify Summer Internship 2026 (Java Track — Month 1, Project 1).

This project introduces core Java OOP concepts (classes, objects, methods) along with `Random`, file I/O, and event-driven GUI programming using JavaFX.

---

## Features

-  **Multiple difficulty levels** — Easy (1–50), Medium (1–100), Hard (1–200), and a fully **Custom range** mode
-  **Hint system** — Too High / Too Low feedback plus "Getting warmer / colder" cues based on guess distance
-  **Live timer** — tracks how long each round takes
-  **Progress bar** — visually shows attempts used vs. the attempt limit (color-graded: green → gold → red)
-  **Smart attempt limit** — calculated automatically from the selected range (harder range = more allowed attempts)
-  **Leaderboard (Top 5)** — persisted to file, ranked by fewest attempts then fastest time
-  **Win streak tracking** — current streak and all-time best streak, saved across sessions
-  **Stats screen** — total games played, total wins, win rate, and average attempts
-  **Sound feedback** — system beep on game end
-  **Custom dark theme** — orange/gold color grading, rounded buttons, hover effects (no blinking/flicker elements)

---

## Tech Stack

- **Language:** Java
- **UI Framework:** JavaFX (Scene Builder–free, built entirely in code)
- **Persistence:** Plain text file storage (`gui_leaderboard.txt`, `gui_stats.txt`)
- **Key Concepts:** Classes & Objects, `Random`, `Scanner`/GUI events, File I/O (`BufferedReader`/`FileWriter`), `Comparable`, JavaFX `Timeline` for the live timer

---

## Project Structure

```
├── GuessingGameApp.java      # Main JavaFX application (UI + game logic)
├── gui_leaderboard.txt       # Auto-generated — top 5 scores
├── gui_stats.txt             # Auto-generated — player statistics
└── README.md
```

---

## How to Run

JavaFX is no longer bundled with the JDK (Java 11+), so the JavaFX SDK must be downloaded separately.

1. Download the JavaFX SDK for your platform: https://gluonhq.com/products/javafx/
2. Compile:
   ```bash
   javac --module-path "/path/to/javafx-sdk/lib" --add-modules javafx.controls GuessingGameApp.java
   ```
3. Run:
   ```bash
   java --module-path "/path/to/javafx-sdk/lib" --add-modules javafx.controls GuessingGameApp
   ```

Alternatively, open the project in **IntelliJ IDEA** or **Eclipse** and add the JavaFX SDK as a library, then run `GuessingGameApp.java` directly.

---

## How to Play

1. Enter your name on the welcome screen.
2. Choose a difficulty level (or set a custom range).
3. Guess the secret number — the game tells you if you're too high, too low, or getting warmer/colder.
4. Guess it within the attempt limit to win and (possibly) land on the leaderboard!
5. Check your stats and streaks anytime from the welcome or result screen.

---

## Author

CloudExify Summer Internship 2026 — Java Track, Month 1, Project 1

---

## License

This project was built for educational purposes as part of the CloudExify internship program.

## Author
Salman Farooq
