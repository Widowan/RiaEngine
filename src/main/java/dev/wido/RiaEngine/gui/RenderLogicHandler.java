package dev.wido.RiaEngine.gui;

import dev.wido.RiaEngine.RiaEngine;
import dev.wido.RiaEngine.events.commands.RenderQueueAdd;
import dev.wido.RiaEngine.events.commands.RenderQueueRemove;
import lombok.extern.slf4j.Slf4j;
import org.greenrobot.eventbus.Subscribe;

/**
 * For internal use only
 */
@Slf4j
public final class RenderLogicHandler {
    final private GameWindow game;

    RenderLogicHandler(GameWindow game) {
        this.game = game;
        RiaEngine.getOrCreate().getCommandQueue().register(this);
    }

    @Subscribe
    public void onRenderQueueAdd(RenderQueueAdd e) {
        game.renderQueue.addLast(e.sprite());
    }

    @Subscribe
    public void onRenderQueueRemove(RenderQueueRemove e) {
        int[] indices = new int[game.renderQueue.size];
        int lastToRemoveIdx = 0, totalIdx = 0;

        // Should be faster than .get on every iteration
        for (var s : game.renderQueue) {
            if (s.name().equals(e.name())) {
                indices[lastToRemoveIdx] = totalIdx;
                lastToRemoveIdx += 1;
            }

            totalIdx += 1;
        }

        for (int i = 0; i < lastToRemoveIdx; i++)
            game.renderQueue.removeIndex(indices[i]);
    }
}
