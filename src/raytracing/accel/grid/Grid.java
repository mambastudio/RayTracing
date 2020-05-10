/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.accel.grid;

import java.util.ArrayList;
import raytracing.core.coordinate.BoundingBox;
import raytracing.core.coordinate.Point3f;
import raytracing.core.coordinate.Vector3f;

/**
 *
 * @author user
 */
public class Grid {
    Entry entries;             ///< Voxel map, stored as a contiguous array
    int   ref_ids;             ///< Array of primitive references
    Cell  cells;               ///< Cells of the structure (nullptr if compressed)

    //SmallCell* small_cells;     ///< Compressed cells (nullptr if not compressed)

    BoundingBox bbox;                  ///< Bounding box of the scene
    Point3f dims;                 ///< Top-level dimensions
    int num_cells;              ///< Number of cells
    int num_entries;            ///< Number of elements in the voxel map
    int num_refs;               ///< Number of primitive references
    int shift;                  ///< Amount of bits to shift to get from the deepest level to the top-level
    ArrayList<Integer> offsets;   ///< Offset to each level of the voxel map octree
    
    /// Returns a voxel map entry with the given dimension and starting index
    public static Entry make_entry(int log_dim, int begin) 
    {
        return new Entry(log_dim, begin);
    }
    
    /// Computes the range of cells that intersect the given box
    public static Range compute_range(Point3f dims, BoundingBox grid_bb, BoundingBox obj_bb) 
    {
        Vector3f inv = dims.getVCoordInstance().div(grid_bb.extents());
        int lx = Math.max((int)((obj_bb.minimum.x - grid_bb.minimum.x) * inv.x), 0);
        int ly = Math.max((int)((obj_bb.minimum.y - grid_bb.minimum.y) * inv.y), 0);
        int lz = Math.max((int)((obj_bb.minimum.z - grid_bb.minimum.z) * inv.z), 0);
        int hx = Math.min((int)((obj_bb.maximum.x - grid_bb.minimum.x) * inv.x), (int)dims.x - 1);
        int hy = Math.min((int)((obj_bb.maximum.y - grid_bb.minimum.y) * inv.y), (int)dims.y - 1);
        int hz = Math.min((int)((obj_bb.maximum.z - grid_bb.minimum.z) * inv.z), (int)dims.z - 1);
        return new Range(lx, ly, lz, hx, hy, hz);
    }
}
