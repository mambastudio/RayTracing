/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import coordinate.memory.nativememory.NativeInteger;
import coordinate.parser.obj.OBJParser;
import java.util.Arrays;
import java.util.Comparator;
import raytracing.accel.BVHAfra;
import raytracing.accel.HLBVH;
import raytracing.core.coordinate.Point3i;
import raytracing.core.coordinate.Vector3f;
import raytracing.accel.grid.offheap.base.NEntry;
import raytracing.primitive.TriangleMesh;

/**
 *
 * @author user
 */
public class Test {
    public static void main(String... args)
    {
        NativeInteger n = new NativeInteger(2);
        n.set(0, new NEntry(2, 3134543));
        n.set(1, new NEntry(3, 234234));
        System.out.println(n.getString(0, n.capacity(), new NEntry()));
        
    }
            
}
