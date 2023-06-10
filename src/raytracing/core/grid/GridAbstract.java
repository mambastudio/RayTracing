/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid;

import raytracing.core.grid.base.Entry;

/**
 *
 * @author user
 */
public abstract class GridAbstract {
    public int __ffs(int value)            
    {      
        //https://en.wikipedia.org/wiki/Find_first_set
        return value == 0 ? 0 : Integer.numberOfTrailingZeros(value) + 1;
    }
    
    public int __popc(int mask) {
        return Integer.bitCount(mask); 
    }
    
    public int __clz(int k)
    {          
        return Integer.numberOfLeadingZeros(k);
    }
    
    public int log2nlz(int bits)
    {
        return bits == 0 ? 0 : 31 - Integer.numberOfLeadingZeros(bits);
    }
    
    
    /// Returns a voxel map entry with the given dimension and starting index
    public Entry make_entry(int log_dim, int begin) {
        Entry e = new Entry(log_dim, begin);
        return e;
    }
}
