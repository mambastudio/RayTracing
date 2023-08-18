/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package grid;

import coordinate.memory.NativeInteger;
import coordinate.parser.obj.OBJParser;
import raytracing.accel.grid.onheap.Build;
import raytracing.accel.grid.onheap.base.Hagrid;
import raytracing.accel.grid.offheap.NBuild;
import raytracing.accel.grid.offheap.base.NHagrid;
import raytracing.primitive.TriangleMesh;

/**
 *
 * @author jmburu
 */
public class TestGrid {
    public static void main(String... args)
    {
        OBJParser parser = new OBJParser();
        TriangleMesh mesh = new TriangleMesh();
       // parser.read("C:\\Users\\user\\Documents\\3D Scenes\\mori_knob\\testObj.obj", mesh);
        parser.read("C:\\Users\\jmburu\\Documents\\GitHub\\RayTracing\\SimpleMesh.obj", mesh);
        
        Build b1 = new Build(new Hagrid());
        NBuild b2 = new NBuild(new NHagrid());
        
        
        for(int i = 0; i<4; i++)
        {            //b1.build_grid(mesh);
            b2.build_grid(mesh);
        }
    }
}
