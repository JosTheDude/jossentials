package gg.jos.jossentials.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public final class SchedulerAdapter {
    public interface TaskHandle {
        void cancel();
    }

    private static final TaskHandle NOOP = () -> {
    };

    private final JavaPlugin plugin;
    private final boolean folia;
    private final Object globalScheduler;
    private final Method globalRun;
    private final Method globalRunDelayed;
    private final Method globalRunAtFixedRate;
    private final Method entityGetScheduler;

    public SchedulerAdapter(JavaPlugin plugin) {
        this.plugin = plugin;
        Object global = null;
        Method globalRunMethod = null;
        Method globalRunDelayedMethod = null;
        Method globalRunAtFixedRateMethod = null;
        Method entityGetSchedulerMethod = null;
        boolean foliaDetected = false;

        boolean globalDetected = false;
        boolean entityDetected = false;

        try {
            Method getGlobalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler");
            global = getGlobalScheduler.invoke(null);
            globalRunMethod = global.getClass().getMethod("run", JavaPlugin.class, Consumer.class);
            globalRunDelayedMethod = global.getClass().getMethod("runDelayed", JavaPlugin.class, Consumer.class, long.class);
            globalRunAtFixedRateMethod = global.getClass().getMethod("runAtFixedRate", JavaPlugin.class, Consumer.class, long.class, long.class);
            globalDetected = true;
        } catch (Exception ignored) {
        }

        try {
            entityGetSchedulerMethod = Player.class.getMethod("getScheduler");
            entityDetected = true;
        } catch (Exception ignored) {
        }

        foliaDetected = globalDetected || entityDetected;

        this.folia = foliaDetected;
        this.globalScheduler = global;
        this.globalRun = globalRunMethod;
        this.globalRunDelayed = globalRunDelayedMethod;
        this.globalRunAtFixedRate = globalRunAtFixedRateMethod;
        this.entityGetScheduler = entityGetSchedulerMethod;
    }

    public boolean isFolia() {
        return folia;
    }

    public TaskHandle runGlobal(Runnable task) {
        if (folia && globalScheduler != null && globalRun != null) {
            return wrapFolia(runFolia(globalScheduler, globalRun, task));
        }
        return wrapBukkit(runBukkit(task));
    }

    public TaskHandle runGlobalLater(Runnable task, long delayTicks) {
        if (folia && globalScheduler != null && globalRunDelayed != null) {
            return wrapFolia(runFolia(globalScheduler, globalRunDelayed, task, delayTicks));
        }
        return wrapBukkit(Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks));
    }

    public TaskHandle runGlobalTimer(Runnable task, long delayTicks, long periodTicks) {
        if (folia && globalScheduler != null && globalRunAtFixedRate != null) {
            return wrapFolia(runFolia(globalScheduler, globalRunAtFixedRate, task, delayTicks, periodTicks));
        }
        return wrapBukkit(Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks));
    }

    public TaskHandle runEntity(Player player, Runnable task) {
        if (folia && player != null) {
            Object scheduler = entityScheduler(player);
            if (scheduler != null) {
                Method runMethod = resolveEntityMethod(scheduler, "run", JavaPlugin.class, Consumer.class);
                if (runMethod != null) {
                    return wrapFolia(runFolia(scheduler, runMethod, task));
                }
            }
        }
        if (Bukkit.isPrimaryThread()) {
            task.run();
            return NOOP;
        }
        return wrapBukkit(Bukkit.getScheduler().runTask(plugin, task));
    }

    public TaskHandle runEntityLater(Player player, Runnable task, long delayTicks) {
        if (folia && player != null) {
            Object scheduler = entityScheduler(player);
            if (scheduler != null) {
                Method runMethod = resolveEntityMethod(scheduler, "runDelayed", JavaPlugin.class, Consumer.class, long.class);
                if (runMethod != null) {
                    return wrapFolia(runFolia(scheduler, runMethod, task, delayTicks));
                }
            }
        }
        return wrapBukkit(Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks));
    }

    public TaskHandle runEntityTimer(Player player, Runnable task, long delayTicks, long periodTicks) {
        if (folia && player != null) {
            Object scheduler = entityScheduler(player);
            if (scheduler != null) {
                Method runMethod = resolveEntityMethod(scheduler, "runAtFixedRate", JavaPlugin.class, Consumer.class, long.class, long.class);
                if (runMethod != null) {
                    return wrapFolia(runFolia(scheduler, runMethod, task, delayTicks, periodTicks));
                }
            }
        }
        return wrapBukkit(Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks));
    }

    private TaskHandle wrapBukkit(BukkitTask task) {
        if (task == null) {
            return NOOP;
        }
        return task::cancel;
    }

    private TaskHandle wrapFolia(Object task) {
        if (task == null) {
            return NOOP;
        }
        return () -> {
            try {
                Method cancel = task.getClass().getMethod("cancel");
                cancel.invoke(task);
            } catch (Exception ignored) {
            }
        };
    }

    private Object runFolia(Object scheduler, Method method, Runnable task, Object... args) {
        try {
            Consumer<Object> consumer = ignored -> task.run();
            Object[] params = new Object[args.length + 2];
            params[0] = plugin;
            params[1] = consumer;
            if (args.length > 0) {
                System.arraycopy(args, 0, params, 2, args.length);
            }
            return method.invoke(scheduler, params);
        } catch (Exception ex) {
            plugin.getLogger().warning("failed to schedule folia task, falling back to bukkit scheduler.");
            if (args.length == 0) {
                return runBukkit(task);
            }
            if (args.length == 1) {
                return Bukkit.getScheduler().runTaskLater(plugin, task, (long) args[0]);
            }
            return Bukkit.getScheduler().runTaskTimer(plugin, task, (long) args[0], (long) args[1]);
        }
    }

    private BukkitTask runBukkit(Runnable task) {
        return Bukkit.getScheduler().runTask(plugin, task);
    }

    private Object entityScheduler(Player player) {
        if (entityGetScheduler == null) {
            return null;
        }
        try {
            return entityGetScheduler.invoke(player);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Method resolveEntityMethod(Object scheduler, String name, Class<?>... types) {
        try {
            return scheduler.getClass().getMethod(name, types);
        } catch (Exception ignored) {
            return null;
        }
    }
}
