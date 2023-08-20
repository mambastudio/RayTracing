/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.accel.grid.offheap;

import coordinate.memory.NativeInteger;
import coordinate.memory.NativeObject;
import coordinate.memory.algorithms.SerialNativeIntegerAlgorithm;
import static java.lang.Math.max;
import raytracing.accel.grid.offheap.base.NCell;
import raytracing.accel.grid.offheap.base.NEntry;
import raytracing.accel.grid.offheap.base.NGrid;
import raytracing.accel.grid.offheap.base.NHagrid;
import raytracing.accel.grid.offheap.base.NMergeBuffers;
import raytracing.core.coordinate.Point3i;
import raytracing.core.coordinate.Vector3f;

/**
 *
 * @author user
 */
public class NMerge extends NGridAbstract{
    NHagrid hagrid;
    
    public NMerge(NHagrid hagrid)
    {
        this.hagrid = hagrid;
    }
    
    /// Computes the position of the next cell of the grid on the axis    
    public Point3i next_cell(int axis, Point3i min, Point3i max) {
        return new Point3i(
                    axis == 0 ? max.x : min.x,
                    axis == 1 ? max.y : min.y,
                    axis == 2 ? max.z : min.z);
    }

    /// Computes the position of the previous cell of the grid on the axis    
    public Point3i prev_cell(int axis, Point3i min) {
        return new Point3i(
                    axis == 0 ? min.x - 1 : min.x,
                    axis == 1 ? min.y - 1 : min.y,
                    axis == 2 ? min.z - 1 : min.z);
    }
    
    /// Restricts the merges so that cells are better aligned for the next iteration
    public boolean merge_allowed(int empty_mask, int pos) {
        int top_level_mask      = (1 << hagrid.grid_shift) - 1; 
        boolean is_shifted      = ((pos >> hagrid.grid_shift) & empty_mask) != 0;
        boolean is_top_level    = !((pos & top_level_mask) != 0);
        return !is_shifted || !is_top_level;
    }
    
    public boolean aligned(int axis, NCell cell1, NCell cell2) {
        int axis1 = (axis + 1) % 3;
        int axis2 = (axis + 2) % 3;

        return cell1.max.get(axis)  == (cell2.min.get(axis) ) &&
               cell1.min.get(axis1) == (cell2.min.get(axis1)) &&    
               cell1.min.get(axis2) == (cell2.min.get(axis2)) &&
               cell1.max.get(axis1) == (cell2.max.get(axis1)) && 
               cell1.max.get(axis2) == (cell2.max.get(axis2));
               
    }
    
    /// Counts the number of elements in the union of two sorted arrays
    public int count_union(NativeInteger p0, int c0,
                           NativeInteger p1, int c1) {
         int i = 0, j = 0, c = 0;
         while (i < c0 & j < c1) {
             int a = p0.get(i);
             int b = p1.get(j);
             i += (a <= b) ? 1 : 0;
             j += (a >= b) ? 1 : 0;
             c++;
         }
         return c + (c1 - j) + (c0 - i);
    }
    
    /// Merges the two sorted reference arrays
    public void merge_refs( NativeInteger p0, int c0,
                            NativeInteger p1, int c1,
                            NativeInteger q) {
        int i = 0;
        int j = 0;
        int s = 0;
        while (i < c0 && j < c1) {
            int a = p0.get(i);
            int b = p1.get(j);
            q.set(s++, (a < b) ? a : b);
            i += (a <= b) ? 1 : 0;
            j += (a >= b) ? 1 : 0;
        }
        int k = i < c0 ? i  :  j;
        int c = i < c0 ? c0 : c1;
        NativeInteger p = i < c0 ? p0 : p1;
        
        while (k < c) 
            q.set(s++, p.get(k++));
        
    }
    
