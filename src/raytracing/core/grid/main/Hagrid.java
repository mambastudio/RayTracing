/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid.main;

import static java.lang.Math.cbrt;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import raytracing.core.coordinate.BoundingBox;
import raytracing.core.coordinate.Point3f;
import raytracing.core.coordinate.Vector3f;
import static raytracing.core.grid.main.GridUtility.ENTRY_SHIFT;
import static raytracing.core.grid.main.GridUtility.closest_log2;
import raytracing.primitive.TriangleMesh;

/**
 *
 * @author user
 */
public class Hagrid implements HagridInterface
{
    static boolean SANITY_CHECKS = true;

    @Override
    public boolean build_grid(
            boolean do_merge, 
            boolean do_overlap, 
            boolean do_opt, 
            float alpha, 
            float top_density, 
            float snd_density, 
            TriangleMesh tris, 
            int[] dims, 
            int[] snd_dim, 
            BoundingBox grid_bb) 
    {        
        return false;
    }
    
    public static void compute_bboxes(GridInfo info, TriangleMesh tris, BoundingBox[] bboxes) 
    {        
        BoundingBox grid_bb = new BoundingBox();
        
        for(int i = 0; i<tris.getSize(); i++)
        {
            bboxes[i] = tris.getBound(i);
            grid_bb.include(bboxes[i]);            
        }
        info.bbox = grid_bb;
    }
    
    /// Computes the dimensions of a grid using the formula : R{x, y, z} = e{x, y, z} * (N * d / V)^(1/3).
    public static void compute_grid_dims(Vector3f e, int N, float d, int[] dims) {
        float V = e.x * e.y * e.z;
        float r = (float) (cbrt(d * N / V));

        dims[0] = max(1, (int)(e.x * r));
        dims[1] = max(1, (int)(e.y * r));
        dims[2] = max(1, (int)(e.z * r));
    }
    
    public static void gen_top_refs(GridInfo info,
                         BoundingBox[] bboxes,
                         TriangleMesh tris,
                         ArrayList<Ref> refs) 
    {
        Point3f cell_size = info.cell_size();
        Point3f inv_size  = new Point3f(1.0f/cell_size.x, 1.0f/cell_size.y, 1.0f/cell_size.z);
        Point3f inv_org   = info.bbox.minimum.mul(inv_size);
        
        int[] approx_ref_counts = new int[tris.getSize() + 1];
        
        for(int i = 0; i<tris.getSize(); i++)
        {
            Range range = Range.find_coverage(bboxes[i], inv_org, inv_size, info.dims);
            approx_ref_counts[i + 1] = range.size();
        }
        
        // Get the insertion position into the array of references
        approx_ref_counts[0] = 0;
         //std::partial_sum(approx_ref_counts.begin(), approx_ref_counts.end(), approx_ref_counts.begin());
        Arrays.parallelPrefix(approx_ref_counts, (x, y) -> x + y); //always an inclusive sum
        
        // Allocate and fill the array of references
        //refs.resize(approx_ref_counts.back(), Ref(-1, -1, -1));         
        int refsSize = approx_ref_counts[approx_ref_counts.length - 1];        
        GridUtility.fill(refsSize, refs, ()->new Ref(-1, -1, -1));
        
        GridUtility.parallelFor(0, tris.getSize(), i->{
            Range cov = Range.find_coverage(bboxes[i], inv_org, inv_size, info.dims);
            
            // Examine each cell and determine if the triangle is really inside
            AtomicInteger index = new AtomicInteger(approx_ref_counts[i]);
            cov.iterate((int x, int y, int z) -> {
                Point3f cell_min = info.bbox.minimum.add(new Point3f(x, y, z).mul(cell_size).asVector3f());
                Point3f cell_max = info.bbox.minimum.add(new Point3f(x + 1, y + 1, z + 1).mul(cell_size).asVector3f());
                
                if(Tri_Overlap_Box.tri_overlap_box(true, true, tris.getVertex1(i), tris.e1(i), tris.e2(i), tris.getNorm(i), cell_min, cell_max))
                        refs.set(index.getAndIncrement(), new Ref(i, x + info.dims[0] * (y + info.dims[1] * z), 0));                
               
            });
        });
        //Remove the references that were culled by the triangle-box test from the array
        refs.removeIf((Ref ref) -> {return ref.tri < 0;});
    }
    
