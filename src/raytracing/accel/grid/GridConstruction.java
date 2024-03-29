/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.accel.grid;

import raytracing.primitive.TriangleMesh;

/**
 *
 * @author user
 * @param <G>
 * @param <H>
 */
public abstract class GridConstruction<G extends GridMap, H extends HagridSetup<G>> {
    
    protected H hagrid;
    
    protected GridConstruction(H hagrid)
    {
        this.hagrid = hagrid;
    }
    
    public abstract G initialiseGrid(TriangleMesh tris);
    
    /// Builds an initial irregular grid.
    /// The building process starts by creating a uniform grid of density 'top_density',
    /// and then proceeds to compute an independent resolution in each of its cells
    /// (using the second-level density 'snd_density').
    /// In each cell, an octree depth is computed from these independent resolutions
    /// and the primitive references are split until every cell has reached its maximum depth.
    /// The voxel map follows the octree structure.
    //public void build_grid(Tri[] tris, int num_tris, Grid grid, float top_density, float snd_density);
    protected abstract void build_grid(TriangleMesh tris);

    /// Performs the neighbor merging optimization (merging cells according to the SAH).
    /// public void merge_grid(Grid grid, float alpha);
    protected abstract void merge_grid();

    /// Flattens the voxel map to speed up queries.
    /// Once this optimization is performed, the voxel map no longer follows an octree structure.
    /// Each inner node of the voxel map now may have up to 1 << (3 * (1 << Entry::LOG_DIM_BITS - 1)) children.
    /// public void flatten_grid(Grid grid);
    protected abstract void flatten_grid();

    /// Performs the cell expansion optimization (expands cells over neighbors that share the same set of primitives).
    /// public void expand_grid(Grid grid, Tri[] tris, int iters);
    protected void expand_grid(TriangleMesh tris)
    {
        
    }

    /// Tries to compress the grid by using sentinels in the reference array and using 16-bit cell dimensions. Returns true on success, otherwise false.
    //public boolean compress_grid(Grid grid);
    protected boolean compress_grid()
    {
        return false;
    }
}
