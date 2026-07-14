import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Toolkit;
import java.io.*;
import java.util.*;

public class GuessingGameApp extends Application {

    // ===================== THEME (color grading, no blinking) =====================
    private static final String BG_DARK      = "#1b1e2b";
    private static final String BG_CARD      = "#242838";
    private static final String ACCENT_GOLD  = "#ffd166";
    private static final String ACCENT_ORANGE= "#ff8c42";
    private static final String ACCENT_GREEN = "#4caf78";
    private static final String ACCENT_RED   = "#e5533c";
    private static final String ACCENT_BLUE  = "#4a90d9";
    private static final String TEXT_WHITE   = "#f5f6fa";
    private static final String TEXT_MUTED   = "#9aa0b4";

    // ===================== FILES =====================
    private static final String LEADERBOARD_FILE = "gui_leaderboard.txt";
    private static final String STATS_FILE        = "gui_stats.txt";

    //STATE 
    private Stage stage;
    private String playerName = "Player";
    private int min, max, secretNumber, attempts, maxAttempts;
    private Integer lastGuess = null;
    private long startTimeMillis;
    private Timeline timerTimeline;
    private int elapsedSeconds;

    // persistent state
    private int totalGames, totalWins, currentStreak, bestStreak;
    private long sumAttempts; // for average

