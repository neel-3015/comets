package comets;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * All game state and rules, with no Swing code, so the rules can be unit tested.
 *
 * Every public method is synchronized: the game-loop thread calls update(),
 * while the Swing event thread calls restart() / hyperspace() from key presses
 * and the snapshot methods while painting. One lock keeps them from seeing
 * half-updated state.
 */
public final class GameWorld {
    public static final int STARTING_LIVES = 3;
    public static final int STARTING_COMETS = 5;
    public static final int SHIP_HIT_BUFFER = 10;      // px added to comet radius for ship hits
    public static final double SAFE_SPAWN_DISTANCE = 150;
    public static final double HYPERSPACE_FAIL_CHANCE = 0.10;

    private final int width;
    private final int height;
    private final Random rand;
    private final HighScoreStore store;

    private Ship ship;
    private final List<Comet> comets = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private int score;
    private int lives;
    private int highScore;
    private int wave;
    private boolean highScoreSaved;

    public GameWorld(int width, int height, Random rand, HighScoreStore store) {
        this.width = width;
        this.height = height;
        this.rand = rand;
        this.store = store;
        restart();
    }

    /** Starts a new game: fresh ship, 3 lives, score 0, first wave of comets. */
    public synchronized void restart() {
        ship = newShipAtCenter();
        comets.clear();
        bullets.clear();
        score = 0;
        lives = STARTING_LIVES;
        wave = 1;
        highScore = store.load();
        highScoreSaved = false;
        spawnComets(STARTING_COMETS);
    }

    /** Advances the game one tick (called 60 times per second). */
    public synchronized void update(boolean thrust, boolean left, boolean right, boolean firing, long nowMillis) {
        if (isGameOver()) {
            return;
        }
        ship.update(width, height, thrust, left, right);

        Bullet b = ship.fire(firing, nowMillis);
        if (b != null) {
            bullets.add(b);
        }

        for (Iterator<Bullet> it = bullets.iterator(); it.hasNext(); ) {
            Bullet bullet = it.next();
            bullet.update(width, height);
            if (bullet.isExpired()) {
                it.remove();
            }
        }
        for (Comet c : comets) {
            c.update(width, height);
        }

        handleBulletHits();
        handleShipHit();

        // wave cleared: next wave is bigger (one extra comet per 500 points)
        if (comets.isEmpty() && !isGameOver()) {
            wave++;
            spawnComets(STARTING_COMETS + score / 500);
        }
    }

    /** Each bullet destroys at most one comet; hit comets shatter into smaller ones. */
    void handleBulletHits() {
        List<Comet> pieces = new ArrayList<>();
        for (Iterator<Bullet> bit = bullets.iterator(); bit.hasNext(); ) {
            Bullet bullet = bit.next();
            for (Iterator<Comet> cit = comets.iterator(); cit.hasNext(); ) {
                Comet c = cit.next();
                if (Point2D.distance(bullet.getX(), bullet.getY(), c.getX(), c.getY()) < c.getRadius()) {
                    bit.remove();
                    cit.remove();
                    addScore(c.points());
                    pieces.addAll(c.shatter(rand));
                    break;
                }
            }
        }
        comets.addAll(pieces);
    }

    /** At most one ship collision per tick. Losing a life respawns the ship and clears bullets. */
    void handleShipHit() {
        for (Comet c : comets) {
            double dist = Point2D.distance(c.getX(), c.getY(), ship.getX(), ship.getY());
            if (dist < c.getRadius() + SHIP_HIT_BUFFER) {
                loseLife();
                if (!isGameOver()) {
                    ship = newShipAtCenter();
                    bullets.clear();
                }
                return;
            }
        }
    }

    /**
     * Teleports the ship to a random spot, with a 10% chance the jump fails
     * and costs a life. Ignored once the game is over.
     */
    public synchronized void hyperspace() {
        if (isGameOver()) {
            return;
        }
        if (rand.nextDouble() < HYPERSPACE_FAIL_CHANCE) {
            loseLife();
        } else {
            ship.setX(rand.nextDouble() * width);
            ship.setY(rand.nextDouble() * height);
        }
    }

    private void addScore(int points) {
        score += points;
        highScore = Math.max(highScore, score);
    }

    private void loseLife() {
        lives = Math.max(0, lives - 1);
        if (lives == 0 && !highScoreSaved) {
            store.save(highScore);       // saved exactly once per game
            highScoreSaved = true;
        }
    }

    private Ship newShipAtCenter() {
        return new Ship(width / 2.0, height / 2.0);
    }

    /** Spawns large comets away from the center, aimed roughly at it. */
    private void spawnComets(int count) {
        double cx = width / 2.0, cy = height / 2.0;
        for (int i = 0; i < count; i++) {
            double x, y;
            do {
                x = rand.nextDouble() * width;
                y = rand.nextDouble() * height;
            } while (Point2D.distance(x, y, cx, cy) < SAFE_SPAWN_DISTANCE);

            double angle = Math.atan2(cy - y, cx - x) + (rand.nextDouble() * Math.PI / 4 - Math.PI / 8);
            double speed = 1.0 + rand.nextDouble() * 2.0;
            comets.add(new Comet(x, y, speed * Math.cos(angle), speed * Math.sin(angle),
                    rand.nextDouble() * 2 * Math.PI, rand.nextDouble() * 0.05 - 0.025, Comet.LARGE));
        }
    }

    // ---- read access (snapshots, so painting never races the game loop) ----

    public synchronized boolean isGameOver() { return lives <= 0; }
    public synchronized int getScore() { return score; }
    public synchronized int getLives() { return lives; }
    public synchronized int getHighScore() { return highScore; }
    public synchronized int getWave() { return wave; }
    public synchronized Ship getShip() { return ship; }
    public synchronized List<Comet> cometsSnapshot() { return new ArrayList<>(comets); }
    public synchronized List<Bullet> bulletsSnapshot() { return new ArrayList<>(bullets); }

    // ---- test hooks (package-private) ----

    synchronized void clearComets() { comets.clear(); }
    synchronized void addComet(Comet c) { comets.add(c); }
    synchronized void addBullet(Bullet b) { bullets.add(b); }
    synchronized void placeShip(double x, double y) { ship.setX(x); ship.setY(y); }
}
