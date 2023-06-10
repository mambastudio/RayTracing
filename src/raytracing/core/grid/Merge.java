/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid;

import coordinate.list.IntegerList;
import coordinate.list.ObjectList;
import raytracing.core.coordinate.Point3i;
import raytracing.core.coordinate.Vector3f;
import raytracing.core.grid.base.Cell;
import raytracing.core.grid.base.Hagrid;
import raytracing.core.grid.base.MergeBuffers;

/**
 *
 * @author user
 */
public class Merge {
    Hagrid hagrid;
    
    public Merge(Hagrid hagrid)
    {
        this.hagrid = hagrid;
    }
    
     /// Performs the neighbor merging optimization (merging cells according to the SAH).
    public void merge_grid()
    {        
        MergeBuffers bufs = new MergeBuffers();
                
        ObjectList<Cell> new_cells = new ObjectList(new Cell[hagrid.getIrregularGrid().num_cells]);        
        IntegerList new_refs  = new IntegerList(new int[hagrid.getIrregularGrid().num_refs]);
        
        int buf_size = hagrid.getIrregularGrid().num_cells + 1;
        buf_size = (buf_size % 4) != 0 ? buf_size + 4 - buf_size % 4 : buf_size;
        
        bufs.merge_counts = new IntegerList(new int[buf_size]);
        bufs.ref_counts   = new IntegerList(new int[buf_size]);
        bufs.cell_flags   = new IntegerList(new int[buf_size]);
        bufs.cell_scan    = new IntegerList(new int[buf_size]);
        bufs.ref_scan     = new IntegerList(new int[buf_size]);
        bufs.new_cell_ids = bufs.cell_flags;
        bufs.prevs        = bufs.cell_scan;
        bufs.nexts        = bufs.ref_scan;
        
        Vector3f extents = hagrid.getIrregularGrid().bbox.extents();
        Point3i dims = hagrid.getIrregularGrid().dims.leftShift(hagrid.getIrregularGrid().shift);
        Vector3f cell_size0 = extents.div(new Vector3f(dims));
        
        hagrid.grid_dims = dims;
        hagrid.cell_size = cell_size0;
        hagrid.grid_shift = hagrid.getIrregularGrid().shift;
    }
}
