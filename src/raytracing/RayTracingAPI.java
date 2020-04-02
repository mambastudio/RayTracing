/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing;

import bitmap.display.DynamicDisplay;
import raytracing.generic.API;
import raytracing.generic.Primitive;
import raytracing.core.Scene;
import raytracing.primitive.Box;
import raytracing.renderer.ShadingRenderer;

/**
 *
 * @author user
 */
public final class RayTracingAPI extends API<DynamicDisplay, ShadingRenderer>{
    
    public RayTracingAPI()
    {
        scene = new Scene();        
        renderer = null;
        imageWidth = 0; imageHeight = 0;
        
        setPrimitive(new Box());
    }

    @Override
    public void setPrimitive(Primitive primitive) {
        scene.setPrimitive(primitive);
    }

    @Override
    public void setRenderer(ShadingRenderer renderer) {
        this.renderer = renderer;
    }
    
}
