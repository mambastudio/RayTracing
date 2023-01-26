/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import coordinate.shapes.TriangleShape;
import raytracing.core.coordinate.Point3f;
import raytracing.core.coordinate.Ray;
import raytracing.core.coordinate.Vector3f;

/**
 *
 * @author user
 */
public class TriSimple extends TriangleShape<Point3f, Vector3f, Ray>  {
    
    protected TriSimple(Point3f p1, Point3f p2, Point3f p3)
    {
        super(p1, p2, p3);        
    }

    @Override
    public Vector3f e1() {
        return Point3f.sub(p1, p2);
    }

    @Override
    public Vector3f e2() {
        return Point3f.sub(p3, p1);
    }
    
    public Point3f v0()
    {
        return p1;
    }
    
    public float nx()
    {
        return n.x;
    }
    
    public float ny()
    {
        return n.y;
    }
    
    public float nz()
    {
        return n.z;
    }
    
    /// Packs the normal components into a float3 structure.
    public Vector3f normal() { return new Vector3f(nx(), ny(), nz()); }
}
