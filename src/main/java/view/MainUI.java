import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import java.io.*;
import java.util.*;

/**
 * Main UI for Tomasulo Algorithm Simulator
 * Role 1: Create Main Stage with Tables and Input Forms
 */
public class MainUI extends Application {

    private SimulatorEngine simulator;
    private Stage window;
    
    // Configuration values
    private int addLatency = 2, mulLatency = 10, divLatency = 40, loadLatency = 2;
    private int cacheSize = 64, blockSize = 4;
    private int addStations = 3, mulStations = 2, loadBuffers = 3, storeBuffers = 3;
    
    // UI Components
    private TableView<Register> regFileTable;
    private TableView<CacheBlock> cacheTable;
    private TableView<Integer> memoryTable;
    private Label lblCycle;
    private TextArea instructionLog;
    private TextArea debugInfo;
    private ListView<String> instructionList;
    private List<Instruction> loadedInstructions = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) {
        window = primaryStage;
        window.setTitle("Tomasulo Algorithm Simulator");
        window.setWidth(1400);
        window.setHeight(900);
        
        Scene configScene = createConfigScene();
        window.setScene(configScene);
        window.show();
    }

    /**
     * Configuration Screen - Input forms for cache, latencies, station sizes
     */
    private Scene createConfigScene() {
        BorderPane root = new BorderPane();
        
        // Title
        Label title = new Label("Tomasulo Simulator - Configuration");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        VBox titleBox = new VBox(10, title);
        titleBox.setPadding(new Insets(20));
        root.setTop(titleBox);
        
        // Main configuration area
        ScrollPane scrollPane = new ScrollPane();
        VBox configBox = createConfigurationForm();
        scrollPane.setContent(configBox);
        scrollPane.setFitToWidth(true);
        root.setCenter(scrollPane);
        
        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(20));
        buttonBox.setStyle("-fx-alignment: center;");
        
        Button btnStart = new Button("Start Simulation");
        btnStart.setStyle("-fx-font-size: 14; -fx-padding: 10px 30px;");
        btnStart.setOnAction(e -> {
            if (loadedInstructions.isEmpty()) {
                showError("Please load or create instructions first!");
                return;
            }
            startSimulation();
        });
        
        buttonBox.getChildren().add(btnStart);
        root.setBottom(buttonBox);
        
        return new Scene(root, 1000, 800);
    }

    /**
     * Create configuration form with all input fields
     */
    private VBox createConfigurationForm() {
        VBox form = new VBox(15);
        form.setPadding(new Insets(20));
        
        // Cache Configuration
        TitledPane cachePane = createCacheConfigPane();
        
        // Latency Configuration
        TitledPane latencyPane = createLatencyConfigPane();
        
        // Station Size Configuration
        TitledPane stationPane = createStationConfigPane();
        
        // Instruction Input
        TitledPane instructionPane = createInstructionPane();
        
        form.getChildren().addAll(cachePane, latencyPane, stationPane, instructionPane);
        return form;
    }

    private TitledPane createCacheConfigPane() {
        VBox cache = new VBox(10);
        cache.setPadding(new Insets(10));
        
        HBox cacheSizeBox = createLabeledInputBox("Cache Size (bytes):", "64", v -> cacheSize = Integer.parseInt(v));
        HBox blockSizeBox = createLabeledInputBox("Block Size (bytes):", "4", v -> blockSize = Integer.parseInt(v));
        
        cache.getChildren().addAll(cacheSizeBox, blockSizeBox);
        return new TitledPane("Cache Configuration", cache);
    }

    private TitledPane createLatencyConfigPane() {
        VBox latency = new VBox(10);
        latency.setPadding(new Insets(10));
        
        HBox addBox = createLabeledInputBox("ADD/SUB Latency (cycles):", "2", v -> addLatency = Integer.parseInt(v));
        HBox mulBox = createLabeledInputBox("MUL Latency (cycles):", "10", v -> mulLatency = Integer.parseInt(v));
        HBox divBox = createLabeledInputBox("DIV Latency (cycles):", "40", v -> divLatency = Integer.parseInt(v));
        HBox loadBox = createLabeledInputBox("LOAD/STORE Latency (cycles):", "2", v -> loadLatency = Integer.parseInt(v));
        
        latency.getChildren().addAll(addBox, mulBox, divBox, loadBox);
        return new TitledPane("Instruction Latencies", latency);
    }

    private TitledPane createStationConfigPane() {
        VBox stations = new VBox(10);
        stations.setPadding(new Insets(10));
        
        HBox addStBox = createLabeledInputBox("ADD Station Count:", "3", v -> addStations = Integer.parseInt(v));
        HBox mulStBox = createLabeledInputBox("MUL Station Count:", "2", v -> mulStations = Integer.parseInt(v));
        HBox loadBufBox = createLabeledInputBox("LOAD Buffer Count:", "3", v -> loadBuffers = Integer.parseInt(v));
        HBox storeBufBox = createLabeledInputBox("STORE Buffer Count:", "3", v -> storeBuffers = Integer.parseInt(v));
        
        stations.getChildren().addAll(addStBox, mulStBox, loadBufBox, storeBufBox);
        return new TitledPane("Station/Buffer Sizes", stations);
    }

    private TitledPane createInstructionPane() {
        VBox instrBox = new VBox(10);
        instrBox.setPadding(new Insets(10));
        
        HBox buttonBox = new HBox(10);
        Button btnLoadFile = new Button("Load from File");
        btnLoadFile.setOnAction(e -> loadInstructionsFromFile());
        
        Button btnLoadTestCase = new Button("Load Test Case");
        btnLoadTestCase.setOnAction(e -> loadTestCase());
        
        Button btnClear = new Button("Clear");
        btnClear.setOnAction(e -> {
            loadedInstructions.clear();
            instructionList.getItems().clear();
        });
        
        buttonBox.getChildren().addAll(btnLoadFile, btnLoadTestCase, btnClear);
        
        Label instrLabel = new Label("Loaded Instructions:");
        instructionList = new ListView<>();
        instructionList.setPrefHeight(200);
        
        instrBox.getChildren().addAll(buttonBox, instrLabel, instructionList);
        return new TitledPane("Instructions", instrBox);
    }

    private HBox createLabeledInputBox(String label, String defaultValue, java.util.function.Consumer<String> callback) {
        HBox box = new HBox(10);
        box.setStyle("-fx-alignment: center-left;");
        
        Label lbl = new Label(label);
        lbl.setPrefWidth(200);
        
        TextField input = new TextField(defaultValue);
        input.setPrefWidth(150);
        input.setOnAction(e -> callback.accept(input.getText()));
        
        box.getChildren().addAll(lbl, input);
        return box;
    }

    private void loadTestCase() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Test Case 1", "Test Case 1", "Test Case 2", "Test Case 3");
        dialog.setTitle("Select Test Case");
        dialog.setHeaderText("Choose a test case to load");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String[] testCases = getTestCases();
            String code = testCases[Integer.parseInt(result.get().split(" ")[2]) - 1];
            parseAndLoadInstructions(code);
        }
    }

    private String[] getTestCases() {
        return new String[]{
            "L.D F6, 0(R2)\nL.D F2, 8(R2)\nMUL.D F0, F2, F4\nSUB.D F8, F2, F6\nDIV.D F10, F0, F6\nADD.D F6, F8, F2\nS.D F6, 8(R2)",
            "L.D F6, 0(R2)\nADD.D F7, F1, F3\nL.D F2, 20(R2)\nMUL.D F0, F2, F4\nSUB.D F8, F2, F6\nDIV.D F10, F0, F6\nS.D F10, 0(R2)",
            "DADDI R1, R1, 24\nDADDI R2, R2, 0\nL.D F0, 8(R1)\nMUL.D F4, F0, F2\nS.D F4, 8(R1)\nDSUBI R1, R1, 8\nBNE R1, R2, 2"
        };
    }

    private void loadInstructionsFromFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load Instructions File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        
        File file = chooser.showOpenDialog(window);
        if (file != null) {
            try {
                StringBuilder code = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        code.append(line).append("\n");
                    }
                }
                parseAndLoadInstructions(code.toString());
            } catch (IOException ex) {
                showError("Error loading file: " + ex.getMessage());
            }
        }
    }

    private void parseAndLoadInstructions(String code) {
        loadedInstructions.clear();
        instructionList.getItems().clear();
        
        String[] lines = code.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("//")) {
                Instruction instr = parseInstruction(line);
                if (instr != null) {
                    loadedInstructions.add(instr);
                    instructionList.getItems().add(line);
                }
            }
        }
        
        if (!loadedInstructions.isEmpty()) {
            showInfo("Loaded " + loadedInstructions.size() + " instructions");
        }
    }

    private Instruction parseInstruction(String line) {
        // Simple parser - can be enhanced
        String[] parts = line.split("[,\\s()]+");
        if (parts.length < 2) return null;
        
        String op = parts[0].toUpperCase();
        String rd = parts.length > 1 ? parts[1] : "";
        String rs = parts.length > 2 ? parts[2] : "";
        String rt = parts.length > 3 ? parts[3] : "";
        
        return new Instruction(op, rd, rs, rt);
    }

    /**
     * Main Simulation Scene - Role 1: Create Main Stage with Tables
     */
    private void startSimulation() {
        simulator = new SimulatorEngine(addLatency, mulLatency, divLatency, loadLatency, 
                                       cacheSize, blockSize);
        
        BorderPane root = new BorderPane();
        
        // Top: Cycle counter and controls
        root.setTop(createControlPanel());
        
        // Center: Tables in tabs
        root.setCenter(createTablesPanel());
        
        // Bottom: Instruction log
        root.setBottom(createLogPanel());
        
        Scene scene = new Scene(root, 1400, 900);
        window.setScene(scene);
    }

    private VBox createControlPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;");
        
        HBox cycleBox = new HBox(20);
        lblCycle = new Label("Cycle: 0");
        lblCycle.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
        
        Button btnNext = new Button("Next Cycle");
        btnNext.setPrefWidth(100);
        btnNext.setStyle("-fx-font-size: 14;");
        btnNext.setOnAction(e -> nextCycle());
        
        Button btnReset = new Button("Reset");
        btnReset.setPrefWidth(100);
        btnReset.setOnAction(e -> resetSimulation());
        
        Button btnBack = new Button("Back to Config");
        btnBack.setPrefWidth(130);
        btnBack.setOnAction(e -> window.setScene(createConfigScene()));
        
        cycleBox.getChildren().addAll(lblCycle, btnNext, btnReset, btnBack);
        panel.getChildren().add(cycleBox);
        
        return panel;
    }

    private TabPane createTablesPanel() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // ADD Stations Tab
        Tab addTab = new Tab("ADD Stations", createStationTable("ADD"));
        
        // MUL Stations Tab
        Tab mulTab = new Tab("MUL Stations", createStationTable("MUL"));
        
        // LOAD Buffers Tab
        Tab loadTab = new Tab("LOAD Buffers", createStationTable("LOAD"));
        
        // STORE Buffers Tab
        Tab storeTab = new Tab("STORE Buffers", createStationTable("STORE"));
        
        // Register File Tab
        Tab regTab = new Tab("Register File", createRegFileTable());
        
        // Cache Tab
        Tab cacheTab = new Tab("Cache", createCacheTable());
        
        // Memory Tab
        Tab memTab = new Tab("Memory", createMemoryTable());
        
        tabPane.getTabs().addAll(addTab, mulTab, loadTab, storeTab, regTab, cacheTab, memTab);
        return tabPane;
    }

    private VBox createStationTable(String type) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        
        TableView<ReservationStation> table = new TableView<>();
        
        TableColumn<ReservationStation, String> colName = new TableColumn<>("Station");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(100);
        
        TableColumn<ReservationStation, Boolean> colBusy = new TableColumn<>("Busy");
        colBusy.setCellValueFactory(new PropertyValueFactory<>("busy"));
        colBusy.setPrefWidth(60);
        
        TableColumn<ReservationStation, String> colOp = new TableColumn<>("Op");
        colOp.setCellValueFactory(new PropertyValueFactory<>("op"));
        colOp.setPrefWidth(60);
        
        TableColumn<ReservationStation, Double> colVj = new TableColumn<>("Vj");
        colVj.setCellValueFactory(new PropertyValueFactory<>("Vj"));
        colVj.setPrefWidth(80);
        
        TableColumn<ReservationStation, Double> colVk = new TableColumn<>("Vk");
        colVk.setCellValueFactory(new PropertyValueFactory<>("Vk"));
        colVk.setPrefWidth(80);
        
        TableColumn<ReservationStation, String> colQj = new TableColumn<>("Qj");
        colQj.setCellValueFactory(new PropertyValueFactory<>("Qj"));
        colQj.setPrefWidth(80);
        
        TableColumn<ReservationStation, String> colQk = new TableColumn<>("Qk");
        colQk.setCellValueFactory(new PropertyValueFactory<>("Qk"));
        colQk.setPrefWidth(80);
        
        TableColumn<ReservationStation, Integer> colTime = new TableColumn<>("Time Left");
        colTime.setCellValueFactory(new PropertyValueFactory<>("timeLeft"));
        colTime.setPrefWidth(80);
        
        table.getColumns().addAll(colName, colBusy, colOp, colVj, colVk, colQj, colQk, colTime);
        table.setPrefHeight(250);
        
        // Get stations by type from simulator
        List<ReservationStation> stationsToDisplay = getStationsByType(type);
        table.setItems(FXCollections.observableArrayList(stationsToDisplay));
        
        box.getChildren().addAll(new Label(type + " Stations:"), table);
        return box;
    }

    private List<ReservationStation> getStationsByType(String type) {
        if (simulator == null) return new ArrayList<>();
        
        List<ReservationStation> allStations = simulator.getReservationStations();
        List<ReservationStation> filtered = new ArrayList<>();
        
        for (ReservationStation rs : allStations) {
            if (rs.getName().startsWith(type)) {
                filtered.add(rs);
            }
        }
        return filtered;
    }

    private VBox createRegFileTable() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        
        regFileTable = new TableView<>();
        
        TableColumn<Register, String> colName = new TableColumn<>("Register");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(100);
        
        TableColumn<Register, Double> colValue = new TableColumn<>("Value");
        colValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        colValue.setPrefWidth(150);
        
        TableColumn<Register, String> colQi = new TableColumn<>("Qi (Dependency)");
        colQi.setCellValueFactory(new PropertyValueFactory<>("Qi"));
        colQi.setPrefWidth(150);
        
        regFileTable.getColumns().addAll(colName, colValue, colQi);
        regFileTable.setPrefHeight(400);
        
        box.getChildren().addAll(new Label("Register File:"), regFileTable);
        return box;
    }

    private VBox createCacheTable() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        
        cacheTable = new TableView<>();
        
        TableColumn<CacheBlock, Integer> colIndex = new TableColumn<>("Block Index");
        colIndex.setCellValueFactory(new PropertyValueFactory<>("index"));
        colIndex.setPrefWidth(100);
        
        TableColumn<CacheBlock, String> colValid = new TableColumn<>("Valid");
        colValid.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().isValid() ? "Yes" : "No"));
        colValid.setPrefWidth(60);
        
        TableColumn<CacheBlock, String> colTag = new TableColumn<>("Tag");
        colTag.setCellValueFactory(new PropertyValueFactory<>("tagStr"));
        colTag.setPrefWidth(100);
        
        TableColumn<CacheBlock, String> colData = new TableColumn<>("Data (bytes)");
        colData.setCellValueFactory(new PropertyValueFactory<>("dataStr"));
        colData.setPrefWidth(300);
        
        cacheTable.getColumns().addAll(colIndex, colValid, colTag, colData);
        cacheTable.setPrefHeight(300);
        
        box.getChildren().addAll(new Label("Cache Blocks:"), cacheTable);
        return box;
    }

    private VBox createMemoryTable() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        
        memoryTable = new TableView<>();
        
        TableColumn<Integer, Integer> colAddr = new TableColumn<>("Address");
        colAddr.setPrefWidth(100);
        
        TableColumn<Integer, String> colData = new TableColumn<>("Data");
        colData.setPrefWidth(200);
        
        memoryTable.getColumns().addAll(colAddr, colData);
        memoryTable.setPrefHeight(300);
        
        Label info = new Label("Main Memory (showing first 256 bytes):");
        box.getChildren().addAll(info, memoryTable);
        return box;
    }

    private VBox createLogPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setPrefHeight(150);
        
        Label label = new Label("Instruction/Cycle Log:");
        
        instructionLog = new TextArea();
        instructionLog.setEditable(false);
        instructionLog.setWrapText(true);
        instructionLog.setPrefRowCount(6);
        
        panel.getChildren().addAll(label, instructionLog);
        return panel;
    }

    /**
     * Role 1: Implement "Next Cycle" button and main Cycle() loop
     */
    private void nextCycle() {
        if (simulator != null) {
            simulator.nextCycle();
            updateTables();
            lblCycle.setText("Cycle: " + simulator.getCurrentCycle());
            logCycleInfo();
        }
    }

    private void resetSimulation() {
        if (simulator != null) {
            simulator = new SimulatorEngine(addLatency, mulLatency, divLatency, loadLatency,
                                           cacheSize, blockSize);
            updateTables();
            lblCycle.setText("Cycle: 0");
            instructionLog.clear();
        }
    }

    private void updateTables() {
        // Update Register File Table
        RegisterFile floatRegs = simulator.getFloatRegFile();
        if (floatRegs != null) {
            regFileTable.setItems(FXCollections.observableArrayList(floatRegs.getAllRegisters()));
        }
        
        // Update Cache Table
        MemoryUnit memory = simulator.getMemory();
        if (memory != null) {
            cacheTable.setItems(FXCollections.observableArrayList(memory.getCacheData()));
        }
        
        regFileTable.refresh();
        cacheTable.refresh();
    }

    private void logCycleInfo() {
        String log = "Cycle " + simulator.getCurrentCycle() + ": Executed\n";
        instructionLog.appendText(log);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
