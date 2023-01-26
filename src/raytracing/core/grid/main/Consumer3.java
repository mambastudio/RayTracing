/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.grid.main;

/**
 *
 * @author user
 * @param <T>
 * @param <U>
 * @param <V>
 */
@FunctionalInterface
public interface Consumer3<T, U, V> {
    void accept(T t, U u, V v);
}
