/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid.main;

import coordinate.utility.Value3Di;
import java.util.ArrayList;
import raytracing.core.coordinate.BoundingBox;

/**
 *
 * @author user
 */

/// Structure holding an irregular grid
public class Grid2 {
    public Entry[] entries;                ///< Voxel map, stored as a contiguous array
    public int[]   ref_ids;                ///< Array of primitive references
    public Cell  cells;                    ///< Cells of the structure (nullptr if compressed)

    public SmallCell small_cells;          ///< Compressed cells (nullptr if not compressed)

    public BoundingBox bbox;               ///< Bounding box of the scene
    public Value3Di dims;                  ///< Top-level dimensions
    public int num_cells;                  ///< Number of cells
    public int num_entries;                ///< Number of elements in the voxel map
    public int num_refs;                   ///< Number of primitive references
    public int shift;                      ///< Amount of bits to shift to get from the deepest level to the top-level
    public ArrayList<Integer> offsets;     ///< Offset to each level of the voxel map octree
}
