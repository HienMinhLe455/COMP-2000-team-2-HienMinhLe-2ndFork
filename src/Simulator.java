import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Simulator extends JPanel {
    private final List<Animal> animals = new ArrayList<>();
    private final List<Zone> environment = new ArrayList<>();
    private final List<Grass> grassList = new ArrayList<>();
    private int grassSpawnTimer = 0;
   
    private final Random rand = new Random();
    private final int targetPlantCount = 12;

    private final BufferedImage background =
        SpriteLoader.load("src/assets/background.png");

    private BufferedImage scaledBg;

    // Sim thread: runs the update loop (movement, collisions, combat)
    private final ScheduledExecutorService simExecutor =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sim-loop");
            t.setDaemon(true);
            return t;
        });

    private final Queue<Runnable> commands = new ConcurrentLinkedQueue<>();

    private volatile List<Animal> animalsView = List.of();
    private volatile List<Zone> environmentView = List.of();
    private volatile List<Grass> grassView = List.of();

    private volatile int simWidth;
    private volatile int simHeight;

    public Simulator() {
        createDinosaur(150, 200);
        createDinosaur(150, 500);
        createDinosaur(400, 400);
        createDinosaur(400, 50);
        createDinosaur(300, 50);
        createCaveman(210, 200);
        createCaveman(220, 200);
        createCaveman(500, 500);
        createCaveman(500, 400);
        createCow(200, 300);
        createCow(100, 250);
        createCow(250, 100);
        createCow(400, 400);
        createCow(450, 400);
        createCow(300, 300);

        commands.add(() -> {
            environment.add(ZoneEffects.heal(ZoneEffects.shelter(new Cave(600, 525), a -> a instanceof Caveman), a -> a instanceof Caveman));
            environment.add(ZoneEffects.heal(ZoneEffects.shelter(new Cave(1500, 375), a -> a instanceof Caveman), a -> a instanceof Caveman));
            Volcano volcano = new Volcano(300, 225, 113, 0);
            environment.add(ZoneEffects.poison(new AshCloud(volcano), a -> true, 300));
            environment.add(volcano);
            environment.add(ZoneEffects.cleanse(new WaterHole(90, 750, 68), a -> true));
            environment.add(ZoneEffects.slow(new Swamp(975, 15, 300, 285), a -> true));
            environment.add(ZoneEffects.speedBoost(new Plain(750, 600, 600, 675), a -> a instanceof Dinosaur, 1.5f));
        });

        for (int i = 0; i < targetPlantCount; i++) {
            createGrass();
        }

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                simWidth = getWidth();
                simHeight = getHeight();
            }
        });

        simExecutor.scheduleAtFixedRate(this::tick, 0, 16, TimeUnit.MILLISECONDS);
    }

    private void tick() {
        try {
            Runnable c;
            while ((c = commands.poll()) != null) {
                c.run();
            }

            updateSimulation();

            // Publish fresh copies for the painter
            animalsView = new ArrayList<>(animals);
            environmentView = new ArrayList<>(environment);
            grassView = new ArrayList<>(grassList);

            repaint(); // safe to call from any thread
        } catch (Throwable t) {
            // scheduleAtFixedRate stops silently forever if a task throws
            t.printStackTrace();
        }
    }

    private void updateSimulation() {
        int screenWidth = simWidth;
        int screenHeight = simHeight;
        if (screenWidth <= 0 || screenHeight <= 0) return; // panel not on screen yet

        animals.forEach(Animal::resetEffects);

        for (Zone zone : environment) {
            zone.update();
            for (Animal a : animals) {
                if (zone.contains(a.x, a.y)) {
                    zone.applyEffect(a);
                }
            }
            for (Grass g : grassList) {
                if (zone.contains(g.x, g.y)) {
                    zone.onGrass(g);
                }
            }
        }

        // Spawning grass gets supressed when volcano errupts
        boolean suppressGrass = environment.stream().anyMatch(Zone::suppressesGrass);
        if (!suppressGrass) {
            grassSpawnTimer++;
            if (grassSpawnTimer >= 180) {
                int randomX = 30 + rand.nextInt(Math.max(100, screenWidth - 60));
                int randomY = 30 + rand.nextInt(Math.max(100, screenHeight - 60));
                grassList.add(new Grass(randomX, randomY));
                grassSpawnTimer = 0;
            }
        }

        // Update animals state
        List<Animal> animalsCopy = new ArrayList<>(animals); // Make a copy of animal list
        for (Animal a : animalsCopy) {
            a.update(screenWidth, screenHeight, animals, grassList);
        }

        // (read only) find every pair of animals collided - parallel stream
        List<Animal[]> collisions = IntStream.range(0, animals.size())
            .parallel()
            .boxed()
            .flatMap(i -> IntStream.range(i + 1, animals.size())
                .filter(j -> animals.get(i).getBounds().intersects(animals.get(j).getBounds()))
                .mapToObj(j -> new Animal[] { animals.get(i), animals.get(j) }))
            .collect(Collectors.toList());

        // resolve the fights one at a time -> avoids race conditions
        for (Animal[] pair : collisions) {
            CombatSystem.handleCollision(pair[0], pair[1], animals);
        }

        // Remove dead animals
        animals.removeIf(Animal::isDead);
        grassList.removeIf(g -> g.isEaten);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        int w = getWidth(), h = getHeight();

        // Keep the sim thread's size up to date (covers the startup gap before any resize event)
        simWidth = w;
        simHeight = h;

        // Scale the background once per size change instead of every frame
        if (background != null && w > 0 && h > 0) {
            if (scaledBg == null || scaledBg.getWidth() != w || scaledBg.getHeight() != h) {
                BufferedImage s = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D bg = s.createGraphics();
                bg.drawImage(background, 0, 0, w, h, null);
                bg.dispose();
                scaledBg = s;
            }
            g2.drawImage(scaledBg, 0, 0, null);
        }

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (Zone zone : environmentView) {
            zone.draw(g2);
        }


        for (Grass grass : grassView) {
            grass.draw(g2);
        }

        List<Animal> animalsNow = animalsView;
        for (Animal animal : animalsNow) {
            animal.draw(g2);
        }
        EcosystemDashboard.draw(g2, animalsNow);
    }

    public void createVolcano(int spawnX, int spawnY, int radius, int lavaRad) {
        commands.add(() -> environment.add(new Volcano(spawnX, spawnY, radius, lavaRad)));
    }

    public void createCave(int spawnX, int spawnY) {
        commands.add(() -> environment.add(new Cave(spawnX, spawnY)));
    }

    public void createDinosaur(int spawnX, int spawnY) {
        commands.add(() -> animals.add(new Dinosaur(spawnX, spawnY)));
    }

    public void createCaveman(int spawnX, int spawnY) {
        commands.add(() -> animals.add(new Caveman(spawnX, spawnY)));
    }

    public void createCow(int spawnX, int spawnY) {
        commands.add(() -> animals.add(new Cow(spawnX, spawnY)));
    }

    private void createGrass() {
        int panelWidth = getWidth() > 100 ? getWidth() : 1000;
        int panelHeight = getHeight() > 100 ? getHeight() : 700;

        int x = 30 + rand.nextInt(Math.max(1, panelWidth - 90));
        int y = 60 + rand.nextInt(Math.max(1, panelHeight - 140));

       grassList.add(new Grass(x, y));
    }

    @Override
    public void removeNotify() {
        simExecutor.shutdownNow();
        super.removeNotify();
    }
}
