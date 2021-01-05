/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid.main;

import java.util.Arrays;
import raytracing.core.coordinate.Point3f;
import static raytracing.core.grid.Grid.count_union;
import static java.lang.Math.*;

/**
 *
 * @author user
 */
public class Cell
{
    public int[] min = new int[3];
    public int begin;
    public int[] max = new int[3];
    public int end;

    // Traversal cost for one cell
    public final float unit_cost = 1.0f;
    // Cost of intersecting n triangles
    public static int K(int n) {
        return (n + 1) / 2;
    }

    public int can_merge(Cell other, int axis) 
    {
        int axis1 = (axis + 1) % 3;
        int axis2 = (axis + 2) % 3;
        return other.min[axis]  == max[axis]  &&
               other.min[axis1] == min[axis1] && other.max[axis1] == max[axis1] &&
               other.min[axis2] == min[axis2] && other.max[axis2] == max[axis2] ? 1 : 0;
    }

    public boolean is_merge_profitable(Point3f cell_size, int[] refs, Cell other)
    {
        float cost0 = cost(cell_size);
        float cost1 = other.cost(cell_size);
        int merged_max[] = new int[]{   max(max[0], other.max[0]),
                                        max(max[1], other.max[1]),
                                        max(max[2], other.max[2])};
        int merged_min[] = new int[]{   min(min[0], other.min[0]),
                                        min(min[1], other.min[1]),
                                        min(min[2], other.min[2]) };
        float merged_area = half_area(cell_size, merged_min, merged_max);
        //sub array with refs + begin
        int count = count_union(Arrays.copyOfRange(refs, begin, end), end - begin,
                                Arrays.copyOfRange(refs, other.begin, other.end), other.end - other.begin);
        return merged_area * (K(count) + unit_cost) <= (cost0 + cost1);
    }

    public int size(int axis) 
    { 
        return max[axis] - min[axis]; 
    }

    public float cost(Point3f cell_size)
    {
        return half_area(cell_size, min, max) * (K(end - begin) + unit_cost);
    }

    public static float half_area(Point3f cell_size, int[] min, int[] max) {
        Point3f extents = new Point3f(
                            (max[0] - min[0]) * cell_size.x,
                            (max[1] - min[1]) * cell_size.y,
                            (max[2] - min[2]) * cell_size.z);
        return extents.x * (extents.y + extents.z) + extents.y * extents.z;
    }
}
