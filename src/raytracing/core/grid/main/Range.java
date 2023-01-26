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
public class Range {
    int lx; int hx;
    int ly; int hy;
    int lz; int hz;

    public Range() {}
    public Range(
            int lx, int ly, int lz,
            int hx, int hy, int hz)        
    {        
        this.lx = lx; this.hx = hx;
        this.ly = ly; this.hy = hy;
        this.lz = lz; this.hz = hz;
    }

    public int size()
    { 
        return (hx - lx + 1) * (hy - ly + 1) * (hz - lz + 1); 
    }

    @Override
    public String toString()
    {
        return "range: lx " +lx+ " ly " +ly+ " lz " +lz+ " hx " +hx+ " hy " +hy+ " hz " +hz;
    }
}
