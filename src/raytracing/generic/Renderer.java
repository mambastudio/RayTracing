/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.generic;

import bitmap.core.AbstractDisplay;
import raytracing.core.Scene;

/**
 *
 * @author user
 * @param <T>
 */
public interface Renderer<T extends AbstractDisplay> {
    public boolean prepare(Scene scene, int w, int h);
    public void render(T display);
    public void stop();
    public void pause();
    public void resume();
    public void updateDisplay();
    public void trigger();
    public boolean isRunning();
}
