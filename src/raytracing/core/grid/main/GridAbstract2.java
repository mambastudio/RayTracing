/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid.main;

import raytracing.primitive.Triangle;
import coordinate.utility.Value3Di;
import static java.lang.Math.cbrt;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.util.Arrays;
import raytracing.core.coordinate.BoundingBox;
import raytracing.core.coordinate.Point3f;
import raytracing.core.coordinate.Vector3f;
import raytracing.primitive.TriangleMesh;

/**
 *
 * @author user
 */
public class GridAbstract2 {
    protected Value3Di grid_dims;
    protected BoundingBox  grid_bbox;
    protected Vector3f  cell_size;
    protected int   grid_shift;
    
    /// Computes grid dimensions based on the formula by Cleary et al.
    public Value3Di compute_grid_dims(BoundingBox bb, int num_prims, float density) {
        Vector3f extents = bb.extents();
        float volume = extents.x * extents.y * extents.z;
        float ratio = (float) cbrt(density * num_prims / volume);
        return Value3Di.max(new Value3Di(1), new Value3Di(
                (int)(extents.x * ratio),
                (int)(extents.y * ratio), 
                (int)(extents.z * ratio)));
    }
    
    /// Computes the range of cells that intersect the given box
    public Range compute_range(Value3Di dims, BoundingBox grid_bb, BoundingBox obj_bb) {
        
        Vector3f inv = new Vector3f(dims).div(grid_bb.extents());
             
        int lx = max((int)((obj_bb.minimum.x - grid_bb.minimum.x) * inv.x), 0);
        int ly = max((int)((obj_bb.minimum.y - grid_bb.minimum.y) * inv.y), 0);
        int lz = max((int)((obj_bb.minimum.z - grid_bb.minimum.z) * inv.z), 0);
        int hx = min((int)((obj_bb.maximum.x - grid_bb.minimum.x) * inv.x), dims.x - 1);
        int hy = min((int)((obj_bb.maximum.y - grid_bb.minimum.y) * inv.y), dims.y - 1);
        int hz = min((int)((obj_bb.maximum.z - grid_bb.minimum.z) * inv.z), dims.z - 1);
        
        
        
        return new Range(lx, ly, lz, hx, hy, hz);
    }
    
    public long ballot_sync(boolean[] array)
    {
        long i = 0;
        for(boolean b:array)i=i*2+(b?1:0);
        return i;
    }
    
    /// Given a position on the virtual grid, return the corresponding top-level cell index
    public int top_level_cell(Value3Di pos) {
        return (pos.x >> grid_shift) + grid_dims.x * ((pos.y >> grid_shift) + grid_dims.y * (pos.z >> grid_shift));
    }
    
    /// Returns a voxel map entry with the given dimension and starting index
    public Entry make_entry(int log_dim, int begin) {
        Entry e = new Entry(log_dim, begin);
        return e;
    }
    
    /// Update the logarithm of the sub-level resolution for top-level cells (after a new subdivision level)
    public void update_log_dims(IntArray log_dims, int num_top_cells) {
        for(int id = 0; id<num_top_cells; id++)
        {
            if (id >= num_top_cells) return;            
            log_dims.set(id, max(0, log_dims.get(id) - 1));
            
        }
    }
    
    /// Mark references that are kept so that they can be moved to the beginning of the array
    public void mark_kept_refs(
            IntArray cell_ids,
            Entry[] entries,
            IntArray kept_flags,
            int num_refs) {
        for(int id = 0; id<num_refs; id++)
        {            
            if (id >= num_refs) return;

            int cell_id = cell_ids.get(id);  
            
            int value = ((cell_id >= 0) && (entries[cell_id].log_dim == 0)) ? 1 : 0;            
            kept_flags.set(id, value);
        }     
        
        System.out.println(Arrays.toString(entries));
    }
    
    /// Update the entries for the one level before the current one
    public void update_entries(
            IntArray start_cell,
            Entry[] entries,
            int num_cells) {
        for(int id = 0; id<num_cells; id++)
        {            
            if (id >= num_cells) return;

            int start = start_cell.get(id);
            Entry entry = entries[id];

            // If the cell is subdivided, write the first sub-cell index into the current entry
            entry.begin = entry.log_dim != 0 ? start : id;
            entries[id] = entry;
        }
    }
    
    /// Count the (sub-)dimensions of each cell, based on the array of references
    public void compute_dims(
            IntArray  cell_ids,
            Cell[] cells,
            IntArray log_dims,
            Entry[] entries,
            int num_refs) {
        for(int id = 0; id<num_refs; id++)
        {            
            if (id >= num_refs) return;

            int cell_id = cell_ids.get(id);
            if (cell_id < 0) continue;

            Value3Di cell_min = cells[cell_id].min;
            int top_cell_id = top_level_cell(cell_min);
            int log_dim = log_dims.get(top_cell_id);
            
            entries[cell_id] = make_entry(min(log_dim, 1), 0);
        }
    }
    
