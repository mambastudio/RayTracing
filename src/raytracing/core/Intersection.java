/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core;

import raytracing.core.coordinate.Point3f;
import raytracing.core.coordinate.Vector3f;
import raytracing.generic.Primitive;
import coordinate.generic.raytrace.AbstractIntersection;

/**
 *
 * @author user
 */
public class Intersection implements AbstractIntersection{
    public Point3f p = new Point3f();
    public Vector3f n = new Vector3f();
    public float u, v;
    
    public Primitive primitive = null;    
    public int id;    
}
