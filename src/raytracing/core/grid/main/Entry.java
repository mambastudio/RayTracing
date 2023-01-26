/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid.main;

import static raytracing.core.grid.main.Entry.Bits.BEGIN_BITS;
import static raytracing.core.grid.main.Entry.Bits.LOG_DIM_BITS;

/**
 *
 * @author user
 */

/// Voxel map entry
public class Entry {
    public enum Bits {
        LOG_DIM_BITS(2),
        BEGIN_BITS(32 - LOG_DIM_BITS.getValue());
               
        private final int value;

        Bits(final int newValue) {
            value = newValue;
        }

        public int getValue() { return value; }
    };

    public int log_dim = LOG_DIM_BITS.getValue();    ///< Logarithm of the dimensions of the entry (0 for leaves)
    public int begin   = BEGIN_BITS.getValue();      ///< Next entry index (cell index for leaves)
    
    public Entry(){}
    
    public Entry(int log_dim, int begin)
    {
        this.log_dim = log_dim;
        this.begin = begin;
    }
    
    @Override
    public String toString()
    {
        return "log_dim " +log_dim+ " begin " +begin;
    }
}
