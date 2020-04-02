/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing.core.coordinate;

import coordinate.model.CameraModel;

/**
 *
 * @author user
 */
public class Camera extends CameraModel <Point3f, Vector3f, Ray>{
    
    public Camera(Point3f position, Point3f lookat, Vector3f up, float horizontalFOV) {
        super(position.copy(), lookat.copy(), up.copy(), horizontalFOV);
    }

    @Override
    public Camera copy() {
        Camera camera = new Camera(position, lookat, up, fov);
        camera.setUp();
        return camera;
    }
    
}
