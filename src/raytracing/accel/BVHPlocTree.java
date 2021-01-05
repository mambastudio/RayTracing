/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.accel;

import coordinate.generic.raytrace.AbstractAccelerator;
import java.util.Arrays;
import raytracing.core.Intersection;
import raytracing.core.coordinate.BoundingBox;
import raytracing.core.coordinate.Point3f;
import raytracing.core.coordinate.Point3i;
import raytracing.core.coordinate.Ray;
import raytracing.generic.Primitive;

/**
 *
 * @author user
 * 
 * parallel potential
 * 
 */
public class BVHPlocTree implements AbstractAccelerator<Ray, Intersection, Primitive, BoundingBox> {
    
     //Primitive
    Primitive primitives;
    
    
    BVHNode[] nodes = null;
    
    BVHNode[] output = null;
    BVHNode[] input = null;
    
    int[] nearest = null;
    int radius = 100;
    
    int node_out_idx;
    int end;
    
    int leafS, nodeS, totalS;
    
    boolean swap = false;
    
    public BVHPlocTree()
    {
        
    }
    
    @Override
    public void build(Primitive primitives) {
        this.primitives = primitives;
        
        leafS = primitives.getCount();
        nodeS = leafS - 1;
        totalS = nodeS + leafS;
        
        nodes = new BVHNode[totalS];
        nearest = new int[leafS];
        
        input = new BVHNode[leafS];
        output = new BVHNode[leafS];
        
        System.out.println(leafS);
        
        for(int i = 0; i<leafS; i++)
        {
            nodes[i] = new BVHNode();              
        }
        
        for(int i = 0; i<leafS; i++)
        {                   
            nodes[i].mortonCode = encodeMorton(primitives.getCentroid(i));
            nodes[i].child = i;
            nodes[i].bounds = primitives.getBound(i);            
            nodes[i].isLeaf = true;            
        }
        
        //sort once
        Arrays.sort(nodes, 0, leafS, (node0, node1) -> node0.mortonCode - node1.mortonCode);
        
        for(int i = 0; i<leafS; i++)
        { 
            nodes[i].ptr = i;
            input[i] = nodes[i];           
        }
        
       // printBVHNodes(0, leafS);
        
        node_out_idx = leafS;    
        end = leafS;
        
        //nearest(input);
        //merge(input, output);
        
        
        //sweep
        sweep();
    }
    
    public void printBVHNodes(int s, int e)
    {
        String string = "[";
        for(int i = s; i<e-1; i++)
        {
            string += nodes[i].mortonCode + ", ";
        }
        string += nodes[leafS-1].mortonCode;
        string += "]";
        System.out.println(string);
    }
    
    public void sweep()
    {
        while(end > 1)
        {
            if(swap)
            {
                nearest(output);
                merge(output, input);
            }
            else
            {
                nearest(input);
                merge(input, output);
            }
            swap = !swap;
        }         
    }

    public void nearest(BVHNode[] input)
    {                
        //nearest neighbour
        for(int idx = 0; idx<end; idx++)
        {
            float minDistance = Float.POSITIVE_INFINITY; 
            int minIndex = -1;
            BoundingBox box = input[idx].bounds;
            
            //search left            
            for(int neighbourIndex = idx - radius; neighbourIndex < idx; ++neighbourIndex)
            {
                if(neighbourIndex < 0 || neighbourIndex>= end)
                    continue;
                BoundingBox neighbourBox = input[neighbourIndex].bounds.clone();
                neighbourBox.include(box);
                float distance = neighbourBox.getArea();
                                
                if(minDistance > distance)
                {
                    minDistance = distance;
                    minIndex = neighbourIndex;
                    
                }
                
            }
            
            //search right            
            for(int neighbourIndex = idx + 1; neighbourIndex < idx + radius + 1; ++neighbourIndex)
            {
                if(neighbourIndex < 0 || neighbourIndex >= end)
                    continue;
                BoundingBox neighbourBox = input[neighbourIndex].bounds.clone();
                neighbourBox.include(box);
                float distance = neighbourBox.getArea();
                
                if(minDistance > distance)
                {
                    minDistance = distance;
                    minIndex = neighbourIndex;
                }
                
            }            
            nearest[idx] = minIndex;            
        }
    }
    
    //merge & compaction
    public void merge(BVHNode[] input, BVHNode[] output)
    {
        int out_idx = 0;
        
        for(int idx = 0; idx<end; idx++)
        {
            if(nearest[nearest[idx]] == idx)
            {
                if(nearest[idx] > idx)
                {
                    int left = idx;
                    int right = nearest[idx];
                    
                    //init parent
                    BVHNode node = new BVHNode();
                    node.bounds = new BoundingBox();  
                    node.ptr = node_out_idx;
                    node.bounds.include(input[left].bounds, input[right].bounds); //set bounding box
                    node.left = input[left].ptr;  node.right = input[right].ptr; //update children index
                    
                    //update children (setValue sibling and parent)                   
                    input[left].sibling = input[right].ptr; input[left].parent = node_out_idx;
                    input[right].sibling = input[left].ptr; input[right].parent = node_out_idx;
                    
                    nodes[node_out_idx++] = node; //add parent
                    output[out_idx++] = node; //for processing next stage
                    
                }                
            }
            else
                output[out_idx++] = input[idx];
        }
        
        end = out_idx;
        
        
    }

    @Override
    public boolean intersect(Ray r, Intersection isect) {
        
        int[] todo = new int[164];
        int stackptr = 0;
        boolean hit = false;
        todo[0] = totalS-1;
        
        while(stackptr >= 0)
        {
            int ni = todo[stackptr];
            stackptr--;    
            BVHNode node = nodes[ni];            
            if(node.bounds.intersectP(r))
            {     
                if(node.isLeaf)
                {                       
                    if(primitives.intersect(r, node.child, isect))
                        hit = true;
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
        int nodeId = totalS-1;
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
                if(primitives.intersect(r, node.child, isect))
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
*/
    }
    
    public boolean isInner(int nodeId)
    {
        return !nodes[nodeId].isLeaf;
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
        public int mortonCode, ptr;
                
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
