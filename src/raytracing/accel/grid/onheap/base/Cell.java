/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.accel.grid.onheap.base;

import raytracing.core.coordinate.Point3i;

/**
 *
 * @author user
 */
public class Cell {
    public Point3i min;     ///< Minimum bounding box coordinate
    public int begin;     ///< Index of the first reference
    public Point3i max;     ///< Maximum bounding box coordinate
    public int end;       ///< Past-the-end reference index

    public Cell() {}
    public Cell(Point3i min, int begin, Point3i max, int end)        
    {
        this.min = min.copy();
        this.begin = begin;
        this.max = max.copy();
        this.end = end;
    }
    
    public boolean hasReference()
    {
        return (end - begin) > 0;
    }
    
    @Override
    public final String toString() {
        return String.format("(min %1s, max %1s, begin %5d, end %5d) \n", min, max, begin, end);
        //return Boolean.toString(hasReference());
    }
    
    public String minmax()
    {
        return String.format("(min %1s, max %1s)", min, max);
    }
    
    public Cell copy()
    {
        return new Cell(min.copy(), begin, max.copy(), end);
    }
}
