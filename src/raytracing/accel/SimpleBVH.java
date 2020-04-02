/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.accel;

import coordinate.generic.raytrace.AbstractAccelerator;
import java.util.concurrent.TimeUnit;
import raytracing.core.coordinate.BoundingBox;
import raytracing.core.Intersection;
import raytracing.generic.Primitive;
import raytracing.core.coordinate.Ray;

/**
 *
 * @author user
 */
public class SimpleBVH implements AbstractAccelerator<Ray, Intersection, Primitive, BoundingBox> {

    //Primitive
    Primitive primitives;
    
    //Tree, Primitive index, Boundingbox
    int[] objects;
    BVHNode[] nodes = null;
    BoundingBox bound = null;
    
    //node counter
    int nodesPtr = 0;
    
    @Override
    public void build(Primitive primitives) {
        long time1 = System.nanoTime();
        
        this.primitives = primitives;  
        objects = new int[this.primitives.getCount()];
        for(int i = 0; i<this.primitives.getCount(); i++)
            objects[i] = i;
        bound = this.primitives.getBound();
        
        //Allocate BVH root node
        nodes = new BVHNode[this.primitives.getCount() * 2 - 1];
        BVHNode root = new BVHNode();
        nodes[0] = root;
        nodesPtr = 1;
        
        subdivide(root, 0, objects.length);
        
        long time2 = System.nanoTime();
        
        long timeDuration = time2 - time1;
        String timeTaken= String.format("BVH build time: %02d min, %02d sec", 
                TimeUnit.NANOSECONDS.toMinutes(timeDuration), 
                TimeUnit.NANOSECONDS.toSeconds(timeDuration));
        System.out.println(timeTaken);
        
        System.out.println(nodes[1].axis+ " " +nodes[2].axis);
    }
    
    private void subdivide(BVHNode parent, int start, int end)
    {
        //Calculate the bounding box for the root node
        BoundingBox bb = new BoundingBox();
        BoundingBox bc = new BoundingBox();
        calculateBounds(start, end, bb, bc);
        parent.bounds = bb;
                
        //Initialize leaf
        if(end - start < 10)
        {            
            parent.start = start;
            parent.end = end;
            parent.isLeaf = true;
            return;
        }
        
        //Subdivide parent node        
        BVHNode left, right;        
        synchronized(this)
        {
            left            = new BVHNode();
            right           = new BVHNode();        
            nodes[nodesPtr] = left;     parent.left     = nodesPtr++;
            nodes[nodesPtr] = right;    parent.right    = nodesPtr++;                
        }   
        
        //set the split dimensions
        int split_dim = bc.maximumExtentAxis();
        parent.axis = split_dim;
        int mid = getMid(bc, split_dim, start, end);
                
        //Subdivide
        subdivide(left, start, mid);
        subdivide(right, mid, end);
    }
    
    private int getMid(BoundingBox bc, int split_dim, int start, int end)
    {
        //split on the center of the longest axis
        float split_coord = bc.getCenter(split_dim);

        //partition the list of objects on this split            
        int mid = partition(primitives, objects, start, end, split_dim, split_coord);

        //if we get a bad split, just choose the center...
        if(mid == start || mid == end)
            mid = start + (end-start)/2;
        
        return mid;
    }
    
    private void calculateBounds(int first, int end, BoundingBox bb, BoundingBox bc)
    {                
        for(int p = first; p<end; p++)
        {
            bb.include(primitives.getBound(objects[p]));
            bc.include(primitives.getBound(objects[p]).getCenter());
        }        
    }

    @Override
    public boolean intersect(Ray r, Intersection isect) {
        int[] todo = new int[64];
        int stackptr = 0;
        boolean hit = false;
        int[] dirIsNeg = r.dirIsNeg();
        
        while(stackptr >= 0)
        {
            int ni = todo[stackptr];
            stackptr--;    
            BVHNode node = nodes[ni];            
            if(node.bounds.intersectP(r))
            {     
                System.out.println(ni);
                if(node.isLeaf)
                {                       
                    for(int i = 0; i<(node.end-node.start); i++)                       
                        if(primitives.intersect(r, objects[node.start+i], isect))
                            hit = true;
                }
                else
                {                       
                    if (dirIsNeg[node.axis] == 1) 
                    {   
                        todo[++stackptr] = node.right;
                        todo[++stackptr] = node.left;
                    }
                    else
                    {
                        todo[++stackptr] = node.left;
                        todo[++stackptr] = node.right;
                    }                      
                    //System.out.println(todo[0]+ " " +todo[1]);
                }         
            }
        }
        return hit;
    }

    @Override
    public boolean intersectP(Ray ray) {
        return false;
    }

    @Override
    public void intersect(Ray[] rays, Intersection[] isects) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public BoundingBox getBound() {
        return bound;
    }
    
    public static class BVHNode
    {
        public BoundingBox bounds;
        public boolean isLeaf;
        public int left, right, start, end, axis;        
    }
}
