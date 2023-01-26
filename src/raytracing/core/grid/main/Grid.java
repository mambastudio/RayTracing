/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid.main;

import coordinate.generic.SCoord;
import static java.lang.Math.max;
import java.util.List;
import raytracing.core.coordinate.BoundingBox;
import raytracing.core.coordinate.Point3f;
import raytracing.core.coordinate.Vector3f;
import static raytracing.core.grid.main.GridUtility.parallelFor;

/**
 *
 * @author user
 */
public class Grid {
    /// Counts the number of elements in the union of two sorted arrays.
    int count_union(int[] p0, int c0, int[] p1, int c1) {
        int i = 0;
        int j = 0;

        int count = 0;
        while (i < c0 & j < c1) {
            int k0 = p0[i] <= p1[j] ? 1 : 0;
            int k1 = p0[i] >= p1[j] ? 1 : 0;
            i += k0;
            j += k1;
            count++;
        }

        return count + (c1 - j) + (c0 - i);
    }
    
   
    
    public static void compute_bboxes(GridInfo info, List<Tri> tris, List<BoundingBox> bboxes) {
        parallelFor(0, tris.size(), i->{
            Tri tri = tris.get(i);
            Vector3f v0 = new Vector3f(tri.v0.x, tri.v0.y, tri.v0.z);
            Vector3f e1 = new Vector3f(tri.e1.x, tri.e1.y, tri.e1.z);
            Vector3f e2 = new Vector3f(tri.e2.x, tri.e2.y, tri.e2.z);
            Vector3f v1 = v0.sub(e1);
            Vector3f v2 = v0.add(e2);
            
            BoundingBox bbox = new BoundingBox();
            bbox.minimum = (Point3f) SCoord.min3(v0.asPoint3f(), SCoord.min3(v1.asPoint3f(), v2.asPoint3f()));
            bbox.maximum = (Point3f) SCoord.max3(v0.asPoint3f(), SCoord.max3(v1.asPoint3f(), v2.asPoint3f()));
            bboxes.add(bbox);
            
            synchronized (info) {
                info.bbox.include(bbox);
            }
        });       
    }
}
