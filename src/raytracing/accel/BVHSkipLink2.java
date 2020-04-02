/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.accel;

import coordinate.generic.raytrace.AbstractAccelerator;
import coordinate.struct.FloatStruct;
import coordinate.struct.StructFloatArray;
import java.util.concurrent.TimeUnit;
import raytracing.core.Intersection;
import raytracing.core.coordinate.BoundingBox;
import raytracing.core.coordinate.Ray;
import raytracing.generic.Primitive;

/**
 *
 * @author user
 */
public class BVHSkipLink2 implements AbstractAccelerator<Ray, Intersection, Primitive, BoundingBox> {
    //Primitive
    Primitive primitives;
    
    //Tree, Primitive index, Boundingbox
    int[] objects;
    StructFloatArray<BVHNode> nodes = null;    
    BoundingBox bound = null;
    
    //node counter
    int nodeCount = 0;
    
    //constant values
    final private int LEAF = 0;
    
    @Override
    public void build(Primitive primitives) {
        long time1 = System.nanoTime();
        
        this.primitives = primitives;  
        objects = new int[this.primitives.getCount()];
        for(int i = 0; i<this.primitives.getCount(); i++)
            objects[i] = i;
        bound = this.primitives.getBound();
        
        //Allocate BVH root node
        nodes = new StructFloatArray<>(BVHNode.class, this.primitives.getCount() * 2 - 1);
        BVHNode root = new BVHNode();
        nodes.set(root, 0);        
        nodeCount = 1;
        
        //build tree
        subdivide(root, 0, objects.length);
        
        long time2 = System.nanoTime();
        
        long timeDuration = time2 - time1;
        String timeTaken= String.format("BVH build time: %02d min, %02d sec", 
                TimeUnit.NANOSECONDS.toMinutes(timeDuration), 
                TimeUnit.NANOSECONDS.toSeconds(timeDuration));
        System.out.println(timeTaken);        
    }
    
    private void subdivide(BVHNode parent, int start, int end)
    {
        //Calculate the bounding box for the root node
        BoundingBox bb = new BoundingBox();
        BoundingBox bc = new BoundingBox();
        calculateBounds(start, end, bb, bc);
        parent.setBounds(bb);
              
        //Initialize leaf
        if(end - start == 1)
        {    
            parent.setPrimOffset(start);               
            return;
        }
        
        //Subdivide parent node        
        BVHNode left, right;   
        synchronized(this)
        {
            left             = new BVHNode();
            right            = new BVHNode();           
        }
        
        //set split
        int split_dim = bc.maximumExtentAxis();
        int mid = getMid(bc, split_dim, start, end);
        
        //Subdivide
        nodes.set(left, nodeCount++);       
        subdivide(left, start, mid); 
        
        parent.setSkipIndex(nodeCount); //PLEASE NOTE HERE
        
        nodes.set(right, nodeCount++);        
        subdivide(right, mid, end);
    }
    
    private void calculateBounds(int first, int end, BoundingBox bb, BoundingBox bc)
    {                
        for(int p = first; p<end; p++)
        {
            bb.include(primitives.getBound(objects[p]));
            bc.include(primitives.getBound(objects[p]).getCenter());
        }        
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


    @Override
    public boolean intersect(Ray ray, Intersection isect) {
        int currentNode = 0;
        boolean hit = false;
        
        while(currentNode < nodes.size())
        {            
            BVHNode node = nodes.get(currentNode);            
            if(node.bounds.intersectP(ray))
            {                
                if(isLeaf(node))               
                    hit |= primitives.intersect(ray, objects[node.primOffset], isect);                    
                currentNode++;      
            }
            else
            {
                if(node.skipIndex > 0)
                    currentNode = node.skipIndex;
                else
                    currentNode++;
            }            
        }        
        return hit;
    }
    
    private boolean isLeaf(BVHNode node)
    {
        return node.skipIndex == LEAF;
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
    
    public static class BVHNode extends FloatStruct
    {
        private BoundingBox bounds;
        private int primOffset, skipIndex;  
        
        public BVHNode()
        {
            this.bounds = new BoundingBox();
        }
        
        public void setBounds(BoundingBox bound)
        {
            this.bounds = bound;
            this.refreshGlobalArray();
        }
        
        public void setPrimOffset(int primOffset)
        {
            this.primOffset = primOffset;
            this.refreshGlobalArray();
        }
        
        public void setSkipIndex(int skipIndex)
        {
            this.skipIndex = skipIndex;
            this.refreshGlobalArray();
        }

        @Override
        public void initFromGlobalArray() {
            float[] globalArray = getGlobalArray();
            if(globalArray == null)
                return;
            int globalArrayIndex = getGlobalArrayIndex();
            
            bounds.minimum.x =       globalArray[globalArrayIndex + 0];
            bounds.minimum.y =       globalArray[globalArrayIndex + 1];
            bounds.minimum.z =       globalArray[globalArrayIndex + 2];
            bounds.maximum.x =       globalArray[globalArrayIndex + 3];
            bounds.maximum.y =       globalArray[globalArrayIndex + 4];
            bounds.maximum.z =       globalArray[globalArrayIndex + 5];
            primOffset       = (int) globalArray[globalArrayIndex + 6];
            skipIndex        = (int) globalArray[globalArrayIndex + 7];        
        }

        @Override
        public float[] getArray() {
            return new float[]{bounds.minimum.x, bounds.minimum.y, bounds.minimum.z,
                               bounds.maximum.x, bounds.maximum.y, bounds.maximum.z,
                               primOffset, skipIndex};
        }

        @Override
        public int getSize() {
            return 8;
        }       
    }    
}
