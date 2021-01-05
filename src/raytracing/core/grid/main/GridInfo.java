package raytracing.core.grid.main;


import raytracing.core.coordinate.BoundingBox;
import raytracing.core.coordinate.Point3f;
import raytracing.core.coordinate.Vector3f;

// Uncommenting this definition enables many correctness checks.
//#define SANITY_CHECKS

/// Information required to build an irregular grid.
public class GridInfo {
    public BoundingBox bbox;           ///< Bounding box of the grid
    public int[]  dims = new int[3];   ///< Dimensions of the top level
    public int  max_snd_dim;           ///< Maximum second level density

    /// Returns the size of a top-level cell.
    public Point3f cell_size()
    { 
        return bbox.maximum.sub(bbox.minimum).div(new Vector3f(dims[0], dims[1], dims[2])).asPoint3f();            
    }

    /// Returns the number of top-level cells.
    public int num_top_cells() 
    { 
        return dims[0] * dims[1] * dims[2]; 
    }
}