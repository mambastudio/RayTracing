/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raytracing;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

/**
 *
 * @author user
 */
public class RayTracing extends Application{

    /**
     * @param args the command line arguments
     */
    
    RayTracingFXMLController controller;
    
    public static void main(String[] args) {
        // TODO code application logic here
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("RayTracingFXML.fxml"));
        Parent root = loader.load();
        controller = (RayTracingFXMLController)loader.getController();
                        
        Scene scene = new Scene(root); 
        scene.addEventFilter(KeyEvent.KEY_PRESSED, ke->{
            if (ke.getCode()== KeyCode.DIGIT1) 
                controller.getAPI().getRenderer().setHeatmap(false);            
            else if(ke.getCode() == KeyCode.DIGIT2)
                controller.getAPI().getRenderer().setHeatmap(true);
        });
        
        primaryStage.setScene(scene);        
        primaryStage.setTitle("Simple Ray Tracer");
        primaryStage.show();
                
        primaryStage.setOnCloseRequest(evt -> {
            //do changes in code when
            System.exit(0);
        });        
    }
    
}
