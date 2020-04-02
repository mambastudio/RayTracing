/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.generic;

import coordinate.generic.raytrace.AbstractAccelerator;
import coordinate.generic.raytrace.AbstractPrimitive;
import raytracing.core.coordinate.BoundingBox;
import raytracing.core.Intersection;
import raytracing.core.coordinate.Point3f;
import raytracing.core.coordinate.Ray;

/**
 *
 * @author user
 */
public interface Primitive extends AbstractPrimitive<Point3f, Ray, Intersection, AbstractAccelerator<Ray, Intersection, Primitive, BoundingBox>, BoundingBox>{
    
}
