/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.util.Arrays;
import raytracing.core.MortonCode.MortonData;

/**
 *
 * @author user
 */
public class SimpleTest {
    public static void main(String... args)
    {
        int index = 127;
        int div   = 64;
        int value = 0;
        
        if (index % div == 0) { 
            value = (int) ((Math.floor(index / div)) * div); 
        } else { 
            value = (int) ((Math.floor(index / div)) * div) + div; 
        } 
        
        System.out.println(value);
    }
}
