/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.accel;

import coordinate.generic.raytrace.AbstractAccelerator;
import raytracing.core.Intersection;
import raytracing.core.coordinate.BoundingBox;
import raytracing.core.coordinate.Point3f;
import raytracing.core.coordinate.Point3i;
import raytracing.core.coordinate.Ray;
import raytracing.generic.Primitive;

/**
 *
 * @author user
 */
public class BVHPlocTree implements AbstractAccelerator<Ray, Intersection, Primitive, BoundingBox> {
    
     //Primitive
    Primitive primitives;
    
    
    BVHNode[] nodes = null;
    BoundingBox bound = null;
    int[] nearest = null;
    int r = 3;
    
    @Override
    public void build(Primitive primitives) {
        this.primitives = primitives;
    }

    @Override
    public boolean intersect(Ray ray, Intersection isect) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean intersectP(Ray ray) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void intersect(Ray[] rays, Intersection[] isects) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public BoundingBox getBound() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public float distance(BVHNode node1, BVHNode node2)
    {
        BoundingBox b = new BoundingBox();
        b.include(node1.bounds, node2.bounds);
        return b.getArea();
    }
    
    public int leftShift3(int x)
    {
        if (x == (1 << 10)) --x;
        x = (x | (x << 16)) & 0b00000011000000000000000011111111;
        x = (x | (x <<  8)) & 0b00000011000000001111000000001111;
        x = (x | (x <<  4)) & 0b00000011000011000011000011000011;
        x = (x | (x <<  2)) & 0b00001001001001001001001001001001;
        return x;
    }

    public int encodeMortonInt(Point3i scaledValue)
    {
        return (leftShift3(scaledValue.x) << 2) | (leftShift3(scaledValue.y) << 1) | leftShift3(scaledValue.z);
    }

    public int encodeMorton(Point3f centroid)
    {
        int mortonBits = 10;
        int mortonScale = 1 << mortonBits;  //morton scale to 10 bits maximum, this will enable
                                            //to left shift three 10 bits values into a 32 bit.
        Point3i scaledValue = new Point3i(centroid.mul(mortonScale));
        return encodeMortonInt(scaledValue);
    }
    
    public static class BVHNode
    {
        public BoundingBox bounds;
        public boolean isLeaf;        
        public int parent, sibling, left, right, child;  
        public int mortonCode;
                
        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder(); 
            builder.append("bounds   ").append(bounds).append("\n");
            builder.append("is leaf  ").append(isLeaf).append("\n");
            builder.append("parent   ").append(parent).append("\n");
            builder.append("sibling  ").append(sibling).append("\n");
            builder.append("left     ").append(left).append(" right     ").append(right).append("\n");
            builder.append("child no ").append(child).append("\n");
            builder.append("\n");
            return builder.toString();
        }
    }
}
