/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.accel.grid;

/**
 *
 * @author user
 */
public class Entry {
    final int LOG_DIM_BITS = 2;
    final int BEGIN_BITS   = 32 - LOG_DIM_BITS;
    
    int log_dim = LOG_DIM_BITS;    ///< Logarithm of the dimensions of the entry (0 for leaves)
    int begin   = BEGIN_BITS;      ///< Next entry index (cell index for leaves)
    
    public Entry(int log_dim, int begin)
    {
        this.log_dim = log_dim;
        this.begin = begin;
    }

}
