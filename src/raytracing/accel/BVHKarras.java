package raytracing.accel;

import coordinate.generic.raytrace.AbstractAccelerator;
import static java.lang.Math.max;
import static java.lang.Math.min;
import raytracing.core.Intersection;
import raytracing.core.coordinate.BoundingBox;
import raytracing.core.coordinate.Point2i;
import raytracing.core.coordinate.Point3f;
import raytracing.core.coordinate.Point3i;
import raytracing.core.coordinate.Ray;
import raytracing.generic.Primitive;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author user
 */
public class BVHKarras implements AbstractAccelerator<Ray, Intersection, Primitive, BoundingBox> {
    BVHNode[] nodes;
    BoundingBox[] bounds;
    int leafS, nodeS, totalS;
    
    Primitive primitives = null;
        
    @Override
    public void build(Primitive primitives) 
    {
        this.primitives = primitives;
        
        leafS = primitives.getSize();
        nodeS = leafS - 1;
        totalS = nodeS + leafS;
        
        nodes = new BVHNode[totalS];
        bounds = new BoundingBox[totalS];
        
        MortonPrimitive[] mortons = new MortonPrimitive[leafS];
        int[] flags = new int[nodeS];
        
        for(int i = 0; i<leafS; i++)
        {
            mortons[i] = new MortonPrimitive();
            mortons[i].mortonCode = encodeMorton(primitives.getCentroid(i));
            mortons[i].primitiveIndex = i;
            
            bounds[LEAFIDX(leafS, i)] = new BoundingBox();
            nodes[LEAFIDX(leafS, i)] = new BVHNode();

            if(i < nodeS)
            {
                bounds[NODEIDX(leafS, i)] = new BoundingBox();
                nodes[NODEIDX(leafS, i)] = new BVHNode();
            }
        }
        
        sort(mortons);
       
        for(int i = 0; i<leafS; i++)
        {
            int leafIndex = i;
            
            //Init leaf
            if(i < leafS)
            {                
                nodes[leafIndex].isLeaf = true;
                nodes[leafIndex].child = mortons[leafIndex].primitiveIndex;
                nodes[leafIndex].bound = leafIndex;
                bounds[leafIndex] = primitives.getBound(nodes[leafIndex].child);
            }
            
            //Set internal nodes
            if(i < nodeS)
            {
                
                // Find span occupied by the current node
                Point2i range = findSpan(mortons, leafS, i);

                // Find split position inside the range
                int  split = findSplit(mortons, leafS, range.x, range.y);

                // Create child nodes if needed
                int c1idx = (split == range.x) ? LEAFIDX(leafS, split) : NODEIDX(leafS, split);
                int c2idx = (split + 1 == range.y) ? LEAFIDX(leafS, split + 1) : NODEIDX(leafS, split + 1);
                
                // Set left, right child, and init bounding box
                nodes[NODEIDX(leafS, i)].left = c1idx;
                nodes[NODEIDX(leafS, i)].right = c2idx;
                
                // Set parent of left, right child  and also siblings
                nodes[c1idx].parent = NODEIDX(leafS, i);     nodes[c1idx].sibling = c2idx;
                nodes[c2idx].parent = NODEIDX(leafS, i);     nodes[c2idx].sibling = c1idx;
            }
        }
                
        
        //refit bounds
        for(int i = 0; i<leafS; i++)
        {
            // Get my leaf index
            int idx = LEAFIDX(leafS, i);

            do
            {
                // Move to parent node
                idx = nodes[idx].parent;
                
                System.out.println(idx - leafS);
                                
                // Check node's flag
                if (cmpxchg(flags, (idx - leafS), 0, 1) == 1)
                {
                    // If the flag was 1 the second child is ready and
                    // this thread calculates bbox for the node

                    // Fetch kids
                    int lc = nodes[idx].left;
                    int rc = nodes[idx].right;
                    BoundingBox b1 = bounds[lc];
                    BoundingBox b2 = bounds[rc];

                    // Calculate bounds
                    BoundingBox b = new BoundingBox();
                    b.include(b1, b2);
                    
                    // Write bounds
                    bounds[idx] = b;
                    nodes[idx].bound = idx;
                }
                else
                {
                    // If the flag was 0 setValue it to 1 and bail out.
                    // The thread handling the second child will
                    // handle this node.
                    break;
                }
            }
            while ((idx - leafS) != 0);
        }
        
    }

