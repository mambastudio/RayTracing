/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.accel;

import coordinate.generic.raytrace.AbstractAccelerator;
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
        
        this.primitives = primitives;  
        objects = new int[this.primitives.getSize()];
        for(int i = 0; i<this.primitives.getSize(); i++)
            objects[i] = i;
        bound = this.primitives.getBound();
        
        //Allocate BVH root node
        nodes = new BVHNode[this.primitives.getSize() * 2 - 1];
        BVHNode root = new BVHNode();  
        nodes[0] = root; 
        nodesPtr = 1;
        
        subdivide(root, 0, 0, objects.length);        
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
        getSAHMid(bc, bb, split_dim, start, end);
                
        //Subdivide
        subdivide(left, leftIndex, start, mid);
        subdivide(right, rightIndex, mid, end);
    }
    
    //FIXME
    private int getSAHMid(BoundingBox bc, BoundingBox bb, int split_dim, int start, int end)
    {
        int nBuckets = 12;
        BucketInfo[] buckets = new BucketInfo[nBuckets];
        for(int i = 0; i<nBuckets; i++)
            buckets[i] = new BucketInfo();
        
        //initialize bucket info
        for(int i = start; i<end; ++i)
        {
            int b = (int) (nBuckets * bc.offset(primitives.getCentroid(objects[i])).get(split_dim));
            if(b == nBuckets) b = nBuckets - 1;
            buckets[b].count++;
            buckets[b].bounds.include(primitives.getBound(objects[i]));            
        }
        
        //compute cost for splitting after each bucket
        float[] cost = new float[nBuckets - 1];
        for(int i = 0; i<nBuckets-1; ++i)
        {
            BoundingBox b0 = new BoundingBox();
            BoundingBox b1 = new BoundingBox();
            int count0 = 0, count1 = 0;
            for(int j = 0; j<=i; ++j)
            {
                b0.include(buckets[j].bounds);
                count0 += buckets[j].count;                
            }
            
            for(int j = i+1; j<nBuckets; ++j)
            {
                b1.include(buckets[j].bounds);
                count1 += buckets[j].count;
            }
            cost[i] = .125f + (count0 + b0.getArea() + 
                               count1 + b1.getArea())/bb.getArea();
            
        }
        
        float minCost = cost[0];
        int minCostSplitBucket = 0;
        for(int i = 1; i<nBuckets-1; ++i)
            if(cost[i]<minCost)
            {
                minCost = cost[i];
                minCostSplitBucket = i;
            }
        return 0;
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
        int[] todo = new int[64];
        int stackptr = 0;
        boolean hit = false;
        
        while(stackptr >= 0)
        {
            int ni = todo[stackptr];
            stackptr--;    
            BVHNode node = nodes[ni];            
            if(node.bounds.intersectP(r))
            {     
                if(node.isLeaf)
                {                       
                    if(primitives.intersect(r, objects[node.child], isect))
                        hit |= true;
                }
                else
                {  
                    BVHNode left        = nodes[node.left];
                    BVHNode right       = nodes[node.right];
                
                    float[] leftT       = new float[2];
                    float[] rightT      = new float[2];
                    boolean leftHit     = left.bounds.intersectP(r, leftT);
                    boolean rightHit    = right.bounds.intersectP(r, rightT);
                    
                    if(!leftHit && !rightHit) 
                        continue; 
                
                    if(leftHit && rightHit)
                    {   
                        if(rightT[0] < leftT[0])
                        {
                            todo[++stackptr] = node.right;
                            todo[++stackptr] = node.left;
                        }
                        else
                        {
                            todo[++stackptr] = node.left;
                            todo[++stackptr] = node.right;
                        }
                    }
                    else
                    {
                        todo[++stackptr] = leftHit ? node.left : node.right;                   
                    }               
                    
                    //System.out.println(todo[0]+ " " +todo[1]);
                }         
            }
        }
        return hit;
        /*
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
            bitstack ^= 1;                    //turn bit to 0, we are done with it
        }  
*/
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
    
    private class BucketInfo
    {
        int count;
        BoundingBox bounds;
        
        BucketInfo()
        {
            count = 0;
            bounds = new BoundingBox();
        }
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
