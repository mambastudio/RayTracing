/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.hagrid;

import coordinate.parser.obj.OBJParser;
import raytracing.core.grid.base.Hagrid;
import raytracing.core.grid.construction.HagridConstruction;
import raytracing.primitive.TriangleMesh;

/**
 *
 * @author user
 */
public class TestIrregularGrid {
    public static void main(String... args)
    {
        OBJParser parser = new OBJParser();
        TriangleMesh mesh = new TriangleMesh();
       // parser.read("C:\\Users\\user\\Documents\\3D Scenes\\mori_knob\\testObj.obj", mesh);
        parser.read("C:\\Users\\user\\Documents\\3D Scenes\\sponza\\sponza.obj", mesh);
        
        HagridConstruction construction = new HagridConstruction(new Hagrid());
        construction.initialiseGrid(mesh);
    }
}
