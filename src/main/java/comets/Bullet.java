package comets;

/** A bullet that travels in a straight line and expires after a fixed number of ticks. */
public class Bullet extends GameObject {
    public static final int LIFESPAN = 120; // ticks (2 s at 60 updates/s)
    public static final int RADIUS = 2;

    private int lifeRemaining;

    public Bullet(double x, double y, double dx, double dy) {
        super(x, y, dx, dy, 0);
        this.lifeRemaining = LIFESPAN;
    }

    public int getLifeRemaining() {
        return lifeRemaining;
    }

    public void update(int width, int height) {
        move(width, height);
        lifeRemaining--;
    }

    public boolean isExpired() {
        return lifeRemaining <= 0;
    }
}
