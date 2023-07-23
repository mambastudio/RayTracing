/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid2;

import coordinate.memory.NativeInteger;
import coordinate.memory.NativeObject;
import java.util.function.BiConsumer;
import raytracing.core.Intersection;
import raytracing.core.coordinate.Point3i;
import raytracing.core.grid2.base.Cell2;
import raytracing.core.grid2.base.Entry2;

/**
 *
 * @author user
 */
public class GridAbstract2 {
    public int __ffs(int value)            
    {      
        //https://en.wikipedia.org/wiki/Find_first_set
        return value == 0 ? 0 : Integer.numberOfTrailingZeros(value) + 1;
    }
    
    public int __popc(int mask) {
        return Integer.bitCount(mask); 
    }
    
    public int __clz(int k)
    {          
        return Integer.numberOfLeadingZeros(k);
    }
    
    public int log2nlz(int bits)
    {
        return bits == 0 ? 0 : 31 - Integer.numberOfLeadingZeros(bits);
    }    
    
    /// Returns a voxel map entry with the given dimension and starting index
    public Entry2 make_entry(int log_dim, int begin) {
        Entry2 e = new Entry2(log_dim, begin);
        return e;
    }
    
    public int lookup_entry(NativeObject<Entry2> entries, int shift, Point3i dims, Point3i voxel) {
        Entry2 entry = entries.get((voxel.x >> shift) + dims.x * ((voxel.y >> shift) + dims.y * (voxel.z >> shift)));
        int log_dim = entry.log_dim, d = log_dim;
        while (log_dim != 0) {
            int begin = entry.begin;
            int mask = (1 << log_dim) - 1;

            //auto k = (voxel >> int(shift - d)) & mask;
            Point3i k = voxel.rightShift(shift -d).and(mask);
            entry = entries.get(begin + k.x + ((k.y + (k.z << log_dim)) << log_dim));
            log_dim = entry.log_dim;
            d += log_dim;
        }
        return entry.begin;
    }
    
    public int foreach_ref(Cell2 cell, Intersection isect, NativeInteger ref_ids, BiConsumer<Integer, Intersection> f) {
        int cur = cell.begin, ref = cur < cell.end ? ref_ids.get(cur++) : -1;
        while (ref >= 0) {
            // Preload the next reference
            int next = cur < cell.end ? ref_ids.get(cur++) : -1;
            f.accept(ref, isect);
            ref = next;
        }
        return cell.end - cell.begin;
    }
}
