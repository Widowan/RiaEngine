package dev.wido.RiaEngine;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.utils.Queue;
import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Scheduler;
import dev.wido.RiaEngine.gui.GameWindow;
import lombok.AccessLevel;
import lombok.Getter;
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

    @Getter private final Dominion  ecs        = Dominion.create("Ria");
    @Getter private final EventBus  eventQueue = EventBus.builder()
        // Config files are for the weak (c) Greenrobot, probably
        .logNoSubscriberMessages(false).sendNoSubscriberEvent(false).build();
    @Getter private final EventBus commandQueue = EventBus.builder()
        .logNoSubscriberMessages(false).sendNoSubscriberEvent(false).build();
    @Getter private RiaController controller;

    private static RiaEngine instance = null;
    private boolean setUp = false;
    private final Scheduler scheduler = getEcs().createScheduler();

    @Getter(AccessLevel.PRIVATE)
    private final Queue<Runnable> scheduleOnceSystemsQueue = new Queue<>();
    @Getter(AccessLevel.PRIVATE)
    private final Queue<Runnable> parallelScheduleOnceSystemsQueue = new Queue<>();

    private RiaEngine() {}

    public static RiaEngine get() {
        if (RiaEngine.instance != null)
            return instance;

        val ria = new RiaEngine();
        for (var r : Systems.getSystems()) ria.scheduler.schedule(r);
        RiaEngine.instance = ria;
        ria.controller = new RiaController(ria);
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

    @SuppressWarnings("unused")
    public void schedule(Runnable system) {
        scheduler.schedule(system);
    }

    @SuppressWarnings("unused")
    public void parallelSchedule(Runnable system) {
        scheduler.parallelSchedule(system);
    }

    @SuppressWarnings("unused")
    public void parallelSchedule(Runnable... systems) {
        scheduler.parallelSchedule(systems);
    }

    public void scheduleOnce(Runnable system) {
        synchronized (getScheduleOnceSystemsQueue()) {
            getScheduleOnceSystemsQueue().addLast(system);
        }
    }

    public void parallelScheduleOnce(Runnable system) {
        synchronized (getParallelScheduleOnceSystemsQueue()) {
            getParallelScheduleOnceSystemsQueue().addLast(system);
        }
    }

    public void parallelScheduleOnce(Runnable... systems) {
        synchronized (getParallelScheduleOnceSystemsQueue()) {
            for (var s : systems)
                getParallelScheduleOnceSystemsQueue().addLast(s);
        }
    }

    public void schedulerTick() {
        scheduler.tick();
    }

    //////////////////////////////
    private static class Systems {
        private static List<Runnable> systems;

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

        @SuppressWarnings("unused")
        static Runnable ExecOnceSystem = () -> {
            var ria = RiaEngine.get();
            if (ria.getScheduleOnceSystemsQueue().isEmpty())
                return;

            synchronized (ria.getScheduleOnceSystemsQueue()) {
                // Theoretically I could've used forkAndJoin, but that's just waste of resources
                // even not considering additional checks by Dominion; why does it even exist?
                ria.getScheduleOnceSystemsQueue().forEach(Runnable::run);
                ria.getScheduleOnceSystemsQueue().clear();
            }
        };
        
        @SuppressWarnings("unused")
        static Runnable ParallelExecOnceSystem = () -> {
            var ria = RiaEngine.get();
            if (ria.getParallelScheduleOnceSystemsQueue().size == 0)
                return;

            synchronized (ria.getParallelScheduleOnceSystemsQueue()) {
                Runnable[] arr = new Runnable[ria.getParallelScheduleOnceSystemsQueue().size];

                int idx = 0;
                for (var r : ria.getParallelScheduleOnceSystemsQueue()) {
                    arr[idx] = r;
                    idx += 1;
                }

                ria.scheduler.forkAndJoinAll(arr);
                ria.getParallelScheduleOnceSystemsQueue().clear();
            }
        };
    }
}
