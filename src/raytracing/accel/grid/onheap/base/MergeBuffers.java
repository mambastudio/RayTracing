/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.accel.grid.onheap.base;

import coordinate.list.IntegerList;

/**
 *
 * @author user
 */
public class MergeBuffers {
    public IntegerList merge_counts;  ///< Contains the number of references in each cell (positive if merged, otherwise negative)
    public IntegerList prevs, nexts;  ///< Contains the index of the previous/next neighboring cell on the merging axis (positive if merged, otherwise negative)
    public IntegerList ref_counts;    ///< Contains the number of references per cell after merge
    public IntegerList cell_flags;    ///< Contains 1 if the cell is kept (it is not a residue), otherwise 0
    public IntegerList cell_scan;     ///< Scan over cell_flags (insertion position of the cells into the new cell array)
    public IntegerList ref_scan;      ///< Scan over ref_counts (insertion position of the references into the new reference array)
    public IntegerList new_cell_ids;  ///< Mapping between the old cell indices and the new cell indices
}
