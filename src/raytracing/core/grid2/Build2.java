/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid2;

import coordinate.memory.NativeInteger;
import coordinate.memory.NativeObject;
import coordinate.memory.algorithms.ParallelNative;
import coordinate.memory.algorithms.SerialNativeIntegerAlgorithm;
import coordinate.memory.algorithms.SerialNativeObjectAlgorithm;
import static java.lang.Math.cbrt;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.util.ArrayList;
import raytracing.core.coordinate.BoundingBox;
import raytracing.core.coordinate.Point3f;
import raytracing.core.coordinate.Point3i;
import raytracing.core.coordinate.Vector3f;
import raytracing.core.grid.base.Range;
import raytracing.core.grid2.base.Cell2;
import raytracing.core.grid2.base.Entry2;
import raytracing.core.grid2.base.Grid2;
import raytracing.core.grid2.base.Hagrid2;
import raytracing.core.grid2.base.Level2;
import raytracing.primitive.Triangle;
import raytracing.primitive.TriangleMesh;

/**
 *
 * @author user
 */
public class Build2 extends GridAbstract2{
    Hagrid2 hagrid;
    
    public Build2(Hagrid2 hagrid)
    {
        this.hagrid = hagrid;
    }
    
    /// Given a position on the virtual grid, return the corresponding top-level cell index
    public int top_level_cell(Point3i pos) {
        return (pos.x >> hagrid.grid_shift) + hagrid.grid_dims.x * ((pos.y >> hagrid.grid_shift) + hagrid.grid_dims.y * (pos.z >> hagrid.grid_shift));
    }
    
    /// Update the logarithm of the sub-level resolution for top-level cells (after a new subdivision level)
    public void update_log_dims(NativeInteger log_dims, int num_top_cells) {
        for(int id = 0; id<num_top_cells; id++)
        {
            if (id >= num_top_cells) return;            
            log_dims.set(id, max(0, log_dims.get(id) - 1));            
        }
        
    }
    
    /// Mark references that are kept so that they can be moved to the beginning of the array
    public void mark_kept_refs(
            NativeInteger cell_ids,
            NativeInteger entries,
            NativeInteger kept_flags,
            int num_refs) {
        for(int id = 0; id<num_refs; id++)
        {            
            if (id >= num_refs) return;

            int cell_id = cell_ids.get(id);  
            
            int value = ((cell_id >= 0) && (entries.get(cell_id, new Entry2()).log_dim == 0)) ? 1 : 0;              
            kept_flags.set(id, value);
        }             
    }
    
    /// Update the entries for the one level before the current one
    public void update_entries(
            NativeInteger start_cell,
            NativeInteger entries,
            int num_cells) {
        for(int id = 0; id<num_cells; id++)
        {            
            if (id >= num_cells) return;

            int start = start_cell.get(id);
            Entry2 entry = entries.get(id, new Entry2());

            // If the cell is subdivided, write the first sub-cell index into the current entry
            entry.begin = entry.log_dim != 0 ? start : id;
            entries.set(id, entry);
        }
    }
    
    /// Computes the range of cells that intersect the given box
    public Range compute_range(Point3i dims, BoundingBox grid_bb, BoundingBox obj_bb) {
        
        Vector3f inv = hagrid.grid().grid_inv();
        
        int lx = max((int)((obj_bb.minimum.x - grid_bb.minimum.x) * inv.x), 0);
        int ly = max((int)((obj_bb.minimum.y - grid_bb.minimum.y) * inv.y), 0);
        int lz = max((int)((obj_bb.minimum.z - grid_bb.minimum.z) * inv.z), 0);
        int hx = min((int)((obj_bb.maximum.x - grid_bb.minimum.x) * inv.x), dims.x - 1);
        int hy = min((int)((obj_bb.maximum.y - grid_bb.minimum.y) * inv.y), dims.y - 1);
        int hz = min((int)((obj_bb.maximum.z - grid_bb.minimum.z) * inv.z), dims.z - 1);
        
        
        return new Range(lx, ly, lz, hx, hy, hz);
    }
    
