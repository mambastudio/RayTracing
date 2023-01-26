/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid.main;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 *
 * @author user
 * @param <T>
 */
public class Partition<T>  {
    public int execute(List<T> list, Predicate<T> predicate) {
        int i = 0;
        int j = list.size() - 1;
        while (i <= j) {
            while (i <= j && predicate.test(list.get(i))) {
                i++;
            }
            while (i <= j && !predicate.test(list.get(j))) {
                j--;
            }
            if (i <= j) {
                Collections.swap(list, i, j);
                i++;
                j--;
            }
        }
        return i;
    }
    
    public int execute(List<T> list, int start, int end, Predicate<T> predicate) {
        int i = start;
        int j = end - 1;
        while (i <= j) {
            while (i <= j && predicate.test(list.get(i))) {
                i++;
            }
            while (i <= j && !predicate.test(list.get(j))) {
                j--;
            }
            if (i <= j) {
                Collections.swap(list, i, j);
                i++;
                j--;
            }
        }
        return i;
    }    
}