    /// Compute an over-approximation of the number of references
    /// that are going to be generated during reference emission
    public void count_new_refs(
            BoundingBox[]  bboxes,
            IntArray counts,
            int num_prims) {
        
        for(int id = 0; id<num_prims; id++)
        {
            if (id >= num_prims) return;
            
            BoundingBox ref_bb = bboxes[id].getCopy();//load_bbox(bboxes + id);
            Range range  = compute_range(grid_dims, grid_bbox, ref_bb);
            counts.set(id, Math.max(0, range.size()));               
        }        
    }
    
    public void count_refs_per_cell(IntArray cell_ids,
                                    IntArray refs_per_cell,                                    
                                    int num_refs) {
        for(int id = 0; id<num_refs; id++)
        {
            if (id >= num_refs) return;
            int cell_id = cell_ids.get(id);
            if (cell_id >= 0) 
            {
                //atomicAdd(refs_per_cell + cell_id, 1);
                int v = refs_per_cell.get(cell_id); 
                refs_per_cell.set(cell_id, v + 1);
            }
        }
    }
    
    /// Compute the logarithm of the sub-level resolution for top-level cells
    public void compute_log_dims(
            IntArray refs_per_cell,
            IntArray log_dims,
            float snd_density,
            int num_cells) {
        
        for(int id = 0; id<num_cells; id++)
        {         
            if (id >= num_cells) return;

            Vector3f extents = grid_bbox.extents().div(new Vector3f(grid_dims));
            BoundingBox bbox = new BoundingBox();
            bbox.include(new Point3f(), extents.asPoint3f());
            
            Value3Di dims = compute_grid_dims(bbox, refs_per_cell.get(id), snd_density);
            int max_dim = max(dims.x, max(dims.y, dims.z));
            int log_dim = 31 - Integer.numberOfLeadingZeros(max_dim);
            log_dim = (1 << log_dim) < max_dim ? log_dim + 1 : log_dim;
            log_dims.set(id, log_dim);
        }
    }
    
    /// Generate cells for the top level
    public void emit_top_cells(Cell[] new_cells, int num_cells) {
        for(int id = 0; id<num_cells; id++)
        {            
            if (id >= num_cells) return;

            int x = id % grid_dims.x;
            int y = (id / grid_dims.x) % grid_dims.y;
            int z = id / (grid_dims.x * grid_dims.y);
            int inc = 1 << grid_shift;

            x <<= grid_shift;
            y <<= grid_shift;
            z <<= grid_shift;

            Cell cell = new Cell();
            cell.min = new Value3Di(x, y, z);
            cell.max = new Value3Di(x + inc, y + inc, z + inc);
            cell.begin = 0;
            cell.end   = 0;
            new_cells[id] = cell;
            
            
        }
    }
    
    public boolean intersect_prim_cell(Triangle tri, BoundingBox bbox) 
    {
        //return intersect_tri_box<false, true>(tri.v0, tri.e1, tri.e2, tri.normal(), bbox.min, bbox.max);
        //System.out.println(tri.v0()+ " " +tri.e1()+ " " + tri.e2()+ " " +tri.normal());
        //System.out.println(bbox);
        //System.out.println(tri);
        //System.out.println(Tri_Overlap_Box.tri_overlap_box(false, true, tri.v0(), tri.e1(), tri.e2(), tri.normal(), bbox.minimum, bbox.maximum));
        return Tri_Overlap_Box.tri_overlap_box(false, true, tri.v0(), tri.e1(), tri.e2(), tri.normal(), bbox.minimum, bbox.maximum);
        
    }
    
    /// Filter out references that do not intersect the cell they are in
    public void filter_refs(
            IntArray cell_ids,
            IntArray ref_ids,
            TriangleMesh prims,
            Cell[] cells,
            int num_refs) 
    {        
        for(int id = 0; id<num_refs; id++)
        {            
            if (id >= num_refs) return;

            
            Cell cell = cells[cell_ids.get(id)];
            Triangle prim = prims.getTriangle(ref_ids.get(id));
            
            BoundingBox bbox = new BoundingBox(
                    grid_bbox.minimum.add(new Vector3f(cell.min).mul(cell_size)),
                    grid_bbox.minimum.add(new Vector3f(cell.max).mul(cell_size)));     
            
            boolean intersect = intersect_prim_cell(prim, bbox);
            if (!intersect) {
                cell_ids.set(id, -1);
                ref_ids.set(id, -1);                
            }    
            
        }
    }
    
