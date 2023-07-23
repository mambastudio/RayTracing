/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid;

import raytracing.core.grid.base.template.GridConstruction;
import raytracing.core.grid.base.Grid;
import raytracing.core.grid.base.Hagrid;
import raytracing.primitive.TriangleMesh;

/**
 *
 * @author user
 */
public class HagridConstruction extends GridConstruction<Grid, Hagrid>{
    
    public HagridConstruction(Hagrid hagrid)
    {
        super(hagrid);
    }
    
    @Override
    public Grid initialiseGrid(TriangleMesh tris)
    {       
        build_grid(tris);
        merge_grid();
        //flatten_grid();
        
        return hagrid.grid();
    }

    @Override
    protected void build_grid(TriangleMesh tris) {
        Build build = new Build(hagrid);
        build.build_grid(tris);
        System.out.println("finished building");
    }

    @Override
    protected void merge_grid() {
        Merge merge = new Merge(hagrid);
        merge.merge_grid();
        System.out.println("finished merging");
    }

    @Override
    protected void flatten_grid() {
        
    }
    
}
