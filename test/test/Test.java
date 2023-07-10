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
import raytracing.core.coordinate.Point3i;
import raytracing.core.coordinate.Vector3f;
import raytracing.primitive.TriangleMesh;

/**
 *
 * @author user
 */
public class Test {
    public static void main(String... args)
    {
        int[] prefix = new int[]{1, 1, 1, 1, 1};
        Arrays.parallelPrefix(prefix, (x, y) -> x + y);
        
        System.out.println(Arrays.toString(prefix));
        
        Vector3f b = new Vector3f(0, 3, 0);
        Vector3f c = new Vector3f(2, 2, 2);
        
        System.out.println(Vector3f.min(b, c));
    }
            
}
