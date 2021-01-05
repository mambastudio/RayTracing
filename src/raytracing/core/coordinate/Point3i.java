/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.coordinate;

/**
 *
 * @author user
 */
public class Point3i {
    public int x, y, z;
    
    public Point3i(){}
    public Point3i(int v){x = y = z = v;}
    public Point3i(int x, int y, int z){this.x = x; this.y = y; this.z = z;}
    public Point3i(float x, float y, float z){this.x = (int) x; this.y = (int) y; this.z = (int) z;}
    public Point3i(Point3f p){this(p.x, p.y, p.z);}
    
    public static Point3i max(Point3i a, Point3i b)
    {
        return new Point3i(Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z));
    }
}
