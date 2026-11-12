package comets;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Reads and writes the high score as a single number in a text file. */
public class HighScoreStore {
    private final Path file;

    public HighScoreStore(Path file) {
        this.file = file;
    }

    /** Returns the saved high score, or 0 if the file is missing or unreadable. */
    public int load() {
        try {
            if (!Files.exists(file)) return 0;
            String text = Files.readString(file, StandardCharsets.UTF_8).trim();
            return Math.max(0, Integer.parseInt(text));
        } catch (IOException | NumberFormatException e) {
            return 0;   // a corrupt file shouldn't crash the game
        }
    }

    public void save(int highScore) {
        try {
            Files.writeString(file, highScore + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("could not save high score: " + e.getMessage());
        }
    }
}
