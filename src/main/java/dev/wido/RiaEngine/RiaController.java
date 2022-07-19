package dev.wido.RiaEngine;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import dev.wido.RiaEngine.events.commands.RenderQueueAdd;
import dev.wido.RiaEngine.utils.SpriteAux;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class RiaController {
    private final RiaEngine riaEngine;

    public void addSprite(String name, Texture texture, int x, int y) {
        var sprite = new Sprite(texture);
        sprite.setPosition(x, y);
        riaEngine.getCommandQueue().post(new RenderQueueAdd(new SpriteAux(sprite, name)));
    }

    public void addSprite(SpriteAux spriteAux) {
        riaEngine.getCommandQueue().post(new RenderQueueAdd(spriteAux));
    }
}