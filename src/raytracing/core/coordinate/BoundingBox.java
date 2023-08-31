/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.coordinate;

import coordinate.memory.nativememory.NativeObject.Element;
import coordinate.shapes.AlignedBBoxShape;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.nio.ByteBuffer;

/**
 *
 * @author user
 */
public class BoundingBox extends AlignedBBoxShape<Point3f, Vector3f, Ray, BoundingBox> implements Element<BoundingBox>{
    
    public BoundingBox() 
    {
        super(
            new Point3f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
            new Point3f(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)
        );
    }
    
    public BoundingBox(Point3f p) 
    {
        super(
            new Point3f(p),
            new Point3f(p)
        );
    }
    
    public BoundingBox(Point3f p1, Point3f p2) 
    {
        super(
            new Point3f(//
                min(p1.x, p2.x), min(p1.y, p2.y), min(p1.z, p2.z)),
            new Point3f(//
                max(p1.x, p2.x), max(p1.y, p2.y), max(p1.z, p2.z))
        );
    }
    
    public BoundingBox(float x1, float y1, float z1, float x2, float y2, float z2) {
        super(
            new Point3f(//
                min(x1, x2), min(y1, y2), min(z1, z2)),
            new Point3f(//
                max(x1, x2), max(y1, y2), max(z1, z2))
        );
    }
    
    @Override
    public BoundingBox include(Point3f p) {
        if (p != null) {
            if (p.x < minimum.x)
                minimum.x = p.x;
            if (p.x > maximum.x)
                maximum.x = p.x;
            if (p.y < minimum.y)
                minimum.y = p.y;
            if (p.y > maximum.y)
                maximum.y = p.y;
            if (p.z < minimum.z)
                minimum.z = p.z;
            if (p.z > maximum.z)
                maximum.z = p.z;
        }
        return this;
    }
    
    @Override
    public Point3f getCenter() {
        Point3f dest = new Point3f();
        dest.x = 0.5f * (minimum.x + maximum.x);
        dest.y = 0.5f * (minimum.y + maximum.y);
        dest.z = 0.5f * (minimum.z + maximum.z);
        return dest;
    }
    
    public final void enlargeUlps() {
        final float eps = 0.001f;
        minimum.x -= Math.max(eps, Math.ulp(minimum.x));
        minimum.y -= Math.max(eps, Math.ulp(minimum.y));
        minimum.z -= Math.max(eps, Math.ulp(minimum.z));
        maximum.x += Math.max(eps, Math.ulp(maximum.x));
        maximum.y += Math.max(eps, Math.ulp(maximum.y));
        maximum.z += Math.max(eps, Math.ulp(maximum.z));
    }

    @Override
    public float getCenter(int dim) {
        return getCenter().get(dim);
    }
    
    public float getArea()
    {
        Vector3f w = this.getExtents();
        float ax = Math.max(w.x, 0);
        float ay = Math.max(w.y, 0);
        float az = Math.max(w.z, 0);
        return 2 * (ax * ay + ay * az + az * ax);
    }
    
    public Vector3f offset(Point3f p)
    {
        Vector3f o = p.sub(minimum);
        if(maximum.x > minimum.x) o.x /= maximum.x - minimum.x;
        if(maximum.y > minimum.y) o.y /= maximum.y - minimum.y;
        if(maximum.z > minimum.z) o.z /= maximum.z - minimum.z;
        return o;
    }

    @Override
    public Point3f getMinimum() {
        return minimum;
    }

    @Override
    public Point3f getMaximum() {
        return maximum;
    }

    @Override
    public BoundingBox getInstance() {
        return new BoundingBox();
    }
    
    @Override
    public final String toString() {
        return String.format("(%.2f, %.2f, %.2f) to (%.2f, %.2f, %.2f)", minimum.x, minimum.y, minimum.z, maximum.x, maximum.y, maximum.z);
    } 
    
