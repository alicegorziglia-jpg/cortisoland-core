package com.giuli.progressivedifficulty;

import java.util.ArrayList;
import java.util.List;

/**
 * A minimal replacement for Bukkit's BukkitScheduler#runTaskLater. Tasks are
 * ticked forward once per server tick via {@link #tick()}, which is wired to
 * ServerTickEvent.Post in {@link ProgressiveDifficultyEvents}.
 */
public class DelayedTaskScheduler {
    private record ScheduledTask(int ticksRemaining, Runnable task) {
        private ScheduledTask tickedDown() {
            return new ScheduledTask(ticksRemaining - 1, task);
        }
    }

    private static final List<ScheduledTask> QUEUE = new ArrayList<>();

    private DelayedTaskScheduler() {
    }

    /** Runs {@code task} after {@code delayTicks} server ticks (20 ticks = 1 second). */
    public static void schedule(int delayTicks, Runnable task) {
        QUEUE.add(new ScheduledTask(Math.max(delayTicks, 1), task));
    }

    public static void tick() {
        if (QUEUE.isEmpty()) {
            return;
        }

        List<ScheduledTask> due = new ArrayList<>();
        List<ScheduledTask> remaining = new ArrayList<>();
        for (ScheduledTask scheduled : QUEUE) {
            ScheduledTask next = scheduled.tickedDown();
            if (next.ticksRemaining() <= 0) {
                due.add(next);
            } else {
                remaining.add(next);
            }
        }

        QUEUE.clear();
        QUEUE.addAll(remaining);

        for (ScheduledTask scheduled : due) {
            try {
                scheduled.task().run();
            } catch (Exception exception) {
                ProgressiveDifficultyMod.LOGGER.error("Error en una tarea programada", exception);
            }
        }
    }
}
