/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid.main;

/**
 *
 * @author user
 */

// When this symbol is defined, the overlap optimization (called cell expansion in the paper)
// uses a simple and fast criterion to expand a cell: A cell C is expanded if all its neighbouring cells
// contains only a subset of the primitives in C. When this symbol is commented out, the optimization
// inspects each primitive that is not in the primitives contained in C, to see by how many virtual
// grid voxels the cell C can be expanded (that was not originally mentioned in the paper for reasons of space).
// Of course, this process is much more expensive (10x slower), but it yields a significant improvement in performance.
public class Overlap {
    int dmin, dmax;
    
    public Overlap()        
    {
        dmin = dmax = 0;
    }
}
