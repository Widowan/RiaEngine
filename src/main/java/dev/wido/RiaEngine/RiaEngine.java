package dev.wido.RiaEngine;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.utils.Queue;
import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Scheduler;
import dev.wido.RiaEngine.gui.GameWindow;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static dev.wido.RiaEngine.utils.Utils.checkedLift;

@Slf4j
public final class RiaEngine {
    static {
        System.setProperty("dominion.show-banner", "false");
    }

    public final Dominion  ecs        = Dominion.create("Ria");
    public final EventBus  eventQueue = EventBus.builder()
        // Config files are for the weak (c) Greenrobot, probably
        .logNoSubscriberMessages(false).sendNoSubscriberEvent(false).build();
    public final EventBus commandQueue = EventBus.builder()
        .logNoSubscriberMessages(false).sendNoSubscriberEvent(false).build();
    public final RiaController controller = new RiaController(this);

    private final Scheduler scheduler = ecs.createScheduler();

    private boolean setUp = false;

    static RiaEngine instance = null;

    final Queue<Runnable> scheduleOnceSystemsQueue = new Queue<>();
    final Queue<Runnable> parallelScheduleOnceSystemsQueue = new Queue<>();

    private RiaEngine() {}

    public static RiaEngine get() {
        if (RiaEngine.instance != null)
            return instance;

        val ria = new RiaEngine();
        for (var r : Systems.getSystems()) ria.scheduler.schedule(r);
        RiaEngine.instance = ria;
        return ria;
    }

    public void setupWindow() {
        var cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setIdleFPS(0);
        cfg.setTitle("RiaEngine game");
        cfg.setWindowedMode(640, 480);
        cfg.setForegroundFPS(12);
        setupWindow(cfg);
    }

    public void setupWindow(Lwjgl3ApplicationConfiguration cfg) {
        if (setUp)
            throw new IllegalStateException("Window is already set up");

        var game = new GameWindow();
        setUp = true;
        new Lwjgl3Application(game, cfg);
    }

    public void schedule(Runnable system) {
        scheduler.schedule(system);
    }

    public void parallelSchedule(Runnable system) {
        scheduler.parallelSchedule(system);
    }

    public void parallelSchedule(Runnable... systems) {
        scheduler.parallelSchedule(systems);
    }

    public void scheduleOnce(Runnable system) {
        synchronized (scheduleOnceSystemsQueue) {
            this.scheduleOnceSystemsQueue.addLast(system);
        }
    }

    public void parallelScheduleOnce(Runnable system) {
        synchronized (parallelScheduleOnceSystemsQueue) {
            this.parallelScheduleOnceSystemsQueue.addLast(system);
        }
    }

    public void parallelScheduleOnce(Runnable... systems) {
        synchronized (parallelScheduleOnceSystemsQueue) {
            for (var s : systems)
                this.parallelScheduleOnceSystemsQueue.addLast(s);
        }
    }

    public void schedulerTick() {
        scheduler.tick();
    }

    //////////////////////////////
    private static class Systems {
        private static List<Runnable> systems = null;

        private Systems() {}

        static List<Runnable> getSystems() {
            if (systems != null)
                return systems;

            //noinspection InstantiationOfUtilityClass
            systems = Arrays.stream(Systems.class.getDeclaredFields())
                .filter(f -> f.getType() == Runnable.class)
                .map(f -> checkedLift(() -> (Runnable)f.get(new Systems())))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
            return systems;
        }

        static Runnable ExecOnceSystem = () -> {
            var ria = RiaEngine.get();
            if (ria.scheduleOnceSystemsQueue.size == 0)
                return;

            synchronized (ria.scheduleOnceSystemsQueue) {
                // Theoretically I could've used forkAndJoin, but that's just waste of resources
                // even not considering additional checks by Dominion; why does it even exist?
                ria.scheduleOnceSystemsQueue.forEach(Runnable::run);
                ria.scheduleOnceSystemsQueue.clear();
            }
        };
        static Runnable ParallelExecOnceSystem = () -> {
            var ria = RiaEngine.get();
            if (ria.parallelScheduleOnceSystemsQueue.size == 0)
                return;

            synchronized (ria.parallelScheduleOnceSystemsQueue) {
                Runnable[] arr = new Runnable[ria.parallelScheduleOnceSystemsQueue.size];

                int idx = 0;
                for (var r : ria.parallelScheduleOnceSystemsQueue) {
                    arr[idx] = r;
                    idx += 1;
                }

                ria.scheduler.forkAndJoinAll(arr);
                ria.parallelScheduleOnceSystemsQueue.clear();
            }
        };
    }
}