    public void compute_merge_counts(
                                    int axis,
                                    NativeInteger entries,
                                    NativeObject<NCell> cells,
                                    NativeInteger refs,
                                    NativeInteger merge_counts,
                                    NativeInteger nexts,
                                    NativeInteger prevs,
                                    int empty_mask,
                                    int num_cells) {       
        for(int id = 0; id<num_cells; id++)
        {            
            if (id >= num_cells) return;

            final float unit_cost = 1.0f;
            
            NCell cell1 = cells.get(id);
            Point3i next_pos = next_cell(axis, cell1.min, cell1.max);            
            int count = -(cell1.end - cell1.begin + 1);
            int next_id = -1;    
            
            if (merge_allowed(empty_mask, cell1.min.get(axis)) && 
                    next_pos.get(axis) < hagrid.grid_dims.get(axis)) {               
                next_id = lookup_entry(entries, hagrid.grid_shift, hagrid.grid_dims.rightShift(hagrid.grid_shift), next_pos);
                NCell cell2 = cells.get(next_id);
                
                if (aligned(axis,cell1, cell2)) {
                    Vector3f e1 = hagrid.cell_size.mul(new Vector3f(cell1.max.sub(cell1.min)));
                    Vector3f e2 = hagrid.cell_size.mul(new Vector3f(cell2.max.sub(cell2.min)));
                    float a1 = e1.x * (e1.y + e1.z) + e1.y * e1.z;
                    float a2 = e2.x * (e2.y + e2.z) + e2.y * e2.z;
                    float a  = a1 + a2 - e1.get((axis + 1) % 3) * e1.get((axis + 2) % 3);

                    int n1 = cell1.end - cell1.begin;
                    int n2 = cell2.end - cell2.begin;
                    float c1 = a1 * (n1 + unit_cost);
                    float c2 = a2 * (n2 + unit_cost);
                    // Early exit test: there is a minimum of max(n1, n2)
                    // primitives in the union of the two cells
                    if (a * (max(n1, n2) + unit_cost) <= c1 + c2) {
                        int n = count_union(refs.offsetMemory(cell1.begin), n1,
                                            refs.offsetMemory(cell2.begin), n2);
                        float c = a * (n + unit_cost);
                        if (c <= c1 + c2) count = n;
                        
                        
                    }
                }
            }
            
            merge_counts.set(id, count);
                        
            next_id = count >= 0 ? next_id : -1;
            nexts.set(id, next_id);           
            if(next_id >= 0) prevs.set(next_id, id);
        }        
    }
    
    public void compute_cell_flags(NativeInteger nexts,
                                   NativeInteger prevs,
                                   NativeInteger cell_flags,
                                   int num_cells) {
        
        for(int id = 0; id<num_cells; id++)
        {            
            if (id >= num_cells) return;

            // If the previous cell does not exist or does not want to merge with this cell
            if (prevs.get(id) < 0) {
                int next_id = nexts.get(id);
                cell_flags.set(id, 1);
                
                // If this cell wants to merge with the next
                if (next_id >= 0) {
                    int count = 1;

                    // Traverse the merge chain
                    do { 
                        cell_flags.set(next_id, (count % 2) != 0 ? 0 : 1);
                        next_id = nexts.get(next_id);
                        count++;
                    } while (next_id >= 0);
                }
            }
        }
    }
    
    /// Computes the number of new references per cell
    public void compute_ref_counts(NativeInteger merge_counts,
                                   NativeInteger cell_flags,
                                   NativeInteger ref_counts,
                                       int num_cells) 
    {
        for(int id = 0; id<num_cells; id++)
        {            
            if (id >= num_cells) return;

            int count = 0;
            if (cell_flags.get(id) != 0) {
                int merged = merge_counts.get(id);
                count = merged >= 0 ? merged : -(merged + 1);
            }
            ref_counts.set(id, count);
        }
    }
    
    /// Maps the old cell indices in the voxel map to the new ones
    public void remap_entries(NativeInteger entries,
                              NativeInteger new_cell_ids,
                              int num_entries) {
        for(int id = 0; id<num_entries; id++)
        {            
            if (id < num_entries) {
                NEntry entry = entries.get(id, new NEntry());                
                if (entry.log_dim == 0) entries.set(id, make_entry(0, new_cell_ids.get(entry.begin)));
            }
        }
    }
    
    public void merge(int axis,
                      NativeInteger entries,
                      NativeObject<NCell> cells,
                      NativeInteger refs,
                      NativeInteger cell_scan,
                      NativeInteger ref_scan,
                      NativeInteger merge_counts,
                      NativeInteger new_cell_ids,
                      NativeObject<NCell> new_cells,
                      NativeInteger new_refs,
                      int num_cells) {
                
        for (int id = 0; id < num_cells; id++) {
            boolean valid = id < num_cells; 
            int new_id = valid ? cell_scan.get(id) : 0;
            valid &= cell_scan.get(id + 1) > new_id;
            
            

            int cell_begin = 0, cell_end = 0;
            int next_begin = 0, next_end = 0;
            int new_refs_begin = -1;
             
            if (valid) {
                NCell cell = cells.get(id);
                int merge_count = merge_counts.get(id);

                new_refs_begin = ref_scan.get(id);
                new_cell_ids.set(id, new_id);
                cell_begin = cell.begin;
                cell_end   = cell.end;

                Point3i new_min;
                Point3i new_max;
                int new_refs_end;
                if (merge_count >= 0) {
                    // Do the merge and store the references into the new array
                    int next_id = lookup_entry(entries, hagrid.grid_shift, hagrid.grid_dims.rightShift(hagrid.grid_shift), next_cell(axis, cell.min, cell.max));
                    NCell next_cell = cells.get(next_id);
                    next_begin = next_cell.begin;
                    next_end   = next_cell.end;

                    // Make the next cell point to the merged one
                    new_cell_ids.set(next_id, new_id);

                    new_min = Point3i.min(next_cell.min, cell.min);
                    new_max = Point3i.max(next_cell.max, cell.max);
                    new_refs_end = new_refs_begin + merge_count;
                } else {
                   new_min = cell.min;
                   new_max = cell.max;
                   new_refs_end = new_refs_begin + (cell_end - cell_begin);                    
                }
                                
                new_cells.set(new_id, new NCell( new_min, new_refs_begin,
                                                 new_max, new_refs_end));
            }
            
            boolean merge = next_begin < next_end;
            
            if(!merge)
            {
                if(cell_begin < cell_end)
                {                   
                    int new_begin = new_refs_begin;                                        
                    for (int i = cell_begin; i < cell_end; i++, new_begin++)
                    {                       
                        new_refs.set(new_begin, refs.get(i));                
                    }                    
                }
            }
            else
            {
                // Merge references if required   
                merge_refs(refs.offsetMemory(cell_begin), cell_end - cell_begin,
                           refs.offsetMemory(next_begin), next_end - next_begin,
                           new_refs.offsetMemory(new_refs_begin));
            }            
        }       
    }
    
