/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.util.Arrays;
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
        testSwapIntArray();
        //testTriangleBox();
        //testPartition();
        //test();
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
    
    public static void testSwapIntArray()
    {
        IntArray x = new IntArray(new int[]{0, 0, 0, 0, 1, 1, 1, 1});
        IntArray y = new IntArray(new int[]{1, 1, 1, 1, 0, 0, 0, 0});
        
        IntArray xx = x.splitSubArrayFrom(4);
        IntArray yy = y.splitSubArrayFrom(4);
        
        x.swap(y);
        xx.swap(yy);
        
        System.out.println(x);
        System.out.println(y);
        
        System.out.println(xx);
        System.out.println(yy);
        
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
        TriSimple tri = new TriSimple(new Point3f(0, 0, 5), new Point3f(0, 5f, 0), new Point3f(5, 0, 0));
        BoundingBox box = new BoundingBox(new Point3f(), new Point3f(2, 2, 2));
        
        boolean intersect = tri_overlap_box(true, false, 
                tri.v0(), tri.e1(), tri.e2(), tri.normal(), 
                box.minimum, box.maximum) ;
        
        System.out.println(intersect);
    }
    
    public static void testPartition()
    {
        IntArray d_in = new IntArray(new int[]{1, 2, 3, 4, 5, 6, 7, 8});
        IntArray d_flags =  new IntArray(new int[]{0, 0, 0, 0, 0, 0, 0, 0});
        IntArray d_out = new IntArray(new int[d_in.size()]);
        int d_num_selected_out = partition2(d_in, d_out, d_in.size(), d_flags);
        
        System.out.println(d_in);
        System.out.println(d_flags);
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
        int selected_index = 0;
        int remaining_index = n - 1;
        for (int i = 0; i < n; i++) {
            if (flags.get(i) == 1) {
                output.set(selected_index++, input.get(i));
            } else {
                output.set(remaining_index--, input.get(i));
            }
        }

        return selected_index;
    }
    
    public static void test()
    {
        int num_items = 8;
        int[] d_in = {1, 2, 3, 4, 5, 6, 7, 8};
        char[] d_flags = {1, 0, 0, 1, 0, 0, 0, 0};
        int[] d_out = new int[num_items];
        int[] d_num_selected_out = new int[1];

        int selected_index = 0;
        int remaining_index = num_items - 1;
        for (int i = 0; i < num_items; i++) {
            if (d_flags[i] == 1) {
                d_out[selected_index++] = d_in[i];
            } else {
                d_out[remaining_index--] = d_in[i];
            }
        }

        d_num_selected_out[0] = selected_index;
        
        System.out.println(Arrays.toString(d_out));
        System.out.println(d_num_selected_out[0]);
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
