/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import raytracing.core.coordinate.BoundingBox;
import raytracing.core.coordinate.Point3f;
import raytracing.core.grid.main.IntArray;
import static raytracing.core.grid.main.Tri_Overlap_Box.tri_overlap_box;

/**
 *
 * @author user
 */
public class Test3 {
    public static void main(String... args)
    {
        //testIntArray();
        //testTriangleBox();
        testPartition();
        //swapTwoArrays();
        //int mask = ballot_sync(new boolean[]{true, false, false});        
        //System.out.println(Integer.toString(mask, 2));
        //System.out.println(31 - Integer.numberOfTrailingZeros(mask));
    }
    
    public static void testIntArray()
    {
        IntArray array = new IntArray(new int[]{4, 3, 4, 9, 0, 1, 3, 4});
        System.out.println(array);
        IntArray sArray = array.getSubArray(1, 7);
        System.out.println(sArray);
        System.out.println(sArray.getSubArray(1, 3));
        System.out.println(sArray.get(5));
    }
    
    public static void run()
    {
        long mask = Long.parseLong("100000000000000000000000000000000000000000", 2);
        while (mask != 0) {
            long bit = Long.numberOfTrailingZeros(mask);
            mask &= ~(1L << bit);
            System.out.println(bit);
            System.out.println(mask);
        }
    }
    
    public static void reverse()
    {
        int b = Integer.parseInt("10011", 2);     
        int v = Integer.reverse(b);
        System.out.println(Integer.toBinaryString(v));
    }
    
    public static int ballot_sync(boolean[] array)
    {
        int i = 0;
        for(boolean b:array)i=i*2+(b?1:0);
        return i;
    }
    
    public static void testTriangleBox()
    {
        TriSimple tri = new TriSimple(new Point3f(0, 0, 7), new Point3f(0, 4.5f, 0), new Point3f(7.5f, 0, 0));
        BoundingBox box = new BoundingBox(new Point3f(), new Point3f(2, 2, 2));
        
        boolean intersect = tri_overlap_box(false, true, 
                tri.v0(), tri.e1(), tri.e2(), tri.normal(), 
                box.minimum, box.maximum) ;
        
        System.out.println(intersect);
    }
    
    public static void testPartition()
    {
        IntArray d_in = new IntArray(new int[]{1, 2, 3, 4, 5, 6, 7, 8});
        IntArray d_flags =  new IntArray(new int[]{1, 0, 0, 1, 0, 1, 1, 0});
        IntArray d_out = new IntArray(new int[d_in.size()]);
        int d_num_selected_out = partition(d_in, d_out, d_in.size(), d_flags);
        
        System.out.println(d_num_selected_out);
        System.out.println(d_out);
        
    }
            
    public static int partition(IntArray input, IntArray output, int n, IntArray flags)
    {
        System.arraycopy(input.getWholeArray(), 0, output.getWholeArray(), 0, n);
        int start = 0;
        int mid = start;
        for(int i = start; i < n; i++)
        {
            if(flags.get(i)!=0)
            {
                swap(i, mid, output.getWholeArray());                
                mid++;
            }
        }
        return mid;
    }
    
    public static int partition2(IntArray input, IntArray output, int n, IntArray flags)
    {
        int count_ptr = 0;
        int outputIndex = 0;
        for (int i = 0; i < flags.size(); i++) {
            if (flags.get(i) == 1) {
                output.set(outputIndex++, input.get(i));
                count_ptr++;
            }
        }

        for (int i = flags.size() - 1; i >= 0; i--) {
            if (flags.get(i) == 0) {
                output.set(outputIndex++, input.get(i));               
            }
        }
        return count_ptr;
    }
    
    private static void swap(int i, int j, int... arr)
    {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }
    
    public static void swapTwoArrays()
    {
        IntArray x = new IntArray(new int[]{1, 3, 5, 3});
        IntArray y = new IntArray(new int[]{2, 4, 6, 8});
        
        swap(x, y);
        
        System.out.println(y);
    }
    
    protected static void swap(IntArray x, IntArray y)
    {
        if(x.size() != y.size())
            throw new UnsupportedOperationException("no swap since the two arrays are not equal");
        int temp;
        for(int i = 0; i<x.size(); i++)
        {
            temp = x.get(i);
            x.set(i, y.get(i));
            y.set(i, temp);
        }
    }
}
