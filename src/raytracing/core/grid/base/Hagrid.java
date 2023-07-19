/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid.base;

import raytracing.core.coordinate.BoundingBox;
import raytracing.core.coordinate.Point3i;
import raytracing.core.coordinate.Vector3f;

/**
 *
 * @author user
 */
public class Hagrid {
    public float top_density = 0.15f;
    public float snd_density = 1.0f;
    public float alpha = 0.995f;
    public int exp_iters = 3;
    
    //grid info (temporary data set used during building)
    public Point3i grid_dims;
    public BoundingBox  grid_bbox;
    public Vector3f cell_size;
    public int   grid_shift;
    
    //grid accelerating structure
    public Grid irregular_grid;
    
    public Hagrid()
    {
        grid_dims = new Point3i();
        grid_bbox = new BoundingBox();
        cell_size = new Vector3f();
        
        irregular_grid = new Grid();
    }
    
    public Grid grid()
    {
        return irregular_grid;
    }
}
