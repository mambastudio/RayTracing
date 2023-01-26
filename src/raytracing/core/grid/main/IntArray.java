/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid.main;

import java.util.Arrays;

/**
 *
 * @author user
 */
public class IntArray {
    private final int[] array;
    private final int offset;
    private int size;
            
    public IntArray(int[] array)
    {
        if(array == null)
            throw new NullPointerException("array is null");
        this.array = array;
        this.offset = 0;
        this.size = array.length;
    }
    
    private IntArray(IntArray intArray, int offset, int fromIndex, int toIndex)
    {
        rangeCheckBound(fromIndex, toIndex, intArray.size);
        this.array = intArray.getWholeArray();
        this.offset = offset + fromIndex;
        this.size = toIndex - fromIndex;
    }
    
    public static IntArray createFromArray(int... array)
    {
        return new IntArray(array);
    }
    
    public static IntArray createFromSize(int size)
    {
        return new IntArray(new int[size]);
    }
    
    public IntArray getSubArray(int start, int end)
    {
        return new IntArray(this, offset, start, end);
    }
    
    public IntArray splitSubArrayFrom(int start)
    {
        return getSubArray(start, this.size());
    }
    
    public void set(int index, int value)
    {
        rangeCheck(index);
        this.array[offset + index] = value;
    }
    
    public int get(int index)
    {
        rangeCheck(index);
        return this.array[offset + index];
    }
    
    public int[] getCopyRangeArray()
    {
        return Arrays.copyOfRange(array, offset, offset + size);
    }
    
    public int[] getWholeArray()
    {
        return array;
    }
    
    public int size()
    {
        return size;
    }
    
    private void rangeCheck(int index) {
        if (index < 0 || index >= this.size)
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }
    
    private String outOfBoundsMsg(int index) {
        return "Index: "+index+", Size: "+this.size;
    }
    
    
    private void rangeCheckBound(int fromIndex, int toIndex, int size) {
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
        if (toIndex > size)
            throw new IndexOutOfBoundsException("toIndex = " + toIndex);
        if (fromIndex > toIndex)
            throw new IllegalArgumentException("fromIndex(" + fromIndex +
                                               ") > toIndex(" + toIndex + ")");
    }
    
    @Override
    public String toString()
    {
        return Arrays.toString(getCopyRangeArray());
    }
}