/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.coordinate;

import coordinate.generic.AbstractRay;

/**
 *
 * @author user
 */
public class Ray implements AbstractRay<Point3f, Vector3f>
{
    public Point3f o = null;
    public Vector3f d = null;
    
    public Vector3f inv_d = null;    
    private float tMin;
    private float tMax;
    
    public int[] sign;
    
    public static final float EPSILON = 0.01f;// 0.01f;
    
    public Ray() 
    {
        o = new Point3f();
        d = new Vector3f();  
        
        tMin = EPSILON;
        tMax = Float.POSITIVE_INFINITY;        
    }
    
    public final void init()
    {        
        inv_d = new Vector3f(safe_rcp(d.x), safe_rcp(d.y), safe_rcp(d.z));
        sign = new int[3];
        sign[0] = inv_d.x < 0 ? 1 : 0;
        sign[1] = inv_d.y < 0 ? 1 : 0;
        sign[2] = inv_d.z < 0 ? 1 : 0;
    }
    
    public int[] dirIsNeg()
    {
        int[] dirIsNeg = {sign[0], sign[1], sign[2]};
        return dirIsNeg;
    }
    
    @Override
    public final boolean isInside(float t) 
    {
        return (tMin < t) && (t < tMax);
    }
    
    public Vector3f getInvDir()
    {
        return new Vector3f(inv_d);
    }
    
    @Override
    public void set(float ox, float oy, float oz, float dx, float dy, float dz) {
        o = new Point3f(ox, oy, oz);
        d = new Vector3f(dx, dy, dz).normalize();  
        
        tMin = EPSILON;
        tMax = Float.POSITIVE_INFINITY;
        
        init();
    }

    @Override
    public void set(Point3f o, Vector3f d) {
        set(o.x, o.y, o.z, d.x, d.y, d.z);
    }

    @Override
    public Point3f getPoint() {
        Point3f dest = new Point3f();        
        dest.x = o.x + (tMax * d.x);
        dest.y = o.y + (tMax * d.y);
        dest.z = o.z + (tMax * d.z);
        return dest;
    }

    @Override
    public Point3f getPoint(float t) {
        Point3f dest = new Point3f();        
        dest.x = o.x + (t * d.x);
        dest.y = o.y + (t * d.y);
        dest.z = o.z + (t * d.z);
        return dest;
    }

    @Override
    public Vector3f getDirection() {
        return d;
    }
        
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        
        builder.append("Ray: ").append("\n");
        builder.append("         o    ").append(String.format("(%.5f, %.5f, %.5f)", o.x, o.y, o.z)).append("\n");
        builder.append("         d    ").append(String.format("(%.5f, %.5f, %.5f)", d.x, d.y, d.z)).append("\n");
        builder.append("         tMin ").append(String.format("(%.5f)", tMin)).append("\n");
        builder.append("         tMax ").append(String.format("(%.5f)", tMax));
                
        return builder.toString();   
    }

    @Override
    public float getMin() {
        return tMin;
    }

    @Override
    public float getMax() {
        return tMax;
    }
    
    public final void setMax(float t) {
        tMax = t;
    }
    
    @Override
    public Point3f getOrigin() {
        return o.copy();
    }

    @Override
    public Vector3f getInverseDirection() {
        return inv_d.copy();
    }
    
    private float safe_rcp(float x) {
        return x != 0 ? 1.0f / x : Math.copySign(Float.intBitsToFloat(0x7f800000), x);
    }

    @Override
    public AbstractRay<Point3f, Vector3f> copy() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