    public void merge_iteration(int axis, NGrid grid, NativeObject<NCell> new_cells, NativeInteger new_refs, int empty_mask, NMergeBuffers bufs)
    {        
        SerialNativeIntegerAlgorithm par = new SerialNativeIntegerAlgorithm();        
        
        int num_cells               = grid.num_cells;
        int num_entries             = grid.num_entries;
        NativeObject<NCell> cells   = grid.cells;
        NativeInteger refs          = grid.ref_ids;
        NativeInteger entries       = grid.entries;
                
        bufs.prevs.fill(-1, num_cells); //be careful here. The original code shows fillOne, which is counterintuitive
        compute_merge_counts(axis, entries, cells, refs, bufs.merge_counts, bufs.nexts, bufs.prevs, empty_mask, num_cells);     
        compute_cell_flags(bufs.nexts, bufs.prevs, bufs.cell_flags, num_cells);  
        compute_ref_counts(bufs.merge_counts, bufs.cell_flags, bufs.ref_counts, num_cells); 
         
        int num_new_refs  = par.exclusive_scan(bufs.ref_counts, num_cells + 1, bufs.ref_scan);
        int num_new_cells = par.exclusive_scan(bufs.cell_flags, num_cells + 1, bufs.cell_scan);
                
        merge(  axis,entries, cells, refs,
                bufs.cell_scan, bufs.ref_scan,
                bufs.merge_counts, bufs.new_cell_ids,
                new_cells, new_refs,
                num_cells);
        
        remap_entries(entries, bufs.new_cell_ids, num_entries);
                        
        new_cells.swap(cells);
        new_refs.swap(refs);
        
        grid.cells     = cells;
        grid.ref_ids   = refs;
        grid.num_cells = num_new_cells;
        grid.num_refs  = num_new_refs;       
    }
    
     /// Performs the neighbor merging optimization (merging cells according to the SAH).
    public void merge_grid()
    {        
        NMergeBuffers bufs = new NMergeBuffers();
                
        NativeObject<NCell> new_cells = new NativeObject(NCell.class, hagrid.grid().num_cells);        
        NativeInteger       new_refs  = new NativeInteger(hagrid.grid().num_refs);
        
        int buf_size = hagrid.grid().num_cells + 1;
        buf_size = (buf_size % 4) != 0 ? buf_size + 4 - buf_size % 4 : buf_size;
        
        bufs.merge_counts = new NativeInteger(buf_size);
        bufs.ref_counts   = new NativeInteger(buf_size);
        bufs.cell_flags   = new NativeInteger(buf_size);
        bufs.cell_scan    = new NativeInteger(buf_size);
        bufs.ref_scan     = new NativeInteger(buf_size);
        bufs.new_cell_ids = bufs.cell_flags;
        bufs.prevs        = bufs.cell_scan;
        bufs.nexts        = bufs.ref_scan;
                
        Vector3f extents = hagrid.grid().bbox.extents();
        Point3i dims = hagrid.grid().dims.leftShift(hagrid.grid().shift);
        Vector3f cell_size = extents.div(new Vector3f(dims));
        
        hagrid.grid_dims = dims;
        hagrid.cell_size = cell_size;
        hagrid.grid_shift = hagrid.grid().shift;
        
        if (hagrid.alpha > 0) {
            int prev_num_cells, iter = 0;
            do {                
                prev_num_cells = hagrid.grid().num_cells;
                int mask = iter > 3 ? 0 : (1 << (iter + 1)) - 1;           
                merge_iteration(0, hagrid.grid(), new_cells, new_refs, mask, bufs);                
                merge_iteration(1, hagrid.grid(), new_cells, new_refs, mask, bufs);              
                merge_iteration(2, hagrid.grid(), new_cells, new_refs, mask, bufs);                  
                iter++;     
            } while (hagrid.grid().num_cells < hagrid.alpha * prev_num_cells);                
        }
        
        bufs.merge_counts.freeMemory();
        bufs.ref_counts.freeMemory();
        bufs.cell_flags.freeMemory();
        bufs.cell_scan.freeMemory();
        bufs.ref_scan.freeMemory();

        new_cells.freeMemory();
        new_refs.freeMemory();
    }
}
