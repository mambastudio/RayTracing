/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import coordinate.parser.obj.OBJParser;
import raytracing.accel.BVHKarras;
import raytracing.primitive.TriangleMesh;

/**
 *
 * @author user
 */
public class TestKarrasBVH {
    public static void main(String... args)
    {
        OBJParser parser = new OBJParser();
        TriangleMesh mesh = new TriangleMesh();
        parser.read("C:\\Users\\user\\Documents\\3D Scenes\\mori_knob\\testObj.obj", mesh);
        
        BVHKarras bvh = new BVHKarras();
        bvh.build(mesh);
    }
}
