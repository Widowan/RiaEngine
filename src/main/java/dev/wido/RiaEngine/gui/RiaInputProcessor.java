package dev.wido.RiaEngine.gui;

import dev.wido.RiaEngine.RiaEngine;
import dev.wido.RiaEngine.events.input.*;
import org.greenrobot.eventbus.EventBus;

class RiaInputProcessor implements com.badlogic.gdx.InputProcessor {
    private final EventBus eq = RiaEngine.get().eventQueue;

    @Override
    public boolean keyDown(int keycode) {
        eq.post(new KeyDown(keycode));
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        eq.post(new KeyUp(keycode));
        return true;
    }

    @Override
    public boolean keyTyped(char character) {
        eq.post(new KeyTyped(character));
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        eq.post(new TouchDown(screenX, screenY, pointer, button));
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        eq.post(new TouchUp(screenX, screenY, pointer, button));
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        eq.post(new TouchDragged(screenX, screenY, pointer));
        return true;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        eq.post(new MouseMoved(screenX, screenY));
        return true;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        eq.post(new Scrolled(amountX, amountY));
        return true;
    }
}
