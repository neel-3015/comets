package comets;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Comets: an Asteroids-style arcade game in Java Swing. */
public final class Comets extends JFrame {
    private static final long serialVersionUID = 1L;
    public static final int GAME_WIDTH = 800;
    public static final int GAME_HEIGHT = 600;

    public Comets() {
        setTitle("Comets");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        GamePanel panel = new GamePanel(GAME_WIDTH, GAME_HEIGHT);
        add(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        panel.requestFocusInWindow();
        panel.startGame();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Comets::new);
    }
}
