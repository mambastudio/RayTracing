/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.coordinate;

import coordinate.generic.VCoord;

/**
 *
 * @author user
 */
public class Vector3f  implements VCoord<Vector3f>{
    
    public float x, y, z;
    
    public Vector3f() {x = 0; y = 0; z = 0;}
    public Vector3f(float a) {x = a; y = a; z = a;}
    public Vector3f(float a, float b, float c) {x = a; y = b; z = c;}
    public Vector3f(Vector3f a) {x = a.x; y = a.y; z = a.z;}

    public static Vector3f cross(Vector3f a, Vector3f b)
    {
        return a.cross(b);
    }
    
    public static float dot(Vector3f a, Vector3f b) {return a.x*b.x + a.y*b.y + a.z*b.z;}
    
    @Override
    public Vector3f getCoordInstance() {
        return new Vector3f();
    }

    @Override
    public Vector3f copy() {
        return new Vector3f(x, y, z);
    }

    @Override
    public float get(char axis) {
        
        switch (axis) {
            case 'x':
                return x;
            case 'y':
                return y;
            case 'z':
                return z;
            default:
                throw new UnsupportedOperationException("Invalid");
        }
    }

    @Override
    public void set(char axis, float value) {
         switch (axis) {
            case 'x':
                x = value;
                break;
            case 'y':
                y = value;
                break;
            case 'z':
                z = value;
                break;
            default:
                throw new UnsupportedOperationException("Invalid");
        }
    }

    @Override
    public void set(float... values) {
        x = values[0];
        y = values[1];
        z = values[2];
    }
       

    @Override
    public int getSize() {
        return 3;
    }

    @Override
    public float[] getArray() {
        return new float[] {x, y, z};
    }

    @Override
    public void setIndex(int index, float value) {
       switch (index)
        {
            case 0:
                x = value;
                break;
            case 1:
                y = value;
                break;    
            case 2:
                z = value;
                break;
        }
    }

    @Override
    public int getByteSize() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