    public void compute_bboxes(
            TriangleMesh prims,
            NativeObject<BoundingBox> bboxes,
            int num_prims) 
    {
        for(int i = 0; i<num_prims; i++)
            bboxes.set(i, prims.getBound(i));
    }
    
    /// Computes grid dimensions based on the formula by Cleary et al.
    public Point3i compute_grid_dims(BoundingBox bb, int num_prims, float density) {
        Vector3f extents = bb.extents();
        extents.x = extents.x != 0 ? extents.x : 1;
        extents.y = extents.y != 0 ? extents.y : 1;
        extents.z = extents.z != 0 ? extents.z : 1;
        
        float volume = extents.x * extents.y * extents.z;
        
        float ratio = (float) cbrt(density * num_prims / volume);
        return Point3i.max(new Point3i(1), new Point3i(
                extents.x * ratio,
                extents.y * ratio,
                extents.z * ratio));        
    }
    
    /// Compute an over-approximation of the number of references
    /// that are going to be generated during reference emission
    public void count_new_refs(
            NativeObject<BoundingBox>  bboxes,
            NativeInteger counts,
            int num_prims) {
        
        for(int id = 0; id<num_prims; id++)
        {
            if (id >= num_prims) return;
            
            BoundingBox ref_bb = bboxes.get(id);//load_bbox(bboxes + id);
            Range range  = compute_range(hagrid.grid_dims, hagrid.grid_bbox, ref_bb);            
            counts.set(id, Math.max(0, range.size()));               
        }    
    }
    
    /// Count the (sub-)dimensions of each cell, based on the array of references
    public void compute_dims(
            NativeInteger  cell_ids,
            NativeObject<Cell2> cells,
            NativeInteger log_dims,
            NativeInteger entries,
            int num_refs) {
        
        for(int id = 0; id<num_refs; id++)
        {            
            if (id >= num_refs) return;

            int cell_id = cell_ids.get(id);
            if (cell_id < 0) continue;
            
            Point3i cell_min = cells.get(cell_id).min;           
                        
            int top_cell_id = top_level_cell(cell_min);
            int log_dim = log_dims.get(top_cell_id);
            
            entries.set(cell_id, make_entry(min(log_dim, 1), 0));
        }       
    }
    
    public void compute_split_masks(NativeInteger cell_ids,
                                    NativeInteger ref_ids,
                                    TriangleMesh prims,
                                    NativeObject<Cell2> cells,
                                    NativeInteger split_masks,
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
            Cell2 cell = cells.get(cell_id).copy();          
            
            Triangle prim = prims.getTriangle(ref);

            Point3f cell_min = hagrid.grid_bbox.minimum.add(hagrid.cell_size.mul(new Vector3f(cell.min)));
            Point3f cell_max = hagrid.grid_bbox.minimum.add(hagrid.cell_size.mul(new Vector3f(cell.max)));
            Point3f middle = (cell_min.addS(cell_max)).mul(0.5f);

            int mask = 0xFF;

            // Optimization: Test against half spaces first
            BoundingBox ref_bb = prim.getBound();            
            if (ref_bb.minimum.x > cell_max.x ||
                ref_bb.maximum.x < cell_min.x) mask  = 0;
            if (ref_bb.minimum.x >   middle.x) mask &= 0xAA; //10101010
            if (ref_bb.maximum.x <   middle.x) mask &= 0x55; //01010101
            if (ref_bb.minimum.y > cell_max.y ||
                ref_bb.maximum.y < cell_min.y) mask  = 0;
            if (ref_bb.minimum.y >   middle.y) mask &= 0xCC; //11001100
            if (ref_bb.maximum.y <   middle.y) mask &= 0x33; //00110011  
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
                if (!prim.triangleBoxIntersection(bbox)) mask &= ~(1 << i);

                // Skip non-intersected children
                int skip = __ffs(mask >> (i + 1));
                if (skip == 0) break;
                i += 1 + (skip - 1);
            }

