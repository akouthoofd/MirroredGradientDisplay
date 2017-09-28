import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Playing with pixels
 */
public class Pixel extends Canvas {
    private static final int GRID_SIZE = 500;
    private static final int FRAME_SIZE = 1000;
    private static final String GAME_TITLE = "Gradient Display";
    private static final long ONE_SECOND_NANO = 1000000000;
    private static final long GENERATION_SPEED = 30;
    
    // The world to be displayed
    private BufferedImage image;
    // Each pixel in the buffered image
    private int[] pixels;
    // A duplicate of @pixels
    private int[] mask;
    // Pause variable
    private boolean paused;

    /**
     * Start of the program
     * @param args
     *      Unused but required parameters
     */
    public static void main(String[] args) {
       SwingUtilities.invokeLater(Pixel::new);     
    }

    /**
     * Constructor.
     * Sets up container for content
     */
    private Pixel() {
        // Setting up main frame
        JFrame frame = new JFrame(GAME_TITLE);
        // Exit on close
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        // Adding canvas to frame
        frame.add(this);
        // Set preferred size of screen
        frame.setPreferredSize(new Dimension(FRAME_SIZE, FRAME_SIZE));
        // Packing...
        frame.pack();
        // Display!
        frame.setVisible(true);
        // Kick off game off EDT
        new Thread(this::run).start();
        // Add action listener
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                double projection = (double)FRAME_SIZE / (double)GRID_SIZE;
                int x = (int) (e.getX() / projection);
                int y = (int) (e.getY() / projection);
                mask[x + (GRID_SIZE * y)] = 0xFF0000; // Set red on click
            }
        });
        // Pause game and move step
        addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_SPACE)
                    paused = !paused;
                else if (e.getKeyCode() == KeyEvent.VK_RIGHT)
                    update();
            }
        });
    }

    /**
     * Initial game setup and then run the game loop forever
     */
    private void run() {
        image = new BufferedImage(GRID_SIZE, GRID_SIZE, BufferedImage.TYPE_INT_RGB);
        pixels = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        paused = false;
        // Randomize initial world
        for (int i = 0; i < pixels.length; i++)
            pixels[i] = 0x000000; // Set screen to Black
        pixels[0] = 0xFF0000; // Set initial pixel Red

        double frameCut = ONE_SECOND_NANO / GENERATION_SPEED;

        long currentTime = System.nanoTime();
        long previousTime;
        long deltaTime;
        double unprocessedTime = 0.0;

        while (true) {
            previousTime = currentTime;
            currentTime = System.nanoTime();
            deltaTime = currentTime - previousTime;

            unprocessedTime += deltaTime;

            // Update @GENERATION_SPEED times per second
            if (unprocessedTime > frameCut && !paused) {
                unprocessedTime = 0L;
                update();
            }

            render();
        }
    }

    /**
     * Updates game state
     */
    private void update() {
        mask = Arrays.copyOf(pixels, pixels.length);
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                int result = pixels[x + (y * GRID_SIZE)];

                int left = x - 1;
                int right = x + 1;
                int top = y - 1;
                int bottom = y + 1;

                if (x != 0) // left
                    result = ((result ^ pixels[left + (y * GRID_SIZE)]) >> 1) + (result & pixels[left + (y * GRID_SIZE)]);
                if (y != 0) // top
                    result = ((result ^ pixels[top + (x * GRID_SIZE)]) >> 1) + (result & pixels[top + (x * GRID_SIZE)]);
                if (x != 0 && y != 0) // top left
                    result = ((result ^ pixels[left + (top * GRID_SIZE)]) >> 1) + (result & pixels[left + (top * GRID_SIZE)]);

                mask[x + (y * GRID_SIZE)] = result;
            }
        }
    }

    /**
     * Displays the current game state to screen
     */
    private void render() {
        BufferStrategy bs = getBufferStrategy();
        if (bs == null) {
            // Three means if current render (using 1 & 2) is computed, look ahead and compute next render (stored in 3)
            createBufferStrategy(3);
            return;
        }

        Graphics g = bs.getDrawGraphics();
        
        if (mask == null)
            mask = pixels;

        for (int i = 0; i < pixels.length; i++)
            pixels[i] = mask[i];

        g.drawImage(image, 0, 0, FRAME_SIZE, FRAME_SIZE, this);
        g.dispose();
        bs.show();
    }
}
