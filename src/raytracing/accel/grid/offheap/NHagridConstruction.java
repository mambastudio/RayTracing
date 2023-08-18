/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.accel.grid.offheap;

import raytracing.accel.grid.offheap.base.NGrid;
import raytracing.accel.grid.GridConstruction;
import raytracing.accel.grid.offheap.base.NHagrid;
import raytracing.primitive.TriangleMesh;

/**
 *
 * @author user
 */
public class NHagridConstruction extends GridConstruction<NGrid, NHagrid>{
    
    public NHagridConstruction(NHagrid hagrid)
    {
        super(hagrid);
    }
    
    @Override
    public NGrid initialiseGrid(TriangleMesh tris) {
        build_grid(tris);
        
        return hagrid.grid();
    }

    @Override
    protected void build_grid(TriangleMesh tris) {
        NBuild build = new NBuild(hagrid);
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
