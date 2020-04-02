/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.accel;

import coordinate.generic.raytrace.AbstractAccelerator;
import java.util.Arrays;
import raytracing.core.Intersection;
import raytracing.core.MortonCode.MortonData;
import static raytracing.core.MortonCode.determineRange;
import static raytracing.core.MortonCode.encodeMorton;
import static raytracing.core.MortonCode.findSplit;
import raytracing.core.coordinate.BoundingBox;
import raytracing.core.coordinate.Ray;
import raytracing.generic.Primitive;

/**
 *
 * @author user
 */
public class HLBVH implements AbstractAccelerator<Ray, Intersection, Primitive, BoundingBox> {
    Primitive primitives;
    
    @Override
    public void build(Primitive primitives) {
        this.primitives = primitives;
        
        //1: Compute bounding box of all primitive centroids      
        BoundingBox bounds = primitives.getBound();
        
        //2: Compute Morton indices of primitives
        MortonData[] mortonPrims = new MortonData[primitives.getCount()];
        for(int i = 0; i<mortonPrims.length; i++) 
        {
            mortonPrims[i] = new MortonData();            
            mortonPrims[i].index = i;            
            mortonPrims[i].mortonCode = encodeMorton(bounds.offset(primitives.getCentroid(i)));            
        }
        
        //3: Sort Morton Primitive
        Arrays.sort(mortonPrims, (a, b) -> Integer.signum(a.mortonCode - b.mortonCode));
        
        System.out.println(findSplit(mortonPrims, 0, mortonPrims.length-1));
        System.out.println(Arrays.toString(determineRange(mortonPrims, 0)));
       
    }
    
    public void generateHiererchy(MortonData[] mortonPrims, Node[] leafNodes, Node[] internalNodes) //leafNodes = new Node[numOfObjects], internalNodes = new Node[numOfObjects-1]
    {
        int numOfObjects = primitives.getCount();
                
        for(int idx = 0; idx < numOfObjects; idx++) //parallel
            leafNodes[idx].objectID = mortonPrims[idx].index;
        
        //Construct internal nodes
        for(int idx = 0; idx < numOfObjects-1; idx++) //parallel
        {
            int[] range = determineRange(mortonPrims, idx);
            int first = range[0];
            int last = range[1];
            
            //Determine where to split
            int split = findSplit(mortonPrims, first, last);
            
            //Select child A
            Node childA;            
            if(split == first)
                childA = leafNodes[split];
            else
                childA = internalNodes[split];
            
            //Select child B
            Node childB;
            if(split + 1 == last)
                childB = leafNodes[split + 1];
            else
                childB = internalNodes[split + 1];
            
            internalNodes[idx].childA = childA;
            internalNodes[idx].childB = childB;
            childA.parent = internalNodes[idx];
            childB.parent = internalNodes[idx];           
        }
        
        
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
    
    public class Node
    {
        public int objectID;
        public Node parent = null;
        public Node childA = null;
        public Node childB = null;
    }    
}
