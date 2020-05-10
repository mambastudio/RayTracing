/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.accel.grid;

/**
 *
 * @author user
 */
public class Range {
    int lx, ly, lz;
    int hx, hy, hz;
    public Range() {}
    public Range(int lx, int ly, int lz,
                      int hx, int hy, int hz)
    {
        this.lx = lx; this.ly = ly; this.lz = lz;
        this.hx = hx; this.hy = hy; this.hz = hz;
    }
    public int size() 
    { 
        return (hx - lx + 1) * (hy - ly + 1) * (hz - lz + 1) ; 
    }
}
