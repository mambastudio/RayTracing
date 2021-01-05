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
/// A 3D integer range.
public class Range {
    int lx; int hx;
    int ly; int hy;
    int lz; int hz;

    public Range() {}
    public Range(int lx, int hx,
          int ly, int hy,
          int lz, int hz)            
    {
        this.lx = lx; this.hx = hx;
        this.ly = ly; this.hy = hy;
        this.lz = lz; this.hz = hz;
    }

    public int size() 
    { 
        return (hx - lx + 1) * (hy - ly + 1) * (hz - lz + 1);
    }

    public void iterate(FunctionParameter f) {
        for (int z = lz; z <= hz; z++) {
            for (int y = ly; y <= hy; y++) {
                for (int x = lx; x <= hx; x++) {
                    f.function(x, y, z);
                }
            }
        }
    }
}
