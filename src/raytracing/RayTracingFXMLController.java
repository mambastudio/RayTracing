/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing;

import bitmap.display.DynamicDisplay;
import coordinate.parser.obj.OBJParser;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import raytracing.core.FileUtility;
import raytracing.primitive.Box;
import raytracing.primitive.TriangleMesh;
import raytracing.renderer.ShadingRenderer;
import thread.model.LambdaThread;

/**
 * FXML Controller class
 *
 * @author user
 */
public class RayTracingFXMLController implements Initializable {

    /**
     * Initializes the controller class.
     */
    
    @FXML
    BorderPane pane;
        
    private final DynamicDisplay display = new DynamicDisplay();    
    private final RayTracingAPI api = new RayTracingAPI();
    private final FileUtility utility = FileUtility.getFileUtility();
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        pane.setCenter(display); 
        
        api.setPrimitive(new Box());
        api.setRenderer(new ShadingRenderer());
        api.render(display);
    }    
    
    
    public void close(ActionEvent e)
    {
        System.exit(0);
    }
    
    public void open(ActionEvent e)
    {
        File file = launchSceneFileChooser(null);
        
        if(file == null) return;
        
        LambdaThread.executeThread(()->{
            OBJParser parser = new OBJParser();
            TriangleMesh mesh = new TriangleMesh();
            parser.read(file.toURI(), mesh);               
            mesh.buildAccelerator();
            utility.setWaveFrontFolder(file.getParent());
            api.setPrimitive(mesh);                        
            api.reposition();
            api.triggerRender();
        });
    }
    
    public File launchSceneFileChooser(Window window)
    {        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(utility.getWavefrontFolder());
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Wavefront obj", "*.obj"));
        File file = fileChooser.showOpenDialog(window);   
        
        return file;
    }
    
}
