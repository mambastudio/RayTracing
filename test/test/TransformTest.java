/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import coordinate.model.Transform;
import raytracing.core.coordinate.Point3f;
import raytracing.core.coordinate.Vector3f;

/**
 *
 * @author user
 */
public class TransformTest {
    public static void main(String... args)
    {
        Transform<Point3f, Vector3f> transform = Transform.translate(new Vector3f());
        Vector3f up = new Vector3f(0, 10, 0);
        transform.transformAssign(up);
        System.out.println(up);        
    }
}
