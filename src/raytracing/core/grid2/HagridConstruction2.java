/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid2;

import raytracing.core.grid.base.template.GridConstruction;
import raytracing.core.grid2.base.Grid2;
import raytracing.core.grid2.base.Hagrid2;
import raytracing.primitive.TriangleMesh;

/**
 *
 * @author user
 */
public class HagridConstruction2 extends GridConstruction<Grid2, Hagrid2>{
    
    public HagridConstruction2(Hagrid2 hagrid)
    {
        super(hagrid);
    }
    
    @Override
    public Grid2 initialiseGrid(TriangleMesh tris) {
        build_grid(tris);
        
        return hagrid.grid();
    }

    @Override
    protected void build_grid(TriangleMesh tris) {
        Build2 build = new Build2(hagrid);
        //build.build_grid(tris);
        System.out.println("finished building");
    }

    @Override
    protected void merge_grid() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void flatten_grid() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
    
}
