package net.recasino.addon;

import net.recasino.api.animation.RouletteAnimation;
import net.recasino.api.animation.RouletteAnimationContext;
import net.recasino.model.Prize;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public final class DefaultRouletteAnimation implements RouletteAnimation {

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public void play(RouletteAnimationContext context) {
        scheduleFrame(context, context.createInitialStrip(), 0, context.getStepCount());
    }

    private void scheduleFrame(RouletteAnimationContext context, List<Prize> strip, int step, int maxSteps) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!context.isViewerStillPresent()) {
                    cancel();
                    return;
                }

                shiftStrip(strip, step >= maxSteps - 1 ? context.getResult().getPrize() : null, context);
                Prize centerPrize = strip.get(strip.size() / 2);
                context.render(strip, centerPrize, step >= maxSteps - 1);

                if (step < maxSteps - 1) {
                    context.getPlayer().playSound(context.getPlayer().getLocation(), Sound.UI_BUTTON_CLICK, 0.6F, 1.8F - Math.min(step * 0.04F, 0.5F));
                    scheduleFrame(context, strip, step + 1, maxSteps);
                    cancel();
                    return;
                }

                context.getPlayer().playSound(context.getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.9F, 1.15F);
                context.finish();
                cancel();
            }
        }.runTaskLater(context.getPlugin(), step == 0 ? 0L : context.getStepDelay(step - 1, maxSteps));
    }

    private void shiftStrip(List<Prize> strip, Prize finalPrize, RouletteAnimationContext context) {
        for (int i = 0; i < strip.size() - 1; i++) {
            strip.set(i, strip.get(i + 1));
        }
        strip.set(strip.size() - 1, finalPrize == null ? context.nextPreviewPrize() : finalPrize);
    }
}
