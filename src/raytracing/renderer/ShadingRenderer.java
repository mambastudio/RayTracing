/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.renderer;

import bitmap.Color;
import bitmap.display.DynamicDisplay;
import bitmap.image.BitmapRGB;
import static java.lang.Math.min;
import java.util.logging.Level;
import java.util.logging.Logger;
import raytracing.core.coordinate.Camera;
import raytracing.core.Intersection;
import raytracing.core.coordinate.Ray;
import raytracing.generic.Renderer;
import raytracing.core.Scene;
import raytracing.core.coordinate.Point3f;
import thread.model.KernelThread;

/**
 *
 * @author user
 */
public class ShadingRenderer extends KernelThread implements  Renderer<DynamicDisplay> 
{
    DynamicDisplay display;
    Scene scene;
    boolean fast;
    boolean heatmap;
    
    @Override
    public void execute() {
        BitmapRGB tile = null;
        
        if(display != null)
        {            
            for(int i = 3; i >=0; i--)
            {       
                
                chill();
                
                
                int w = display.getImageWidth();
                int h = display.getImageHeight();

                tile = new BitmapRGB(w, h);
               
                int step = 1 << i;
                if(fast && i != 3) return;
                                   
                DummyClass dthread = new DummyClass(tile, 0, 0, w, h, step, i);
                dthread.start();
                dthread.finish();

                if(!dthread.finished) return;      
                
                display.imageFill(tile);                
                fast = false;            
            }
        }    
        pauseKernel();
    }

    @Override
    public boolean prepare(Scene scene, int w, int h) {
        this.scene = scene;
        return true;
    }

    @Override
    public void render(DynamicDisplay display) {
        this.display = display;
        
        this.display.viewportW.addListener((observable, old_value, new_value) -> {
            trigger();
        });
        
        this.display.viewportH.addListener((observable, old_value, new_value) -> {
            trigger();
        });
        
        this.display.translationDepth.addListener((observable, old_value, new_value) -> {
             scene.orientation.translateDistance(scene.getCamera(), new_value.floatValue() * scene.getWorldBound().getExtents().length() * 0.1f);
             trigger();
        });
        
        this.display.translationXY.addListener((observable, old_value, new_value) -> {
            scene.orientation.rotateX(scene.camera, (float) new_value.getX());
            scene.orientation.rotateY(scene.camera, (float) new_value.getY());
            trigger();
        });
        
        startKernel();
    }

    @Override
    public void stop() {
        stopKernel();
    }

    @Override
    public void pause() {
        pauseKernel();
    }

    @Override
    public void resume() {
        resumeKernel();
    }

    @Override
    public void updateDisplay() {
        display.imagePaint();
    }

    @Override
    public void trigger() {
        fast = true;
        resumeKernel();        
    }
    
    //heat map
    Color gradient(float k) {
        Point3f g[] = new Point3f[]{
            new Point3f(0, 0, 1),            //blue
            new Point3f(0, 1, 1),
            new Point3f(0, 0.50196f, 0),     //green
            new Point3f(1, 1, 0),
            new Point3f(1, 0, 0)             //red
        };
        int n = g.length;
        float s = 1.0f / n;

        int i = min(n - 1, (int)(k * n));
        int j = min(n - 1, i + 1);

        float t = (k - i * s) / s;
        Point3f c =  g[i].mul(1.0f - t).addS(g[j].mul(t));

        return new Color(c.x, c.y, c.z);
    }
    
    private Color doSomething(Camera camera, int x, int y, int w, int h)
    {        
        Ray ray = camera.getFastRay(x, y, w, h, new Ray());         
       
        Intersection isect = new Intersection();
        boolean hit = scene.intersect(ray, isect);
                    
        Color col;
        
        if(heatmap)
            col = gradient(Math.min(100, isect.data)/100.f);
        
        else
        {
            if(hit)
            {
                float coeff = Math.abs(ray.d.dot(isect.n));            
                col = Color.WHITE.mul(coeff);
            }
            else
                col = Color.BLACK;
        }
        
        return col;
    }
    
    public void setHeatmap(boolean heatmap)
    {
        this.heatmap = heatmap;
        trigger();
    }

    @Override
    public boolean isRunning() {
        return kernelTerminated();
    }

    public class DummyClass extends Thread
    {
        BitmapRGB tile;      
        
        int i, w, h, step;
        int startX, startY;
        int endX, endY;
        
        boolean finished = false;
        
        public DummyClass(BitmapRGB tile, int startX, int startY, int endX, int endY, int step, int i)
        {
            this.tile = tile;                  
            this.w = tile.getWidth(); this.h = tile.getHeight();
            this.step = step; this.i = i;
            this.startX = startX; this.startY = startY;
            this.endX = endX; this.endY = endY;
        }
        
        @Override
        public void run()
        {
            Camera camera = scene.camera.copy();
            
            for(int y = startY; y < endY; y+=step)
                for(int x = startX; x < endX; x+=step)
                {
                    
                    chill();
                    if(fast && i != 3) return;
                    
                    int wi = x + step >= endX ? endX - x : step;
                    int hi = y + step >= endY ? endY - y : step;
                    
                    Color col = doSomething(camera, x, y, w, h);
                    
                    tile.writeColor(col, 1, x, y, wi, hi);                    
                }
            finished = true;
        }
        
        public void finish()
        {
            try {
                join();
            } catch (InterruptedException ex) {
                Logger.getLogger(ShadingRenderer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
