/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid.base;

import coordinate.list.IntegerList;
import coordinate.list.ObjectList;
import raytracing.core.coordinate.BoundingBox;
import raytracing.core.coordinate.Point3f;
import raytracing.core.coordinate.Point3i;
import raytracing.core.coordinate.Vector3f;
import raytracing.core.grid.base.template.GridMap;

/**
 *
 * @author user
 */
public class Grid implements GridMap{
    public Entry[] entries;         ///< Voxel map, stored as a contiguous array
    public IntegerList   ref_ids;   ///< Array of primitive references
    public ObjectList<Cell>  cells; ///< Cells of the structure (nullptr if compressed)

    //SmallCell* small_cells;       ///< Compressed cells (nullptr if not compressed)

    public BoundingBox bbox;        ///< Bounding box of the scene
    public Point3i dims;            ///< Top-level dimensions
    public int num_cells;           ///< Number of cells
    public int num_entries;         ///< Number of elements in the voxel map
    public int num_refs;            ///< Number of primitive references
    public int shift;               ///< Amount of bits to shift to get from the deepest level to the top-level
    public IntegerList offsets;     ///< Offset to each level of the voxel map octree
    
    public Vector3f grid_inv()              {return new Vector3f(grid_dims()).div(bbox.extents());}   
    public Vector3f cell_size()             {return bbox.extents().div(new Vector3f(grid_dims()));}
    public Point3i grid_dims()              {return dims.leftShift(shift);}
    public int grid_shift()                 {return shift;}
    public Point3f grid_min()               {return bbox.minimum.copy();}
    public Point3f grid_max()               {return bbox.maximum.copy();}
    public BoundingBox cellbound(Cell cell) {return new BoundingBox(
                                                        bbox.minimum.add(cell_size().mul(new Vector3f(cell.min))), 
                                                        bbox.minimum.add(cell_size().mul(new Vector3f(cell.max))));}
}