    @Override
    public boolean intersect(Ray ray, Intersection isect) {
        boolean hit = false;
        int nodeId = leafS;
        long bitstack = 0;                      //be careful when you use a 32 bit integer. For deeper hierarchy traversal may lead into an infinite loop for certain scenes
        int parentId = 0, siblingId = 0;
        
        for(;;)
        {            
            while(!nodes[nodeId].isLeaf)
            {
                BVHNode node        = nodes[nodeId];
                parentId            = node.parent;
                siblingId           = node.sibling;
                
                BVHNode left        = nodes[node.left];
                BVHNode right       = nodes[node.right];
                
                float[] leftT       = new float[2];
                float[] rightT      = new float[2];
                boolean leftHit     = bounds[left.bound].intersectP(ray, leftT);
                boolean rightHit    = bounds[right.bound].intersectP(ray, rightT);
                
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
            if(nodes[nodeId].isLeaf)
            {
                BVHNode node    = nodes[nodeId];
                if(primitives.intersect(ray, node.child, isect))
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
        return bounds[leafS];
    }
    
    int LEAFIDX(int size, int i)
    {
        return i;
    }
    
    int NODEIDX(int size, int i)
    {
        return size + i;
    }
    
    int cmpxchg(int[] flags, int index, int cmp, int val)
    {
        int old = flags[index];
        flags[index] = (old == cmp) ? val : old;
        return old;
    }
    
    int delta(MortonPrimitive[] sortedMortonCodes, int num_prims, int i1, int i2)
    {
        // Select left end
        int left = min(i1, i2);
        // Select right end
        int right = max(i1, i2);
        // This is to ensure the node breaks if the index is out of bounds
        if (left < 0 || right >= num_prims)
        {            
            return -1;
        }
        // Fetch Morton codes for both ends
        int left_code = sortedMortonCodes[left].mortonCode;
        int right_code = sortedMortonCodes[right].mortonCode;

        // Special handling of duplicated codes: use their indices as a fall
        return left_code != right_code ? Integer.numberOfLeadingZeros(left_code ^ right_code) : (32 + Integer.numberOfLeadingZeros(left ^ right));
    }
    
    int findSplit(MortonPrimitive[] sortedMortonCodes, int num_prims, int first, int end)
    {
        // Fetch codes for both ends
        int left = first;
        int right = end;

        // Calculate the number of identical bits from higher end
        int num_identical = delta(sortedMortonCodes, num_prims, left, right);
        
        //if(true) return (right + left)/2;
        
        do
        {
            // Proposed split
            int new_split = (right + left) / 2;

            // If it has more equal leading bits than left and right accept it
            if (delta(sortedMortonCodes, num_prims, left, new_split) > num_identical)
                left = new_split;            
            else            
                right = new_split;            
        }
        while (right > left + 1);

        return left;
    }
    
    public Point2i findSpan(MortonPrimitive[] sortedMortonCodes, int num_prims, int idx)
    {
        // Find the direction of the range
        int d = Integer.signum((delta(sortedMortonCodes, num_prims, idx, idx+1) - delta(sortedMortonCodes, num_prims, idx, idx-1)));

        // Find minimum number of bits for the break on the other side
        int delta_min = delta(sortedMortonCodes, num_prims, idx, idx-d);

        // Search conservative far end
        int lmax = 2;
        while (delta(sortedMortonCodes, num_prims, idx,idx + lmax * d) > delta_min)
            lmax *= 2;

        // Search back to find exact bound
        // with binary search
        int l = 0;
        int t = lmax;
        do
        {
            t /= 2;
            if(delta(sortedMortonCodes, num_prims, idx, idx + (l + t)*d) > delta_min)
            {
                l = l + t;
            }
        }
        while (t > 1);

        // Pack span 
        Point2i  span = new Point2i();
        span.x = min(idx, idx + l*d);
        span.y = max(idx, idx + l*d);

        return span;
    }
    
    public void sort(MortonPrimitive[] mortonPrimitives)
    {
        MortonPrimitive[] temp = new MortonPrimitive[mortonPrimitives.length];
        int bitsPerPass = 6;
        int nBits = 30;
        int nPasses = nBits/bitsPerPass;
        
        for(int pass = 0; pass < nPasses; ++pass)
        {
            int lowBit = pass * bitsPerPass;
            
            MortonPrimitive[] in  = ((pass & 1) == 1) ? temp              : mortonPrimitives;
            MortonPrimitive[] out = ((pass & 1) == 1) ? mortonPrimitives  : temp;
            
            int nBuckets = 1 << bitsPerPass;
            int bucketCount[] = new int[nBuckets];
            int bitMask = (1 << bitsPerPass) - 1;
            
            for (MortonPrimitive p : in) {
                int bucket = (p.mortonCode >> lowBit) & bitMask; 
                ++bucketCount[bucket];
            }
            
            int[] outIndex = new int[nBuckets];
            for(int i = 1; i < nBuckets; ++i)
                outIndex[i] = outIndex[i - 1] + bucketCount[i-1];
            
            for (MortonPrimitive p : in) {
                int bucket = (p.mortonCode >> lowBit) & bitMask;
                out[outIndex[bucket]++] = p;
            }            
        }
        
        if((nPasses & 1) == 1) System.arraycopy(temp, 0, mortonPrimitives, 0, temp.length);    
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
     
    public class BVHNode
    {
        public int bound;
        public boolean isLeaf;        
        public int parent, sibling, left, right, child;  
        
        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder(); 
            builder.append("bounds   ").append(bound).append("\n");
            builder.append("is leaf  ").append(isLeaf).append("\n");
            builder.append("parent   ").append(parent).append("\n");
            builder.append("sibling  ").append(sibling).append("\n");
            builder.append("left     ").append(left).append(" right     ").append(right).append("\n");
            builder.append("child no ").append(child).append("\n");
            builder.append("\n");
            return builder.toString();
        }
    }
    
    public class MortonPrimitive
    {
        public int mortonCode;
        public int primitiveIndex;
        
        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append("primitive index - ").append(primitiveIndex).append(" ");
            builder.append("morton code     - ").append(mortonCode);
            return builder.toString();
        }
    }
}
