/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.primitive;

import coordinate.generic.AbstractMesh;
import coordinate.generic.raytrace.AbstractAccelerator;
import coordinate.list.CoordinateFloatList;
import coordinate.list.IntList;
import coordinate.utility.Value2Df;
import raytracing.accel.BVHPlocTree;
import raytracing.core.coordinate.BoundingBox;
import raytracing.core.Intersection;
import raytracing.core.coordinate.Point2f;
import raytracing.core.coordinate.Point3f;
import raytracing.generic.Primitive;
import raytracing.core.coordinate.Ray;
import raytracing.core.coordinate.Vector3f;

/**
 *
 * @author user
 */
public final class TriangleMesh extends AbstractMesh<Point3f, Vector3f, Point2f, Ray, Triangle> implements Primitive
{
    private AbstractAccelerator accelerator;
    private final BoundingBox bounds;
    
    public TriangleMesh()
    {
        points = new CoordinateFloatList(Point3f.class);
        normals = new CoordinateFloatList(Vector3f.class);
        texcoords = new CoordinateFloatList(Point2f.class);
        triangleFaces = new IntList();
        bounds = new BoundingBox();
    }
    
    public void initCoordList(int sizeP, int sizeN, int sizeT, int sizeF)
    {
        this.initCoordList(Point3f.class, Vector3f.class, Point2f.class, sizeP, sizeN, sizeT, sizeF);
    }
    
    
    @Override
    public void addPoint(Point3f p) {
        points.add(p);
        bounds.include(p);
    }

    @Override
    public void addPoint(float... values) {
        Point3f p = new Point3f(values[0], values[1], values[2]);
        addPoint(p);
        bounds.include(p);
    }

    @Override
    public void addNormal(Vector3f n) {
        normals.add(n);
    }

    @Override
    public void addNormal(float... values) {
        Vector3f n = new Vector3f(values[0], values[1], values[2]);
        normals.add(n);
    }

    @Override
    public void addTexCoord(Point2f t) {
        texcoords.add(t);
    }

    @Override
    public void addTexCoord(float... values) {
        Point2f t = new Point2f(values[0], values[1]);
        texcoords.add(t);   
    }

    @Override
    public int getSize() {
        return triangleSize();
    }

    @Override
    public BoundingBox getBound(int primID) {
        BoundingBox bbox = new BoundingBox();
        bbox.include(getVertex1(primID));
        bbox.include(getVertex2(primID));
        bbox.include(getVertex3(primID));
        return bbox; 
    }

    @Override
    public Point3f getCentroid(int primID) {
        return getBound(primID).getCenter();
    }

    @Override
    public BoundingBox getBound() {       
        return bounds;
    }

    @Override
    public boolean intersect(Ray r, int primID, Intersection isect) {
        Triangle triangle = getTriangle(primID);
        float[] tuv = new float[3];
        
        if(triangle.intersect(r, tuv))
        {
            r.setMax(tuv[0]);

            isect.u = tuv[1];
            isect.v = tuv[2];
            isect.n = triangle.getNormal(new Value2Df(tuv[1], tuv[2]));
            isect.p = r.getPoint();
            isect.id = primID;
            isect.primitive = this;

            return true;
        } 
        else
            return false;
    }

    @Override
    public boolean intersectP(Ray r, int primID) {       
        Triangle triangle = getTriangle(primID);
        return triangle.intersect(r);                
    }

    @Override
    public boolean intersect(Ray r, Intersection isect) {
        return accelerator.intersect(r, isect);
    }

    @Override
    public boolean intersectP(Ray r) {
        return accelerator.intersectP(r);
    }

    @Override
    public AbstractAccelerator getAccelerator() {
        return accelerator;
    }

    @Override
    public void buildAccelerator() {
        accelerator = new BVHPlocTree();
        accelerator.build(this);
    }

    @Override
    public float getArea(int primID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public Triangle getTriangle(int index) {
        if(this.hasNormal(index))
            return new Triangle(
                    this.getVertex1(index), 
                    this.getVertex2(index), 
                    this.getVertex3(index),
                    this.getNormal1(index),
                    this.getNormal2(index),
                    this.getNormal3(index));
        else
            return new Triangle(
                    this.getVertex1(index), 
                    this.getVertex2(index), 
                    this.getVertex3(index));
    }
}
