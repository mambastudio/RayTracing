/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid;

import static coordinate.utility.Utility.clamp;
import static java.lang.Math.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import raytracing.core.coordinate.BoundingBox;
import raytracing.core.coordinate.Point3f;
import raytracing.core.coordinate.Vector3f;
import static raytracing.core.grid.main.Common.closest_log2;
import raytracing.core.grid.main.GridInfo;
import raytracing.core.grid.main.Range;
import raytracing.core.grid.main.Ref;
import raytracing.core.grid.main.TriOverlapBox;
import static raytracing.core.grid.main.TriOverlapBox.tri_overlap_box;
import static raytracing.core.grid.main.VoxelMap.ENTRY_SHIFT;
import raytracing.primitive.TriangleMesh;

/**
 *
 * @author user
 */
public class Grid {
    

       
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
    
    /// Computes the range of voxels covered by a bounding box on a grid.
    public static Range find_coverage(BoundingBox bb, Point3f inv_org, Point3f inv_size, int[] dims) {
        int lx = (int)clamp(bb.minimum.x * inv_size.x - inv_org.x, 0, dims[0] - 1);
        int ly = (int)clamp(bb.minimum.y * inv_size.y - inv_org.y, 0, dims[1] - 1);
        int lz = (int)clamp(bb.minimum.z * inv_size.z - inv_org.z, 0, dims[2] - 1);

        int hx = (int)clamp(bb.maximum.x * inv_size.x - inv_org.x, 0, dims[0] - 1);
        int hy = (int)clamp(bb.maximum.y * inv_size.y - inv_org.y, 0, dims[1] - 1);
        int hz = (int)clamp(bb.maximum.z * inv_size.z - inv_org.z, 0, dims[2] - 1);

        return new Range(lx, hx, ly, hy, lz, hz);
    }
    
    /// Computes the dimensions of a grid using the formula : R{x, y, z} = e{x, y, z} * (N * d / V)^(1/3).
    public static void compute_grid_dims(Point3f e, int N, float d, int[] dims) {
        float V = e.x * e.y * e.z;
        float r = (float) (cbrt(d * N / V));
        dims[0] = max(1, (int)(e.x * r));
        dims[1] = max(1, (int)(e.y * r));
        dims[2] = max(1, (int)(e.z * r));
    }
    
    public static void compute_bboxes(GridInfo info, TriangleMesh tris, BoundingBox[] bboxes) 
    {
        bboxes = new BoundingBox[tris.getSize()];
        BoundingBox grid_bb = new BoundingBox();
        
        for(int i = 0; i<tris.getSize(); i++)
        {
            bboxes[i] = tris.getBound(i);
            grid_bb.include(bboxes[i]);
        }
        
        info.bbox = grid_bb;
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
            Range range = Grid.find_coverage(bboxes[i], inv_org, inv_size, info.dims);
            approx_ref_counts[i + 1] = range.size();
        }
        
        // Get the insertion position into the array of references
        approx_ref_counts[0] = 0;
         //std::partial_sum(approx_ref_counts.begin(), approx_ref_counts.end(), approx_ref_counts.begin());
        Arrays.parallelPrefix(approx_ref_counts, (x, y) -> x + y); //always an inclusive sum
        
        // Allocate and fill the array of references
        //refs.resize(approx_ref_counts.back(), Ref(-1, -1, -1));
        //CONFIRM IF THIS IS RIGHT
        refs = new ArrayList<>(approx_ref_counts[approx_ref_counts.length - 1]);        
        final ArrayList<Ref> refss = refs;
        
        Collections.fill(refs, new Ref(-1, -1, -1));
        
        for(int i = 0; i<tris.getSize(); i++)
        {
            final int ii = i;
            Range cov = find_coverage(bboxes[i], inv_org, inv_size, info.dims);
           
            //    const Tri& tri = tris[i];
            
            // Examine each cell and determine if the triangle is really inside
            AtomicInteger index = new AtomicInteger(approx_ref_counts[i]);
            cov.iterate((int x, int y, int z)->{
                Point3f cell_min = info.bbox.minimum.add(new Point3f(x, y, z).mul(cell_size).asVector3f());
                Point3f cell_max = info.bbox.minimum.add(new Point3f(x + 1, y + 1, z + 1).mul(cell_size).asVector3f());
             
                if(tri_overlap_box(tris.getVertex1(ii), tris.e1(ii), tris.e2(ii), tris.getNorm(ii), cell_min, cell_max))
                        refss.set(index.getAndIncrement(), new Ref(ii, x + info.dims[0] * (y + info.dims[1] * z), 0));
                    
            });
        }        
        refs.removeIf((Ref ref) -> {return ref.tri < 0;});
    }
    
    public static void compute_snd_dims(GridInfo info, float snd_density, ArrayList<Ref> refs, int[] snd_dims) 
    {
        int num_top_cells = info.num_top_cells();
        Point3f cell_size = info.cell_size();

        // Compute the number of references per cell
        //std::vector<atomic<uint32_t> > cell_ref_counts(num_top_cells, 0);
        int[] cell_ref_counts = new int[num_top_cells];
        refs.forEach((ref) -> {
            cell_ref_counts[ref.top_cell]++;
        });
        
        snd_dims = new int[num_top_cells];
        
        for(int i = 0; i<num_top_cells; i++)
        {
            int inner_dims[] = new int[3];
            compute_grid_dims(cell_size, cell_ref_counts[i], snd_density, inner_dims);
            int max_dim = max(inner_dims[0], max(inner_dims[1], inner_dims[2]));
            snd_dims[i] = min(closest_log2(max_dim), (1 << ENTRY_SHIFT) - 1);
        }
        
        info.max_snd_dim = Arrays.stream(snd_dims).max().getAsInt();//*std::max_element(snd_dims.begin(), snd_dims.end());
    }
    
    public static Point3f compute_cell_pos(long snd_cell, Point3f cell_size) {
        Point3f cur_size = cell_size;
        Point3f pos = new Point3f();
        while (snd_cell > 0) {
            pos.x += (snd_cell & 1) > 0 ? cur_size.x : 0.0f;
            pos.y += (snd_cell & 2) > 0 ? cur_size.y : 0.0f;
            pos.z += (snd_cell & 4) > 0 ? cur_size.z : 0.0f;
            cur_size.mulAssign(2.0f);
            snd_cell >>= 3;
        }
        return pos;
    }
}
