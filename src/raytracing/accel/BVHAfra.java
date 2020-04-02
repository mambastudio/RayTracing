/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.accel;

import coordinate.generic.raytrace.AbstractAccelerator;
import java.util.concurrent.TimeUnit;
import raytracing.core.Intersection;
import raytracing.core.coordinate.BoundingBox;
import raytracing.core.coordinate.Ray;
import raytracing.generic.Primitive;

/**
 *
 * @author user
 */
public class BVHAfra implements AbstractAccelerator<Ray, Intersection, Primitive, BoundingBox> {
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
        
        subdivide(root, 0, 0, objects.length);
        
        long time2 = System.nanoTime();
        
        long timeDuration = time2 - time1;
        String timeTaken= String.format("BVH build time: %02d min, %02d sec", 
                TimeUnit.NANOSECONDS.toMinutes(timeDuration), 
                TimeUnit.NANOSECONDS.toSeconds(timeDuration));
        System.out.println(timeTaken);     
        
        for(BVHNode node : nodes)
            System.out.println(node);
    }
    
    private void subdivide(BVHNode parent, int parentIndex, int start, int end)
    {
        //Calculate the bounding box for the root node
        BoundingBox bb = new BoundingBox();
        BoundingBox bc = new BoundingBox();
        calculateBounds(start, end, bb, bc);
        parent.bounds = bb;
                
        //Initialize leaf
        if(end - start < 2)
        {            
            parent.child = start;            
            parent.isLeaf = true;
            return;
        }
        
        //Subdivide parent node        
        BVHNode left, right;  int leftIndex, rightIndex;      
        synchronized(this)
        {
            left            = new BVHNode();   left.parent = parentIndex;
            right           = new BVHNode();   right.parent = parentIndex;
            
            nodes[nodesPtr] = left;  leftIndex  = nodesPtr;   parent.left     = nodesPtr++;
            nodes[nodesPtr] = right; rightIndex = nodesPtr;   parent.right    = nodesPtr++;   
            
            left.sibling  = rightIndex;
            right.sibling = leftIndex;
        }   
        
        //set the split dimensions
        int split_dim = bc.maximumExtentAxis();        
        int mid = getMid(bc, split_dim, start, end);
                
        //Subdivide
        subdivide(left, leftIndex, start, mid);
        subdivide(right, rightIndex, mid, end);
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
    public boolean intersect(Ray r, Intersection isect)
    {
        boolean hit = false;
        int nodeId = 0;
        long bitstack = 0;                      //be careful when you use a 32 bit integer. For deeper hierarchy traversal may lead into an infinite loop for certain scenes
        int parentId = 0, siblingId = 0;
        
        for(;;)
        {            
            while(isInner(nodeId))
            {
                BVHNode node        = nodes[nodeId];
                parentId            = node.parent;
                siblingId           = node.sibling;
                
                BVHNode left        = nodes[node.left];
                BVHNode right       = nodes[node.right];
                
                float[] leftT       = new float[2];
                float[] rightT      = new float[2];
                boolean leftHit     = left.bounds.intersectP(r, leftT);
                boolean rightHit    = right.bounds.intersectP(r, rightT);
                
                if(!leftHit && !rightHit) 
                    break; 
                
                bitstack <<= 1; //push 0 bit into bitstack to skip the sibling later
                
                if(leftHit && rightHit)
                {                    
                    nodeId = (rightT[0] < leftT[0]) ? node.right : node.left;                    
                    bitstack |= 1; //change skip code to 1 to traverse the sibling later
                }
                else
                {
                    nodeId = leftHit ? node.left : node.right;                   
                }                 
            }
            if(!isInner(nodeId))
            {
                BVHNode node    = nodes[nodeId];
                if(primitives.intersect(r, objects[node.child], isect))
                    hit |= true;    
                
                //This is not in the paper. But it had me debugging for 1 week
                parentId            = node.parent;
                siblingId           = node.sibling;
            }  
            
            while ((bitstack & 1) == 0)  //while skip bit in the top stack is 0 traverse up the tree
            {
                if (bitstack == 0) return hit;  //if bitstack is 0 meaning stack is empty, it is now safe to exit the tree and return hit
                nodeId = parentId;
                BVHNode node = nodes[nodeId];
                parentId = node.parent;
                siblingId = node.sibling;
                bitstack >>= 1;               //pop the bit in top most part os stack by right bit shifting
            }
            nodeId = siblingId;
            bitstack ^= 1;           
        }     
    }
    
    public boolean isInner(int nodeId)
    {
        return !nodes[nodeId].isLeaf;
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
        public int parent, sibling, left, right, child;  
        
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
