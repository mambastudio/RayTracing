/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid.main;

import static java.lang.Math.max;
import static java.lang.Math.min;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import raytracing.core.coordinate.Point3f;
import raytracing.core.coordinate.Vector3f;
import static raytracing.core.grid.main.GridUtility.lookup_entry;
import static raytracing.core.grid.main.GridUtility.parallelFor;
import static raytracing.core.grid.main.OverlapUtility.find_overlap;
import static raytracing.core.grid.main.Tri_Overlap_Box.tri_overlap_box;

/**
 *
 * @author user
 */

// When this symbol is defined, the overlap optimization (called cell expansion in the paper)
// uses a simple and fast criterion to expand a cell: A cell C is expanded if all its neighbouring cells
// contains only a subset of the primitives in C. When this symbol is commented out, the optimization
// inspects each primitive that is not in the primitives contained in C, to see by how many virtual
// grid voxels the cell C can be expanded (that was not originally mentioned in the paper for reasons of space).
// Of course, this process is much more expensive (10x slower), but it yields a significant improvement in performance.
public class OverlapUtility 
{
    public static boolean OVERLAP_SUBSET = false;
    
    // Finds the index of an element in a sorted array, return -1 if not found
    public static int bisection(int[] p, int c, int e) {
        // Cannot use std::binary_search here because we need the position within the array
        int a = 0, b = c - 1;
        while (a <= b) {
            int m = (a + b) / 2;
            int f = p[m];
            if (f == e) return m;
            a = (f < e) ? m + 1 : a;
            b = (f > e) ? m - 1 : b;
        }
        return -1;
    }
    
    public static boolean is_subset(
            List<Integer> refs, 
            int cell_begin, 
            int count, 
            int next_cell_begin, 
            int next_count) 
    {
        List<Integer> subList1 = refs.subList(cell_begin, cell_begin + count);
        List<Integer> subList2 = refs.subList(next_cell_begin, next_cell_begin + next_count);
        return subList1.containsAll(subList2);
    }
    
    public static int bisection(List<Integer> refs, int first_ref, int end_ref, int ref) {
        List<Integer> subList = refs.subList(first_ref, end_ref);
        return Collections.binarySearch(subList, ref);
    }
    
