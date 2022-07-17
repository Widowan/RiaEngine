package dev.wido.RiaEngine;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.utils.Queue;
import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Scheduler;
import dev.wido.RiaEngine.gui.GameWindow;
import dev.wido.RiaEngine.events.commands.RenderQueueAdd;
import dev.wido.RiaEngine.utils.SpriteAux;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;
import java.util.List;

@Slf4j
public final class RiaEngine {
    static {
        System.setProperty("dominion.show-banner", "false");
    }

    @Getter private final Dominion  ecs        = Dominion.create("Ria");
    @Getter private final EventBus  eventQueue = EventBus.builder()
            // Config files are for the weak (c) Greenrobot, probably
            .logNoSubscriberMessages(false).sendNoSubscriberEvent(false).build();
    @Getter private final EventBus  commandQueue = EventBus.builder()
            .logNoSubscriberMessages(false).sendNoSubscriberEvent(false).build();

    private final Scheduler scheduler = ecs.createScheduler();
    static RiaEngine instance = null;
    private boolean setUp = false;

    final Queue<Runnable> scheduleOnceSystemsQueue = new Queue<>();
    final Queue<Runnable> parallelScheduleOnceSystemsQueue = new Queue<>();

    private RiaEngine() {}

    public static RiaEngine getOrCreate() {
        if (RiaEngine.instance != null)
            return instance;

        val ria = new RiaEngine();
        for (var r : Systems.getSystems()) ria.scheduler.schedule(r);
//        ria.scheduler.schedule(Systems.IoSystem);
//        ria.scheduler.schedule(Systems.ScriptSystem);
//        ria.scheduler.schedule(Systems.RenderSystem);
        RiaEngine.instance = ria;
        return ria;
    }

    public void setupWindow() {
        var cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setIdleFPS(0);
        cfg.setTitle("Hi");
        cfg.setWindowedMode(600, 480);
        cfg.setForegroundFPS(12);
        setupWindow(cfg);
    }

    public void setupWindow(Lwjgl3ApplicationConfiguration cfg) {
        if (setUp)
            // TODO: Change class
            throw new RuntimeException("Window is already set up");

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

    public void addSprite(String name, Texture texture, int x, int y) {
        var sprite = new Sprite(texture);
        sprite.setPosition(x, y);
        commandQueue.post(new RenderQueueAdd(new SpriteAux(sprite, name)));
    }

    public void addSprite(SpriteAux spriteAux) {
        commandQueue.post(new RenderQueueAdd(spriteAux));
    }

    //////////////////////////////
    private static class Systems {
        private static List<Runnable> systems = null;

        private Systems() {}

        static List<Runnable> getSystems() {
            if (systems != null)
                return systems;

            var r = Arrays.stream(Systems.class.getDeclaredFields())
                .filter(f -> f.getType() == Runnable.class)
                // I want lift so bad, checked exceptions are horrible
                .map(f -> {
                    try {
                        //noinspection InstantiationOfUtilityClass
                        return (Runnable)f.get(new Systems());
                    } catch (Exception e) {
                        // TODO: Change class
                        throw new RuntimeException(e);
                    }
                })
                .toList();
            systems = r;
            return r;
        }

        static Runnable ExecOnceSystem = () -> {
            var ria = RiaEngine.getOrCreate();
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
            var ria = RiaEngine.getOrCreate();
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
