import java.awt.*;
import java.awt.image.BufferedImage;

public class Caveman extends Animal implements AnimalBehaviours{
    // stats can change later for balancing
    private static float defaultHealth = 10f;
    private static int defaultSpeed = 2;
    //private static int defaultHunger = 10; //seconds before dying?
    private static int defaultDamage = 2;
    private static int defaultMaxHunger = 800;
    private static int defaultFoodValue = 300;
    public static final int ALERT_RADIUS = 150;

    private static final BufferedImage sprite =
        SpriteLoader.load("src/assets/caveman.png");

    Caveman(){
        setStats();
        setStrategies();
        x = 0;
        y = 0;
    }

    Caveman(int spawnX, int spawnY) {
        setStats();
        setStrategies();
        x = spawnX;
        y = spawnY;
        width = 30;
        height = 50;
    }
    
        
    @Override
    public void setStats(){
        this.health = defaultHealth;
        this.speed = defaultSpeed;
        this.hunger = defaultMaxHunger;
        this.maxHunger = defaultMaxHunger;
        this.foodValue = defaultFoodValue;
        this.damage = defaultDamage;
    }

    @Override
    public void createNest(){
    }

    @Override
    public void setStrategies() {
        setWanderStrategy(new RandomWanderStrategy());
        setHuntStrategy(new HuntStrategy(a -> a instanceof Cow));
    }

    @Override
    public void draw(Graphics2D g2) {
         if (sprite != null) {
        g2.drawImage(sprite, x, y, 100, 100, null);
             } else {
        g2.setColor(Color.BLACK);
        g2.fillOval(x, y, 20, 20);
    }

    drawStats(g2);
}

}
