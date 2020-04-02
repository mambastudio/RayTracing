/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core;

import static java.lang.Integer.max;
import static java.lang.Integer.min;
import raytracing.core.coordinate.Vector3f;

/**
 *
 * @author user
 */
public class MortonCode {
    
    public static int encodeMorton(Vector3f v)
    {
        return encodeMorton(v.x, v.y, v.z);
    }
    
    //float x, y, z should be scaled scaled between 0 to 1
    public static int encodeMorton(float x, float y, float z)
    {
        int mortonBits = 10;
        int mortonScale = 1 << mortonBits;  //morton scale to 10 bits maximum, this will enable 
                                            //to left shift three 10 bits values into a 32 bit.
        
        return encodeMortonInt( (int)(x * mortonScale), 
                                (int)(y * mortonScale),
                                (int)(z * mortonScale));
    }
    
    public static int encodeMortonInt(int x, int y, int z)
    {
        return (leftShift3(x) << 2) | (leftShift3(y) << 1) | leftShift3(z);
    }
    
    private static int leftShift3(int x)
    {
        if (x == (1 << 10)) --x;
        x = (x | (x << 16)) & 0b00000011000000000000000011111111;    
        x = (x | (x <<  8)) & 0b00000011000000001111000000001111;    
        x = (x | (x <<  4)) & 0b00000011000011000011000011000011;    
        x = (x | (x <<  2)) & 0b00001001001001001001001001001001;
        return x;
    } 
    
    public static int findSplit(MortonData[] sortedMortonCodes, int first, int last)
    {
        int firstCode = sortedMortonCodes[first].mortonCode;
        int lastCode = sortedMortonCodes[last].mortonCode;
        
        if(firstCode == lastCode)
            return (first + last) >> 1;
        
        int commonPrefix = Integer.numberOfLeadingZeros(firstCode ^ lastCode);
        
        int split = first;
        int step = last - first;
        
        do
        {
            step = (step + 1) >> 1;
            int newSplit = split + step;
            
            if(newSplit < last)
            {
                int splitCode = sortedMortonCodes[newSplit].mortonCode;
                int splitPrefix = Integer.numberOfLeadingZeros(firstCode ^ splitCode);
                if(splitPrefix > commonPrefix)
                    split = newSplit;
            }
        }
        while(step > 1);
        
        return split;
    }
    
    public static int[] determineRange(MortonData[] codes, int index)
    {
        int lastIndex = codes.length - 1;
        
        //incase root
        if(index == 0)
            return new int[] {0,  lastIndex};
        
        //direction to walk to, 1 to the right, -1 to the left
        int dir;
        //morton code diff on the outer known side of our range ... diff mc3 diff mc4 ->DIFF<- [mc5 diff mc6 diff ... ] diff .. 
        int d_min;
        int initialindex = index;
        
        int minone = codes[index-1].mortonCode;
        int precis = codes[index].mortonCode;
        int pluone = codes[index+1].mortonCode;
        if((minone == precis && pluone == precis))
        {
            //set the mode to go towards the right, when the left and the right
            //object are being the same as this one, so groups of equal
            //code will be processed from the left to the right
            //and in node order from the top to the bottom, with each node X (ret.x = index)
            //containing Leaf object X and nodes from X+1 (the split func will make this split there)
            //till the end of the groups
            //(if any bit differs... DEP=32) it will stop the search
            while(index > 0 && index < lastIndex)
            {
               //move one step into our direction
               index += 1;
               if(index >= lastIndex || codes[index].mortonCode != codes[index+1].mortonCode)
               //we hit the left end of our list, or morton codes differ
                    break;
            }
            //return the end of equal grouped codes
            int[] res = {initialindex, index};            
            return res;            
        }
        else
        {
            //Our codes differ, so we seek for the ranges end in the binary search fashion:
            int[] lr = {Integer.numberOfLeadingZeros(precis ^ minone), Integer.numberOfLeadingZeros(precis ^ pluone)};
            //now check wich one is higher (codes put side by side and wrote from up to down)
            if(lr[0] > lr[1])
            {
                //to the left, set the search-depth to the right depth
                dir = -1;
                d_min = lr[1];
            }
            else
            {
                //to the right, set the search-depth to the left depth
                dir = 1;
                d_min = lr[0];
            }   
        }
        
        //Now look for an range to search in (power of two)
        int l_max = 2;
        //so we don't have to calc it 3x
        int testindex = index + l_max * dir;
        while((testindex <= lastIndex && testindex >= 0) ? 
                (Integer.numberOfLeadingZeros(precis ^ codes[testindex].mortonCode) > d_min) : (false))
        {
            l_max = l_max << 1;
            testindex = index + l_max * dir;
        }
        
        int l = 0;
	//go from l_max/2 ... l_max/4 ... l_max/8 .......... 1 all the way down
	for(int div = 2 ; l_max / div >= 1 ; div = div << 1)
	{
            //calculate the ofset state
            int t = l_max/div;
            //calculate where to test next
            int newTest = index + (l + t)*dir;
            //test if in code range
            if (newTest <= lastIndex && newTest >= 0)
            {
                int splitPrefix = Integer.numberOfLeadingZeros(precis ^ codes[newTest].mortonCode);
                //and if the code is higher then our minimum, update the position
                if (splitPrefix > d_min)
                    l = l+t;
            }
	}
        
        return new int[]{min(index,index + l*dir),max(index,index + l*dir)};
    }
    
    public static class MortonData
    {        
        public int mortonCode; 
        public int index;
        
        public MortonData(){}
        public MortonData(int mortonCode, int index)
        {
            this.mortonCode = mortonCode;
            this.index = index;
        }
    }
    
    public static String toBinaryString(MortonData data)
    {
        return Integer.toBinaryString(data.mortonCode);
    }   
}
