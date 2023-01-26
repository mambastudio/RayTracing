/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid.main;

import coordinate.utility.Value3Di;

/**
 *
 * @author user
 */

/// Compressed irregular grid cell
public class SmallCell {
    public Value3Di min;       ///< Minimum bounding box coordinate
    public Value3Di max;       ///< Maximum bounding box coordinate
    public int begin;          ///< Index of the first reference
    
    public SmallCell()
    {
        
    }
    
    public SmallCell(Value3Di min, Value3Di max, int begin)
    {
        this.min = min; this.max = max; this.begin = begin;
    }
}
