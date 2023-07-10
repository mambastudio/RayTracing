/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid.base;

import coordinate.utility.Utility;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.util.Arrays;
import raytracing.core.Intersection;
import raytracing.core.coordinate.BoundingBox;
import raytracing.core.coordinate.Point2f;
import raytracing.core.coordinate.Point3f;
import raytracing.core.coordinate.Point3i;
import raytracing.core.coordinate.Ray;
import raytracing.core.coordinate.Vector3f;
import raytracing.core.grid.GridAbstract;
import raytracing.primitive.TriangleMesh;

/**
 *
 * @author user
 */
public class Traverse extends GridAbstract{
    Grid grid;
    
    public Traverse(Grid grid)
    {
        this.grid = grid;
    }
    
    public Point2f intersect_ray_box(
            Point3f org, 
            Vector3f inv_dir, 
            Point3f box_min, 
            Point3f box_max) {
        //System.out.println(org);
        Vector3f tmin = box_min.sub(org).mul(inv_dir);
        Vector3f tmax = box_max.sub(org).mul(inv_dir);
        Vector3f t0 = Vector3f.min(tmin, tmax);
        Vector3f t1 = Vector3f.max(tmin, tmax);
        return new Point2f( max(t0.x, max(t0.y, t0.z)),
                            min(t1.x, min(t1.y, t1.z)));
    }

    private Vector3f compute_voxel(Point3f org, Vector3f dir, float t)
    {
        //return (t * dir + org - grid_min) * grid_inv;
        return dir.mul(t).add(org.asVector3f()).sub(grid.grid_min().asVector3f()).mul(grid.grid_inv());
    }
    
    public boolean traverse(Ray r, Intersection intersection, TriangleMesh mesh) {
        Vector3f inv_dir = r.getInvDir().copy();
        
        Point2f tbox = intersect_ray_box(r.getOrigin(), inv_dir, grid.grid_min(), grid.grid_max());
        float tstart = max(tbox.x, r.getMin());
        float tend   = min(tbox.y, r.getMax());
        
        int steps = 0;
        Point3i voxel;

        // Early exit if the ray does not hit the grid
        if (tstart > tend)  
            return false;       
        
        // Find initial voxel
        voxel = Point3i.clamp(new Point3i(compute_voxel(r.getOrigin(), r.getDirection(), tstart)), new Point3i(0, 0, 0), grid.grid_dims().sub(1));
        
        while(true)
        {
            // Lookup entry
            int entry = lookup_entry(grid.entries, grid.grid_shift(), grid.grid_dims().rightShift(grid.grid_shift()), voxel);

            // Lookup the cell associated with this voxel
            Cell cell = grid.cells.get(entry);            
           
            // Intersect the farmost planes of the cell bounding box
            Point3i cell_point = new Point3i(   r.getDirection().x >= 0.0f ? cell.max.x : cell.min.x,
                                                r.getDirection().y >= 0.0f ? cell.max.y : cell.min.y,
                                                r.getDirection().z >= 0.0f ? cell.max.z : cell.min.z);
            
            Point3f tcell = new Point3f(cell_point). //auto tcell = (vec3(cell_point) * cell_size + grid_min - ray.org) * inv_dir;
                    mul(grid.cell_size().asPoint3f()).
                    addS(grid.grid_min()).
                    subS(r.getOrigin()).
                    mul(inv_dir.asPoint3f());                        
            float texit = min(tcell.x, min(tcell.y, tcell.z)); 
            
            // Move to the next voxel
            Point3i exit_point = new Point3i(compute_voxel(r.getOrigin(), r.getDirection(), texit));
            Point3i next_voxel = new Point3i(   texit == tcell.x ? cell_point.x + (r.getDirection().x >= 0.0f ? 0 : -1) : exit_point.x,
                                                texit == tcell.y ? cell_point.y + (r.getDirection().y >= 0.0f ? 0 : -1) : exit_point.y,
                                                texit == tcell.z ? cell_point.z + (r.getDirection().z >= 0.0f ? 0 : -1) : exit_point.z);
                        
            voxel.x = r.getDirection().x >= 0.0f ? max(next_voxel.x, voxel.x) : min(next_voxel.x, voxel.x);
            voxel.y = r.getDirection().y >= 0.0f ? max(next_voxel.y, voxel.y) : min(next_voxel.y, voxel.y);
            voxel.z = r.getDirection().z >= 0.0f ? max(next_voxel.z, voxel.z) : min(next_voxel.z, voxel.z);
                       
            // Intersect the cell contents and exit if an intersection was found
            steps += 1 + foreach_ref(cell, intersection, grid.ref_ids, (ref, isect)->{
                mesh.intersect(r, ref, isect);                
            });
            
            if (r.getMax() <= texit ||
                (voxel.x < 0 | voxel.x >= grid.grid_dims().x |
                 voxel.y < 0 | voxel.y >= grid.grid_dims().y |
                 voxel.z < 0 | voxel.z >= grid.grid_dims().z))
                break;

        }
        intersection.data = steps;
        return intersection.hasHit();
    }
    
    public static Vector3f getCellSize(Point3i dims, BoundingBox bound)
    {
        return bound.extents().div(new Vector3f(dims));
    }
}