    @Override
    public boolean intersectP(Ray ray)
    {
        float tmin = (get(ray.sign[0]).x - ray.getOrigin().x) * ray.getInverseDirection().x;
        float tmax = (get(1-ray.sign[0]).x - ray.getOrigin().x) * ray.getInverseDirection().x;
        float tymin = (get(ray.sign[1]).y - ray.getOrigin().y) * ray.getInverseDirection().y;
        float tymax = (get(1-ray.sign[1]).y - ray.getOrigin().y) * ray.getInverseDirection().y;
        if ( (tmin > tymax) || (tymin > tmax) )
            return false;
        if (tymin > tmin)
            tmin = tymin;
        if (tymax < tmax)
            tmax = tymax;
        float tzmin = (get(ray.sign[2]).z - ray.getOrigin().z) * ray.getInverseDirection().z;
        float tzmax = (get(1-ray.sign[2]).z - ray.getOrigin().z) * ray.getInverseDirection().z;
        if ( (tmin > tzmax) || (tzmin > tmax) )
            return false;
        if (tzmin > tmin)
            tmin = tzmin;
        if (tzmax < tmax)
            tmax = tzmax;
        return ( (tmin < ray.getMax()) && (tmax > ray.getMin()));
    }
    
    public boolean intersectP(Ray ray, float[] t)
    {
        float tmin = (get(ray.sign[0]).x - ray.getOrigin().x) * ray.getInverseDirection().x;
        float tmax = (get(1-ray.sign[0]).x - ray.getOrigin().x) * ray.getInverseDirection().x;
        float tymin = (get(ray.sign[1]).y - ray.getOrigin().y) * ray.getInverseDirection().y;
        float tymax = (get(1-ray.sign[1]).y - ray.getOrigin().y) * ray.getInverseDirection().y;
        if ( (tmin > tymax) || (tymin > tmax) )
            return false;
        if (tymin > tmin)
            tmin = tymin;
        if (tymax < tmax)
            tmax = tymax;
        float tzmin = (get(ray.sign[2]).z - ray.getOrigin().z) * ray.getInverseDirection().z;
        float tzmax = (get(1-ray.sign[2]).z - ray.getOrigin().z) * ray.getInverseDirection().z;
        if ( (tmin > tzmax) || (tzmin > tmax) )
            return false;
        if (tzmin > tmin)
            tmin = tzmin;
        if (tzmax < tmax)
            tmax = tzmax;
        if( (tmin < ray.getMax()) && (tmax > ray.getMin()))
        {
            t[0] = tmin;
            t[1] = tmax;
            return true;
        }
        return false;
    }
    
    public Vector3f extents()
    {
        return maximum.sub(minimum);
    }
    
    public final boolean intersects(BoundingBox b) {
        return ((b != null) && (minimum.x <= b.maximum.x) && (maximum.x >= b.minimum.x) && (minimum.y <= b.maximum.y) && (maximum.y >= b.minimum.y) && (minimum.z <= b.maximum.z) && (maximum.z >= b.minimum.z));
    }
    
    @Override
    public BoundingBox copy() 
    {        
        return new BoundingBox(minimum, maximum);
    }

    @Override
    public int sizeOf() {
        return 24;
    }

    @Override
    public byte[] getBytes() {
        ByteBuffer buf = ByteBuffer.allocate(sizeOf());
        buf.putFloat(minimum.x);
        buf.putFloat(minimum.y);
        buf.putFloat(minimum.z);
        buf.putFloat(maximum.x);
        buf.putFloat(maximum.y);
        buf.putFloat(maximum.z);
        return buf.array();
    }

    @Override
    public void putBytes(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        minimum.x = buf.getFloat();
        minimum.y = buf.getFloat();
        minimum.z = buf.getFloat();
        maximum.x = buf.getFloat();
        maximum.y = buf.getFloat();
        maximum.z = buf.getFloat();
    }

    @Override
    public BoundingBox newInstance() {
        return new BoundingBox();
    }
}
