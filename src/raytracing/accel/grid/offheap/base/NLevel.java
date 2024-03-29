/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.accel.grid.offheap.base;

import coordinate.memory.nativememory.NativeInteger;
import coordinate.memory.nativememory.NativeObject;

/**
 *
 * @author jmburu
 */
public class NLevel {
    public NativeInteger        ref_ids;               ///< Array of primitive indices
    public NativeInteger        cell_ids;              ///< Array of cell indices
    public int                  num_refs;               ///< Number of references in the level
    public int                  num_kept;               ///< Number of references kept (remaining is split)
    public NativeObject<NCell>  cells;                ///< Array of cells
    public NativeInteger        entries;             ///< Array of voxel map entries
    public int                  num_cells;              ///< Number of cells
    
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        
        builder.append("Level ").append("\n");
        builder.append("  num of references: ").append(num_refs).append("\n");
        builder.append("  num of cells:      ").append(num_cells).append("\n");
        builder.append("  num of kept:       ").append(num_kept).append("\n");
        if(entries != null)
            builder.append("  num of entries:    ").append(entries.capacity()).append("\n");
        
        return builder.toString();
    }
}