    public static void find_overlap(
            int axis,
            GridInfo info,
            List<Integer> entries,
            List<Integer> refs,
            List<Tri> tris,
            List<Cell> cells,
            int cell_id,
            Overlap overlap) 
    {
        Cell cell = cells.get(cell_id);
        int count = cell.end - cell.begin;
        int axis1 = (axis + 1) % 3;
        int axis2 = (axis + 2) % 3;

        Vector3f cell_size = new Vector3f(info.cell_size()).div(1 << info.max_snd_dim);
        Point3f min_bb = info.bbox.minimum.add(new Vector3f(cell.min[0], cell.min[1], cell.min[2]).mul(cell_size));
        Point3f max_bb = info.bbox.minimum.add(new Vector3f(cell.max[0], cell.max[1], cell.max[2]).mul(cell_size));
        
        int dims[] = {
            info.dims[0] << info.max_snd_dim,
            info.dims[1] << info.max_snd_dim,
            info.dims[2] << info.max_snd_dim
            };
        int dmin = 0, dmax = 0;
        
        if (cell.min[axis] > 0) {
            dmin = -dims[axis];

            int k1, k2;
            for (int i = cell.min[axis1]; i < cell.max[axis1] && dmin < 0; i += k1) {
                k1 = dims[axis1];
                for (int j = cell.min[axis2]; j < cell.max[axis2]; j += k2) {
                    k2 = dims[axis2];

                    int xyz[] = new int[3];
                    xyz[axis] = cell.min[axis] - 1;
                    xyz[axis1] = i;
                    xyz[axis2] = j;

                    int entry = lookup_entry(entries, info.dims, info.max_snd_dim, xyz[0], xyz[1], xyz[2]);
                    Cell next_cell = cells.get(entry);
                    int next_count = next_cell.end - next_cell.begin;

                    if(OVERLAP_SUBSET)
                        if (is_subset(refs, cell.begin, count,
                                        next_cell.begin, next_count)) 
                        {
                            dmin = max(dmin, next_cell.min[axis] - cell.min[axis]);
                        } 
                        else 
                        {
                            dmin = 0;
                            break;
                        }
                    else
                    {
                        dmin = max(dmin, next_cell.min[axis] - cell.min[axis]);

                        int first_ref = cell.begin;
                        for (int p = next_cell.begin; p < next_cell.end; p++) {
                            int ref = refs.get(p);
                            int found = bisection(refs, first_ref, cell.end - first_ref, ref);
                            first_ref = found + 1 + first_ref;
                            // If the reference is not in the cell we try to expand
                            if (found < 0) {
                                Tri tri = tris.get(ref);
                                Point3f cur_min = min_bb;
                                int a = dmin, b = -1;
                                // Using bisection, find the offset by which we can overlap the neighbour
                                while (a <= b) {
                                    int m = (a + b) / 2; 
                                    cur_min.setIndex(axis, info.bbox.minimum.get(axis) + cell_size.get(axis) * (cell.min[axis] + m));
                                    if (tri_overlap_box(true, true,
                                            tri.v0, tri.e1, tri.e2, tri.normal(), cur_min, max_bb)) {
                                        a = m + 1;
                                    } else {
                                        b = m - 1;
                                    }
                                }
                                dmin = b + 1;
                                if (dmin == 0) break;
                            }
                        }
                        if (dmin == 0) break;
                    }

                    k2 = next_cell.max[axis2] - j;
                    k1 = min(k1, next_cell.max[axis1] - i);
                }
            }
        }
        
        if (cell.max[axis] < dims[axis]) {
            dmax = dims[axis];

            int k1, k2;
            for (int i = cell.min[axis1]; i < cell.max[axis1] && dmax > 0; i += k1) {
                k1 = dims[axis1];
                for (int j = cell.min[axis2]; j < cell.max[axis2]; j += k2) {
                    k2 = dims[axis2];

                    int xyz[] = new int[3];
                    xyz[axis] = cell.max[axis];
                    xyz[axis1] = i;
                    xyz[axis2] = j;

                    int entry = lookup_entry(entries, info.dims, info.max_snd_dim, xyz[0], xyz[1], xyz[2]);
                    Cell next_cell = cells.get(entry);
                    int next_count = next_cell.end - next_cell.begin;

                    if(OVERLAP_SUBSET)
                    {
                        if (is_subset(refs, cell.begin, count,
                                      next_cell.begin, next_count)) {
                            dmax = min(dmax, next_cell.max[axis] - cell.max[axis]);
                        } else {
                            dmax = 0;
                            break;
                        }
                    }
                    else
                    {
                        dmax = min(dmax, next_cell.max[axis] - cell.max[axis]);

                        int first_ref = cell.begin;
                        for (int p = next_cell.begin; p < next_cell.end; p++) {
                            int ref = refs.get(p);
                            int found = bisection(refs, first_ref, cell.end - first_ref, ref);
                            first_ref = found + 1 + first_ref;
                            // If the reference is not in the cell we try to expand
                            if (found < 0) {
                                Tri tri = tris.get(ref);
                                Point3f cur_max = max_bb;
                                int a = 1, b = dmax;
                                // Using bisection, find the offset by which we can overlap the neighbour
                                while (a <= b) {
                                    int m = (a + b) / 2;
                                    cur_max.setIndex(axis, info.bbox.minimum.get(axis) + cell_size.get(axis) * (cell.max[axis] + m));
                                    if (tri_overlap_box(true, true, tri.v0, tri.e1, tri.e2, tri.normal(), min_bb, cur_max)) {
                                        b = m - 1;
                                    } else {
                                        a = m + 1;
                                    }
                                }
                                dmax = a - 1;
                                if (dmax == 0) break;
                            }
                        }
                        if (dmax == 0) break;
                    }
                    k2 = next_cell.max[axis2] - j;
                    k1 = min(k1, next_cell.max[axis1] - i);
                }
            }
        }

        overlap.dmin = dmin;
        overlap.dmax = dmax;
    }
    
    public static int optimize_overlap(
            GridInfo info,
            List<Integer> entries,
            List<Integer> refs,
            List<Tri> tris,
            List<Boolean> cell_flags,
            List<Cell> cells) 
    {
        AtomicInteger overlaps = new AtomicInteger(0);
        parallelFor(0, cells.size(), (i)->{
            if (!cell_flags.get(i)) 
                return;
            Cell cell = cells.get(i);
            int k = 0;
            
            Overlap overlap = new Overlap();
            find_overlap(0, info, entries, refs, tris, cells, i, overlap);
            cell.min[0] += overlap.dmin;
            cell.max[0] += overlap.dmax;
            k += (overlap.dmin < 0 | overlap.dmax > 0) ? 1 : 0;

        });
        return overlaps.get();
    }
}