            split_masks.set(id, mask);
        }
    }
    
    /// Copy the references with an offset, different for each level
    public void copy_refs(
                            NativeInteger cell_ids,
                            NativeInteger new_cell_ids,
                            int cell_off,
                            int num_kept) {
        for(int id = 0; id<num_kept; id++)
        {        
            if (id >= num_kept) return;
            new_cell_ids.set(id, cell_ids.get(id) + cell_off);
        }
    }
    
    /// Remap references so that they map to the correct cells
    public void remap_refs(
                        NativeInteger cell_ids,
                        NativeInteger start_cell,
                        int num_refs) {
        for(int id = 0; id<num_refs; id++)
        {
            if (id >= num_refs) return;
            cell_ids.set(id, start_cell.get(cell_ids.get(id)));
        }
    }
    
    /// Mark the cells that are used as 'kept'
    public void mark_kept_cells(
                                NativeInteger entries,
                                NativeInteger kept_cells,
                                int num_cells) {        
        for(int id = 0; id<num_cells; id++)
        {
            if (id >= num_cells) return;
            kept_cells.set(id, (entries.get(id, new Entry2()).log_dim == 0) ? 1 : 0);
        }
    }
    
    /// Copy only the cells that are kept to another array of cells
    public void copy_cells(
                            NativeObject<Cell2> cells,
                            NativeInteger start_cell,
                            NativeObject<Cell2> new_cells,
                            int cell_off,
                            int num_cells) {
        for(int id = 0; id<num_cells; id++)
        {
            if (id >= num_cells) return;

            Cell2 cell = cells.get(id).copy();
            int start = start_cell.get(cell_off + id + 0);
            int end   = start_cell.get(cell_off + id + 1);
            if (start < end) 
                new_cells.set(start, cell);                
        }
    }
    
    /// Copy the voxel map entries and remap kept cells to their correct indices
    public void copy_entries(NativeInteger entries,
                             NativeInteger start_cell,
                             NativeObject<Entry2> new_entries,
                             int cell_off,
                             int next_level_off,
                             int num_cells) {
        for(int id = 0; id<num_cells; id++)
        {
            if (id >= num_cells) return;

            Entry2 entry = entries.get(id, new Entry2());
            if (entry.log_dim == 0) {
                // Points to a cell
                entry.begin = start_cell.get(cell_off + entry.begin);
            } else {
                // Points to another entry in the next level
                entry.begin += next_level_off;
            }
            new_entries.set(id + cell_off, entry);
        }
    }
    
    /// Sets the cell ranges once the references are sorted by cell
    public void compute_cell_ranges(NativeInteger cell_ids, NativeObject<Cell2> cells, int num_refs) {
        
        for(int id = 0; id<num_refs; id++)
        {
            if (id >= num_refs) return;
            
            int cell_id = cell_ids.get(id + 0);
            if (id >= num_refs - 1) {
                Cell2 cell_ = cells.get(cell_id);
                cell_.end = id + 1;
                cells.set(cell_id, cell_);
                return;
            }
            int next_id = cell_ids.get(id + 1);
            
            if (cell_id != next_id) {
                Cell2 cell_1 = cells.get(cell_id);
                Cell2 cell_2 = cells.get(next_id);
                cell_1.end = id + 1;
                cell_2.begin = id + 1;
                cells.set(cell_id, cell_1);
                cells.set(next_id, cell_2);                
            }
        }
    }
    
    /// Emit the new references by inserting existing ones into the sub-levels
    public void emit_new_refs(
            NativeObject<BoundingBox> bboxes,
            NativeInteger start_emit, //this is a prefix sum
            NativeInteger new_ref_ids,
            NativeInteger new_cell_ids,
            int num_prims){              
        for(int id = 0; id<num_prims; id++)
        {
            int start = start_emit.get(id + 0);
            int end   = start_emit.get(id + 1);
            Range range;
            //Very strange method. In the cuda code, it seems quite complex
            if (start < end) 
            {
                BoundingBox ref_bb = bboxes.get(id).copy(); 
                range  = compute_range(hagrid.grid().grid_dims(), hagrid.grid().bbox, ref_bb);
                             
                int x = range.lx;
                int y = range.ly;    
                int z = range.lz;
                int cur = start;
                while (cur < end) 
                {                    
                    new_ref_ids.set(cur, id);
                    new_cell_ids.set(cur, x + hagrid.grid_dims.x * (y + hagrid.grid_dims.y * z));
                    cur++; 
                    x++;
                    if (x > range.hx) { x = range.lx; y++; }
                    if (y > range.hy) { y = range.ly; z++; } 
                }                
            }            
        }        
    }
    
    public void count_refs_per_cell(NativeInteger cell_ids,
                                    NativeInteger refs_per_cell,                                    
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
            NativeInteger refs_per_cell,
            NativeInteger log_dims,
            float snd_density,
            int num_cells) {       
        for(int id = 0; id<num_cells; id++)
        {         
            if (id >= num_cells) return;

            Vector3f extents = hagrid.grid_bbox.extents().div(new Vector3f(hagrid.grid_dims));
            BoundingBox bbox = new BoundingBox(new Point3f(), extents.asPoint3f());
            
            Point3i dims = compute_grid_dims(bbox, refs_per_cell.get(id), snd_density);
            int max_dim = max(dims.x, max(dims.y, dims.z));
            int log_dim = log2nlz(max_dim); //log2
            log_dim = (1 << log_dim) < max_dim ? log_dim + 1 : log_dim;            
            log_dims.set(id, log_dim);                   
        }  
    }
    
    /// Generate cells for the top level
    public void emit_top_cells(NativeObject<Cell2> new_cells, int num_cells) {
        for(int id = 0; id<num_cells; id++)
        {            
            if (id >= num_cells) return;

            int x = id % hagrid.grid_dims.x;
            int y = (id / hagrid.grid_dims.x) % hagrid.grid_dims.y;
            int z = id / (hagrid.grid_dims.x * hagrid.grid_dims.y);
            int inc = 1 << hagrid.grid_shift;
            
            x <<= hagrid.grid_shift;
            y <<= hagrid.grid_shift;
            z <<= hagrid.grid_shift;

            Cell2 cell = new Cell2();
            cell.min = new Point3i(x, y, z);
            cell.max = new Point3i(x + inc, y + inc, z + inc);
            cell.begin = 0;
            cell.end   = 0;
            new_cells.set(id, cell);            
        }
    }
    
    /// Filter out references that do not intersect the cell they are in
    public void filter_refs(
            NativeInteger cell_ids,
            NativeInteger ref_ids,
            TriangleMesh prims,
            NativeObject<Cell2> cells,
            int num_refs) 
    {        
        
        for(int id = 0; id<num_refs; id++)
        {            
            if (id >= num_refs) return;

            
            Cell2 cell = cells.get(cell_ids.get(id));
            Triangle prim = prims.getTriangle(ref_ids.get(id)); 
            
            BoundingBox bbox = new BoundingBox(
                    hagrid.grid_bbox.minimum.add(new Vector3f(cell.min).mul(hagrid.cell_size)),
                    hagrid.grid_bbox.minimum.add(new Vector3f(cell.max).mul(hagrid.cell_size)));    
            
            boolean intersect = prim.triangleBoxIntersection(bbox);//prim.planeBoxIntersection(bbox);
            if (!intersect) {
                cell_ids.set(id, -1);
                ref_ids.set(id, -1);                
            }                
        }
    }
    
    /// Split references according to the given array of split masks
    public void split_refs(
                    NativeInteger           cell_ids,
                    NativeInteger           ref_ids,
                    NativeInteger           entries,
                    NativeInteger           split_masks,
                    NativeInteger           start_split,
                    NativeInteger           new_cell_ids,
                    NativeInteger           new_ref_ids,
                    int num_split) { 
        for(int id = 0; id<num_split; id++)
        {
            if (id >= num_split) return;
            
            int cell_id = cell_ids.get(id); 
            
            //Not in the code
            if(cell_id < 0)
               continue;
            
            int ref = ref_ids.get(id); 
            int begin = entries.get(cell_id, new Entry2()).begin;
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
    
    /// Generate new cells based on the previous level
    public void emit_new_cells(NativeInteger entries,
                               NativeObject<Cell2> cells,
                               NativeObject<Cell2> new_cells,
                               int num_cells) {
        for(int id = 0; id<num_cells; id++)
        {
            if (id >= num_cells) return;

            Entry2 entry = entries.get(id, new Entry2());
            int log_dim = entry.log_dim;
            if (log_dim == 0) continue;            

            int start = entry.begin;
            Cell2 cell = cells.get(id);
            int min_x = cell.min.x;
            int min_y = cell.min.y;
            int min_z = cell.min.z;
            int inc = (cell.max.x - cell.min.x) >> 1;
            
            for (int i = 0; i < 8; i++) {
                int x = min_x + (i & 1) * inc;
                int y = min_y + ((i >> 1) & 1) * inc;
                int z = min_z + (i >> 2) * inc;
                 
                cell.min = new Point3i(x, y, z);
                cell.max = new Point3i(x + inc, y + inc, z + inc);
                cell.begin = 0;
                cell.end   = 0;
                new_cells.set(start + i, cell);                    
            }
        }
    }
    
    public void first_build_iter(
            float snd_density,
            TriangleMesh prims,
            NativeObject<BoundingBox> bboxes, BoundingBox grid_bb, Point3i dims, NativeInteger log_dims, 
            int[] grid_shift, ArrayList<Level2> levels) 
    {
        SerialNativeIntegerAlgorithm par = new SerialNativeIntegerAlgorithm();
        
        int num_top_cells = dims.x * dims.y * dims.z;
       
        NativeInteger start_emit        = new NativeInteger(prims.getSize() + 1);
        NativeInteger new_ref_counts    = new NativeInteger(prims.getSize() + 1);
        NativeInteger refs_per_cell     = new NativeInteger(num_top_cells);        
        log_dims.resize(num_top_cells + 1);
        
        count_new_refs(bboxes, new_ref_counts, prims.getSize());  //new references generated based on how many cells each ref overlaps      
        
        new_ref_counts.copyToMem(start_emit); //copy to and shift to the right
        int num_new_refs = par.exclusive_scan(new_ref_counts, prims.getSize() + 1, start_emit);  
                
        // We are creating cells and their references of top level        
        NativeInteger new_ref_ids  = new NativeInteger(2 * num_new_refs); 
        NativeInteger new_cell_ids = new_ref_ids.offsetMemory(num_new_refs);   
        emit_new_refs(bboxes, start_emit, new_ref_ids, new_cell_ids, prims.getSize()); //insert the initial references into the new cells 
        
        // Compute the number of references per cell
        count_refs_per_cell(new_cell_ids, refs_per_cell, num_new_refs); //simple atomic add
        
        // Compute an independent resolution in each of the top-level cells
        compute_log_dims(refs_per_cell, log_dims, snd_density, num_top_cells);
                
        // Find the max sub-level resolution
        grid_shift[0] = par.reduce(log_dims, num_top_cells, log_dims.offsetMemory(num_top_cells), (a, b)->max(a,b));       
        hagrid.grid().shift = grid_shift[0];
        
        this.hagrid.cell_size = grid_bb.extents().div(new Vector3f(dims.leftShift(grid_shift[0])));
        this.hagrid.grid_shift = grid_shift[0];
                
        //Emission of the new cells
        NativeObject<Cell2> new_cells   = new NativeObject(Cell2.class, num_top_cells + 0);
        NativeInteger new_entries = new NativeInteger(num_top_cells + 1);
        
        emit_top_cells(new_cells, num_top_cells);
        new_entries.fill(0);
        
        // Filter out the references that do not intersect the cell they are in
        // Initially cell intersection was based on primitive bounding box, here we are more precise with primitive intersection with cell
        filter_refs(new_cell_ids, new_ref_ids, prims, new_cells, num_new_refs);
        
        Level2 level = new Level2();
        level.ref_ids   = new_ref_ids;  
        level.cell_ids  = new_cell_ids; 
        level.num_refs  = num_new_refs;   
        level.num_kept  = num_new_refs;   
        level.cells     = new_cells;   
        level.entries   = new_entries;
        level.num_cells = num_top_cells;  
        
        System.out.println(new_ref_ids);
                        
        levels.add(level);               
    }
    
    public boolean build_iter(
                TriangleMesh prims,
                Point3i dims, NativeInteger log_dims,
                ArrayList<Level2> levels,
                int iter)
    {
        SerialNativeIntegerAlgorithm par = new SerialNativeIntegerAlgorithm();
        
        NativeInteger cell_ids              = levels.get(levels.size()-1).cell_ids;
        NativeInteger ref_ids               = levels.get(levels.size()-1).ref_ids;
        NativeObject<Cell2> cells           = levels.get(levels.size()-1).cells;
        NativeInteger entries               = levels.get(levels.size()-1).entries;
        
        int num_top_cells       = dims.x * dims.y * dims.z;
        int num_refs            = levels.get(levels.size()-1).num_refs;
        int num_cells           = levels.get(levels.size()-1).num_cells;
        
        int cur_level  = levels.size();
        
        NativeInteger kept_flags = new NativeInteger(num_refs + 1);
        
        // Find out which cell will be split based on whether it is empty or not and the max depth
        compute_dims(cell_ids, cells, log_dims, entries, num_refs); 
        update_log_dims(log_dims, num_top_cells);        
        mark_kept_refs(cell_ids, entries, kept_flags, num_refs);
        
        // Store the sub-cells starting index in the entries
        NativeInteger start_cell = new NativeInteger(num_cells + 1);        
        int num_new_cells = par.exclusive_scan(
                par.transform(entries, e->new Entry2(e).log_dim == 0 ? 0 : 8), 
                num_cells + 1, start_cell);               
        
        update_entries(start_cell, entries, num_cells);   
        
        // Partition the set of cells into the sets of those which will be split and those which won't        
        NativeInteger tmp_ref_ids  = new NativeInteger(num_refs * 2);
        NativeInteger tmp_cell_ids = tmp_ref_ids.offsetMemory(num_refs);
        int num_sel_refs  = par.partition(ref_ids,  tmp_ref_ids,  num_refs, kept_flags); 
        int num_sel_cells = par.partition(cell_ids, tmp_cell_ids, num_refs, kept_flags);
        
        if(num_sel_refs != num_sel_cells)
            throw new UnsupportedOperationException("num_sel_refs is not equal to num_sel_cells");
        
        //Swap
        tmp_ref_ids.swap(ref_ids);
        tmp_cell_ids.swap(cell_ids); 
         
        int num_kept = num_sel_refs;
        levels.get(levels.size()-1).ref_ids  = ref_ids;
        levels.get(levels.size()-1).cell_ids = cell_ids;
        levels.get(levels.size()-1).num_kept = num_kept;
        
        if (num_new_cells == 0) {
            // Exit here because no new reference will be emitted            
            return false;
        }
        
        int num_split = num_refs - num_kept;
                
        // Split the references
        NativeInteger split_masks = new NativeInteger(num_split + 1);
        NativeInteger start_split = new NativeInteger(num_split + 1);
        
        /// Compute a mask for each reference which determines which sub-cell is intersected
        compute_split_masks(
                cell_ids.offsetMemory(num_kept), 
                ref_ids.offsetMemory(num_kept), 
                prims, cells, 
                split_masks, 
                num_split);
        
        // Store the sub-cells starting index in the entries              
        int num_new_refs = par.exclusive_scan(par.transform(split_masks, mask->__popc(mask)), 
                num_split+1, start_split);
      
        if(!(num_new_refs <= 8 * num_split))        
            throw new UnsupportedOperationException("num_new_refs: " +num_new_refs+ " should be <= " +8 * num_split);
                
        NativeInteger new_ref_ids = new NativeInteger(num_new_refs * 2);
        NativeInteger new_cell_ids = new_ref_ids.offsetMemory(num_new_refs);
                
        split_refs(
                cell_ids.offsetMemory(num_kept), 
                ref_ids.offsetMemory(num_kept), 
                entries, 
                split_masks, 
                start_split, 
                new_cell_ids, 
                new_ref_ids, 
                num_split);
        
        
        // Emission of the new cells
        NativeObject<Cell2> new_cells   = new NativeObject(Cell2.class, num_new_cells + 0);
        NativeInteger new_entries = new NativeInteger(num_new_cells + 1);
        emit_new_cells(entries, cells, new_cells, num_cells);
                
        new_entries.fill(0);
                                
        Level2 level = new Level2();
        level.ref_ids   = new_ref_ids;         
        level.cell_ids  = new_cell_ids;        
        level.num_refs  = num_new_refs;        
        level.num_kept  = num_new_refs;        
        level.cells     = new_cells;           
        level.entries   = new_entries;         
        level.num_cells = num_new_cells;     
        
        levels.add(level);
                
        return true;        
    }
    
    public void concat_levels(ArrayList<Level2> levels, Grid2 grid) {
        int num_levels = levels.size();
        
        // Start with references
        int total_refs = 0;
        int total_cells = 0;
        for (Level2 level : levels) {
            total_refs  += level.num_kept;
            total_cells += level.num_cells;
        }
        
        // Copy primitive references as-is
        NativeInteger ref_ids  = new NativeInteger(total_refs);
        NativeInteger cell_ids = new NativeInteger(total_refs);
        for (int i = 0, off = 0; i < num_levels; off += levels.get(i).num_kept, i++)            
            levels.get(i).ref_ids.copyToMem(ref_ids.offsetMemory(off), levels.get(i).num_kept);
        
        // Copy the cell indices with an offset       
        for (int i = 0, off = 0, cell_off = 0; i < num_levels; off += levels.get(i).num_kept, cell_off += levels.get(i).num_cells, i++) {
            int num_kept = levels.get(i).num_kept;
            if (num_kept != 0) {
                copy_refs(levels.get(i).cell_ids, cell_ids.offsetMemory(off), cell_off, num_kept);
                //DEBUG_SYNC();
            }
            levels.get(i).ref_ids = null;
        }
        
        // Mark the cells at the leaves of the structure as kept
        NativeInteger kept_cells = new NativeInteger(total_cells + 1);
        
        for (int i = 0, cell_off = 0; i < num_levels; cell_off += levels.get(i).num_cells, i++) {
            int num_cells = levels.get(i).num_cells;
            mark_kept_cells(levels.get(i).entries, kept_cells.offsetMemory(cell_off), num_cells);
            //DEBUG_SYNC();
        }
        
        // Compute the insertion position of each cell
        NativeInteger start_cell = new NativeInteger(total_cells + 1);  
        kept_cells.copyToMem(start_cell);
        int new_total_cells = ParallelNative.exclusiveScan(start_cell);
                
        // Allocate new cells, and copy only the cells that are kept
        NativeObject<Cell2> cells = new NativeObject(Cell2.class, new_total_cells);
        for (int i = 0, cell_off = 0; i < num_levels; cell_off += levels.get(i).num_cells, i++) {
            int num_cells = levels.get(i).num_cells;
            copy_cells(levels.get(i).cells, start_cell, cells, cell_off, num_cells);
            //DEBUG_SYNC();
            //mem.free(levels[i].cells);
            levels.get(i).cells.dispose();
        }
        
        NativeObject<Entry2> entries = new NativeObject(Entry2.class, total_cells);
        for (int i = 0, off = 0; i < num_levels; off += levels.get(i).num_cells, i++) {
            int num_cells = levels.get(i).num_cells;
            int next_level_off = off + num_cells;
            //copy_entries(levels.get(i).entries, start_cell, entries + off, off, next_level_off, num_cells);
        copy_entries(levels.get(i).entries, start_cell, entries, off, next_level_off, num_cells);
            //DEBUG_SYNC();
            //mem.free(levels[i].entries);
            levels.get(i).entries = null;
        }
        
        // Remap the cell indices in the references (which currently map to incorrect cells)
        remap_refs(cell_ids, start_cell, total_refs);
        //DEBUG_SYNC();
        
        // Sort the references by cell (re-use old slots whenever possible)
        NativeInteger tmp_ref_ids  = new NativeInteger(total_refs);
        NativeInteger tmp_cell_ids = new NativeInteger(total_refs);
        NativeInteger new_ref_ids = tmp_ref_ids;
        NativeInteger new_cell_ids = tmp_cell_ids;        
        ParallelNative.sort_pair(cell_ids, ref_ids, new_cell_ids, new_ref_ids, total_refs, (a, b) -> a > b);
        
        if (!ref_ids.equals(new_ref_ids))  //ref_ids and cell_ids don't share same array hence one can swap both
            ref_ids.swap(tmp_ref_ids);           
        if (!cell_ids.equals(new_cell_ids)) 
            cell_ids.swap(tmp_cell_ids);  
        
        // Compute the ranges of references for each cell
        compute_cell_ranges(cell_ids, cells, total_refs);
        //DEBUG_SYNC();
        
        grid.entries = entries;
        grid.ref_ids = ref_ids;
        grid.cells   = cells;
        grid.shift   = levels.size() - 1;
        grid.num_cells   = new_total_cells;
        grid.num_entries = total_cells;
        grid.num_refs    = total_refs;
        
        grid.offsets = new NativeInteger(levels.size());
        for (int i = 0, off = 0; i < levels.size(); i++) {
            off += levels.get(i).num_cells;
            grid.offsets.set(i, off);
        }        
    }
    
    public void build_grid(TriangleMesh prims)
    {
        SerialNativeObjectAlgorithm<BoundingBox> par = new SerialNativeObjectAlgorithm();
        
        // Allocate a bounding box for each primitive + one for the global bounding box       
        NativeObject<BoundingBox> bboxes = new NativeObject(BoundingBox.class, prims.getSize() + 1);
        
        compute_bboxes(prims, bboxes, prims.getSize());         
        BoundingBox grid_bb = par.reduce(bboxes, prims.getSize(), bboxes.offsetMemoryLast(), 
                (a, b) -> a.include(b));
        
        Point3i dims = compute_grid_dims(grid_bb, prims.getSize(), hagrid.top_density);
        
        // Round to the next multiple of 2 on each dimension (in order to align the memory)
        dims.x = (dims.x % 2) != 0 ? dims.x + 1 : dims.x;
        dims.y = (dims.y % 2) != 0 ? dims.y + 1 : dims.y;
        dims.z = (dims.z % 2) != 0 ? dims.z + 1 : dims.z;
        
        // Slightly enlarge the bounding box of the grid        
        grid_bb.enlargeUlps();
                
        hagrid.grid().bbox = grid_bb;
        hagrid.grid().dims = dims;
        
        this.hagrid.grid_bbox = grid_bb;
        this.hagrid.grid_dims = dims;
        
        NativeInteger log_dims = new NativeInteger(1);
        int[] gridd_shift = new int[1];
        ArrayList<Level2> levels = new ArrayList();
        
        // Build top level
        first_build_iter(hagrid.snd_density, prims, bboxes, hagrid.grid().bbox, dims, log_dims, gridd_shift, levels);
    }
}
