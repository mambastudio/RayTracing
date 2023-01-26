/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import coordinate.parser.obj.OBJParser;
import java.util.ArrayList;
import java.util.Arrays;
import raytracing.core.coordinate.BoundingBox;
import raytracing.core.grid.main.Grid2;
import raytracing.core.grid.main.GridInfo;
import raytracing.core.grid.main.GridUtility;
import raytracing.core.grid.main.Hagrid;
import raytracing.core.grid.main.Hagrid2;
import raytracing.core.grid.main.Ref;
import raytracing.primitive.TriangleMesh;

/**
 *
 * @author user
 */
public class TestIrregularGrid {
    static float top_density = 0.12f;
    static float snd_density = 2.4f;
    static Grid2 grid = new Grid2();
    
    public static void main(String... args)
    {
        OBJParser parser = new OBJParser();
        TriangleMesh tris = new TriangleMesh();
        parser.read("C:\\Users\\user\\Documents\\3D Scenes\\box\\Mori.obj", tris);
        
        Hagrid2 hagrid = new Hagrid2();
        hagrid.build_grid(tris, tris.getSize(), grid, top_density, snd_density);
        
        //test(tris);

    }
    
    public static void test(TriangleMesh tris)
    {
        BoundingBox bboxes[] = new BoundingBox[tris.getSize()];
        GridInfo info = new GridInfo();
        ArrayList<Ref> refs = new ArrayList();
        int[] snd_dims;
        
        GridUtility.profile("Generating top-level references", ()->{
            //compute bounding boxes and overall scene bounding box
            Hagrid.compute_bboxes(info, tris, bboxes);
            //Determine the top-level grid resolution
            Hagrid.compute_grid_dims(info.bbox.maximum.sub(info.bbox.minimum), tris.getSize(), top_density, info.dims);
            // Generate references for the top-level grid
            Hagrid.gen_top_refs(info, bboxes, tris, refs);
            
            //System.out.println(tris.getSize());
            //System.out.println(info.bbox);
            //System.out.println(Arrays.toString(info.dims));
            //System.out.println(refs);
        });
        
        snd_dims = new int[info.num_top_cells()];
        
        GridUtility.profile("Subdividing references", ()->{
            // Compute the resolution at which each top-level cell will be subdivided
            Hagrid.compute_snd_dims(info, snd_density, refs, snd_dims);
            
            // Subdivide the references until the second-level dimensions are reached
            Hagrid.subdivide_refs(info, tris, snd_dims, refs);
        });
    }
}
