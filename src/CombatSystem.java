import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CombatSystem {

    public static void handleCollision(Animal a, Animal b, List<Animal> animals) {
        if (a instanceof Dinosaur && b instanceof Caveman) {
            dinoCavemanCombat((Dinosaur) a, (Caveman) b, animals);
        } else if (a instanceof Caveman && b instanceof Dinosaur) {
            dinoCavemanCombat((Dinosaur) b, (Caveman) a, animals);
        } else if (a instanceof Dinosaur && b instanceof Dinosaur) {
            dinoDinoCombat((Dinosaur) a, (Dinosaur) b);
        } else if (a instanceof Caveman && b instanceof Cow) {
            cavemanCowCombat((Caveman) a, (Cow) b);
        } else if (a instanceof Cow && b instanceof Caveman) {
            cavemanCowCombat((Caveman) b, (Cow) a);
        }
    }

    private static void dinoCavemanCombat(Dinosaur dino, Caveman caveman, List<Animal> animals) {
        if (dino.isHungry
             && dino.attackCooldown == 0 && caveman.attackCooldown == 0) {
            List<Caveman> fighters = new ArrayList<>();
            fighters.add(caveman);

            // dino attack first
            caveman.takeDamage(dino.damage);
            dino.attackCooldown = 30;
            caveman.attackCooldown = 30;
            ally.attackCooldown = 30;

            // Caveman counter attack
            if (!caveman.isDead()) {
                dino.takeDamage(caveman.damage);

                // Find ally Caveman in Alert Radius
                List<Caveman> nearbyAllies = animals.parallelStream()
                    .filter(other -> other instanceof Caveman && other != caveman && !other.isDead())
                    .map(other -> (Caveman) other)
                    .filter(ally -> caveman.getDistanceTo(ally) <= Caveman.ALERT_RADIUS)
                    .collect(Collectors.toList());

                fighters.addAll(nearbyAllies);

                for (Caveman ally : nearbyAllies) {
                    dino.takeDamage(ally.damage);
                    MovementStrategy.moveToward(ally, dino.x, dino.y);
                }
            }

            // If caveman die -> dino eat
            if (caveman.isDead()) {
                dino.hunger = Math.min(dino.maxHunger, dino.hunger + caveman.foodValue);
                dino.isHungry = false;
                dino.starvationTimer = dino.defaultStarvationTime;
                caveman.setState(new DeadState());
            }

            // If dino die -> cavemen eat
            if (dino.isDead()) {
                for (Caveman fighter : fighters) {
                    if (!fighter.isDead()) {
                        fighter.hunger = Math.min(fighter.maxHunger, fighter.hunger + dino.foodValue);
                        fighter.isHungry = false;
                        fighter.starvationTimer = fighter.defaultStarvationTime;
                    }
                }
                dino.setState(new DeadState());
            }
        }
    }

    private static void cavemanCowCombat(Caveman caveman, Cow cow) {
        if (caveman.isHungry && caveman.attackCooldown == 0) {
            // Caveman hits the cow (cows have 0 damage, no counter attack)
            cow.takeDamage(caveman.damage);
            caveman.attackCooldown = 30;

            if (cow.isDead()) {
                caveman.hunger = Math.min(caveman.maxHunger, caveman.hunger + cow.foodValue);
                caveman.isHungry = false;
                caveman.starvationTimer = caveman.defaultStarvationTime;
                cow.setState(new DeadState());
            }
        }
    }

    private static void dinoDinoCombat(Dinosaur d1, Dinosaur d2) {
        if (d1.isHungry && d1.attackCooldown == 0 && d2.attackCooldown == 0) {
            executeDinoFight(d1, d2);
        } else if (d2.isHungry && d1.attackCooldown == 0 && d2.attackCooldown == 0) {
            executeDinoFight(d2, d1);
        }
    }

    private static void executeDinoFight(Dinosaur attacker, Dinosaur defender) {
        defender.takeDamage(attacker.damage);
        attacker.attackCooldown = 30;
        defender.attackCooldown = 30;

        if (!defender.isDead()) {
            attacker.takeDamage(defender.damage);
        }

        if (defender.isDead()) {
            attacker.hunger = Math.min(attacker.maxHunger, attacker.hunger + defender.foodValue);
            attacker.isHungry = false;
            attacker.starvationTimer = attacker.defaultStarvationTime;
            defender.setState(new DeadState());
        } else if (attacker.isDead()) {
            defender.hunger = Math.min(defender.maxHunger, defender.hunger + attacker.foodValue);
            defender.isHungry = false;
            defender.starvationTimer = defender.defaultStarvationTime;
            attacker.setState(new DeadState());
        }
    }
}