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
    public Point3i(Point3i p){this(p.x, p.y, p.z);}
    public Point3i(Point3f p){this(p.x, p.y, p.z);}
    public Point3i(Vector3f p){this(p.x, p.y, p.z);}
    
    public Point3i mul(Point3i a){return new Point3i(x * a.x, y * a.y, z * a.z); }
    public Point3i mul(int a){return new Point3i(x * a, y * a, z * a); }
    public Point3i div(Point3i a){return new Point3i(x / a.x, y / a.y, z / a.z); }
    public Point3i div(int a){return new Point3i(x / a, y / a, z / a); }
    public Point3i add(Point3i a){return new Point3i(x + a.x, y + a.y, z + a.z); }
    public Point3i add(int a){return new Point3i(x + a, y + a, z + a); }
    public Point3i sub(Point3i a){return new Point3i(x - a.x, y - a.y, z - a.z); }
    public Point3i sub(int a){return new Point3i(x - a, y - a, z - a); }
    public Point3i neg(){return new Point3i(-x, -y, -z);}
    
    public boolean hasNegative(){return x < 0 || y < 0 || z < 0;}
    
    public Point3i rightShift(int shift){return new Point3i(x >> shift, y >> shift, z >> shift);}
    public Point3i leftShift(int shift){return new Point3i(x << shift, y << shift, z << shift);}
    public Point3i and(int shift){return new Point3i(x & shift, y & shift, z & shift);}
    
    public static Point3i min(Point3i a, Point3i b){return new Point3i(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z));}
    public static Point3i max(Point3i a, Point3i b){return new Point3i(Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z));}
    public static Point3i clamp(Point3i a, int b, int c) { return new Point3i(Math.min(Math.max(a.x, b), c), Math.min(Math.max(a.y, b), c), Math.min(Math.max(a.z, b), c)); }   
    public static Point3i clamp(Point3i a, Point3i b, Point3i c) {return new Point3i(Math.min(Math.max(a.x, b.x), c.x), Math.min(Math.max(a.y, b.y), c.y), Math.min(Math.max(a.z, b.z), c.z)); }
        
    public int get(int axis) {
        switch (axis) {
            case 0:
                return x;
            case 1:
                return y;
            default:
                return z;
        }
    }
    
    public Point3i copy(){return new Point3i(x, y, z);}
    
    @Override
    public final String toString() {
        return String.format("(%3d, %3d, %3d)", x, y, z);
    }
}
