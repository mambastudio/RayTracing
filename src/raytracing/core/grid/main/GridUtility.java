/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid.main;

import static java.lang.Math.max;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import raytracing.core.coordinate.BoundingBox;
import raytracing.core.coordinate.Point3f;

/**
 *
 * @author user
 */
public class GridUtility {
    static int ENTRY_SHIFT = 4;
    
    /// Counts the number of elements in the union of two sorted arrays.
    public static int count_union(int[] p0, int c0, int[] p1, int c1) {
        int i = 0;
        int j = 0;

        int count = 0;
        while (i < c0 & j < c1) {
            int k0 = p0[i] <= p1[j] ? 1 : 0;
            int k1 = p0[i] >= p1[j] ? 1 : 0;
            i += k0;
            j += k1;
            count++;
        }

        return count + (c1 - j) + (c0 - i);
    }
    
    public static int countUnion(int[] array1, int[] array2) {
        int i = 0;
        int j = 0;

        int count = 0;
        while (i < array1.length && j < array2.length) {
            if (array1[i] <= array2[j]) {
                i++;
            }
            if (array1[i] >= array2[j]) {
                j++;
            }
            count++;
        }

        return count + (array2.length - j) + (array1.length - i);
    }
    
    public static<T> void swap(List<T> list1, List<T> list2)
    {
        List<T> temp1 = new ArrayList<>(list1);
        List<T> temp2 = new ArrayList<>(list2);
        
        list1.clear(); list2.clear();

        list1.addAll(temp2.stream().collect(Collectors.toList()));
        list2.addAll(temp1.stream().collect(Collectors.toList()));
    }
    
    public static void parallelFor(int start, int end, IntConsumer body) {
        IntStream.range(start, end).parallel().forEach(i->{
            body.accept(i);
        });        
    }
    
    public static void partial_sum(List<Integer> list)
    {
        Integer[] arr = list.toArray(new Integer[0]);
        Arrays.parallelPrefix(arr, Integer::sum);        
        for (int i = 0; i < arr.length; i++) {
            list.set(i, arr[i]);
        }
        
    }
    
    /// Returns the log of the dimension in stored in the top-level given voxel map entry.
    public static int entry_log_dim(int entry) {
        return entry & ((1 << ENTRY_SHIFT) - 1);
    }

    /// Returns the pointer to the second-level stored in the given top-level voxel map entry.
    public static int entry_begin(int entry) {
        return entry >> ENTRY_SHIFT;
    }

    /// Creates a top-level voxel map entry.
    public static int make_entry(int begin, int log_dim) {
        assert(log_dim < (1 << ENTRY_SHIFT) &&
               begin < (1 << (32 - ENTRY_SHIFT)));
        return (begin << ENTRY_SHIFT) | log_dim;
    }
    
    /// Lookups an entry in the voxel map.
    public static int lookup_entry(List<Integer> entries, int[] coarse_dims, int shift, int x, int y, int z) {
        int entry = entries.get((x >> shift) + coarse_dims[0] * ((y >> shift) + coarse_dims[1] * (z >> shift)));
        int log_dim = entry_log_dim(entry);
        int mask = (1 << log_dim) - 1;
        int begin = entry_begin(entry);
        int kx = (x >> (shift - log_dim)) & mask;
        int ky = (y >> (shift - log_dim)) & mask;
        int kz = (z >> (shift - log_dim)) & mask;
        return entries.get(begin + kx + ((ky + (kz << log_dim)) << log_dim));
    }
    
    public static<T> void fill(int count, ArrayList<T> list, Supplier<T> supplier)
    {
        for(int i = 0; i<count; i++)
            list.add(i, supplier.get());
    }
    
     /// Computes the dimensions of a grid using the formula : R{x, y, z} = e{x, y, z} * (N * d / V)^(1/3).
    public static void compute_grid_dims(Point3f e, int N, float d, int[] dims) {
        float V = e.x * e.y * e.z;
        float r = (float) (Math.cbrt(d * N / V));
        dims[0] = max(1, (int)(e.x * r));
        dims[1] = max(1, (int)(e.y * r));
        dims[2] = max(1, (int)(e.z * r));
    }

    public static int closest_log2(int k) {
        // One could use a CLZ instruction if the hardware supports it
        int i = 0;
        while ((1 << i) < k) i++;
        return i;
    }
    
    public static Point3f compute_cell_pos(long snd_cell, Point3f cell_size) {
        Point3f cur_size = cell_size.copy();
        Point3f pos = new Point3f();
        while (snd_cell > 0) {
            pos.x += (snd_cell & 1) != 0 ? cur_size.x : 0.0f;
            pos.y += (snd_cell & 2) != 0 ? cur_size.y : 0.0f;
            pos.z += (snd_cell & 4) != 0 ? cur_size.z : 0.0f;
            cur_size.mulAssign(2.0f);
            snd_cell >>= 3;
        }
        return pos;
    }
    
    public static BoundingBox compute_cell_box(int[] dims, int top_cell, long snd_cell, Point3f org, Point3f cell_size, int iter) {
        int x = top_cell % dims[0];
        int y = (top_cell / dims[0]) % dims[1];
        int z = top_cell / (dims[0] * dims[1]);
        Point3f sub_cell_size = cell_size.mul(1.0f / (1 << iter));
        Point3f pos = compute_cell_pos(snd_cell, sub_cell_size).addS(cell_size.mul(new Point3f(x, y, z))).addS(org);
        BoundingBox box = new BoundingBox();
        box.include(pos, pos.addS(sub_cell_size));
        return box;
    }
    
    public static void profile(String message, ConsumerVoid consume)
    {
        System.out.println(message);
        consume.accept();
    }
}
