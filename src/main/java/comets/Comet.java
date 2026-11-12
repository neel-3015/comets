package comets;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** A spinning comet. Large and medium comets shatter into two smaller ones when hit. */
public class Comet extends GameObject {
    public static final int LARGE = 3;
    public static final int MEDIUM = 2;
    public static final int SMALL = 1;

    public static final int RADIUS_LARGE = 40;
    public static final int RADIUS_MEDIUM = 25;
    public static final int RADIUS_SMALL = 15;

    /** Pieces fly apart 20% faster than the comet they came from. */
    public static final double SHATTER_SPEEDUP = 1.2;

    private final double rotationSpeed;
    private final int size;

    public Comet(double x, double y, double dx, double dy, double rotation, double rotationSpeed, int size) {
        super(x, y, dx, dy, rotation);
        if (size < SMALL || size > LARGE) {
            throw new IllegalArgumentException("comet size must be 1-3, got " + size);
        }
        this.rotationSpeed = rotationSpeed;
        this.size = size;
    }

    public double getRotationSpeed() { return rotationSpeed; }
    public int getSize() { return size; }

    public int getRadius() {
        return switch (size) {
            case LARGE -> RADIUS_LARGE;
            case MEDIUM -> RADIUS_MEDIUM;
            default -> RADIUS_SMALL;
        };
    }

    /** Points for destroying this comet. */
    public int points() {
        return 10 * size;
    }

    public void update(int width, int height) {
        setRotation(getRotation() + rotationSpeed);
        move(width, height);
    }

    public List<Comet> shatter() {
        return shatter(new Random());
    }

    /**
     * Breaks this comet into two smaller comets flying off in random directions.
     * @return two comets one size smaller, or an empty list if this one is already small
     */
    public List<Comet> shatter(Random rand) {
        List<Comet> pieces = new ArrayList<>();
        int nextSize = size - 1;
        if (nextSize < SMALL) {
            return pieces;
        }
        double pieceSpeed = speed() * SHATTER_SPEEDUP;
        for (int i = 0; i < 2; i++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            pieces.add(new Comet(
                    getX(), getY(),
                    pieceSpeed * Math.cos(angle), pieceSpeed * Math.sin(angle),
                    rand.nextDouble() * 2 * Math.PI,
                    rand.nextDouble() * 0.05 - 0.025,
                    nextSize));
        }
        return pieces;
    }
}
