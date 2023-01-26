/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;
import raytracing.core.grid.main.GridUtility;

/**
 *
 * @author user
 */
public class Test2 {
    public static void main(String... args)
    {
       // List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
       // GridUtility.partial_sum(list);
       // System.out.println(list);
        
        int[] array = new int[]{1, 2, 3, 4, 5};
        Arrays.parallelPrefix(array, 0, array.length, (a, b)-> a+b);
        System.arraycopy(array, 0, array, 1, array.length - 1);
        array[0] = 0;
        System.out.println(Arrays.toString(array));
    }
    
    public static int __ffs(int value)
    {               
        return Integer.numberOfTrailingZeros(value);
    }
}
