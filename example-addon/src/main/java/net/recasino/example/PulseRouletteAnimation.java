package net.recasino.example;

import net.recasino.api.animation.RouletteAnimation;
import net.recasino.api.animation.RouletteAnimationContext;
import net.recasino.model.Prize;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public final class PulseRouletteAnimation implements RouletteAnimation {

    @Override
    public String getId() {
        return "example-pulse";
    }

    @Override
    public void play(RouletteAnimationContext context) {
        schedule(context, context.createInitialStrip(), 0, context.getStepCount());
    }

    private void schedule(RouletteAnimationContext context, List<Prize> strip, int step, int maxSteps) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!context.isViewerStillPresent()) {
                    cancel();
                    return;
                }

                shift(strip, step >= maxSteps - 1 ? context.getResult().getPrize() : null, context);
                Prize centerPrize = strip.get(strip.size() / 2);
                context.render(strip, centerPrize, step >= maxSteps - 1);

                float progress = maxSteps <= 1 ? 1.0F : (float) step / (float) (maxSteps - 1);
                float pitch = 2.0F - (progress * 1.2F);
                context.getPlayer().playSound(context.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5F, Math.max(0.7F, pitch));

                if (step >= maxSteps - 1) {
                    context.getPlayer().playSound(context.getPlayer().getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8F, 1.15F);
                    context.finish();
                    cancel();
                    return;
                }

                schedule(context, strip, step + 1, maxSteps);
                cancel();
            }
        }.runTaskLater(context.getPlugin(), step == 0 ? 0L : context.getStepDelay(step - 1, maxSteps));
    }

    private void shift(List<Prize> strip, Prize forcedPrize, RouletteAnimationContext context) {
        for (int i = 0; i < strip.size() - 1; i++) {
            strip.set(i, strip.get(i + 1));
        }
        strip.set(strip.size() - 1, forcedPrize == null ? context.nextPreviewPrize() : forcedPrize);
    }
}
