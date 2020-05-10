/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import coordinate.parser.obj.OBJParser;
import raytracing.accel.BVHAfra;
import raytracing.accel.BVHPlocTree;
import raytracing.primitive.TriangleMesh;

/**
 *
 * @author user
 */
public class TestPloc {
    public static void main(String... args)
    {
        OBJParser parser = new OBJParser();
        TriangleMesh mesh = new TriangleMesh();
        parser.read("C:\\Users\\user\\Documents\\3D Scenes\\Ajax\\Ajax_wall_emitter.obj", mesh);
        BVHAfra bvh = new BVHAfra();
        long time1 = System.nanoTime();
        bvh.build(mesh);
        long time2 = System.nanoTime();
        double mTime = (double)(time2 - time1)/1_000_000_000;
        System.out.printf("%.12f seconds \n", mTime);
    }
}
