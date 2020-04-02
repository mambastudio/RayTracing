/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import coordinate.parser.obj.OBJParser;
import java.util.Arrays;
import java.util.Comparator;
import raytracing.accel.BVHAfra;
import raytracing.accel.HLBVH;
import raytracing.primitive.TriangleMesh;

/**
 *
 * @author user
 */
public class Test {
    public static void main(String... args)
    {
        OBJParser parser = new OBJParser();
        TriangleMesh mesh = new TriangleMesh();
        parser.read("C:\\Users\\user\\Documents\\NetBeansProjects\\RayTracing\\SimpleMesh.obj", mesh);
        BVHAfra bvh = new BVHAfra();
        bvh.build(mesh);
       
    }
            
}
