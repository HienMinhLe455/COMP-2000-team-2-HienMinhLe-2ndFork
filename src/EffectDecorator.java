import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

class EffectDecorator extends ZoneDecorator {
    private final Predicate<Animal> target; // which animals this effect applies to
    private final Consumer<Animal> effect; // what it does to them
    private final Color tint; // translucent overlay so the effect is visible

    EffectDecorator(Zone inner, Predicate<Animal> target, Consumer<Animal> effect, Color tint) {
        super(inner);
        this.target = target;
        this.effect = effect;
        this.tint = tint;
    }

    @Override
    public void applyEffect(Animal a) {
        inner.applyEffect(a); // keep whatever the wrapped zone already does
        if (target.test(a)) {
            effect.accept(a);
        }
    }

    @Override
    public void draw(Graphics2D drawing) {
        inner.draw(drawing); // draw the original first, then tint over it
        drawing.setColor(tint);
        drawing.fill(getShape());
    }
}