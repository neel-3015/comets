package comets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhysicsTest {
    private static final double EPS = 1e-9;

    @Test
    void objectsWrapAroundEveryEdge() {
        GameObject o = new GameObject(795, 5, 10, -10, 0);
        o.move(800, 600);
        assertEquals(5, o.getX(), EPS);     // off the right edge -> left side
        assertEquals(595, o.getY(), EPS);   // off the top -> bottom
    }

    @Test
    void thrustPushesShipInTheDirectionItFaces() {
        Ship ship = new Ship(400, 300);
        ship.setRotation(Math.PI / 2);            // facing down the screen
        ship.update(800, 600, true, false, false);
        assertEquals(0, ship.getDx(), 1e-12);
        assertEquals(Ship.THRUST * Ship.FRICTION, ship.getDy(), EPS);
    }

    @Test
    void frictionSlowsAnIdleShip() {
        Ship ship = new Ship(400, 300);
        ship.setDx(4);
        ship.update(800, 600, false, false, false);
        assertEquals(4 * Ship.FRICTION, ship.getDx(), EPS);
    }

    @Test
    void speedIsCappedButDirectionIsKept() {
        Ship ship = new Ship(400, 300);
        ship.setDx(30);
        ship.setDy(40);                           // speed 50, direction 3:4
        ship.update(800, 600, false, false, false);
        assertEquals(Ship.MAX_SPEED, ship.speed(), EPS);
        assertEquals(0.75, ship.getDx() / ship.getDy(), EPS);
    }

    @Test
    void turningChangesRotationByTurnRate() {
        Ship ship = new Ship(400, 300);
        ship.update(800, 600, false, true, false);
        assertEquals(-Ship.TURN_RATE, ship.getRotation(), EPS);
        ship.update(800, 600, false, false, true);
        assertEquals(0, ship.getRotation(), EPS);
    }

    @Test
    void fireRespectsCooldownAndSpawnsAtTheNose() {
        Ship ship = new Ship(100, 100);
        Bullet first = ship.fire(true, 1_000);
        assertNotNull(first);
        assertEquals(100 + Ship.NOSE_OFFSET, first.getX(), EPS);
        assertNull(ship.fire(true, 1_000 + Ship.FIRE_COOLDOWN_MS - 1), "still cooling down");
        assertNotNull(ship.fire(true, 1_000 + Ship.FIRE_COOLDOWN_MS));
        assertNull(ship.fire(false, 99_999), "not pressing fire");
    }

    @Test
    void bulletExpiresAfterItsLifespan() {
        Bullet b = new Bullet(0, 0, 1, 0);
        for (int i = 0; i < Bullet.LIFESPAN - 1; i++) b.update(800, 600);
        assertFalse(b.isExpired());
        b.update(800, 600);
        assertTrue(b.isExpired());
    }
}
