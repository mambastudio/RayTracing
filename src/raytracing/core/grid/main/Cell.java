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

/// Cell of the irregular grid
public class Cell {
    public Value3Di min = new Value3Di();   ///< Minimum bounding box coordinate
    public int begin;                       ///< Index of the first reference
    public Value3Di max = new Value3Di();   ///< Maximum bounding box coordinate
    public int end;                         ///< Past-the-end reference index
    
    public Cell(){}
    public Cell(Value3Di min, int begin, Value3Di max, int end)
    {
        this.min = min;
        this.begin = begin;
        this.max = max;
        this.end = end;
    }
}
