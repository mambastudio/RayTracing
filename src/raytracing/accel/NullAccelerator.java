/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.accel;

import coordinate.generic.raytrace.AbstractAccelerator;
import raytracing.generic.Primitive;
import raytracing.core.coordinate.BoundingBox;
import raytracing.core.Intersection;
import raytracing.core.coordinate.Ray;

/**
 *
 * @author user
 */
public class NullAccelerator implements AbstractAccelerator<Ray, Intersection, Primitive, BoundingBox>  
{
    Primitive primitives = null;
    BoundingBox bound = null;
    
    @Override
    public void build(Primitive primitives) {
        this.primitives = primitives;
        this.bound = new BoundingBox();
        this.bound.include(primitives.getBound());
    }

    @Override
    public boolean intersect(Ray ray, Intersection isect) {
        boolean hit = false;
        for(int i = 0; i<primitives.getCount(); i++)
            if(primitives.getBound(i).intersectP(ray))
                if(primitives.intersect(ray, i, isect))
                    hit |= true;
        return hit;
    }

    @Override
    public boolean intersectP(Ray ray) {
        boolean hit = false;
        for(int i = 0; i<primitives.getCount(); i++)
            if(primitives.intersectP(ray, i))
                hit |= true;
        return hit;
    }

    @Override
    public void intersect(Ray[] rays, Intersection[] isects) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public BoundingBox getBound() {
        return bound;
    }
    
}
