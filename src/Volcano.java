import java.awt.*;
import java.awt.image.BufferedImage;

public class Volcano extends Zone {

        // set the x value for where i want the zone to begin
    int r;
    int lavaRadius;
    private static final BufferedImage sprite = SpriteLoader.load("src/assets/volcano.png");



    enum Phase {
        DORMANT, ERUPTING, FADING
    }

    Phase phase = Phase.DORMANT;
    boolean errupting = false;
    int tickCount = 0;

    static final int DORMANT_TICKS = 3750;
    static final int MAX_LAVA = 180;
    static final int GROW_RATE = 3;
    static final int SHRINK_RATE = 1;

    // tick rate for timing on when the volcano errupts
    Volcano(int spawnX, int spawnY, int radius, int lavaRadius) {
        x = spawnX;
        y = spawnY;
        r = radius;
        this.lavaRadius = lavaRadius;
    }

    @Override
    public Shape getShape() {
        return new java.awt.geom.Ellipse2D.Double(x - lavaRadius, (y - r) - lavaRadius, lavaRadius * 2, lavaRadius * 2);
    }

    @Override
    public void update() {
        switch (phase) {
            case DORMANT:
                tickCount++;
                if (tickCount >= DORMANT_TICKS) {
                    phase = Phase.ERUPTING;
                }
                break;
            case ERUPTING:
                lavaRadius += GROW_RATE;
                if (lavaRadius >= MAX_LAVA) {
                    lavaRadius = MAX_LAVA;
                    phase = Phase.FADING;
                }
                break;
            case FADING:
                lavaRadius -= SHRINK_RATE;
                if (lavaRadius <= 0) {
                    lavaRadius = 0;
                    tickCount = 0; // timer restarts
                    phase = Phase.DORMANT;
                }
                break;
        }

    }

    @Override
    public boolean contains(int checkX, int checkY) {
        if (lavaRadius <= 0)
            return false; // no lava, nothing to be inside
        double dx = checkX - x;
        double dy = checkY - (y - r);
        return Math.sqrt(dx * dx + dy * dy) <= lavaRadius;
    }

    @Override
    public void applyEffect(Animal a) {
        a.kill();
    }

    @Override
    public void onGrass(Grass g) {
        g.isEaten = true;
    }

    @Override
    public boolean suppressesGrass() {
        return phase != Phase.DORMANT; // no grass spawning while erupting or fading
    }

    @Override
    public void draw(Graphics2D drawing) { // draws the shape of the volcano and lava
        // volcano base
        if (sprite !=null) {
        drawing.drawImage(sprite, x - r, y - r, r * 2, r * 2, null);
        } else {
        drawing.setColor(new Color(90, 70, 60));
        int[] volcX = { x, x - r, x + r };
        int[] volcY = { y - r, y + r, y + r };
        drawing.fillPolygon(volcX, volcY, 3);
        }

        // lava
        drawing.setColor(new Color(255, 80, 0, 200));
        int lavaX = x - lavaRadius;
        int lavaY = (y - r) - lavaRadius;
        ;
        drawing.fillOval(lavaX, lavaY, lavaRadius * 2, lavaRadius * 2);

    }

}
