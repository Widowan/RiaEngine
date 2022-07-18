package dev.wido.RiaEngine.gui;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Queue;
import com.badlogic.gdx.utils.ScreenUtils;
import dev.wido.RiaEngine.RiaEngine;
import dev.wido.RiaEngine.utils.SpriteAux;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class GameWindow extends ApplicationAdapter {
    private RenderLogicHandler logicHandler;
    SpriteBatch batch;
    OrthographicCamera camera;
    Queue<SpriteAux> renderQueue;

    static boolean once = false;

    @Override
    public void render() {
        RiaEngine.get().schedulerTick();
        camera.update();

        ScreenUtils.clear(Color.GRAY);

        batch.begin();
        renderQueue.forEach(d -> batch.draw(
            d.sprite().getTexture(),
            d.sprite().getX(),
            d.sprite().getY()));
        batch.end();
    }

    @Override
    public void create() {
        Gdx.input.setInputProcessor(new RiaInputProcessor());

        if (camera == null) {
            camera = new OrthographicCamera();
            camera.setToOrtho(false,
                Lwjgl3ApplicationConfiguration.getDisplayMode().width,
                Lwjgl3ApplicationConfiguration.getDisplayMode().height
            );
        }

        if (batch == null) {
            batch = new SpriteBatch();
            batch.setProjectionMatrix(camera.combined);
        }

        if (renderQueue == null) {
            renderQueue = new Queue<>();
        }

        if (logicHandler == null) {
            logicHandler = new RenderLogicHandler(this);
        }
    }
}
