package comets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class GameWorldTest {
    private Path scoreFile;
    private CountingStore store;
    private GameWorld world;

    /** Records how many times the high score is written. */
    static final class CountingStore extends HighScoreStore {
        int saves;
        CountingStore(Path p) { super(p); }
        @Override public void save(int highScore) { saves++; super.save(highScore); }
    }

    @BeforeEach
    void setUp() throws IOException {
        scoreFile = Files.createTempFile("comets-highscore", ".txt");
        Files.delete(scoreFile);
        store = new CountingStore(scoreFile);
        world = new GameWorld(800, 600, new Random(7), store);
        world.clearComets();                 // tests place comets themselves
    }

    private static Comet still(double x, double y, int size) {
        return new Comet(x, y, 0, 0, 0, 0, size);
    }

    private void tick() {
        world.update(false, false, false, false, 0);
    }

    @Test
    void newGameStartsWithThreeLivesAndFiveLargeComets() {
        world.restart();
        assertEquals(GameWorld.STARTING_LIVES, world.getLives());
        assertEquals(GameWorld.STARTING_COMETS, world.cometsSnapshot().size());
        for (Comet c : world.cometsSnapshot()) {
            assertEquals(Comet.LARGE, c.getSize());
            assertTrue(Math.hypot(c.getX() - 400, c.getY() - 300) >= GameWorld.SAFE_SPAWN_DISTANCE,
                    "comets never spawn on top of the ship");
        }
    }

    @Test
    void bulletHitScoresShattersAndIsConsumed() {
        world.addComet(still(100, 100, Comet.LARGE));
        world.addComet(still(700, 500, Comet.LARGE));
        world.addBullet(new Bullet(100, 100, 0, 0));
        tick();
        assertEquals(30, world.getScore());
        assertTrue(world.bulletsSnapshot().isEmpty());
        assertEquals(3, world.cometsSnapshot().size());   // 1 untouched + 2 medium pieces
        assertEquals(2, world.cometsSnapshot().stream().filter(c -> c.getSize() == Comet.MEDIUM).count());
    }

    @Test
    void oneBulletDestroysOnlyOneComet() {
        world.addComet(still(100, 100, Comet.SMALL));
        world.addComet(still(102, 100, Comet.SMALL));
        world.addComet(still(700, 500, Comet.LARGE));
        world.addBullet(new Bullet(101, 100, 0, 0));
        tick();
        assertEquals(2, world.cometsSnapshot().size());
        assertEquals(10, world.getScore());
    }

    @Test
    void shipCollisionCostsALifeAndRespawnsAtCenter() {
        world.addComet(still(100, 100, Comet.LARGE));
        world.placeShip(120, 100);
        world.addBullet(new Bullet(600, 50, 0, 0));
        tick();
        assertEquals(2, world.getLives());
        assertEquals(400, world.getShip().getX(), 1e-9);
        assertEquals(300, world.getShip().getY(), 1e-9);
        assertTrue(world.bulletsSnapshot().isEmpty(), "respawn clears bullets");
    }

    @Test
    void gameOverSavesHighScoreExactlyOnceAndFreezesTheGame() throws IOException {
        world.addComet(still(100, 100, Comet.SMALL));
        world.addBullet(new Bullet(100, 100, 0, 0));
        tick();                                            // score 10
        world.addComet(still(400, 300, Comet.LARGE));      // sits on the respawn point
        for (int i = 0; i < 10; i++) tick();
        assertTrue(world.isGameOver());
        assertEquals(0, world.getLives());
        assertEquals(1, store.saves, "high score written once, not every frame");
        assertEquals("10", Files.readString(scoreFile).trim());

        int before = world.getScore();
        world.hyperspace();                                // ignored after game over
        tick();
        assertEquals(0, world.getLives());
        assertEquals(before, world.getScore());
    }

    @Test
    void highScorePersistsAcrossGamesAndCorruptFilesAreIgnored() throws IOException {
        Files.writeString(scoreFile, "250");
        world.restart();
        assertEquals(250, world.getHighScore());
        Files.writeString(scoreFile, "not a number");
        assertEquals(0, new HighScoreStore(scoreFile).load());
    }

    @Test
    void clearingTheFieldStartsABiggerNextWave() {
        world.addComet(still(100, 100, Comet.SMALL));
        world.addBullet(new Bullet(100, 100, 0, 0));
        tick();
        assertEquals(2, world.getWave());
        assertEquals(GameWorld.STARTING_COMETS, world.cometsSnapshot().size());
    }

    @Test
    void hyperspaceEitherTeleportsOrCostsALife() {
        world.addComet(still(10, 10, Comet.SMALL));
        int teleports = 0, failures = 0;
        for (int i = 0; i < 200 && !world.isGameOver(); i++) {
            int lives = world.getLives();
            double x = world.getShip().getX(), y = world.getShip().getY();
            world.hyperspace();
            if (world.getLives() < lives) failures++;
            else if (world.getShip().getX() != x || world.getShip().getY() != y) teleports++;
        }
        assertTrue(teleports > 0);
        assertTrue(failures > 0, "10% failure chance should show up in 200 jumps");
        assertTrue(world.getLives() >= 0);
    }
}
