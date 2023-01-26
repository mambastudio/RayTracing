/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid.main;

import coordinate.utility.Value3Df;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;
import static raytracing.core.grid.main.GridUtility.lookup_entry;
import static raytracing.core.grid.main.GridUtility.parallelFor;
import static raytracing.core.grid.main.GridUtility.swap;

/**
 *
 * @author user
 */
public class Merge {
    public static void merge_pairs(
            int top_entries,
            List<MergePair> pairs,
            List<Cell> cells,
            List<Integer> refs,
            List<Integer> entries) 
    {
        if (pairs.isEmpty()) return;
        
        //tbb::parallel_sort(pairs.begin(), pairs.end());
        Collections.sort(pairs, Comparator.comparingInt(MergePair::getFirst)
                        .thenComparingInt(MergePair::getSecond));
        
        // Build new references
        List<Integer> new_refs = new ArrayList();
        List<Integer> indices = new ArrayList(cells.size());
        int cur_id = 0, p = 0;
        for (int  i = 0; i < cells.size(); i++) {
            if (p < pairs.size() && pairs.get(p).first == i) {
                int e0 = i;
                int e1 = pairs.get(p).second;
                p++;

                if (e1 < i) continue;

                cells.get(cur_id).min[0] = min(cells.get(e0).min[0], cells.get(e1).min[0]);               
                cells.get(cur_id).min[1] = min(cells.get(e0).min[1], cells.get(e1).min[1]);
                cells.get(cur_id).min[2] = min(cells.get(e0).min[2], cells.get(e1).min[2]);
                cells.get(cur_id).max[0] = max(cells.get(e0).max[0], cells.get(e1).max[0]);
                cells.get(cur_id).max[1] = max(cells.get(e0).max[1], cells.get(e1).max[1]);
                cells.get(cur_id).max[2] = max(cells.get(e0).max[2], cells.get(e1).max[2]);

                // Merge references
                int begin = new_refs.size();
                int a = cells.get(e0).begin;
                int b = cells.get(e1).begin;
                while (a < cells.get(e0).end && b < cells.get(e1).end) {
                    if (refs.get(a) < refs.get(b)) {
                        new_refs.add(refs.get(a));
                        a++;
                    } else if (refs.get(a) > refs.get(b)) {
                        new_refs.add(refs.get(b));
                        b++;
                    } else {
                        new_refs.add(refs.get(a));
                        a++;
                        b++;
                    }
                }
                for (int j = a; j < cells.get(e0).end; j++) new_refs.add(refs.get(j));
                for (int j = b; j < cells.get(e1).end; j++) new_refs.add(refs.get(j));

                cells.get(cur_id).begin = begin;
                cells.get(cur_id).end = new_refs.size();
                indices.set(e1, cur_id);
            } 
            else 
            {
                int begin = new_refs.size();
                for(int j = cells.get(i).begin; j < cells.get(i).end; j++) 
                {
                    new_refs.add(refs.get(j));
                }
                cells.get(cur_id).min[0] = cells.get(i).min[0];
                cells.get(cur_id).min[1] = cells.get(i).min[1];
                cells.get(cur_id).min[2] = cells.get(i).min[2];
                cells.get(cur_id).max[0] = cells.get(i).max[0];
                cells.get(cur_id).max[1] = cells.get(i).max[1];
                cells.get(cur_id).max[2] = cells.get(i).max[2];
                
                cells.get(cur_id).begin = begin;
                cells.get(cur_id).end = new_refs.size();                
            }

            indices.set(i, cur_id++);
        }
        
        swap(refs, new_refs);        
        cells.subList(cur_id, cells.size()).clear(); //cells.resize(cur_id);
        
        IntStream.range(top_entries, entries.size())
            .parallel()
            .forEach(i -> entries.set(i, indices.get(entries.get(i))));
    }
    
