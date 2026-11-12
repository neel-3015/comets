package comets;

/** The player's ship: rotation, thrust, friction, a speed cap, and a fire cooldown. */
public class Ship extends GameObject {
    public static final double THRUST = 0.15;
    public static final double FRICTION = 0.99;
    public static final double MAX_SPEED = 8.0;
    public static final double TURN_RATE = Math.toRadians(5);
    public static final long FIRE_COOLDOWN_MS = 200;
    public static final double NOSE_OFFSET = 20;

    private long lastFireTime;

    public Ship(double x, double y) {
        super(x, y, 0, 0, 0);
        this.lastFireTime = Long.MIN_VALUE / 2;   // can fire immediately
    }

    /** Applies rotation, thrust, friction and the speed cap, then moves one tick. */
    public void update(int width, int height, boolean isThrusting, boolean turnLeft, boolean turnRight) {
        double newRotation = getRotation();
        if (turnLeft)  newRotation -= TURN_RATE;
        if (turnRight) newRotation += TURN_RATE;
        setRotation(newRotation);

        double newDx = getDx();
        double newDy = getDy();

        if (isThrusting) {
            newDx += THRUST * Math.cos(newRotation);
            newDy += THRUST * Math.sin(newRotation);
        }

        newDx *= FRICTION;
        newDy *= FRICTION;

        // cap the overall speed while keeping the direction of travel
        double speed = Math.hypot(newDx, newDy);
        if (speed > MAX_SPEED) {
            newDx = newDx / speed * MAX_SPEED;
            newDy = newDy / speed * MAX_SPEED;
        }

        setDx(newDx);
        setDy(newDy);
        move(width, height);
    }

    /** Fires using the system clock. */
    public Bullet fire(boolean isFiring) {
        return fire(isFiring, System.currentTimeMillis());
    }

    /**
     * Fires a bullet from the ship's nose if the cooldown has passed.
     * Taking the time as a parameter keeps the cooldown testable.
     * @return the new bullet, or null if not firing or still cooling down
     */
    public Bullet fire(boolean isFiring, long nowMillis) {
        if (!isFiring || nowMillis - lastFireTime < FIRE_COOLDOWN_MS) {
            return null;
        }
        lastFireTime = nowMillis;

        double r = getRotation();
        double bulletSpeed = MAX_SPEED + 5;
        return new Bullet(
                getX() + NOSE_OFFSET * Math.cos(r),
                getY() + NOSE_OFFSET * Math.sin(r),
                bulletSpeed * Math.cos(r),
                bulletSpeed * Math.sin(r));
    }
}
