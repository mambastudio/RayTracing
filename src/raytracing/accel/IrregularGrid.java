/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.accel;

import coordinate.generic.raytrace.AbstractAccelerator;
import raytracing.core.Intersection;
import raytracing.core.coordinate.BoundingBox;
import raytracing.core.coordinate.Ray;
import raytracing.accel.grid.onheap.base.Grid;
import raytracing.accel.grid.onheap.base.Hagrid;
import raytracing.accel.grid.onheap.HagridConstruction;
import raytracing.accel.grid.onheap.Traverse;
import raytracing.primitive.TriangleMesh;

/**
 *
 * @author user
 */
public class IrregularGrid implements AbstractAccelerator<Ray, Intersection, TriangleMesh, BoundingBox> {
    Grid grid = null;
    TriangleMesh mesh = null;
    
    @Override
    public void build(TriangleMesh mesh) {
        this.mesh = mesh;
        
        Hagrid hagrid = new Hagrid();
        HagridConstruction construction = new HagridConstruction(hagrid);
        construction.initialiseGrid(mesh);
        grid = hagrid.grid();
    }

    @Override
    public boolean intersect(Ray ray, Intersection isect) {
        Traverse traverse = new Traverse(grid);
        return traverse.traverse(ray, isect, mesh);
    }

    @Override
    public boolean intersectP(Ray ray) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void intersect(Ray[] rays, Intersection[] isects) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public BoundingBox getBound() {
        return grid.bbox;
    }
    
}
