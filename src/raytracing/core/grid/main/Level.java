/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid.main;

/**
 *
 * @author user
 */
public class Level {
    public IntArray ref_ids;               ///< Array of primitive indices
    public IntArray cell_ids;              ///< Array of cell indices
    public int num_refs;               ///< Number of references in the level
    public int num_kept;               ///< Number of references kept (remaining is split)
    public Cell[] cells;                ///< Array of cells
    public Entry[] entries;             ///< Array of voxel map entries
    public int num_cells;              ///< Number of cells

}
