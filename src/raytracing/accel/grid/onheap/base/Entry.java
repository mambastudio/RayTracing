/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.accel.grid.onheap.base;

/**
 *
 * @author user
 */
public class Entry {
    public static final int LOG_DIM_BITS = 2;
    public static final int BEGIN_BITS = 32 - LOG_DIM_BITS;
    
    public int log_dim;    ///< Logarithm of the dimensions of the entry (0 for leaves)
    public int begin;      ///< Next entry index (cell index for leaves)
    
    public Entry(){
        log_dim = 0;
        begin = 0;
    }
    
    public Entry(int log_dim, int begin)
    {
        this.log_dim = log_dim;
        this.begin = begin;
    }
    
    public Entry copy()
    {
        return new Entry(log_dim, begin);
    }
}
