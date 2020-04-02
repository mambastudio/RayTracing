/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.primitive;

import coordinate.generic.raytrace.AbstractAccelerator;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import raytracing.accel.NullAccelerator;
import raytracing.core.coordinate.BoundingBox;
import raytracing.core.Intersection;
import raytracing.core.coordinate.Point3f;
import raytracing.generic.Primitive;
import raytracing.core.coordinate.Ray;
import raytracing.core.coordinate.Vector3f;

/**
 *
 * @author user
 */
public class Box implements Primitive
{
    private Point3f max = null;
    private Point3f min = null;
    private AbstractAccelerator accelerator = null;
    
    public Box()
    {
        max = new Point3f(1, 1, 1);
        min = new Point3f(-1, -1, -1);      
        
        accelerator = new NullAccelerator();
        accelerator.build(this);
    }
    
    @Override
    public int getCount() {
        return 1;
    }

    @Override
    public BoundingBox getBound(int primID) {
        BoundingBox bound = new BoundingBox();
        bound.include(min);
        bound.include(max);
        return bound;
    }

    @Override
    public Point3f getCentroid(int primID) {
        return getBound(0).getCenter();
    }

    @Override
    public BoundingBox getBound() {
        return getBound(0);
    }

    @Override
    public boolean intersect(Ray r, int primID, Intersection isect)
    {
        //Ray box intersection (fastest intersection out there)
        //It's branchless
        //https://tavianator.com/fast-branchless-raybounding-box-intersections-part-2-nans/
        
        float t1 = (min.get(0) - r.getOrigin().get(0)) * r.getInverseDirection().get(0);
        float t2 = (max.get(0) - r.getOrigin().get(0)) * r.getInverseDirection().get(0);
        
        float tmin = min(t1, t2);
        float tmax = max(t1, t2);
        
        for (int i = 1; i < 3; ++i) 
        {
            t1 = (min.get(i) - r.getOrigin().get(i)) * r.getInverseDirection().get(i);
            t2 = (max.get(i) - r.getOrigin().get(i)) * r.getInverseDirection().get(i);

            tmin = max(tmin, min(min(t1, t2), tmax));
            tmax = min(tmax, max(max(t1, t2), tmin));
        }
        
        boolean hit = tmax > max(tmin, 0.0f);
        
        //http://blog.johnnovak.net/2016/10/22/the-nim-raytracer-project-part-4-calculating-box-normals/
        //I don't like the normal calculation here, has visible artifacts.
        float t = (tmin > r.getMin()) ? tmin : tmax;        
        if(hit)
        {            
            Point3f  c = min.middle(max);
            
            Vector3f p = r.getPoint(t).sub(c);
            Vector3f d = min.sub(max).mul(0.5f);
            double bias = 1.00001;
            
            Vector3f normal = new Vector3f(
                    (int)(p.x/abs(d.x) * bias), 
                    (int)(p.y/abs(d.y) * bias), 
                    (int)(p.z/abs(d.z) * bias)).normalize();
            
            //System.out.println(normal);
            
            isect.p = r.getPoint(t);
            isect.n = normal;
            isect.u = 0;
            isect.v = 0;
            isect.id = 0;
            isect.primitive = this;
            return true;
        }
        return false;
    }
    
    //From pbrt
    public boolean intersect1(Ray r, int primID, Intersection isect) {
        float t0 = Float.MIN_VALUE, t1 = Float.MAX_VALUE;
        Vector3f n = null;
        for (int i = 0; i < 3; ++i) 
        {
            // Update interval for _i_th bounding box slab, page 180           
            float tNear = (min.get(i) - r.o.get(i)) * r.getInvDir().get(i);
            float tFar = (max.get(i) - r.o.get(i)) * r.getInvDir().get(i);

            // Update parametric interval from slab intersection $t$s
            if (tNear > tFar) 
            {
                float swap = tNear;
                tNear = tFar;
                tFar = swap;
            }
            if (tNear > t0)
            {
                t0=tNear; 
                
                if(t0 > r.getMin())
                {
                    n = new Vector3f();
                    n.setIndex(i, 1f);                    
                }
            }
            
            if (tFar < t1)
            {
                t1=tFar;
                
                if(t0 < r.getMin())
                {
                    n = new Vector3f();
                    n.setIndex(i, 1f);                      
                }                
            }
            if (t0 > t1) return false;                 
        }
        
        float t;
        if(t0 > r.getMin())
            t = t0;
        else
            t = t1;
        
        isect.p = r.getPoint(t);
        isect.n = n;
        isect.u = 0;
        isect.v = 0;
        isect.id = 0;
        isect.primitive = this;
        
        return true;        
    }

    @Override
    public boolean intersectP(Ray r, int primID) {
        BoundingBox bound = new BoundingBox();
        bound.include(min);
        bound.include(max);
        
        return bound.intersectP(r);
    }

    @Override
    public boolean intersect(Ray r, Intersection isect) {
        return accelerator.intersect(r, isect);
    }

    @Override
    public boolean intersectP(Ray r) {
        return accelerator.intersectP(r);
    }

    @Override
    public AbstractAccelerator<Ray, Intersection, Primitive, BoundingBox> getAccelerator() {
        return accelerator;
    }

    @Override
    public void buildAccelerator() {
        accelerator.build(this);
    }

    @Override
    public float getArea(int primID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
