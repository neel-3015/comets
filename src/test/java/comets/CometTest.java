package comets;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class CometTest {

    private static Comet comet(int size, double dx, double dy) {
        return new Comet(100, 100, dx, dy, 0, 0.01, size);
    }

    @Test
    void radiusAndPointsDependOnSize() {
        assertEquals(Comet.RADIUS_LARGE, comet(Comet.LARGE, 0, 0).getRadius());
        assertEquals(Comet.RADIUS_MEDIUM, comet(Comet.MEDIUM, 0, 0).getRadius());
        assertEquals(Comet.RADIUS_SMALL, comet(Comet.SMALL, 0, 0).getRadius());
        assertEquals(30, comet(Comet.LARGE, 0, 0).points());
        assertEquals(10, comet(Comet.SMALL, 0, 0).points());
    }

    @Test
    void largeCometShattersIntoTwoFasterMediumPieces() {
        List<Comet> pieces = comet(Comet.LARGE, 3, 4).shatter(new Random(42));   // speed 5
        assertEquals(2, pieces.size());
        for (Comet p : pieces) {
            assertEquals(Comet.MEDIUM, p.getSize());
            assertEquals(5 * Comet.SHATTER_SPEEDUP, p.speed(), 1e-9);
            assertEquals(100, p.getX(), 1e-9);
        }
    }

    @Test
    void smallCometLeavesNothingBehind() {
        assertTrue(comet(Comet.SMALL, 1, 1).shatter(new Random(1)).isEmpty());
    }

    @Test
    void invalidSizeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> comet(4, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> comet(0, 0, 0));
    }

    @Test
    void updateSpinsAndMoves() {
        Comet c = comet(Comet.MEDIUM, 2, 0);
        c.update(800, 600);
        assertEquals(102, c.getX(), 1e-9);
        assertEquals(0.01, c.getRotation(), 1e-9);
    }
}
