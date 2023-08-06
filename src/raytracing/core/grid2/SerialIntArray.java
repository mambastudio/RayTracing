/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid2;

import java.util.function.IntBinaryOperator;

/**
 *
 * @author user
 */
public interface SerialIntArray {
    public int[] transform(int[] values, IntBinaryOperator f);
    public int scan(int[] values, int n, int[] result);
    public int reduce(int[] values, int n, int[] result, IntBinaryOperator f);
    public int partition(int[] values, int[] result, int n, int[] flags);
    public void sort_pairs(int[] keys_in, int[] values_in, int[] keys_out, int[] values_out, int n);
}
