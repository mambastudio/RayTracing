/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.accel.grid;

import raytracing.core.coordinate.Point3f;

/**
 *
 * @author user
 */
public class Cell {
    Point3f min;     ///< Minimum bounding box coordinate
    int begin;     ///< Index of the first reference
    Point3f max;     ///< Maximum bounding box coordinate
    int end;       ///< Past-the-end reference index

    public Cell() {}
    public Cell(Point3f min, int begin, Point3f max, int end)
    {
        this.min = min;
        this.begin = begin;
        this.max = max;
        this.end = end;
    }
}
