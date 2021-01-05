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
public class Common {
    public static int closest_log2(int k) {
        // One could use a CLZ instruction if the hardware supports it
        int i = 0;
        while ((1 << i) < k) i++;
        return i;
    }
}
