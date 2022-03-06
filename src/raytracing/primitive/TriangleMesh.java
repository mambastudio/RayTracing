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
import raytracing.accel.BVHAfra;
import raytracing.accel.BVHKarras;
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
public final class TriangleMesh extends AbstractMesh<Point3f, Vector3f, Point2f> implements Primitive
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
    
    public Vector3f getNorm(int primID)
    {
        Point3f p1 = getVertex1(primID);
        Point3f p2 = getVertex2(primID);
        Point3f p3 = getVertex3(primID);
        
        Vector3f e1 = Point3f.sub(p2, p1);
        Vector3f e2 = Point3f.sub(p3, p1);

        return Vector3f.cross(e1, e2).normalize();
        
    }
    
    public Vector3f e1(int primID)
    {
        Point3f p1 = getVertex1(primID);
        Point3f p2 = getVertex2(primID);
        
        return Point3f.sub(p2, p1);
        
    }
    
    public Vector3f e2(int primID)
    {
        Point3f p1 = getVertex1(primID);
        Point3f p3 = getVertex3(primID);
        
        return  Point3f.sub(p3, p1);
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
        Point3f p1 = getVertex1(primID);
        Point3f p2 = getVertex2(primID);
        Point3f p3 = getVertex3(primID);
        float[] tuv = new float[3];
        
        if(mollerIntersection(r, tuv, p1, p2, p3))
        {
            r.setMax(tuv[0]);

            isect.u = tuv[1];
            isect.v = tuv[2];
            isect.n = getNormal(p1, p2, p3, primID, new Value2Df(tuv[1], tuv[2]));
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
        Point3f p1 = getVertex1(primID);
        Point3f p2 = getVertex2(primID);
        Point3f p3 = getVertex3(primID);
                
        return mollerIntersection(r, null, p1, p2, p3);    
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
    
    
    public Vector3f getNormal(Point3f p1, Point3f p2, Point3f p3, int primID, Value2Df uv)
    {        
        if(hasNormal(primID) && hasUV(primID))
        {
            Vector3f n1 = getNormal1(primID);
            Vector3f n2 = getNormal2(primID);
            Vector3f n3 = getNormal3(primID);
            
            return n1.mul(1 - uv.x - uv.y).add(n2.mul(uv.x).add(n3.mul(uv.y)));            
        }
        else
        {
            Vector3f e1 = Point3f.sub(p2, p1);
            Vector3f e2 = Point3f.sub(p3, p1);

            return Vector3f.cross(e1, e2).normalize();
        }
    } 
    
    public boolean mollerIntersection(Ray r, float[] tuv, Point3f p1, Point3f p2, Point3f p3)
    {
        Vector3f e1, e2, h, s, q;
        double a, f, b1, b2;

        e1 = Point3f.sub(p2, p1);
        e2 = Point3f.sub(p3, p1);
        h = Vector3f.cross(r.d, e2);
        a = Vector3f.dot(e1, h);

        if (a > -0.0000001 && a < 0.0000001)
            return false;

        f = 1/a;
        
        s = Point3f.sub(r.o, p1);
	b1 = f * (Vector3f.dot(s, h));

        if (b1 < 0.0 || b1 > 1.0)
            return false;

        q = Vector3f.cross(s, e1);
	b2 = f * Vector3f.dot(r.d, q);

	if (b2 < 0.0 || b1 + b2 > 1.0)
            return false;

	float t = (float) (f * Vector3f.dot(e2, q));
        
        if(r.isInside(t)) 
        {
            if(tuv != null)
            {
                tuv[0] = t;
                tuv[1] = (float) b1;
                tuv[2] = (float) b2;
            }
            return true;
        }
        else
            return false;
    }
}
