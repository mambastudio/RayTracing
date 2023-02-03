/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid.main;

import coordinate.utility.Value3Di;
import java.util.ArrayList;
import java.util.Arrays;
import raytracing.core.coordinate.BoundingBox;
import raytracing.core.coordinate.Vector3f;
import raytracing.primitive.TriangleMesh;

/**
 *
 * @author user
 */
public class Hagrid2 extends GridAbstract2 implements HagridInterface2{
        
    public void compute_bboxes(
            TriangleMesh prims,
            BoundingBox[] bboxes,
            int num_prims) 
    {
        for(int i = 0; i<num_prims; i++)
            bboxes[i] = prims.getBound(i);
    }
    
    
    
    
    //NOT SURE IF IT'S CORRECT
    public void emit_new_refs(
            BoundingBox[] bboxes,
            IntArray start_emit,
            IntArray new_ref_ids,
            IntArray new_cell_ids,
            int num_prims){
        
        for (int id = 0; id < num_prims; id++) 
        {
            int start = start_emit.get(id + 0);
            int end   = start_emit.get(id + 1);
            Range range = null;

            if (start < end) 
            {
                BoundingBox ref_bb = bboxes[id];
                range  = compute_range(grid_dims, grid_bbox, ref_bb);
            }

            boolean blocked = (end - start) >= 16;
            if (!blocked && start < end) 
            {
                int x = range.lx;
                int y = range.ly;
                int z = range.lz;
                int cur = start;
                while (cur < end) 
                {
                    new_ref_ids .set(cur, id);
                    new_cell_ids.set(cur, x + grid_dims.x * (y + grid_dims.y * z));
                    cur++;
                    x++;
                    if (x > range.hx) { x = range.lx; y++; }
                    if (y > range.hy) { y = range.ly; z++; } 
                }
            }
            
            if (blocked) 
            {                
                for (int i = start; i < end; i++) 
                {
                    int lx = range.lx;
                    int ly = range.ly;
                    int lz = range.lz;
                    int hx = range.hx;
                    int hy = range.hy;
                    int r  = id;

                    int sx = hx - lx + 1;
                    int sy = hy - ly + 1;
                    
                    // Split the work on all the threads of the warp
                    int k = i - start;
                    int x = lx + (k % (sx));
                    int y = ly + ((k / (sx)) % sy);
                    int z = lz + (k / ((sx) * sy));
                    new_ref_ids.set(i,  r); 
                    new_cell_ids.set(i, x + grid_dims.x * (y + grid_dims.y * z));    
                }
            }
        }        
    }
    public void first_build_iter(
            float snd_density,
            TriangleMesh prims, int num_prims,
            BoundingBox[] bboxes, BoundingBox grid_bb, Value3Di dims,
            IntArray log_dims, IntArray grid_shift, ArrayList<Level> levels) {
        int num_top_cells = dims.x * dims.y * dims.z;
        
        IntArray start_emit        = new IntArray(new int[num_prims + 1]);
        IntArray new_ref_counts    = new IntArray(new int[num_prims + 1]);
        IntArray refs_per_cell     = new IntArray(new int[num_top_cells]);
        //int[] log_dims          = new int[num_top_cells + 1];
        
        count_new_refs(bboxes, new_ref_counts, num_prims);  
        
        System.arraycopy(new_ref_counts.array(), 0, start_emit.array(), 0, new_ref_counts.size());        
        int num_new_refs = exclusiveScan(start_emit.array());
        
        IntArray new_ref_ids  = new IntArray(new int[2 * num_new_refs]);
        IntArray new_cell_ids = new_ref_ids.splitSubArrayFrom(num_new_refs);       
        emit_new_refs(bboxes, start_emit, new_ref_ids, new_cell_ids, num_prims); 
                
        // Compute the number of references per cell       
        count_refs_per_cell(new_cell_ids, refs_per_cell, num_new_refs);
        
        // Compute an independent resolution in each of the top-level cells
        compute_log_dims(refs_per_cell, log_dims, snd_density, num_top_cells);
                        
        // Find the maximum sub-level resolution
        grid_shift.set(0, Arrays.stream(log_dims.array(), 0, num_top_cells).reduce(Integer.MIN_VALUE, (a, b) -> Math.max(a, b)));
        
        cell_size = grid_bb.extents().div(new Vector3f(
                dims.x << grid_shift.get(0),
                dims.y << grid_shift.get(0),
                dims.z << grid_shift.get(0)));
        
        this.grid_shift = grid_shift.get(0);
        
        //Emission of the new cells
        Cell[] new_cells   = new Cell[num_top_cells + 0];
        Entry[] new_entries = new Entry[num_top_cells + 1];
        emit_top_cells(new_cells, num_top_cells);
        for(int i = 0; i<num_top_cells + 1; i++)
            new_entries[i] = new Entry();
        
        
        
        // Filter out the references that do not intersect the cell they are in
        filter_refs(new_cell_ids, new_ref_ids, prims, new_cells, num_new_refs);
        
        
        Level level = new Level();
        level.ref_ids   = new_ref_ids;  
        level.cell_ids  = new_cell_ids; 
        level.num_refs  = num_new_refs;   
        level.num_kept  = num_new_refs;   
        level.cells     = new_cells;   
        level.entries   = new_entries;
        level.num_cells = num_top_cells;  
                        
        levels.add(level);
        
    }
    
