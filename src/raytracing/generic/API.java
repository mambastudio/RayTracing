/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.generic;

import bitmap.core.AbstractDisplay;
import raytracing.core.coordinate.BoundingBox;
import raytracing.core.coordinate.Camera;
import raytracing.core.Scene;

/**
 *
 * @author user
 * @param <D>
 * @param <R>
 */
public abstract class API <D extends AbstractDisplay, R  extends Renderer<D>>
{
    protected Scene scene;
    protected R renderer;
    
    protected int imageWidth, imageHeight;
    
    public abstract void setPrimitive(Primitive primitive);     
    public abstract void setRenderer(R renderer);
    public abstract R getRenderer();
    
    public BoundingBox getWorldBound()
    {
        return scene.getWorldBound();
    }
    
    public void setCamera(Camera camera)
    {
        this.scene.setCamera(camera);
    }
    
    public Camera getCamera()
    {
        return scene.getCamera();
    }
        
    public void setImageSize(int width, int height)
    {
        this.imageWidth = width;
        this.imageHeight = height;
    }
    
    public void reposition()
    {
        scene.reposition();
    }
    
    public void triggerRender()
    {
        renderer.trigger();
    }
    
    public boolean render(D display)
    {
        if(renderer == null) 
        {
            System.out.println("renderer null");
            return false;
        }
        else if(renderer.isRunning())
        {
            System.out.println("still probably running or paused or about to finish a task intensive process");
            return false;
        }
        
        this.renderer.prepare(scene, imageWidth, imageHeight);
        this.renderer.render(display);
                      
        return true;
    }
}
