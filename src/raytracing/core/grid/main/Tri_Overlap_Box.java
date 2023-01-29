/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid.main;

import static java.lang.Math.max;
import static java.lang.Math.min;
import raytracing.core.coordinate.BoundingBox;
import raytracing.core.coordinate.Point3f;
import raytracing.core.coordinate.Vector3f;

/**
 *
 * @author user
 */
public class Tri_Overlap_Box 
{
    /// Tests for intersection between a plane and a box.
    public static boolean plane_overlap_box(Vector3f n, float d, Point3f min, Point3f max) {
        Point3f first = new Point3f(), last = new Point3f();
        
        first.x = n.x > 0 ? min.x : max.x;
        first.y = n.y > 0 ? min.y : max.y;
        first.z = n.z > 0 ? min.z : max.z;

        last.x = n.x <= 0 ? min.x : max.x;
        last.y = n.y <= 0 ? min.y : max.y;
        last.z = n.z <= 0 ? min.z : max.z;

        float d0 = Vector3f.dot(n, first.asVector3f()) - d; 
        float d1 = Vector3f.dot(n, last.asVector3f())  - d;
        
        return d0 * d1 <= 0.0f;
        
    }
    
    public static boolean axis_test_x(  Point3f half_size,
                                        Vector3f e, Point3f f,
                                        Vector3f v0, Vector3f v1) {
        float p0 = e.y * v0.z - e.z * v0.y;
        float p1 = e.y * v1.z - e.z * v1.y;
        float rad = f.z * half_size.y + f.y * half_size.z;
        return min(p0, p1) > rad | max(p0, p1) < -rad;
    }
    
    public static boolean axis_test_y(  Point3f half_size,
                                        Vector3f e, Point3f f,
                                        Vector3f v0, Vector3f v1) {
        float p0 = e.z * v0.x - e.x * v0.z;
        float p1 = e.z * v1.x - e.x * v1.z;
        float rad = f.z * half_size.x + f.x * half_size.z;
        return min(p0, p1) > rad | max(p0, p1) < -rad;
    }
    
    public static boolean axis_test_z(  Point3f half_size,
                                        Vector3f e, Point3f f,
                                        Vector3f v0, Vector3f v1) {
        float p0 = e.x * v0.y - e.y * v0.x;
        float p1 = e.x * v1.y - e.y * v1.x;
        float rad = f.y * half_size.x + f.x * half_size.y;
        return min(p0, p1) > rad | max(p0, p1) < -rad;
    }
    
    public static boolean tri_overlap_box(
            boolean bounds_check,
            boolean cross_checks,
            Point3f v0, 
            Vector3f e1, 
            Vector3f e2, 
            Vector3f n, 
            Point3f min, 
            Point3f max) 
    {        
        if (!plane_overlap_box(n, Vector3f.dot(v0.asVector3f(), n), min, max))
            return false;
        
        Vector3f v1 = v0.sub(e1.asPoint3f());
        Vector3f v2 = v0.add(e2.asPoint3f());
        
        BoundingBox b = new BoundingBox();
        b.include(v0, v1.asPoint3f(), v2.asPoint3f());
        
        if (bounds_check) {
            
            float min_x = min(v0.x, min(v1.x, v2.x));
            float max_x = max(v0.x, max(v1.x, v2.x));
            if (min_x > max.x || max_x < min.x) return false;
            
            float min_y = min(v0.y, min(v1.y, v2.y));
            float max_y = max(v0.y, max(v1.y, v2.y));
            if (min_y > max.y || max_y < min.y) return false;

            float min_z = min(v0.z, min(v1.z, v2.z));
            float max_z = max(v0.z, max(v1.z, v2.z));
            if (min_z > max.z || max_z < min.z) return false;       
            
        }
        
        if (cross_checks) {
            
            Point3f center    = (max.add(min)).mul(0.5f).asPoint3f();
            Point3f half_size = (max.sub(min)).mul(0.5f).asPoint3f();
                        
            Vector3f w0 = v0.sub(center);
            Vector3f w1 = v1.asPoint3f().sub(center);
            Vector3f w2 = v2.asPoint3f().sub(center);

            Point3f f1 = new Point3f(Math.abs(e1.x), Math.abs(e1.y), Math.abs(e1.z));
            if (axis_test_x(half_size, e1, f1, w0, w2) ||
                axis_test_y(half_size, e1, f1, w0, w2) ||
                axis_test_z(half_size, e1, f1, w1, w2))
                return false;
            
            Point3f f2 = new Point3f(Math.abs(e2.x), Math.abs(e2.y), Math.abs(e2.z));
            if (axis_test_x(half_size, e2, f2, w0, w1) ||
                axis_test_y(half_size, e2, f2, w0, w1) ||
                axis_test_z(half_size, e2, f2, w1, w2))
                return false;
           
            Vector3f e3 = e1.add(e2);
            
            Point3f f3 = new Point3f(Math.abs(e3.x), Math.abs(e3.y), Math.abs(e3.z));
            if (axis_test_x(half_size, e3, f3, w0, w2) ||
                axis_test_y(half_size, e3, f3, w0, w2) ||
                axis_test_z(half_size, e3, f3, w0, w1))
                return false;
            
        } 
        return true;
    }

}
