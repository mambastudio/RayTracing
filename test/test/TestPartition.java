/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.util.Arrays;
import java.util.List;
import raytracing.core.grid.main.Partition;

/**
 *
 * @author user
 */
public class TestPartition {
    public static void main(String... args)
    {
        Partition<Integer> partition = new Partition();
        List<Integer> myList = Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5, 3, 3, 8);
        int value = partition.execute(myList, 5, 8, x -> x < 3);
        
        System.out.println(myList);
        System.out.println(value);
    }
}