    private Label timerLabel;
    private Label attemptsLabel;
    private Label hintLabel;
    private ProgressBar progressBar;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        loadStats();
        stage.setTitle("CloudExify - Number Guessing Game");
        stage.setResizable(false);
        showWelcomeScreen();
        stage.show();
    }

    // SCREEN 1: WELCOME / NAME
    private void showWelcomeScreen() {
        VBox root = baseCard();

        Label title = h1("CLOUDEXIFY");
        Label subtitle = h2("Number Guessing Game");

        TextField nameField = new TextField();
        nameField.setPromptText("Enter your name");
        nameField.setMaxWidth(240);
        styleTextField(nameField);

        Button startBtn = primaryButton("Start Game");
        startBtn.setOnAction(e -> {
            String n = nameField.getText().trim();
            playerName = n.isEmpty() ? "Player" : n;
            showDifficultyScreen();
        });

        HBox secondaryRow = new HBox(12);
        secondaryRow.setAlignment(Pos.CENTER);
        Button leaderboardBtn = secondaryButton("Leaderboard");
        leaderboardBtn.setOnAction(e -> showLeaderboardScreen());
        Button statsBtn = secondaryButton("Stats");
        statsBtn.setOnAction(e -> showStatsScreen());
        secondaryRow.getChildren().addAll(leaderboardBtn, statsBtn);

        root.getChildren().addAll(title, subtitle, spacer(20), nameField, spacer(16), startBtn, spacer(24), secondaryRow);
        setScene(root);
    }

    // SCREEN 2: DIFFICULTY 
    private void showDifficultyScreen() {
        VBox root = baseCard();
        Label title = h2("Choose Difficulty");

        Button easyBtn   = difficultyButton("Easy", "1 - 50", ACCENT_GREEN);
        Button mediumBtn = difficultyButton("Medium", "1 - 100", ACCENT_BLUE);
        Button hardBtn   = difficultyButton("Hard", "1 - 200", ACCENT_RED);
        Button customBtn = difficultyButton("Custom", "Set your own range", ACCENT_GOLD);

        easyBtn.setOnAction(e -> startGame(1, 50));
        mediumBtn.setOnAction(e -> startGame(1, 100));
        hardBtn.setOnAction(e -> startGame(1, 200));

        customBtn.setOnAction(e -> {
            TextField minField = new TextField();
            minField.setPromptText("Min");
            minField.setMaxWidth(90);
            styleTextField(minField);
            TextField maxField = new TextField();
            maxField.setPromptText("Max");
            maxField.setMaxWidth(90);
            styleTextField(maxField);

            HBox rangeRow = new HBox(10, minField, maxField);
            rangeRow.setAlignment(Pos.CENTER);

            Label errorLabel = errorLabel("");

            Button confirmBtn = primaryButton("Confirm");
            confirmBtn.setOnAction(ev -> {
                try {
                    int mn = Integer.parseInt(minField.getText().trim());
                    int mx = Integer.parseInt(maxField.getText().trim());
                    if (mn >= mx) {
                        errorLabel.setText("Min must be less than Max");
                    } else {
                        startGame(mn, mx);
                    }
                } catch (NumberFormatException ex) {
                    errorLabel.setText("Enter valid whole numbers");
                }
            });

            VBox customBox = baseCard();
            customBox.getChildren().addAll(h2("Custom Range"), rangeRow, errorLabel, confirmBtn, backButton());
            setScene(customBox);
        });

        Button back = backButton();

        root.getChildren().addAll(title, spacer(10), easyBtn, mediumBtn, hardBtn, customBtn, spacer(16), back);
        setScene(root);
    }

    // GAME START
    private void startGame(int min, int max) {
        this.min = min;
        this.max = max;
        int range = max - min + 1;
        this.maxAttempts = (int) Math.ceil(Math.log(range) / Math.log(2)) + 4;
        this.secretNumber = new Random().nextInt(range) + min;
        this.attempts = 0;
        this.lastGuess = null;
        this.elapsedSeconds = 0;
        this.startTimeMillis = System.currentTimeMillis();
        showGameScreen();
        startTimer();
    }

    //SCREEN 3: GAMEPLAY
    private void showGameScreen() {
        VBox root = baseCard();

        Label title = h2("Guess between " + min + " and " + max);

        timerLabel = smallLabel("Time: 0s");
        timerLabel.setTextFill(Color.web(ACCENT_GOLD));

        attemptsLabel = smallLabel("Attempts: 0 / " + maxAttempts);
        attemptsLabel.setTextFill(Color.web(TEXT_MUTED));

        HBox statsRow = new HBox(20, timerLabel, attemptsLabel);
        statsRow.setAlignment(Pos.CENTER);

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(260);
        progressBar.setStyle("-fx-accent: " + ACCENT_GREEN + ";");

        TextField guessField = new TextField();
        guessField.setPromptText("Your guess");
        guessField.setMaxWidth(140);
        styleTextField(guessField);

        hintLabel = new Label("Make your first guess!");
        hintLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 16));
        hintLabel.setTextFill(Color.web(TEXT_WHITE));
        hintLabel.setWrapText(true);
        hintLabel.setMaxWidth(280);
        hintLabel.setAlignment(Pos.CENTER);
        hintLabel.setStyle("-fx-text-alignment: center;");

        Button guessBtn = primaryButton("Guess");
        Runnable submitGuess = () -> handleGuess(guessField, guessBtn);
        guessBtn.setOnAction(e -> submitGuess.run());
        guessField.setOnAction(e -> submitGuess.run());

        root.getChildren().addAll(title, statsRow, progressBar, spacer(10),
                hintLabel, spacer(10), guessField, guessBtn);
        setScene(root);
    }

    private void handleGuess(TextField guessField, Button guessBtn) {
        String text = guessField.getText().trim();
        int guess;
        try {
            guess = Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            hintLabel.setText("Please enter a valid whole number.");
            hintLabel.setTextFill(Color.web(ACCENT_RED));
            return;
        }
        if (guess < min || guess > max) {
            hintLabel.setText("Stay within " + min + " - " + max + "!");
            hintLabel.setTextFill(Color.web(ACCENT_RED));
            return;
        }

        attempts++;
        attemptsLabel.setText("Attempts: " + attempts + " / " + maxAttempts);
        double progress = Math.min(1.0, (double) attempts / maxAttempts);
        progressBar.setProgress(progress);
        progressBar.setStyle("-fx-accent: " + (progress < 0.6 ? ACCENT_GREEN : progress < 0.85 ? ACCENT_GOLD : ACCENT_RED) + ";");

        if (guess == secretNumber) {
            endGame(true);
            return;
        }

        String direction = guess < secretNumber ? "Too LOW" : "Too HIGH";
        String warmth = "";
        if (lastGuess != null) {
            int prevDist = Math.abs(lastGuess - secretNumber);
            int newDist = Math.abs(guess - secretNumber);
            warmth = newDist < prevDist ? "  (Getting warmer)" : "  (Getting colder)";
        }
        lastGuess = guess;

        hintLabel.setText(direction + warmth);
        hintLabel.setTextFill(Color.web(guess < secretNumber ? ACCENT_BLUE : ACCENT_ORANGE));
        guessField.clear();

        if (attempts >= maxAttempts) {
            endGame(false);
        }
    }

    private void startTimer() {
        if (timerTimeline != null) {
            timerTimeline.stop();
        }
        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            elapsedSeconds++;
            if (timerLabel != null) {
                timerLabel.setText("Time: " + elapsedSeconds + "s");
            }
        }));
        timerTimeline.setCycleCount(Timeline.INDEFINITE);
        timerTimeline.play();
    }

    //SCREEN 4: RESULT
    private void endGame(boolean won) {
        if (timerTimeline != null) {
            timerTimeline.stop();
        }
        Toolkit.getDefaultToolkit().beep();

        totalGames++;
        sumAttempts += attempts;
        if (won) {
            totalWins++;
            currentStreak++;
            bestStreak = Math.max(bestStreak, currentStreak);
            saveLeaderboardEntry(new PlayerRecord(playerName, attempts, elapsedSeconds, rangeLabel()));
        } else {
            currentStreak = 0;
        }
        saveStats();

        VBox root = baseCard();
        Label title = won ? h1("YOU WON!") : h1("GAME OVER");
        title.setTextFill(Color.web(won ? ACCENT_GREEN : ACCENT_RED));

        String message = won
                ? "Guessed " + secretNumber + " in " + attempts + " attempts, " + elapsedSeconds + "s"
                : "The number was " + secretNumber;
        Label msgLabel = smallLabel(message);
        msgLabel.setTextFill(Color.web(TEXT_WHITE));

        Label streakLabel = smallLabel("Current streak: " + currentStreak + "   |   Best streak: " + bestStreak);
        streakLabel.setTextFill(Color.web(ACCENT_GOLD));

        Button playAgainBtn = primaryButton("Play Again");
        playAgainBtn.setOnAction(e -> showDifficultyScreen());

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER);
        Button leaderboardBtn = secondaryButton("Leaderboard");
        leaderboardBtn.setOnAction(e -> showLeaderboardScreen());
        Button statsBtn = secondaryButton("Stats");
        statsBtn.setOnAction(e -> showStatsScreen());
        row.getChildren().addAll(leaderboardBtn, statsBtn);

        root.getChildren().addAll(title, msgLabel, streakLabel, spacer(16), playAgainBtn, spacer(16), row);
        setScene(root);
    }

    private String rangeLabel() {
        if (min == 1 && max == 50) return "Easy";
        if (min == 1 && max == 100) return "Medium";
        if (min == 1 && max == 200) return "Hard";
        return "Custom(" + min + "-" + max + ")";
    }

    //SCREEN 5: LEADERBOARD
    private void showLeaderboardScreen() {
        VBox root = baseCard();
        Label title = h2("Top 5 Leaderboard");

        List<PlayerRecord> records = loadLeaderboard();
        VBox list = new VBox(6);
        list.setAlignment(Pos.CENTER);

        if (records.isEmpty()) {
            list.getChildren().add(smallLabel("No scores yet — go play!"));
        } else {
            int rank = 1;
            for (PlayerRecord r : records) {
                Label entry = new Label(rank + ". " + r.name + " — " + r.attempts + " attempts, "
                        + r.timeSeconds + "s (" + r.difficulty + ")");
                entry.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
                entry.setTextFill(Color.web(rank == 1 ? ACCENT_GOLD : TEXT_WHITE));
                list.getChildren().add(entry);
                rank++;
            }
        }

        root.getChildren().addAll(title, list, spacer(16), backButton());
        setScene(root);
    }

    // SCREEN 6: STATS 
    private void showStatsScreen() {
        VBox root = baseCard();
        Label title = h2("Your Stats");

        double winRate = totalGames == 0 ? 0 : (100.0 * totalWins / totalGames);
        double avgAttempts = totalGames == 0 ? 0 : ((double) sumAttempts / totalGames);

        VBox statsBox = new VBox(8);
        statsBox.setAlignment(Pos.CENTER);
        statsBox.getChildren().addAll(
                statLine("Total Games", String.valueOf(totalGames)),
                statLine("Total Wins", String.valueOf(totalWins)),
                statLine("Win Rate", String.format("%.1f%%", winRate)),
                statLine("Average Attempts", String.format("%.1f", avgAttempts)),
                statLine("Current Streak", String.valueOf(currentStreak)),
                statLine("Best Streak", String.valueOf(bestStreak))
        );

        root.getChildren().addAll(title, statsBox, spacer(16), backButton());
        setScene(root);
    }

    private HBox statLine(String label, String value) {
        Label l = new Label(label + ":");
        l.setTextFill(Color.web(TEXT_MUTED));
        l.setFont(Font.font("Consolas", 13));
        Label v = new Label(value);
        v.setTextFill(Color.web(ACCENT_GOLD));
        v.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
        HBox row = new HBox(10, l, v);
        row.setAlignment(Pos.CENTER);
        return row;
    }

    //  FILE PERSISTENCE
    private List<PlayerRecord> loadLeaderboard() {
        List<PlayerRecord> records = new ArrayList<>();
        File file = new File(LEADERBOARD_FILE);
        if (!file.exists()) return records;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    records.add(new PlayerRecord(parts[0], Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2]), parts[3]));
                }
            }
        } catch (IOException e) {
            System.out.println("Could not read leaderboard.");
        }
        Collections.sort(records);
        return records;
    }

    private void saveLeaderboardEntry(PlayerRecord newRecord) {
        List<PlayerRecord> records = loadLeaderboard();
        records.add(newRecord);
        Collections.sort(records);
        while (records.size() > 5) {
            records.remove(records.size() - 1);
        }
        try (FileWriter writer = new FileWriter(LEADERBOARD_FILE)) {
            for (PlayerRecord r : records) {
                writer.write(r.name + "," + r.attempts + "," + r.timeSeconds + "," + r.difficulty + "\n");
            }
        } catch (IOException e) {
            System.out.println("Could not save leaderboard.");
        }
    }

    private void loadStats() {
        File file = new File(STATS_FILE);
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line != null) {
                String[] p = line.split(",");
                totalGames = Integer.parseInt(p[0]);
                totalWins = Integer.parseInt(p[1]);
                currentStreak = Integer.parseInt(p[2]);
                bestStreak = Integer.parseInt(p[3]);
                sumAttempts = Long.parseLong(p[4]);
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("Could not read stats.");
        }
    }

    private void saveStats() {
        try (FileWriter writer = new FileWriter(STATS_FILE)) {
            writer.write(totalGames + "," + totalWins + "," + currentStreak + "," + bestStreak + "," + sumAttempts);
        } catch (IOException e) {
            System.out.println("Could not save stats.");
        }
    }

    // UI HELPERS (theme + layout)
    private void setScene(VBox root) {
        Scene scene = new Scene(root, 380, 560);
        stage.setScene(scene);
    }

    private VBox baseCard() {
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setStyle("-fx-background-color: linear-gradient(to bottom, " + BG_DARK + ", " + BG_CARD + ");");
        return box;
    }

    private Label h1(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Consolas", FontWeight.EXTRA_BOLD, 30));
        l.setTextFill(Color.web(ACCENT_ORANGE));
        return l;
    }

    private Label h2(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
        l.setTextFill(Color.web(TEXT_WHITE));
        return l;
    }

    private Label smallLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Consolas", 14));
        l.setTextFill(Color.web(TEXT_MUTED));
        return l;
    }

    private Label errorLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        l.setTextFill(Color.web(ACCENT_RED));
        return l;
    }

    private Region spacer(double height) {
        Region r = new Region();
        r.setPrefHeight(height);
        return r;
    }

    private void styleTextField(TextField field) {
        field.setStyle("-fx-background-color: #2f3446; -fx-text-fill: " + TEXT_WHITE + "; "
                + "-fx-prompt-text-fill: " + TEXT_MUTED + "; -fx-background-radius: 8; "
                + "-fx-padding: 8; -fx-font-family: Consolas; -fx-font-size: 13;");
    }

    private Button primaryButton(String text) {
        Button b = new Button(text);
        b.setPrefWidth(180);
        b.setStyle(buttonStyle(ACCENT_ORANGE, "#1b1e2b"));
        b.setOnMouseEntered(e -> b.setStyle(buttonStyle(ACCENT_GOLD, "#1b1e2b")));
        b.setOnMouseExited(e -> b.setStyle(buttonStyle(ACCENT_ORANGE, "#1b1e2b")));
        return b;
    }

    private Button secondaryButton(String text) {
        Button b = new Button(text);
        b.setPrefWidth(130);
        b.setStyle(buttonStyle(BG_CARD, TEXT_WHITE) + "-fx-border-color: " + ACCENT_GOLD + "; -fx-border-radius: 8;");
        return b;
    }

    private Button difficultyButton(String name, String desc, String color) {
        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("Consolas", FontWeight.EXTRA_BOLD, 17));
        nameLabel.setTextFill(Color.web("#1b1e2b"));

        Label descLabel = new Label(desc);
        descLabel.setFont(Font.font("Consolas", FontWeight.NORMAL, 12));
        descLabel.setTextFill(Color.web("#1b1e2b"));
        descLabel.setOpacity(0.75);

        VBox content = new VBox(4, nameLabel, descLabel);
        content.setAlignment(Pos.CENTER);

        Button b = new Button();
        b.setGraphic(content);
        b.setPrefWidth(240);
        b.setPrefHeight(64);
        b.setStyle(buttonStyle(color, "#1b1e2b"));
        b.setOnMouseEntered(e -> b.setOpacity(0.85));
        b.setOnMouseExited(e -> b.setOpacity(1.0));
        return b;
    }

    private Button backButton() {
        Button b = secondaryButton("Back");
        b.setOnAction(e -> showWelcomeScreen());
        return b;
    }

    private String buttonStyle(String bg, String fg) {
        return "-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; "
                + "-fx-font-family: Consolas; -fx-font-weight: bold; -fx-font-size: 13; "
                + "-fx-background-radius: 10; -fx-padding: 10;";
    }

    public static void main(String[] args) {
        launch(args);
    }
}

// LEADERBOARD RECORD
class PlayerRecord implements Comparable<PlayerRecord> {
    String name;
    int attempts;
    int timeSeconds;
    String difficulty;

    PlayerRecord(String name, int attempts, int timeSeconds, String difficulty) {
        this.name = name;
        this.attempts = attempts;
        this.timeSeconds = timeSeconds;
        this.difficulty = difficulty;
    }

    @Override
    public int compareTo(PlayerRecord other) {
        if (this.attempts != other.attempts) {
            return Integer.compare(this.attempts, other.attempts);
        }
        return Integer.compare(this.timeSeconds, other.timeSeconds);
    }
}