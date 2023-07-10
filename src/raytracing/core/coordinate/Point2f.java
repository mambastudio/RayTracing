/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.coordinate;

import coordinate.generic.AbstractCoordinateFloat;

/**
 *
 * @author user
 */
public class Point2f implements AbstractCoordinateFloat{
    public float x, y;
    
    public Point2f(){}
    public Point2f(float x, float y){this.x = x; this.y = y;}
    
    @Override
    public float get(char axis) {
        switch (axis)
        {
            case 'x' : 
                return x;                
            case 'y' :
                return y;
            default :
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    @Override
    public void set(char axis, float value) {
        switch (axis)
        {
            case 'x' : 
                this.x = value;                
            case 'y' :
                this.y = value;
            default :
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    @Override
    public void set(float... values) {
        x = values[0];
        y = values[1];        
    }

    @Override
    public int getSize() {
        return 2;
    }

    @Override
    public float[] getArray() {
        return new float[]{x, y};
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
        }
    }

    @Override
    public int getByteSize() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public String toString()
    {
        float[] array = getArray();
        return String.format("(%3.2f, %3.2f)", array[0], array[1]);
    }
}
