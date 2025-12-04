package view;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import components.*;
import simulation.*;
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
    private int addLatency = 2, mulLatency = 4, divLatency = 7, loadLatency = 2, storeLatency = 2;
    private int intAddLatency = 1, intSubLatency = 1;  // Integer instruction latencies
    private int cacheSize = 64, blockSize = 4;
    private int cacheHitLatency = 1, cacheMissPenalty = 10;
    private int addStations = 3, mulStations = 2, loadBuffers = 3, storeBuffers = 3;
    private int intStations = 2;  // Integer ALU reservation stations
    
    // Register initial values (32 float + 32 integer as per MIPS architecture)
    private Map<String, Double> floatRegInitValues = new HashMap<>();
    private Map<String, Integer> intRegInitValues = new HashMap<>();
    private Map<Integer, Double> memoryInitValues = new HashMap<>(); // address -> value
    
    // UI Components
    private TableView<Register> regFileTable;
    private TableView<CacheBlock> cacheTable;
    private TableView<MemoryRow> memoryTable;
    private TableView<Instruction> issueTable;
    private TableView<ReservationStation> addStationTable;
    private TableView<ReservationStation> mulStationTable;
    private TableView<LoadBuffer> loadBufferTable;
    private TableView<StoreBuffer> storeBufferTable;
    private Label lblCycle;
    private Label lblBranchStalled;
    private Label lblCacheStats;
    private TextArea instructionLog;
    private TextArea debugInfo;
    private ListView<String> instructionList;
    private List<Instruction> loadedInstructions = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) {
        window = primaryStage;
        window.setTitle("Tomasulo Algorithm Simulator");
        window.setWidth(1000);
        window.setHeight(700);
        
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
        
        return new Scene(root, 900, 650);
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
        
        // Register Initialization
        TitledPane regPane = createRegisterInitPane();
        
        // Memory Initialization
        TitledPane memPane = createMemoryInitPane();
        
        // Instruction Input
        TitledPane instructionPane = createInstructionPane();
        
        form.getChildren().addAll(cachePane, latencyPane, stationPane, regPane, memPane, instructionPane);
        return form;
    }

    private TitledPane createCacheConfigPane() {
        VBox cache = new VBox(10);
        cache.setPadding(new Insets(10));
        
        HBox cacheSizeBox = createLabeledInputBox("Cache Size (bytes):", "64", v -> cacheSize = Integer.parseInt(v));
        HBox blockSizeBox = createLabeledInputBox("Block Size (bytes):", "4", v -> blockSize = Integer.parseInt(v));
        HBox hitLatencyBox = createLabeledInputBox("Cache Hit Latency (cycles):", "1", v -> cacheHitLatency = Integer.parseInt(v));
        HBox missPenaltyBox = createLabeledInputBox("Cache Miss Penalty (cycles):", "10", v -> cacheMissPenalty = Integer.parseInt(v));
        
        cache.getChildren().addAll(cacheSizeBox, blockSizeBox, hitLatencyBox, missPenaltyBox);
        return new TitledPane("Cache Configuration", cache);
    }

    private TitledPane createLatencyConfigPane() {
        VBox latency = new VBox(10);
        latency.setPadding(new Insets(10));
        
        Label floatLabel = new Label("Floating Point Instructions:");
        floatLabel.setStyle("-fx-font-weight: bold;");
        HBox addBox = createLabeledInputBox("  ADD.D/SUB.D Latency (cycles):", "2", v -> addLatency = Integer.parseInt(v));
        HBox mulBox = createLabeledInputBox("  MUL.D Latency (cycles):", "10", v -> mulLatency = Integer.parseInt(v));
        HBox divBox = createLabeledInputBox("  DIV.D Latency (cycles):", "40", v -> divLatency = Integer.parseInt(v));
        HBox loadBox = createLabeledInputBox("  L.D (Load) Latency (cycles):", "2", v -> loadLatency = Integer.parseInt(v));
        HBox storeBox = createLabeledInputBox("  S.D (Store) Latency (cycles):", "2", v -> storeLatency = Integer.parseInt(v));
        
        Label intLabel = new Label("Integer Instructions:");
        intLabel.setStyle("-fx-font-weight: bold;");
        HBox intAddBox = createLabeledInputBox("  DADDI Latency (cycles):", "1", v -> intAddLatency = Integer.parseInt(v));
        HBox intSubBox = createLabeledInputBox("  DSUBI Latency (cycles):", "1", v -> intSubLatency = Integer.parseInt(v));
        
        latency.getChildren().addAll(floatLabel, addBox, mulBox, divBox, loadBox, storeBox, intLabel, intAddBox, intSubBox);
        return new TitledPane("Instruction Latencies", latency);
    }

    private TitledPane createStationConfigPane() {
        VBox stations = new VBox(10);
        stations.setPadding(new Insets(10));
        
        HBox addStBox = createLabeledInputBox("FP ADD/SUB Station Count:", "3", v -> addStations = Integer.parseInt(v));
        HBox mulStBox = createLabeledInputBox("FP MUL/DIV Station Count:", "2", v -> mulStations = Integer.parseInt(v));
        HBox intStBox = createLabeledInputBox("INT ALU Station Count:", "2", v -> intStations = Integer.parseInt(v));
        HBox loadBufBox = createLabeledInputBox("LOAD Buffer Count:", "3", v -> loadBuffers = Integer.parseInt(v));
        HBox storeBufBox = createLabeledInputBox("STORE Buffer Count:", "3", v -> storeBuffers = Integer.parseInt(v));
        
        stations.getChildren().addAll(addStBox, mulStBox, intStBox, loadBufBox, storeBufBox);
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
    
    private TitledPane createRegisterInitPane() {
        VBox regBox = new VBox(10);
        regBox.setPadding(new Insets(10));
        
        Label info = new Label("Initialize Register Values (MIPS: 32 Float + 32 Integer):");
        info.setStyle("-fx-font-weight: bold;");
        
        // Create scrollable grid for all 32 registers
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);
        grid.setPadding(new Insets(5));
        
        // Float registers F0-F31 (8 columns, 4 rows)
        Label floatHeader = new Label("Floating Point Registers (F0-F31):");
        floatHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 11;");
        grid.add(floatHeader, 0, 0, 8, 1);
        
        for (int i = 0; i < 32; i++) {
            final int index = i;
            Label lbl = new Label("F" + i + ":");
            TextField input = new TextField("0.0");
            input.setPrefWidth(60);
            input.textProperty().addListener((obs, oldVal, newVal) -> {
                try {
                    if (!newVal.isEmpty()) {
                        floatRegInitValues.put("F" + index, Double.parseDouble(newVal));
                    }
                } catch (NumberFormatException e) {
                    // Invalid input, ignore
                }
            });
            int row = 1 + (i / 8);
            int col = (i % 8) * 2;
            grid.add(lbl, col, row);
            grid.add(input, col + 1, row);
        }
        
        // Integer registers R0-R31 (8 columns, 4 rows)
        Label intHeader = new Label("Integer Registers (R0-R31):");
        intHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 11;");
        grid.add(intHeader, 0, 5, 8, 1);
        
        for (int i = 0; i < 32; i++) {
            final int index = i;
            Label lbl = new Label("R" + i + ":");
            TextField input = new TextField("0");
            input.setPrefWidth(60);
            input.textProperty().addListener((obs, oldVal, newVal) -> {
                try {
                    if (!newVal.isEmpty()) {
                        intRegInitValues.put("R" + index, Integer.parseInt(newVal));
                    }
                } catch (NumberFormatException e) {
                    // Invalid input, ignore
                }
            });
            int row = 6 + (i / 8);
            int col = (i % 8) * 2;
            grid.add(lbl, col, row);
            grid.add(input, col + 1, row);
        }
        
        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);
        
        regBox.getChildren().addAll(info, scrollPane);
        return new TitledPane("Register Initialization", regBox);
    }
    
    private TitledPane createMemoryInitPane() {
        VBox memBox = new VBox(10);
        memBox.setPadding(new Insets(10));
        
        Label info = new Label("Initialize Memory Values (Address -> Double Value):");
        info.setStyle("-fx-font-weight: bold;");
        
        TextArea memTextArea = new TextArea();
        memTextArea.setPromptText("Enter memory values, one per line:\nAddress: Value\nExample:\n0: 5.5\n8: 10.0\n16: -3.2");
        memTextArea.setPrefHeight(150);
        
        Button btnLoadMem = new Button("Load Memory Values");
        btnLoadMem.setOnAction(e -> {
            memoryInitValues.clear();
            String[] lines = memTextArea.getText().split("\\n");
            int count = 0;
            StringBuilder summary = new StringBuilder();
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                
                try {
                    String[] parts = line.split(":");
                    if (parts.length == 2) {
                        int address = Integer.parseInt(parts[0].trim());
                        double value = Double.parseDouble(parts[1].trim());
                        memoryInitValues.put(address, value);
                        if (count < 5) { // Show first 5
                            summary.append(String.format("  [0x%04X] = %.2f\n", address, value));
                        }
                        count++;
                    }
                } catch (NumberFormatException ex) {
                    // Skip invalid lines
                }
            }
            String msg = "Loaded " + count + " memory value(s):\n" + summary.toString();
            if (count > 5) msg += "  ... and " + (count - 5) + " more";
            showInfo(msg);
        });
        
        memBox.getChildren().addAll(info, memTextArea, btnLoadMem);
        return new TitledPane("Memory Initialization", memBox);
    }

    private HBox createLabeledInputBox(String label, String defaultValue, java.util.function.Consumer<String> callback) {
        HBox box = new HBox(10);
        box.setStyle("-fx-alignment: center-left;");
        
        Label lbl = new Label(label);
        lbl.setPrefWidth(200);
        
        TextField input = new TextField(defaultValue);
        input.setPrefWidth(150);
        input.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty()) {
                try {
                    callback.accept(newVal);
                } catch (NumberFormatException e) {
                    // Ignore invalid input
                }
            }
        });
        
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
        
        try {
            // Use the Instruction class's parseProgram method
            List<Instruction> parsed = Instruction.parseProgram(code);
            loadedInstructions.addAll(parsed);
            
            // Add to display list
            String[] lines = code.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("//") && !line.startsWith("#")) {
                    instructionList.getItems().add(line);
                }
            }
            
            if (!loadedInstructions.isEmpty()) {
                showInfo("Loaded " + loadedInstructions.size() + " instructions");
            }
        } catch (Exception ex) {
            showError("Error parsing instructions: " + ex.getMessage());
        }
    }

    private Instruction parseInstruction(String line) {
        // Use Instruction.parseProgram for single line
        try {
            List<Instruction> result = Instruction.parseProgram(line);
            return result.isEmpty() ? null : result.get(0);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Main Simulation Scene - Role 1: Create Main Stage with Tables
     */
    private void startSimulation() {
        // Create simulator with user-configured values
        simulator = new SimulatorEngine(
            addStations, mulStations, loadBuffers, storeBuffers,
            4096,  // memorySize (4KB)
            cacheSize, blockSize,
            cacheHitLatency,     // Use user-configured hit latency
            cacheMissPenalty     // Use user-configured miss penalty
        );
        
        // Apply user-configured instruction latencies
        simulator.setInstructionLatency("ADD_D", addLatency);
        simulator.setInstructionLatency("ADD_S", addLatency);
        simulator.setInstructionLatency("SUB_D", addLatency);
        simulator.setInstructionLatency("SUB_S", addLatency);
        simulator.setInstructionLatency("MUL_D", mulLatency);
        simulator.setInstructionLatency("MUL_S", mulLatency);
        simulator.setInstructionLatency("DIV_D", divLatency);
        simulator.setInstructionLatency("DIV_S", divLatency);
        simulator.setInstructionLatency("LOAD", loadLatency);
        simulator.setInstructionLatency("STORE", storeLatency);
        simulator.setInstructionLatency("DADDI", intAddLatency);
        simulator.setInstructionLatency("DSUBI", intSubLatency);
        
        // Apply user-defined register initial values (all 32 float + 32 integer as per MIPS)
        Register[] floatRegs = simulator.getFloatRegs();
        Register[] intRegs = simulator.getIntRegs();
        
        for (Map.Entry<String, Double> entry : floatRegInitValues.entrySet()) {
            int regNum = Integer.parseInt(entry.getKey().substring(1));
            if (regNum >= 0 && regNum < floatRegs.length) {
                floatRegs[regNum].value = entry.getValue();
            }
        }
        
        for (Map.Entry<String, Integer> entry : intRegInitValues.entrySet()) {
            int regNum = Integer.parseInt(entry.getKey().substring(1));
            if (regNum >= 0 && regNum < intRegs.length) {
                intRegs[regNum].value = entry.getValue();
            }
        }
        
        // Apply user-defined memory initial values
        for (Map.Entry<Integer, Double> entry : memoryInitValues.entrySet()) {
            simulator.initializeMemory(entry.getKey(), entry.getValue());
        }
        
        // Load the instructions into the simulator
        if (!loadedInstructions.isEmpty()) {
            // Convert loaded instructions back to assembly text for loading
            StringBuilder asmText = new StringBuilder();
            for (Instruction instr : loadedInstructions) {
                asmText.append(instr.op).append(" ");
                if (instr.dest != null) asmText.append(instr.dest);
                if (instr.j != null) asmText.append(", ").append(instr.j);
                if (instr.k != null) asmText.append(", ").append(instr.k);
                asmText.append("\n");
            }
            simulator.loadProgram(asmText.toString());
        }
        
        BorderPane root = new BorderPane();
        
        // Top: Cycle counter and controls
        root.setTop(createControlPanel());
        
        // Center: Tables in tabs
        root.setCenter(createTablesPanel());
        
        // Bottom: Instruction log
        root.setBottom(createLogPanel());
        
        Scene scene = new Scene(root, 1000, 700);
        window.setScene(scene);
    }

    private VBox createControlPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;");
        
        // First row: Cycle and branch status
        HBox statusBox = new HBox(20);
        lblCycle = new Label("Cycle: 0");
        lblCycle.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
        
        lblBranchStalled = new Label("Branch: Ready");
        lblBranchStalled.setStyle("-fx-font-size: 14;");
        
        lblCacheStats = new Label("Cache: 0 hits, 0 misses (0.0%)");
        lblCacheStats.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");
        
        statusBox.getChildren().addAll(lblCycle, lblBranchStalled, lblCacheStats);
        
        // Second row: Control buttons
        HBox buttonBox = new HBox(10);
        
        Button btnNext = new Button("Next Cycle");
        btnNext.setPrefWidth(100);
        btnNext.setStyle("-fx-font-size: 14;");
        btnNext.setOnAction(e -> nextCycle());
        
        Button btnReset = new Button("Reset");
        btnReset.setPrefWidth(100);
        btnReset.setOnAction(e -> resetSimulation());
        
        Button btnStats = new Button("Show Stats");
        btnStats.setPrefWidth(100);
        btnStats.setOnAction(e -> showStatistics());
        
        Button btnBack = new Button("Back to Config");
        btnBack.setPrefWidth(130);
        btnBack.setOnAction(e -> window.setScene(createConfigScene()));
        
        buttonBox.getChildren().addAll(btnNext, btnReset, btnStats, btnBack);
        
        panel.getChildren().addAll(statusBox, buttonBox);
        
        return panel;
    }

    private TabPane createTablesPanel() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Instruction Issue Table Tab
        Tab issueTab = new Tab("Instruction Queue", createInstructionIssueTable());
        
        // Reservation Stations Tab (Combined)
        Tab stationsTab = new Tab("Reservation Stations", createCombinedStationsPanel());
        
        // Register File Tab
        Tab regTab = new Tab("Register File", createRegFileTable());
        
        // Cache Tab
        Tab cacheTab = new Tab("Cache", createCacheTable());
        
        // Memory Tab
        Tab memTab = new Tab("Memory", createMemoryTable());
        
        tabPane.getTabs().addAll(issueTab, stationsTab, regTab, cacheTab, memTab);
        return tabPane;
    }

    private GridPane createCombinedStationsPanel() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        
        // Top row: ADD and MUL
        grid.add(createStationTable("ADD"), 0, 0);
        grid.add(createStationTable("MUL"), 1, 0);
        
        // Bottom row: LOAD and STORE
        grid.add(createLoadBufferTable(), 0, 1);
        grid.add(createStoreBufferTable(), 1, 1);
        
        return grid;
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
        table.setPrefHeight(180);
        
        // Get stations by type from simulator and set as table data
        List<ReservationStation> stationsToDisplay = getStationsByType(type);
        table.setItems(FXCollections.observableArrayList(stationsToDisplay));
        
        // Store table reference for updates
        if (type.equals("ADD")) {
            addStationTable = table;
        } else if (type.equals("MUL")) {
            mulStationTable = table;
        }
        
        box.getChildren().addAll(new Label(type + " Stations:"), table);
        return box;
    }

    private List<ReservationStation> getStationsByType(String type) {
        if (simulator == null) return new ArrayList<>();
        
        switch (type) {
            case "ADD":
                return simulator.getAddStations();
            case "MUL":
                return simulator.getMulStations();
            default:
                return new ArrayList<>();
        }
    }
    
    private VBox createInstructionIssueTable() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        
        issueTable = new TableView<>();
        
        TableColumn<Instruction, String> colInstr = new TableColumn<>("Instruction");
        colInstr.setCellValueFactory(cellData -> {
            Instruction i = cellData.getValue();
            StringBuilder instrStr = new StringBuilder(i.op.toString());
            
            // Handle branch instructions specially (BNE, BEQ)
            if (i.op == Instruction.OpType.BNE || i.op == Instruction.OpType.BEQ) {
                // Format: BNE R1, R2, target
                if (i.j != null) instrStr.append(" ").append(i.j);
                if (i.k != null) instrStr.append(", ").append(i.k);
                if (i.immediate != 0 || i.k != null) instrStr.append(", ").append(i.immediate);
            }
            // Handle load instructions (L.D F0, 8(R1))
            else if (i.op == Instruction.OpType.LD || i.op == Instruction.OpType.L_D ||
                     i.op == Instruction.OpType.LW || i.op == Instruction.OpType.L_S) {
                // Format: L.D dest, offset(base)
                if (i.dest != null) instrStr.append(" ").append(i.dest);
                instrStr.append(", ").append(i.immediate).append("(");
                if (i.j != null) instrStr.append(i.j);
                instrStr.append(")");
            }
            // Handle store instructions (S.D F4, 8(R1))
            else if (i.op == Instruction.OpType.SD || i.op == Instruction.OpType.S_D ||
                     i.op == Instruction.OpType.SW || i.op == Instruction.OpType.S_S) {
                // Format: S.D source, offset(base)
                if (i.j != null) instrStr.append(" ").append(i.j);
                instrStr.append(", ").append(i.immediate).append("(");
                if (i.k != null) instrStr.append(i.k);
                instrStr.append(")");
            }
            else {
                // Normal instructions (ADD, MUL, DADDI, etc.)
                // Add destination
                if (i.dest != null) {
                    instrStr.append(" ").append(i.dest);
                }
                
                // Add first source operand
                if (i.j != null) {
                    instrStr.append(", ").append(i.j);
                }
                
                // Add second source operand or immediate
                if (i.k != null) {
                    instrStr.append(", ").append(i.k);
                } else if (i.immediate != 0 && (i.op == Instruction.OpType.DADDI || 
                           i.op == Instruction.OpType.DSUBI)) {
                    // For DADDI/DSUBI, show immediate as third operand
                    instrStr.append(", ").append(i.immediate);
                }
            }
            
            return new javafx.beans.property.SimpleStringProperty(instrStr.toString());
        });
        colInstr.setPrefWidth(180);
        
        TableColumn<Instruction, Integer> colIssue = new TableColumn<>("Issue");
        colIssue.setCellValueFactory(cellData -> {
            int val = cellData.getValue().issueCycle;
            return new javafx.beans.property.SimpleObjectProperty<>(val >= 0 ? val : null);
        });
        colIssue.setPrefWidth(60);
        
        TableColumn<Instruction, Integer> colExecStart = new TableColumn<>("Exec Start");
        colExecStart.setCellValueFactory(cellData -> {
            int val = cellData.getValue().executionStartCycle;
            return new javafx.beans.property.SimpleObjectProperty<>(val >= 0 ? val : null);
        });
        colExecStart.setPrefWidth(80);
        
        TableColumn<Instruction, Integer> colExecEnd = new TableColumn<>("Exec End");
        colExecEnd.setCellValueFactory(cellData -> {
            int val = cellData.getValue().executionEndCycle;
            return new javafx.beans.property.SimpleObjectProperty<>(val >= 0 ? val : null);
        });
        colExecEnd.setPrefWidth(80);
        
        TableColumn<Instruction, Integer> colWrite = new TableColumn<>("Write Result");
        colWrite.setCellValueFactory(cellData -> {
            int val = cellData.getValue().writeResultCycle;
            return new javafx.beans.property.SimpleObjectProperty<>(val >= 0 ? val : null);
        });
        colWrite.setPrefWidth(90);
        
        issueTable.getColumns().addAll(colInstr, colIssue, colExecStart, colExecEnd, colWrite);
        issueTable.setPrefHeight(300);
        issueTable.setItems(FXCollections.observableArrayList(loadedInstructions));
        
        box.getChildren().addAll(new Label("Instruction Issue Queue:"), issueTable);
        return box;
    }
    
    private VBox createLoadBufferTable() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        
        TableView<LoadBuffer> table = new TableView<>();
        
        TableColumn<LoadBuffer, String> colName = new TableColumn<>("Buffer");
        colName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().name));
        colName.setPrefWidth(80);
        
        TableColumn<LoadBuffer, Boolean> colBusy = new TableColumn<>("Busy");
        colBusy.setCellValueFactory(data -> new javafx.beans.property.SimpleBooleanProperty(data.getValue().busy));
        colBusy.setPrefWidth(60);
        
        TableColumn<LoadBuffer, Integer> colAddress = new TableColumn<>("Address");
        colAddress.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(
            data.getValue().addressReady ? data.getValue().calculatedAddress : null));
        colAddress.setPrefWidth(100);
        
        TableColumn<LoadBuffer, String> colQj = new TableColumn<>("Qj");
        colQj.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().Qj != null ? data.getValue().Qj : ""));
        colQj.setPrefWidth(80);
        
        table.getColumns().addAll(colName, colBusy, colAddress, colQj);
        table.setPrefHeight(180);
        
        if (simulator != null) {
            table.setItems(FXCollections.observableArrayList(simulator.getLoadBuffers()));
        }
        
        // Store table reference for updates
        loadBufferTable = table;
        
        box.getChildren().addAll(new Label("LOAD Buffers:"), table);
        return box;
    }
    
    private VBox createStoreBufferTable() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        
        TableView<StoreBuffer> table = new TableView<>();
        
        TableColumn<StoreBuffer, String> colName = new TableColumn<>("Buffer");
        colName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().name));
        colName.setPrefWidth(80);
        
        TableColumn<StoreBuffer, Boolean> colBusy = new TableColumn<>("Busy");
        colBusy.setCellValueFactory(data -> new javafx.beans.property.SimpleBooleanProperty(data.getValue().busy));
        colBusy.setPrefWidth(60);
        
        TableColumn<StoreBuffer, Integer> colAddress = new TableColumn<>("Address");
        colAddress.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(
            data.getValue().addressReady ? data.getValue().calculatedAddress : null));
        colAddress.setPrefWidth(100);
        
        TableColumn<StoreBuffer, Double> colValue = new TableColumn<>("Value");
        colValue.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(
            data.getValue().valueReady ? data.getValue().V_Value : null));
        colValue.setPrefWidth(80);
        
        TableColumn<StoreBuffer, String> colQj = new TableColumn<>("Qj");
        colQj.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().Qj != null ? data.getValue().Qj : ""));
        colQj.setPrefWidth(70);
        
        TableColumn<StoreBuffer, String> colQk = new TableColumn<>("Qk");
        colQk.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().Qk != null ? data.getValue().Qk : ""));
        colQk.setPrefWidth(70);
        
        table.getColumns().addAll(colName, colBusy, colAddress, colValue, colQj, colQk);
        table.setPrefHeight(180);
        
        if (simulator != null) {
            table.setItems(FXCollections.observableArrayList(simulator.getStoreBuffers()));
        }
        
        // Store table reference for updates
        storeBufferTable = table;
        
        box.getChildren().addAll(new Label("STORE Buffers:"), table);
        return box;
    }
    
    private VBox createRegFileTable() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        
        regFileTable = new TableView<>();
        
        TableColumn<Register, String> colName = new TableColumn<>("Register");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(100);
        
        TableColumn<Register, String> colValue = new TableColumn<>("Value");
        colValue.setCellValueFactory(cellData -> {
            double val = cellData.getValue().value;
            // Handle NaN and display as 0.0 or format the value
            if (Double.isNaN(val)) {
                return new javafx.beans.property.SimpleStringProperty("0.0");
            } else {
                return new javafx.beans.property.SimpleStringProperty(String.format("%.2f", val));
            }
        });
        colValue.setPrefWidth(150);
        
        TableColumn<Register, String> colQi = new TableColumn<>("Qi (Dependency)");
        colQi.setCellValueFactory(new PropertyValueFactory<>("Qi"));
        colQi.setPrefWidth(150);
        
        regFileTable.getColumns().addAll(colName, colValue, colQi);
        regFileTable.setPrefHeight(400);
        
        // Populate with all 32 float + 32 integer registers (MIPS architecture)
        if (simulator != null) {
            List<Register> allRegs = new ArrayList<>();
            Register[] floats = simulator.getFloatRegs();
            Register[] ints = simulator.getIntRegs();
            
            // Add all 32 float registers
            for (int i = 0; i < floats.length; i++) {
                allRegs.add(floats[i]);
            }
            // Add all 32 integer registers
            for (int i = 0; i < ints.length; i++) {
                allRegs.add(ints[i]);
            }
            
            regFileTable.setItems(FXCollections.observableArrayList(allRegs));
        }
        
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
        cacheTable.setPrefHeight(250);
        
        // Populate with cache blocks from simulator
        if (simulator != null) {
            MemorySystem memSys = simulator.getMemorySystem();
            if (memSys != null) {
                cacheTable.setItems(FXCollections.observableArrayList(memSys.getCacheBlocks()));
            }
        }
        
        box.getChildren().addAll(new Label("Cache Blocks:"), cacheTable);
        return box;
    }

    private VBox createMemoryTable() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        
        memoryTable = new TableView<>();
        
        TableColumn<MemoryRow, String> colAddr = new TableColumn<>("Address");
        colAddr.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            String.format("0x%04X", data.getValue().address)));
        colAddr.setPrefWidth(100);
        
        TableColumn<MemoryRow, String> colData = new TableColumn<>("Data (Hex)");
        colData.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().dataHex));
        colData.setPrefWidth(300);
        
        memoryTable.getColumns().addAll(colAddr, colData);
        memoryTable.setPrefHeight(250);
        
        Label info = new Label("Main Memory (showing first 256 bytes, 16 bytes per row):");
        box.getChildren().addAll(info, memoryTable);
        return box;
    }

    private VBox createLogPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setPrefHeight(100);
        
        Label label = new Label("Instruction/Cycle Log:");
        
        instructionLog = new TextArea();
        instructionLog.setEditable(false);
        instructionLog.setWrapText(true);
        instructionLog.setPrefRowCount(3);
        
        panel.getChildren().addAll(label, instructionLog);
        return panel;
    }

    /**
     * Role 1: Implement "Next Cycle" button and main Cycle() loop
     */
    private void nextCycle() {
        if (simulator != null) {
            int beforeCycle = simulator.getCurrentCycle();
            simulator.nextCycle();
            int afterCycle = simulator.getCurrentCycle();
            
            System.out.println("=== CYCLE TRANSITION: " + beforeCycle + " -> " + afterCycle + " ===");
            
            updateTables();
            updateStatusIndicators();
            lblCycle.setText("Cycle: " + afterCycle);
            logCycleInfo();
        }
    }

    private void resetSimulation() {
        if (simulator != null) {
            simulator.reset();
            
            // Reload the instructions if they were loaded
            if (!loadedInstructions.isEmpty()) {
                StringBuilder asmText = new StringBuilder();
                for (Instruction instr : loadedInstructions) {
                    asmText.append(instr.op).append(" ");
                    if (instr.dest != null) asmText.append(instr.dest);
                    if (instr.j != null) asmText.append(", ").append(instr.j);
                    if (instr.k != null) asmText.append(", ").append(instr.k);
                    asmText.append("\n");
                }
                simulator.loadProgram(asmText.toString());
            }
            
            updateTables();
            updateStatusIndicators();
            lblCycle.setText("Cycle: 0");
            instructionLog.clear();
            showInfo("Simulation reset successfully");
        }
    }

    private void updateStatusIndicators() {
        if (simulator == null) return;
        
        // Update branch stall indicator
        boolean branchStalled = simulator.isBranchStalled();
        lblBranchStalled.setText(branchStalled ? "Branch: STALLED" : "Branch: Ready");
        lblBranchStalled.setStyle("-fx-font-size: 14; -fx-text-fill: " + 
            (branchStalled ? "red; -fx-font-weight: bold;" : "green;"));
        
        // Update cache statistics
        MemorySystem memSys = simulator.getMemorySystem();
        if (memSys != null) {
            int hits = memSys.getHits();
            int misses = memSys.getMisses();
            double hitRate = memSys.getHitRate();
            lblCacheStats.setText(String.format("Cache: %d hits, %d misses (%.1f%%)", 
                hits, misses, hitRate));
        }
    }
    
    private void showStatistics() {
        if (simulator == null) return;
        
        MemorySystem memSys = simulator.getMemorySystem();
        if (memSys != null) {
            StringBuilder stats = new StringBuilder();
            stats.append("=== Simulator Statistics ===\n\n");
            stats.append("Current Cycle: ").append(simulator.getCurrentCycle()).append("\n");
            stats.append("Branch Stalled: ").append(simulator.isBranchStalled() ? "Yes" : "No").append("\n\n");
            
            stats.append("--- Cache Statistics ---\n");
            stats.append("Hits: ").append(memSys.getHits()).append("\n");
            stats.append("Misses: ").append(memSys.getMisses()).append("\n");
            stats.append("Hit Rate: ").append(String.format("%.2f%%", memSys.getHitRate())).append("\n\n");
            
            stats.append("--- Active Stations ---\n");
            long busyAdd = simulator.getAddStations().stream().filter(rs -> rs.busy).count();
            long busyMul = simulator.getMulStations().stream().filter(rs -> rs.busy).count();
            long busyLoad = simulator.getLoadBuffers().stream().filter(lb -> lb.busy).count();
            long busyStore = simulator.getStoreBuffers().stream().filter(sb -> sb.busy).count();
            
            stats.append("ADD Stations Busy: ").append(busyAdd).append("/").append(simulator.getAddStations().size()).append("\n");
            stats.append("MUL Stations Busy: ").append(busyMul).append("/").append(simulator.getMulStations().size()).append("\n");
            stats.append("LOAD Buffers Busy: ").append(busyLoad).append("/").append(simulator.getLoadBuffers().size()).append("\n");
            stats.append("STORE Buffers Busy: ").append(busyStore).append("/").append(simulator.getStoreBuffers().size()).append("\n\n");
            
            stats.append("--- Instruction Queue ---\n");
            stats.append("Instructions Waiting: ").append(simulator.getInstructionQueue().size()).append("\n");
            stats.append("Total Instructions: ").append(loadedInstructions.size()).append("\n");
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Simulator Statistics");
            alert.setHeaderText("Detailed Statistics");
            alert.setContentText(stats.toString());
            alert.showAndWait();
        }
    }
    
    private void updateTables() {
        if (simulator == null) return;
        
        // Update instruction issue table with simulator's program (which has updated cycle info)
        if (issueTable != null) {
            List<Instruction> currentProgram = simulator.getProgram();
            issueTable.setItems(FXCollections.observableArrayList(currentProgram));
            issueTable.refresh();
        }
        
        // Update ADD reservation stations
        if (addStationTable != null) {
            addStationTable.setItems(FXCollections.observableArrayList(simulator.getAddStations()));
            addStationTable.refresh();
        }
        
        // Update MUL reservation stations
        if (mulStationTable != null) {
            mulStationTable.setItems(FXCollections.observableArrayList(simulator.getMulStations()));
            mulStationTable.refresh();
        }
        
        // Update LOAD buffers
        if (loadBufferTable != null) {
            loadBufferTable.setItems(FXCollections.observableArrayList(simulator.getLoadBuffers()));
            loadBufferTable.refresh();
        }
        
        // Update STORE buffers
        if (storeBufferTable != null) {
            storeBufferTable.setItems(FXCollections.observableArrayList(simulator.getStoreBuffers()));
            storeBufferTable.refresh();
        }
        
        // Update register file table (all 32 float + 32 integer)
        if (regFileTable != null) {
            List<Register> allRegs = new ArrayList<>();
            Register[] floats = simulator.getFloatRegs();
            Register[] ints = simulator.getIntRegs();
            
            // Add all 32 float registers
            for (Register reg : floats) {
                allRegs.add(reg);
            }
            // Add all 32 integer registers
            for (Register reg : ints) {
                allRegs.add(reg);
            }
            
            regFileTable.setItems(FXCollections.observableArrayList(allRegs));
            regFileTable.refresh();
        }
        
        // Update cache table with actual cache blocks
        if (cacheTable != null) {
            MemorySystem memSys = simulator.getMemorySystem();
            if (memSys != null) {
                cacheTable.setItems(FXCollections.observableArrayList(memSys.getCacheBlocks()));
            }
            cacheTable.refresh();
        }
        
        // Update memory table with first 256 bytes (16 rows of 16 bytes each)
        if (memoryTable != null) {
            MemorySystem memSys = simulator.getMemorySystem();
            if (memSys != null) {
                List<MemoryRow> memRows = new ArrayList<>();
                for (int addr = 0; addr < Math.min(256, memSys.getMemorySize()); addr += 16) {
                    StringBuilder hexData = new StringBuilder();
                    for (int i = 0; i < 16 && (addr + i) < memSys.getMemorySize(); i++) {
                        hexData.append(String.format("%02X ", memSys.getMemoryByte(addr + i) & 0xFF));
                    }
                    memRows.add(new MemoryRow(addr, hexData.toString().trim()));
                }
                memoryTable.setItems(FXCollections.observableArrayList(memRows));
            }
            memoryTable.refresh();
        }
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

    /**
     * Helper class for memory table display
     */
    private static class MemoryRow {
        int address;
        String dataHex;
        
        MemoryRow(int address, String dataHex) {
            this.address = address;
            this.dataHex = dataHex;
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