    public static int merge(
            int iter,
            GridInfo info,
            List<Cell> cells,
            List<Integer> refs,
            List<Integer> entries) 
    {
        Value3Df cell_size = info.cell_size();
        int top_entries = info.num_top_cells();

        int dims[] = {
            info.dims[0] << info.max_snd_dim,
            info.dims[1] << info.max_snd_dim,
            info.dims[2] << info.max_snd_dim
        };
        
        List<AtomicBoolean> merge_flag = new ArrayList(Collections.nCopies(cells.size(), new AtomicBoolean(true)));
        List<MergePair> to_merge = new ArrayList();
        int total_merged = 0;
        ReentrantLock mut = new ReentrantLock();
        
        parallelFor(0, cells.size(), (i)->{
            Cell cell0 = cells.get(i);

            int x1 = cell0.max[0];
            int y1 = cell0.min[1];
            int z1 = cell0.min[2];
            
            if (x1 >= dims[0] || restricted_merge(cell0.min[0], info.max_snd_dim, iter)) 
                return;
            
            int entry = lookup_entry(entries, info.dims, info.max_snd_dim, x1, y1, z1);
            Cell cell1 = cells.get(entry);
            
            if (cell0.can_merge(0, cell1) && cell0.is_merge_profitable(cell_size, refs.stream().mapToInt(Integer::intValue).toArray(), cell1)) 
            {
                if (merge_flag.get(entry).compareAndSet(false, true))
                {
                    if (merge_flag.get(i).compareAndSet(false, true))
                    {
                        mut.lock();
                        try {
                            to_merge.add(new MergePair(i, entry));
                            to_merge.add(new MergePair(entry, i));
                        } finally {
                            mut.unlock();
                        }
                    }
                    else 
                    {
                        merge_flag.get(entry).set(true);
                    }
                }
            }
        });
        
        total_merged += to_merge.size() / 2;
        merge_pairs(top_entries, to_merge, cells, refs, entries);

        to_merge.clear();
        //std::fill(merge_flag.begin(), merge_flag.end(), true);
        merge_flag.forEach(merge->merge.set(true));
        
        parallelFor(0, cells.size(), (i)->{
            Cell cell0 = cells.get(i);

            int x1 = cell0.min[0];
            int y1 = cell0.max[1];
            int z1 = cell0.min[2];
            if (y1 >= dims[1] || restricted_merge(cell0.min[1], info.max_snd_dim, iter)) 
                return;
            
            int entry = lookup_entry(entries, info.dims, info.max_snd_dim, x1, y1, z1);
            Cell cell1 = cells.get(entry);
            if (cell0.can_merge(1, cell1) && cell0.is_merge_profitable(cell_size, refs.stream().mapToInt(Integer::intValue).toArray(), cell1)) 
            {
                if (merge_flag.get(entry).compareAndSet(false, true))
                {
                    if (merge_flag.get(i).compareAndSet(false, true))
                    {
                        mut.lock();
                        try {
                            to_merge.add(new MergePair(i, entry));
                            to_merge.add(new MergePair(entry, i));
                        } finally {
                            mut.unlock();
                        }
                    }
                    else 
                    {
                        merge_flag.get(entry).set(true);
                    }
                }
            }
        });
        
        total_merged += to_merge.size() / 2;
        merge_pairs(top_entries, to_merge, cells, refs, entries);

        to_merge.clear();
        //std::fill(merge_flag.begin(), merge_flag.end(), true);
        merge_flag.forEach(merge->merge.set(true));
        
        parallelFor(0, cells.size(), (i)->{
            Cell cell0 = cells.get(i);
            
            int x1 = cell0.min[0];
            int y1 = cell0.min[1];
            int z1 = cell0.max[2];
            if (z1 >= dims[2] || restricted_merge(cell0.min[2], info.max_snd_dim, iter)) 
                return;
            
            int entry = lookup_entry(entries, info.dims, info.max_snd_dim, x1, y1, z1);
            Cell cell1 = cells.get(entry);
            if (cell0.can_merge(2, cell1) && cell0.is_merge_profitable(cell_size, refs.stream().mapToInt(Integer::intValue).toArray(), cell1)) 
            {
                if (merge_flag.get(entry).compareAndSet(false, true))
                {
                    if (merge_flag.get(i).compareAndSet(false, true))
                    {
                        mut.lock();
                        try {
                            to_merge.add(new MergePair(i, entry));
                            to_merge.add(new MergePair(entry, i));
                        } finally {
                            mut.unlock();
                        }
                    }
                    else 
                    {
                        merge_flag.get(entry).set(true);
                    }
                }
            }            
        });
        
        total_merged += to_merge.size() / 2;
        merge_pairs(top_entries, to_merge, cells, refs, entries);

        return total_merged;
    }
    
     private static boolean restricted_merge(int x, int shift, int iter) {
        // Make sure top-level cells are aligned so that
        // they can be merged along another dimension
        int top_mask = (1 << shift) - 1;
        int empty_mask = iter > 3 ? 0 : (1 << (iter + 1)) - 1;
        return !((x & top_mask) != 0) && (((x >> shift) & empty_mask) != 0);
    }
}