    //DON'T FORGET TO DECLARE: snd_dims.resize(num_top_cells);
    public static void compute_snd_dims(GridInfo info, float snd_density, ArrayList<Ref> refs, int[] snd_dims) 
    {
        int num_top_cells = info.num_top_cells();
        Point3f cell_size = info.cell_size();
        
        // Compute the number of references per cell
        AtomicInteger[] cell_ref_counts = new AtomicInteger[num_top_cells];
        for (int i = 0; i < num_top_cells; i++) {
            cell_ref_counts[i] = new AtomicInteger(0);
        }
        
        GridUtility.parallelFor(0, refs.size(), i->{ 
            cell_ref_counts[refs.get(i).top_cell].incrementAndGet();
        });
        
        // Compute the second level dimensions for each top-level cell
        GridUtility.parallelFor(0, num_top_cells, i->{
            int inner_dims[] = new int[3];
            GridUtility.compute_grid_dims(cell_size, cell_ref_counts[i].get(), snd_density, inner_dims);
            int max_dim = max(inner_dims[0], max(inner_dims[1], inner_dims[2]));
            snd_dims[i] = min(closest_log2(max_dim), (1 << ENTRY_SHIFT) - 1);
        });
        
        info.max_snd_dim = Arrays.stream(snd_dims).max().getAsInt();//*std::max_element(snd_dims.begin(), snd_dims.end());
    
    }
    
    public static void remove_invalid_references(ArrayList<Integer> flags, ArrayList<Ref> refs, int first_ref) {
        // Same as std::remove_if except that we operate on two arrays at the same time (one could use boost::zip_iterator for this)
        int flags_it = flags.indexOf(0);
        if (flags_it != flags.size()) //in C++ std::vector::end means size() in java list
        {
            int refs_it = first_ref + (flags_it);
            int it1 = refs_it  + 1;
            int it2 = flags_it + 1;
            for (; it1 != refs.size(); ++it1, ++it2) {
                if (it2 != 0) {
                    //*(flags_it++) = std::move(*it2);
                    flags.set(flags_it++, flags.get(it2));
                    flags.set(it2, null);
                    //*(refs_it ++) = std::move(*it1);
                    refs.set(refs_it++, refs.get(it1));
                    refs.set(it1, null);
                }
            }
            
            if (SANITY_CHECKS) {
                int invalidReferences = flags.size() - flags_it;
                if (invalidReferences > 0) {
                    System.out.println("INVALID REFERENCES: " + (100.0f * (float) invalidReferences) / flags.size() + "%");
                } else {
                    System.out.println("No INVALID REFERENCES found");
                }
            }
            System.out.println(flags_it);
            System.out.println(refs_it);
            for(int i = flags_it; i<flags.size(); i++)
                flags.remove(i);
            for(int i = refs_it; i<refs.size(); i++)
                refs.remove(i);
        
        }              
    }
    
