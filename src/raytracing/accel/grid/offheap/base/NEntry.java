/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.accel.grid.offheap.base;

import coordinate.memory.nativememory.NativeInteger.IntElement;
import coordinate.utility.BitUtility;

/**
 *
 * @author jmburu
 */
public final class NEntry implements IntElement<NEntry> {
    
    //LOG_DIM_BITS and BEGIN_BITS are to compress the log_dim and begin into 32bit value
    public static final int LOG_DIM_BITS = 2;                   
    public static final int BEGIN_BITS = 32 - LOG_DIM_BITS;
    
    public int log_dim;    ///< Logarithm of the dimensions of the entry (1, 2, 3 - axis and 0 for leaves)
    public int begin;      ///< Next entry index (cell index for leaves)
    
    public NEntry(){
        log_dim = 0;
        begin = 0;
    }
    
    public NEntry(int value)
    {
        setInt(value);
    }
    
    public NEntry(int log_dim, int begin)
    {
        this.log_dim = log_dim;
        this.begin = begin;
    }
    
    @Override
    public String toString()
    {
        return "log_dim: " +log_dim+ " begin: " +begin;
    }
        
    @Override
    public NEntry newInstance() {
        return new NEntry();
    }

    @Override
    public int getInt() {
        int value = 0;
        value = BitUtility.apply_bits_at(0, log_dim, value);
        value = BitUtility.apply_bits_at(2, begin, value);
        return value;
    }

    @Override
    public void setInt(int value) {
        log_dim = BitUtility.get_bits_at(0, value, LOG_DIM_BITS);
        begin = BitUtility.get_bits_at(2, value, BEGIN_BITS);
    }
    
}