    public boolean build_iter(
                TriangleMesh prims, int num_prims,
                Value3Di dims, IntArray log_dims,
                ArrayList<Level> levels) {
        IntArray cell_ids  = levels.get(levels.size()-1).cell_ids;
        IntArray ref_ids   = levels.get(levels.size()-1).ref_ids;
        Cell[] cells    = levels.get(levels.size()-1).cells;
        Entry[] entries = levels.get(levels.size()-1).entries;
                        
        int num_top_cells = dims.x * dims.y * dims.z;
        int num_refs  = levels.get(levels.size()-1).num_refs;
        int num_cells = levels.get(levels.size()-1).num_cells;

        int cur_level  = levels.size();
        
        IntArray kept_flags = new IntArray(new int[num_refs + 1]);
        
        // Find out which cell will be split based on whether it is empty or not and the maximum depth
        compute_dims(cell_ids, cells, log_dims, entries, num_refs);
        update_log_dims(log_dims, num_top_cells);
        mark_kept_refs(cell_ids, entries, kept_flags, num_refs);
        
        // Store the sub-cells starting index in the entries
        IntArray start_cell = new IntArray(new int[num_cells + 1]);
        for(int i = 0; i<num_cells; i++)
            start_cell.set(i, entries[i].log_dim == 0 ? 0 : 8);
        int num_new_cells = this.exclusiveScan(start_cell.array());
        
        update_entries(start_cell, entries, num_cells);   
        
        // Partition the set of cells into the sets of those which will be split and those which won't
        IntArray tmp_ref_ids  = new IntArray(new int[num_refs * 2]);
        IntArray tmp_cell_ids = tmp_ref_ids.getSubArray(num_refs, tmp_ref_ids.size());
        int num_sel_refs  = partition(ref_ids,  tmp_ref_ids,  num_refs, kept_flags);
        int num_sel_cells = partition(cell_ids, tmp_cell_ids, num_refs, kept_flags);
        
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
        IntArray split_masks = new IntArray(new int[num_split + 1]);
        IntArray start_split = new IntArray(new int[num_split + 1]);
                        
        compute_split_masks(
                cell_ids.splitSubArrayFrom(num_kept), 
                ref_ids.splitSubArrayFrom(num_kept), 
                prims, cells, 
                split_masks, 
                num_split);
        
        
        // Store the sub-cells starting index in the entries        
        for(int i = 0; i<split_masks.size(); i++)
            start_split.set(i, __popc(split_masks.get(i)));
        int num_new_refs = this.exclusiveScan(start_split.array());
                
        if(!(num_new_refs <= 8 * num_split))
            throw new UnsupportedOperationException();
        
        IntArray new_ref_ids = new IntArray(new int[num_new_refs * 2]);
        IntArray new_cell_ids = new_ref_ids.splitSubArrayFrom(num_new_refs);
               
        split_refs(
                cell_ids.splitSubArrayFrom(num_kept), 
                ref_ids.splitSubArrayFrom(num_kept), 
                entries, 
                split_masks, 
                start_split, 
                new_cell_ids, 
                new_ref_ids, 
                num_split);

        // Emission of the new cells
        Cell[] new_cells   = new Cell[num_new_cells + 0];
        Entry[] new_entries = new Entry[num_new_cells + 1];
        emit_new_cells(entries, cells, new_cells, num_cells);
        
        for(int i = 0; i<num_new_cells + 1; i++)
            new_entries[i] = new Entry();
        
        Level level = new Level();
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
    
    public void build(TriangleMesh prims, int num_prims, Grid2 grid, float top_density, float snd_density)
    {
        // Allocate a bounding box for each primitive + one for the global bounding box
        BoundingBox[] bboxes = new BoundingBox[num_prims + 1];
       
        compute_bboxes(prims, bboxes, num_prims);
       
        BoundingBox grid_bb = Arrays.stream(bboxes, 0, num_prims).reduce(new BoundingBox(), (a, b) ->{
            a.include(b); return a;});
        
                
        Value3Di dims = compute_grid_dims(grid_bb, num_prims, top_density);
        // Round to the next multiple of 2 on each dimension (in order to align the memory)
        dims.x = (dims.x % 2) != 0 ? dims.x + 1 : dims.x;
        dims.y = (dims.y % 2) != 0 ? dims.y + 1 : dims.y;
        dims.z = (dims.z % 2) != 0 ? dims.z + 1 : dims.z;
        
        // Slightly enlarge the bounding box of the grid
        Vector3f extents = grid_bb.extents();
        grid_bb.minimum = grid_bb.minimum.sub(extents.mul(0.001f));
        grid_bb.maximum = grid_bb.maximum.add(extents.mul(0.001f));
        
        this.grid_dims = dims;
        this.grid_bbox = grid_bb;
        
        IntArray log_dims = new IntArray(new int[dims.x * dims.y * dims.z + 1]);
        IntArray grid_shiftt = new IntArray(new int[1]);
        ArrayList<Level> levels = new ArrayList();
        
        // Build top level
        first_build_iter(snd_density, prims, num_prims, bboxes, grid_bb, dims, log_dims, grid_shiftt, levels);

        int iter = 1;
        while(this.build_iter(prims, num_prims, dims, log_dims, levels))
            iter++;
        
        concat_levels(levels, grid);
        
        grid.dims = dims;
        grid.bbox = grid_bb;
    }

    @Override
    public void build_grid(TriangleMesh tris, int num_tris, Grid2 grid, float top_density, float snd_density) {
        build(tris, num_tris, grid, top_density, snd_density);
    }

    @Override
    public void merge_grid(Grid2 grid, float alpha) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void flatten_grid(Grid2 grid) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void expand_grid(Grid2 grid, TriangleMesh tris, int iters) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean compress_grid(Grid2 grid) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
