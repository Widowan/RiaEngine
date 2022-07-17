package dev.wido.RiaEngine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import dev.wido.RiaEngine.utils.SpriteAux;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public class Main {
    public static void main(String[] args) {
        val ria = RiaEngine.getOrCreate();
        ria.scheduleOnce(mySystem);
        ria.setupWindow();
    }

    public static final Runnable mySystem =
        () -> Gdx.app.postRunnable(() -> RiaEngine.getOrCreate().addSprite(
            new SpriteAux(
                new Sprite(new Texture("a.png")),
                "auf"
            )
        ));
}