    public void compute_split_masks(IntArray cell_ids,
                                    IntArray ref_ids,
                                    TriangleMesh prims,
                                    Cell[] cells,
                                    IntArray split_masks,
                                    int num_split) {
        for(int id = 0; id<num_split; id++)
        {            
            if (id >= num_split) return;

            int cell_id = cell_ids.get(id);
            if (cell_id < 0) {
                split_masks.set(id, 0);
                continue;
            }
            int ref  =  ref_ids.get(id);
            Cell cell = cells[cell_id];            
            Triangle prim = prims.getTriangle(ref);

            Point3f cell_min = grid_bbox.minimum.add(cell_size.mul(new Vector3f(cell.min)));
            Point3f cell_max = grid_bbox.minimum.add(cell_size.mul(new Vector3f(cell.max)));
            Point3f middle = (cell_min.addS(cell_max)).mul(0.5f);

            int mask = 0xFF;

            // Optimization: Test against half spaces first
            BoundingBox ref_bb = prim.getBound();
            if (ref_bb.minimum.x > cell_max.x ||
                ref_bb.maximum.x < cell_min.x) mask  = 0;
            if (ref_bb.minimum.x >   middle.x) mask &= 0xAA;
            if (ref_bb.maximum.x <   middle.x) mask &= 0x55;
            if (ref_bb.minimum.y > cell_max.y ||
                ref_bb.maximum.y < cell_min.y) mask  = 0;
            if (ref_bb.minimum.y >   middle.y) mask &= 0xCC;
            if (ref_bb.maximum.y <   middle.y) mask &= 0x33;
            if (ref_bb.minimum.z > cell_max.z ||
                ref_bb.maximum.z < cell_min.z) mask  = 0;
            if (ref_bb.minimum.z >   middle.z) mask &= 0xF0;
            if (ref_bb.maximum.z <   middle.z) mask &= 0x0F;

            for (int i = __ffs(mask) - 1;;) {
                BoundingBox bbox = new BoundingBox(
                        new Point3f((i & 1) != 0 ? middle.x : cell_min.x,
                                    (i & 2) != 0 ? middle.y : cell_min.y,
                                    (i & 4) != 0 ? middle.z : cell_min.z),
                        new Point3f((i & 1) != 0 ? cell_max.x : middle.x,
                                    (i & 2) != 0 ? cell_max.y : middle.y,
                                    (i & 4) != 0 ? cell_max.z : middle.z));
                if (!intersect_prim_cell(prim, bbox)) mask &= ~(1 << i);

                // Skip non-intersected children
                int skip = __ffs(mask >> (i + 1));
                if (skip == 0) break;
                i += 1 + (skip - 1);
            }

            split_masks.set(id, mask);
        }
    }
    
    /// Split references according to the given array of split masks
    public void split_refs(
                    IntArray cell_ids,
                    IntArray ref_ids,
                    Entry[] entries,
                    IntArray split_masks,
                    IntArray start_split,
                    IntArray new_cell_ids,
                    IntArray new_ref_ids,
                    int num_split) {
       
        for(int id = 0; id<num_split; id++)
        {
            if (id >= num_split) return;

            int cell_id = cell_ids.get(id);            
            int ref = ref_ids.get(id); 
            int begin = entries[cell_id].begin;
            int mask  = split_masks.get(id);
            int start = start_split.get(id);
            while (mask != 0) {
                int child_id = __ffs(mask) - 1;
                mask &= ~(1 << child_id);
                new_ref_ids.set(start, ref);
                new_cell_ids.set(start, begin + child_id);
                start++;
            }
        }
    }
    
    public int __ffs(int value)
    {
        int pos = 1;
        while ((value & 1) == 0 && value != 0) {
            value >>= 1;
            pos++;
        }
        return (value == 0) ? 0 : pos;
    }
    
    public int __popc(int mask) {
        return Integer.bitCount(mask);
    }

    public void inclusiveScan(int[] array)
    {
        Arrays.parallelPrefix(array, (a, b)-> a+b);
    }
    
    public int exclusiveScan(int[] array)
    {
        Arrays.parallelPrefix(array, (a, b)-> a+b);
        System.arraycopy(array, 0, array, 1, array.length - 1);
        array[0] = 0;
        return array[array.length-1];
    }
    
    public int partition(IntArray input, IntArray output, int n, IntArray flags)
    {
        System.arraycopy(input.getWholeArray(), 0, output.getWholeArray(), 0, n);
        int start = 0;
        int mid = start;
        for(int i = start; i < n; i++)
        {
            if(flags.get(i)!=0)
            {
                swap(i, mid, output.getWholeArray());                
                mid++;
            }
        }
        return mid;
    }
    
    private void swap(int i, int j, int... arr)
    {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }
    
    protected void swap(IntArray x, IntArray y)
    {
        if(x.size() != y.size())
            throw new UnsupportedOperationException("no swap since the two arrays are not equal");
        int temp;
        for(int i = 0; i<x.size(); i++)
        {
            temp = x.get(i);
            x.set(i, y.get(i));
            y.set(i, temp);
        }
    }
}
