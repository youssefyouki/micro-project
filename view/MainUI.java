package view;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainUI extends Application {

    @Override
    public void start(Stage primaryStage) {
        [cite_start]// TODO (Member 1): Setup Tables for RS, Buffers, and Registers [cite: 27]
        TableView rsTable = new TableView();
        TableView regTable = new TableView();
        
        Button stepBtn = new Button("Next Cycle");
        stepBtn.setOnAction(e -> {
            // simulator.nextCycle();
            // refreshTables();
        });

        VBox root = new VBox(stepBtn, rsTable, regTable);
        Scene scene = new Scene(root, 800, 600);
        
        primaryStage.setTitle("Tomasulo Simulator - Team XX");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}