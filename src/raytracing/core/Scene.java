/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core;

import coordinate.model.OrientationModel;
import raytracing.core.coordinate.Ray;
import raytracing.core.coordinate.Camera;
import raytracing.core.coordinate.BoundingBox;
import raytracing.core.coordinate.Point3f;
import raytracing.core.coordinate.Vector3f;
import raytracing.generic.Primitive;

/**
 *
 * @author user
 */
public class Scene {
    public Camera camera; 
    public Primitive primitive;
    public OrientationModel<Point3f, Vector3f, Ray, BoundingBox> orientation = new OrientationModel(Point3f.class, Vector3f.class);
    

    public Scene() {
        this.camera = new Camera(new Point3f(0, 0, 4), new Point3f(), new Vector3f(0, 1, 0), 45);
    } 

    public Camera getCamera() {
        return camera;
    }
    
    public boolean intersect(Ray ray, Intersection isect)
    {
        return primitive.intersect(ray, isect);
    }
    
    public boolean intersectP(Ray ray)
    {
        return primitive.intersectP(ray);
    }
    
    public void setPrimitive(Primitive primitive)
    {
        this.primitive = primitive;
    }
    
    public void setCamera(Camera camera)
    {
        this.camera = camera;
    }
    
    public Primitive getPrimitive()
    {
        return primitive;
    }
    
    public BoundingBox getWorldBound()
    {        
        return primitive.getBound();
    }
    
    public void reposition()
    {
        orientation.reposition(camera, getWorldBound());
    }
}
