/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid.main;

import raytracing.core.coordinate.BoundingBox;
import raytracing.primitive.TriangleMesh;

/**
 *
 * @author user
 */
public interface HagridInterface {
    /// Builds the irregular grid with the given parameters.
    boolean build_grid(
        boolean do_merge, boolean do_overlap, boolean do_opt,
        float alpha, float top_density, float snd_density,
        TriangleMesh tris,
        int[] dims, int[] snd_dim, BoundingBox grid_bb);
}
