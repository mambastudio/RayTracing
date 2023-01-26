/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid.main;

import raytracing.core.coordinate.BoundingBox;
import raytracing.core.coordinate.Point3f;
import raytracing.core.coordinate.Vector3f;

/**
 *
 * @author user
 */
public class GridInfo {
    public BoundingBox bbox;               ///< Bounding box of the grid
    public int  dims[] = new int[3];       ///< Dimensions of the top level
    public int  max_snd_dim;               ///< Maximum second level density

    /// Returns the size of a top-level cell.
    public Point3f cell_size() 
    {        
        Vector3f vv = (bbox.maximum.sub(bbox.minimum)).div(new Vector3f(dims[0], dims[1], dims[2]));
        return new Point3f(vv.x, vv.y, vv.z);
    }

    /// Returns the number of top-level cells.
    public int num_top_cells() { return dims[0] * dims[1] * dims[2]; }
}
