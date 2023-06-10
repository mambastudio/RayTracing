/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid.construction;

import raytracing.core.grid.Build;
import raytracing.core.grid.base.Grid;
import raytracing.core.grid.base.Hagrid;
import raytracing.primitive.TriangleMesh;

/**
 *
 * @author user
 */
public class HagridConstruction extends GridConstruction{
    
    public HagridConstruction(Hagrid hagrid)
    {
        super(hagrid);
    }
    
    public Grid initialiseGrid(TriangleMesh tris)
    {       
        build_grid(tris);
        merge_grid();
        flatten_grid();
        
        return hagrid.getIrregularGrid();
    }

    @Override
    protected void build_grid(TriangleMesh tris) {
        Build build = new Build(hagrid);
        build.build_grid(tris);
    }

    @Override
    protected void merge_grid() {
        
    }

    @Override
    protected void flatten_grid() {
        
    }
    
}