    public static void subdivide_refs(
            GridInfo info, TriangleMesh tris, int[] snd_dims, ArrayList<Ref> refs) {
        
        Point3f cell_size = info.cell_size();
        AtomicInteger first_ref = new AtomicInteger(0);
       
        Partition<Ref> partition = new Partition();
        
        // Subdivide until the maximum depth is reached
        AtomicInteger iter = new AtomicInteger(0);        
        while (iter.get() < info.max_snd_dim) {
            ArrayList<Integer> split_counts;
            ArrayList<Ref> new_refs;
           
             
            // Partition the set of references so that the ones that will not be subdivided anymore are in front
            first_ref.set(partition.execute(refs, first_ref.get(), refs.size(), ref->{ 
                return snd_dims[ref.top_cell] <= iter.get(); 
            }));
            
            int valid_refs = refs.size() - first_ref.get();
            if (valid_refs == 0) break;
            
             ArrayList<Integer> split_flags = new ArrayList(valid_refs);
                        
            // Compute, for each reference, how many sub-references will be created (up to 8 by reference)
            
            GridUtility.fill(valid_refs, split_flags, ()-> 0);            
            GridUtility.parallelFor(0, valid_refs, i->{
                Ref ref = refs.get(first_ref.get() + i);
                BoundingBox cell_box = GridUtility.compute_cell_box(info.dims, ref.top_cell, ref.snd_cell, info.bbox.minimum, cell_size, iter.get());
                Point3f center = cell_box.getCenter();
                
                int tri = ref.tri;
                int flag = 0;
                for (int j = 0; j < 8; j++) {
                    Point3f min = new Point3f(
                            (j & 1) != 0 ? center.x : cell_box.minimum.x,
                            (j & 2) != 0 ? center.y : cell_box.minimum.y,
                            (j & 4) != 0 ? center.z : cell_box.minimum.z);
                    Point3f max = new Point3f(
                            (j & 1) != 0 ? cell_box.maximum.x : center.x,
                            (j & 2) != 0 ? cell_box.maximum.y : center.y,
                            (j & 4) != 0 ? cell_box.maximum.z : center.z);

                    if (Tri_Overlap_Box.tri_overlap_box(true, true, tris.getVertex1(tri), tris.e1(tri), tris.e2(tri), tris.getNorm(tri), min, max)) 
                        flag |= 1 << j;
                }
                
                // The result is a bitfield whose popcount is the number of sub-refs for this reference
                split_flags.set(i, flag);
            });
            
            // Sometimes, a reference that is in a cell is in no sub-cell of that cell,
            // because of precision problems. We remove those problematic references here.
            //System.out.println(split_flags);
            System.out.println("s " +split_flags.size());
            remove_invalid_references(split_flags, refs, first_ref.get());
            System.out.println("s " +split_flags.size());
            System.out.println("v " +valid_refs);
            split_counts  = new ArrayList(valid_refs + 1);
            GridUtility.fill(valid_refs + 1, split_counts, ()-> 0);
            int pop_count[]  = {0, 1, 1, 2,
                                1, 2, 2, 3,
                                1, 2, 2, 3,
                                2, 3, 3, 4};
            // System.out.println(split_flags);
            System.out.println("valid refs " +valid_refs);
            System.out.println(split_flags.size());
            GridUtility.parallelFor(0, valid_refs, i->{
                //System.out.println(valid_refs);
                int flag = split_flags.get(i);
                // Could use the popcnt x86 instruction here or a similar one for other
                // architectures (but we only need 8 bits, and portability is a bigger issue)
                split_counts.set(i + 1, pop_count[flag & 0x0F] + pop_count[flag >> 4]);
            });
            
            // Sum the number of primitives split in order to know their insertion point in the array
            split_counts.set(0, first_ref.get());
            GridUtility.partial_sum(split_counts);
            
            // Allocate the new references
            new_refs = new ArrayList(split_counts.size());
            new_refs.addAll(refs.subList(0, first_ref.get()));

            //if(SANITY_CHECKS)
            ReentrantLock mut = new ReentrantLock();
            
            GridUtility.parallelFor(0, valid_refs, i->{
                Ref ref = refs.get(first_ref.get());
                int flag  = split_flags.get(i);
                int index = split_counts.get(i);
                
                for (int j = 0; j < 8; j++) {
                    if ((flag & (1 << j)) != 0) {
                        // Add one reference
                        int snd_cell = (int) ((ref.snd_cell << 3) | j);
                        new_refs.set(
                                index++, 
                                new Ref(ref.tri, ref.top_cell, snd_cell));
                    }
                }
                if(SANITY_CHECKS)
                {
                    if (index != split_counts.get(i + 1)) 
                    {
                        mut.lock();
                        try {
                            System.out.println("INCORRECT NUMBER OF REFERENCES SPLIT");
                        }
                        finally{
                            mut.unlock();
                        }
                    }
                }
            });
            
            // Transform the iterators into the new set of references
            first_ref.set(new_refs.size() - (refs.size() - first_ref.get()));
            GridUtility.swap(refs, new_refs);
            iter.incrementAndGet();
        }
    }
}
