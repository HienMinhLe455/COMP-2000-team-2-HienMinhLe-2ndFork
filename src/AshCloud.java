import java.awt.*;
import java.awt.geom.Ellipse2D;

public class AshCloud extends Zone {
    private final Volcano volcano;
    private static final int MARGIN = 120; // how far the ash reaches past the lava edge

    AshCloud(Volcano volcano) {
        this.volcano = volcano;
        this.x = volcano.x;
        this.y = volcano.y;
    }

    private boolean active() {
        return volcano.lavaRadius > 0; // only exists while the volcano is erupting or fading
    }

    private int radius() {
        return volcano.lavaRadius + MARGIN;
    }

    @Override
    public void update() { } // follows the volcano, nothing of its own to animate

    @Override
    public boolean contains(int checkX, int checkY) {
        if (!active()) return false;
        double dx = checkX - volcano.x;
        double dy = checkY - (volcano.y - volcano.r); // same centre as the lava circle
        return Math.sqrt(dx * dx + dy * dy) <= radius();
    }

    @Override
    public Shape getShape() {
        if (!active()) return new Ellipse2D.Double(0, 0, 0, 0);
        int cy = volcano.y - volcano.r;
        return new Ellipse2D.Double(volcano.x - radius(), cy - radius(), radius() * 2, radius() * 2);
    }

    @Override
    public void draw(Graphics2D drawing) {
        if (!active()) return;
        drawing.setColor(new Color(90, 90, 90, 70)); // translucent grey ash
        drawing.fill(getShape());
    }
}