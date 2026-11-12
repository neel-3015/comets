package comets;

/**
 * Base class for everything that moves: position, velocity, rotation,
 * and screen wrapping (leaving one edge re-enters from the opposite edge).
 */
public class GameObject {
    private double x, y, dx, dy, rotation;

    public GameObject(double x, double y, double dx, double dy, double rotation) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.rotation = rotation;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getDx() { return dx; }
    public double getDy() { return dy; }
    public double getRotation() { return rotation; }

    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setDx(double dx) { this.dx = dx; }
    public void setDy(double dy) { this.dy = dy; }
    public void setRotation(double rotation) { this.rotation = rotation; }

    public double speed() {
        return Math.hypot(dx, dy);
    }

    /** Moves by one tick of velocity and wraps around the screen edges. */
    public void move(int width, int height) {
        x += dx;
        y += dy;

        if (x < 0) x += width;
        if (x > width) x -= width;

        if (y < 0) y += height;
        if (y > height) y -= height;
    }
}
