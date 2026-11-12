package comets;

import javax.swing.JPanel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.nio.file.Path;
import java.util.Random;

/** Draws the GameWorld and turns key presses into input. Runs the fixed-timestep game loop. */
public final class GamePanel extends JPanel implements Runnable, KeyListener {
    private static final long serialVersionUID = 1L;
    private static final double NS_PER_TICK = 1_000_000_000.0 / 60.0;   // 60 updates per second

    private final transient GameWorld world;
    private final Random flameRand = new Random();
    private volatile boolean running;

    // written on the Swing event thread, read on the game-loop thread
    private volatile boolean upPressed, leftPressed, rightPressed, firePressed;

    public GamePanel(int width, int height) {
        world = new GameWorld(width, height, new Random(), new HighScoreStore(Path.of("highscore.txt")));
        setPreferredSize(new Dimension(width, height));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
    }

    public void startGame() {
        running = true;
        Thread loop = new Thread(this, "game-loop");
        loop.setDaemon(true);
        loop.start();
    }

    /** Fixed timestep: game logic always advances in 1/60 s steps, however fast frames render. */
    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double delta = 0;
        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / NS_PER_TICK;
            lastTime = now;
            while (delta >= 1) {
                world.update(upPressed, leftPressed, rightPressed, firePressed, System.currentTimeMillis());
                delta--;
            }
            repaint();
            try {
                Thread.sleep(Math.max(1, 16 - (System.nanoTime() - now) / 1_000_000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (world.isGameOver()) {
            drawGameOver(g2d);
            return;
        }
        drawComets(g2d);
        drawShip(g2d);
        drawBullets(g2d);
        drawHud(g2d);
    }

    private void drawComets(Graphics2D g2d) {
        g2d.setColor(Color.LIGHT_GRAY);
        for (Comet comet : world.cometsSnapshot()) {
            AffineTransform saved = g2d.getTransform();
            g2d.translate(comet.getX(), comet.getY());
            g2d.rotate(comet.getRotation());
            int radius = comet.getRadius();
            Path2D.Double shape = new Path2D.Double();
            int points = 12;
            for (int i = 0; i < points; i++) {
                double a = i * 2 * Math.PI / points;
                double px = radius * Math.cos(a), py = radius * Math.sin(a);
                if (i == 0) shape.moveTo(px, py); else shape.lineTo(px, py);
            }
            shape.closePath();
            g2d.draw(shape);
            g2d.setTransform(saved);
        }
    }

    private void drawShip(Graphics2D g2d) {
        Ship ship = world.getShip();
        AffineTransform saved = g2d.getTransform();
        g2d.translate(ship.getX(), ship.getY());
        g2d.rotate(ship.getRotation());

        Path2D.Double hull = new Path2D.Double();
        hull.moveTo(15, 0);
        hull.lineTo(-10, 10);
        hull.lineTo(-5, 0);
        hull.lineTo(-10, -10);
        hull.closePath();
        g2d.setColor(Color.WHITE);
        g2d.draw(hull);

        if (upPressed) {
            g2d.setColor(Color.YELLOW);
            Path2D.Double flame = new Path2D.Double();
            flame.moveTo(-10, 0);
            flame.lineTo(-15 - flameRand.nextInt(5), 5);
            flame.lineTo(-15 - flameRand.nextInt(5), -5);
            flame.closePath();
            g2d.fill(flame);
        }
        g2d.setTransform(saved);
    }

    private void drawBullets(Graphics2D g2d) {
        g2d.setColor(Color.RED);
        for (Bullet b : world.bulletsSnapshot()) {
            g2d.fillOval((int) b.getX() - Bullet.RADIUS, (int) b.getY() - Bullet.RADIUS,
                    Bullet.RADIUS * 2, Bullet.RADIUS * 2);
        }
    }

    private void drawHud(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.drawString("Score: " + world.getScore(), 10, 25);
        g2d.drawString("Lives: " + world.getLives(), 10, 50);
        g2d.drawString("High Score: " + world.getHighScore(), 10, 75);
        g2d.drawString("Wave: " + world.getWave(), 10, 100);
    }

    private void drawGameOver(Graphics2D g2d) {
        drawCentered(g2d, "GAME OVER", new Font("Arial", Font.BOLD, 48), Color.RED, -30);
        Font f = new Font("Arial", Font.PLAIN, 24);
        drawCentered(g2d, "Final Score: " + world.getScore(), f, Color.WHITE, 20);
        drawCentered(g2d, "High Score: " + world.getHighScore(), f, Color.WHITE, 50);
        drawCentered(g2d, "Press R to play again", new Font("Arial", Font.PLAIN, 18), Color.GRAY, 90);
    }

    private void drawCentered(Graphics2D g2d, String text, Font font, Color color, int dy) {
        g2d.setFont(font);
        g2d.setColor(color);
        int w = g2d.getFontMetrics().stringWidth(text);
        g2d.drawString(text, (getWidth() - w) / 2, getHeight() / 2 + dy);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP, KeyEvent.VK_W -> upPressed = true;
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> leftPressed = true;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> rightPressed = true;
            case KeyEvent.VK_SPACE -> firePressed = true;
            case KeyEvent.VK_E -> world.hyperspace();
            case KeyEvent.VK_R -> { if (world.isGameOver()) world.restart(); }
            default -> { }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP, KeyEvent.VK_W -> upPressed = false;
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> leftPressed = false;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> rightPressed = false;
            case KeyEvent.VK_SPACE -> firePressed = false;
            default -> { }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) { }
}
